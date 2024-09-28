package com.vantar.database.query;

import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.*;
import com.vantar.locale.VantarKey;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.util.*;


public class QueryBuilder {

    private static final Logger log = LoggerFactory.getLogger(QueryBuilder.class);
    private List<ValidationError> errors;

    private Dto dto;
    private QueryCondition condition;
    private QueryCondition conditionGroup;
    private String[] sort;
    private Integer skip;
    private Integer limit;
    private List<QueryGroup> group;
    private List<String> union;
    private String[] locales;

    private List<String> columns;
    private List<ColumnMany> columnsMany;
    private List<QueryJoin> joins;
    private boolean updateMany;

    private boolean isPagination;
    private long total;


    public QueryBuilder() {

    }

    public QueryBuilder(Dto dto) {
        this.dto = dto;
    }

    public QueryBuilder setLocale(String... locales) {
        this.locales = locales;
        return this;
    }

    public String[] getLocale() {
        return locales;
    }

    // > > > DTO

    public QueryBuilder setDto(Dto dto) {
        this.dto = dto;
        return this;
    }

    public Dto getDto() {
        return dto;
    }

    // > > > SORT

    public QueryBuilder sort(String... sort) {
        this.sort = sort;
        return this;
    }

    public String[] getSort() {
        return sort;
    }

    // > > > CONDITION

    public QueryBuilder setCondition(QueryCondition condition) {
        this.condition = condition;
        return this;
    }

    public QueryBuilder clearConditions() {
        this.condition = null;
        return this;
    }

    public QueryCondition condition() {
        return condition(QueryOperator.AND);
    }

    public QueryCondition conditionInspect(QueryOperator operator) {
        if (condition == null) {
            condition = new QueryCondition(operator);
        }
        condition.inspect = true;
        return condition;
    }

    public QueryCondition conditionInspect() {
        return conditionInspect(QueryOperator.AND);
    }

    public QueryCondition condition(QueryOperator operator) {
        if (condition == null) {
            condition = new QueryCondition(operator);
        }
        return condition;
    }

    public boolean conditionIsEmpty() {
//        if (dto.isDeleteLogicalEnabled()) {
//            return false;
//        }
        return condition == null;
    }

    public QueryCondition getCondition() {
        return condition;
    }

    public QueryBuilder setConditionFromDtoEqualTextMatch() {
        return setConditionFromDto(QueryOperator.AND, false);
    }

    public QueryBuilder setConditionFromDtoEqualTextMatch(QueryOperator operator) {
        return setConditionFromDto(operator, false);
    }

    public QueryBuilder setConditionFromDtoLikeTextMatch() {
        return setConditionFromDto(QueryOperator.AND, true);
    }

    public QueryBuilder setConditionFromDtoLikeTextMatch(QueryOperator operator) {
        return setConditionFromDto(operator, true);
    }

    // > > > GROUPING

    public QueryBuilder addGroup(String column) {
        this.group.add(new QueryGroup(QueryGroupType.MAP, column));
        return this;
    }

    /**
     * @param columns ---> columns(0:last-1) As columns(last)
     */
    public QueryBuilder addGroup(String... columns) {
        if (this.group == null) {
            this.group = new ArrayList<>(5);
        }
        this.group.add(new QueryGroup(columns));
        return this;
    }

    public QueryBuilder addGroup(QueryGroupType groupType, String column) {
        if (this.group == null) {
            this.group = new ArrayList<>(5);
        }
        this.group.add(new QueryGroup(groupType, column));
        return this;
    }

    public QueryBuilder addGroup(QueryGroupType groupType, String... columns) {
        if (this.group == null) {
            this.group = new ArrayList<>(5);
        }
        this.group.add(new QueryGroup(groupType, columns));
        return this;
    }

    public List<QueryGroup> getGroup() {
        return group;
    }

    public boolean groupIsEmpty() {
        return group == null || group.isEmpty();
    }

    public QueryBuilder setGroupCondition(QueryCondition condition) {
        conditionGroup = condition;
        return this;
    }

    public QueryCondition groupCondition() {
        return condition(QueryOperator.AND);
    }

    public QueryCondition groupCondition(QueryOperator operator) {
        if (conditionGroup == null) {
            conditionGroup = new QueryCondition(operator);
        }
        return conditionGroup;
    }

    public boolean groupConditionIsEmpty() {
        return conditionGroup == null;
    }

    public QueryCondition getGroupCondition() {
        return conditionGroup;
    }

    // > > > LIMIT AND PAGINATION

    public QueryBuilder setTotal(long total) {
        this.total = total;
        return this;
    }

    public long getTotal() {
        return total;
    }

    /**
     * first n records
     */
    public QueryBuilder top(int n) {
        limit = n;
        skip = 0;
        return this;
    }

    /**
     * n >= 0
     */
    public QueryBuilder skip(int n) {
        skip = n;
        return this;
    }

    public QueryBuilder limit(int n) {
        limit = n;
        return this;
    }

    /**
     * pageNumber >= 1
     */
    public QueryBuilder page(int page, int length) {
        limit = length;
        skip = (page - 1) * length;
        return this;
    }

    public Integer getSkip() {
        return skip;
    }

    public Integer getLimit() {
        return limit;
    }

    public int getPageNo() {
        return skip == null || limit == null ? 1 : (skip / limit) + 1;
    }

    public void setPagination() {
        isPagination = true;
    }

    public void disablePagination() {
        isPagination = false;
    }

    public void disableLimit() {
        isPagination = false;
        skip = null;
        limit = null;
    }

    public boolean isPagination() {
        return isPagination;
    }

    public void addUnion(String name) {
        if (union == null) {
            union = new ArrayList<>();
        }
        union.add(name);
    }

    public List<String> getUnion() {
        return union;
    }


