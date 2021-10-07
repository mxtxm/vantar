package com.vantar.business;

import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.service.log.LogEvent;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.slf4j.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class CommonModelMongo extends CommonModel {

    private static final Logger log = LoggerFactory.getLogger(CommonModelMongo.class);


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

    public static ResponseMessage insertJson(String json, Dto dto) throws InputException, ServerException {
        return insertX(json, dto, null, null);
    }

    public static ResponseMessage insertJson(Params params, String key, Dto dto) throws InputException, ServerException {
        return insertX(params.getString(key), dto, null, params);
    }

    public static ResponseMessage insertJson(String json, Dto dto, WriteEvent event) throws InputException, ServerException {
        return insertX(json, dto, event, null);
    }

    private static ResponseMessage insertX(Object params, Dto dto, WriteEvent event, Params requestParams) throws InputException, ServerException {
        List<ValidationError> errors;
        if (params instanceof String) {
            errors = dto.set((String) params, Dto.Action.INSERT);
        } else {
            errors = dto.set((Params) params, Dto.Action.INSERT);
        }
        // todo: this must be impossible
        if (errors == null) {
            log.error(">>>>>>>> {}\n{}\n\n ", params, dto);
            LogEvent.error("DEBUG", params, dto);
            errors = new ArrayList<>();
        }

        if (event != null) {
            try {
                event.beforeWrite(dto);
            } catch (InputException e) {
                errors.addAll(e.getErrors());
            }
        }

        if (!errors.isEmpty()) {
            throw new InputException(errors);
        }

        try {
            errors = CommonRepoMongo.getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }

            long id = CommonRepoMongo.insert(dto);
            if (event != null) {
                event.afterWrite(dto);
            }

            try {
                dto = CommonRepoMongo.getById(dto);
            } catch (DatabaseException e) {
                log.error("! {}", dto, e);
            } catch (NoContentException e) {
                throw new ServerException(VantarKey.INSERT_FAIL);
            }

            logAction(requestParams == null ? params : requestParams, dto, Dto.Action.INSERT);

            return new ResponseMessage(VantarKey.INSERT_SUCCESS, id, dto);
        } catch (DatabaseException e) {
            log.error("! {}", dto, e);
            throw new ServerException(VantarKey.INSERT_FAIL);
        }
    }

    public static <T extends Dto> ResponseMessage insertBatch(Params params, Class<T> tClass)
        throws InputException, ServerException {

        return insertBatch(params, tClass, null);
    }

    public static <T extends Dto> ResponseMessage insertBatch(Params params, Class<T> tClass, WriteEvent event)
        throws InputException, ServerException {

        List<T> dtos = params.getJsonList(tClass);
        if (dtos == null || dtos.isEmpty()) {
            throw new InputException(VantarKey.INVALID_JSON_DATA);
        }

        for (Dto dto : dtos) {
            if (event != null) {
                event.beforeWrite(dto);
            }

            List<ValidationError> errors = dto.validate(Dto.Action.INSERT);
            if (!errors.isEmpty()) {
                throw new InputException(errors);
            }
        }

        try {
            CommonRepoMongo.insert(dtos);
        } catch (DatabaseException e) {
            log.error("! {}", dtos, e);
            throw new ServerException(VantarKey.INSERT_FAIL);
        }

        if (event != null) {
            for (Dto dto : dtos) {
                logAction(params, dto, Dto.Action.INSERT);
                event.afterWrite(dto);
            }
        }

        return new ResponseMessage(VantarKey.INSERT_SUCCESS, dtos.size());
    }

    public static ResponseMessage update(Params params, Dto dto) throws InputException, ServerException {
        return updateX(params, dto, null, Dto.Action.UPDATE, null);
    }

    public static ResponseMessage updateStrict(Params params, Dto dto) throws InputException, ServerException {
        return updateX(params, dto, null, Dto.Action.UPDATE_STRICT, null);
    }

    public static ResponseMessage update(Params params, Dto dto, WriteEvent event) throws InputException, ServerException {
        return updateX(params, dto, event, Dto.Action.UPDATE, null);
    }

    public static ResponseMessage updateStrict(Params params, Dto dto, WriteEvent event) throws InputException, ServerException {
        return updateX(params, dto, event, Dto.Action.UPDATE_STRICT, null);
    }

    public static ResponseMessage updateJson(Params params, Dto dto) throws InputException, ServerException {
        return updateX(params.getJson(), dto, null, Dto.Action.UPDATE, null);
    }

    public static ResponseMessage updateJsonStrict(Params params, Dto dto) throws InputException, ServerException {
        return updateX(params.getJson(), dto, null, Dto.Action.UPDATE_STRICT, null);
    }

    public static ResponseMessage updateJson(Params params, Dto dto, WriteEvent event) throws InputException, ServerException {
        return updateX(params.getJson(), dto, event, Dto.Action.UPDATE, null);
    }

    public static ResponseMessage updateJsonStrict(Params params, Dto dto, WriteEvent event) throws InputException, ServerException {
        return updateX(params.getJson(), dto, event, Dto.Action.UPDATE_STRICT, null);
    }

    public static ResponseMessage updateJson(String json, Dto dto) throws InputException, ServerException {
        return updateX(json, dto, null, Dto.Action.UPDATE, null);
    }

    public static ResponseMessage updateJsonStrict(String json, Dto dto) throws InputException, ServerException {
        return updateX(json, dto, null, Dto.Action.UPDATE_STRICT, null);
    }

    public static ResponseMessage updateJson(Params params, String key, Dto dto) throws InputException, ServerException {
        return updateX(params.getString(key), dto, null, Dto.Action.UPDATE_STRICT, params);
    }

    public static ResponseMessage updateJson(String json, Dto dto, WriteEvent event) throws InputException, ServerException {
        return updateX(json, dto, event, Dto.Action.UPDATE, null);
    }

    public static ResponseMessage updateJsonStrict(String json, Dto dto, WriteEvent event) throws InputException, ServerException {
        return updateX(json, dto, event, Dto.Action.UPDATE_STRICT, null);
    }

    private static ResponseMessage updateX(Object params, Dto dto, WriteEvent event, Dto.Action action, Params requestParams)
        throws InputException, ServerException {

        List<ValidationError> errors;
        if (params instanceof String) {
            errors = dto.set((String) params, action);
        } else {
            errors = dto.set((Params) params, action);
        }

        if (event != null) {
            try {
                event.beforeWrite(dto);
            } catch (InputException e) {
                List<ValidationError> ee = e.getErrors();
                if (ee != null) {
                    errors.addAll(e.getErrors());
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new InputException(errors);
        }

        try {
            errors = CommonRepoMongo.getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }

            Mongo.update(dto);
        } catch (DatabaseException e) {
            log.error("! {}", dto, e);
            throw new ServerException(VantarKey.UPDATE_FAIL);
        }

        if (event != null) {
            event.afterWrite(dto);
        }

        try {
            dto = CommonRepoMongo.getById(dto);
        } catch (DatabaseException e) {
            log.error("! {}", dto, e);
        } catch (NoContentException e) {
            throw new ServerException(VantarKey.INSERT_FAIL);
        }

        logAction(requestParams == null ? params : requestParams, dto, Dto.Action.UPDATE);

        return new ResponseMessage(VantarKey.UPDATE_SUCCESS, dto);
    }

    public static <T extends Dto> ResponseMessage updateBatch(Params params, Class<T> tClass)
        throws InputException, ServerException {

        return updateBatch(params, tClass, null);
    }

    public static <T extends Dto> ResponseMessage updateBatch(Params params, Class<T> tClass, WriteEvent event)
        throws InputException, ServerException {

        List<T> dtos = params.getJsonList(tClass);
        if (dtos == null || dtos.isEmpty()) {
            throw new InputException(VantarKey.INVALID_JSON_DATA);
        }

        try {
            for (Dto dto : dtos) {
                if (event != null) {
                    event.beforeWrite(dto);
                }

                List<ValidationError> errors = dto.validate(Dto.Action.UPDATE);
                if (!errors.isEmpty()) {
                    throw new InputException(errors);
                }

                Mongo.update(dto);

                if (event != null) {
                    event.afterWrite(dto);
                }

                logAction(params, dto, Dto.Action.UPDATE);
            }

            if (!dtos.isEmpty()) {
                afterDataChange(dtos.get(0));
            }

            return new ResponseMessage(VantarKey.UPDATE_SUCCESS, dtos.size());

        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.UPDATE_FAIL);
        }
    }

    public static ResponseMessage delete(Params params, Dto dto) throws InputException, ServerException {
        return delete(params, dto, null);
    }

    public static ResponseMessage delete(Params params, Dto dto, WriteEvent event) throws InputException, ServerException {
        List<ValidationError> errors = dto.set(params, Dto.Action.DELETE);
        if (!errors.isEmpty()) {
            throw new InputException(errors);
        }

        if (event != null) {
            event.beforeWrite(dto);
        }

        try {
            ResponseMessage r = new ResponseMessage(VantarKey.DELETE_SUCCESS, CommonRepoMongo.delete(dto));
            if (event != null) {
                event.afterWrite(dto);
            }

            logAction(params, dto, Dto.Action.DELETE);

            return r;
        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
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

        T dto = ObjectUtil.getInstance(tClass);
        if (dto == null) {
            throw new InputException(VantarKey.INVALID_JSON_DATA);
        }

        try {
            for (Long id : ids) {
                dto.setId(id);
                if (event != null) {
                    event.beforeWrite(dto);
                }

                Mongo.delete(dto);

                logAction(params, dto, Dto.Action.DELETE);
            }

            if (!ids.isEmpty()) {
                afterDataChange(dto);
            }

        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.DELETE_FAIL);
        }

        return new ResponseMessage(VantarKey.DELETE_SUCCESS, ids.size());
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

        T dto = ObjectUtil.getInstance(tClass);
        if (dto == null) {
            throw new InputException(VantarKey.INVALID_JSON_DATA);
        }

        try {
            for (Long id : ids) {
                dto.setId(id);
                if (event != null) {
                    event.beforeWrite(dto);
                }

                Mongo.unset(dto, Mongo.LOGICAL_DELETE_FIELD);

                logAction(params, dto, Dto.Action.UN_DELETE);
            }

            if (!ids.isEmpty()) {
                afterDataChange(dto);
            }

        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.UPDATE_FAIL);
        }

        return new ResponseMessage(VantarKey.UPDATE_MANY_SUCCESS, ids.size());
    }




    public static ResponseMessage batch(Params params, Dto dto) throws InputException, ServerException {
        return batch(params, dto, null);
    }

    public static ResponseMessage batch(Params params, Dto dto, BatchEvent event) throws InputException, ServerException {
        Batch update = params.getJson(Batch.class);
        String msg =
            deleteMany(params, update.delete, dto, event) + "\n\n" +
            insertMany(params, update.insert, dto.getClass(), event) + "\n\n" +
            updateMany(params, update.update, dto, event) + "\n\n";

        afterDataChange(dto);
        return new ResponseMessage(msg);
    }

    private static String insertMany(Params params, List<Map<String, Object>> records, Class<? extends Dto> tClass, BatchEvent event)
        throws InputException, ServerException {

        List<Dto> dtos = new ArrayList<>();
        for (Map<String, Object> record : records) {
            Dto dto = ObjectUtil.getInstance(tClass);
            if (dto == null) {
                throw new ServerException(VantarKey.CAN_NOT_CREATE_DTO);
            }

            if (event != null && event.beforeInsert(dto)) {
                throw new ServerException(VantarKey.INSERT_FAIL);
            }

            List<ValidationError> errors = dto.set(record, Dto.Action.INSERT);
            if (!errors.isEmpty()) {
                throw new InputException(errors);
            }

            try {
                errors = CommonRepoMongo.getUniqueViolation(dto);
                if (errors != null) {
                    throw new InputException(errors);
                }
            } catch (DatabaseException e) {
                log.error("! {}", e.getMessage());
                throw new ServerException(VantarKey.INSERT_FAIL);
            }

            dtos.add(dto);
        }

        try {
            Mongo.insert(dtos);
        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.INSERT_FAIL);
        }

        logAction(params, dtos, Dto.Action.INSERT);

        return Locale.getString(VantarKey.INSERT_MANY_SUCCESS, dtos.size());
    }

    private static String updateMany(Params params, List<Map<String, Object>> records, Dto dto, BatchEvent event)
        throws InputException, ServerException {

        for (Map<String, Object> record : records) {
            if (event != null && event.beforeUpdate(dto)) {
                throw new ServerException(VantarKey.UPDATE_FAIL);
            }

            List<ValidationError> errors = dto.set(record, Dto.Action.UPDATE);
            if (!errors.isEmpty()) {
                throw new InputException(errors);
            }

            try {
                errors = CommonRepoMongo.getUniqueViolation(dto);
                if (errors != null) {
                    throw new InputException(errors);
                }

                Mongo.update(dto);
            } catch (DatabaseException e) {
                log.error("! {}", e.getMessage());
                throw new ServerException(VantarKey.UPDATE_FAIL);
            }

            logAction(params, dto, Dto.Action.UPDATE);
        }

        return Locale.getString(VantarKey.UPDATE_MANY_SUCCESS, records.size());
    }

    private static String deleteMany(Params params, List<Long> ids, Dto dto, BatchEvent event) throws InputException, ServerException {
        for (Long id : ids) {
            dto.setId(id);

            if (event != null && event.beforeDelete(dto)) {
                throw new ServerException(VantarKey.DELETE_FAIL);
            }

            if (ObjectUtil.isIdInvalid(id)) {
                throw new InputException(new ValidationError(VantarParam.ID, VantarKey.INVALID_ID));
            }

            try {
                Mongo.delete(dto);
            } catch (DatabaseException e) {
                log.error("! {}", e.getMessage());
                throw new ServerException(VantarKey.DELETE_FAIL);
            }

            logAction(params, dto, Dto.Action.DELETE);
        }

        return Locale.getString(VantarKey.DELETE_MANY_SUCCESS, ids.size());
    }

    public static ResponseMessage purge(Params params, String collection) throws ServerException {
        try {
            CommonRepoMongo.purge(collection);
            logAction(params, collection, Dto.Action.PURGE);
            return new ResponseMessage(VantarKey.DELETE_SUCCESS);
        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.DELETE_FAIL);
        }
    }

    public static ResponseMessage purgeData(Params params, String collection) throws ServerException {
        try {
            CommonRepoMongo.purgeData(collection);
            logAction(params, collection, Dto.Action.PURGE);
            return new ResponseMessage(VantarKey.DELETE_SUCCESS);
        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.DELETE_FAIL);
        }
    }

    // > > > admin tools

    public static void importDataAdmin(String data, Dto dto, List<String> presentField, boolean deleteAll, WebUi ui) {
        logAction(ui.params, "", Dto.Action.IMPORT);

        ui.beginBox2(dto.getClass().getSimpleName()).write();

        if (deleteAll) {
            try {
                CommonRepoMongo.purge(dto.getStorage());
            } catch (DatabaseException e) {
                ui.addErrorMessage(e);
                log.error("! batch import failed", e);
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

                Mongo.insert(dto);

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










    /* > > > GET DATA */

    public static Object search(Params params, Dto dto, Dto dtoView) throws ServerException, NoContentException, InputException {
        QueryData queryData = params.getQueryData();
        if (queryData == null) {
            throw new InputException(VantarKey.NO_SEARCH_COMMAND);
        }
        queryData.setDto(dto, dtoView);

        try {
            return CommonRepoMongo.search(queryData, params.getLang());
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

    public static <D extends Dto> D getById(Params params, D dto) throws InputException, ServerException, NoContentException {
        Long id = params.getLong("id");
        if (!ObjectUtil.isIdValid(id)) {
            throw new InputException(VantarKey.INVALID_ID, dto.getClass().getSimpleName() + ".id");
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
            log.error("! {}>{}", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    /**
     * Automatically fetches: cache
     * lang only for database
     */
    public static <T extends Dto> List<T> getAll(Params params, T dto) throws NoContentException, ServerException {
        String lang = params.getLang();
        if (dto.hasAnnotation(Cache.class)) {
            ServiceDtoCache cache = Services.get(ServiceDtoCache.class);
            if (cache == null) {
                throw new ServiceException(ServiceDtoCache.class);
            }
            return (List<T>) cache.getList(dto.getClass());
        } else {
            try {
                QueryResult result = MongoSearch.getAllData(dto);
                if (StringUtil.isNotEmpty(lang)) {
                    result.setLocale(lang);
                }
                return result.asList();
            } catch (DatabaseException e) {
                throw new ServerException(VantarKey.FETCH_FAIL);
            }
        }
    }

    /**
     * Automatically fetches: cache, lang
     */
    public static <D extends Dto, L extends Dto> List<L> getAllFromCache(Params params, Class<D> dto, Class<L> localizedDtoClass)
        throws NoContentException, ServerException {

        ServiceDtoCache cache = Services.get(ServiceDtoCache.class);
        if (cache == null) {
            throw new ServiceException(ServiceDtoCache.class);
        }

        String lang = params.getLang();
        List<L> localized = new ArrayList<>();

        for (Dto d : cache.getList(dto)) {
            L localizedDto;
            try {
                localizedDto = localizedDtoClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.warn("! ({}, {}) < {}", dto, localizedDtoClass, d, e);
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
            ServiceDtoCache cache = Services.get(ServiceDtoCache.class);
            if (cache == null) {
                throw new ServiceException(ServiceDtoCache.class);
            }
            Map<String, String> result = new LinkedHashMap<>();
            for (Dto d : cache.getList(dto.getClass())) {
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
}