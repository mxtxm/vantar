package com.vantar.business;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.service.auth.CommonUserPassword;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.service.log.ServiceLog;
import com.vantar.service.log.dto.*;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.Params;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public abstract class ModelCommon {

    private static Set<String> disabledDtoClasses;
    private static final Map<String, Object> locks = new ConcurrentHashMap<>(500, 1);


    public static void afterDataChange(Dto dto, Settings s) throws VantarException {
        Class<? extends Dto> dtoClass = dto.getClass();
        updateDtoCache(dtoClass);
        if (s.eventAfterWrite != null) {
            s.eventAfterWrite.write(dto);
        }
        if (s.logEvent && s.action != null && Services.isUp(ServiceLog.class)
            && !dtoClass.equals(Log.class) && !dtoClass.equals(UserWebLog.class) && !dtoClass.equals(UserLog.class)) {

            ServiceLog.addAction(s.action, dto);
        }
    }

    public static void updateDtoCache(Dto dto) {
        updateDtoCache(dto.getClass());
    }

    public static void updateDtoCache(Class<? extends Dto> dtoClass) {
        if (dtoClass.isAnnotationPresent(Cache.class)) {
            String dtoName = dtoClass.getSimpleName();
            if (disabledDtoClasses != null && disabledDtoClasses.contains(dtoName)) {
                return;
            }
            ServiceDtoCache service;
            try {
                service = Services.getService(ServiceDtoCache.class);
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

    public static void enableDtoCache(Class<? extends Dto> dtoClass) {
        if (disabledDtoClasses != null) {
            disabledDtoClasses.remove(dtoClass.getSimpleName());
        }
    }

    public static void enableDtoCache(Dto dto) {
        Class<? extends Dto> dtoClass = dto.getClass();
        String dtoName = dtoClass.getSimpleName();
        if (disabledDtoClasses != null) {
            disabledDtoClasses.remove(dtoName);
        }
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
                userPassword.autoIncrementOnInsert(false);
                userPassword.setId(user.getId());
                userPassword.setPassword(password);
                try {
                    if (MongoQuery.existsById(userPassword)) {
                        ModelMongo.update(new Settings(userPassword).logEvent(false).mutex(false));
                    } else {
                        ModelMongo.insert(new Settings(userPassword).logEvent(false).mutex(false));
                    }
                } catch (VantarException e) {
                    throw new ServerException(VantarKey.FAIL_FETCH);
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

    /**
     * Create a mutex on a key
     * @param key mutex value
     * @param m callback
     * @return optional value returned in the callback
     * @throws VantarException call back exceptions
     */
    public static Object mutex(String key, Mutex m) throws VantarException {
        MutexParams mp = new MutexParams();
        locks.computeIfAbsent(key, k -> {
            try {
                mp.returnValue = m.block();
            } catch (Exception e) {
                mp.exception = e;
            }
            return null;
        });
        if (mp.exception != null) {
            if (mp.exception instanceof VantarException) {
                throw (VantarException) mp.exception;
            } else {
                throw new VantarException(mp.exception);
            }
        }
        return mp.returnValue;
    }

    /**
     * Create a mutex on a (dtoClass+id)
     * @param dto dto object with id set
     * @param m callback
     * @return optional value returned in the callback
     * @throws VantarException call back exceptions
     */
    public static Object mutex(Dto dto, MutexDto m) throws VantarException {
        MutexParams mp = new MutexParams();
        locks.computeIfAbsent(dto.getClass().getSimpleName() + dto.getId(), k -> {
            try {
                mp.returnValue = m.block(dto);
            } catch (Exception e) {
                mp.exception = e;
            }
            return null;
        });
        if (mp.exception != null) {
            if (mp.exception instanceof VantarException) {
                throw (VantarException) mp.exception;
            } else {
                throw new VantarException(mp.exception);
            }
        }
        return mp.returnValue;
    }

    /**
     * Create a mutex on a (dtoClass+id)
     * @param dtoClass dto class
     * @param id value
     * @param m callback
     * @return optional value returned in the callback
     * @throws VantarException call back exceptions
     */
    public static Object mutex(Class<? extends Dto> dtoClass, Long id, Mutex m) throws VantarException {
        MutexParams mp = new MutexParams();
        locks.computeIfAbsent(dtoClass.getSimpleName() + id, k -> {
            try {
                mp.returnValue = m.block();
            } catch (Exception e) {
                mp.exception = e;
            }
            return null;
        });
        if (mp.exception != null) {
            if (mp.exception instanceof VantarException) {
                throw (VantarException) mp.exception;
            } else {
                throw new VantarException(mp.exception);
            }
        }
        return mp.returnValue;
    }

    /**
     * Create a mutex on a (dtoClass)
     * @param dtoClass dto class
     * @param m callback
     * @return optional value returned in the callback
     * @throws VantarException call back exceptions
     */
    public static Object mutex(Class<? extends Dto> dtoClass, Mutex m) throws VantarException {
        MutexParams mp = new MutexParams();
        locks.computeIfAbsent("_" + dtoClass.getSimpleName(), k -> {
            try {
                mp.returnValue = m.block();
            } catch (Exception e) {
                mp.exception = e;
            }
            return null;
        });
        if (mp.exception != null) {
            if (mp.exception instanceof VantarException) {
                throw (VantarException) mp.exception;
            } else {
                throw new VantarException(mp.exception);
            }
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

        void write(Dto dto) throws VantarException;
    }


    public interface ReadEvent {

        void read(Dto dto) throws VantarException;
    }


    public interface QueryEvent extends QueryResultBase.Event {

        void beforeQuery(QueryBuilder q) throws VantarException;
    }


    public interface QueryEventForeach extends QueryResultBase.EventForeach {

        void beforeQuery(QueryBuilder q) throws VantarException;
    }


    public static class Settings {

        // insert update delete
        public Params params;
        // insert update
        public String key;
        // insert update delete
        public Dto dto;
        // update delete
        public QueryBuilder q;
        // insert update --> after set before validation
        public WriteEvent eventBeforeValidation;
        // insert update delete --> before db write
        public WriteEvent eventBeforeWrite;
        // insert update delete --> after db write
        public WriteEvent eventAfterWrite;
        // update delete --> after getting full data / before validation
        public ReadEvent eventAfterRead;
        // update delete
        public boolean dtoHasFullData;
        // insert update delete
        public boolean logEvent = true;
        // insert update delete
        public boolean mutex = true;
        // insert update
        public boolean isJson;
        // delete
        public boolean cascade;
        // delete
        public boolean force;
        // update
        public Dto.Action action = Dto.Action.UPDATE_FEW_COLS;
        // delete
        private long count;

        public Settings(Dto dto) {
            this.dto = dto;
        }

        public Settings(Params params, Dto dto) {
            this.params = params;
            this.dto = dto;
        }

        public Settings(QueryBuilder q) {
            this.q = q;
        }

        public Settings dtoHasFullData(boolean s) {
            dtoHasFullData = s;
            return this;
        }
        public Settings logEvent(boolean s) {
            logEvent = s;
            return this;
        }
        public Settings mutex(boolean s) {
            mutex = s;
            return this;
        }
        public Settings cascade(boolean s) {
            cascade = s;
            return this;
        }
        public Settings force(boolean s) {
            force = s;
            return this;
        }
        public Settings isJson() {
            isJson = true;
            return this;
        }
        public Settings isJson(String key) {
            isJson = true;
            this.key = key;
            return this;
        }
        public Settings setEventBeforeValidation(WriteEvent event) {
            this.eventBeforeValidation = event;
            return this;
        }
        public Settings setEventBeforeWrite(WriteEvent event) {
            this.eventBeforeWrite = event;
            return this;
        }
        public Settings setEventAfterWrite(WriteEvent event) {
            this.eventAfterWrite = event;
            return this;
        }
        public Settings setEventAfterRead(ReadEvent event) {
            this.eventAfterRead = event;
            return this;
        }

        public void addDeletedCount(long c) {
            count += c;
        }

        public long getDeletedCount() {
            return count;
        }
    }

}
