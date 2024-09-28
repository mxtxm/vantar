package com.vantar.business;

import com.vantar.common.VantarParam;
import com.vantar.database.common.*;
import com.vantar.database.dependency.DataDependencyMongo;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.bson.Document;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class ModelMongo extends ModelCommon {

    public ModelMongo() {
        db = Db.mongo;
    }

    public ModelMongo(DbMongo db) {
        this.db = db;
    }

    public long getNextAutoIncrement(Dto dto) throws VantarException {
        return db.autoIncrementGetNext(dto);
    }

    public long setAutoIncrementToMax(Dto dto) throws VantarException {
        return db.autoIncrementSetToMax(dto);
    }

    // INSERT > > >

    public ResponseMessage insert(Settings settings) throws VantarException {
        if (!settings.autoIncrementOnInsert) {
            settings.updateAutoIncrement = true;
            settings.dto.autoIncrementOnInsert(false);
        }
        return settings.mutex ?
            (ResponseMessage) mutex(settings.dto, (Dto dto) -> insertX(settings, dto)) :
            insertX(settings, settings.dto);
    }

    private ResponseMessage insertX(Settings s, Dto dto) throws VantarException {
        s.action = Dto.Action.INSERT;

        if (s.params != null) {
            if (s.exclude != null) {
                s.params.removeParams(s.exclude);
            }
            if (s.isJson) {
                dto.set(s.key == null ? s.params.getJson() : s.params.getString(s.key), s.action);
            } else {
                dto.set(s.params, s.action);
            }
        }

        if (s.eventBeforeValidation != null) {
            s.eventBeforeValidation.write(dto);
        }
        List<ValidationError> errors = dto.validate(s.action);
        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }
        dto.removeNullPropertiesNatural();
        ModelMongoChecking.throwUniqueViolation(dto, db);
        ModelMongoChecking.throwDependencyViolations(dto, db);
        if (s.eventBeforeWrite != null) {
            s.eventBeforeWrite.write(dto);
        }
        db.insert(dto);
        ModelCommon.afterDataChange(dto, s);
        if (s.updateAutoIncrement) {
            setAutoIncrementToMax(dto);
        }

        return ResponseMessage.success(VantarKey.SUCCESS_INSERT, dto.getId(), dto);
    }

    // < < < INSERT


    // UPDATE > > >

    public ResponseMessage update(Settings settings) throws VantarException {
        if (settings.q != null) {
            settings.dto = settings.q.getDto();
            return ResponseMessage.success(
                VantarKey.SUCCESS_UPDATE,
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
            Dto dtoX = ClassUtil.getInstance(settings.dto.getClass());
            if (dtoX == null) {
                return null;
            }
            dtoX.setId(settings.dto.getId());
            dtoX = getById(dtoX);
            dtoX.set(settings.dto, settings.action);
            settings.dto = dtoX;
        }
        if (settings.eventAfterRead != null) {
            settings.eventAfterRead.read(settings.dto.getClone());
        }

        if (settings.mutex) {
            mutex(settings.dto, (Dto dto) -> {
                updateById(settings, dto);
                return null;
            });
        } else {
            updateById(settings, settings.dto);
        }
        return ResponseMessage.success(VantarKey.SUCCESS_UPDATE, settings.dto);
    }

    private List<Dto> updateByQuery(Settings s) throws VantarException {
        Dto updatedDto = s.dto.getClone();
        updatedDto.setId(null);
        s.dtoHasFullData = true;
        List<Dto> dtos = new ArrayList<>(100);
        try {
            disableDtoCache(s.q.getDto());
            forEach(s.q, dto -> {
                dto.set(updatedDto, s.action);
                updateById(s, dto);
                dtos.add(dto);
                return true;
            });
        } catch (NoContentException ignore) {

        } finally {
            enableDtoCache(s.q.getDto());
            updateDtoCache(s.q.getDto());
        }
        return dtos;
    }

    private void updateById(Settings s, Dto dto) throws VantarException {
        if (s.action == null) {
            s.action = Dto.Action.UPDATE_FEW_COLS;
        }

        Dto dtoBefore = s.eventCompare != null ? dto.getClone() : null;

        if (s.params != null) {
            if (s.exclude != null) {
                s.params.removeParams(s.exclude);
            }
            s.action = s.params.getX("action", s.action);
            if (s.isJson) {
                dto.set(s.key == null ? s.params.getJson() : s.params.getString(s.key), s.action);
            } else {
                dto.set(s.params, s.action);
            }
        }
        if (s.eventBeforeValidation != null) {
            s.eventBeforeValidation.write(dto);
        }
        List<ValidationError> errors = dto.validate(s.action);
        if (ObjectUtil.isNotEmpty(errors)) {
            throw new InputException(errors);
        }
        ModelMongoChecking.throwUniqueViolation(dto, db);
        ModelMongoChecking.throwDependencyViolations(dto, db);

        if (s.eventBeforeWrite != null) {
            s.eventBeforeWrite.write(dto);
        }
        if (s.eventCompare != null) {
            s.eventCompare.compare(dtoBefore, dto);
        }

        db.update(dto);
        ModelCommon.afterDataChange(dto, s);
        if (s.updateAutoIncrement) {
            setAutoIncrementToMax(dto);
        }
    }

    /**
     * by id
     * Options = fieldName1, value1, fieldName2, value2, ...
     */
    public void updateSimple(Dto dto, Object... options) throws VantarException {
        if (dto.getId() == null) {
            return;
        }
        Document updates = new Document();
        for (int i = 0, l = options.length; i < l; i += 2) {
            updates.append((String) options[i], options[i + 1]);
        }
        db.update(dto.getStorage(), dto.getId(), updates);
    }

    // < < < UPDATE


    // DELETE > > >

    public ResponseMessage delete(Settings settings) throws VantarException {
        if (settings.q != null) {
            settings.dto = settings.q.getDto();
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
            if (settings.eventAfterRead != null) {
                settings.eventAfterRead.read(settings.dto.getClone());
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
        return ResponseMessage.success(VantarKey.SUCCESS_DELETE, settings.getDeletedCount());
    }

    private void deleteByQuery(Settings s) throws VantarException {
        s.dtoHasFullData = true;
        try {
            disableDtoCache(s.q.getDto());
            forEach(s.q, dto -> {
                deleteById(s, dto);
                return true;
            });
        } catch (NoContentException ignore) {

        } finally {
            enableDtoCache(s.q.getDto());
            updateDtoCache(s.q.getDto());
        }
    }

    private void deleteById(Settings s, Dto dto) throws VantarException {
        if (s.action == null) {
            s.action = Dto.Action.DELETE;
        }
        if (s.eventBeforeWrite != null) {
            try {
                s.eventBeforeWrite.write(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }
        if (s.cascade) {
            DataDependencyMongo dep = new DataDependencyMongo(dto);
            dep.deleteCascade(dtoX -> {
                s.addDeletedCount(db.delete(dtoX));
                ModelCommon.afterDataChange(dtoX, s);
            });
        } else if (s.force) {
            s.addDeletedCount(db.delete(dto));
            ModelCommon.afterDataChange(dto, s);
        } else {
            new DataDependencyMongo(dto).throwOnFirstDependency();
            s.addDeletedCount(db.delete(dto));
            ModelCommon.afterDataChange(dto, s);
        }
        if (s.updateAutoIncrement) {
            setAutoIncrementToMax(dto);
        }
    }

    // < < < DELETE


    // PURGE > > >

    public ResponseMessage purge(Dto dto) throws VantarException {
        try {
            String collection = dto.getStorage();
            db.deleteAll(collection);
            db.autoIncrementRemove(collection);
            db.indexRemove(collection);
            if (Services.isUp(ServiceLog.class)) {
                ServiceLog.addAction(Dto.Action.PURGE, dto);
            }
            return ResponseMessage.success(VantarKey.SUCCESS_DELETE);
        } catch (Exception e) {
            ServiceLog.log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.FAIL_DELETE);
        }
    }

    // < < < PURGE


    // > > > ITEM WORKS

    public ResponseMessage addToList(Dto dto, String fieldName, Object item) throws VantarException {
        return addToCollection(dto, null, fieldName, item, true, true);
    }

    public ResponseMessage addToList(QueryBuilder q, String fieldName, Object item) throws VantarException {
        return addToCollection(q.getDto(), q, fieldName, item, true, true);
    }

    public ResponseMessage addToListNoLog(Dto dto, String fieldName, Object item) throws VantarException {
        return addToCollection(dto, null, fieldName, item, true, false);
    }

    public ResponseMessage addToListNoLog(QueryBuilder q, String fieldName, Object item) throws VantarException {
        return addToCollection(q.getDto(), q, fieldName, item, true, false);
    }

    public ResponseMessage addToSet(Dto dto, String fieldName, Object item) throws VantarException {
        return addToCollection(dto, null, fieldName, item, false, true);
    }

    public ResponseMessage addToSet(QueryBuilder q, String fieldName, Object item) throws VantarException {
        return addToCollection(q.getDto(), q, fieldName, item, false, true);
    }

    public ResponseMessage addToSetNoLog(Dto dto, String fieldName, Object item) throws VantarException {
        return addToCollection(dto, null, fieldName, item, false, false);
    }

    public ResponseMessage addToSetNoLog(QueryBuilder q, String fieldName, Object item) throws VantarException {
        return addToCollection(q.getDto(), q, fieldName, item, false, false);
    }

    private ResponseMessage addToCollection(Dto dto, QueryBuilder q, String fieldName, Object item
        , boolean isList, boolean logEvent) throws VantarException {

        db.addToCollection(
            dto.getStorage(),
            q == null ? new Document(DbMongo.ID, dto.getId()) : new MongoQuery(q).matches,
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

        return ResponseMessage.success(VantarKey.SUCCESS_UPDATE, dto);
    }

    public ResponseMessage removeFromCollection(Dto dto, String fieldName, Object item) throws VantarException {
        return removeFromCollection(dto, null, fieldName, item, true);
    }

    public ResponseMessage removeFromCollection(QueryBuilder q, String fieldName, Object item) throws VantarException {
        return removeFromCollection(q.getDto(), q, fieldName, item, true);
    }

    public ResponseMessage removeFromCollectionNoLog(Dto dto, String fieldName, Object item) throws VantarException {
        return removeFromCollection(dto, null, fieldName, item, false);
    }

    public ResponseMessage removeFromCollectionNoLog(QueryBuilder q, String fieldName, Object item) throws VantarException {
        return removeFromCollection(q.getDto(), q, fieldName, item, false);
    }

    private ResponseMessage removeFromCollection(Dto dto, QueryBuilder q, String fieldName, Object item
        , boolean logEvent) throws VantarException {

        db.removeFromCollection(
            dto.getStorage(),
            q == null ? new Document(DbMongo.ID, dto.getId()) : new MongoQuery(q).matches,
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

        return ResponseMessage.success(VantarKey.SUCCESS_UPDATE, dto);
    }

    public ResponseMessage addToMap(Dto dto, String fieldName, String key, Object item) throws VantarException {
        return addToMap(dto, null, fieldName, key, item, true);
    }

    public ResponseMessage addToMap(QueryBuilder q, String fieldName, String key, Object item) throws VantarException {
        return addToMap(q.getDto(), q, fieldName, key, item, true);
    }

    public ResponseMessage addToMapNoLog(Dto dto, String fieldName, String key, Object item) throws VantarException {
        return addToMap(dto, null, fieldName, key, item, false);
    }

    public ResponseMessage addToMapNoLog(QueryBuilder q, String fieldName, String key, Object item) throws VantarException {
        return addToMap(q.getDto(), q, fieldName, key, item, false);
    }

    private ResponseMessage addToMap(Dto dto, QueryBuilder q, String fieldName, String key, Object value
        , boolean logEvent) throws VantarException {

        db.addToMap(
            dto.getStorage(),
            q == null ? new Document(DbMongo.ID, dto.getId()) : new MongoQuery(q).matches,
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

        return ResponseMessage.success(VantarKey.SUCCESS_UPDATE, dto);
    }

    public ResponseMessage removeFromMap(Dto dto, String fieldName, String key) throws VantarException {
        return removeFromMap(dto, null, fieldName, key, true);
    }

    public ResponseMessage removeFromMap(QueryBuilder q, String fieldName, String key) throws VantarException {
        return removeFromMap(q.getDto(), q, fieldName, key, true);
    }

    public ResponseMessage removeFromMapNoLog(Dto dto, String fieldName, String key) throws VantarException {
        return removeFromMap(dto, null, fieldName, key, false);
    }

    public ResponseMessage removeFromMapNoLog(QueryBuilder q, String fieldName, String key) throws VantarException {
        return removeFromMap(q.getDto(), q, fieldName, key, false);
    }

    private ResponseMessage removeFromMap(Dto dto, QueryBuilder q, String fieldName, String key, boolean logEvent)
        throws VantarException {

        db.removeFromMap(
            dto.getStorage(),
            q == null ? new Document(DbMongo.ID, dto.getId()) : new MongoQuery(q).matches,
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

        return ResponseMessage.success(VantarKey.SUCCESS_UPDATE, dto);
    }

    // ITEM WORKS < < <


    // GET DATA > > >

    public void forEach(Dto dto, QueryResultBase.EventForeach event) throws VantarException {
        db.getAllData(dto).forEach(event);
    }

    public void forEach(QueryBuilder q, QueryResultBase.EventForeach event) throws VantarException {
        QueryResult result = db.getData(q);
        result.setLocale(q.getLocale());
        result.forEach(event);
    }

    public PageData search(Params params, Dto dto) throws VantarException {
        return search(params, dto, null);
    }

    public PageData search(Params params, Dto dto, QueryEvent event) throws VantarException {
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

    public PageData searchForEach(Params params, Dto dto, QueryEventForeach event) throws VantarException {
        QueryBuilder q = params.getQueryBuilder(dto);
        if (q == null) {
            q = new QueryBuilder(dto);
        }

        if (event != null) {
            event.beforeQuery(q);
        }

        return searchForEach(q, event, params.getLang());
    }

    public PageData search(QueryBuilder q, String... locales) throws VantarException {
        return search(q, null, locales);
    }

    public PageData search(QueryBuilder q, QueryEvent event, String... locales) throws VantarException {
        if (q.isPagination()) {
            return db.getPage(q, event, locales);
        } else {
            QueryResult result = db.getData(q);
            if (event != null) {
                result.setEvent(event);
            }
            if (ObjectUtil.isNotEmpty(locales)) {
                result.setLocale(locales);
            }
            PageData data = new PageData(result.asList());
            List<ValidationError> errors = q.getErrors();
            if (errors != null) {
                data.errors = ValidationError.toString(errors);
            }
            return data;
        }
    }

    public PageData searchForEach(QueryBuilder q, QueryResultBase.EventForeach event, String... locales) throws VantarException {
        PageData data = db.getPageForeach(q, event, locales);
        List<ValidationError> errors = q.getErrors();
        if (errors != null) {
            data.errors = ValidationError.toString(errors);
        }
        return data;
    }

    public <D extends Dto> List<D> getData(QueryBuilder q, String... locales) throws VantarException {
        return getData(q, null, locales);
    }

    public <D extends Dto> List<D> getData(Params params, QueryBuilder q) throws VantarException {
        return getData(params, q, null);
    }

    public <D extends Dto> List<D> getData(Params params, QueryBuilder q, QueryEvent event) throws VantarException {
        return getData(q, event, params.getLang());
    }

    public <D extends Dto> List<D> getData(QueryBuilder q, QueryEvent event, String... locales) throws VantarException {
        QueryResult result = db.getData(q);
        if (event != null) {
            result.setEvent(event);
        }
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asList();
    }

    public <D extends Dto> Map<Object, D> getMap(QueryBuilder q, String keyProperty, String... locales) throws VantarException {
        return getMap(q, keyProperty, null, locales);
    }

    public <D extends Dto> Map<Object, D> getMap(Params params, String keyProperty, QueryBuilder q) throws VantarException {
        return getMap(params, keyProperty, q, null);
    }

    public <D extends Dto> Map<Object, D> getMap(Params params, String keyProperty, QueryBuilder q, QueryEvent event)
        throws VantarException {

        return getMap(q, keyProperty, event, params.getLang());
    }

    public <D extends Dto> Map<Object, D> getMap(QueryBuilder q, String keyProperty, QueryEvent event, String... locales)
        throws VantarException {

        QueryResult result = db.getData(q);
        if (event != null) {
            result.setEvent(event);
        }
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asMap(keyProperty);
    }

    public <D extends Dto> Map<Object, D> getMap(Dto dto, String keyProperty, String... locales) throws VantarException {
        QueryResult result = db.getAllData(dto);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asMap(keyProperty);
    }

    public Collection<Object> getPropertyList(QueryBuilder q, String property, String... locales) throws VantarException {
        QueryResult result = db.getData(q);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asPropertyList(property);
    }

    public Collection<Object> getPropertyList(Dto dto, String property, String... locales) throws VantarException {
        QueryResult result = db.getAllData(dto);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asPropertyList(property);
    }

    public Map<Long, Object> getPropertyMap(QueryBuilder q, String property, String... locales) throws VantarException {
        QueryResult result = db.getData(q);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asPropertyMap(property);
    }

    public Map<Long, Object> getPropertyMap(Dto dto, String property, String... locales) throws VantarException {
        QueryResult result = db.getAllData(dto);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.asPropertyMap(property);
    }

    public <D extends Dto> D getFirst(QueryBuilder q, String... locales) throws VantarException {
        QueryResult result = db.getData(q);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.first();
    }

    public <D extends Dto> D getFirst(Params params, QueryBuilder q) throws VantarException {
        String locales = params.getLang();
        QueryResult result = db.getData(q);
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.first();
    }

    public <D extends Dto> D getById(Params params, D dto) throws VantarException {
        return getById(params, VantarParam.ID, dto);
    }

    @SuppressWarnings("unchecked")
    public <D extends Dto> D getById(Params params, String idParam, D dto) throws VantarException {
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
    public <D extends Dto> D getById(D dto, Long id, String... locales) throws VantarException {
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
    public <D extends Dto> D getById(D dto, String... locales) throws VantarException {
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

    private <T extends Dto> T getByIdX(T dto, String... locales) throws VantarException {
        QueryResult result =
            new MongoQueryResult(
                db.getDatabase()
                    .getCollection(dto.getStorage())
                    .find(new Document(DbMongo.ID, dto.getId())),
                dto,
                db
            );
        if (ObjectUtil.isNotEmpty(locales)) {
            result.setLocale(locales);
        }
        return result.first();
    }

    public <T extends Dto> List<T> getAll(T dto) throws VantarException {
        return getAll(null, dto, null, null);
    }

    public <T extends Dto> List<T> getAll(T dto, QueryEvent event) throws VantarException {
        return getAll(null, dto, event, null);
    }

    public <T extends Dto> List<T> getAll(T dto, String... locales) throws VantarException {
        return getAll(null, dto, null, locales);
    }

    public <T extends Dto> List<T> getAll(T dto, QueryEvent event, String... locales) throws VantarException {
        return getAll(null, dto, event, locales);
    }

    /**
     * Automatically fetches: cache
     * lang only for database
     */
    public <T extends Dto> List<T> getAll(Params params, T dto) throws VantarException {
        return getAll(params, dto, null, null);
    }

    @SuppressWarnings("unchecked")
    private <T extends Dto> List<T> getAll(Params params, T dto, QueryEvent event, String[] locales) throws VantarException {
        if (dto.hasAnnotation(Cache.class)) {
            List<T> d = (List<T>) Services.getService(ServiceDtoCache.class).getList(dto.getClass());
            if (d == null) {
                throw new NoContentException();
            }
            return d;
        }
        QueryResult result = db.getAllData(dto);
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
    }

    /**
     * Automatically fetches: cache, lang
     */
    public <D extends Dto, L extends Dto> List<L> getAllFromCache(
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
    public Map<String, String> getKeyValue(Dto dto, String keyProperty, String valueProperty) throws VantarException {
        return getKeyValue(null, dto, keyProperty, valueProperty);
    }

    public Map<String, String> getKeyValue(Params params, Dto dto, String keyProperty, String valueProperty)
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
            QueryResult result = db.getAllData(dto);
            if (StringUtil.isNotEmpty(lang)) {
                result.setLocale(lang);
            }
            return result.asKeyValue(keyProperty, valueProperty);
        }
    }

    public Map<String, String> getKeyValue(QueryBuilder q, String keyProperty, String valueProperty, String... locales)
        throws VantarException {

        QueryResult result = db.getData(q);
        result.setLocale(locales);
        return result.asKeyValue(keyProperty, valueProperty);
    }


    // > > > count exists


    public long count(QueryBuilder q) throws VantarException {
        return db.count(q);
    }

    public long count(String collectionName) throws VantarException {
        return db.count(collectionName);
    }

    public boolean exists(QueryBuilder q) throws VantarException {
        return db.exists(q);
    }

    public boolean exists(Dto dto, String property) throws VantarException {
        return db.exists(dto, property);
    }

    public boolean existsById(Dto dto) throws VantarException {
        return db.existsById(dto);
    }

    public boolean existsByDto(Dto dto) throws VantarException {
        return db.existsByDto(dto);
    }
}