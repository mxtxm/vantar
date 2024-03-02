package com.vantar.admin.model.database;

import com.vantar.admin.model.index.Admin;
import com.vantar.business.*;
import com.vantar.business.importexport.ImportMongo;
import com.vantar.common.*;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dependency.DataDependency;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.*;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.query.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.CommonUser;
import com.vantar.service.dbarchive.ServiceDbArchive;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.file.FileUtil;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.*;


public class AdminData {

    public static final int N_PER_PAGE = 100;


    public static void fields(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(VantarKey.ADMIN_DATA_FIELDS, params, response, dtoInfo);

        ui.addHeading(2, dtoInfo.dtoClass.getName() + " (" + dtoInfo.title + ")");

        Dto dto = dtoInfo.getDtoInstance();

        for (Field field : dto.getFields()) {
            StringBuilder t = new StringBuilder(1000);

            Class<?> type = field.getType();
            t.append(type.getSimpleName());

            if (type.equals(List.class) || type.equals(ArrayList.class) || type.equals(Set.class)) {
                t.append("<").append(dto.getPropertyGenericTypes(field.getName())[0].getSimpleName()).append(">");

            } else if (type.equals(Map.class)) {
                Class<?>[] genericTypes = dto.getPropertyGenericTypes(field.getName());
                t.append("<").append(genericTypes[0].getSimpleName()).append(", ")
                    .append(genericTypes[1].getSimpleName()).append(">");

            } else if (type.isEnum()) {
                t.append(" (enum)");
            }

            ui.addKeyValue(field.getName(), t.toString());
        }

        ui.finish();
    }

