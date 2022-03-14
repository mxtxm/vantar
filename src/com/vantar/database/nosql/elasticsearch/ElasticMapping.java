package com.vantar.database.nosql.elasticsearch;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.query.*;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.string.StringUtil;
import org.elasticsearch.index.query.*;
import java.util.*;
import static com.vantar.database.query.QueryOperator.BETWEEN;
import static com.vantar.database.query.QueryOperator.QUERY;


class ElasticMapping {

    public static Map<String, Object> getDataForWrite(Dto dto) {
        Map<String, Object> data = StorableData.toMap(dto.getStorableData());

        data.forEach((name, value) -> {
            if (value instanceof DateTime) {
                DateTime dateTime = (DateTime) value;
                long timestamp;
                if (DateTime.DATE.equals(dateTime.getType())) {
                    timestamp = dateTime.getDateAsTimestamp();
                } else {
                    timestamp = dateTime.getAsTimestamp();
                }
                data.put(name, timestamp);
            } else if (value instanceof List) {
                DataType dataType = dto.getField(name).getAnnotation(DataType.class);
                if (dataType != null) {
                    String[] fieldsToAdd = StringUtil.split(StringUtil.remove(dataType.value(), "list>>"), VantarParam.SEPARATOR_COMMON);
                    List<Map<String, Object>> listValues = new ArrayList<>();

                    List<?> list = (List<?>) value;
                    if (list.size() == 0 || !(list.get(0) instanceof Dto)) {
                        return;
                    }

                    for (Dto dtoL : (List<Dto>) value) {
                        Map<String, Object> fields = new HashMap<>();
                        for (String item : fieldsToAdd) {
                            String fieldName = StringUtil.split(item, VantarParam.SEPARATOR_KEY_VAL)[0];
                            fields.put(fieldName, dtoL.getPropertyValue(fieldName));
                        }
                        listValues.add(fields);
                    }

                    data.put(name, listValues);
                }
            }
        });

        return data;
    }

