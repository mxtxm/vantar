package com.vantar.business;

import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticSearch;
import com.vantar.database.query.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.util.object.ObjectUtil;
import java.util.*;


public class CommonRepoSql extends SqlExecute {

    public CommonRepoSql(SqlConnection connection) {
        this.connection = connection;
    }

    public long insert(Dto dto) throws DatabaseException, VantarException {
        long id = super.insert(dto);
        //ModelCommon.afterDataChange(dto);
        return id;
    }

    public void insert(List<? extends Dto> dtos) throws DatabaseException, VantarException {
        super.insert(dtos);
        if (!dtos.isEmpty()) {
            //ModelCommon.afterDataChange(dtos.get(0));
        }
    }

    public void update(Dto dto) throws DatabaseException, VantarException {
        super.update(dto);
        //ModelCommon.afterDataChange(dto);
    }

    public void update(QueryBuilder q) throws DatabaseException, VantarException {
        super.update(q);
        //ModelCommon.afterDataChange(q.getDto());
    }

    public void delete(Dto dto) throws DatabaseException, VantarException {
        super.delete(dto);
        //ModelCommon.afterDataChange(dto);
    }

    public void createAllDtoIndexes(boolean deleteIfExists) throws DatabaseException {
        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.SQL)) {
            createIndex(info.getDtoInstance(), deleteIfExists);
        }
    }

    public List<ValidationError> getUniqueViolation(Dto dto) throws DatabaseException {
        SqlSearch search = new SqlSearch(connection);

        List<ValidationError> errors = null;
        for (String fieldName : dto.annotatedProperties(Unique.class)) {
            if (search.exists(dto, fieldName)) {
                if (errors == null) {
                    errors = new ArrayList<>();
                }
                errors.add(new ValidationError(fieldName, VantarKey.UNIQUE));
            }
        }
        return errors;
    }

    public void purge(String table) throws DatabaseException {
        execute("DROP TABLE IF EXISTS " + table + " CASCADE;");
    }

    public void purgeData(String table) throws DatabaseException {
        execute("DELETE FROM " + table + " CASCADE;");
    }

    // > > > by dto

    public <T extends Dto> T getById(T dto, String... locales) throws NoContentException, DatabaseException {
        SqlSearch search = new SqlSearch(connection);

        QueryBuilder q = new QueryBuilder(dto);
        q.condition().equal(VantarParam.ID, dto.getId());
        QueryResult result = search.getData(q).first();
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.first();
    }

    public <T extends Dto> T getFirst(T dto, String... locales) throws DatabaseException, NoContentException {
        SqlSearch search = new SqlSearch(connection);

        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch(QueryOperator.AND);
        QueryResult result = search.getData(q);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.first();
    }

    public <T extends Dto> List<T> getData(T dto, String... locales) throws DatabaseException, NoContentException {
        SqlSearch search = new SqlSearch(connection);

        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch(QueryOperator.AND);
        QueryResult result = search.getData(q);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asList();
    }

    public <T extends Dto> List<T> getAll(Dto dto, String... locales) throws NoContentException, DatabaseException {
        QueryResult result = ElasticSearch.getAllData(dto);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asList();
    }

    public Map<String, String> getAsKeyValue(Dto dto, String key, String value, String... locales) throws NoContentException, DatabaseException {
        return new SqlSearch(connection).getAllData(dto).setLocale(locales).asKeyValue(key, value);
    }

    // > > > by QueryBuilder

    public <T extends Dto> T getFirst(QueryBuilder q, String... locales) throws DatabaseException, NoContentException {
        QueryResult result = new SqlSearch(connection).getData(q);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.first();
    }

    public <T extends Dto> List<T> getData(QueryBuilder q, String... locales) throws DatabaseException, NoContentException {
        QueryResult result = new SqlSearch(connection).getData(q);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asList();
    }

    public Object search(QueryBuilder q, String... locales) throws DatabaseException, NoContentException {
        if (q.isPagination()) {
            return new SqlSearch(connection).getPage(q, locales);
        } else {
            QueryResult result = new SqlSearch(connection).getData(q);
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.asList();
        }
    }

    // > > > count exists

    public boolean existsByDto(Dto dto) throws DatabaseException {
        return new SqlSearch(connection).existsByDto(dto);
    }

    public long count(QueryBuilder q) throws DatabaseException {
        return new SqlSearch(connection).count(q);
    }

    public long count(String collectionName) throws DatabaseException {
        return new SqlSearch(connection).count(collectionName);
    }

    public boolean exists(QueryBuilder q) throws DatabaseException {
        return new SqlSearch(connection).exists(q);
    }

    public boolean exists(Dto dto, String property) throws DatabaseException {
        return new SqlSearch(connection).exists(dto, property);
    }

    public boolean existsById(Dto dto) throws DatabaseException {
        return new SqlSearch(connection).existsById(dto);
    }
}