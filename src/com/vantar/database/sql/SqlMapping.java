package com.vantar.database.sql;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.*;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.string.StringUtil;
import java.util.*;
import static com.vantar.database.query.QueryOperator.*;


public class SqlMapping {

    public static SqlParams getSqlMatches(QueryCondition condition, Dto dto) {
        if (condition == null || condition.q.size() == 0) {
            return null;
        }

        SqlParams sqlParams = new SqlParams();
        for (QueryMatchItem item : condition.q) {
            item.setDatetimeAsString();
            if (item.type == QUERY) {
                SqlParams innerParams = getSqlMatches(item.queryValue, dto);
                sqlParams.addTemplate("(" + innerParams.getTemplate()  + ")");
                sqlParams.addValues(innerParams.getValues());
                continue;
            }

            if (item.type == EXISTS) {
                SqlParams innerParams = getSqlMatches(item.queryValue, dto);
                sqlParams.addTemplate("EXISTS (SELECT 1 FROM " + item.queryValue.storage + " WHERE " + innerParams.getTemplate()  + ")");
                sqlParams.addValues(innerParams.getValues());
                continue;
            }

            String fieldName = StringUtil.toSnakeCase(item.fieldName);

            switch (item.type) {
                case EQUAL:
                    sqlParams.addTemplate(fieldName + "=?");
                    sqlParams.addValue(item.getValue());
                    break;
                case NOT_EQUAL:
                    sqlParams.addTemplate(fieldName + "<>?");
                    sqlParams.addValue(item.getValue());
                    break;
                case LIKE:
                    sqlParams.addTemplate(fieldName + " ILIKE ?");
                    sqlParams.addValue(item.getValue());
                    break;
                case NOT_LIKE:
                    sqlParams.addTemplate(fieldName + " NOT ILIKE ?");
                    sqlParams.addValue(item.getValue());
                    break;

                case NOT_IN:
                case IN:
                    StringBuilder inStr = new StringBuilder();
                    for (Object value : item.getValues()) {
                        inStr.append('?').append(',');
                        sqlParams.addValue(value);
                    }
                    inStr.setLength(inStr.length() - 1);
                    sqlParams.addTemplate(fieldName + (item.type == NOT_IN ? " NOT IN(" : " IN(") + inStr.toString() + ')');
                    break;

                case FULL_SEARCH:
                    StringBuilder fullStr = new StringBuilder();
                    dto.getPropertyTypes().forEach((name, tClass) -> {
                        if (tClass.equals(String.class)) {
                            fullStr.append(name).append(" ILIKE ?").append(" OR ");
                            sqlParams.addValue(item.getValue());
                        }
                    });
                    fullStr.setLength(fullStr.length() - 1);
                    sqlParams.addTemplate(fullStr.toString());
                    break;

                case BETWEEN:
                    Object[] values = item.getValues();
                    sqlParams.addTemplate("(" + fieldName + " BETWEEN ? AND ?)");
                    sqlParams.addValue(values[0]);
                    sqlParams.addValue(values[1]);
                    break;
                case NOT_BETWEEN:
                    Object[] values2 = item.getValues();
                    sqlParams.addTemplate("(" + fieldName + " NOT BETWEEN ? AND ?)");
                    sqlParams.addValue(values2[0]);
                    sqlParams.addValue(values2[1]);
                    break;

                case LESS_THAN:
                    sqlParams.addTemplate(fieldName + "<?");
                    sqlParams.addValue(item.getValue());
                    break;
                case GREATER_THAN:
                    sqlParams.addTemplate(fieldName + ">?");
                    sqlParams.addValue(item.getValue());
                    break;
                case LESS_THAN_EQUAL:
                    sqlParams.addTemplate(fieldName + "<=?");
                    sqlParams.addValue(item.getValue());
                    break;
                case GREATER_THAN_EQUAL:
                    sqlParams.addTemplate(fieldName + ">=?");
                    sqlParams.addValue(item.getValue());
                    break;

                case IS_NULL:
                    sqlParams.addTemplate(fieldName + " IS NULL");
                    break;
                case IS_NOT_NULL:
                    sqlParams.addTemplate(fieldName + " IS NOT NULL");
                    break;
                case IS_EMPTY:
                    sqlParams.addTemplate(fieldName + " IS NULL AND " + fieldName + "=''");
                    break;
                case IS_NOT_EMPTY:
                    sqlParams.addTemplate(fieldName + " IS NOT NULL AND " + fieldName + "<>''");
                    break;
            }
        }

        switch (condition.operator) {
            case OR:
                sqlParams.setGlue(" OR ");
                break;
            case NOR:
            case NOT:
                sqlParams.setGlue(" NOT ");
                break;
            default:
                sqlParams.setGlue(" AND ");
        }

        return sqlParams;
    }

