package com.vantar.business;

import com.vantar.business.importexport.ImportCommon;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.Dto;
import com.vantar.database.nosql.elasticsearch.*;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.util.object.*;
import com.vantar.web.*;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class CommonModelElastic extends ModelCommon {

    private static final Logger log = LoggerFactory.getLogger(CommonModelElastic.class);


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

    public static ResponseMessage insertJson(String json, Dto dto) throws VantarException {
        return insertX(json, dto, null);
    }

    public static ResponseMessage insertJson(String json, Dto dto, WriteEvent event) throws VantarException {
        return insertX(json, dto, event);
    }

    private static ResponseMessage insertX(Object params, Dto dto, WriteEvent event) throws VantarException {
        List<ValidationError> errors = new ArrayList<>();
        if (params instanceof String) {
             dto.set((String) params, Dto.Action.INSERT);
        } else {
             dto.set((Params) params, Dto.Action.INSERT);
        }

        if (event != null) {
//            try {
//                event.beforeWrite(dto);
//            } catch (InputException e) {
//                errors.addAll(e.getErrors());
//            }
        }

        if (!errors.isEmpty()) {
            throw new InputException(errors);
        }

        try {
            errors = CommonRepoElastic.getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }

            long id = ElasticWrite.insertOne(dto);
//            if (event != null) {
//                event.afterWrite(dto);
//            }
            return ResponseMessage.success(VantarKey.SUCCESS_INSERT, id);
        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.FAIL_INSERT);
        }
    }

    public static <T extends Dto> ResponseMessage insertBatch(Params params, Class<T> tClass) throws VantarException {
        return insertBatch(params, tClass, null);
    }

    public static <T extends Dto> ResponseMessage insertBatch(Params params, Class<T> tClass, WriteEvent event) throws VantarException {
        List<T> dtos = params.getJsonList(tClass);
        if (dtos == null || dtos.isEmpty()) {
            throw new InputException(VantarKey.INVALID_JSON_DATA);
        }

        try (ElasticWrite write = new ElasticWrite()) {
            for (Dto dto : dtos) {
//                if (event != null) {
//                    event.beforeWrite(dto);
//                }

                List<ValidationError> errors = dto.validate(Dto.Action.INSERT);
                if (!errors.isEmpty()) {
                    throw new InputException(errors);
                }

                errors = CommonRepoElastic.getUniqueViolation(dto);
                if (errors != null) {
                    throw new InputException(errors);
                }

                write.insert(dto);
            }

            write.commit();
        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.FAIL_INSERT);
        }

//        if (!dtos.isEmpty()) {
//            afterDataChange(dtos.get(0));
//        }

//        if (event != null) {
//            for (Dto dto : dtos) {
//                event.afterWrite(dto);
//            }
//        }

        return ResponseMessage.success(VantarKey.SUCCESS_INSERT, dtos.size());
    }

    public static ResponseMessage update(Params params, Dto dto) throws VantarException {
        return updateX(params, dto, null);
    }

    public static ResponseMessage update(Params params, Dto dto, WriteEvent event) throws VantarException {
        return updateX(params, dto, event);
    }

    public static ResponseMessage updateJson(Params params, Dto dto) throws VantarException {
        return updateX(params.getJson(), dto, null);
    }

    public static ResponseMessage updateJson(Params params, Dto dto, WriteEvent event) throws VantarException {
        return updateX(params.getJson(), dto, event);
    }

    public static ResponseMessage updateJson(String json, Dto dto) throws VantarException {
        return updateX(json, dto, null);
    }

    public static ResponseMessage updateJson(String json, Dto dto, WriteEvent event) throws VantarException {
        return updateX(json, dto, event);
    }

    private static ResponseMessage updateX(Object params, Dto dto, WriteEvent event) throws VantarException {
        List<ValidationError> errors = new ArrayList<>();
        if (params instanceof String) {
             dto.set((String) params, Dto.Action.UPDATE_ALL_COLS);
        } else {
             dto.set((Params) params, Dto.Action.UPDATE_ALL_COLS);
        }

//        if (event != null) {
//            try {
//                event.beforeWrite(dto);
//            } catch (InputException e) {
//                errors.addAll(e.getErrors());
//            }
//        }
        if (!errors.isEmpty()) {
            throw new InputException(errors);
        }

        try {
            errors = CommonRepoElastic.getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }

            ElasticWrite.updateOne(dto);

        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.FAIL_UPDATE);
        }

