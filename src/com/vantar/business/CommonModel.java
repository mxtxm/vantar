package com.vantar.business;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.MongoSearch;
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


public abstract class CommonModel {

    public static boolean SEARCH_POLICY_ALLOW_EMPTY_CONDITION = true;
    public static boolean SEARCH_POLICY_THROW_ON_CONDITION_ERROR = true;

    protected static final Logger log = LoggerFactory.getLogger(CommonModel.class);


    public static void afterDataChange(Dto dto) {
        if (dto.hasAnnotation(Cache.class)) {
            ServiceDtoCache service;
            try {
                service = Services.get(ServiceDtoCache.class);
            } catch (ServiceException e) {
                return;
            }
            String dtoName = dto.getClass().getSimpleName();
            service.update(dtoName);
            Services.messaging.broadcast(VantarParam.MESSAGE_DATABASE_UPDATED, dtoName);
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
                userPassword.setClearIdOnInsert(false);
                userPassword.setId(user.getId());
                userPassword.setPassword(password);
                try {
                    if (MongoSearch.existsById(userPassword)) {
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


    public interface WriteEvent {

        void beforeSet(Dto dto) throws VantarException;
        void beforeWrite(Dto dto) throws VantarException;
        void afterWrite(Dto dto) throws VantarException;
    }


    public interface QueryEvent extends QueryResultBase.Event {

        void beforeQuery(QueryBuilder q) throws VantarException;

    }


    public interface BatchEvent {

        boolean beforeInsert(Dto dto);
        boolean beforeUpdate(Dto dto);
        boolean beforeDelete(Dto dto);
    }
}