    public static void list(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(VantarKey.ADMIN_DATA_LIST, params, response, dtoInfo);
        Dto dto = dtoInfo.getDtoInstance();

        Event event = getEvent();
        if (event != null) {
            dto = event.dtoExchange(dto, "list");
        }

        QueryBuilder q = params.getQueryBuilder("jsonsearch", dto);
        if (q == null) {
            q = new QueryBuilder(dto)
                .page(params.getInteger("page", 1), params.getInteger("count", AdminData.N_PER_PAGE))
                .sort(params.getString("sort", VantarParam.ID) + ":" + params.getString("sortpos", "desc"));
        } else {
            q.setDto(dto);
            List<ValidationError> errors = q.getErrors();
            if (errors != null) {
                ui.write().addErrorMessage(ValidationError.toString(errors)).finish();
                return;
            }
        }

//        if (dto.isDeleteLogicalEnabled()) {
//            String deletePolicy = params.getString("delete-policy", "n");
//            dto.setDeletedQueryPolicy(
//                deletePolicy.equals("d") ? Dto.QueryDeleted.SHOW_DELETED :
//                    (deletePolicy.equals("a") ? Dto.QueryDeleted.SHOW_ALL :  Dto.QueryDeleted.SHOW_NOT_DELETED)
//            );
//        }

        PageData data = null;
        try {
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                if (SqlConnection.isUp()) {
                    try (SqlConnection connection = new SqlConnection()) {
                        SqlSearch search = new SqlSearch(connection);
                        data = search.getPage(q);
                    }
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                if (MongoConnection.isUp()) {
                    data = MongoQuery.getPage(q, null);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                if (ElasticConnection.isUp()) {
                    data = ElasticSearch.getPage(q);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                }
            }
        } catch (NoContentException ignore) {

        } catch (DatabaseException e) {
            ui.addErrorMessage(ObjectUtil.throwableToString(e));
            ServiceLog.log.error("! {}", dto, e);
        }

        ui.addDtoListWithHeader(data, dtoInfo, q.getDto().getProperties());

        ui.finish();
    }

//    @SuppressWarnings("unchecked")
//    public static void delete(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
//        if (dtoInfo == null) {
//            return;
//        }
//        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_DELETE), params, response, dtoInfo);
//
//        Dto dto = dtoInfo.getDtoInstance();
//        //dto.setDeletedQueryPolicy(Dto.QueryDeleted.SHOW_ALL);
//        Event event = getEvent();
//        if (event != null) {
//            dto = event.dtoExchange(dto, "delete");
//        }
//
//        if (!params.isChecked("f")) {
//            //dto.setDeleteLogical(false);
//            QueryBuilder q = new QueryBuilder(dto);
//            q.condition().inNumber(VantarParam.ID, params.getLongList(VantarParam.ID));
//
//            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
//                if (SqlConnection.isUp()) {
//                    try (SqlConnection connection = new SqlConnection()) {
//                        CommonRepoSql repo = new CommonRepoSql(connection);
//                        ui.addDeleteForm(repo.getData(q));
//                    } catch (NoContentException x) {
//                        ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
//                    } catch (DatabaseException e) {
//                        ui.addErrorMessage(e);
//                        Admin.log.error("! {}", dto, e);
//                    }
//                } else {
//                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
//                }
//            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
//                if (MongoConnection.isUp()) {
//                    try {
//                        ui.addDeleteForm(ModelMongo.getData(q));
//                    } catch (NoContentException x) {
//                        ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
//                    } catch (VantarException e) {
//                        ui.addErrorMessage(e);
//                        Admin.log.error("! {}", dto, e);
//                    }
//                } else {
//                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
//                }
//            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
//                if (ElasticConnection.isUp()) {
//                    try {
//                        ui.addDeleteForm(CommonRepoElastic.getData(q));
//                    } catch (NoContentException x) {
//                        ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
//                    } catch (DatabaseException e) {
//                        ui.addErrorMessage(e);
//                        Admin.log.error("! {}", dto, e);
//                    }
//                } else {
//                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
//                }
//            }
//
//            ui.finish();
//            return;
//        }
//
//        if (event != null) {
//            event.beforeDelete(dto);
//        }
//
//        try {
//            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
//                if (SqlConnection.isUp()) {
//                    ui.addMessage(CommonModelSql.deleteBatch(params, dto.getClass()).message);
//                } else {
//                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
//                }
//            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
//                if (MongoConnection.isUp()) {
////                    if (params.isChecked(VantarParam.LOGICAL_DELETED_UNDO)) {
////                        ui.addMessage(ModelMongo.unDeleteBatch(params, dto.getClass()).message);
////                    } else {
//                        //dto.setDeleteLogical(params.isChecked("delete-logic"));
//
//                        ResponseMessage resp = ModelMongo.deleteBatch(params, dto.getClass());
//                        ui.addMessage(resp.message);
//                        if (resp.value instanceof List) {
//                            List<DataDependency.Dependants> items = (List<DataDependency.Dependants>) resp.value;
//                            for (DataDependency.Dependants item : items) {
//                                ui.addHeading(2, item.className);
//                                for (Map.Entry<Long, String> record : item.records.entrySet()) {
//                                    ui.addBlock("pre", record.getKey() + " - " + record.getValue());
//                                }
//                            }
//
//                        }
//       //             }
//                } else {
//                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
//                }
//            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
//                if (ElasticConnection.isUp()) {
//                    ui.addMessage(CommonModelElastic.deleteBatch(params, dto.getClass()).message);
//                } else {
//                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
//                }
//            }
//
//            if (event != null) {
//                event.afterDelete(dto);
//            }
//
//        } catch (VantarException e) {
//            ui.addErrorMessage(e);
//            Admin.log.error("! {}", dto, e);
//        }
//
//        if (dtoInfo.broadcastMessage != null) {
//            Services.messaging.broadcast(dtoInfo.broadcastMessage);
//        }
//
//        ui.finish();
//    }

    @SuppressWarnings("unchecked")
    public static void deleteMany(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_DELETE), params, response, dtoInfo);

        if (!params.isChecked("confirm-delete")) {
            ui.addMessage(Locale.getString(VantarKey.DELETE_FAIL)).finish();
            return;
        }

