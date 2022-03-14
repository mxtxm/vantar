package com.vantar.database.query;

import com.vantar.database.common.ValidationError;
import com.vantar.database.datatype.Location;
import com.vantar.database.dto.*;
import com.vantar.exception.DateTimeException;
import com.vantar.locale.VantarKey;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.ObjectUtil;
import org.slf4j.*;
import java.util.*;


public class QueryData {

    protected static final Logger log = LoggerFactory.getLogger(QueryResultBase.class);

    public final List<ValidationError> errors = new ArrayList<>();
    public Boolean all;
    public String dto;
    public String dtoResult;
    public String lang;
    public Boolean pagination;
    public Integer page;
    public Integer length;
    public Integer offset;
    public Integer limit;
    public Long total;
    public String[] sort;
    public QueryData.Condition condition;
    public QueryData.Condition conditionGroup;
    public List<String> columns;
    public List<JoinDef> joins;
    public List<GroupDef> group;

    private Dto cachedDto;
    private Dto cachedDtoResult;


    public QueryData setDto(Dto dto) {
        cachedDto = dto;
        cachedDtoResult = dto;
        return this;
    }

    public QueryData setDto(Dto dto, Dto dtoResult) {
        cachedDto = dto;
        cachedDtoResult = dtoResult;
        return this;
    }

