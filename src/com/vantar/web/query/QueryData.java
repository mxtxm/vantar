package com.vantar.web.query;

import com.vantar.common.VantarParam;
import com.vantar.database.datatype.Location;
import com.vantar.database.dto.*;
import com.vantar.database.query.*;
import com.vantar.exception.DateTimeException;
import com.vantar.locale.VantarKey;
import com.vantar.util.bool.BoolUtil;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Database query Data structure
 * * not intended to be set manually, this represents a JSON to command a database search/filter/query
 * * used by Vantar to adapt/extract query data from input/JSON to be used by QueryBuilder
 */
public class QueryData {

    private static final Logger log = LoggerFactory.getLogger(QueryBuilder.class);

    public String dto;
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
    public List<JoinDef> joins;
    public List<GroupDef> group;
    private Boolean updateMany;

    private Dto dtObject;
    private final QueryBuilder q = new QueryBuilder();


    public QueryBuilder getQueryBuilder(Dto dtObjectX) {
        q.setUpdateMany(BoolUtil.isTrue(updateMany));

        if (dtObjectX == null) {
            dtObject = DtoDictionary.getInstance(dto);
            if (dtObject == null) {
                q.addError("dto", VantarKey.REQUIRED);
                return null;
            }
        } else {
            dtObject = dtObjectX;
        }
        q.setDto(dtObject);

        if (sort != null) {
            q.sort(sort);
        }

        if (BoolUtil.isTrue(pagination)) {
            q.setPagination();
            q.page(
                page == null ? VantarParam.PAGINATION_DEFAULT_PAGE : page,
                length == null ? VantarParam.PAGINATION_DEFAULT_SIZE : length
            );
            if (total != null) {
                q.setTotal(total);
            }
        } else if (page != null && length != null) {
            q.page(page, length);
        } else {
            if (limit != null) {
                q.limit(limit);
            }
            if (offset != null) {
                q.skip(offset);
            }
        }

        if (condition != null && ObjectUtil.isNotEmpty(condition.items)) {
            q.setCondition(getCondition(condition, null));
        }

        if (conditionGroup != null && ObjectUtil.isNotEmpty(conditionGroup.items)) {
            q.setGroupCondition(getCondition(conditionGroup, null));
        }

        if (group != null) {
            setGroups(q);
        }

        if (joins != null) {
            setJoins(q);
        }

        return q;
    }

    public String toString() {
        return ObjectUtil.toStringViewable(this);
    }

