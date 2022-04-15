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
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.service.log.ServiceUserActionLog;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.slf4j.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class CommonModelMongo extends CommonModel {

    private static final Logger log = LoggerFactory.getLogger(CommonModelMongo.class);
    public static boolean SEARCH_POLICY_ALLOW_EMPTY_CONDITION = true;
    public static boolean SEARCH_POLICY_THROW_ON_CONDITION_ERROR = true;


    // INSERT > > >


    public static ResponseMessage insert(Params params, Dto dto) throws InputException, ServerException {
        return insertX(params, dto, null, null);
    }
    public static ResponseMessage insert(Params params, Dto dto, WriteEvent event) throws InputException, ServerException {
        return insertX(params, dto, event, null);
    }

    public static ResponseMessage insertJson(Params params, Dto dto) throws InputException, ServerException {
        return insertX(params.getJson(), dto, null, null);
    }
    public static ResponseMessage insertJson(Params params, Dto dto, WriteEvent event) throws InputException, ServerException {
        return insertX(params.getJson(), dto, event, null);
    }
    public static ResponseMessage insertJson(Params params, String key, Dto dto) throws InputException, ServerException {
        return insertX(params.getString(key), dto, null, params);
    }
    public static ResponseMessage insertJson(Params params, String key, Dto dto, WriteEvent event) throws InputException, ServerException {
        return insertX(params.getString(key), dto, event, params);
    }
    public static ResponseMessage insertJson(String json, Dto dto) throws InputException, ServerException {
        return insertX(json, dto, null, null);
    }
    public static ResponseMessage insertJson(String json, Dto dto, WriteEvent event) throws InputException, ServerException {
        return insertX(json, dto, event, null);
    }

    private static ResponseMessage insertX(Object params, Dto dto, WriteEvent event, Params requestParams) throws InputException, ServerException {
        if (event != null) {
            event.beforeSet(dto);
        }

        List<ValidationError> errors = params instanceof String ?
            dto.set((String) params, Dto.Action.INSERT) :
            dto.set((Params) params, Dto.Action.INSERT);

        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }

        if (event != null) {
            event.beforeWrite(dto);
            dto.removeNullPropertiesNatural();
        }

        try {
            errors = CommonRepoMongo.getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            errors = CommonRepoMongo.getRelationViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            Mongo.insert(dto);
            errors = CommonRepoMongo.getParentChildViolation(dto);
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
            event.afterWrite(dto);
        }

        try {
            dto = CommonRepoMongo.getById(dto);
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
        } catch (NoContentException e) {
            throw new ServerException(VantarKey.INSERT_FAIL);
        }

        ServiceUserActionLog.add(Dto.Action.INSERT, dto);

        return ResponseMessage.success(VantarKey.INSERT_SUCCESS, dto.getId(), dto);
    }

    public static ResponseMessage insert(Dto dto) throws ServerException, InputException {
        return insert(dto, null);
    }
    public static ResponseMessage insert(Dto dto, WriteEvent event) throws ServerException, InputException {
        if (event != null) {
            event.beforeSet(dto);
        }

        List<ValidationError> errors = dto.validate(Dto.Action.INSERT);
        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }

        if (event != null) {
            event.beforeWrite(dto);
            dto.removeNullPropertiesNatural();
        }

        try {
            errors = CommonRepoMongo.getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            errors = CommonRepoMongo.getRelationViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            Mongo.insert(dto);
            errors = CommonRepoMongo.getParentChildViolation(dto);
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
            event.afterWrite(dto);
        }

        try {
            dto = CommonRepoMongo.getById(dto);
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
        } catch (NoContentException e) {
            throw new ServerException(VantarKey.INSERT_FAIL);
        }

        ServiceUserActionLog.add(Dto.Action.INSERT, dto);

        return ResponseMessage.success(VantarKey.INSERT_SUCCESS, dto.getId(), dto);
    }


    // < < < INSERT



    // UPDATE > > >


    public static ResponseMessage update(Params params, Dto dto) throws InputException, ServerException {
        return updateX(params, dto, null, params.getX("action", Dto.Action.UPDATE_FEW_COLS));
    }
    public static ResponseMessage update(Params params, Dto dto, WriteEvent event) throws InputException, ServerException {
        return updateX(params, dto, event, params.getX("action", Dto.Action.UPDATE_FEW_COLS));
    }

    public static ResponseMessage updateJson(Params params, Dto dto) throws InputException, ServerException {
        return updateX(params.getJson(), dto, null, params.getX("action", Dto.Action.UPDATE_FEW_COLS));
    }
    public static ResponseMessage updateJson(Params params, Dto dto, WriteEvent event) throws InputException, ServerException {
        return updateX(params.getJson(), dto, event, params.getX("action", Dto.Action.UPDATE_FEW_COLS));
    }
    public static ResponseMessage updateJson(Params params, String key, Dto dto) throws InputException, ServerException {
        return updateX(params.getString(key), dto, null, params.getX("action", Dto.Action.UPDATE_FEW_COLS));
    }
    public static ResponseMessage updateJson(Params params, String key, Dto dto, WriteEvent event) throws InputException, ServerException {
        return updateX(params.getString(key), dto, event, params.getX("action", Dto.Action.UPDATE_FEW_COLS));
    }
    public static ResponseMessage updateJson(String json, Dto dto) throws InputException, ServerException {
        return updateX(json, dto, null, Dto.Action.UPDATE_FEW_COLS);
    }
    public static ResponseMessage updateJson(String json, Dto dto, WriteEvent event) throws InputException, ServerException {
        return updateX(json, dto, event, Dto.Action.UPDATE_FEW_COLS);
    }

    private static ResponseMessage updateX(Object params, Dto dto, WriteEvent event, Dto.Action action)
        throws InputException, ServerException {
        if (event != null) {
            event.beforeSet(dto);
        }

        List<ValidationError> errors = params instanceof String ?
            dto.set((String) params, action) :
            dto.set((Params) params, action);

        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }

        if (event != null) {
            event.beforeWrite(dto);
            dto.removeNullPropertiesNatural();
        }

        try {
            errors = CommonRepoMongo.getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            errors = CommonRepoMongo.getParentChildViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            errors = CommonRepoMongo.getRelationViolation(dto);
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
            event.afterWrite(dto);
        }

        try {
            dto = CommonRepoMongo.getById(dto);
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
        } catch (NoContentException e) {
            throw new ServerException(VantarKey.UPDATE_FAIL);
        }

        ServiceUserActionLog.add(action, dto);

        return ResponseMessage.success(VantarKey.UPDATE_SUCCESS, dto);
    }

    public static ResponseMessage update(Dto dto) throws ServerException, InputException {
        return updateX(dto, null, Dto.Action.UPDATE_FEW_COLS, null);
    }
    public static ResponseMessage update(Dto dto, Dto.Action action) throws ServerException, InputException {
        return updateX(dto, null, action, null);
    }
    public static ResponseMessage update(Dto dto, WriteEvent event) throws ServerException, InputException {
        return updateX(dto, null, Dto.Action.UPDATE_FEW_COLS, event);
    }
    public static ResponseMessage update(Dto dto, Dto.Action action, WriteEvent event) throws ServerException, InputException {
        return updateX(dto, null, action, event);
    }

    public static ResponseMessage update(QueryBuilder q) throws ServerException, InputException {
        return updateX(null, q, Dto.Action.UPDATE_FEW_COLS, null);
    }
    public static ResponseMessage update(QueryBuilder q, Dto.Action action) throws ServerException, InputException {
        return updateX(null, q, action, null);
    }
    public static ResponseMessage update(QueryBuilder q, WriteEvent event) throws ServerException, InputException {
        return updateX(null, q, Dto.Action.UPDATE_FEW_COLS, event);
    }
    public static ResponseMessage update(QueryBuilder q, Dto.Action action, WriteEvent event) throws ServerException, InputException {
        return updateX(null, q, action, event);
    }

    private static ResponseMessage updateX(Dto dto, QueryBuilder q, Dto.Action action, WriteEvent event) throws ServerException, InputException {
        if (q != null) {
            dto = q.getDto();
        }
        if (event != null) {
            event.beforeSet(dto);
        }

        List<ValidationError> errors = dto.validate(action);
        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }

        if (event != null) {
            event.beforeWrite(dto);
            dto.removeNullPropertiesNatural();
        }

        try {
            errors = CommonRepoMongo.getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            errors = CommonRepoMongo.getParentChildViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }
            errors = CommonRepoMongo.getRelationViolation(dto);
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
            event.afterWrite(dto);
        }

        try {
            dto = CommonRepoMongo.getById(dto);
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
        } catch (NoContentException e) {
            throw new ServerException(VantarKey.UPDATE_FAIL);
        }

        ServiceUserActionLog.add(action, dto);

        return ResponseMessage.success(VantarKey.UPDATE_SUCCESS, dto);
    }


    // < < < UPDATE



    // DELETE > > >


    public static ResponseMessage delete(Params params, Dto dto) throws InputException, ServerException {
        return delete(params, dto, null);
    }
    public static ResponseMessage delete(Params params, Dto dto, WriteEvent event) throws InputException, ServerException {
        if (event != null) {
            event.beforeSet(dto);
        }

        List<ValidationError> errors = dto.set(params, Dto.Action.DELETE);
        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }

        if (event != null) {
            event.beforeWrite(dto);
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
                event.afterWrite(dto);
            }

            ServiceUserActionLog.add(Dto.Action.DELETE, dto);

            return r;
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.DELETE_FAIL);
        }
    }

    public static ResponseMessage deleteById(Dto dto) throws InputException, ServerException {
        return deleteX(dto, null, null);
    }
    public static ResponseMessage deleteById(Dto dto, WriteEvent event) throws InputException, ServerException {
        return deleteX(dto, null, event);
    }

    public static ResponseMessage delete(QueryBuilder q) throws InputException, ServerException {
        return deleteX(null, q, null);
    }
    public static ResponseMessage delete(QueryBuilder q, WriteEvent event) throws InputException, ServerException {
        return deleteX(null, q, event);
    }

    public static ResponseMessage deleteX(Dto dto, QueryBuilder q, WriteEvent event) throws InputException, ServerException {
        if (q != null) {
            dto = q.getDto();
        }
        if (event != null) {
            event.beforeSet(dto);
        }

        if (q == null) {
            List<ValidationError> errors = dto.validate(Dto.Action.DELETE);
            if (ObjectUtil.isNotEmpty(errors)) {
                throw new InputException(errors);
            }
        }

        if (event != null) {
            event.beforeWrite(dto);
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
                event.afterWrite(dto);
            }

            ServiceUserActionLog.add(Dto.Action.DELETE, dto);

            return r;
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.DELETE_FAIL);
        }
    }

    public static <T extends Dto> ResponseMessage deleteBatch(Params params, Class<T> tClass)
        throws InputException, ServerException {

        return deleteBatch(params, tClass, null);
    }
    public static <T extends Dto> ResponseMessage deleteBatch(Params params, Class<T> tClass, WriteEvent event)
        throws InputException, ServerException {

        List<Long> ids = params.getLongList(VantarParam.IDS);
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
                    event.beforeSet(dto);
                }

                dto.setId(id);

                if (NumberUtil.isIdValid(id)) {
                    List<DataDependency.Dependants> items = new DataDependency(dto).getDependencies();
                    if (!items.isEmpty()) {
                        return ResponseMessage.success(VantarKey.DELETE_FAIL_HAS_DEPENDENCIES, items);
                    }
                }

                if (event != null) {
                    event.beforeWrite(dto);
                }

                CommonRepoMongo.delete(dto);

                ServiceUserActionLog.add(Dto.Action.DELETE, dto);
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

    public static <T extends Dto> ResponseMessage unDeleteBatch(Params params, Class<T> tClass)
        throws InputException, ServerException {

        return unDeleteBatch(params, tClass, null);
    }
    public static <T extends Dto> ResponseMessage unDeleteBatch(Params params, Class<T> tClass, WriteEvent event)
        throws InputException, ServerException {

        List<Long> ids = params.getLongList(VantarParam.IDS);
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
                    event.beforeSet(dto);
                }

                dto.setId(id);

                if (event != null) {
                    event.beforeWrite(dto);
                }

                CommonRepoMongo.unset(dto, Mongo.LOGICAL_DELETE_FIELD);
                afterDataChange(dto);

                ServiceUserActionLog.add(Dto.Action.UN_DELETE, dto);
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


    public static ResponseMessage purge(Dto dto) throws ServerException {
        try {
            String collection = dto.getStorage();
            Mongo.deleteAll(collection);
            Mongo.Sequence.remove(collection);
            Mongo.Index.remove(collection);
            afterDataChange(dto);
            ServiceUserActionLog.add(Dto.Action.PURGE, dto);
            return ResponseMessage.success(VantarKey.DELETE_SUCCESS);
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.DELETE_FAIL);
        }
    }


    // < < < PURGE



    // ADMIN DASHBOARD > > >


    public static void importDataAdmin(String data, Dto dto, List<String> presentField, boolean deleteAll, WebUi ui) {
        ServiceUserActionLog.add(Dto.Action.IMPORT, dto);

        ui.beginBox2(dto.getClass().getSimpleName()).write();

        if (deleteAll) {
            try {
                CommonRepoMongo.purge(dto.getStorage());
            } catch (DatabaseException e) {
                ui.addErrorMessage(e);
                log.error(" !! {} : {} > {}\n", dto.getClass().getSimpleName(), dto, data, e);
                return;
            }
            ui.addPre(Locale.getString(VantarKey.DELETE_SUCCESS)).write();
        }

        AtomicInteger failed = new AtomicInteger();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();

        CommonModel.Import imp = (String presentValue) -> {
            try {
                if (dto.getId() == null ? CommonRepoMongo.existsByDto(dto) : CommonRepoMongo.existsById(dto)) {
                    duplicate.getAndIncrement();
                    return;
                }

                CommonRepoMongo.insert(dto);

                success.getAndIncrement();
            } catch (DatabaseException e) {
                ui.addErrorMessage(presentValue + " " + Locale.getString(VantarKey.IMPORT_FAIL));
                failed.getAndIncrement();
            }
        };

        importDataX(imp, data, dto, presentField, ui);
        long max;
        try {
            max = Mongo.Sequence.setToMax(dto);
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
            max = 0;
        }

        ui.addPre(
            Locale.getString(VantarKey.BUSINESS_WRITTEN_COUNT, success) + "\n" +
            Locale.getString(VantarKey.BUSINESS_ERROR_COUNT, failed) + "\n" +
            Locale.getString(VantarKey.BUSINESS_DUPLICATE_COUNT, duplicate) + "\n" +
            Locale.getString(VantarKey.BUSINESS_SERIAL_MAX, max)
        );
        ui.containerEnd().containerEnd().write();
    }


    // < < < ADMIN DASHBOARD



    // GET DATA > > >


    public static Object search(Params params, Dto dto) throws ServerException, NoContentException, InputException {
        return search(params, dto, dto, null);
    }

    public static Object search(Params params, Dto dto, QueryEvent event) throws ServerException, NoContentException, InputException {
        return search(params, dto, dto, event);
    }

    public static Object search(Params params, Dto dto, Dto dtoView) throws ServerException, NoContentException, InputException {
        return search(params, dto, dtoView, null);
    }

    public static Object search(Params params, Dto dto, Dto dtoView, QueryEvent event) throws ServerException, NoContentException, InputException {
        QueryData queryData = params.getQueryData();
        if (queryData == null) {
            if (!SEARCH_POLICY_ALLOW_EMPTY_CONDITION) {
                throw new InputException(VantarKey.NO_SEARCH_COMMAND);
            }
            if (event == null) {
                return getAll(params, dtoView, null);
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

    public static Object search(QueryBuilder q, String... locales) throws NoContentException, ServerException {
        return search(q, null, locales);
    }

    public static Object search(QueryBuilder q, QueryResultBase.Event event, String... locales) throws NoContentException, ServerException {
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
                return result.asList();
            } catch (DatabaseException e) {
                throw new ServerException(VantarKey.FETCH_FAIL);
            }
        }
    }

    public static <D extends Dto> List<D> getData(QueryBuilder q) throws ServerException, NoContentException {
        try {
            return CommonRepoMongo.getData(q);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <D extends Dto> List<D> getData(Params params, QueryBuilder q) throws ServerException, NoContentException {
        try {
            return CommonRepoMongo.getData(q, params.getLang());
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <D extends Dto> D getFirst(QueryBuilder q) throws ServerException, NoContentException {
        try {
            return CommonRepoMongo.getFirst(q);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <D extends Dto> D getFirst(Params params, QueryBuilder q) throws ServerException, NoContentException {
        try {
            return CommonRepoMongo.getFirst(q, params.getLang());
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <D extends Dto> D getById(Params params, D dto) throws InputException, ServerException, NoContentException {
        Long id = params.getLong("id");
        if (!NumberUtil.isIdValid(id)) {
            throw new InputException(VantarKey.INVALID_ID, dto.getClass().getSimpleName() + ".id");
        }

        if (dto.hasAnnotation(Cache.class)) {
            D d = (D) Services.get(ServiceDtoCache.class).getMap(dto.getClass()).get(id);
            if (d == null) {
                throw new NoContentException();
            }
            return d;
        }

        QueryBuilder q = new QueryBuilder(dto);
        q.condition().equal(Mongo.ID, id);
        String lang = params.getLang();
        try {
            QueryResult result = MongoSearch.getData(q);
            if (StringUtil.isNotEmpty(lang)) {
                result.setLocale(lang);
            }
            return result.first();
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <D extends Dto> D getById(D dto) throws InputException, ServerException, NoContentException {
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
        try {
            return CommonRepoMongo.getById(dto);
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <T extends Dto> List<T> getAll(T dto) throws NoContentException, ServerException {
        return getAll(null, dto, null);
    }

    public static <T extends Dto> List<T> getAll(T dto, String... locales) throws NoContentException, ServerException {
        return getAll(null, dto, locales);
    }

    /**
     * Automatically fetches: cache
     * lang only for database
     */
    public static <T extends Dto> List<T> getAll(Params params, T dto) throws NoContentException, ServerException {
        return getAll(params, dto, null);
    }

    private static <T extends Dto> List<T> getAll(Params params, T dto, String[] locales) throws NoContentException, ServerException {
        if (dto.hasAnnotation(Cache.class)) {
            List<T> d = (List<T>) Services.get(ServiceDtoCache.class).getList(dto.getClass());
            if (d == null) {
                throw new NoContentException();
            }
            return d;
        }
        try {
            QueryResult result = MongoSearch.getAllData(dto);
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
    public static <D extends Dto, L extends Dto> List<L> getAllFromCache(Params params, Class<D> dto, Class<L> localizedDtoClass)
        throws NoContentException, ServerException {

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
        throws ServerException, NoContentException {

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


    public static long count(QueryBuilder q) throws ServerException {
        try {
            return MongoSearch.count(q);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static long count(String collectionName) throws ServerException {
        try {
            return MongoSearch.count(collectionName);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static boolean exists(QueryBuilder q) throws ServerException {
        try {
            return MongoSearch.exists(q);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static boolean exists(Dto dto, String property) throws ServerException {
        try {
            return MongoSearch.exists(dto, property);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static boolean existsById(Dto dto) throws ServerException {
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
}