    public static SqlParams queryBuilderToSql(QueryBuilder q, CountMethod countMethod) {
        String table = q.getDto().getStorage();
        String tClass = q.getDto().getClass().getSimpleName();
        Map<String, String> tableMap = new HashMap<>();
        tableMap.put(table, "t1");
        tableMap.put(tClass, "t1");

        // > > > FROM

        StringBuilder from = new StringBuilder("FROM ").append(table).append(" AS t1 ");
        List<QueryJoin> joins = q.getJoins();

        if (joins != null) {
            int i = 1;
            for (QueryJoin join : joins) {
                String tableRight = join.getTable();
                String classRight = join.gettClass();
                String as = join.getAs();
                if (as == null) {
                    as = "t" + (++i);
                }
                tableMap.put(tableRight, as);
                tableMap.put(classRight, as);

                from.append(join.getJoin()).append(' ').append(tableRight).append(" AS ").append(as).append(' ');

                switch (join.getJoin()) {
                    case QueryJoin.INNER_JOIN:
                    case QueryJoin.LEFT_JOIN:
                    case QueryJoin.SELF_JOIN:
                    case QueryJoin.FULL_JOIN:
                        from.append("ON ")
                            .append(tableToAs(join.getKeyLeft(), tableMap))
                            .append("=")
                            .append(tableToAs(join.getKeyRight(), tableMap))
                            .append(' ');
                        break;

                    case QueryJoin.CROSS_JOIN:
                    case QueryJoin.NATURAL_JOIN:
                        // nothing to add
                        break;
                }
            }
        }

        // > > > SELECT

        Set<String> selectCols = new LinkedHashSet<>();
        if (q.getColumns() != null) {
            for (String col : q.getColumns()) {
                selectCols.add(tableToAs(col, tableMap));
            }
        }

        // > > > GROUP BY, HAVING

        List<QueryGroup> groups = q.getGroup();
        Set<String> groupCols;

        if (groups != null && !groups.isEmpty()) {
            groupCols = new LinkedHashSet<>();
            for (QueryGroup group : q.getGroup()) {
                StringBuilder selectCol = new StringBuilder();
                switch (group.groupType) {
                    case SUM:
                        selectCol.append("SUM(");
                        break;
                    case AVG:
                        selectCol.append("AVG(");
                        break;
                    case COUNT:
                        selectCol.append("COUNT(");
                        break;
                    case MIN:
                        selectCol.append("MIN(");
                        break;
                    case MAX:
                        selectCol.append("MAX(");
                        break;
                    default:
                        groupCols.add(tableToAs(group.columns[0], tableMap));
                        continue;
                }

                selectCol.append(tableToAs(group.columns[0], tableMap)).append(")");
                if (StringUtil.isNotEmpty(group.columns[1])) {
                    selectCol.append(" AS ").append(group.columns[1]);
                }
                selectCols.add(selectCol.toString());
            }
        } else {
            groupCols = null;
        }

        // > > > SELECT

        if (q.getColumnsMany() != null) {
            for (ColumnMany columnMany :  q.getColumnsMany()) {
                String tableRight = StringUtil.toSnakeCase(columnMany.className);
                if (tableMap.containsKey(tableRight)) {
                    tableRight = tableMap.get(tableRight);
                }

                String[] columns = columnMany.properties;
                for (int i = 0; i < columns.length; i++) {
                    columns[i] = tableRight + '.' + StringUtil.toSnakeCase(columns[i]);
                }

                selectCols.add(getArrayColumn(columns, StringUtil.toSnakeCase(columnMany.propertyOut)));
            }

            if (groupCols == null) {
                groupCols = new LinkedHashSet<>();
            }
            groupCols.add("t1.id");
        }

        if (selectCols.isEmpty()) {
            selectCols.add("*");
        }

        if (countMethod.equals(CountMethod.INCLUDE_TOTAL_COUNT)) {
            selectCols.add(getFullCountColumn());
        }

        SqlParams sql = new SqlParams();
        if (countMethod.equals(CountMethod.COUNT_QUERY)) {
            sql.appendTemplate("SELECT COUNT(*) ");
        } else {
            sql.appendTemplate("SELECT " + CollectionUtil.join(selectCols, ", ") + " " + from.toString());
        }

        // > > > WHERE

        SqlParams condition = getSqlMatches(q.condition(), q.getDto());
        if (condition != null) {
            sql.appendTemplate("WHERE " + tableToAs(condition.getTemplate(), tableMap) + " ");
            sql.addValues(condition.getValues());
        }

        // > > > HAVING

        if (groupCols != null) {
            sql.appendTemplate("GROUP BY " + CollectionUtil.join(groupCols, ',') + " ");
        }

        SqlParams conditionHaving = getSqlMatches(q.getGroupCondition(), q.getDto());
        if (conditionHaving != null) {
            sql.appendTemplate("HAVING " + conditionHaving.getTemplate() + " ");
            sql.addValues(conditionHaving.getValues());
        }


        if (!countMethod.equals(CountMethod.COUNT_QUERY)) {

            // > > > ORDER BY

            String[] sortCols = q.getSort();
            if (sortCols != null && sortCols.length > 0) {
                StringBuilder sort = new StringBuilder();
                for (String col : sortCols) {
                    sort.append(tableToAs(StringUtil.replace(col, VantarParam.SEPARATOR_KEY_VAL, ' '), tableMap)).append(", ");
                }
                sort.setLength(sort.length() - 2);
                sql.appendTemplate("ORDER BY " + sort.toString() + " ");
            }

            // > > > LIMIT

            if (q.getLimit() != null) {
                sql.appendTemplate("LIMIT " + q.getLimit() + " ");
            }
            if (q.getSkip() != null) {
                sql.appendTemplate("OFFSET " + q.getSkip() + " ");
            }
        }

        return sql;
    }