    @SuppressWarnings("unchecked")
    private QueryCondition getCondition(Condition condition, String mainCol) {
        if (condition == null) {
            return null;
        }
        if (dtObject == null) {
            log.error("! dto not set, condition not created > {}\n", this.toString());
            return null;
        }

        Dto dtoU = DtoDictionary.getInstance((Class<? extends Dto>) dtObject.getClass().getDeclaringClass());
        Dto dtObjectBase = dtoU == null ? dtObject : dtoU;

        QueryCondition queryCondition = new QueryCondition(condition.getOperator());
        for (ConditionItem item : condition.items) {
            if (StringUtil.isEmpty(item.type)) {
                q.addError(item.col, VantarKey.SEARCH_CONDITION_TYPE_INVALID);
                return null;
            }
            String conditionType = item.type.toUpperCase();

            if (item.col == null
                && !conditionType.equals("FULL_SEARCH")
                && !conditionType.equals("CONDITION")
                && !conditionType.equals("EXISTS")) {
                q.addError("?", VantarKey.SEARCH_COL_MISSING);
                return null;
            }
            item.separateCols();

            Class<?> dataType = item.getValueDataType(getLastTraversedPropertyType(
                dtObjectBase,
                mainCol == null ? item.col : (mainCol + '.' + item.col)
            ));

            switch (conditionType) {
                case "CONDITION": {
                    QueryCondition cond = ObjectUtil.isEmpty(item.condition) ? null : getCondition(item.condition, null);
                    if (cond == null) {
                        q.addError("CONDITION", VantarKey.SEARCH_VALUE_MISSING);
                        continue;
                    }
                    queryCondition.addCondition(cond);
                    break;
                }
                case "EXISTS": {
                    QueryCondition cond = ObjectUtil.isEmpty(item.condition) ? null : getCondition(item.condition, null);
                    if (cond == null) {
                        q.addError("EXISTS", VantarKey.SEARCH_VALUE_MISSING);
                        continue;
                    }
                    queryCondition.exists(cond);
                    break;
                }

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

                case "EQUAL":
                    queryCondition.equal(item.col, getValue(item, dataType));
                    break;
                case "NOT_EQUAL":
                    queryCondition.notEqual(item.col, getValue(item, dataType));
                    break;
                case "LIKE":
                    queryCondition.like(item.col, (String) getValue(item, String.class));
                    break;
                case "NOT_LIKE":
                    queryCondition.notLike(item.col, (String) getValue(item, String.class));
                    break;
                case "FULL_SEARCH":
                    queryCondition.phrase((String) getValue(item, String.class));
                    break;

                case "IN":
                    if (String.class.equals(dataType) || dataType.isEnum()) {
                        queryCondition.in(item.col, getValuesAsString(item));
                    } else if (ClassUtil.isInstantiable(dataType, Number.class)) {
                        queryCondition.in(item.col, getValuesAsNumber(item, dataType));
                    } else if (DateTime.class.equals(dataType)) {
                        queryCondition.in(item.col, getValuesAsDateTime(item));
                    } else if (Character.class.equals(dataType)) {
                        queryCondition.in(item.col, getValuesAsCharacter(item));
                    } else {
                        queryCondition.in(item.col, getValuesAsString(item));
                    }
                    break;
                case "NOT_IN":
                    if (String.class.equals(dataType) || dataType.isEnum()) {
                        queryCondition.notIn(item.col, getValuesAsString(item));
                    } else if (ClassUtil.isInstantiable(dataType, Number.class)) {
                        queryCondition.notIn(item.col, getValuesAsNumber(item, dataType));
                    } else if (DateTime.class.equals(dataType)) {
                        queryCondition.notIn(item.col, getValuesAsDateTime(item));
                    } else if (Character.class.equals(dataType)) {
                        queryCondition.notIn(item.col, getValuesAsCharacter(item));
                    } else {
                        queryCondition.notIn(item.col, getValuesAsString(item));
                    }
                    break;

                case "BETWEEN":
                    if (item.colB == null) {
                        if (ClassUtil.isInstantiable(dataType, Number.class)) {
                            queryCondition.between(item.col, getValuesAsNumber(item, dataType));
                        } else if (DateTime.class.equals(dataType)) {
                            queryCondition.between(item.col, getValuesAsDateTime(item));
                        } else {
                            q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                        }
                    } else {
                        if (ClassUtil.isInstantiable(dataType, Number.class)) {
                            queryCondition.between(item.col, item.colB, (Number) getValue(item, Number.class));
                        } else if (DateTime.class.equals(dataType)) {
                            queryCondition.between(item.col, item.colB, (DateTime) getValue(item, DateTime.class));
                        } else {
                            q.addError(item.col + ":" + item.colB, VantarKey.SEARCH_VALUE_INVALID);
                        }
                    }
                    break;
                case "NOT_BETWEEN":
                    if (item.colB == null) {
                        if (ClassUtil.isInstantiable(dataType, Number.class)) {
                            queryCondition.notBetween(item.col, getValuesAsNumber(item, dataType));
                        } else if (DateTime.class.equals(dataType)) {
                            queryCondition.notBetween(item.col, getValuesAsDateTime(item));
                        } else {
                            q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                        }
                    } else {
                        if (ClassUtil.isInstantiable(dataType, Number.class)) {
                            queryCondition.notBetween(item.col, item.colB, (Number) getValue(item, Number.class));
                        } else if (DateTime.class.equals(dataType)) {
                            queryCondition.notBetween(item.col, item.colB, (DateTime) getValue(item, DateTime.class));
                        } else {
                            q.addError(item.col + ":" + item.colB, VantarKey.SEARCH_VALUE_INVALID);
                        }
                    }
                    break;

                case "LESS_THAN":
                    if (ClassUtil.isInstantiable(dataType, Number.class)) {
                        queryCondition.lessThan(item.col, (Number) getValue(item, dataType));
                    } else if (DateTime.class.equals(dataType)) {
                        queryCondition.lessThan(item.col, (DateTime) getValue(item, DateTime.class));
                    } else {
                        q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                    }
                    break;
                case "GREATER_THAN":
                    if (ClassUtil.isInstantiable(dataType, Number.class)) {
                        queryCondition.greaterThan(item.col, (Number) getValue(item, dataType));
                    } else if (DateTime.class.equals(dataType)) {
                        queryCondition.greaterThan(item.col, (DateTime) getValue(item, DateTime.class));
                    } else {
                        q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                    }
                    break;
                case "LESS_THAN_EQUAL":
                    if (ClassUtil.isInstantiable(dataType, Number.class)) {
                        queryCondition.lessThanEqual(item.col, (Number) getValue(item, dataType));
                    } else if (DateTime.class.equals(dataType)) {
                        queryCondition.lessThanEqual(item.col, (DateTime) getValue(item, DateTime.class));
                    } else {
                        q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                    }
                    break;
                case "GREATER_THAN_EQUAL":
                    if (ClassUtil.isInstantiable(dataType, Number.class)) {
                        queryCondition.greaterThanEqual(item.col, (Number) getValue(item, dataType));
                    } else if (DateTime.class.equals(dataType)) {
                        queryCondition.greaterThanEqual(item.col, (DateTime) getValue(item, DateTime.class));
                    } else {
                        q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                    }
                    break;

                case "CONTAINS_ALL":
                    if (ObjectUtil.isNotEmpty(item.objects)) {
                        queryCondition.containsAll(item.col, item.objects);
                    } else if (ObjectUtil.isNotEmpty(item.values)) {
                        queryCondition.containsAll(item.col, Arrays.asList(item.values));
                    } else {
                        q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                    }
                    break;

                case "NEAR": {
                    if (item.values.length < 3 || item.values.length > 4) {
                        q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                        break;
                    }
                    Location location = new Location(NumberUtil.toDouble(item.values[0]), NumberUtil.toDouble(item.values[1]));
                    Double maxDistance = NumberUtil.toDouble(item.values[2]);
                    if (!location.isValid() || maxDistance == null) {
                        q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                        break;
                    }
                    if (item.values.length == 3) {
                        queryCondition.near(item.col, location, maxDistance);
                    } else {
                        Double minDistance = NumberUtil.toDouble(item.values[3]);
                        if (minDistance == null) {
                            q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                            break;
                        }
                        queryCondition.near(item.col, location, maxDistance, minDistance);
                    }
                    break;
                }
                case "FAR": {
                    if (item.values.length != 3) {
                        q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                        break;
                    }
                    Location location = new Location(NumberUtil.toDouble(item.values[0]), NumberUtil.toDouble(item.values[1]));
                    Double minDistance = NumberUtil.toDouble(item.values[2]);
                    if (!location.isValid() || minDistance == null) {
                        q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                        break;
                    }
                    queryCondition.far(item.col, location, minDistance);
                    break;
                }
                case "WITHIN":
                    int l = item.values.length;
                    if (l < 6 || l % 2 != 0) {
                        q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                        break;
                    }
                    List<Location> polygon = new ArrayList<>(16);
                    for (int i = 0; i < l; ++i) {
                        polygon.add(new Location(NumberUtil.toDouble(item.values[i]), NumberUtil.toDouble(item.values[++i])));
                    }
                    queryCondition.within(item.col, polygon);
                    break;

                case "MAP_KEY_EXISTS":
                    queryCondition.mapKeyExists(item.col, getValue(item, String.class));
                    break;

                case "IN_LIST": {
                    QueryCondition cond = ObjectUtil.isEmpty(item.condition) ? null : getCondition(item.condition, item.col);
                    if (cond == null) {
                        q.addError("IN_LIST", VantarKey.SEARCH_VALUE_MISSING);
                        continue;
                    }
                    queryCondition.inList(item.col, cond);
                    break;
                }

                case "IN_DTO": {
                    Dto dtObjectTemp = dtObject;
                    dtObject = item.getDto();
                    QueryCondition cond = ObjectUtil.isEmpty(item.condition) ? null : getCondition(item.condition, null);
                    dtObject = dtObjectTemp;
                    if (cond == null) {
                        q.addError("condition", VantarKey.SEARCH_VALUE_MISSING);
                        continue;
                    }
                    queryCondition.inDto(item.col, item.getDto(), cond);
                    break;
                }

                default:
                    q.addError(item.col, VantarKey.SEARCH_CONDITION_TYPE_INVALID);
            }
        }

        return queryCondition;
    }

