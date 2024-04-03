package com.vantar.business;

import com.vantar.common.VantarParam;
import com.vantar.database.common.*;
import com.vantar.database.dependency.*;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.service.log.ServiceLog;
import com.vantar.service.log.dto.UserLog;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.util.string.*;
import com.vantar.web.*;
import org.bson.Document;
import java.lang.reflect.*;
import java.util.*;

/**
 * params  write methods: +events +validation +mutex +log
 * normal  write methods: +events +validation +mutex +log
 */
public class ModelMongo extends ModelCommon {

    public static long getNextSequence(Dto dto) throws ServerException {
        try {
            return Mongo.Sequence.getNext(dto);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }


    // INSERT > > >

    public static ResponseMessage insert(Settings settings) throws VantarException {
        return settings.mutex ?
            (ResponseMessage) mutex(settings.dto, (Dto dto) -> insertX(settings, dto)) :
            insertX(settings, settings.dto);
    }

    private static ResponseMessage insertX(Settings s, Dto dto) throws VantarException {
        s.action = Dto.Action.INSERT;
        List<ValidationError> errors;
        if (s.params == null) {
            errors = dto.validate(s.action);
        } else {
            errors = s.isJson ?
                dto.set(s.key == null ? s.params.getJson() : s.params.getString(s.key), s.action) :
                dto.set(s.params, s.action);
        }
        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }

        if (s.event != null) {
            try {
                s.event.beforeWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        dto.removeNullPropertiesNatural();
        ModelMongoChecking.throwUniqueViolation(dto);
        ModelMongoChecking.throwDependencyViolations(dto);
        Mongo.insert(dto);
        ModelCommon.afterDataChange(dto, s.event, s.logEvent, Dto.Action.INSERT);

        return ResponseMessage.success(VantarKey.INSERT_SUCCESS, dto.getId(), dto);
    }

    // < < < INSERT


    // UPDATE > > >

    public static ResponseMessage update(Settings settings) throws VantarException {
        if (settings.q != null) {
            return ResponseMessage.success(
                VantarKey.UPDATE_SUCCESS,
                settings.mutex ? mutex(settings.dto, (Dto dto) -> updateByQuery(settings)) : updateByQuery(settings)
            );
        }

        if (settings.params != null) {
            settings.dto.setId(
                settings.params.getLong(VantarParam.ID, settings.params.extractFromJson(VantarParam.ID, Long.class))
            );
        }
        if (NumberUtil.isIdInvalid(settings.dto.getId())) {
            throw new InputException(VantarKey.INVALID_ID, VantarParam.ID);
        }
        if (!settings.dtoHasFullData) {
            if (settings.params == null) {
                Dto dtoX = ClassUtil.getInstance(settings.dto.getClass());
                dtoX.setId(settings.dto.getId());
                dtoX = getById(dtoX);
                dtoX.set(settings.dto);
                settings.dto = dtoX;
            } else {
                settings.dto = getById(settings.dto);
            }
        }
        if (settings.mutex) {
            mutex(settings.dto, (Dto dto) -> {
                updateById(settings, dto);
                return null;
            });
        } else {
            updateById(settings, settings.dto);
        }
        return ResponseMessage.success(VantarKey.UPDATE_SUCCESS, settings.dto);
    }

    private static List<Dto> updateByQuery(Settings s) throws VantarException {
        s.dtoHasFullData = true;
        List<Dto> dtos = new ArrayList<>(100);
        try {
            disableDtoCache(s.q.getDto());
            forEach(s.q, dto -> {
                dto.set(s.dto);
                updateById(s, dto);
                dtos.add(dto);
            });
        } catch (NoContentException ignore) {

        } finally {
            enableDtoCache(s.q.getDto(), s.event);
        }
        return dtos;
    }

    private static void updateById(Settings s, Dto dto) throws VantarException {
        if (s.action == null) {
            s.action = Dto.Action.UPDATE_FEW_COLS;
        }
        List<ValidationError> errors;
        if (s.params == null) {
            errors = dto.validate(s.action);
        } else {
            s.action = s.params.getX("action", s.action);
            errors = s.isJson ?
                dto.set(s.key == null ? s.params.getJson() : s.params.getString(s.key), s.action) :
                dto.set(s.params, s.action);
        }
        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }

        if (s.event != null) {
            try {
                s.event.beforeWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        dto.removeNullPropertiesNatural();
        ModelMongoChecking.throwUniqueViolation(dto);
        ModelMongoChecking.throwDependencyViolations(dto);
        Mongo.update(dto);
        ModelCommon.afterDataChange(dto, s.event, s.logEvent, s.action);
    }

    /**
     * by id
     * Options = fieldName1, value1, fieldName2, value2, ...
     */
    public static void updateSimple(Dto dto, Object... options) throws VantarException {
        if (dto.getId() == null) {
            return;
        }
        Document updates = new Document();
        for (int i = 0, l = options.length; i < l; i += 2) {
            updates.append((String) options[i], options[i + 1]);
        }
        Mongo.update(dto.getStorage(), dto.getId(), updates);
    }

    // < < < UPDATE


    // DELETE > > >

    public static ResponseMessage delete(Settings settings) throws VantarException {
        if (settings.q != null) {
            if (settings.mutex) {
                mutex(settings.dto, (Dto dto) -> {
                    deleteByQuery(settings);
                    return null;
                });
            } else {
                deleteByQuery(settings);
            }
        } else {
            if (settings.params != null) {
                settings.dto.setId(
                    settings.params.getLong(VantarParam.ID, settings.params.extractFromJson(VantarParam.ID, Long.class))
                );
            }
            if (NumberUtil.isIdInvalid(settings.dto.getId())) {
                throw new InputException(VantarKey.INVALID_ID, VantarParam.ID);
            }
            if (settings.logEvent && !settings.dtoHasFullData) {
                settings.dto = getById(settings.dto);
            }
            if (settings.mutex) {
                mutex(settings.dto, (Dto dto) -> {
                    deleteById(settings, dto);
                    return null;
                });
            } else {
                deleteById(settings, settings.dto);
            }
        }
        return ResponseMessage.success(VantarKey.DELETE_SUCCESS, settings.getDeletedCount());
    }

    private static void deleteByQuery(Settings s) throws VantarException {
        s.dtoHasFullData = true;
        try {
            disableDtoCache(s.q.getDto());
            forEach(s.q, dto -> deleteById(s, dto));
        } catch (NoContentException ignore) {

        } finally {
            enableDtoCache(s.q.getDto(), s.event);
        }
    }

    private static void deleteById(Settings s, Dto dto) throws VantarException {
        if (s.event != null) {
            try {
                s.event.beforeWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }
        if (s.cascade) {
            DataDependencyMongo dep = new DataDependencyMongo(dto);
            dep.deleteCascade(dtoX -> {
                s.addDeletedCount(Mongo.delete(dtoX));
                ModelCommon.afterDataChange(dtoX, s.event, s.logEvent, Dto.Action.DELETE);
            });
        } else if (s.force) {
            s.addDeletedCount(Mongo.delete(dto));
            ModelCommon.afterDataChange(dto, s.event, s.logEvent, Dto.Action.DELETE);
        } else {
            new DataDependencyMongo(dto).throwOnFirstDependency();
            s.addDeletedCount(Mongo.delete(dto));
            ModelCommon.afterDataChange(dto, s.event, s.logEvent, Dto.Action.DELETE);
        }
    }








    public static ResponseMessage retrieveFromLog(long id) throws VantarException {
        UserLog userLog = new UserLog();
        userLog.id = id;
        userLog = getById(userLog);

        Dto dto = DtoDictionary.getInstance(userLog.classNameSimple);
        if (dto == null) {
            throw new InputException(VantarKey.INVALID_VALUE, "classNameSimple");
        }

        dto.set(userLog.object, Dto.Action.SET);
        return new ResponseMessage();
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
            if (Services.isUp(ServiceLog.class)) {
                ServiceLog.addAction(Dto.Action.PURGE, dto);
            }
            return ResponseMessage.success(VantarKey.DELETE_SUCCESS);
        } catch (DatabaseException e) {
            ServiceLog.log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.DELETE_FAIL);
        }
    }

    // < < < PURGE


    // > > > ITEM WORKS

    public static ResponseMessage addToList(Dto dto, String fieldName, Object item) throws VantarException {
        return addToCollection(dto, null, fieldName, item, true, true);
    }

    public static ResponseMessage addToList(QueryBuilder q, String fieldName, Object item) throws VantarException {
        return addToCollection(q.getDto(), q, fieldName, item, true, true);
    }

    public static ResponseMessage addToListNoLog(Dto dto, String fieldName, Object item) throws VantarException {
        return addToCollection(dto, null, fieldName, item, true, false);
    }

    public static ResponseMessage addToListNoLog(QueryBuilder q, String fieldName, Object item) throws VantarException {
        return addToCollection(q.getDto(), q, fieldName, item, true, false);
    }

    public static ResponseMessage addToSet(Dto dto, String fieldName, Object item) throws VantarException {
        return addToCollection(dto, null, fieldName, item, false, true);
    }

    public static ResponseMessage addToSet(QueryBuilder q, String fieldName, Object item) throws VantarException {
        return addToCollection(q.getDto(), q, fieldName, item, false, true);
    }

    public static ResponseMessage addToSetNoLog(Dto dto, String fieldName, Object item) throws VantarException {
        return addToCollection(dto, null, fieldName, item, false, false);
    }

    public static ResponseMessage addToSetNoLog(QueryBuilder q, String fieldName, Object item) throws VantarException {
        return addToCollection(q.getDto(), q, fieldName, item, false, false);
    }

    private static ResponseMessage addToCollection(Dto dto, QueryBuilder q, String fieldName, Object item
        , boolean isList, boolean logEvent) throws VantarException {

        Mongo.addToCollection(
            dto.getStorage(),
            q == null ? new Document(Mongo.ID, dto.getId()) : new MongoQuery(q).matches,
            isList,
            fieldName,
            item,
            q != null && q.isUpdateMany()
        );

        if (logEvent && Services.isUp(ServiceLog.class)) {
            Map<String, Object> object = new HashMap<>(1, 1);
            object.put(fieldName, item);
            ServiceLog.addAction(
                Dto.Action.UPDATE_ADD_ITEM,
                new ServiceLog.DtoLogAction(dto.getClass(), dto.getId(), object)
            );
        }

        return ResponseMessage.success(VantarKey.UPDATE_SUCCESS, dto);
    }

    public static ResponseMessage removeFromCollection(Dto dto, String fieldName, Object item) throws VantarException {
        return removeFromCollection(dto, null, fieldName, item, true);
    }

    public static ResponseMessage removeFromCollection(QueryBuilder q, String fieldName, Object item) throws VantarException {
        return removeFromCollection(q.getDto(), q, fieldName, item, true);
    }

    public static ResponseMessage removeFromCollectionNoLog(Dto dto, String fieldName, Object item) throws VantarException {
        return removeFromCollection(dto, null, fieldName, item, false);
    }

    public static ResponseMessage removeFromCollectionNoLog(QueryBuilder q, String fieldName, Object item) throws VantarException {
        return removeFromCollection(q.getDto(), q, fieldName, item, false);
    }

    private static ResponseMessage removeFromCollection(Dto dto, QueryBuilder q, String fieldName, Object item
        , boolean logEvent) throws VantarException {

        Mongo.removeFromCollection(
            dto.getStorage(),
            q == null ? new Document(Mongo.ID, dto.getId()) : new MongoQuery(q).matches,
            fieldName,
            item,
            q != null && q.isUpdateMany()
        );

        if (logEvent && Services.isUp(ServiceLog.class)) {
            Map<String, Object> object = new HashMap<>(1, 1);
            object.put(fieldName, item);
            ServiceLog.addAction(
                Dto.Action.UPDATE_REMOVE_ITEM,
                new ServiceLog.DtoLogAction(dto.getClass(), dto.getId(), object)
            );
        }

        return ResponseMessage.success(VantarKey.UPDATE_SUCCESS, dto);
    }

    public static ResponseMessage addToMap(Dto dto, String fieldName, String key, Object item) throws VantarException {
        return addToMap(dto, null, fieldName, key, item, true);
    }

    public static ResponseMessage addToMap(QueryBuilder q, String fieldName, String key, Object item) throws VantarException {
        return addToMap(q.getDto(), q, fieldName, key, item, true);
    }

    public static ResponseMessage addToMapNoLog(Dto dto, String fieldName, String key, Object item) throws VantarException {
        return addToMap(dto, null, fieldName, key, item, false);
    }

    public static ResponseMessage addToMapNoLog(QueryBuilder q, String fieldName, String key, Object item) throws VantarException {
        return addToMap(q.getDto(), q, fieldName, key, item, false);
    }

    private static ResponseMessage addToMap(Dto dto, QueryBuilder q, String fieldName, String key, Object value
        , boolean logEvent) throws VantarException {

        Mongo.addToMap(
            dto.getStorage(),
            q == null ? new Document(Mongo.ID, dto.getId()) : new MongoQuery(q).matches,
            fieldName,
            key,
            value,
            q != null && q.isUpdateMany()
        );

        if (logEvent && Services.isUp(ServiceLog.class)) {
            Map<String, Object> object = new HashMap<>(1, 1);
            object.put(fieldName + key, value);
            ServiceLog.addAction(
                Dto.Action.UPDATE_ADD_ITEM,
                new ServiceLog.DtoLogAction(dto.getClass(), dto.getId(), object)
            );
        }

        return ResponseMessage.success(VantarKey.UPDATE_SUCCESS, dto);
    }

    public static ResponseMessage removeFromMap(Dto dto, String fieldName, String key) throws VantarException {
        return removeFromMap(dto, null, fieldName, key, true);
    }

    public static ResponseMessage removeFromMap(QueryBuilder q, String fieldName, String key) throws VantarException {
        return removeFromMap(q.getDto(), q, fieldName, key, true);
    }

    public static ResponseMessage removeFromMapNoLog(Dto dto, String fieldName, String key) throws VantarException {
        return removeFromMap(dto, null, fieldName, key, false);
    }

    public static ResponseMessage removeFromMapNoLog(QueryBuilder q, String fieldName, String key) throws VantarException {
        return removeFromMap(q.getDto(), q, fieldName, key, false);
    }

    private static ResponseMessage removeFromMap(Dto dto, QueryBuilder q, String fieldName, String key, boolean logEvent)
        throws VantarException {

        Mongo.removeFromMap(
            dto.getStorage(),
            q == null ? new Document(Mongo.ID, dto.getId()) : new MongoQuery(q).matches,
            fieldName + "." + key,
            q != null && q.isUpdateMany()
        );

        if (logEvent && Services.isUp(ServiceLog.class)) {
            Map<String, Object> object = new HashMap<>(1, 1);
            object.put(fieldName, key);
            ServiceLog.addAction(
                Dto.Action.UPDATE_REMOVE_ITEM,
                new ServiceLog.DtoLogAction(dto.getClass(), dto.getId(), object)
            );
        }

        return ResponseMessage.success(VantarKey.UPDATE_SUCCESS, dto);
    }

    // ITEM WORKS < < <


    // GET DATA > > >

    public static void forEach(Dto dto, QueryResultBase.EventForeach event) throws VantarException {
        try {
            MongoQuery.getAllData(dto).forEach(event);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static void forEach(QueryBuilder q, QueryResultBase.EventForeach event) throws VantarException {
        try {
            QueryResult result = MongoQuery.getData(q);
            result.setLocale(q.getLocale());
            result.forEach(event);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static PageData search(Params params, Dto dto) throws VantarException {
        return search(params, dto, null);
    }

    public static PageData search(Params params, Dto dto, QueryEvent event) throws VantarException {
        QueryBuilder q = params.getQueryBuilder(dto);
        if (q == null) {
            if (event == null) {
                return new PageData(getAll(params, dto, null, null));
            }
            q = new QueryBuilder(dto);
        }

        if (event != null) {
            event.beforeQuery(q);
        }

        return search(q, event, params.getLang());
    }

    public static PageData searchForEach(Params params, Dto dto, QueryEventForeach event) throws VantarException {
        QueryBuilder q = params.getQueryBuilder(dto);
        if (q == null) {
            q = new QueryBuilder(dto);
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
                return MongoQuery.getPage(q, event, locales);
            } catch (DatabaseException e) {
                throw new ServerException(VantarKey.FETCH_FAIL);
            }
        } else {
            QueryResult result;
            try {
                result = MongoQuery.getData(q);
            } catch (DatabaseException e) {
                throw new ServerException(VantarKey.FETCH_FAIL);
            }
            if (event != null) {
                result.setEvent(event);
            }
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            PageData data;
            try {
                data = new PageData(result.asList());
            } catch (DatabaseException e) {
                throw new ServerException(VantarKey.FETCH_FAIL);
            }
            List<ValidationError> errors = q.getErrors();
            if (errors != null) {
                data.errors = ValidationError.toString(errors);
            }
            return data;
        }
    }

    public static PageData searchForEach(QueryBuilder q, QueryResultBase.EventForeach event, String... locales) throws VantarException {
        PageData data;
        try {
            data = MongoQuery.getPageForeach(q, event, locales);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
        List<ValidationError> errors = q.getErrors();
        if (errors != null) {
            data.errors = ValidationError.toString(errors);
        }
        return data;
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
            QueryResult result = MongoQuery.getData(q);
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
            QueryResult result = MongoQuery.getData(q);
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
            QueryResult result = MongoQuery.getAllData(dto);
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.asMap(keyProperty);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static Collection<Object> getPropertyList(QueryBuilder q, String property, String... locales) throws VantarException {
        try {
            QueryResult result = MongoQuery.getData(q);
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.asPropertyList(property);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static Collection<Object> getPropertyList(Dto dto, String property, String... locales) throws VantarException {
        try {
            QueryResult result = MongoQuery.getAllData(dto);
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.asPropertyList(property);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static Map<Long, Object> getPropertyMap(QueryBuilder q, String property, String... locales) throws VantarException {
        try {
            QueryResult result = MongoQuery.getData(q);
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.asPropertyMap(property);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static Map<Long, Object> getPropertyMap(Dto dto, String property, String... locales) throws VantarException {
        try {
            QueryResult result = MongoQuery.getAllData(dto);
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.asPropertyMap(property);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <D extends Dto> D getFirst(QueryBuilder q, String... locales) throws VantarException {
        try {
            QueryResult result = MongoQuery.getData(q);
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
            QueryResult result = MongoQuery.getData(q);
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            return result.first();
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static <D extends Dto> D getById(Params params, D dto) throws VantarException {
        return getById(params, VantarParam.ID, dto);
    }

    @SuppressWarnings("unchecked")
    public static <D extends Dto> D getById(Params params, String idParam, D dto) throws VantarException {
        Long id = params.getLong(idParam);
        if (id == null) {
            id = params.extractFromJson(idParam, Long.class);
        }
        if (!NumberUtil.isIdValid(id)) {
            throw new InputException(VantarKey.INVALID_ID, dto.getClass().getSimpleName() + ".id");
        }
        dto.setId(id);
        if (dto.hasAnnotation(Cache.class)) {
            D d = (D) Services.getService(ServiceDtoCache.class).getMap(dto.getClass()).get(id);
            if (d == null) {
                throw new NoContentException();
            }
            return d;
        }
        return getByIdX(dto, params.getLang());
    }

    @SuppressWarnings("unchecked")
    public static <D extends Dto> D getById(D dto, Long id, String... locales) throws VantarException {
        if (!NumberUtil.isIdValid(id)) {
            throw new InputException(VantarKey.INVALID_ID, dto.getClass().getSimpleName() + ".id");
        }
        if (dto.hasAnnotation(Cache.class)) {
            D d = (D) Services.getService(ServiceDtoCache.class).getMap(dto.getClass()).get(id);
            if (d == null) {
                throw new NoContentException();
            }
            return d;
        }
        dto.setId(id);
        return getByIdX(dto, locales);
    }

    @SuppressWarnings("unchecked")
    public static <D extends Dto> D getById(D dto, String... locales) throws VantarException {
        if (!NumberUtil.isIdValid(dto.getId())) {
            throw new InputException(VantarKey.INVALID_ID, dto.getClass().getSimpleName() + ".id");
        }
        if (dto.hasAnnotation(Cache.class)) {
            D d = (D) Services.getService(ServiceDtoCache.class).getMap(dto.getClass()).get(dto.getId());
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
            ServiceLog.log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
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
            List<T> d = (List<T>) Services.getService(ServiceDtoCache.class).getList(dto.getClass());
            if (d == null) {
                throw new NoContentException();
            }
            return d;
        }
        try {
            QueryResult result = MongoQuery.getAllData(dto);
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

        ServiceDtoCache cache = Services.getService(ServiceDtoCache.class);

        String lang = params.getLang();
        List<L> localized = new ArrayList<>();

        for (Dto d : cache.getList(dto)) {
            L localizedDto;
            try {
                localizedDto = localizedDtoClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                ServiceLog.log.warn(" ! ({}, {}) < {}\n", dto, localizedDtoClass, d, e);
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
    public static Map<String, String> getKeyValue(Dto dto, String keyProperty, String valueProperty) throws VantarException {
        return getKeyValue(null, dto, keyProperty, valueProperty);
    }

    public static Map<String, String> getKeyValue(Params params, Dto dto, String keyProperty, String valueProperty)
        throws VantarException {

        String lang = params == null ? null : params.getLangNoDefault();
        if (dto.hasAnnotation(Cache.class)) {
            Map<String, String> result = new LinkedHashMap<>();
            for (Dto d : Services.getService(ServiceDtoCache.class).getList(dto.getClass())) {
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
                QueryResult result = MongoQuery.getAllData(dto);
                if (StringUtil.isNotEmpty(lang)) {
                    result.setLocale(lang);
                }
                return result.asKeyValue(keyProperty, valueProperty);
            } catch (DatabaseException e) {
                throw new ServerException(VantarKey.FETCH_FAIL);
            }
        }
    }

    public static Map<String, String> getKeyValue(QueryBuilder q, String keyProperty, String valueProperty, String... locales)
        throws VantarException {

        try {
            QueryResult result = MongoQuery.getData(q);
            result.setLocale(locales);
            return result.asKeyValue(keyProperty, valueProperty);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }


    // > > > count exists


    public static long count(QueryBuilder q) throws VantarException {
        try {
            return MongoQuery.count(q);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static long count(String collectionName) throws VantarException {
        try {
            return MongoQuery.count(collectionName);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static boolean exists(QueryBuilder q) throws VantarException {
        try {
            return MongoQuery.exists(q);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static boolean exists(Dto dto, String property) throws VantarException {
        try {
            return MongoQuery.exists(dto, property);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static boolean existsById(Dto dto) throws VantarException {
        try {
            return MongoQuery.existsById(dto);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }

    public static boolean existsByDto(Dto dto) throws ServerException {
        try {
            return MongoQuery.existsByDto(dto);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }
    }
}