    protected static BoolQueryBuilder getElasticQuery(QueryCondition condition, Dto dto) {
        if (condition == null || condition.q.size() == 0) {
            return null;
        }

        BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();

        for (QueryMatchItem item : condition.q) {
            if (item.type == QUERY) {
                BoolQueryBuilder qInner = getElasticQuery(item.queryValue, dto);
                if (condition.operator == QueryOperator.OR) {
                    mainQuery.should(qInner);
                } else if (condition.operator == QueryOperator.AND) {
                    mainQuery.must(qInner);
                } else if (condition.operator == QueryOperator.NOT) {
                    mainQuery.mustNot(qInner);
                }
                continue;
            }

            String fieldName = StringUtil.toSnakeCase(item.fieldName);
            switch (item.type) {
                case EQUAL:
                    TermQueryBuilder tQuery = QueryBuilders.termQuery(fieldName, item.getValue());
                    if (condition.operator == QueryOperator.OR) {
                        mainQuery.should(tQuery);
                    } else if (condition.operator == QueryOperator.AND) {
                        mainQuery.must(tQuery);
                    } else if (condition.operator == QueryOperator.NOT) {
                        mainQuery.mustNot(tQuery);
                    }
                    break;
                case NOT_EQUAL:
                    mainQuery.mustNot(QueryBuilders.termQuery(fieldName, item.getValue()));
                    break;

                case LIKE:
                    QueryStringQueryBuilder qLike = QueryBuilders.queryStringQuery(item.getValue().toString());
                    qLike.defaultField(fieldName);
                    if (condition.operator == QueryOperator.OR) {
                        mainQuery.should(qLike);
                    } else if (condition.operator == QueryOperator.AND) {
                        mainQuery.must(qLike);
                    } else if (condition.operator == QueryOperator.NOT) {
                        mainQuery.mustNot(qLike);
                    }
                    break;
                case NOT_LIKE:
                    mainQuery.mustNot(QueryBuilders.queryStringQuery(item.getValue().toString()));
                    break;

                case FULL_SEARCH:
                    QueryStringQueryBuilder qFull = QueryBuilders.queryStringQuery(item.getValue().toString());
                    Map<String, Float> fields = new HashMap<>();
                    dto.getPropertyTypes().forEach((name, tClass) -> {
                        if (tClass.equals(String.class)) {
                            fields.put(name, 1F);
                        }
                    });
                    qFull.fields(fields);
                    if (condition.operator == QueryOperator.OR) {
                        mainQuery.should(qFull);
                    } else if (condition.operator == QueryOperator.AND) {
                        mainQuery.must(qFull);
                    } else if (condition.operator == QueryOperator.NOT) {
                        mainQuery.mustNot(qFull);
                    }
                    break;

                case IN:
                    TermsQueryBuilder qIn = QueryBuilders.termsQuery(fieldName, item.getValues());
                    if (condition.operator == QueryOperator.OR) {
                        mainQuery.should(qIn);
                    } else if (condition.operator == QueryOperator.AND) {
                        mainQuery.must(qIn);
                    } else if (condition.operator == QueryOperator.NOT) {
                        mainQuery.mustNot(qIn);
                    }
                    break;
                case NOT_IN:
                    mainQuery.mustNot(QueryBuilders.termsQuery(fieldName, item.getValues()));
                    break;

                case BETWEEN:
                case NOT_BETWEEN:
                    Object[] values = item.getValues();
                    if (item.type == BETWEEN) {
                        RangeQueryBuilder qBetween = QueryBuilders.rangeQuery(fieldName).gte(values[0]).lte(values[1]);
                        if (condition.operator == QueryOperator.OR) {
                            mainQuery.should(qBetween);
                        } else if (condition.operator == QueryOperator.AND) {
                            mainQuery.must(qBetween);
                        } else if (condition.operator == QueryOperator.NOT) {
                            mainQuery.mustNot(qBetween);
                        }
                    } else {
                        mainQuery.mustNot(QueryBuilders.rangeQuery(fieldName).gte(values[0]).lte(values[1]));
                    }
                    break;

                case LESS_THAN:
                case GREATER_THAN:
                case LESS_THAN_EQUAL:
                case GREATER_THAN_EQUAL:
                    RangeQueryBuilder qRange;
                    switch (item.type) {
                        case LESS_THAN:
                            qRange = QueryBuilders.rangeQuery(fieldName).lt(item.getValue());
                            break;
                        case GREATER_THAN:
                            qRange = QueryBuilders.rangeQuery(fieldName).gt(item.getValue());
                            break;
                        case LESS_THAN_EQUAL:
                            qRange = QueryBuilders.rangeQuery(fieldName).lte(item.getValue());
                            break;
                        case GREATER_THAN_EQUAL:
                            qRange = QueryBuilders.rangeQuery(fieldName).gte(item.getValue());
                            break;
                        default:
                            continue;
                    }
                    if (condition.operator == QueryOperator.OR) {
                        mainQuery.should(qRange);
                    } else if (condition.operator == QueryOperator.AND) {
                        mainQuery.must(qRange);
                    } else if (condition.operator == QueryOperator.NOT) {
                        mainQuery.mustNot(qRange);
                    }
                    break;

                case IS_NULL:
                case IS_EMPTY:
                    TermQueryBuilder eQuery = QueryBuilders.termQuery(fieldName, "");
                    if (condition.operator == QueryOperator.OR) {
                        mainQuery.should(eQuery);
                    } else if (condition.operator == QueryOperator.AND) {
                        mainQuery.must(eQuery);
                    } else if (condition.operator == QueryOperator.NOT) {
                        mainQuery.mustNot(eQuery);
                    }
                    break;
                case IS_NOT_EMPTY:
                case IS_NOT_NULL:
                    mainQuery.mustNot(QueryBuilders.termQuery(fieldName, ""));
                    break;
            }
        }

        return mainQuery;
    }
}