    private Object getValue(ConditionItem item, Class<?> dataType) {
        Object v = ObjectUtil.convert(item.value, dataType);
        if (v == null) {
            q.addError(item.col, VantarKey.SEARCH_VALUE_MISSING);
            return null;
        }
        return v;
    }

    private String[] getValuesAsString(ConditionItem item) {
        if (item.values == null) {
            q.addError(item.col, VantarKey.SEARCH_VALUE_MISSING);
            return null;
        }
        String[] items = new String[item.values.length];
        for (int i = 0, l = item.values.length; i < l; ++i) {
            items[i] = ObjectUtil.toString(item.values[i]);
        }
        return items;
    }

    private Number[] getValuesAsNumber(ConditionItem item, Class<?> dataType) {
        if (item.values == null) {
            q.addError(item.col, VantarKey.SEARCH_VALUE_MISSING);
            return null;
        }
        Number[] items = new Number[item.values.length];
        for (int i = 0, l = item.values.length; i < l; ++i) {
            Number n;
            if (ClassUtil.isInstantiable(dataType, Integer.class)) {
                n = NumberUtil.toInteger(item.values[i].toString());
            } else if (ClassUtil.isInstantiable(dataType, Long.class)) {
                n = NumberUtil.toLong(item.values[i].toString());
            } else if (ClassUtil.isInstantiable(dataType, Float.class)) {
                n = NumberUtil.toFloat(item.values[i].toString());
            } else {
                n = NumberUtil.toDouble(item.values[i].toString());
            }
            if (n == null) {
                q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                return null;
            }
            items[i] = n;
        }
        return items;
    }

