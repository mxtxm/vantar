package com.vantar.database.sql;

import com.vantar.util.string.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.List;


public abstract class SqlQueryHelper {

    /**
     * teh__n
     * teh*n
     * teh?n
     * tehran|shiraz|tabriz
     */
    private static final Logger log = LoggerFactory.getLogger(SqlQueryHelper.class);

    
    public static void setKeywordSearch(String[] columns, String keyword, StringBuilder sql, List<Object> params) {
        sql.append(" AND (false");

        String keywordToSet = "!%(" +
            keyword.toLowerCase()
                .replace("!", "!!")
                .replace("+", "")
                .replace("*", "!%")
                .replace("?", "!%")
                .replace("_", "!_")
                .replace("[", "![") + ")!%";

        for (String column : columns) {
            sql.append(" OR LOWER(").append(column).append(") SIMILAR TO ?");
            params.add(keywordToSet);
        }

        sql.append(')');
    }

    public static void setKeywordSearch(String[] columns, String[] others, String keyword, StringBuilder sql, List<Object> params) {
        sql.append(" AND (false");

        String keywordToSet =
            "%(" +
            keyword.toLowerCase()
                .replace("!", "")
                .replace("+", "")
                .replace("*", "%")
                .replace("_", "\\_")
                .replace("?", "_")
                .replace("[", "\\[")
                .replace("(", "\\(") +
            ")%";

        for (String column : columns) {
            sql.append(" OR LOWER(").append(column).append(") SIMILAR TO ?");
            params.add(keywordToSet);
        }

        log.debug("similar to keyword={}", keywordToSet);
        keyword = getKeywordForLike(keyword);
        log.debug("ilike keyword={}", keyword);

        for (String other : others) {
            sql.append(" OR ").append(other);
            for (int i = 0, l = StringUtil.countMatches(other,'?'); i < l; ++i) {
                params.add(keyword);
            }
        }

        sql.append(')');
    }

    public static String getKeywordForLike(String value) {
        return
            "%" + value
                .replace("*", "%")
                .replace("_", "\\_")
                .replace("?", "_") +
            "%";
    }

    public static void setLike(String column, String value, StringBuilder sql, List<Object> params) {
        if (value == null) {
            return;
        }

        params.add(" AND " + getKeywordForLike(value));
        sql.append(column).append(" ILIKE ?");
    }

    public static void setIn(String column, List<Integer> values, StringBuilder sql) {
        if (values != null && !values.isEmpty()) {
            sql.append(" AND ").append(column).append(" IN(null");
            values.forEach(value -> {
                sql.append(',').append(value);
            });
            sql.append(')');
        }
    }

    public static <T> void setIn(String column, List<T> values, StringBuilder sql, List<Object> params) {
        if (values != null && !values.isEmpty()) {
            sql.append(" AND ").append(column).append(" IN(null");
            values.forEach(value -> {
                sql.append(",?");
                params.add(value);
            });
            sql.append(')');
        }
    }

    public static <T> void setInTemplate(String column, List<T> values, StringBuilder sql) {
        if (values != null && !values.isEmpty()) {
            sql.append(" AND ").append(column).append(" IN(null");
            values.forEach(value -> {
                sql.append(",?");
            });
            sql.append(')');
        }
    }

    public static void setBoolean(String column, Boolean value, StringBuilder sql) {
        if (value != null) {
            sql.append(" AND ").append(column).append('=').append(value ? "true " : "false ");
        }
    }

    public static void setId(String column, Integer value, StringBuilder sql) {
        if (value != null && value > 0) {
            sql.append(" AND ").append(column).append('=').append(value).append(' ');
        }
    }

    public static void setInt(String column, Integer value, StringBuilder sql) {
        setInt(column, "=", value, sql);
    }
    public static void setInt(String column, String operator, Integer value, StringBuilder sql) {
        if (value != null) {
            sql.append(" AND ").append(column).append(operator).append(value).append(' ');
        }
    }

    public static void setChar(String column, Character value, StringBuilder sql) {
        if (value != null) {
            sql.append(" AND ").append(column).append("='").append(value).append("' ");
        }
    }

    public static void setDouble(String column, Double value, StringBuilder sql) {
        if (value != null) {
            sql.append(" AND ").append(column).append('=').append(value).append(' ');
        }
    }

    public static void setBetween(String column, Object valueLower, Object valueUpper, StringBuilder sql, List<Object> params) {
        if (valueLower != null && valueUpper != null) {
            sql.append(" AND ").append(column).append(" BETWEEN ? AND ?");
            params.add(valueLower);
            params.add(valueUpper);
        }
    }

    public static void setDateOverlaps(String columnLower, String columnUpper, Date valueLower,
        Date valueUpper, StringBuilder sql, List<Object> params) {

        if (valueLower != null && valueUpper != null) {
            sql.append(" AND (").append(columnLower).append(',').append(columnUpper).append(") OVERLAPS (?,?)");
            params.add(valueLower);
            params.add(valueUpper);
        }
    }
}
