package com.vantar.business;

import com.vantar.business.importexport.CommonImport;
import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.Dto;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.web.*;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class CommonModelSql extends CommonModel {

    private static final Logger log = LoggerFactory.getLogger(CommonModelSql.class);


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
        List<ValidationError> errors;
        if (params instanceof String) {
            errors = dto.set((String) params, Dto.Action.INSERT);
        } else {
            errors = dto.set((Params) params, Dto.Action.INSERT);
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

        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repo = new CommonRepoSql(connection);

            errors = repo.getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }

            long id = repo.insert(dto);
            if (event != null) {
                event.afterWrite(dto);
            }
            return ResponseMessage.success(VantarKey.INSERT_SUCCESS, id);
        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.INSERT_FAIL);
        }
    }

    public static <T extends Dto> ResponseMessage insertBatch(Params params, Class<T> tClass) throws VantarException {
        return insertBatch(params, tClass, null);
    }

    public static <T extends Dto> ResponseMessage insertBatch(Params params, Class<T> tClass, WriteEvent event)
        throws VantarException {

        List<T> dtos = params.getJsonList(tClass);
        if (dtos == null || dtos.isEmpty()) {
            throw new InputException(VantarKey.INVALID_JSON_DATA);
        }

        try (SqlConnection connection = new SqlConnection()) {
            connection.startTransaction();
            CommonRepoSql repo = new CommonRepoSql(connection);

            for (Dto dto : dtos) {
                if (event != null) {
                    event.beforeWrite(dto);
                }

                List<ValidationError> errors = dto.validate(Dto.Action.INSERT);
                if (!errors.isEmpty()) {
                    throw new InputException(errors);
                }

                errors = repo.getUniqueViolation(dto);
                if (errors != null) {
                    throw new InputException(errors);
                }
            }

            repo.insert(dtos);
            connection.commit();

        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.INSERT_FAIL);
        }

        if (event != null) {
            for (Dto dto : dtos) {
                event.afterWrite(dto);
            }
        }

        return ResponseMessage.success(VantarKey.INSERT_SUCCESS, dtos.size());
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
        List<ValidationError> errors;
        if (params instanceof String) {
            errors = dto.set((String) params, Dto.Action.UPDATE_ALL_COLS);
        } else {
            errors = dto.set((Params) params, Dto.Action.UPDATE_ALL_COLS);
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

        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repo = new CommonRepoSql(connection);

            errors = repo.getUniqueViolation(dto);
            if (errors != null) {
                throw new InputException(errors);
            }

            repo.update(dto);

        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.UPDATE_FAIL);
        }

        if (event != null) {
            event.afterWrite(dto);
        }
        return ResponseMessage.success(VantarKey.UPDATE_SUCCESS);
    }

    public static <T extends Dto> ResponseMessage updateBatch(Params params, Class<T> tClass) throws VantarException {
        return updateBatch(params, tClass, null);
    }

    public static <T extends Dto> ResponseMessage updateBatch(Params params, Class<T> tClass, WriteEvent event)
        throws VantarException {

        List<T> dtos = params.getJsonList(tClass);
        if (dtos == null || dtos.isEmpty()) {
            throw new InputException(VantarKey.INVALID_JSON_DATA);
        }

        try (SqlConnection connection = new SqlConnection()) {
            connection.startTransaction();
            SqlExecute execute = new SqlExecute(connection);

            for (Dto dto : dtos) {
                if (event != null) {
                    event.beforeWrite(dto);
                }

                List<ValidationError> errors = dto.validate(Dto.Action.UPDATE_ALL_COLS);
                if (!errors.isEmpty()) {
                    throw new InputException(errors);
                }

                execute.update(dto);

                if (event != null) {
                    event.afterWrite(dto);
                }
            }

            connection.commit();

        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.UPDATE_FAIL);
        }

        if (!dtos.isEmpty()) {
            afterDataChange(dtos.get(0));
        }

        return ResponseMessage.success(VantarKey.UPDATE_SUCCESS, dtos.size());
    }

    public static ResponseMessage delete(Params params, Dto dto) throws VantarException {
        List<ValidationError> errors = dto.set(params, Dto.Action.DELETE);
        if (!errors.isEmpty()) {
            throw new InputException(errors);
        }

        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repo = new CommonRepoSql(connection);
            repo.delete(dto);

            return ResponseMessage.success(VantarKey.DELETE_SUCCESS);
        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
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

        try (SqlConnection connection = new SqlConnection()) {
            connection.startTransaction();
            SqlExecute repo = new SqlExecute(connection);

            for (Long id : ids) {
                dto.setId(id);
                if (event != null) {
                    event.beforeWrite(dto);
                }

                repo.delete(dto);
            }

            connection.commit();

        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.DELETE_FAIL);
        }

        if (!ids.isEmpty()) {
            afterDataChange(dto);
        }

        return ResponseMessage.success(VantarKey.DELETE_SUCCESS, ids.size());
    }

    public static ResponseMessage batch(Params params, Dto dto) throws VantarException {
        return batch(params, dto, null);
    }

    public static ResponseMessage batch(Params params, Dto dto, BatchEvent event) throws VantarException {
        Batch update = params.getJson(Batch.class);
        try (SqlConnection connection = new SqlConnection()) {
            connection.startTransaction();

            CommonRepoSql repo = new CommonRepoSql(connection);
            SqlExecute sqlExecute = new SqlExecute(connection);
            String msg =
                deleteMany(repo, update.delete, dto, event) + "\n\n" +
                insertMany(repo, sqlExecute, update.insert, dto, event) + "\n\n" +
                updateMany(repo, sqlExecute, update.update, dto, event) + "\n\n";

            connection.commit();
            afterDataChange(dto);
            return ResponseMessage.success(msg);

        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.UPDATE_FAIL);
        }
    }

    private static String insertMany(
        CommonRepoSql repo, SqlExecute execute, List<Map<String, Object>> records, Dto dto, BatchEvent event)
        throws VantarException {

        for (Map<String, Object> record : records) {
            if (event != null) {
                event.beforeInsert(dto);
            }

            List<ValidationError> errors = dto.set(record, Dto.Action.INSERT);
            if (!errors.isEmpty()) {
                throw new InputException(errors);
            }

            try {
                errors = repo.getUniqueViolation(dto);
                if (errors != null) {
                    throw new InputException(errors);
                }

                execute.insert(dto);
            } catch (DatabaseException e) {
                log.error("! {}", e.getMessage());
                throw new ServerException(VantarKey.INSERT_FAIL);
            }
        }

        return Locale.getString(VantarKey.INSERT_MANY_SUCCESS, records.size());
    }

    private static String updateMany(
        CommonRepoSql repo, SqlExecute execute, List<Map<String, Object>> records, Dto dto, BatchEvent event)
        throws VantarException {

        for (Map<String, Object> record : records) {
            if (event != null && event.beforeUpdate(dto)) {
                throw new ServerException(VantarKey.UPDATE_FAIL);
            }

            List<ValidationError> errors = dto.set(record, Dto.Action.UPDATE_ALL_COLS);
            if (!errors.isEmpty()) {
                throw new InputException(errors);
            }

            try {
                errors = repo.getUniqueViolation(dto);
                if (errors != null) {
                    throw new InputException(errors);
                }

                execute.update(dto);
            } catch (DatabaseException e) {
                log.error("! {}", e.getMessage());
                throw new ServerException(VantarKey.UPDATE_FAIL);
            }
        }

        return Locale.getString(VantarKey.UPDATE_MANY_SUCCESS, records.size());
    }

    private static String deleteMany(SqlExecute execute, List<Long> ids, Dto dto, BatchEvent event) throws VantarException {
        for (Long id : ids) {
            dto.setId(id);

            if (event != null && event.beforeDelete(dto)) {
                throw new ServerException(VantarKey.DELETE_FAIL);
            }

            if (NumberUtil.isIdInvalid(id)) {
                throw new InputException(new ValidationError(VantarParam.ID, VantarKey.INVALID_ID));
            }

            try {
                execute.delete(dto);
            } catch (DatabaseException e) {
                log.error("! {}", e.getMessage());
                throw new ServerException(VantarKey.DELETE_FAIL);
            }
        }

        return Locale.getString(VantarKey.DELETE_MANY_SUCCESS, ids.size());
    }

    public static ResponseMessage purge(String table) throws VantarException {
        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repo = new CommonRepoSql(connection);
            repo.purge(table);
            return ResponseMessage.success(VantarKey.DELETE_SUCCESS);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.DELETE_FAIL);
        }
    }

    public static ResponseMessage purgeData(String table) throws VantarException {
        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repo = new CommonRepoSql(connection);
            repo.purgeData(table);
            return ResponseMessage.success(VantarKey.DELETE_SUCCESS);
        } catch (DatabaseException e) {
            log.error("! {}", e.getMessage());
            throw new ServerException(VantarKey.DELETE_FAIL);
        }
    }

    // > > > admin tools

    public static void importDataAdmin(String data, Dto dto, List<String> presentField, boolean deleteAll, WebUi ui) {
        ui.beginBox2(dto.getClass().getSimpleName()).write();

        try (SqlConnection connection = new SqlConnection()) {
            connection.startTransaction();
            CommonRepoSql repo = new CommonRepoSql(connection);
            SqlExecute sqlExecute = new SqlExecute(connection);

            if (deleteAll) {
                repo.purgeData(dto.getStorage());
                ui.addBlock("pre", Locale.getString(VantarKey.DELETE_SUCCESS)).write();
            }

            AtomicInteger failed = new AtomicInteger();
            AtomicInteger success = new AtomicInteger();
            AtomicInteger duplicate = new AtomicInteger();

            CommonImport.Import imp = (String presentValue, Map<String, Object> values) -> {
                try {
                    if (dto.getId() == null ? repo.existsByDto(dto) : repo.existsById(dto)) {
                        duplicate.getAndIncrement();
                        return;
                    }

                    //sqlExecute.insert(dto);
                    success.getAndIncrement();
                } catch (DatabaseException e) {
                    ui.addErrorMessage(presentValue + " " + Locale.getString(VantarKey.IMPORT_FAIL));
                    failed.getAndIncrement();
                }
            };

            //importDataX(imp, data, dto, presentField, ui);
            connection.commit();

            ui.addBlock("pre",
                Locale.getString(VantarKey.BUSINESS_WRITTEN_COUNT, success) + "\n" +
                Locale.getString(VantarKey.BUSINESS_ERROR_COUNT, failed) + "\n" +
                Locale.getString(VantarKey.BUSINESS_DUPLICATE_COUNT, duplicate)
            );

        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
            log.error("! batch import failed", e);
        }

        ui.blockEnd().blockEnd().write();
    }
}