        Dto dto = dtoInfo.getDtoInstance();

        for (Long id : params.getLongList("delete-check")) {
            dto.reset();
            dto.setId(id);

            Event event = getEvent();
            if (event != null) {
                event.beforeDelete(dto);
            }

            try {
                if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                    if (MongoConnection.isUp()) {
//                        if (params.isChecked(VantarParam.LOGICAL_DELETED_UNDO)) {
//                            ui.addMessage(ModelMongo.unDeleteBatch(params, dto.getClass()).message);
//                        } else {
                            //dto.setDeleteLogical(params.isChecked("delete-logic"));
                            ResponseMessage resp = ModelMongo.deleteById(dto);
                            ui.addMessage(resp.message);
                            if (resp.value instanceof List) {
                                List<DataDependency.Dependants> items = (List<DataDependency.Dependants>) resp.value;
                                for (DataDependency.Dependants item : items) {
                                    ui.addHeading(2, item.className);
                                    for (Map.Entry<Long, String> record : item.records.entrySet()) {
                                        ui.addBlock("pre", record.getKey() + " - " + record.getValue());
                                    }
                                }
                            }
//                        }
                    } else {
                        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
                    }

                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                    if (SqlConnection.isUp()) {
                        ui.addMessage(CommonModelSql.deleteBatch(params, dto.getClass()).message);
                    } else {
                        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
                    }

                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                    if (ElasticConnection.isUp()) {
                        ui.addMessage(CommonModelElastic.deleteBatch(params, dto.getClass()).message);
                    } else {
                        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                    }
                }

                if (event != null) {
                    event.afterDelete(dto);
                }
            } catch (InputException e) {
                ui.addErrorMessage(e);
            } catch (VantarException e) {
                ui.addErrorMessage(e);
                ServiceLog.log.error("! {}", dto, e);
            }