    public boolean isEmpty() {
        if (condition != null && !condition.isEmpty()) {
            return false;
        }
        if (page != null) {
            return false;
        }
        if (offset != null) {
            return false;
        }
        if (limit != null) {
            return false;
        }
        if (sort != null && sort.length > 0) {
            return false;
        }
        if (conditionGroup != null) {
            return false;
        }
        if (columns != null && !columns.isEmpty()) {
            return false;
        }
        if (group != null && !group.isEmpty()) {
            return false;
        }
        return all == null || !all;
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public Dto getDto() {
        if (cachedDto == null) {
            DtoDictionary.Info info = DtoDictionary.get(dto);
            if (info == null) {
                errors.add(new ValidationError("dto", VantarKey.REQUIRED));
                return null;
            }
            cachedDto = info.getDtoInstance();
        }
        return cachedDto;
    }

    public Dto getDtoResult() {
        if (dtoResult == null && cachedDtoResult == null) {
            dtoResult = dto;
        }
        if (cachedDtoResult == null) {
            DtoDictionary.Info info = DtoDictionary.get(dtoResult);
            if (info == null) {
                errors.add(new ValidationError("dtoResult", VantarKey.REQUIRED));
                return null;
            }
            cachedDtoResult = info.getDtoInstance();
        }
        return cachedDtoResult;
    }

    public boolean isPagination() {
        return pagination != null && pagination;
    }

    public String toString() {
        return ObjectUtil.toString(this);
    }

    public QueryCondition getCondition() {
        return getCondition(condition);
    }

    public QueryCondition getGroupCondition() {
        return getCondition(conditionGroup);
    }

    private QueryCondition getCondition(QueryData.Condition condition) {
        if (condition == null) {
            return null;
        }

        Dto dto = getDto();
        if (dto == null) {
            return null;
        }

        QueryOperator operator;
        if (condition.operator == null) {
            operator = QueryOperator.AND;
        } else {
            switch (condition.operator.toLowerCase()) {
                case "or":
                    operator = QueryOperator.OR;
                    break;
                case "not":
                    operator = QueryOperator.NOT;
                    break;
                case "nor":
                    operator = QueryOperator.NOR;
                    break;
                default:
                    operator = QueryOperator.AND;
                    break;
            }
        }
        QueryCondition c = new QueryCondition(operator);

        for (QueryData.Condition.Item item : condition.items) {
            if (item.type == null) {
                errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_INVALID_CONDITION_TYPE));
                return null;
            }
            String conditionType = item.type.toUpperCase();
            if (item.col == null && !conditionType.equals("FULL_SEARCH") && !conditionType.equals("CONDITION")) {
                errors.add(new ValidationError("?", VantarKey.SEARCH_PARAM_COL_MISSING));
                return null;
            }

            switch (conditionType) {
                case "CONDITION":
                    c.addCondition(getCondition((Condition) item.value));
                    break;

                case "EQUAL":
                    c.equal(item.col, item.getValue(dto));
                    break;

                case "NOT_EQUAL":
                    c.notEqual(item.col, item.getValue(dto));
                    break;

                case "LIKE":
                    c.like(item.col, (String) item.value);
                    break;

                case "NOT_LIKE":
                    c.notLike(item.col, (String) item.value);
                    break;

                case "FULL_SEARCH":
                    c.phrase((String) item.value);
                    break;

                case "IN":
                    Class<?> dataType = item.getDataType(dto);
                    if (CollectionUtil.isCollectionAndMap(dataType)) {
                        dataType = item.getGenericDataTypes(dto)[0];
                    }
                    if (dataType == null) {
                        dataType = item.getValueItemDataTypes();
                    }

                    if (String.class.equals(dataType) || dataType.isEnum()) {
                        c.in(item.col, item.getValuesAsString());
                    } else if (Integer.class.equals(dataType)) {
                        c.in(item.col, item.getValuesAsInteger());
                    } else if (Long.class.equals(dataType)) {
                        c.in(item.col, item.getValuesAsLong());
                    } else if (Double.class.equals(dataType)) {
                        c.in(item.col, item.getValuesAsDouble());
                    } else if (DateTime.class.equals(dataType)) {
                        c.in(item.col, item.getValuesAsDateTime());
                    } else if (Character.class.equals(dataType)) {
                        c.in(item.col, item.getValuesAsCharacter());
                    } else {
                        c.in(item.col, item.getValuesAsString());
                    }
                    break;

                case "NOT_IN":
                    Class<?> dataTypeB = item.getDataType(dto);
                    if (CollectionUtil.isCollectionAndMap(dataTypeB)) {
                        dataTypeB = item.getGenericDataTypes(dto)[0];
                    }
                    if (dataTypeB == null) {
                        dataTypeB = item.getValueItemDataTypes();
                    }

                    if (String.class.equals(dataTypeB) || dataTypeB.isEnum()) {
                        c.notIn(item.col, item.getValuesAsString());
                    } else if (Integer.class.equals(dataTypeB)) {
                        c.notIn(item.col, item.getValuesAsInteger());
                    } else if (Long.class.equals(dataTypeB)) {
                        c.notIn(item.col, item.getValuesAsLong());
                    } else if (Double.class.equals(dataTypeB)) {
                        c.notIn(item.col, item.getValuesAsDouble());
                    } else if (DateTime.class.equals(dataTypeB)) {
                        c.notIn(item.col, item.getValuesAsDateTime());
                    } else if (Character.class.equals(dataTypeB)) {
                        c.notIn(item.col, item.getValuesAsCharacter());
                    } else {
                        c.notIn(item.col, item.getValuesAsString());
                    }
                    break;

                case "BETWEEN":
                    Class<?> dataTypeC = item.getDataType(dto);
                    if (CollectionUtil.isCollectionAndMap(dataTypeC)) {
                        dataTypeC = item.getGenericDataTypes(dto)[0];
                    }
                    if (dataTypeC == null) {
                        dataTypeC = item.getValueItemDataTypes();
                    }

                    if (Integer.class.equals(dataTypeC)) {
                        c.between(item.col, item.getValuesAsInteger());
                    } else if (Long.class.equals(dataTypeC)) {
                        c.between(item.col, item.getValuesAsLong());
                    } else if (Double.class.equals(dataTypeC)) {
                        c.between(item.col, item.getValuesAsDouble());
                    } else if (DateTime.class.equals(dataTypeC)) {
                        c.between(item.col, item.getValuesAsDateTime());
                    }
                    break;
                case "NOT_BETWEEN":
                    Class<?> dataTypeD = item.getDataType(dto);
                    if (CollectionUtil.isCollectionAndMap(dataTypeD)) {
                        dataTypeD = item.getGenericDataTypes(dto)[0];
                    }
                    if (dataTypeD == null) {
                        dataTypeD = item.getValueItemDataTypes();
                    }

                    if (Integer.class.equals(dataTypeD)) {
                        c.notBetween(item.col, item.getValuesAsInteger());
                    } else if (Long.class.equals(dataTypeD)) {
                        c.notBetween(item.col, item.getValuesAsLong());
                    } else if (Double.class.equals(dataTypeD)) {
                        c.notBetween(item.col, item.getValuesAsDouble());
                    } else if (DateTime.class.equals(dataTypeD)) {
                        c.notBetween(item.col, item.getValuesAsDateTime());
                    }
                    break;
//todo: support all types
                case "LESS_THAN":
                    c.lessThan(item.col, (Number) item.value);
                    break;

                case "GREATER_THAN":
                    c.greaterThan(item.col, (Number) item.value);
                    break;

                case "LESS_THAN_EQUAL":
                    c.lessThanEqual(item.col, (Number) item.value);
                    break;

                case "GREATER_THAN_EQUAL":
                    c.greaterThanEqual(item.col, (Number) item.value);
                    break;

                case "IS_NULL":
                    c.isNull(item.col);
                    break;

                case "IS_NOT_NULL":
                    c.isNotNull(item.col);
                    break;

                case "IS_EMPTY":
                    c.isEmpty(item.col);
                    break;

                case "IS_NOT_EMPTY":
                    c.isNotEmpty(item.col);
                    break;

                case "NEAR":
                    c.near(
                        item.col,
                        new Location(NumberUtil.toDouble(item.values[0]), NumberUtil.toDouble(item.values[1])),
                        NumberUtil.toDouble(item.values[2])
                    );
                    break;

                case "FAR":
                    c.far(
                        item.col,
                        new Location(NumberUtil.toDouble(item.values[0]), NumberUtil.toDouble(item.values[1])),
                        NumberUtil.toDouble(item.values[2])
                    );
                    break;

                case "WITHIN":
                    List<Location> polygon = new ArrayList<>();
                    for (int i = 0, valuesLength = item.values.length; i < valuesLength; i++) {
                        polygon.add(new Location(NumberUtil.toDouble(item.values[i]), NumberUtil.toDouble(item.values[++i])));
                    }
                    c.within(item.col, polygon);
                    break;
            }
        }

