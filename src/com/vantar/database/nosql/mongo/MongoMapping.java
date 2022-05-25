package com.vantar.database.nosql.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.geojson.*;
import com.vantar.common.VantarParam;
import com.vantar.database.datatype.Location;
import com.vantar.database.dto.Date;
import com.vantar.database.dto.*;
import com.vantar.database.query.*;
import com.vantar.exception.DatabaseException;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.*;
import java.util.regex.Pattern;
import static com.vantar.database.query.QueryOperator.AND;
import static com.vantar.database.query.QueryOperator.QUERY;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MongoMapping {

    public static Document sort(String[] sort) {
        if (sort == null || sort.length == 0) {
            return null;
        }
        Document document = new Document();
        for (String item : sort) {
            String[] parts = StringUtil.split(item, ':');
            if (parts[0].equals(DtoBase.ID)) {
                parts[0] = Mongo.ID;
            }
            document.append(
                StringUtil.toSnakeCase(parts[0]),
                parts.length == 1 ? 1 : (parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("1") ? 1 : -1)
            );
        }
        return document;
    }

    public static Document getFieldValuesAsDocument(Dto dto, Dto.Action action) {
        Document document = new Document();

        for (StorableData info : dto.getStorableData()) {
            if (info.name.equals("id")) {
                info.name = Mongo.ID;
                if (action.equals(Dto.Action.INSERT)) {
                    info.isNull = false;
                }
            }
            if (info.isNull) {
                document.put(info.name, null);
            } else if (info.value == null && (action.equals(Dto.Action.UPDATE_ALL_COLS) || action.equals(Dto.Action.INSERT))) {
                document.put(info.name, null);
            } else if (info.value != null) {
                document.put(info.name, getValueForDocument(info, action));
            }
        }

        return document;
    }

    private static Object getValueForDocument(StorableData info, Dto.Action action) {
        if (info.value == null) {
            return null;
        }

        if (info.type.equals(String.class)) {
            return action.equals(Dto.Action.GET) && StringUtil.isNotEmpty((String) info.value) ?
                new Document("$regex", "(?i)" + Pattern.quote((String) info.value) + ".*") : info.value;

        } else if (info.type.equals(DateTime.class)) {
            DateTime dateTime = (DateTime) info.value;
            if (info.hasAnnotation(Timestamp.class)) {
                return dateTime.getAsTimestamp();
            } else if (info.hasAnnotation(Date.class)) {
                return dateTime.getAsDate();
            } else if (info.hasAnnotation(Time.class)) {
                return dateTime.formatter().getTimeHms();
            } else {
                return dateTime.getAsTimestamp();
            }

        } else if (info.type.isEnum()) {
            return info.value.toString();

        } else if (info.type.equals(Location.class)) {
            Location location = (Location) info.value;
            if (location.latitude == null || location.longitude == null) {
                return null;
            }
            return new Point(new Position(location.latitude, location.longitude));

        } else if (info.type.equals(com.vantar.database.datatype.Polygon.class)) {
            com.vantar.database.datatype.Polygon polygon = (com.vantar.database.datatype.Polygon) info.value;
            List<Position> positions = new ArrayList<>();
            for (Location location : polygon.locations) {
                if (location != null) {
                    if (location.latitude == null || location.longitude == null) {
                        continue;
                    }
                    positions.add(new Position(location.latitude, location.longitude));
                }
            }
            return new Polygon(positions);

        } else if (info.value instanceof List) {
            List<Object> list = new ArrayList<>();
            for (Object v : (List) info.value) {
                if (v != null) {
                    list.add(getValueForDocument(new StorableData(v.getClass(), v, false), action));
                }
            }
            return list;

        } else if (info.value instanceof Set) {
            Set<Object> set = new HashSet<>();
            for (Object v : (Set) info.value) {
                if (v != null) {
                    set.add(getValueForDocument(new StorableData(v.getClass(), v, false), action));
                }
            }
            return set;

        } else if (info.value instanceof Map) {
            return mapToDocumentObject((Map<?, ?>) info.value, action);

        } else if (info.value instanceof Dto) {
            ((Dto) info.value).setToDefaultsWhenNull();
            return getFieldValuesAsDocument((Dto) info.value, action);

        } else {
            if (!info.type.getPackage().getName().startsWith("java.")) {
                return mapToDocument(ObjectUtil.getPropertyValues(info.value), action);
            }
        }

        return info.value;
    }

    public static Document mapToDocument(Map<String, Object> data, Dto.Action action) {
        Document document = new Document();
        data.forEach((k, v) -> {
            if (k !=null && v != null) {
                document.put(
                    Mongo.Escape.keyForStore(k),
                    getValueForDocument(new StorableData(v.getClass(), v, false), action)
                );
            }
        });
        return document;
    }

    public static Document mapToDocumentObject(Map<?, ?> data, Dto.Action action) {
        Document document = new Document();
        data.forEach((k, v) -> {
            if (k !=null && v != null) {
                document.put(
                    Mongo.Escape.keyForStore(k.toString()),
                    getValueForDocument(new StorableData(v.getClass(), v, false), action)
                );
            }
        });
        return document;
    }

    private static List<Object> normalizeList(List<?> inList) {
        List<Object> outList = new ArrayList<>();
        for (Object item: inList) {
            if (item instanceof Number || item instanceof String || item instanceof Character || item instanceof Boolean) {
                outList.add(item);
            } else {
                outList.add(item.toString());
            }
        }
        return outList;
    }

    // > > > QUERY BUILDER

    public static void queryBuilderToMongoQuery(QueryBuilder q, MongoQuery mongoQuery) {
        mongoQuery.sort = sort(q.getSort());
        if (q.getUnion() != null) {
            mongoQuery.union = q.getUnion();
        }
        if (q.getLimit() != null) {
            mongoQuery.limit = q.getLimit();
        }
        if (q.getSkip() != null) {
            mongoQuery.skip = q.getSkip();
        }
        if (!q.conditionIsEmpty()) {
            Document matches = getMongoMatches(q.getCondition(), q.getDto());
            if (matches != null) {
                mongoQuery.matches = matches;
            }
        }
        if (!q.groupIsEmpty()) {
            Document document = new Document();
            for (QueryGroup group : q.getGroup()) {
                for (int i = 0, l = group.columns.length; i < l; ++i) {
                    String fieldName = group.columns[i];
                    if (fieldName == null) {
                        continue;
                    }
                    if (fieldName.equals("id")) {
                        group.columns[i] = Mongo.ID;
                    } else if (fieldName.endsWith(".id")) {
                        group.columns[i] = StringUtil.replace(fieldName, ".id", "." + Mongo.ID);
                    }
                }

                switch (group.groupType) {
                    case SUM:
                        if (group.columns.length > 2) {
                            List<String> list = new ArrayList<>();
                            for (int i = 2, l = group.columns.length; i < l; ++i) {
                                list.add("$" + group.columns[i]);
                            }
                            document.put(
                                group.columns[0],
                                new Document(
                                    "$sum",
                                    new Document(
                                        "$" + group.columns[1],
                                        list
                                    )
                                )
                            );
                        } else {
                            document.put(group.columns[0], new Document("$sum", "$" + group.columns[1]));
                        }
                        break;
                    case COUNT:
                        document.put(group.columns[0], new Document("$sum", 1));
                        break;
                    case AVG:
                        mongoQuery.average(group.columns[0]);
                        break;
                    default:
                        document.put(group.columns[1], group.columns[0] == null ? null : "$" + group.columns[0]);
                }
            }
            if (!document.isEmpty()) {
                mongoQuery.group(document);
            }
        }
    }

    public static Document getMongoMatches(QueryCondition qCondition, Dto dto) {
        if (!dto.isDeleteLogicalEnabled() && (qCondition == null || qCondition.q.size() == 0)) {
            return null;
        }

        QueryCondition condition;
        if (dto.isDeleteLogicalEnabled() && dto.getDeletedQueryPolicy() != Dto.QueryDeleted.SHOW_ALL) {

            if (qCondition == null) {
                condition = new QueryCondition(dto.getStorage());
            } else if (qCondition.operator.equals(AND)) {
                condition = qCondition;
            } else {
                condition = new QueryCondition(dto.getStorage());
                condition.addCondition(qCondition);
            }

            switch (dto.getDeletedQueryPolicy()) {
                case SHOW_NOT_DELETED:
                    condition.notEqual(Mongo.LOGICAL_DELETE_FIELD, Mongo.LOGICAL_DELETE_VALUE);
                    break;
                case SHOW_DELETED:
                    condition.equal(Mongo.LOGICAL_DELETE_FIELD, Mongo.LOGICAL_DELETE_VALUE);
                    break;
            }
        } else {
            condition = qCondition;
        }

        List<Bson> matches = new ArrayList<>();
        for (QueryMatchItem item : condition.q) {
            if (item.type == QUERY) {
                matches.add(getMongoMatches(item.queryValue, dto));
                continue;
            }

            String fieldName = StringUtil.toSnakeCase(item.fieldName);
            if (fieldName != null) {
                if (fieldName.equals("id")) {
                    fieldName = Mongo.ID;
                } else if (fieldName.endsWith(".id")) {
                    fieldName = StringUtil.replace(fieldName, ".id", "." + Mongo.ID);
                }
            }

            switch (item.type) {
                case EQUAL:
                    matches.add(new Document(fieldName, item.getValue()));
                    break;
                case NOT_EQUAL:
                    matches.add(new Document(fieldName, new Document("$ne", item.getValue())));
                    break;

                case LIKE:
                    matches.add(new Document(
                        fieldName,
                        new Document("$regex", "(?i)" + Pattern.quote(item.stringValue) + ".*")
                    ));
                    break;
                case NOT_LIKE:
                    matches.add(new Document(
                        fieldName,
                        new Document("$ne", new Document("$regex", "(?i)" + Pattern.quote(item.stringValue) + ".*"))
                    ));
                    break;

                case IN:
                    matches.add(new Document(fieldName, new Document("$in", Arrays.asList(item.getValues()))));
                    break;
                case NOT_IN:
                    matches.add(new Document(fieldName, new Document("$nin", Arrays.asList(item.getValues()))));
                    break;

                case FULL_SEARCH:
                    if (StringUtil.isNotEmpty(fieldName)) {
                        matches.add(new Document(fieldName, new Document("$text", new Document("$search", item.getValue()))));
                        break;
                    }
                    List<Document> textMatches = new ArrayList<>();
                    String p = Pattern.quote(item.stringValue);
                    dto.getPropertyTypes().forEach((name, tClass) -> {
                        if (tClass.equals(String.class)) {
                            textMatches.add(new Document(name, new Document("$regex", "(?i)" + p + ".*")));
                        }
                    });
                    matches.add(new Document("$or", textMatches));
                    break;

                case BETWEEN: {
                    Object[] values = item.getValues();
                    List<Document>  bConditions = new ArrayList<>(2);
                    bConditions.add(new Document(fieldName, new Document("$gte", values[0])));
                    bConditions.add(new Document(fieldName, new Document("$lte", values[1])));
                    matches.add(new Document("$and",  bConditions));
                    break;
                }
                case NOT_BETWEEN: {
                    Object[] values = item.getValues();
                    List<Document> bConditions = new ArrayList<>(2);
                    bConditions.add(new Document(fieldName, new Document("$lt", values[0])));
                    bConditions.add(new Document(fieldName, new Document("$gt", values[1])));
                    matches.add(new Document("$or", bConditions));
                    break;
                }
                case LESS_THAN:
                    matches.add(new Document(fieldName, new Document("$lt", item.getValue())));
                    break;
                case GREATER_THAN:
                    matches.add(new Document(fieldName, new Document("$gt", item.getValue())));
                    break;
                case LESS_THAN_EQUAL:
                    matches.add(new Document(fieldName, new Document("$lte", item.getValue())));
                    break;
                case GREATER_THAN_EQUAL:
                    matches.add(new Document(fieldName, new Document("$gte", item.getValue())));
                    break;

                case IS_NULL:
                    matches.add(new Document(fieldName, new Document("$eq", null)));
                    break;
                case IS_NOT_NULL:
                    matches.add(new Document(fieldName, new Document("$ne", null)));
                    break;
                case IS_EMPTY: {
                    List<Document> neConditions = new ArrayList<>(5);
                    neConditions.add(new Document(fieldName, new Document("$eq", null)));
                    neConditions.add(new Document(fieldName, new Document("$eq", "")));
                    neConditions.add(new Document(fieldName, new Document("$eq", new ArrayList<>(1))));
                    neConditions.add(new Document(fieldName, new Document("$eq", new HashMap<>(1))));
                    neConditions.add(new Document(fieldName, new Document("$exists", false)));
                    matches.add(new Document("$or", neConditions));
                    break;
                }
                case IS_NOT_EMPTY: {
                    List<Document> neConditions = new ArrayList<>(5);
                    neConditions.add(new Document(fieldName, new Document("$ne", null)));
                    neConditions.add(new Document(fieldName, new Document("$ne", "")));
                    neConditions.add(new Document(fieldName, new Document("$ne", new ArrayList<>(1))));
                    neConditions.add(new Document(fieldName, new Document("$ne", new HashMap<>(1))));
                    neConditions.add(new Document(fieldName, new Document("$exists", true)));
                    matches.add(new Document("$and", neConditions));
                    break;
                }
                case CONTAINS_ALL:
                    matches.add(new Document(fieldName, new Document("$all", normalizeList(item.itemList))));
                    break;

                case NEAR:
                    Object[] numbers = item.getValues();
                    List<Double> point = new ArrayList<>(2);
                    point.add((Double) numbers[0]);
                    point.add((Double) numbers[1]);
                    Document geometry = new Document(
                        "$geometry",
                        new Document("type", "Point").append(VantarParam.COORDINATE, point)
                    );
                    if (numbers[2] != null) {
                        geometry.append("$maxDistance", numbers[2]);
                    }
                    if (numbers[3] != null) {
                        geometry.append("$minDistance", numbers[3]);
                    }
                    matches.add(
                        new Document(
                            fieldName,
                            new Document("$near", geometry)
                        )
                    );
                    break;

                case WITHIN:
                    List<List<Double>> polygon = new ArrayList<>();
                    for (Object o : item.getValues()) {
                        Location location = (Location) o;
                        List<Double> pt = new ArrayList<>(2);
                        pt.add(location.latitude);
                        pt.add(location.longitude);
                        polygon.add(pt);
                    }
                    matches.add(
                        Filters.geoWithinPolygon(fieldName, polygon)
                    );
                    break;

                case IN_LIST: {
                    matches.add(new Document(
                        fieldName,
                        new Document("$elemMatch", getMongoMatches(item.queryValue, dto))
                    ));
                    break;
                }

                case IN_DTO: {
                    String[] parts = StringUtil.split(fieldName, ':');
                    fieldName = parts[0].trim();
                    if (fieldName.equals("id")) {
                        fieldName = Mongo.ID;
                    }
                    String fieldNameInner = parts.length == 2 ? parts[1].trim() : Mongo.ID;

                    Document innerMatch = getMongoMatches(item.queryValue, item.dto);
                    if (innerMatch == null || innerMatch.isEmpty()) {
                        continue;
                    }

                    MongoQuery q = new MongoQuery(item.dto, item.dto);
                    q.matches = innerMatch;
                    q.columns = new String[] { fieldNameInner };
                    FindIterable<Document> cursor;
                    try {
                        cursor = q.getResult();
                    } catch (DatabaseException e) {
                        Mongo.log.error("!! IN_DTO error {}", item, e);
                        continue;
                    }
                    Set<Long> ids = new HashSet<>(100);
                    for (Document document : cursor) {
                        ids.add(document.getLong(fieldNameInner));
                    }
                    if (ids.isEmpty()) {
                        continue;
                    }
                    matches.add(new Document(fieldName, new Document("$in", ids)));
                    break;
                }
            }
        }

        String op;
        switch (condition.operator) {
            case OR:
                op = "$or";
                break;
            case NOR:
                op = "$nor";
                break;
            default:
                op = "$and";
        }

        return new Document(op, matches);
    }
}