//        if (event != null) {
//            event.afterWrite(dto);
//        }
        return ResponseMessage.success(VantarKey.SUCCESS_UPDATE);
    }

    public static <T extends Dto> ResponseMessage updateBatch(Params params, Class<T> tClass) throws VantarException {
        return updateBatch(params, tClass, null);
    }

    public static <T extends Dto> ResponseMessage updateBatch(Params params, Class<T> tClass, WriteEvent event) throws VantarException {
        List<T> dtos = params.getJsonList(tClass);
        if (dtos == null || dtos.isEmpty()) {
            throw new InputException(VantarKey.INVALID_JSON_DATA);
        }

        try (ElasticWrite write = new ElasticWrite()) {
            write.startTransaction();

            for (Dto dto : dtos) {
//                if (event != null) {
//                    event.beforeWrite(dto);
//                }

                List<ValidationError> errors = dto.validate(Dto.Action.UPDATE_ALL_COLS);
                if (!errors.isEmpty()) {
                    throw new InputException(errors);
                }

                write.update(dto);

//                if (event != null) {
//                    event.afterWrite(dto);
//                }
            }

            write.commit();

        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.FAIL_UPDATE);
        }

//        if (!dtos.isEmpty()) {
//            afterDataChange(dtos.get(0));
//        }

        return ResponseMessage.success(VantarKey.SUCCESS_UPDATE, dtos.size());
    }

    public static ResponseMessage delete(Params params, Dto dto) throws VantarException {
//        List<ValidationError> errors = dto.set(params, Dto.Action.DELETE);
//        if (!errors.isEmpty()) {
//            throw new InputException(errors);
//        }

        try {
            ElasticWrite.deleteOne(dto);
        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.FAIL_DELETE);
        }

        return ResponseMessage.success(VantarKey.SUCCESS_DELETE);
    }

    public static <T extends Dto> ResponseMessage deleteBatch(Params params, Class<T> tClass) throws VantarException {
        return deleteBatch(params, tClass, null);
    }

    public static <T extends Dto> ResponseMessage deleteBatch(Params params, Class<T> tClass, WriteEvent event) throws VantarException {
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

        try (ElasticWrite write = new ElasticWrite()) {
            write.startTransaction();

            for (Long id : ids) {
                dto.setId(id);
//                if (event != null) {
//                    event.beforeWrite(dto);
//                }

                write.delete(dto);
            }

            write.commit();

        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.FAIL_DELETE);
        }

//        if (!ids.isEmpty()) {
//            afterDataChange(dto);
//        }

        return ResponseMessage.success(VantarKey.SUCCESS_DELETE, ids.size());
    }

//    public static ResponseMessage batch(Params params, Dto dto) throws VantarException {
//        return batch(params, dto, null);
//    }
//
//    public static ResponseMessage batch(Params params, Dto dto, BatchEvent event) throws VantarException {
//        Batch update = params.getJson(Batch.class);
//        ElasticWrite write = new ElasticWrite();
//        write.startTransaction();
//        try {
//            String msg =
//                deleteMany(write, update.delete, dto, event) + "\n\n" +
//                insertMany(write, update.insert, dto.getClass(), event) + "\n\n" +
//                updateMany(write, update.update, dto, event) + "\n\n";
//
//            write.commit();
//            afterDataChange(dto);
//            return ResponseMessage.success(msg);
//
//        } catch (DatabaseException e) {
//            log.error("! {}", e.getMessage());
//            throw new ServerException(VantarKey.FAIL_UPDATE);
//        }
//    }

