package com.vantar.database.nosql.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.geojson.*;
import com.vantar.common.VantarParam;
import com.vantar.database.datatype.Location;
import com.vantar.database.dto.Date;
import com.vantar.database.dto.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import org.bson.*;
import org.bson.conversions.Bson;
import java.util.*;
import java.util.regex.Pattern;
import static com.vantar.database.query.QueryOperator.QUERY;

@SuppressWarnings({"rawtypes"})
public class MongoMapping {

    public static Document sort(String[] sort) {
        if (sort == null || sort.length == 0) {
            return null;
        }
        Document document = new Document();
        for (String item : sort) {
            String[] parts = StringUtil.splitTrim(item, ':');
            document.append(
                parts[0].equals(DtoBase.ID) ? DbMongo.ID : parts[0],
                parts.length == 1 ? 1 : (parts[1].equalsIgnoreCase("asc") || parts[1].equals("1") ? 1 : -1)
            );
        }
        return document;
    }

    public static Document asDocument(Dto dto, Dto.Action action) {
        Document document = new Document();
        for (StorableData info : dto.getStorableData()) {
            if (info.name.equals(VantarParam.ID)) {
                info.name = DbMongo.ID;
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
            return location.altitude == null ?
                new Point(new Position(location.latitude, location.longitude)) :
                new Point(new Position(location.latitude, location.longitude, location.altitude));

        } else if (info.type.equals(com.vantar.database.datatype.Polygon.class)) {
            com.vantar.database.datatype.Polygon polygon = (com.vantar.database.datatype.Polygon) info.value;
            List<Position> positions = new ArrayList<>();
            for (Location location : polygon.locations) {
                if (location != null) {
                    if (location.latitude == null || location.longitude == null) {
                        continue;
                    }
                    if (location.altitude == null) {
                        positions.add(new Position(location.latitude, location.longitude));
                    } else {
                        positions.add(new Position(location.latitude, location.longitude, location.altitude));
                    }
                }
            }
            return new Polygon(positions);

        } else if (info.value instanceof List) {
            List<Object> list = new ArrayList<>(((List<?>) info.value).size());
            for (Object v : (List) info.value) {
                if (v != null) {
                    list.add(getValueForDocument(new StorableData(v.getClass(), v, false), action));
                }
            }
            return list;

        } else if (info.value instanceof Set) {
            Set<Object> set = new HashSet<>(((Set<?>) info.value).size());
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
            return asDocument((Dto) info.value, action);

        } else {
            if (!info.type.getPackage().getName().startsWith("java.")) {
                return mapToDocument(ObjectUtil.getPropertyValues(info.value), action);
            }
        }

        return info.value;
    }

    private static Document mapToDocumentObject(Map<?, ?> data, Dto.Action action) {
        Document document = new Document();
        data.forEach((k, v) -> {
            if (k != null && v != null) {
                document.put(
                    DbMongo.escapeKeyForWrite(k.toString()),
                    getValueForDocument(new StorableData(v.getClass(), v, false), action)
                );
            }
        });
        return document;
    }

    public static Document mapToDocument(Map<String, Object> data, Dto.Action action) {
        Document document = new Document();
        data.forEach((k, v) -> {
            if (k != null && v != null) {
                document.put(
                    DbMongo.escapeKeyForWrite(k),
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
            Document matches = getMongoMatches(q.getCondition(), q.getDto(), mongoQuery.db);
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
                    if (fieldName.equals(VantarParam.ID)) {
                        group.columns[i] = DbMongo.ID;
                    } else if (fieldName.endsWith(".id")) {
                        group.columns[i] = StringUtil.replace(fieldName, ".id", "." + DbMongo.ID);
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
                        mongoQuery.setAverage(group.columns[0]);
                        break;
                    default:
                        int l = group.columns.length;
                        if (l == 2) {
                            document.put(
                                StringUtil.replace(group.columns[1], '.', '_'),
                                group.columns[0] == null ? null : "$" + group.columns[0]
                            );
                            break;
                        }
                        Document columnsDoc = new Document();
                        --l;
                        for (int i = 0; i < l; ++i) {
                            columnsDoc.append(
                                StringUtil.replace(group.columns[i], '.', '_'),
                                "$" + group.columns[i]
                            );
                        }
                        document.put(group.columns[l], columnsDoc);
                }
            }
            if (!document.isEmpty()) {
                mongoQuery.setGroup(document);
            }
        }
    }

    public static Document getMongoMatches(QueryCondition qCondition, Dto dto, DbMongo db) {
        if (qCondition == null || qCondition.q.size() == 0) {
            return null;
        }

        List<Bson> matches = new ArrayList<>(10);
        for (QueryMatchItem item : qCondition.q) {
            if (item.type == QUERY) {
                matches.add(getMongoMatches(item.queryValue, dto, db));
                continue;
            }

            String fieldName = item.fieldName;
            boolean searchInLastListItem = false;
            if (fieldName != null) {
                if (fieldName.equals(VantarParam.ID)) {
                    fieldName = DbMongo.ID;
                } else if (fieldName.endsWith(".id")) {
                    fieldName = StringUtil.replace(fieldName, ".id", "." + DbMongo.ID);
                    // todo: is this correct?
                } else if (fieldName.endsWith(".+id")) {
                    fieldName = StringUtil.replace(fieldName, ".+id", ".id");
                } else if (fieldName.endsWith(".-1")) {
                    fieldName = StringUtil.remove(fieldName, ".-1");
                    searchInLastListItem = true;
                }
            }

            String op = null;
            switch (item.type) {
                case EQUAL:
                    op = "$eq";
                case NOT_EQUAL:
                    if (op == null) {
                        op = "$ne";
                    }
                    matches.add(
                        searchInLastListItem ?
                            getListLastElementExp(fieldName, item.getValue(), op) :
                            new Document(fieldName, new Document(op, item.getValue()))
                    );
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
                    op = "$in";
                case NOT_IN:
                    if (op == null) {
                        op = "$nin";
                    }
                    matches.add(
                        searchInLastListItem ?
                            getListLastElementExp(fieldName, Arrays.asList(item.getValues()), op) :
                            new Document(fieldName, new Document(op, Arrays.asList(item.getValues())))
                    );
                    break;
                case CONTAINS_ALL:
                    matches.add(
                        searchInLastListItem ?
                            getListLastElementExp(fieldName, normalizeList(item.itemList), "$all") :
                            new Document(fieldName, new Document("$all", normalizeList(item.itemList)))
                    );
                    matches.add(new Document(fieldName, new Document("$all", normalizeList(item.itemList))));
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
                    List<Document> bConditions = new ArrayList<>(2);
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
                    op = "$lt";
                case GREATER_THAN:
                    if (op == null) {
                        op = "$gt";
                    }
                case LESS_THAN_EQUAL:
                    if (op == null) {
                        op = "$lte";
                    }
                case GREATER_THAN_EQUAL:
                    if (op == null) {
                        op = "$gte";
                    }
                    matches.add(
                        searchInLastListItem ?
                            getListLastElementExp(fieldName, item.getValue(), op) :
                            new Document(fieldName, new Document(op, item.getValue()))
                    );
                    break;

                case IS_NULL: {
                    if (fieldName.contains(".")) {
                        matches.add(new Document(fieldName, new Document("$exists", false)));
                    } else {
                        matches.add(new Document(fieldName, null));
                    }
                    break;
                }
                case IS_NOT_NULL: {
                    if (fieldName.contains(".")) {
                        matches.add(new Document(fieldName, new Document("$exists", true)));
                    } else {
                        matches.add(new Document(fieldName, new Document("$ne", null)));
                    }
                    break;
                }
                case IS_EMPTY: {
                    List<Document> c = new ArrayList<>(5);
                    c.add(new Document(fieldName, new Document("$eq", null)));
                    c.add(new Document(fieldName, new Document("$eq", "")));
                    c.add(new Document(fieldName, new Document("$eq", new ArrayList<>(1))));
                    c.add(new Document(fieldName, new Document("$eq", new HashMap<>(1, 1))));
                    c.add(new Document(fieldName, new Document("$exists", false)));
                    matches.add(new Document("$or", c));
                    break;
                }
                case IS_NOT_EMPTY: {
                    List<Document> c = new ArrayList<>(5);
                    c.add(new Document(fieldName, new Document("$ne", null)));
                    c.add(new Document(fieldName, new Document("$ne", "")));
                    c.add(new Document(fieldName, new Document("$ne", new ArrayList<>(1))));
                    c.add(new Document(fieldName, new Document("$ne", new HashMap<>(1, 1))));
                    c.add(new Document(fieldName, new Document("$exists", true)));
                    matches.add(new Document("$and", c));
                    break;
                }

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
                    matches.add(new Document(fieldName, new Document("$near", geometry)));
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
                    matches.add(Filters.geoWithinPolygon(fieldName, polygon));
                    break;

                case MAP_KEY_EXISTS: {
                    matches.add(new Document(fieldName + "." + item.getValue(), new Document("$exists", true)));
                    break;
                }

                case IN_LIST: {
                    Document innerMatch = getMongoMatches(item.queryValue, dto, db);
                    if (innerMatch == null || innerMatch.isEmpty()) {
                        continue;
                    }
                    matches.add(new Document(fieldName, new Document("$elemMatch", innerMatch)));
                    break;
                }

                case IN_DTO: {
                    String[] parts = StringUtil.split(fieldName, ':');
                    if (parts == null) {
                        break;
                    }
                    fieldName = parts[0].trim();
                    if (fieldName.equals(VantarParam.ID)) {
                        fieldName = DbMongo.ID;
                    }
                    String fieldNameInner = parts.length == 2 ? parts[1].trim() : DbMongo.ID;
                    if (fieldNameInner.equals(VantarParam.ID)) {
                        fieldNameInner = DbMongo.ID;
                    }

                    Document innerMatch = getMongoMatches(item.queryValue, item.dto, db);
                    if (innerMatch == null || innerMatch.isEmpty()) {
                        continue;
                    }

                    MongoQuery q = new MongoQuery(item.dto);
                    q.matches = innerMatch;
                    q.columns = new String[] { fieldNameInner };
                    FindIterable<Document> cursor;
                    try {
                        cursor = db.getResult(q);
                    } catch (Exception e) {
                        ServiceLog.log.error("!! IN_DTO error {}", item, e);
                        continue;
                    }
                    Set<Long> ids = new HashSet<>(100, 1);
                    for (Document document : cursor) {
                        ids.add(document.getLong(fieldNameInner));
                    }
                    if (ids.isEmpty()) {
                        matches.add(new Document(DbMongo.ID, -1));
                        continue;
                    }
                    matches.add(new Document(fieldName, new Document("$in", ids)));
                    break;
                }
            }
        }

        String op;
        switch (qCondition.operator) {
            case OR:
                op = "$or";
                break;
            case NOR:
                op = "$nor";
                break;
            default:
                op = "$and";
        }

        Document document = matches.isEmpty() ? new Document() : new Document(op, matches);
        if (qCondition.inspect) {
            ServiceLog.log.info(" > mongo condition dump: {} --> {}", dto.getClass().getSimpleName(), document);
        }

        return document;
    }

    private static Document getListLastElementExp(String fieldName, Object value, String op) {
        List<Object> ex = new ArrayList<>(1);
        ex.add(new Document("$last", "$" + fieldName));
        ex.add(value);
        return new Document("$expr", new Document(op, ex));
    }
}