    private static String getFullCountColumn() {
        if (SqlConnection.getDbms().equals(SqlDbms.POSTGRESQL)) {
            return "COUNT(*) OVER() AS " + VantarParam.TOTAL_COUNT;
        }
        return "";
    }

    private static String getArrayColumn(String[] columns, String asColumn) {
        if (SqlConnection.getDbms().equals(SqlDbms.POSTGRESQL)) {
            return "STRING_AGG(DISTINCT " +
                CollectionUtil.join(columns, " || '" +  VantarParam.SEPARATOR_COMMON_COMPLEX + "' || ")
                + "::TEXT, '" + VantarParam.SEPARATOR_BLOCK_COMPLEX + "') AS " + asColumn;
        }
        if (SqlConnection.getDbms().equals(SqlDbms.MYSQL)) {
            return "GROUP_CONCAT(DISTINCT CONCAT(" +
                CollectionUtil.join(columns, " || '" +  VantarParam.SEPARATOR_COMMON_COMPLEX + "' || ")
                + ") SEPARATOR '" + VantarParam.SEPARATOR_BLOCK_COMPLEX + "') AS " + asColumn;
        }
        return "";
    }

    private static String tableToAs(String statement, Map<String, String> colMap) {
        statement = " " + StringUtil.replace(StringUtil.replace(StringUtil.replace(StringUtil.replace(statement, "=", "= "), "<", "< "), ">", "> "), ")", ") ");

        for (Map.Entry<String, String> entry : colMap.entrySet()) {
            String table = entry.getKey() + ".";
            if (statement.contains(table)) {
                statement = StringUtil.replaceWord(statement, table, entry.getValue() + ".");
                break;
            }
        }

        return StringUtil.replace(StringUtil.replace(StringUtil.replace(StringUtil.replace(statement, "= ", "="), "< ", "<"), "> ", ">"), ") ", ")").trim();
    }

    public enum CountMethod {
        NONE,
        INCLUDE_TOTAL_COUNT,
        COUNT_QUERY,
    }
}