    private DateTime[] getValuesAsDateTime(ConditionItem item) {
        if (item.values == null) {
            q.addError(item.col, VantarKey.SEARCH_VALUE_MISSING);
            return null;
        }
        DateTime[] items = new DateTime[item.values.length];
        for (int i = 0, l = item.values.length; i < l; ++i) {
            try {
                items[i] = new DateTime(item.values[i].toString());
            } catch (DateTimeException e) {
                q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                return null;
            }
        }
        return items;
    }

    private Character[] getValuesAsCharacter(ConditionItem item) {
        if (item.values == null) {
            q.addError(item.col, VantarKey.SEARCH_VALUE_MISSING);
            return null;
        }
        Character[] items = new Character[item.values.length];
        for (int i = 0, l = item.values.length; i < l; ++i) {
            Character c = StringUtil.toCharacter(item.values[i].toString());
            if (c == null) {
                q.addError(item.col, VantarKey.SEARCH_VALUE_INVALID);
                return null;
            }
            items[i] = c;
        }
        return items;
    }

    private void setGroups(QueryBuilder q) {
        for (GroupDef groupDef : group) {
            if (StringUtil.isEmpty(groupDef.column)) {
                q.addError("GROUP.column", VantarKey.SEARCH_VALUE_MISSING);
                continue;
            }
            if (groupDef.groupType != null) {
                QueryGroupType type;
                try {
                    type = QueryGroupType.valueOf(groupDef.groupType);
                } catch (IllegalArgumentException e) {
                    q.addError("GROUP.groupType=" + groupDef.groupType, VantarKey.SEARCH_VALUE_INVALID);
                    continue;
                }
                if (StringUtil.isEmpty(groupDef.columnAs)) {
                    q.addGroup(type, groupDef.column);
                    continue;
                }
                q.addGroup(type, groupDef.column, groupDef.columnAs);
                continue;
            }

            if (StringUtil.isEmpty(groupDef.columnAs)) {
                q.addGroup(groupDef.column, groupDef.columnAs);
                continue;
            }
            q.addGroup(groupDef.column);
        }
    }

