package com.vantar.database.nosql.mongo;

import com.mongodb.client.*;
import com.mongodb.client.model.Projections;
import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.util.object.*;
import org.bson.Document;
import org.elasticsearch.client.security.user.User;
import java.util.*;


public class MongoQuery {

    public Document matches;
    public Document sort;
    public Integer skip;
    public Integer limit;
    public List<String> union;
    public String[] columns;
    private List<Document> groups;
    private final Dto dto;
    private final String storage;


    public MongoQuery(Dto dto) {
        storage = dto.getStorage();
        columns = dto.getFieldNames();
        this.dto = dto;
    }

    public MongoQuery(QueryBuilder q) {
        dto = q.getDto();
        storage = dto.getStorage();
        columns = dto.getFieldNames();
        MongoMapping.queryBuilderToMongoQuery(q, this);
    }

    public void setGroup(Document group) {
        if (groups == null) {
             groups = new ArrayList<>();
        }
        groups.add(group);
    }

    public void setAverage(String field) {
        if (groups == null) {
            groups = new ArrayList<>();
        }
        Document group = new Document(Mongo.ID, null);
        group.put("average", new Document("$avg", "$" + field));
        groups.add(group);
    }

    public long count() throws DatabaseException {
        try {
            return matches == null || matches.isEmpty() ?
                MongoConnection.getDatabase().getCollection(storage).estimatedDocumentCount() :
                MongoConnection.getDatabase().getCollection(storage).countDocuments(matches);
        } catch (Exception e) {
            Mongo.log.error(" ! count --> {}", dto, e);
            throw new DatabaseException(e);
        }
    }

    public QueryResult getData() throws DatabaseException {
        try {
            FindIterable<Document> find = (matches == null || matches.isEmpty()) ?
                MongoConnection.getDatabase().getCollection(storage).find() :
                MongoConnection.getDatabase().getCollection(storage).find(matches);

            if (sort != null) {
                find.sort(sort);
            }
            if (skip != null) {
                find.skip(skip);
            }
            if (limit != null) {
                find.limit(limit);
            }
            if (columns != null && columns.length > 0) {
                find.projection(Projections.fields(Projections.include(columns)));
            }
            return new MongoQueryResult(find, dto);
        } catch (Exception e) {
            Mongo.log.error(" ! count --> {}", dto, e);
            throw new DatabaseException(e);
        }
    }

    public FindIterable<Document> getResult() throws DatabaseException {
        try {
            FindIterable<Document> find = (matches == null || matches.isEmpty()) ?
                MongoConnection.getDatabase().getCollection(storage).find() :
                MongoConnection.getDatabase().getCollection(storage).find(matches);

            if (sort != null) {
                find.sort(sort);
            }
            if (skip != null) {
                find.skip(skip);
            }
            if (limit != null) {
                find.limit(limit);
            }
            if (columns != null && columns.length > 0) {
                find.projection(Projections.fields(Projections.include(columns)));
            }

            if (columns != null && columns.length > 0) {
                find.projection(Projections.fields(Projections.include(columns)));
            }

            return find;
        } catch (Exception e) {
            Mongo.log.error(" !! query {}", storage, e);
            throw new DatabaseException(e);
        }
    }

    public QueryResult getDataByAggregate() throws DatabaseException {
        return new MongoQueryResult(getAggregate(), dto);
    }

    public AggregateIterable<Document> getAggregate() throws DatabaseException {
        List<Document> query = new ArrayList<>(10);
        if (matches != null && !matches.isEmpty()) {
            query.add(new Document("$match", matches));
        }
        if (groups != null && !groups.isEmpty()) {
            for (Document group : groups) {
                query.add(new Document("$group", group));
            }
        }
            if (union != null) {
            for (String collection : union) {
                query.add(new Document("$unionWith", collection));
            }
        }
        if (sort != null) {
            query.add(new Document("$sort", sort));
        }
        if (skip != null) {
            query.add(new Document("$skip", skip));
        }
        if (limit != null) {
            query.add(new Document("$limit", limit));
        }

        try {
            return MongoConnection.getDatabase().getCollection(storage).aggregate(query).allowDiskUse(true);
        } catch (Exception e) {
            Mongo.log.error(" !! aggr {}", dto, e);
            throw new DatabaseException(e);
        }
    }









    /**
     * Check by property values
     * if dto.id is set, it will be taken into account thus (id, property) must be unique
     */
    public static boolean isUnique(Dto dto, String... properties) throws DatabaseException {
        Document condition = new Document(Mongo.LOGICAL_DELETE_FIELD, new Document("$ne", Mongo.LOGICAL_DELETE_VALUE));
        for (String property : properties) {
            property = property.trim();
            if (property.equals(Mongo.ID)) {
                property = VantarParam.ID;
            }
            Object v = dto.getPropertyValue(property);
            if (properties.length == 1 && ObjectUtil.isEmpty(v)) {
                return true;
            }
            condition.append(property.equals(VantarParam.ID) ? Mongo.ID : property, v);
        }

        try {
            MongoCursor<Document> documents = MongoConnection.getDatabase().getCollection(dto.getStorage())
                .find(condition)
                .limit(2)
                .iterator();
            if (!documents.hasNext()) {
                return true;
            }
            Long id = dto.getId();
            if (id != null) {
                Document d = documents.next();
                if (documents.hasNext()) {
                    return false;
                }
                return id.equals(d.getLong(Mongo.ID));
            }
        } catch (Exception e) {
            Mongo.log.error("! isUnique({})", dto, e);
            throw new DatabaseException(e);
        }
        return false;
    }

