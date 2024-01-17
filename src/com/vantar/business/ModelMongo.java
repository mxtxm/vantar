package com.vantar.business;

import com.vantar.common.VantarParam;
import com.vantar.database.common.*;
import com.vantar.database.dependency.DataDependency;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.query.*;
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

/**
 * params  write methods: +events +validation +mutex +log
 * normal  write methods: +events +validation +mutex +log
 * noLog   write methods: +events +validation +mutex -log
 * NoMutex write methods: +events +validation -mutex -log
 */
public class ModelMongo extends CommonModel {


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
    private static ResponseMessage insertX(Object params, Dto dto, WriteEvent event) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> {
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
                errors = getRelationValueViolation(dto);
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

            CommonModel.afterDataChange(dto, event, true, Dto.Action.INSERT);

            return ResponseMessage.success(VantarKey.INSERT_SUCCESS, dto.getId(), dto);
        });
    }

    public static ResponseMessage insert(Dto dto) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> insertY(dto, true, null));
    }
    public static ResponseMessage insert(Dto dto, WriteEvent event) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> insertY(dto, true, event));
    }
    public static ResponseMessage insertNoLog(Dto dto) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> insertY(dto, false, null));
    }
    public static ResponseMessage insertNoLog(Dto dto, WriteEvent event) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> insertY(dto, false, event));
    }
    public static ResponseMessage insertNoMutex(Dto dto) throws VantarException {
        return insertY(dto, true, null);
    }
    public static ResponseMessage insertNoMutex(Dto dto, WriteEvent event) throws VantarException {
        return insertY(dto, true, event);
    }
    public static ResponseMessage insertNoMutexNoLog(Dto dto) throws VantarException {
        return insertY(dto, false, null);
    }
    public static ResponseMessage insertNoMutexNoLog(Dto dto, WriteEvent event) throws VantarException {
        return insertY(dto, false, event);
    }
    private static ResponseMessage insertY(Dto dto, boolean logEvent, WriteEvent event) throws VantarException {
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
            errors = getRelationValueViolation(dto);
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

        CommonModel.afterDataChange(dto, event, logEvent, Dto.Action.INSERT);

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
    public static ResponseMessage updateJson(Params params, String key, Dto dto, WriteEvent event) throws VantarException {
        return updateX(params.getString(key), dto, event, params.getX("action", Dto.Action.UPDATE_FEW_COLS));
    }
    private static ResponseMessage updateX(Object params, Dto dto, WriteEvent event, Dto.Action action) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> {
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
                errors = getRelationValueViolation(dto);
                if (errors != null) {
                    throw new InputException(errors);
                }
                Mongo.update(dto);
            } catch (DatabaseException e) {
                log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
                throw new ServerException(VantarKey.UPDATE_FAIL);
            }

            CommonModel.afterDataChange(dto, event, true, action);

            return ResponseMessage.success(VantarKey.UPDATE_SUCCESS, dto);
        });
    }

    public static ResponseMessage update(Dto dto) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, null, Dto.Action.UPDATE_FEW_COLS, true, null));
    }
    public static ResponseMessage update(Dto dto, Dto.Action action) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, null, action, true, null));
    }
    public static ResponseMessage update(Dto dto, WriteEvent event) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, null, Dto.Action.UPDATE_FEW_COLS, true, event));
    }
    public static ResponseMessage update(Dto dto, Dto.Action action, WriteEvent event) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, null, action, true, event));
    }

    public static ResponseMessage update(QueryBuilder q) throws VantarException {
        Dto dto = q.getDto();
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, q, Dto.Action.UPDATE_FEW_COLS, true, null));
    }
    public static ResponseMessage update(QueryBuilder q, Dto.Action action) throws VantarException {
        Dto dto = q.getDto();
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, q, action, true, null));
    }
    public static ResponseMessage update(QueryBuilder q, WriteEvent event) throws VantarException {
        Dto dto = q.getDto();
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, q, Dto.Action.UPDATE_FEW_COLS, true, event));
    }
    public static ResponseMessage update(QueryBuilder q, Dto.Action action, WriteEvent event) throws VantarException {
        Dto dto = q.getDto();
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, q, action, true, event));
    }

    public static ResponseMessage updateNoLog(Dto dto) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, null, Dto.Action.UPDATE_FEW_COLS, false, null));
    }
    public static ResponseMessage updateNoLog(Dto dto, Dto.Action action) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, null, action, false, null));
    }
    public static ResponseMessage updateNoLog(Dto dto, WriteEvent event) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, null, Dto.Action.UPDATE_FEW_COLS, false, event));
    }
    public static ResponseMessage updateNoLog(Dto dto, Dto.Action action, WriteEvent event) throws VantarException {
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, null, action, false, event));
    }

    public static ResponseMessage updateNoLog(QueryBuilder q) throws VantarException {
        Dto dto = q.getDto();
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, q, Dto.Action.UPDATE_FEW_COLS, false, null));
    }
    public static ResponseMessage updateNoLog(QueryBuilder q, Dto.Action action) throws VantarException {
        Dto dto = q.getDto();
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, q, action, false, null));
    }
    public static ResponseMessage updateNoLog(QueryBuilder q, WriteEvent event) throws VantarException {
        Dto dto = q.getDto();
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, q, Dto.Action.UPDATE_FEW_COLS, false, event));
    }
    public static ResponseMessage updateNoLog(QueryBuilder q, Dto.Action action, WriteEvent event) throws VantarException {
        Dto dto = q.getDto();
        return (ResponseMessage) mutex(dto.getClass(), () -> updateY(dto, q, action, false, event));
    }

    public static ResponseMessage updateNoMutex(Dto dto) throws VantarException {
        return updateY(dto, null, Dto.Action.UPDATE_FEW_COLS, true, null);
    }
    public static ResponseMessage updateNoMutex(Dto dto, Dto.Action action) throws VantarException {
        return updateY(dto, null, action, true, null);
    }
    public static ResponseMessage updateNoMutex(Dto dto, WriteEvent event) throws VantarException {
        return updateY(dto, null, Dto.Action.UPDATE_FEW_COLS, true, event);
    }
    public static ResponseMessage updateNoMutex(Dto dto, Dto.Action action, WriteEvent event) throws VantarException {
        return updateY(dto, null, action, true, event);
    }

    public static ResponseMessage updateNoMutex(QueryBuilder q) throws VantarException {
        return updateY(q.getDto(), q, Dto.Action.UPDATE_FEW_COLS, true, null);
    }
    public static ResponseMessage updateNoMutex(QueryBuilder q, Dto.Action action) throws VantarException {
        return updateY(q.getDto(), q, action, true, null);
    }
    public static ResponseMessage updateNoMutex(QueryBuilder q, WriteEvent event) throws VantarException {
        return updateY(q.getDto(), q, Dto.Action.UPDATE_FEW_COLS, true, event);
    }
    public static ResponseMessage updateNoMutex(QueryBuilder q, Dto.Action action, WriteEvent event) throws VantarException {
        return updateY(q.getDto(), q, action, true, event);
    }

    public static ResponseMessage updateNoMutexNoLog(Dto dto) throws VantarException {
        return updateY(dto, null, Dto.Action.UPDATE_FEW_COLS, false, null);
    }
    public static ResponseMessage updateNoMutexNoLog(Dto dto, Dto.Action action) throws VantarException {
        return updateY(dto, null, action, false, null);
    }
    public static ResponseMessage updateNoMutexNoLog(Dto dto, WriteEvent event) throws VantarException {
        return updateY(dto, null, Dto.Action.UPDATE_FEW_COLS, false, event);
    }
    public static ResponseMessage updateNoMutexNoLog(Dto dto, Dto.Action action, WriteEvent event) throws VantarException {
        return updateY(dto, null, action, false, event);
    }

    public static ResponseMessage updateNoMutexNoLog(QueryBuilder q) throws VantarException {
        return updateY(q.getDto(), q, Dto.Action.UPDATE_FEW_COLS, false, null);
    }
    public static ResponseMessage updateNoMutexNoLog(QueryBuilder q, Dto.Action action) throws VantarException {
        return updateY(q.getDto(), q, action, false, null);
    }
    public static ResponseMessage updateNoMutexNoLog(QueryBuilder q, WriteEvent event) throws VantarException {
        return updateY(q.getDto(), q, Dto.Action.UPDATE_FEW_COLS, false, event);
    }
    public static ResponseMessage updateNoMutexNoLog(QueryBuilder q, Dto.Action action, WriteEvent event) throws VantarException {
        return updateY(q.getDto(), q, action, false, event);
    }

    private static ResponseMessage updateY(Dto dto, QueryBuilder q, Dto.Action action, boolean logEvent, WriteEvent event)
        throws VantarException {

        boolean updateFewCols = action.equals(Dto.Action.UPDATE_FEW_COLS);
        if (q != null) {
            action = updateFewCols ? Dto.Action.UPDATE_FEW_COLS_NO_ID : Dto.Action.UPDATE_ALL_COLS_NO_ID;
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
            errors = getRelationValueViolation(dto);
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

        CommonModel.afterDataChange(dto, event, logEvent, action);

        return ResponseMessage.success(VantarKey.UPDATE_SUCCESS, dto);
    }

    // < < < UPDATE


    // DELETE > > >

    public static ResponseMessage delete(Params params, Dto dto) throws VantarException {
        return delete(params, dto, null);
    }
    public static ResponseMessage delete(Params params, Dto dtoX, WriteEvent event) throws VantarException {
        dtoX.setId(params.getLong(VantarParam.ID, params.extractFromJson(VantarParam.ID, Long.class)));
        if (NumberUtil.isIdInvalid(dtoX.getId())) {
            throw new InputException(VantarKey.EMPTY_ID, VantarParam.ID);
        }

        return (ResponseMessage) mutex(dtoX, (Dto dto) -> {
            if (event != null) {
                try {
                    event.beforeSet(dto);
                    event.beforeWrite(dto);
                } catch (NoContentException e) {
                    throw new InputException(VantarKey.NO_CONTENT);
                }
            }

            List<DataDependency.Dependants> items = new DataDependency(dto, 1).getDependencies();
            if (!items.isEmpty()) {
                throw new InputException(VantarKey.DELETE_FAIL_HAS_DEPENDENCIES, DataDependency.toString(items));
            }

            try {
                ResponseMessage r = ResponseMessage.success(VantarKey.DELETE_SUCCESS, Mongo.delete(dto));
                CommonModel.afterDataChange(dto, event, true, Dto.Action.DELETE);
                return r;
            } catch (DatabaseException e) {
                log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
                throw new ServerException(VantarKey.DELETE_FAIL);
            }
        });
    }

    public static ResponseMessage deleteById(Dto dtoX) throws VantarException {
        if (NumberUtil.isIdInvalid(dtoX.getId())) {
            throw new InputException(VantarKey.EMPTY_ID, VantarParam.ID);
        }
        return (ResponseMessage) mutex(dtoX, (Dto dto) -> deleteByIdX(dto, true, null, null));
    }
    public static ResponseMessage deleteById(Dto dtoX, WriteEvent event) throws VantarException {
        if (NumberUtil.isIdInvalid(dtoX.getId())) {
            throw new InputException(VantarKey.EMPTY_ID, VantarParam.ID);
        }
        return (ResponseMessage) mutex(dtoX, (Dto dto) -> deleteByIdX(dto, true, event, null));
    }
    public static ResponseMessage deleteByIdNoLog(Dto dtoX) throws VantarException {
        if (NumberUtil.isIdInvalid(dtoX.getId())) {
            throw new InputException(VantarKey.EMPTY_ID, VantarParam.ID);
        }
        return (ResponseMessage) mutex(dtoX, (Dto dto) -> deleteByIdX(dto, false, null, null));
    }
    public static ResponseMessage deleteByIdNoLog(Dto dtoX, WriteEvent event) throws VantarException {
        if (NumberUtil.isIdInvalid(dtoX.getId())) {
            throw new InputException(VantarKey.EMPTY_ID, VantarParam.ID);
        }
        return (ResponseMessage) mutex(dtoX, (Dto dto) -> deleteByIdX(dto, false, event, null));
    }
    public static ResponseMessage deleteByIdNoMutex(Dto dto) throws VantarException {
        if (NumberUtil.isIdInvalid(dto.getId())) {
            throw new InputException(VantarKey.EMPTY_ID, VantarParam.ID);
        }
        return deleteByIdX(dto, true, null, null);
    }
    public static ResponseMessage deleteByIdNoMutex(Dto dto, WriteEvent event) throws VantarException {
        if (NumberUtil.isIdInvalid(dto.getId())) {
            throw new InputException(VantarKey.EMPTY_ID, VantarParam.ID);
        }
        return deleteByIdX(dto, true, event, null);
    }
    public static ResponseMessage deleteByIdNoMutexNoLog(Dto dto) throws VantarException {
        if (NumberUtil.isIdInvalid(dto.getId())) {
            throw new InputException(VantarKey.EMPTY_ID, VantarParam.ID);
        }
        return deleteByIdX(dto, false, null, null);
    }
    public static ResponseMessage deleteByIdNoMutexNoLog(Dto dto, WriteEvent event) throws VantarException {
        if (NumberUtil.isIdInvalid(dto.getId())) {
            throw new InputException(VantarKey.EMPTY_ID, VantarParam.ID);
        }
        return deleteByIdX(dto, false, event, null);
    }
    private static ResponseMessage deleteByIdX(Dto dto, boolean logEvent, WriteEvent event
        , List<DataDependency.Dependants> dependants) throws VantarException {

        if (event != null) {
            try {
                event.beforeSet(dto);
                event.beforeWrite(dto);
            } catch (NoContentException e) {
                throw new InputException(VantarKey.NO_CONTENT);
            }
        }

        List<DataDependency.Dependants> items = new DataDependency(dto, 1).getDependencies();
        if (!items.isEmpty()) {
            if (dependants == null) {
                throw new InputException(VantarKey.DELETE_FAIL_HAS_DEPENDENCIES, DataDependency.toString(items));
            }
            dependants.addAll(items);
            return null;
        }

        try {
            ResponseMessage r = ResponseMessage.success(VantarKey.DELETE_SUCCESS, Mongo.delete(dto));
            CommonModel.afterDataChange(dto, event, logEvent, Dto.Action.DELETE);
            return r;
        } catch (DatabaseException e) {
            log.error(" !! {} : {}\n", dto.getClass().getSimpleName(), dto, e);
            throw new ServerException(VantarKey.DELETE_FAIL);
        }
    }

    public static ResponseMessage delete(QueryBuilder q) throws VantarException {
        return deleteByQuery(q, true, true, null);
    }
    public static ResponseMessage delete(QueryBuilder q, WriteEvent event) throws VantarException {
        return deleteByQuery(q, true, true, event);
    }
    public static ResponseMessage deleteNoLog(QueryBuilder q) throws VantarException {
        return deleteByQuery(q, false, true, null);
    }
    public static ResponseMessage deleteNoLog(QueryBuilder q, WriteEvent event) throws VantarException {
        return deleteByQuery(q, false, true, event);
    }
    public static ResponseMessage deleteNoMutex(QueryBuilder q) throws VantarException {
        return deleteByQuery(q, false, true, null);
    }
    public static ResponseMessage deleteNoMutex(QueryBuilder q, WriteEvent event) throws VantarException {
        return deleteByQuery(q, false, true, event);
    }
    public static ResponseMessage deleteNoMutexNoLog(QueryBuilder q) throws VantarException {
        return deleteByQuery(q, false, false, null);
    }
    public static ResponseMessage deleteNoMutexNoLog(QueryBuilder q, WriteEvent event) throws VantarException {
        return deleteByQuery(q, false, false, event);
    }
    private static ResponseMessage deleteByQuery(QueryBuilder q, boolean logEvent, boolean mutex, WriteEvent event)
        throws VantarException {

        List<DataDependency.Dependants> dependants = new ArrayList<>(10);
        Dto sample = null;
        try {
            List<Dto> data = getData(q);
            sample = data.get(0);
            disableDtoCache(sample);
            for (Dto dto : data) {
                if (mutex) {
                    mutex(dto, dtoX -> {
                        deleteByIdX(dtoX, logEvent, event, dependants);
                        return null;
                    });
                } else {
                    deleteByIdX(dto, logEvent, event, dependants);
                }
            }
        } catch (NoContentException nc) {
            return ResponseMessage.success(VantarKey.DELETE_SUCCESS);
        } finally {
            if (sample != null) {
                enableDtoCache(sample, event);
            }
        }

        if (!dependants.isEmpty()) {
            throw new InputException(VantarKey.DELETE_FAIL_HAS_DEPENDENCIES, DataDependency.toString(dependants));
        }

        return ResponseMessage.success(VantarKey.DELETE_SUCCESS);
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

        List<DataDependency.Dependants> dependants = new ArrayList<>(10);
        for (Long id : ids) {
            dto.setId(id);
            deleteByIdX(dto, true, event, dependants);
        }

        if (!dependants.isEmpty()) {
            throw new InputException(VantarKey.DELETE_FAIL_HAS_DEPENDENCIES, DataDependency.toString(dependants));
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

                afterDataChange(dto, event, true, Dto.Action.UN_DELETE);
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

        if (logEvent && Services.isUp(ServiceUserActionLog.class)) {
            Map<String, Object> object = new HashMap<>(1, 1);
            object.put(fieldName, item);
            ServiceUserActionLog.add(
                Dto.Action.UPDATE_ADD_ITEM,
                new ServiceUserActionLog.DtoLogAction(dto.getClass(), dto.getId(), object)
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

        if (logEvent && Services.isUp(ServiceUserActionLog.class)) {
            Map<String, Object> object = new HashMap<>(1, 1);
            object.put(fieldName, item);
            ServiceUserActionLog.add(
                Dto.Action.UPDATE_REMOVE_ITEM,
                new ServiceUserActionLog.DtoLogAction(dto.getClass(), dto.getId(), object)
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

        if (logEvent && Services.isUp(ServiceUserActionLog.class)) {
            Map<String, Object> object = new HashMap<>(1, 1);
            object.put(fieldName + key, value);
            ServiceUserActionLog.add(
                Dto.Action.UPDATE_ADD_ITEM,
                new ServiceUserActionLog.DtoLogAction(dto.getClass(), dto.getId(), object)
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

        if (logEvent && Services.isUp(ServiceUserActionLog.class)) {
            Map<String, Object> object = new HashMap<>(1, 1);
            object.put(fieldName, key);
            ServiceUserActionLog.add(
                Dto.Action.UPDATE_REMOVE_ITEM,
                new ServiceUserActionLog.DtoLogAction(dto.getClass(), dto.getId(), object)
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
            data =  MongoQuery.getPageForeach(q, event, locales);
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
            D d = (D) Services.get(ServiceDtoCache.class).getMap(dto.getClass()).get(id);
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
            D d = (D) Services.get(ServiceDtoCache.class).getMap(dto.getClass()).get(id);
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
    public static Map<String, String> getKeyValue(Dto dto, String keyProperty, String valueProperty) throws VantarException {
        return getKeyValue(null, dto, keyProperty, valueProperty);
    }

    public static Map<String, String> getKeyValue(Params params, Dto dto, String keyProperty, String valueProperty)
        throws VantarException {

        String lang = params == null ? null : params.getLangNoDefault();
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





    // > > > checking



    public static List<ValidationError> getUniqueViolation(Dto dto) throws DatabaseException {
        List<ValidationError> errors = null;
        for (String fieldName : dto.annotatedProperties(Unique.class)) {
            if (!MongoQuery.isUnique(dto, fieldName)) {
                if (errors == null) {
                    errors = new ArrayList<>(5);
                }
                errors.add(new ValidationError(fieldName, VantarKey.UNIQUE));
            }
        }
        for (String fieldName : dto.annotatedProperties(UniqueCi.class)) {
            if (!MongoQuery.isUnique(dto, fieldName)) {
                if (errors == null) {
                    errors = new ArrayList<>(5);
                }
                errors.add(new ValidationError(fieldName, VantarKey.UNIQUE));
            }
        }
        if (dto.getClass().isAnnotationPresent(UniqueGroup.class)) {
            for (String group : dto.getClass().getAnnotation(UniqueGroup.class).value()) {
                if (!MongoQuery.isUnique(dto, StringUtil.split(group, VantarParam.SEPARATOR_COMMON))) {
                    if (errors == null) {
                        errors = new ArrayList<>(5);
                    }
                    errors.add(new ValidationError("(" + group + ")", VantarKey.UNIQUE));
                }
            }
        }

        return errors;
    }

    @SuppressWarnings("unchecked")
    public static List<ValidationError> getRelationViolation(Dto dto) {
        List<ValidationError> errors = new ArrayList<>(5);
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
        boolean exists;
        try {
            exists = Mongo.exists(
                DtoBase.getStorage(dto.getAnnotation(name, Depends.class).value()),
                new Document(Mongo.ID, value)
            );
        } catch (DatabaseException e) {
            log.error(" !! could not check reference dto={} field={} value={}", dto, name, value, e);
            return;
        }
        if (!exists) {
            errors.add(new ValidationError(name, VantarKey.REFERENCE, value));
        }
    }

    public static List<ValidationError> getRelationValueViolation(Dto dto) {
        List<ValidationError> errors = new ArrayList<>();
        for (String name : dto.annotatedProperties(DependsValue.class)) {
            Object value = dto.getPropertyValue(name);
            if (value != null) {
                if (ClassUtil.isInstantiable(value.getClass(), Collection.class)) {
                    if (ClassUtil.getGenericTypes(dto.getField(name))[0].equals(Long.class)) {
                        for (Object v : (Collection<?>) value) {
                            checkValueRelation(dto, name, v, errors);
                        }
                    }
                } else if (value instanceof Long) {
                    checkValueRelation(dto, name, value, errors);
                }
            }
        }
        return errors.isEmpty() ? null : errors;
    }

    private static void checkValueRelation(Dto dto, String name, Object value, List<ValidationError> errors) {
        boolean exists;
        DependsValue depends = dto.getAnnotation(name, DependsValue.class);
        try {
            exists = Mongo.exists(DtoBase.getStorage(depends.dto()), new Document(depends.field(), value));
        } catch (DatabaseException e) {
            log.error(" !! could not check reference dto={} field={} value={}", dto, name, value, e);
            return;
        }
        if (!exists) {
            errors.add(new ValidationError(name, VantarKey.REFERENCE, value));
        }
    }

    /**
     * if a property is depended on the same object (is child)
     *     if parent.id=child.id ---> throw error
     */
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
                        errors = new ArrayList<>(5);
                    }
                    errors.add(new ValidationError(name, VantarKey.REFERENCE));
                }
            }
        }
        return errors;
    }

}