            if (dtoInfo.broadcastMessage != null) {
                Services.messaging.broadcast(dtoInfo.broadcastMessage);
            }
        }

        ui.finish();
    }

    public static void purge(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_DELETE_ALL), params, response, dtoInfo);

        Dto dto = dtoInfo.getDtoInstance();

        if (!params.isChecked("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addErrorMessage(VantarKey.ADMIN_DELETE)
                .addCheckbox(VantarKey.ADMIN_DELETE_ALL_CONFIRM, WebUi.PARAM_CONFIRM)
                .addSubmit(VantarKey.ADMIN_DELETE)
                .blockEnd();
        } else {
            try {
                if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                    if (SqlConnection.isUp()) {
                        ui.addMessage(CommonModelSql.purgeData(dto.getStorage()).message);
                    } else {
                        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
                    }
                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                    if (MongoConnection.isUp()) {
                        ui.addMessage(ModelMongo.purge(dto).message);
                    } else {
                        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
                    }
                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                    if (ElasticConnection.isUp()) {
                        ui.addMessage(CommonModelElastic.purgeData(dto.getStorage()).message);
                    } else {
                        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                    }
                }
            } catch (VantarException e) {
                ui.addErrorMessage(e);
                ServiceLog.log.error("! {}", dto, e);
            }

            if (dtoInfo.broadcastMessage != null) {
                Services.messaging.broadcast(dtoInfo.broadcastMessage);
            }
        }

        ui.finish();
    }

    public static void update(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_UPDATE), params, response, dtoInfo);

        Dto dto = dtoInfo.getDtoInstance();
        Event event = getEvent();
        if (event != null) {
            dto = event.dtoExchange(dto, "update");
        }

        if (!params.contains("f")) {
            dto.setId(params.getLong(VantarParam.ID));
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                if (SqlConnection.isUp()) {
                    try (SqlConnection connection = new SqlConnection()) {
                        CommonRepoSql repo = new CommonRepoSql(connection);
                        ui.addDtoUpdateForm(
                            repo.getById(dto),
                            dto.getProperties()
                        );
                    } catch (DatabaseException e) {
                        ui.addErrorMessage(e);
                        ServiceLog.log.error("! {}", dto, e);
                    } catch (NoContentException e) {
                        ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
                    }
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                if (MongoConnection.isUp()) {
                    try {
                        ui.addDtoUpdateForm(
                            ModelMongo.getById(dto),
                            dto.getProperties()
                        );
                    } catch (NoContentException e) {
                        ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
                    } catch (VantarException e) {
                        ui.addErrorMessage(e);
                        ServiceLog.log.error("! {}", dto, e);
                    }
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                if (ElasticConnection.isUp()) {
                    try {
                        ui.addDtoUpdateForm(
                            CommonRepoElastic.getById(dto),
                            dto.getProperties()
                        );
                    } catch (DatabaseException e) {
                        ui.addErrorMessage(e);
                        ServiceLog.log.error("! {}", dto, e);
                    } catch (NoContentException e) {
                        ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
                    }
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                }
            }

            ui.finish();
            return;
        }

        if (event != null) {
            event.beforeUpdate(dto);
        }

        try {
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                if (SqlConnection.isUp()) {
                    CommonModelSql.updateJson(params.getString("asjson"), dto);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                if (MongoConnection.isUp()) {
                    ModelMongo.updateJson(params, "asjson", dto, new ModelCommon.WriteEvent() {
                        @Override
                        public void beforeSet(Dto dto) {

                        }

                        @Override
                        public void beforeWrite(Dto dto) {

                        }

                        @Override
                        public void afterWrite(Dto dto) throws ServerException {
                            if (dto instanceof CommonUser) {
                                ModelCommon.insertPassword(
                                    dto,
                                    Json.d.extract(params.getString("asjson"), "password", String.class)
                                );
                            }
                        }
                    });
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                if (ElasticConnection.isUp()) {
                    CommonModelElastic.updateJson(params.getString("asjson"), dto);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                }
            }

            if (event != null) {
                event.afterUpdate(dto);
            }

            ui.addMessage(Locale.getString(VantarKey.UPDATE_SUCCESS));

        } catch (VantarException e) {
            ui.addErrorMessage(e);
            ServiceLog.log.error("! {}", dto, e);
        }

        if (dtoInfo.broadcastMessage != null) {
            Services.messaging.broadcast(dtoInfo.broadcastMessage);
        }

        ui.finish();
    }

    public static void insert(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_NEW_RECORD), params, response, dtoInfo);

        Dto dto = dtoInfo.getDtoInstance();
        Event event = getEvent();
        if (event != null) {
            dto = event.dtoExchange(dto, "insert");
        }

        if (!params.isChecked("f")) {
            ui.addDtoAddForm(
                dto,
                dto.getProperties()
            );
            ui.finish();
            return;
        }

        if (event != null) {
            event.beforeInsert(dto);
        }

        try {
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                if (SqlConnection.isUp()) {
                    CommonModelSql.insert(params, dto);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                if (MongoConnection.isUp()) {
                    ModelMongo.insertJson(params, "asjson", dto, new ModelCommon.WriteEvent() {
                        @Override
                        public void beforeSet(Dto dto) {

                        }

                        @Override
                        public void beforeWrite(Dto dto) {

                        }

                        @Override
                        public void afterWrite(Dto dto) throws ServerException {
                            if (dto instanceof CommonUser) {
                                ModelCommon.insertPassword(
                                    dto,
                                    Json.d.extract(params.getString("asjson"), "password", String.class)
                                );
                            }
                        }
                    });
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                if (ElasticConnection.isUp()) {
                    CommonModelElastic.insert(params, dto);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                }
            }

            if (event != null) {
                event.afterInsert(dto);
            }

            ui.addMessage(Locale.getString(VantarKey.INSERT_SUCCESS));

        } catch (VantarException e) {
            ui.addErrorMessage(e);
            ServiceLog.log.error("! {}", dto, e);
        }

        if (dtoInfo.broadcastMessage != null) {
            Services.messaging.broadcast(dtoInfo.broadcastMessage);
        }

        ui.finish();
    }

    public static void exportData(Params params, HttpServletResponse response, DtoDictionary.Info dtoIndex)
        throws VantarException {

        Dto dto = dtoIndex.getDtoInstance();

        if (dtoIndex.dbms.equals(DtoDictionary.Dbms.SQL)) {
            if (SqlConnection.isUp()) {

            }
        } else if (dtoIndex.dbms.equals(DtoDictionary.Dbms.MONGO)) {
            if (MongoConnection.isUp()) {
                List<Dto> data = ModelMongo.getAll(dto);
                String json = Json.d.toJson(data);
                String filepath = FileUtil.getTempFilename();
                FileUtil.write(filepath, json);
                Response.download(
                    response,
                    filepath,
                    StringUtil.toKababCase(dto.getClass().getSimpleName()) + "-"
                        + new DateTime().formatter().getDateTimeAsFilename() + ".json"
                );
            }
        } else if (dtoIndex.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
            if (ElasticConnection.isUp()) {

            }
        }
    }


    public static void importData(Params params, HttpServletResponse response, DtoDictionary.Info dtoIndex)
        throws FinishException {

        if (dtoIndex == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_IMPORT), params, response, dtoIndex);

        Dto dto = dtoIndex.getDtoInstance();

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addTextArea(VantarKey.ADMIN_MENU_DATA, "import", dtoIndex.getImportData(), "large ltr")
                .addCheckbox(VantarKey.ADMIN_DELETE_ALL, "deleteall")
                .addSubmit(VantarKey.ADMIN_DATA_ENTRY)
                .blockEnd()
                .finish();
            return;
        }

        if (dtoIndex.dbms.equals(DtoDictionary.Dbms.SQL)) {
            if (SqlConnection.isUp()) {
                CommonModelSql.importDataAdmin(
                    params.getString("import"),
                    dto,
                    dto.getPresentationPropertyNames(),
                    params.isChecked("deleteall"),
                    ui
                );
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
            }
        } else if (dtoIndex.dbms.equals(DtoDictionary.Dbms.MONGO)) {
            if (MongoConnection.isUp()) {
                ImportMongo.importDataAdmin(
                    params.getString("import"),
                    dto,
                    dto.getPresentationPropertyNames(),
                    params.isChecked("deleteall"),
                    ui
                );
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
            }
        } else if (dtoIndex.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
            if (ElasticConnection.isUp()) {
                CommonModelElastic.importDataAdmin(
                    params.getString("import"),
                    dto,
                    dto.getPresentationPropertyNames(),
                    params.isChecked("deleteall"),
                    ui
                );
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
            }
        }

        if (dtoIndex.broadcastMessage != null) {
            Services.messaging.broadcast(dtoIndex.broadcastMessage);
        }

        ui.finish();
    }

    public static Map<String, Map<String, Long>> getDatabaseRecordCount() {
        Map<String, Map<String, Long>> databases = new HashMap<>(5, 1);

        if (MongoConnection.isUp()) {
            Map<String, Long> db = new HashMap<>(200, 1);
            try {
                for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
                    db.put(info.dtoClass.getSimpleName(), MongoQuery.count(info.getDtoInstance().getStorage()));
                }
            } catch (Exception ignore) {

            }
            databases.put("Mongo", db);
        }

        if (SqlConnection.isUp()) {
            Map<String, Long> db = new HashMap<>(200, 1);
            try (SqlConnection connection = new SqlConnection()) {
                CommonRepoSql repo = new CommonRepoSql(connection);
                for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.SQL)) {
                    db.put(info.dtoClass.getSimpleName(), repo.count(info.getDtoInstance().getStorage()));
                }
            } catch (Exception ignore) {

            }
            databases.put("SQL", db);
        }

        if (ElasticConnection.isUp()) {
            Map<String, Long> db = new HashMap<>(200, 1);
            try {
                for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.ELASTIC)) {
                    db.put(info.dtoClass.getSimpleName(), CommonRepoElastic.count(info.getDtoInstance().getStorage()));
                }
            } catch (Exception ignore) {

            }
            databases.put("Elastic", db);
        }

        return databases;
    }


    public static void statusSql(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi("SQL", params, response, true);
        statusSql(ui);
        ui.finish();
    }

    public static void statusSql(WebUi ui) {
        if (!SqlConnection.isUp()) {
            if (SqlConnection.isEnabled()) {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ENABLED, "SQL"));
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_DISABLED, "SQL"));
            }
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
            return;
        }
        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ON, "SQL")).write();

        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repo = new CommonRepoSql(connection);
            for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.SQL)) {
                ui.addKeyValue(info.dtoClass.getSimpleName(), repo.count(info.getDtoInstance().getStorage()) + " records").write();
            }
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
            ServiceLog.log.error("!", e);
        }

        ui.blockEnd().blockEnd().write();
    }

    public static void statusMongo(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi("Mongo", params, response, true);
        statusMongo(ui);
        ui.finish();
    }

    public static void statusMongo(WebUi ui) {
        if (!MongoConnection.isUp()) {
            if (MongoConnection.isEnabled()) {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ENABLED, "Mongo"));
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_DISABLED, "Mongo"));
            }
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
            return;
        }
        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ON, "Mongo")).write();

        try {
            for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
                ui.addKeyValue(
                    info.dtoClass.getSimpleName(),
                    MongoQuery.count(info.getDtoInstance().getStorage()) + " records"
                ).write();
            }
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
            ServiceLog.log.error("!", e);
        }

        ui.blockEnd().blockEnd().write();
    }



    public static void statusElastic(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi("Elastic", params, response, true);
        statusElastic(ui);
        ui.finish();
    }

    public static void statusElastic(WebUi ui) {
        if (!ElasticConnection.isUp()) {
            if (ElasticConnection.isEnabled()) {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ENABLED, "ElasticSearch"));
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_DISABLED, "ElasticSearch"));
            }
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
            return;
        }
        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ON, "ElasticSearch")).write();

        try {
            for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.ELASTIC)) {
                ui.addKeyValue(
                    info.dtoClass.getSimpleName(),
                    CommonRepoElastic.count(info.getDtoInstance().getStorage()) + " records"
                );
            }
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
            ServiceLog.log.error("!", e);
        }

        ui.blockEnd().blockEnd().write();
    }

    public static void purge(String collection) throws DatabaseException {
        Mongo.deleteAll(collection);
        Mongo.Index.remove(collection);
        Mongo.Sequence.remove(collection);
    }

    public static void archiveSwitch(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_UPDATE), params, response, dtoInfo);

        Dto dto = dtoInfo.getDtoInstance();
        String a = params.getString("a");
        if (StringUtil.isEmpty(a)) {
            ui.addMessage("wrong params");
        } else {
            ServiceDbArchive.switchCollection(dto.getClass(), a);
            ui.addMessage(dto.getClass().getSimpleName() + " storage switched to " + a + " refresh data page.");
        }

        ui.finish();
    }


    public interface Event {

        Dto dtoExchange(Dto dto, String action);

        void beforeInsert(Dto dto);
        void afterInsert(Dto dto);

        void beforeUpdate(Dto dto);
        void afterUpdate(Dto dto);

        void beforeDelete(Dto dto);
        void afterDelete(Dto dto);
    }


    private static Event getEvent() {
        String adminApp = Settings.getAdminApp();
        if (StringUtil.isNotEmpty(adminApp)) {
            try {
                return (Event) ClassUtil.callStaticMethod(adminApp + ".getAdminDataEvent");
            } catch (Throwable e) {
                ServiceLog.log.error("! AdminData '{}.getAdminDataEvent()'", adminApp, e);
            }
        }
        return null;
    }
}