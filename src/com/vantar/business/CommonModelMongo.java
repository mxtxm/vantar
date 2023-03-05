package com.vantar.business;

import com.vantar.common.VantarParam;
import com.vantar.database.common.*;
import com.vantar.database.dependency.DataDependency;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.query.*;
import com.vantar.database.query.data.QueryData;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.service.log.ServiceUserActionLog;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.bson.Document;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class CommonModelMongo extends CommonModel {


    // INSERT > > >


    public static ResponseMessage insert(Params params, Dto dto) throws VantarException {
        return insertX(params, dto, null);
    }
    public static ResponseMessage insert(Params params, Dto dto, WriteEvent event) throws VantarException {
        return insertX(params, dto, event);
    }

    public static ResponseMessage insertJson(Params params, Dto dto) throws VantarException {
        return insertX(params.getJson(), dto, null);
    }
    public static ResponseMessage insertJson(Params params, Dto dto, WriteEvent event) throws VantarException {
        return insertX(params.getJson(), dto, event);
    }
    public static ResponseMessage insertJson(Params params, String key, Dto dto) throws VantarException {
        return insertX(params.getString(key), dto, null);
    }
    public static ResponseMessage insertJson(Params params, String key, Dto dto, WriteEvent event) throws VantarException {
        return insertX(params.getString(key), dto, event);
    }
    public static ResponseMessage insertJson(String json, Dto dto) throws VantarException {
        return insertX(json, dto, null);
    }
    public static ResponseMessage insertJson(String json, Dto dto, WriteEvent event) throws VantarException {
        return insertX(json, dto, event);
    }

    private static synchronized ResponseMessage insertX(Object params, Dto dto, WriteEvent event) throws VantarException {
        if (event != null) {
            try {
                event.beforeSet(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        List<ValidationError> errors = params instanceof String ?
            dto.set((String) params, Dto.Action.INSERT) :
            dto.set((Params) params, Dto.Action.INSERT);

        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }

        if (event != null) {
            try {
                event.beforeWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
            dto.removeNullPropertiesNatural();
        }

        try {
            errors = getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            errors = getRelationViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            Mongo.insert(dto);
            errors = getParentChildViolation(dto);
            if (errors != null) {
                Mongo.delete(dto);
                throw new InputException(errors);
            }
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.INSERT_FAIL);
        }

        CommonModel.afterDataChange(dto);
        if (event != null) {
            try {
                event.afterWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        dto = getByIdX(dto);

        if (Services.isUp(ServiceUserActionLog.class)) {
            ServiceUserActionLog.add(Dto.Action.INSERT, dto);
        }

        return ResponseMessage.success(VantarKey.INSERT_SUCCESS, dto.getId(), dto);
    }

    public static ResponseMessage insert(Dto dto) throws VantarException {
        return insertY(dto, true, null);
    }
    public static synchronized ResponseMessage insert(Dto dto, WriteEvent event) throws VantarException {
        return insertY(dto, true, event);
    }
    public static ResponseMessage insertNoLog(Dto dto) throws VantarException {
        return insertY(dto, false, null);
    }
    public static synchronized ResponseMessage insertNoLog(Dto dto, WriteEvent event) throws VantarException {
        return insertY(dto, false, event);
    }
    public static synchronized ResponseMessage insertY(Dto dto, boolean logEvent, WriteEvent event) throws VantarException {
        if (event != null) {
            try {
                event.beforeSet(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        List<ValidationError> errors = dto.validate(Dto.Action.INSERT);
        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }

        if (event != null) {
            try {
                event.beforeWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
            dto.removeNullPropertiesNatural();
        }

        try {
            errors = getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            errors = getRelationViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            Mongo.insert(dto);
            errors = getParentChildViolation(dto);
            if (errors != null) {
                Mongo.delete(dto);
                throw new InputException(errors);
            }
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.INSERT_FAIL);
        }

        CommonModel.afterDataChange(dto);
        if (event != null) {
            try {
                event.afterWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        if (logEvent) {
            dto = getByIdX(dto);

            if (Services.isUp(ServiceUserActionLog.class)) {
                ServiceUserActionLog.add(Dto.Action.INSERT, dto);
            }
        }

        return ResponseMessage.success(VantarKey.INSERT_SUCCESS, dto.getId(), dto);
    }


    // < < < INSERT



    // UPDATE > > >


    public static ResponseMessage update(Params params, Dto dto) throws VantarException {
        return updateX(params, dto, null, params.getX("action", Dto.Action.UPDATE_FEW_COLS));
    }
    public static ResponseMessage update(Params params, Dto dto, WriteEvent event) throws VantarException {
        return updateX(params, dto, event, params.getX("action", Dto.Action.UPDATE_FEW_COLS));
    }

    public static ResponseMessage updateJson(Params params, Dto dto) throws VantarException {
        return updateX(params.getJson(), dto, null, params.getX("action", Dto.Action.UPDATE_FEW_COLS));
    }
    public static ResponseMessage updateJson(Params params, Dto dto, WriteEvent event) throws VantarException {
        return updateX(params.getJson(), dto, event, params.getX("action", Dto.Action.UPDATE_FEW_COLS));
    }
    public static ResponseMessage updateJson(Params params, String key, Dto dto) throws VantarException {
        return updateX(params.getString(key), dto, null, params.getX("action", Dto.Action.UPDATE_FEW_COLS));
    }
    public static ResponseMessage updateJson(Params params, String key, Dto dto, WriteEvent event)
        throws VantarException {
        return updateX(params.getString(key), dto, event, params.getX("action", Dto.Action.UPDATE_FEW_COLS));
    }
    public static ResponseMessage updateJson(String json, Dto dto) throws VantarException {
        return updateX(json, dto, null, Dto.Action.UPDATE_FEW_COLS);
    }
    public static ResponseMessage updateJson(String json, Dto dto, WriteEvent event) throws VantarException {
        return updateX(json, dto, event, Dto.Action.UPDATE_FEW_COLS);
    }

    private static synchronized ResponseMessage updateX(Object params, Dto dto, WriteEvent event, Dto.Action action)
        throws VantarException {

        if (event != null) {
            try {
                event.beforeSet(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        List<ValidationError> errors = params instanceof String ?
            dto.set((String) params, action) :
            dto.set((Params) params, action);

        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }

        if (event != null) {
            try {
                event.beforeWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
            dto.removeNullPropertiesNatural();
        }

        try {
            errors = getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            errors = getParentChildViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            errors = getRelationViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            Mongo.update(dto);
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.UPDATE_FAIL);
        }

        CommonModel.afterDataChange(dto);
        if (event != null) {
            try {
                event.afterWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        dto = getByIdX(dto);

        if (Services.isUp(ServiceUserActionLog.class)) {
            ServiceUserActionLog.add(action, dto);
        }

        return ResponseMessage.success(VantarKey.UPDATE_SUCCESS, dto);
    }

    public static ResponseMessage update(Dto dto) throws VantarException {
        return updateY(dto, null, Dto.Action.UPDATE_FEW_COLS, true, null);
    }
    public static ResponseMessage update(Dto dto, Dto.Action action) throws VantarException {
        return updateY(dto, null, action, true, null);
    }
    public static ResponseMessage update(Dto dto, WriteEvent event) throws VantarException {
        return updateY(dto, null, Dto.Action.UPDATE_FEW_COLS, true, event);
    }
    public static ResponseMessage update(Dto dto, Dto.Action action, WriteEvent event) throws VantarException {
        return updateY(dto, null, action, true, event);
    }

    public static ResponseMessage update(QueryBuilder q) throws VantarException {
        return updateY(null, q, Dto.Action.UPDATE_FEW_COLS, true, null);
    }
    public static ResponseMessage update(QueryBuilder q, Dto.Action action) throws VantarException {
        return updateY(null, q, action, true, null);
    }
    public static ResponseMessage update(QueryBuilder q, WriteEvent event) throws VantarException {
        return updateY(null, q, Dto.Action.UPDATE_FEW_COLS, true, event);
    }
    public static ResponseMessage update(QueryBuilder q, Dto.Action action, WriteEvent event) throws VantarException {
        return updateY(null, q, action, true, event);
    }

    public static ResponseMessage updateNoLog(Dto dto) throws VantarException {
        return updateY(dto, null, Dto.Action.UPDATE_FEW_COLS, false, null);
    }
    public static ResponseMessage updateNoLog(Dto dto, Dto.Action action) throws VantarException {
        return updateY(dto, null, action, false, null);
    }
    public static ResponseMessage updateNoLog(Dto dto, WriteEvent event) throws VantarException {
        return updateY(dto, null, Dto.Action.UPDATE_FEW_COLS, false, event);
    }
    public static ResponseMessage updateNoLog(Dto dto, Dto.Action action, WriteEvent event) throws VantarException {
        return updateY(dto, null, action, false, event);
    }

    public static ResponseMessage updateNoLog(QueryBuilder q) throws VantarException {
        return updateY(null, q, Dto.Action.UPDATE_FEW_COLS, false, null);
    }
    public static ResponseMessage updateNoLog(QueryBuilder q, Dto.Action action) throws VantarException {
        return updateY(null, q, action, false, null);
    }
    public static ResponseMessage updateNoLog(QueryBuilder q, WriteEvent event) throws VantarException {
        return updateY(null, q, Dto.Action.UPDATE_FEW_COLS, false, event);
    }
    public static ResponseMessage updateNoLog(QueryBuilder q, Dto.Action action, WriteEvent event) throws VantarException {
        return updateY(null, q, action, false, event);
    }

    private static synchronized ResponseMessage updateY(Dto dto, QueryBuilder q, Dto.Action action, boolean logEvent
        , WriteEvent event) throws VantarException {

        if (q != null) {
            dto = q.getDto();
            action = action.equals(Dto.Action.UPDATE_FEW_COLS) ? Dto.Action.UPDATE_FEW_COLS_NO_ID : Dto.Action.UPDATE_ALL_COLS_NO_ID;
        }
        if (event != null) {
            try {
                event.beforeSet(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        List<ValidationError> errors = dto.validate(action);
        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }

        if (event != null) {
            try {
                event.beforeWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
            dto.removeNullPropertiesNatural();
        }

        try {
            errors = getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            errors = getParentChildViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            errors = getRelationViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            if (q == null) {
                Mongo.update(dto);
            } else {
                Mongo.update(q);
            }
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.UPDATE_FAIL);
        }

        CommonModel.afterDataChange(dto);
        if (event != null) {
            try {
                event.afterWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        if (logEvent) {
            if (q == null) {
                dto = getByIdX(dto);
            }

            if (Services.isUp(ServiceUserActionLog.class)) {
                ServiceUserActionLog.add(action, dto);
            }
        }

        return ResponseMessage.success(VantarKey.UPDATE_SUCCESS, dto);
    }


    // < < < UPDATE



    // DELETE > > >


    public static ResponseMessage delete(Params params, Dto dto) throws VantarException {
        return delete(params, dto, null);
    }
    public static ResponseMessage delete(Params params, Dto dto, WriteEvent event) throws VantarException {
        if (event != null) {
            try {
                event.beforeSet(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        List<ValidationError> errors = dto.set(params, Dto.Action.DELETE);
        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }

        if (event != null) {
            try {
                event.beforeWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        if (NumberUtil.isIdValid(dto.getId())) {
            List<DataDependency.Dependants> items = new DataDependency(dto).getDependencies();
            if (!items.isEmpty()) {
                return ResponseMessage.success(VantarKey.DELETE_FAIL_HAS_DEPENDENCIES, items);
            }
        }

        try {
            ResponseMessage r = ResponseMessage.success(VantarKey.DELETE_SUCCESS, Mongo.delete(dto));
            CommonModel.afterDataChange(dto);
            if (event != null) {
                try {
                    event.afterWrite(dto);
                } catch (NoContentException e) {
                    throw new InputException(VantarKey.NO_CONTENT);
                }
            }

            if (Services.isUp(ServiceUserActionLog.class)) {
                ServiceUserActionLog.add(Dto.Action.DELETE, dto);
            }

            return r;
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.DELETE_FAIL);
        }
    }

    public static ResponseMessage deleteById(Dto dto) throws VantarException {
        return deleteX(dto, null, true, null);
    }
    public static ResponseMessage deleteById(Dto dto, WriteEvent event) throws VantarException {
        return deleteX(dto, null, true, event);
    }

    public static ResponseMessage delete(QueryBuilder q) throws VantarException {
        return deleteX(null, q, true, null);
    }
    public static ResponseMessage delete(QueryBuilder q, WriteEvent event) throws VantarException {
        return deleteX(null, q, true, event);
    }

    public static ResponseMessage deleteByIdNoLog(Dto dto) throws VantarException {
        return deleteX(dto, null, false, null);
    }
    public static ResponseMessage deleteByIdNoLog(Dto dto, WriteEvent event) throws VantarException {
        return deleteX(dto, null, false, event);
    }

    public static ResponseMessage deleteNoLog(QueryBuilder q) throws VantarException {
        return deleteX(null, q, false, null);
    }
    public static ResponseMessage deleteNoLog(QueryBuilder q, WriteEvent event) throws VantarException {
        return deleteX(null, q, false, event);
    }

    public static ResponseMessage deleteX(Dto dto, QueryBuilder q, boolean logEvent, WriteEvent event) throws VantarException {
        if (q != null) {
            dto = q.getDto();
        }
        if (event != null) {
            try {
                event.beforeSet(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        if (q == null) {
            List<ValidationError> errors = dto.validate(Dto.Action.DELETE);
            if (ObjectUtil.isNotEmpty(errors)) {
                throw new InputException(errors);
            }
        }

        if (event != null) {
            try {
                event.beforeWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        if (NumberUtil.isIdValid(dto.getId())) {
            List<DataDependency.Dependants> items = new DataDependency(dto).getDependencies();
            if (!items.isEmpty()) {
                return ResponseMessage.success(VantarKey.DELETE_FAIL_HAS_DEPENDENCIES, items);
            }
        }

        try {
            ResponseMessage r = ResponseMessage.success(
                VantarKey.DELETE_SUCCESS,
                q == null ? Mongo.delete(dto) : Mongo.delete(q)
            );
            CommonModel.afterDataChange(dto);
            if (event != null) {
                try {
                    event.afterWrite(dto);
                } catch (NoContentException e) {
                    throw new InputException(VantarKey.NO_CONTENT);
                }
            }

            if (logEvent && Services.isUp(ServiceUserActionLog.class)) {
                ServiceUserActionLog.add(Dto.Action.DELETE, dto);
            }

            return r;
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.DELETE_FAIL);
        }
    }

    public static <T extends Dto> ResponseMessage deleteBatch(Params params, Class<T> tClass) throws VantarException {
        return deleteBatch(params, tClass, null);
    }
    public static <T extends Dto> ResponseMessage deleteBatch(Params params, Class<T> tClass, WriteEvent event)
        throws VantarException {

        List<Long> ids = params.getLongList("ids");
        if (ids == null || ids.isEmpty()) {
            ids = params.getJsonList(Long.class);
        }
        if (ids == null || ids.isEmpty()) {
            throw new InputException(VantarKey.INVALID_JSON_DATA);
        }

        T dto = ClassUtil.getInstance(tClass);
        if (dto == null) {
            throw new InputException(VantarKey.INVALID_JSON_DATA);
        }

        try {
            for (Long id : ids) {
                if (event != null) {
                    try {
                        event.beforeSet(dto);
                    } catch (NoContentException e) {
                        throw new InputException(VantarKey.NO_CONTENT);
                    }
                }

                dto.setId(id);

                if (NumberUtil.isIdValid(id)) {
                    List<DataDependency.Dependants> items = new DataDependency(dto).getDependencies();
                    if (!items.isEmpty()) {
                        return ResponseMessage.success(VantarKey.DELETE_FAIL_HAS_DEPENDENCIES, items);
                    }
                }

                if (event != null) {
                    try {
                        event.beforeWrite(dto);
                    } catch (NoContentException e) {
                        throw new InputException(VantarKey.NO_CONTENT);
                    }
                }

                Mongo.delete(dto);
                CommonModel.afterDataChange(dto);

                if (Services.isUp(ServiceUserActionLog.class)) {
                    ServiceUserActionLog.add(Dto.Action.DELETE, dto);
                }
            }

            if (!ids.isEmpty()) {
                afterDataChange(dto);
            }
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.DELETE_FAIL);
        }

        return ResponseMessage.success(VantarKey.DELETE_SUCCESS, ids.size());
    }

    public static <T extends Dto> ResponseMessage unDeleteBatch(Params params, Class<T> tClass) throws VantarException {
        return unDeleteBatch(params, tClass, null);
    }
    public static <T extends Dto> ResponseMessage unDeleteBatch(Params params, Class<T> tClass, WriteEvent event)
        throws VantarException {

        List<Long> ids = params.getLongList("ids");
        if (ids == null || ids.isEmpty()) {
            ids = params.getJsonList(Long.class);
        }
        if (ids == null || ids.isEmpty()) {
            throw new InputException(VantarKey.INVALID_JSON_DATA);
        }

        T dto = ClassUtil.getInstance(tClass);
        if (dto == null) {
            throw new InputException(VantarKey.INVALID_JSON_DATA);
        }

        try {
            for (Long id : ids) {
                if (event != null) {
                    try {
                        event.beforeSet(dto);
                    } catch (NoContentException e) {
                        throw new InputException(VantarKey.NO_CONTENT);
                    }
                }

                dto.setId(id);

                if (event != null) {
                    try {
                        event.beforeWrite(dto);
                    } catch (NoContentException e) {
                        throw new InputException(VantarKey.NO_CONTENT);
                    }
                }

                Mongo.unset(dto.getStorage(), dto.getId(), Mongo.LOGICAL_DELETE_FIELD);

                afterDataChange(dto);

                if (Services.isUp(ServiceUserActionLog.class)) {
                    ServiceUserActionLog.add(Dto.Action.UN_DELETE, dto);
                }
            }

            if (!ids.isEmpty()) {
                afterDataChange(dto);
            }

        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.UPDATE_FAIL);
        }

        return ResponseMessage.success(VantarKey.UPDATE_MANY_SUCCESS, ids.size());
    }


    // < < < DELETE



    // PURGE > > >


    public static ResponseMessage purge(Dto dto) throws VantarException {
        try {
            String collection = dto.getStorage();
            Mongo.deleteAll(collection);
            Mongo.Sequence.remove(collection);
            Mongo.Index.remove(collection);
            afterDataChange(dto);
            if (Services.isUp(ServiceUserActionLog.class)) {
                ServiceUserActionLog.add(Dto.Action.PURGE, dto);
            }
            return ResponseMessage.success(VantarKey.DELETE_SUCCESS);
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.DELETE_FAIL);
        }
    }


    // < < < PURGE



    // GET DATA > > >



    public static void forEach(Dto dto, QueryResultBase.Event event) throws VantarException {
        try {
            MongoSearch.getAllData(dto).forEach(event);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static void forEach(QueryBuilder q, QueryResultBase.Event event) throws VantarException {
        try {
            MongoSearch.getData(q).forEach(event);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static PageData search(Params params, Dto dto) throws VantarException {
        return search(params, dto, dto, null);
    }

    public static PageData search(Params params, Dto dto, QueryEvent event) throws VantarException {
        return search(params, dto, dto, event);
    }

    public static PageData search(Params params, Dto dto, Dto dtoView) throws VantarException {
        return search(params, dto, dtoView, null);
    }

    public static PageData search(Params params, Dto dto, Dto dtoView, QueryEvent event) throws VantarException {
        QueryData queryData = params.getQueryData();
        if (queryData == null) {
            if (!SEARCH_POLICY_ALLOW_EMPTY_CONDITION) {
                throw new InputException(VantarKey.NO_SEARCH_COMMAND);
            }
            if (event == null) {
                return new PageData(getAll(params, dtoView, null, null));
            }
            queryData = new QueryData();
        }

        queryData.setDto(dto, dtoView);

        QueryBuilder q = new QueryBuilder(queryData);
        List<ValidationError> errors = q.getErrors();
        if (ObjectUtil.isNotEmpty(errors) && SEARCH_POLICY_THROW_ON_CONDITION_ERROR) {
            throw new InputException(errors);
        }

        if (event != null) {
            event.beforeQuery(q);
        }

        return search(q, event, params.getLang());
    }

    public static PageData searchForEach(Params params, Dto dto, Dto dtoView, QueryEvent event) throws VantarException {
        QueryData queryData = params.getQueryData();
        queryData.setDto(dto, dtoView);

        QueryBuilder q = new QueryBuilder(queryData);
        List<ValidationError> errors = q.getErrors();
        if (ObjectUtil.isNotEmpty(errors) && SEARCH_POLICY_THROW_ON_CONDITION_ERROR) {
            throw new InputException(errors);
        }

        if (event != null) {
            event.beforeQuery(q);
        }

        return searchForEach(q, event, params.getLang());
    }

    public static PageData search(QueryBuilder q, String... locales) throws VantarException {
        return search(q, null, locales);
    }

    public static PageData search(QueryBuilder q, QueryEvent event, String... locales) throws VantarException {
        if (q.isPagination()) {
            try {
                return MongoSearch.getPage(q, event, locales);
            } catch (DatabaseException e) {
                throw new ServerException(VantarKey.FETCH_FAIL);
            }
        } else {
            QueryResult result;
            try {
                result = MongoSearch.getData(q);
            } catch (DatabaseException e) {
                throw new ServerException(VantarKey.FETCH_FAIL);
            }
            if (event != null) {
                result.setEvent(event);
            }
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            try {
                return new PageData(result.asList());
            } catch (DatabaseException e) {
                throw new ServerException(VantarKey.FETCH_FAIL);
            }
        }
    }

    public static PageData searchForEach(QueryBuilder q, QueryResultBase.Event event, String... locales) throws VantarException {
        try {
            return MongoSearch.getPageForeach(q, event, locales);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <D extends Dto> List<D> getData(QueryBuilder q, String... locales) throws VantarException {
        return getData(q, null, locales);
    }

    public static <D extends Dto> List<D> getData(Params params, QueryBuilder q) throws VantarException {
        return getData(params, q, null);
    }

    public static <D extends Dto> List<D> getData(Params params, QueryBuilder q, QueryEvent event) throws VantarException {
        return getData(q, event, params.getLang());
    }

    public static <D extends Dto> List<D> getData(QueryBuilder q, QueryEvent event, String... locales) throws VantarException {
        try {
            QueryResult result = MongoSearch.getData(q);
            if (event != null) {
                result.setEvent(event);
            }
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.asList();
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <D extends Dto> Map<Object, D> getMap(QueryBuilder q, String keyProperty, String... locales)
        throws VantarException {

        return getMap(q, keyProperty, null, locales);
    }

    public static <D extends Dto> Map<Object, D> getMap(Params params, String keyProperty, QueryBuilder q)
        throws VantarException {

        return getMap(params, keyProperty, q, null);
    }

    public static <D extends Dto> Map<Object, D> getMap(Params params, String keyProperty, QueryBuilder q, QueryEvent event)
        throws VantarException {

        return getMap(q, keyProperty, event, params.getLang());
    }

    public static <D extends Dto> Map<Object, D> getMap(QueryBuilder q, String keyProperty, QueryEvent event, String... locales)
        throws VantarException {

        try {
            QueryResult result = MongoSearch.getData(q);
            if (event != null) {
                result.setEvent(event);
            }
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.asMap(keyProperty);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <D extends Dto> Map<Object, D> getMap(Dto dto, String keyProperty, String... locales) throws VantarException {
        try {
            QueryResult result = MongoSearch.getAllData(dto);
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.asMap(keyProperty);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <D extends Dto> D getFirst(QueryBuilder q, String... locales) throws VantarException {
        try {
            QueryResult result = MongoSearch.getData(q);
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.first();
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <D extends Dto> D getFirst(Params params, QueryBuilder q) throws VantarException {
        String locales = params.getLang();
        try {
            QueryResult result = MongoSearch.getData(q);
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.first();
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    @SuppressWarnings("unchecked")
    public static <D extends Dto> D getById(Params params, D dto) throws VantarException {
        Long id = params.getLong("id");
        if (id == null) {
            id = params.extractFromJson("id", Long.class);
        }
        if (!NumberUtil.isIdValid(id)) {
            throw new InputException(VantarKey.INVALID_ID, dto.getClass().getSimpleName() + ".id");
        }
        dto.setId(id);
        if (dto.hasAnnotation(Cache.class)) {
            D d = (D) Services.get(ServiceDtoCache.class).getMap(dto.getClass()).get(id);
            if (d == null) {
                throw new NoContentException();
            }
            return d;
        }
        return getByIdX(dto, params.getLang());
    }

    @SuppressWarnings("unchecked")
    public static <D extends Dto> D getById(D dto, String... locales) throws VantarException {
        if (!NumberUtil.isIdValid(dto.getId())) {
            throw new InputException(VantarKey.INVALID_ID, dto.getClass().getSimpleName() + ".id");
        }
        if (dto.hasAnnotation(Cache.class)) {
            D d = (D) Services.get(ServiceDtoCache.class).getMap(dto.getClass()).get(dto.getId());
            if (d == null) {
                throw new NoContentException();
            }
            return d;
        }
        return getByIdX(dto, locales);
    }

    private static <T extends Dto> T getByIdX(T dto, String... locales) throws VantarException {
        try {
            QueryResult result =
                new MongoQueryResult(
                    MongoConnection.getDatabase()
                        .getCollection(dto.getStorage())
                        .find(new Document(Mongo.ID, dto.getId())),
                    dto
                );
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.first();
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <T extends Dto> List<T> getAll(T dto) throws VantarException {
        return getAll(null, dto, null, null);
    }

    public static <T extends Dto> List<T> getAll(T dto, QueryEvent event) throws VantarException {
        return getAll(null, dto, event, null);
    }

    public static <T extends Dto> List<T> getAll(T dto, String... locales) throws VantarException {
        return getAll(null, dto, null, locales);
    }

    public static <T extends Dto> List<T> getAll(T dto, QueryEvent event, String... locales) throws VantarException {
        return getAll(null, dto, event, locales);
    }

    /**
     * Automatically fetches: cache
     * lang only for database
     */
    public static <T extends Dto> List<T> getAll(Params params, T dto) throws VantarException {
        return getAll(params, dto, null, null);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Dto> List<T> getAll(Params params, T dto, QueryEvent event, String[] locales)
        throws VantarException {

        if (dto.hasAnnotation(Cache.class)) {
            List<T> d = (List<T>) Services.get(ServiceDtoCache.class).getList(dto.getClass());
            if (d == null) {
                throw new NoContentException();
            }
            return d;
        }
        try {
            QueryResult result = MongoSearch.getAllData(dto);
            if (event != null) {
                result.setEvent(event);
            }
            if (locales != null && locales.length > 0) {
                result.setLocale(locales);
            } else {
                String lang = params == null ? null : params.getLang();
                if (StringUtil.isNotEmpty(lang)) {
                    result.setLocale(lang);
                }
            }
            return result.asList();
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    /**
     * Automatically fetches: cache, lang
     */
    public static <D extends Dto, L extends Dto> List<L> getAllFromCache(
        Params params, Class<D> dto, Class<L> localizedDtoClass) throws VantarException {

        ServiceDtoCache cache = Services.get(ServiceDtoCache.class);

        String lang = params.getLang();
        List<L> localized = new ArrayList<>();

        for (Dto d : cache.getList(dto)) {
            L localizedDto;
            try {
                localizedDto = localizedDtoClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.warn(" ! ({}, {}) < {}\n", dto, localizedDtoClass, d, e);
                continue;
            }
            localizedDto.set(d, lang);
            localized.add(localizedDto);
        }
        if (localized.isEmpty()) {
            throw new NoContentException();
        }
        return localized;
    }

    /**
     * Automatically fetches: cache, lang
     */
    public static Map<String, String> getAsKeyValue(Params params, Dto dto, String keyProperty, String valueProperty)
        throws VantarException {

        String lang = params.getLangNoDefault();
        if (dto.hasAnnotation(Cache.class)) {
            Map<String, String> result = new LinkedHashMap<>();
            for (Dto d : Services.get(ServiceDtoCache.class).getList(dto.getClass())) {
                Object k = d.getPropertyValue(keyProperty);
                if (k == null) {
                    continue;
                }

                result.put(
                    DbUtil.getKv(k, lang),
                    DbUtil.getKv(d.getPropertyValue(valueProperty), lang)
                );
            }

            if (result.isEmpty()) {
                throw new NoContentException();
            }
            return result;

        } else {
            try {
                QueryResult result = MongoSearch.getAllData(dto);
                if (StringUtil.isNotEmpty(lang)) {
                    result.setLocale(lang);
                }
                return result.asKeyValue(keyProperty, valueProperty);
            } catch (DatabaseException e) {
                throw new ServerException(VantarKey.FETCH_FAIL);
            }
        }
    }



    // > > > count exists


    public static long count(QueryBuilder q) throws VantarException {
        try {
            return MongoSearch.count(q);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static long count(String collectionName) throws VantarException {
        try {
            return MongoSearch.count(collectionName);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static boolean exists(QueryBuilder q) throws VantarException {
        try {
            return MongoSearch.exists(q);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static boolean exists(Dto dto, String property) throws VantarException {
        try {
            return MongoSearch.exists(dto, property);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static boolean existsById(Dto dto) throws VantarException {
        try {
            return MongoSearch.existsById(dto);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static boolean existsByDto(Dto dto) throws ServerException {
        try {
            return MongoSearch.existsByDto(dto);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }



    // > > > checkings



    public static List<ValidationError> getUniqueViolation(Dto dto) throws DatabaseException {
        List<ValidationError> errors = null;
        for (String fieldName : dto.annotatedProperties(Unique.class)) {
            if (!MongoSearch.isUnique(dto, fieldName)) {
                if (errors == null) {
                    errors = new ArrayList<>(5);
                }
                errors.add(new ValidationError(fieldName, VantarKey.UNIQUE));
            }
        }
        if (dto.getClass().isAnnotationPresent(UniqueGroup.class)) {
            for (String group : dto.getClass().getAnnotation(UniqueGroup.class).value()) {
                if (!MongoSearch.isUnique(dto, StringUtil.split(group, VantarParam.SEPARATOR_COMMON))) {
                    if (errors == null) {
                        errors = new ArrayList<>(5);
                    }
                    errors.add(new ValidationError("(" + group + ")", VantarKey.UNIQUE));
                }
            }
        }

        return errors;
    }

    public static List<ValidationError> getRelationViolation(Dto dto) {
        List<ValidationError> errors = new ArrayList<>();
        for (String name : dto.annotatedProperties(Depends.class)) {
            Object value = dto.getPropertyValue(name);
            if (value != null) {
                if (value instanceof Long) {
                    checkRelation(dto, name, (Long) value, errors);
                } else if (ClassUtil.isInstantiable(value.getClass(), Collection.class)) {
                    if (ClassUtil.getGenericTypes(dto.getField(name))[0].equals(Long.class)) {
                        for (Long v : (Collection<Long>) value) {
                            checkRelation(dto, name, v, errors);
                        }
                    }
                }
            }
        }
        return errors.isEmpty() ? null : errors;
    }

    private static void checkRelation(Dto dto, String name, Long value, List<ValidationError> errors) {
        DtoDictionary.Info info = DtoDictionary.get(dto.getAnnotation(name, Depends.class).value());
        if (info == null) {
            return;
        }

        Dto dtoD = info.getDtoInstance();
        if (dtoD == null) {
            return;
        }

        QueryBuilder q = new QueryBuilder(dtoD);
        q.condition().equal(Dto.ID, value);

        try {
            if (!MongoSearch.exists(q)) {
                errors.add(new ValidationError(name, VantarKey.REFERENCE, value));
            }
        } catch (DatabaseException e) {
            log.error(" !! could not check reference dto={} field={} value={}", dto, name, value, e);
        }
    }

    public static List<ValidationError> getParentChildViolation(Dto dto) {
        Long id = dto.getId();
        if (id == null) {
            return null;
        }
        List<ValidationError> errors = null;
        for (String name : dto.annotatedProperties(Depends.class)) {
            if (dto.getAnnotation(name, Depends.class).value().equals(dto.getClass())) {
                Object value = dto.getPropertyValue(name);
                if (value != null && value.equals(id)) {
                    if (errors == null) {
                        errors = new ArrayList<>();
                    }
                    errors.add(new ValidationError(name, VantarKey.UNIQUE));
                }
            }
        }
        return errors;
    }

}