//    private static String insertMany(ElasticWrite write, List<Map<String, Object>> records, Class<? extends Dto> tClass,
//        BatchEvent event) throws VantarException {
//
//        for (Map<String, Object> record : records) {
//            Dto dto = ClassUtil.getInstance(tClass);
//            if (dto == null) {
//                throw new ServerException(VantarKey.CAN_NOT_CREATE_DTO);
//            }
//
//            if (event != null && event.beforeInsert(dto)) {
//                throw new ServerException(VantarKey.FAIL_INSERT);
//            }
//
//            List<ValidationError> errors = dto.set(record, Dto.Action.INSERT);
//            if (!errors.isEmpty()) {
//                throw new InputException(errors);
//            }
//
//            try {
//                errors = CommonRepoElastic.getUniqueViolation(dto);
//                if (errors != null) {
//                    throw new InputException(errors);
//                }
//
//                write.insert(dto);
//            } catch (DatabaseException e) {
//                log.error("! {}", e.getMessage());
//                throw new ServerException(VantarKey.FAIL_INSERT);
//            }
//        }
//
//        return Locale.getString(VantarKey.SUCCESS_INSERT, records.size());
//    }
//
//    private static String updateMany(ElasticWrite write, List<Map<String, Object>> records, Dto dto, BatchEvent event)
//        throws VantarException {
//
//        for (Map<String, Object> record : records) {
//            if (event != null && event.beforeUpdate(dto)) {
//                throw new ServerException(VantarKey.FAIL_UPDATE);
//            }
//
//            List<ValidationError> errors = dto.set(record, Dto.Action.UPDATE_ALL_COLS);
//            if (!errors.isEmpty()) {
//                throw new InputException(errors);
//            }
//
//            try {
//                errors = CommonRepoElastic.getUniqueViolation(dto);
//                if (errors != null) {
//                    throw new InputException(errors);
//                }
//
//                write.update(dto);
//            } catch (DatabaseException e) {
//                log.error("! {}", e.getMessage());
//                throw new ServerException(VantarKey.FAIL_UPDATE);
//            }
//        }
//
//        return Locale.getString(VantarKey.SUCCESS_UPDATE, records.size());
//    }
//
//    private static String deleteMany(ElasticWrite write, List<Long> ids, Dto dto, BatchEvent event) throws VantarException {
//        for (Long id : ids) {
//            dto.setId(id);
//            if (event != null && event.beforeDelete(dto)) {
//                throw new ServerException(VantarKey.FAIL_DELETE);
//            }
//
//            if (NumberUtil.isIdInvalid(id)) {
//                throw new InputException(new ValidationError(VantarParam.ID, VantarKey.INVALID_ID));
//            }
//
//            try {
//                write.delete(dto);
//            } catch (Exception e) {
//                log.error("! {}", e.getMessage());
//                throw new ServerException(VantarKey.FAIL_DELETE);
//            }
//        }
//
//        return Locale.getString(VantarKey.SUCCESS_DELETE, ids.size());
//    }

    public static ResponseMessage purge(String collection) throws ServerException {
        try {
            CommonRepoElastic.purge(collection);
            return ResponseMessage.success(VantarKey.SUCCESS_DELETE);
        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.FAIL_DELETE);
        }
    }

    public static ResponseMessage purgeData(String collection) throws ServerException {
        try {
            CommonRepoElastic.purgeData(collection);
            return ResponseMessage.success(VantarKey.SUCCESS_DELETE);
        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.FAIL_DELETE);
        }
    }

    // > > > admin tools

    public static void importDataAdmin(WebUi ui, String data, Dto dto, List<String> presentField, boolean deleteAll) {
        ui.addHeading(3, dto.getClass().getSimpleName()).write();

        if (deleteAll) {
            try {
                ElasticWrite.purgeData(dto.getStorage());
            } catch (DatabaseException e) {
                if (ui != null) {
                    ui.addErrorMessage(e);
                }
                return;
            }
            if (ui != null) {
                ui.addMessage(VantarKey.SUCCESS_DELETE).write();
            }
        }

        ElasticWrite write = new ElasticWrite();
        write.startTransaction();

        AtomicInteger failed = new AtomicInteger();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();

        ImportCommon.Import imp = (String presentValue, Map<String, Object> values) -> {
            try {
                if (dto.getId() == null && ElasticSearch.existsById(dto)) {
                    duplicate.getAndIncrement();
                    return;
                }

                write.insert(dto);
                success.getAndIncrement();
            } catch (DatabaseException e) {
                ui.addErrorMessage(presentValue + " " + Locale.getString(VantarKey.FAIL_IMPORT));
                failed.getAndIncrement();
            }
        };

        //importDataX(imp, data, dto, presentField, ui);

        try {
            write.commit();

            if (ui != null) {
                ui  .addKeyValue(VantarKey.SUCCESS_COUNT, success)
                    .addKeyValue(VantarKey.FAIL_COUNT, failed)
                    .addKeyValue(VantarKey.DUPLICATE_COUNT, duplicate)
                    .write();
            }

        } catch (DatabaseException e) {
            if (ui != null) {
                ui.addErrorMessage(e);
            }
        }
    }
}
