package com.vantar.database.query.data;

import com.vantar.database.common.ValidationError;
import com.vantar.database.datatype.Location;
import com.vantar.database.dto.*;
import com.vantar.database.query.*;
import com.vantar.locale.VantarKey;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import java.util.*;

/**
 * Database query Data structure
 * * not intended to be set manually, this represents a JSON to command a database search/filter/query
 * * used by Vantar to adapt/extract query data from input/JSON to be used by QueryBuilder
 */
public class QueryData {

    private final List<ValidationError> errors = new ArrayList<>(10);

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
    public Condition condition;
    public Condition conditionGroup;
    public List<String> columns;
    public List<JoinDef> joins;
    public List<GroupDef> group;

    private Dto xDto;
    private Dto xDtoResult;


    public QueryData setDto(Dto dto) {
        xDto = dto;
        xDtoResult = dto;
        return this;
    }

    public QueryData setDto(Dto dto, Dto dtoResult) {
        xDto = dto;
        xDtoResult = dtoResult;
        return this;
    }

    public Dto getDto() {
        if (xDto == null) {
            DtoDictionary.Info info = DtoDictionary.get(dto);
            if (info == null) {
                errors.add(new ValidationError("dto", VantarKey.REQUIRED));
                return null;
            }
            xDto = info.getDtoInstance();
        }
        return xDto;
    }

