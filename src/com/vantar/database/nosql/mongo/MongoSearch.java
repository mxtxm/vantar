package com.vantar.database.nosql.mongo;

import com.mongodb.client.*;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.util.object.*;
import org.bson.Document;
import java.util.List;


public class MongoSearch {

    /**
     * Check by property value
     * if dto.id is set, it will be taken into account thus (id, property) must be unique
     */
//    public static boolean isUnique(Dto dto, String property) throws DatabaseException {
//        try {
//            MongoCursor<Document> documents = MongoConnection.getDatabase().getCollection(dto.getStorage())
//                .find(
//                    new Document(property, dto.getPropertyValue(property))
//                        .append(Mongo.LOGICAL_DELETE_FIELD, new Document("$ne", Mongo.LOGICAL_DELETE_VALUE))
//                )
//                .limit(2)
//                .iterator();
//            if (!documents.hasNext()) {
//                return true;
//            }
//            Long id = dto.getId();
//            if (id != null) {
//                Document d = documents.next();
//                if (documents.hasNext()) {
//                    return false;
//                }
//                return id.equals(d.getLong(Mongo.ID));
//            }
//        } catch (Exception e) {
//            Mongo.log.error("! isUnique({})", dto, e);
//            throw new DatabaseException(e);
//        }
//        return false;
//    }

    /**
     * Check by property values
     * if dto.id is set, it will be taken into account thus (id, property) must be unique
     */
    public static boolean isUnique(Dto dto, String... properties) throws DatabaseException {
        Document condition = new Document(Mongo.LOGICAL_DELETE_FIELD, new Document("$ne", Mongo.LOGICAL_DELETE_VALUE));
        for (String property : properties) {
            property = property.trim();
            condition.append(property, dto.getPropertyValue(property));
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
        try {
            return MongoConnection.getDatabase().getCollection(dto.getStorage())
                .find(
                    new Document(property, dto.getPropertyValue(property))
                        .append(Mongo.LOGICAL_DELETE_FIELD, new Document("$ne", Mongo.LOGICAL_DELETE_VALUE))
                )
                .iterator()
                .hasNext();
        } catch (Exception e) {
            Mongo.log.error("! exists({})", dto, e);
            throw new DatabaseException(e);
        }
    }

    public static boolean existsById(Dto dto) throws DatabaseException {
        return exists(dto, Mongo.ID);
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
            return MongoConnection.getDatabase().getCollection(collectionName).countDocuments();
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
                return new MongoQueryResult(
                    condition == null ?
                        MongoConnection.getDatabase().getCollection(dto.getStorage()).find().sort(MongoMapping.sort(sort)) :
                        MongoConnection.getDatabase().getCollection(dto.getStorage()).find(condition).sort(MongoMapping.sort(sort)),
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

    public static PageData getPage(QueryBuilder q, String... locales) throws NoContentException, DatabaseException {
        MongoQuery mongoQuery = new MongoQuery(q);
        long total = q.getTotal();
        if (total == 0) {
            total = count(q);
        }

        QueryResult result = mongoQuery.getData();
        if (locales.length > 0) {
            result.setLocale(locales);
        }

        return new PageData(
            result.asList(),
            q.getPageNo(),
            q.getLimit(),
            total
        );
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