        return c;
    }


    public static class Condition {

        public String operator;
        public List<QueryData.Condition.Item> items;

        public String toString() {
            return ObjectUtil.toString(this);
        }

        public boolean isEmpty () {
            return items == null || items.isEmpty();
        }


        public static class Item {

            public String col;
            public String type;
            public Object value;
            public Object[] values;
            public QueryData.Condition condition;

            public Class<?> getDataType(Dto dto) {
                Class<?> typeClass = dto.getPropertyType(col);
                if (typeClass != null) {
                    return typeClass;
                }

//                String[] parts = StringUtil.split(col, ".");
//                if (parts.length == 1) {
//                    return null;
//                }
//
//                Dto dtoX = dto;
//                for (String p : parts) {
//                    Class<?> t = dtoX.getPropertyType(p);
//                    log.error(">>>>{}>>>>{}",t);
//                    if (t == null) {
//                        return null;
//                    }
//
//                    for (Class<?> inter : t.getInterfaces()) {
//                        if (inter == Dto.class) {
//                            dtoX = (Dto) v;
//                            continue;
//                        }
//                    }
//
//                    if (CollectionUtil.isCollection(v)) {
//                        return dto.getPropertyGenericType(col)[0];
//                    }
//                    return v.getClass();
//                }

                return null;
            }

            public Class<?>[] getGenericDataTypes(Dto dto) {
                return dto.getPropertyGenericTypes(col);
            }

            public Class<?> getValueItemDataTypes() {
                if (values != null && values.length > 0) {
                    return values[0].getClass();
                }
                return null;
            }

            public Object getValue(Dto dto) {
                if (dto.getPropertyType(col) == DateTime.class) {
                    try {
                        return new DateTime(value.toString());
                    } catch (DateTimeException e) {
                        //errors.add(new ValidationError(col, VantarKey.INVALID_DATE));
                    }
                }
                return value;
            }

            public String toString() {
                return ObjectUtil.toString(this);
            }

            public String[] getValuesAsString() {
                String[] items = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    items[i] = values[i].toString();
                }
                return items;
            }

            public Integer[] getValuesAsInteger() {
                Integer[] items = new Integer[values.length];
                for (int i = 0; i < values.length; i++) {
                    items[i] = (Integer) NumberUtil.toNumber(values[i].toString(), Integer.class);
                }
                return items;
            }

            public Long[] getValuesAsLong() {
                Long[] items = new Long[values.length];
                for (int i = 0; i < values.length; i++) {
                    items[i] = NumberUtil.toNumber(values[i].toString(), Long.class);
                }
                return items;
            }

            public Double[] getValuesAsDouble() {
                Double[] items = new Double[values.length];
                for (int i = 0; i < values.length; i++) {
                    items[i] = NumberUtil.toDouble(values[i].toString());
                }
                return items;
            }

            public DateTime[] getValuesAsDateTime() {
                DateTime[] items = new DateTime[values.length];
                for (int i = 0; i < values.length; i++) {
                    try {
                        items[i] = new DateTime(values[i].toString());
                    } catch (DateTimeException e) {
                        return null;
                    }
                }
                return items;
            }

            public Character[] getValuesAsCharacter() {
                Character[] items = new Character[values.length];
                for (int i = 0; i < values.length; i++) {
                    items[i] = ObjectUtil.toCharacter(values[i].toString());
                }
                return items;
            }
        }
    }


    public static class JoinDef {

        public String dtoLeft;
        public String dtoRight;
        public String joinType;
        public String keyLeft;
        public String keyRight;
        public String as;
    }


    public static class GroupDef {

        public String column;
        public String columnAs;
        public String groupType;
    }
}
