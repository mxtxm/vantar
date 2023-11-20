package com.vantar.business;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.MongoQuery;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.service.auth.CommonUserPassword;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public abstract class CommonModel {

    protected static final Logger log = LoggerFactory.getLogger(CommonModel.class);

    private static Set<String> disabledDtoClasses;
    private static final Map<String, Object> locks = new ConcurrentHashMap<>(500, 1);


    public static void afterDataChange(Dto dto) {
        afterDataChange(dto.getClass());
    }

    public static void afterDataChange(Class<? extends Dto> dtoClass) {
        if (dtoClass.isAnnotationPresent(Cache.class)) {
            String dtoName = dtoClass.getSimpleName();
            if (disabledDtoClasses != null && disabledDtoClasses.contains(dtoName)) {
                return;
            }
            ServiceDtoCache service;
            try {
                service = Services.get(ServiceDtoCache.class);
                service.update(dtoName);
            } catch (ServiceException ignore) {

            }
            Services.messaging.broadcast(VantarParam.MESSAGE_DATABASE_UPDATED, dtoName);
        }
    }

    public static void disableDtoCache(Dto dto) {
        disableDtoCache(dto.getClass());
    }

    public static void disableDtoCache(Class<? extends Dto> dtoClass) {
        String dtoName = dtoClass.getSimpleName();
        if (disabledDtoClasses == null) {
            disabledDtoClasses = new HashSet<>(5, 1);
        }
        disabledDtoClasses.add(dtoName);
    }

    public static void enableDtoCache(Dto dto) {
        enableDtoCache(dto.getClass());
    }

    public static void enableDtoCache(Class<? extends Dto> dtoClass) {
        String dtoName = dtoClass.getSimpleName();
        if (disabledDtoClasses != null) {
            disabledDtoClasses.remove(dtoName);
        }
        afterDataChange(dtoClass);
    }

    public static void insertPassword(Dto user, String password) throws ServerException {
        if (StringUtil.isEmpty(password)) {
            return;
        }
        for (DtoDictionary.Info info: DtoDictionary.getAll()) {
            if (ClassUtil.isInstantiable(info.dtoClass, CommonUserPassword.class)) {
                if (user.getClass().equals(info.dtoClass)) {
                    break;
                }
                CommonUserPassword userPassword = (CommonUserPassword) info.getDtoInstance();
                userPassword.setClearIdOnInsert(false);
                userPassword.setId(user.getId());
                userPassword.setPassword(password);
                try {
                    if (MongoQuery.existsById(userPassword)) {
                        CommonModelMongo.update(userPassword);
                    } else {
                        CommonModelMongo.insert(userPassword);
                    }
                } catch (DatabaseException | VantarException e) {
                    throw new ServerException(VantarKey.FETCH_FAIL);
                }
                break;
            }
        }
    }

    /**
     * Validate values
     * @param objs "obj1", obj1, "obj2", obj2, ...
     * @throws InputException when object is empty
     */
    public static void validateRequired(Object... objs) throws InputException {
        StringBuilder fields = new StringBuilder();
        String fieldName = null;
        for (int i = 0, l = objs.length; i < l; ++i) {
            Object obj = objs[i];
            if (i % 2 == 0) {
                fieldName = (String) obj;
                continue;
            }
            if (obj == null
                || (obj instanceof Long && NumberUtil.isIdInvalid((Long) obj))
                || ObjectUtil.isEmpty(obj)) {

                fields.append(fieldName).append(", ");
            }
        }
        if (fields.length() > 0) {
            fields.setLength(fields.length() - 2);
            throw new InputException(VantarKey.REQUIRED, fields.toString());
        }
    }

    public static Object mutex(String key, Mutex m) throws Exception {
        MutexParams mp = new MutexParams();
        locks.computeIfAbsent(key, k -> {
            try {
                mp.returnValue = m.block();
            } catch (VantarException e) {
                mp.exception = e;
            }
            return null;
        });
        if (mp.exception != null) {
            throw mp.exception;
        }
        return mp.returnValue;
    }

    public static Object mutex(Dto dto, MutexDto m) throws Exception {
        MutexParams mp = new MutexParams();
        locks.computeIfAbsent(dto.getClass().getSimpleName() + dto.getId(), k -> {
            try {
                mp.returnValue = m.block(dto);
            } catch (VantarException e) {
                mp.exception = e;
            }
            return null;
        });
        if (mp.exception != null) {
            throw mp.exception;
        }
        return mp.returnValue;
    }

    public static Object mutex(Class<? extends Dto> dtoClass, Long id, Mutex m) throws Exception {
        MutexParams mp = new MutexParams();
        locks.computeIfAbsent(dtoClass.getSimpleName() + id, k -> {
            try {
                mp.returnValue = m.block();
            } catch (VantarException e) {
                mp.exception = e;
            }
            return null;
        });
        if (mp.exception != null) {
            throw mp.exception;
        }
        return mp.returnValue;
    }


    public interface Mutex {

        Object block() throws VantarException;
    }

    public interface MutexDto {

        Object block(Dto dto) throws VantarException;
    }

    private static class MutexParams {

        public Exception exception;
        public Object returnValue;
    }


    public interface WriteEvent {

        void beforeSet(Dto dto) throws VantarException;
        void beforeWrite(Dto dto) throws VantarException;
        void afterWrite(Dto dto) throws VantarException;
    }


    public interface QueryEvent extends QueryResultBase.Event {

        void beforeQuery(QueryBuilder q) throws VantarException;

    }


    public interface QueryEventForeach extends QueryResultBase.EventForeach {

        void beforeQuery(QueryBuilder q) throws VantarException;

    }


    public interface BatchEvent {

        boolean beforeInsert(Dto dto);
        boolean beforeUpdate(Dto dto);
        boolean beforeDelete(Dto dto);
    }
}