    // > > > SQL
    // > > > COL SELECT

    public QueryBuilder setColumns(String... columns) {
        for (String column : columns) {
            boolean isManyToMany;
            if (StringUtil.contains(column, '.')) {
                String[] parts = StringUtil.split(column, VantarParam.SEPARATOR_DOT);
                isManyToMany = parts[0].equals(dto.getClass().getSimpleName()) && dto.hasAnnotation(parts[1], ManyToManyGetData.class);
                if (isManyToMany) {
                    column = parts[1];
                }
            } else {
                isManyToMany = dto.hasAnnotation(column, ManyToManyGetData.class);
            }
            if (isManyToMany) {
                if (joins == null) {
                    joins = new ArrayList<>();
                }

                String[] parts = StringUtil.splitTrim(dto.getField(column).getAnnotation(ManyToManyGetData.class).value(), VantarParam.SEPARATOR_NEXT);
                String junctionTable = parts[0];
                String tableRight = parts[1];

                String leftTable = dto.getStorage();
                joins.add(new QueryJoin(QueryJoin.INNER_JOIN, junctionTable, leftTable + ".id", junctionTable + "." + leftTable + "_id" ));
                joins.add(new QueryJoin(QueryJoin.INNER_JOIN, tableRight, tableRight + ".id", junctionTable + "." + tableRight + "_id" ));

                if (columnsMany == null) {
                    columnsMany = new ArrayList<>();
                }

                columnsMany.add(new ColumnMany(parts[1], StringUtil.splitTrim(parts[2], VantarParam.SEPARATOR_COMMON), column));
            } else {
                if (this.columns == null) {
                    this.columns = new ArrayList<>();
                }
                this.columns.add(column);
            }
        }
        return this;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<ColumnMany> getColumnsMany() {
        return columnsMany;
    }

    // > > > JOIN

    public QueryBuilder addJoin(String joinType, Dto dtoRight) {
        if (joins == null) {
            joins = new ArrayList<>();
        }

        String keyRight = dtoRight.getStorage() + ".id";
        String keyLeft;
        if (dto.contains(keyRight)) {
            keyLeft = dto.getStorage() + "." + dtoRight.getStorage()  + "_id";
        } else {
            keyLeft = null;
            for (QueryJoin join : joins) {
                Dto joinDto = join.getDto();
                if (joinDto.contains(keyRight)) {
                    keyLeft = joinDto.getStorage() + "." + dtoRight.getStorage()  + "_id";
                    break;
                }
            }
        }
        if (keyLeft == null) {
            log.warn("! not joined ({}, {})", joinType, dtoRight.getClass().getSimpleName());
        } else {
            joins.add(new QueryJoin(joinType, dtoRight, keyLeft, keyRight));
        }
        return this;
    }

    public QueryBuilder addJoin(String joinType, Dto dtoRight, String keyLeft, String keyRight) {
        if (joins == null) {
            joins = new ArrayList<>();
        }
        joins.add(new QueryJoin(joinType, dtoRight, keyLeft, keyRight));
        return this;
    }

    public QueryBuilder addJoin(String joinType, Dto dtoLeft, Dto dtoRight) {
        if (joins == null) {
            joins = new ArrayList<>();
        }
        String keyRight = dtoRight.getStorage() + ".id";
        String keyLeft = dtoLeft.contains(keyRight) ? dtoLeft.getStorage() + "." + dtoRight.getStorage()  + "_id" : null;

        if (keyLeft == null) {
            log.warn("! not joined ({}, {}, {})", joinType, dtoLeft.getClass().getSimpleName(), dtoRight.getClass().getSimpleName());
        } else {
            joins.add(new QueryJoin(joinType, dtoRight, keyLeft, keyRight));
        }
        return this;
    }

    public QueryBuilder addJoin(String joinType, Dto dtoRight, String as, String keyLeft, String keyRight) {
        if (joins == null) {
            joins = new ArrayList<>();
        }
        joins.add(new QueryJoin(joinType, dtoRight, as, keyLeft, keyRight));
        return this;
    }

    public List<QueryJoin> getJoins() {
        return joins;
    }

    // < < < END API

    @SuppressWarnings("unchecked")
    private QueryBuilder setConditionFromDto(QueryOperator operator, boolean likeTextMatch) {
        dto.setCreateTime(false);
        dto.setUpdateTime(false);

        if (condition == null) {
            condition = new QueryCondition(operator);
        }
        StorableData.toMap(dto.getStorableData()).forEach((name, value) -> {
            if (value instanceof String) {
                if (likeTextMatch) {
                    condition.like(name, (String) value);
                } else {
                    condition.equal(name, (String) value);
                }
            } else if (value instanceof Number) {
                condition.equal(name, (Number) value);
            } else if (value instanceof DateTime) {
                condition.equal(name, (DateTime) value);
            } else if (value instanceof Character) {
                condition.equal(name, (Character) value);
            } else if (value instanceof Boolean) {
                condition.equal(name, (Boolean) value);
            } else if (value instanceof Map) {
                condition.containsAll(name, (Map<String, ?>) value);
            } else if (value instanceof List<?>) {
                condition.containsAll(name, (List<?>) value);
            } else if (value instanceof Set<?>) {
                condition.containsAll(name, (Set<?>) value);
            } else if (value != null) {
                condition.equal(name, value.toString());
            }
        });

        return this;
    }

    public void addError(String subject, VantarKey msg) {
        if (errors == null) {
            errors = new ArrayList<>(10);
        }
        errors.add(new ValidationError(subject, msg));
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public String toString() {
        return ObjectUtil.toStringViewable(this);
    }

    public boolean isUpdateMany() {
        return updateMany;
    }

    public void setUpdateMany(boolean updateMany) {
        this.updateMany = updateMany;
    }
}