    public static boolean exists(Dto dto, String property) throws DatabaseException {
        return Mongo.exists(
            DtoBase.getStorage(dto.getClass()),
                new Document(property.equals(VantarParam.ID) ? Mongo.ID : property, dto.getPropertyValue(property))
                    .append(Mongo.LOGICAL_DELETE_FIELD, new Document("$ne", Mongo.LOGICAL_DELETE_VALUE))
            );
    }

    public static boolean existsById(Dto dto) throws DatabaseException {
        return exists(dto, VantarParam.ID);
    }

    public static boolean existsByDto(Dto dto) throws DatabaseException {
        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch();
        return exists(q);
    }

    public static boolean exists(QueryBuilder q) throws DatabaseException {
        q.limit(1);
        return count(q) > 0;
    }

    public static long count(QueryBuilder q) throws DatabaseException {
        return new MongoQuery(q).count();
    }

    public static long count(String collectionName) throws DatabaseException {
        try {
            return MongoConnection.getDatabase().getCollection(collectionName).estimatedDocumentCount();
        } catch (Exception e) {
            Mongo.log.error("! count {}", collectionName, e);
            throw new DatabaseException(e);
        }
    }

    /**
     * Get by id
     */
    public static Dto get(Dto dto) throws DatabaseException, NoContentException {
        QueryBuilder q = new QueryBuilder(dto);
        q.condition().equal(Mongo.ID, dto.getId());
        return getData(q).first();
    }

    public static List<? extends Dto> get(Dto dto, Long... ids) throws DatabaseException, NoContentException {
        QueryBuilder q = new QueryBuilder(dto);
        q.condition().in(Mongo.ID, ids);
        return getData(q).first();
    }

    public static <T extends Dto> T getDto(Class<T> tClass, long id, String... locales) throws NoContentException {
        T dto = ClassUtil.getInstance(tClass);
        if (dto == null) {
            return null;
        }
        try {
            MongoQueryResult result = new MongoQueryResult(
                MongoConnection.getDatabase().getCollection(dto.getStorage()).find(new Document(Mongo.ID, id)),
                dto
            );
            if (locales != null && locales.length > 0) {
                result.setLocale(locales);
            }
            return result.first();
        } catch (DatabaseException e) {
            return null;
        }
    }

    public static QueryResult getData(QueryBuilder q) throws DatabaseException {
        return new MongoQuery(q).getData();
    }

    public static QueryResult getAllData(Dto dto, String... sort) throws DatabaseException {
        try {
            if (dto.isDeleteLogicalEnabled()) {
                Document condition;
                switch (dto.getDeletedQueryPolicy()) {
                    case SHOW_NOT_DELETED:
                        condition = new Document(Mongo.LOGICAL_DELETE_FIELD, new Document("$ne", Mongo.LOGICAL_DELETE_VALUE));
                        break;
                    case SHOW_DELETED:
                        condition = new Document(Mongo.LOGICAL_DELETE_FIELD, Mongo.LOGICAL_DELETE_VALUE);
                        break;
                    default:
                        condition = null;
                }
                Document sortDoc = MongoMapping.sort(sort);
                return new MongoQueryResult(
                    condition == null ?
                        MongoConnection.getDatabase().getCollection(dto.getStorage()).find().sort(sortDoc) :
                        MongoConnection.getDatabase().getCollection(dto.getStorage()).find(condition).sort(sortDoc),
                    dto
                );
            }

            return new MongoQueryResult(
                MongoConnection.getDatabase().getCollection(dto.getStorage()).find().sort(MongoMapping.sort(sort)),
                dto
            );
        } catch (Exception e) {
            Mongo.log.error("! query {}", dto.getStorage(), e);
            throw new DatabaseException(e);
        }
    }

    public static PageData getPage(QueryBuilder q, QueryResultBase.Event event, String... locales)
        throws NoContentException, DatabaseException {

        MongoQuery mongoQuery = new MongoQuery(q);
        long total = q.getTotal();
        if (total == 0) {
            total = count(q);
        }

        QueryResult result = mongoQuery.getData();
        if (locales.length > 0) {
            result.setLocale(locales);
        }
        if (event != null) {
            result.setEvent(event);
        }

        Integer limit = q.getLimit();
        List<Dto> dtos = result.asList();
        return new PageData(
            dtos,
            q.getPageNo(),
            limit == null ? dtos.size() : limit,
            total
        );
    }

    public static PageData getPageForeach(QueryBuilder q, QueryResultBase.EventForeach event, String... locales)
        throws DatabaseException {

        MongoQuery mongoQuery = new MongoQuery(q);
        long total = q.getTotal();
        if (total == 0) {
            total = count(q);
        }

        QueryResult result = mongoQuery.getData();
        if (locales.length > 0) {
            result.setLocale(locales);
        }

        Integer limit = q.getLimit();
        try {
            result.forEach(event);
        } catch (VantarException ignore) {
            // todo handle
        }

        PageData pageData = new PageData();
        pageData.page = q.getPageNo();
        pageData.length = limit == null ? 0 : limit;
        pageData.total = total;
        return pageData;
    }

    public static AggregateIterable<Document> getAggregate(QueryBuilder q) throws DatabaseException {
        return new MongoQuery(q).getAggregate();
    }

    public static double getAverage(QueryBuilder q) throws DatabaseException {
        AggregateIterable<Document> documents = new MongoQuery(q).getAggregate();
        for (Document document : documents) {
            Double average = document.getDouble("average");
            if (average != null) {
                return average;
            }
        }
        return 0;
    }
}