    public Dto getDtoResult() {
        if (xDtoResult == null && dtoResult == null) {
            dtoResult = dto;
        }
        if (xDtoResult == null) {
            DtoDictionary.Info info = DtoDictionary.get(dtoResult);
            if (info == null) {
                errors.add(new ValidationError("dtoResult", VantarKey.REQUIRED));
                return null;
            }
            xDtoResult = info.getDtoInstance();
        }
        return xDtoResult;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public boolean isPagination() {
        return pagination != null && pagination;
    }

    public QueryCondition getCondition() {
        return getCondition(condition);
    }

    public QueryCondition getGroupCondition() {
        return getCondition(conditionGroup);
    }

    private QueryCondition getCondition(Condition condition) {
        if (condition == null) {
            return null;
        }
        Dto dto = getDto();
        if (dto == null) {
            return null;
        }

        QueryCondition queryCondition = new QueryCondition(condition.getOperator());

        for (ConditionItem item : condition.items) {
            if (item.type == null) {
                errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_INVALID_CONDITION_TYPE));
                return null;
            }
            String conditionType = item.type.toUpperCase();

            if (item.col == null && !conditionType.equals("FULL_SEARCH") && !conditionType.equals("CONDITION")
                && !conditionType.equals("EXISTS")) {
                errors.add(new ValidationError("?", VantarKey.SEARCH_PARAM_COL_MISSING));
                return null;
            }

            item.separateCols();

            Class<?> dataType = DtoUtil.getLastTraversedPropertyType(dto, item.col);
            if (dataType == null) {
                dataType = item.getValueDataType();
            }
            if (dataType == null || dataType.isEnum()) {
                dataType = String.class;
            }

            switch (conditionType) {
                case "CONDITION":
                    if (item.condition == null) {
                        errors.add(new ValidationError("condition", VantarKey.SEARCH_PARAM_VALUE_MISSING));
                        return null;
                    }
                    queryCondition.addCondition(getCondition(item.condition));
                    break;
                case "EXISTS":
                    if (item.condition == null) {
                        errors.add(new ValidationError("condition", VantarKey.SEARCH_PARAM_VALUE_MISSING));
                        return null;
                    }
                    queryCondition.exists(getCondition(item.condition));
                    break;

                case "EQUAL":
                    try {
                        queryCondition.equal(item.col, getValue(item, dataType));
                    } catch (Exception e) {
                        return null;
                    }
                    break;
                case "NOT_EQUAL":
                    try {
                        queryCondition.notEqual(item.col, getValue(item, dataType));
                    } catch (Exception e) {
                        return null;
                    }
                    break;
                case "LIKE":
                    try {
                        queryCondition.like(item.col, (String) getValue(item, String.class));
                    } catch (Exception e) {
                        return null;
                    }
                    break;
                case "NOT_LIKE":
                    try {
                        queryCondition.notLike(item.col, (String) getValue(item, String.class));
                    } catch (Exception e) {
                        return null;
                    }
                    break;

                case "FULL_SEARCH":
                    try {
                        queryCondition.phrase((String) getValue(item, String.class));
                    } catch (Exception e) {
                        return null;
                    }
                    break;

                case "IS_NULL":
                    queryCondition.isNull(item.col);
                    break;
                case "IS_NOT_NULL":
                    queryCondition.isNotNull(item.col);
                    break;
                case "IS_EMPTY":
                    queryCondition.isEmpty(item.col);
                    break;
                case "IS_NOT_EMPTY":
                    queryCondition.isNotEmpty(item.col);
                    break;

                case "IN":
                    if (String.class.equals(dataType) || dataType.isEnum()) {
                        queryCondition.in(item.col, item.getValuesAsString());
                    } else if (ClassUtil.isInstantiable(dataType, Number.class)) {
                        queryCondition.in(item.col, item.getValuesAsNumber());
                    } else if (DateTime.class.equals(dataType)) {
                        queryCondition.in(item.col, item.getValuesAsDateTime());
                    } else if (Character.class.equals(dataType)) {
                        queryCondition.in(item.col, item.getValuesAsCharacter());
                    } else {
                        queryCondition.in(item.col, item.getValuesAsString());
                    }
                    break;
                case "NOT_IN":
                    if (String.class.equals(dataType) || dataType.isEnum()) {
                        queryCondition.notIn(item.col, item.getValuesAsString());
                    } else if (ClassUtil.isInstantiable(dataType, Number.class)) {
                        queryCondition.notIn(item.col, item.getValuesAsNumber());
                    } else if (DateTime.class.equals(dataType)) {
                        queryCondition.notIn(item.col, (DateTime) item.getValue(DateTime.class));
                    } else if (Character.class.equals(dataType)) {
                        queryCondition.notIn(item.col, item.getValuesAsCharacter());
                    } else {
                        queryCondition.notIn(item.col, item.getValuesAsString());
                    }
                    break;

                case "BETWEEN":
                    if (item.colB == null) {
                        if (ClassUtil.isInstantiable(dataType, Number.class)) {
                            queryCondition.between(item.col, item.colB, (Number) item.getValue(Number.class));
                        } else if (DateTime.class.equals(dataType)) {
                            queryCondition.between(item.col, item.colB, (DateTime) item.getValue(DateTime.class));
                        } else {
                            errors.add(new ValidationError(item.col + ":" + item.colB, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                        }
                    } else {
                        if (ClassUtil.isInstantiable(dataType, Number.class)) {
                            queryCondition.between(item.col, item.getValuesAsNumber());
                        } else if (DateTime.class.equals(dataType)) {
                            queryCondition.between(item.col, item.getValuesAsDateTime());
                        } else {
                            errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                        }
                    }
                    break;
                case "NOT_BETWEEN":
                    if (item.colB == null) {
                        if (ClassUtil.isInstantiable(dataType, Number.class)) {
                            queryCondition.notBetween(item.col, item.colB, (Number) item.getValue(Number.class));
                        } else if (DateTime.class.equals(dataType)) {
                            queryCondition.notBetween(item.col, item.colB, (DateTime) item.getValue(DateTime.class));
                        } else {
                            errors.add(new ValidationError(item.col + ":" + item.colB, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                        }
                    } else {
                        if (ClassUtil.isInstantiable(dataType, Number.class)) {
                            queryCondition.notBetween(item.col, item.getValuesAsNumber());
                        } else if (DateTime.class.equals(dataType)) {
                            queryCondition.notBetween(item.col, item.getValuesAsDateTime());
                        } else {
                            errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                        }
                    }
                    break;

                case "LESS_THAN":
                    if (ClassUtil.isInstantiable(dataType, Number.class)) {
                        queryCondition.lessThan(item.col, (Number) item.getValue(Double.class));
                    } else if (DateTime.class.equals(dataType)) {
                        queryCondition.lessThan(item.col, (DateTime) item.getValue(DateTime.class));
                    } else {
                        errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                    }
                    break;
                case "GREATER_THAN":
                    if (ClassUtil.isInstantiable(dataType, Number.class)) {
                        queryCondition.greaterThan(item.col, (Number) item.getValue(Double.class));
                    } else if (DateTime.class.equals(dataType)) {
                        queryCondition.greaterThan(item.col, (DateTime) item.getValue(DateTime.class));
                    } else {
                        errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                    }
                    break;
                case "LESS_THAN_EQUAL":
                    if (ClassUtil.isInstantiable(dataType, Number.class)) {
                        queryCondition.lessThanEqual(item.col, (Number) item.getValue(Double.class));
                    } else if (DateTime.class.equals(dataType)) {
                        queryCondition.lessThanEqual(item.col, (DateTime) item.getValue(DateTime.class));
                    } else {
                        errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                    }
                    break;
                case "GREATER_THAN_EQUAL":
                    if (ClassUtil.isInstantiable(dataType, Number.class)) {
                        queryCondition.greaterThanEqual(item.col, (Number) item.getValue(Double.class));
                    } else if (DateTime.class.equals(dataType)) {
                        queryCondition.greaterThanEqual(item.col, (DateTime) item.getValue(DateTime.class));
                    } else {
                        errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                    }
                    break;

                case "CONTAINS_ALL":
                    if (ObjectUtil.isNotEmpty(item.objects)) {
                        queryCondition.containsAll(item.col, item.objects);
                    } else if (ObjectUtil.isNotEmpty(item.values)) {
                        queryCondition.containsAll(item.col, Arrays.asList(item.values));
                    } else {
                        errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                    }
                    break;

                case "NEAR":
                    if (item.values.length < 3 || item.values.length > 4) {
                        errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                        break;
                    }
                    Location location = new Location(NumberUtil.toDouble(item.values[0]), NumberUtil.toDouble(item.values[1]));
                    Double maxDistance = NumberUtil.toDouble(item.values[2]);
                    if (!location.isValid() || maxDistance == null) {
                        errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                        break;
                    }
                    if (item.values.length == 3) {
                        queryCondition.near(item.col, location, maxDistance);
                    } else {
                        Double minDistance = NumberUtil.toDouble(item.values[3]);
                        if (minDistance == null) {
                            errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                            break;
                        }
                        queryCondition.near(item.col, location, maxDistance, minDistance);
                    }
                    break;
                case "FAR":
                    if (item.values.length != 3) {
                        errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                        break;
                    }
                    Location location2 = new Location(NumberUtil.toDouble(item.values[0]), NumberUtil.toDouble(item.values[1]));
                    Double minDistance2 = NumberUtil.toDouble(item.values[2]);
                    if (!location2.isValid() || minDistance2 == null) {
                        errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                        break;
                    }
                    queryCondition.far(item.col, location2, minDistance2);
                    break;

                case "WITHIN":
                    int l = item.values.length;
                    if (l < 6 || l % 2 != 0) {
                        errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
                        break;
                    }
                    List<Location> polygon = new ArrayList<>(16);
                    for (int i = 0; i < l; ++i) {
                        polygon.add(new Location(NumberUtil.toDouble(item.values[i]), NumberUtil.toDouble(item.values[++i])));
                    }
                    queryCondition.within(item.col, polygon);
                    break;

                default:
                    errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_INVALID_CONDITION_TYPE));
            }
        }

        return queryCondition;
    }

    private Object getValue(ConditionItem item, Class<?> dataType) throws Exception {
        Object v = item.getValue(dataType);
        if (v == null) {
            errors.add(new ValidationError(item.col, VantarKey.SEARCH_PARAM_VALUE_INVALID));
            throw new Exception();
        }
        return v;
    }

    public boolean isEmpty() {
        if (ObjectUtil.isNotEmpty(condition)) {
            return false;
        }
        if (page != null) {
            return false;
        }
        if (length != null) {
            return false;
        }
        if (offset != null) {
            return false;
        }
        if (limit != null) {
            return false;
        }
        if (ObjectUtil.isNotEmpty(sort)) {
            return false;
        }
        if (conditionGroup != null) {
            return false;
        }
        if (ObjectUtil.isNotEmpty(columns)) {
            return false;
        }
        if (ObjectUtil.isNotEmpty(group)) {
            return false;
        }
        if (ObjectUtil.isNotEmpty(joins)) {
            return false;
        }
        return all == null || !all;
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public String toString() {
        return ObjectUtil.toString(this);
    }
}