    private void setJoins(QueryBuilder q) {
        for (JoinDef joinDef : joins) {
            if (StringUtil.isEmpty(joinDef.joinType)) {
                q.addError("JOIN.joinType", VantarKey.SEARCH_VALUE_MISSING);
                continue;
            }

            Dto dtoRight = DtoDictionary.getInstance(joinDef.dtoRight);
            if (dtoRight == null) {
                q.addError("JOIN.dtoRight", VantarKey.SEARCH_VALUE_MISSING);
                continue;
            }

            if (StringUtil.isNotEmpty(joinDef.dtoLeft)) {
                Dto dtoLeft = DtoDictionary.getInstance(joinDef.dtoLeft);
                if (dtoLeft == null) {
                    q.addError("JOIN.dtoLeft", VantarKey.SEARCH_VALUE_MISSING);
                    continue;
                }
                q.addJoin(joinDef.joinType, dtoLeft, dtoRight);
                continue;
            }

            if (StringUtil.isNotEmpty(joinDef.keyLeft) && StringUtil.isNotEmpty(joinDef.keyRight)) {
                if (StringUtil.isNotEmpty(joinDef.as)) {
                    q.addJoin(joinDef.joinType, dtoRight, joinDef.as, joinDef.keyLeft, joinDef.keyRight);
                    continue;
                }
                q.addJoin(joinDef.joinType, dtoRight, joinDef.as, joinDef.keyLeft, joinDef.keyRight);
                continue;
            }

            if (StringUtil.isEmpty(joinDef.keyLeft) && StringUtil.isEmpty(joinDef.keyRight) && StringUtil.isEmpty(joinDef.as)) {
                q.addJoin(joinDef.joinType, dtoRight);
            }
        }
    }

    /**
     * List<String>            --> String
     * List<Set<String>>       --> String
     * List<Map<String, Long>> --> Long
     */
    @SuppressWarnings("unchecked")
    private static Class<?> getLastTraversedPropertyType(Dto dto, String traversable) {
        if (traversable == null) {
            return null;
        }

        if (!StringUtil.contains(traversable, '.')) {
            Field field = dto.getField(traversable);
            if (field == null) {
                return null;
            }
            Class<?> type = field.getType();
            if (CollectionUtil.isCollectionOrMap(type)) {
                Class<?>[] generics = dto.getPropertyGenericTypes(field);
                if (generics == null || generics.length == 0) {
                    return null;
                }
                return generics[generics.length - 1];
            }
            return type;
        }

        Field field;
        Class<?> type = null;
        Dto dtoX = dto;
        for (String propertyName : StringUtil.split(traversable, '.')) {
            field = dtoX.getField(propertyName);
            if (field == null) {
                return null;
            }
            type = field.getType();

            if (ClassUtil.isInstantiable(type, Dto.class)) {
                dtoX = DtoDictionary.getInstance((Class<? extends Dto>) type);
                if (dtoX == null) {
                    return null;
                }
                continue;
            }

            if (type == Location.class) {
                return Double.class;
            }

            if (CollectionUtil.isCollectionOrMap(type)) {
                Class<?>[] generics = dto.getPropertyGenericTypes(field);
                if (generics == null || generics.length == 0) {
                    return null;
                }
                type = generics[generics.length - 1];
                if (ClassUtil.isInstantiable(type, Dto.class)) {
                    dtoX = DtoDictionary.getInstance((Class<? extends Dto>) type);
                    if (dtoX == null) {
                        return null;
                    }
                } else {
                    return type;
                }
            }
        }
        return type;
    }
}