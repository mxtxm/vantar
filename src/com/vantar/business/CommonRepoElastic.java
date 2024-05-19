package com.vantar.business;

import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.util.object.ObjectUtil;
import java.util.*;


public class CommonRepoElastic extends ElasticWrite {

    public static long insertOne(Dto dto) throws VantarException {
        long id = ElasticWrite.insertOne(dto);
        //ModelCommon.afterDataChange(dto);
        return id;
    }

    public static void updateOne(Dto dto) throws VantarException {
        ElasticWrite.updateOne(dto);
        //ModelCommon.afterDataChange(dto);
    }

    public static void upsertOne(Dto dto) throws VantarException {
        ElasticWrite.upsertOne(dto);
        //ModelCommon.afterDataChange(dto);
    }

    public static void deleteOne(Dto dto) throws VantarException {
        ElasticWrite.deleteOne(dto);
        //ModelCommon.afterDataChange(dto);
    }

    public static List<ValidationError> getUniqueViolation(Dto dto) throws DatabaseException {
        List<ValidationError> errors = null;
        for (String fieldName : dto.annotatedProperties(Unique.class)) {
            if (ElasticSearch.existsById(dto, fieldName)) {
                if (errors == null) {
                    errors = new ArrayList<>();
                }
                errors.add(new ValidationError(fieldName, VantarKey.UNIQUE));
            }
        }
        return errors;
    }

    public static void purge(String collection) throws DatabaseException {
        ElasticIndexes.deleteIndex(collection);
    }

    // > > > by dto

    public static <T extends Dto> T getById(T dto, String... locales) throws VantarException {
        QueryBuilder q = new QueryBuilder(dto);
        q.condition().equal(VantarParam.ID, dto.getId());
        QueryResult result = ElasticSearch.getData(q);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.first();
    }

    public static <T extends Dto> T getFirst(T dto, String... locales) throws VantarException {
        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch(QueryOperator.AND);
        QueryResult result = ElasticSearch.getData(q);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.first();
    }

    public static <T extends Dto> List<T> getData(T dto, String... locales) throws VantarException {
        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch(QueryOperator.AND);
        QueryResult result = ElasticSearch.getData(q);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asList();
    }

    public static <T extends Dto> List<T> getAll(Dto dto, String... locales) throws VantarException {
        QueryResult result = ElasticSearch.getAllData(dto);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asList();
    }

    public static Map<String, String> getAsKeyValue(Dto dto, String key, String value, String... locales) throws VantarException {
        return ElasticSearch.getAllData(dto).setLocale(locales).asKeyValue(key, value);
    }

    // > > > by QueryBuilder

    public static <T extends Dto> T getFirst(QueryBuilder q, String... locales) throws VantarException {
        QueryResult result = ElasticSearch.getData(q);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.first();
    }

    public static <T extends Dto> List<T> getData(QueryBuilder q, String... locales) throws VantarException {
        QueryResult result = ElasticSearch.getData(q);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asList();
    }

    public static Object search(QueryBuilder q, String... locales) throws VantarException {
        if (q.isPagination()) {
            return ElasticSearch.getPage(q, locales);
        } else {
            QueryResult result = ElasticSearch.getData(q);
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.asList();
        }
    }

    // > > > count exists

    public static long count(QueryBuilder q) throws DatabaseException {
        return ElasticSearch.count(q);
    }

    public static long count(String collectionName) throws DatabaseException {
        return ElasticSearch.count(collectionName);
    }

    public static boolean exists(QueryBuilder q) throws DatabaseException {
        return ElasticSearch.existsById(q);
    }

    public static boolean exists(Dto dto, String property) throws DatabaseException {
        return ElasticSearch.existsById(dto, property);
    }

    public static boolean existsById(Dto dto) throws DatabaseException {
        return ElasticSearch.existsById(dto);
    }
}