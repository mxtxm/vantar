package com.vantar.business;

import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.web.ResponseMessage;
import java.util.*;


public class CommonRepoMongo extends Mongo {

    public static long insert(Dto dto) throws DatabaseException {
        long id = Mongo.insert(dto);
        CommonModel.afterDataChange(dto);
        return id;
    }

    /**
     * auto inc ids are set to dto
     */
    public static void insert(List<? extends Dto> dtos) throws DatabaseException {
        Mongo.insert(dtos);
        if (!dtos.isEmpty()) {
            CommonModel.afterDataChange(dtos.get(0));
        }
    }

    public static void update(Dto dto) throws DatabaseException {
        Mongo.update(dto);
        CommonModel.afterDataChange(dto);
    }

    public static void update(Dto dto, QueryBuilder q) throws DatabaseException {
        q.setDto(dto);
        Mongo.update(q);
        CommonModel.afterDataChange(dto);
    }

    public static void update(Dto dto, Dto condition) throws DatabaseException {
        Mongo.update(dto, condition);
        CommonModel.afterDataChange(dto);
    }

    public static void upsert(Dto dto) throws DatabaseException {
        Mongo.upsert(dto);
        CommonModel.afterDataChange(dto);
    }

    public static long delete(Dto dto) throws DatabaseException {
        long id = Mongo.delete(dto);
        CommonModel.afterDataChange(dto);
        return id;
    }

    public static void deleteAll(Dto dto) throws DatabaseException {
        Mongo.deleteAll(dto);
        CommonModel.afterDataChange(dto);
    }

    public static ResponseMessage delete(List<Long> ids, Dto dto) {
        int i = 0;
        for (Long id : ids) {
            try {
                dto.setId(id);
                CommonRepoMongo.delete(dto);
                ++i;
            } catch (DatabaseException e) {
                log.error("! {}", dto, e);
            }
        }
        return new ResponseMessage(VantarKey.DELETE_SUCCESS, i);
    }

    public static void undoDelete(Dto dto) throws DatabaseException {
        Mongo.unset(dto.getStorage(), dto.getId(), Mongo.LOGICAL_DELETE_FIELD);
        CommonModel.afterDataChange(dto);
    }

    public static void undoDelete(List<? extends Dto> dtos) throws DatabaseException {
        for (Dto dto : dtos) {
            undoDelete(dto);
        }
    }

    public static void increaseValue(Dto dto, String fieldName, long value) throws DatabaseException {
        Mongo.increaseValue(dto, fieldName, value);
        CommonModel.afterDataChange(dto);
    }

    public static void createAllDtoIndexes(boolean deleteIfExists) throws DatabaseException {
        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
            if (deleteIfExists) {
                Mongo.Index.remove(info.getDtoInstance());
            }
            Mongo.Index.create(info.getDtoInstance());
        }
    }

    public static long insertUnique(Dto dto) throws DatabaseException {
        if (dto.getId() != null && MongoSearch.existsById(dto)) {
            return VantarParam.INVALID_ID;
        }
        return insert(dto);
    }

    public static List<ValidationError> getUniqueViolation(Dto dto) throws DatabaseException {
        List<ValidationError> errors = null;
        for (String fieldName : dto.annotatedProperties(Unique.class)) {
            if (!MongoSearch.isUnique(dto, fieldName)) {
                if (errors == null) {
                    errors = new ArrayList<>();
                }
                errors.add(new ValidationError(fieldName, VantarKey.UNIQUE));
            }
        }
        return errors;
    }

    public static void purge(String collection) throws DatabaseException {
        deleteAll(collection);
        Mongo.Index.remove(collection);
        Mongo.Sequence.remove(collection);
    }

    public static void purgeData(String collection) throws DatabaseException {
        deleteAll(collection);
        Mongo.Index.remove(collection);
        Mongo.Sequence.remove(collection);
    }

    // > > > by dto

    public static <T extends Dto> T getById(T dto, String... locales) throws NoContentException, DatabaseException {
        QueryBuilder q = new QueryBuilder(dto);
        q.condition().equal(Mongo.ID, dto.getId());
        QueryResult result = MongoSearch.getData(q);
        if (CollectionUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.first();
    }

    public static <T extends Dto> T getFirst(T dto, String... locales) throws DatabaseException, NoContentException {
        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch(QueryOperator.AND);
        QueryResult result = MongoSearch.getData(q);
        if (CollectionUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.first();
    }

    public static <T extends Dto> List<T> getData(T dto, String... locales) throws DatabaseException, NoContentException {
        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch(QueryOperator.AND);
        QueryResult result = MongoSearch.getData(q);
        if (CollectionUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asList();
    }

    public static <T extends Dto> List<T> getAll(Dto dto, String... locales) throws NoContentException, DatabaseException {
        QueryResult result = MongoSearch.getAllData(dto);
        if (CollectionUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asList();
    }

    public static Map<String, String> getAsKeyValue(Dto dto, String key, String value, String... locales) throws NoContentException, DatabaseException {
        return MongoSearch.getAllData(dto).setLocale(locales).asKeyValue(key, value);
    }

    // > > > by QueryBuilder

    public static <T extends Dto> T getFirst(QueryBuilder q, String... locales) throws DatabaseException, NoContentException {
        QueryResult result = MongoSearch.getData(q);
        if (CollectionUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.first();
    }

    public static <T extends Dto> List<T> getData(QueryBuilder q, String... locales) throws DatabaseException, NoContentException {
        QueryResult result = MongoSearch.getData(q);
        if (CollectionUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asList();
    }

    public static Object search(QueryData queryData, String... locales) throws DatabaseException, NoContentException, InputException {
        return search(new QueryBuilder(queryData), locales);
    }

    public static Object search(QueryBuilder q, String... locales) throws DatabaseException, NoContentException {
        if (q.isPagination()) {
            return MongoSearch.getPage(q, locales);
        } else {
            QueryResult result = MongoSearch.getData(q);
            if (CollectionUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.asList();
        }
    }

    // > > > count exists

    public static long count(QueryBuilder q) throws DatabaseException {
        return MongoSearch.count(q);
    }

    public static long count(String collectionName) throws DatabaseException {
        return MongoSearch.count(collectionName);
    }

    public static boolean exists(QueryBuilder q) throws DatabaseException {
        return MongoSearch.exists(q);
    }

    public static boolean exists(Dto dto, String property) throws DatabaseException {
        return MongoSearch.exists(dto, property);
    }

    public static boolean existsById(Dto dto) throws DatabaseException {
        return MongoSearch.existsById(dto);
    }

    public static boolean existsByDto(Dto dto) throws DatabaseException {
        return MongoSearch.existsByDto(dto);
    }
}