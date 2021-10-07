package com.vantar.admin.model;

import com.vantar.business.*;
import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.*;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.query.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.*;
import com.vantar.web.*;
import org.slf4j.*;
import javax.servlet.http.HttpServletResponse;


public class AdminData {

    private static final Logger log = LoggerFactory.getLogger(AdminData.class);
    private static final int N_PER_PAGE = 30;
    private static final String PARAM_DELETE_ALL = "deleteall";

    public static Event event;


    public static void index(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_MENU_DATA), params, response);
        if (ui == null) {
            return;
        }

        DtoDictionary.getStructure().forEach((groupName, groupDtos) -> {
            ui.beginBox(groupName);
            groupDtos.forEach((dtoName, info) -> {
                if (info.dbms == null) {
                    log.warn("! {} missing dbms", info.getDtoClass());
                    return;
                }
                ui.beginFloatBox("db-box", info.getDtoClass(), info.title)
                    .addTag(info.dbms.toString())
                    .addBlockLink(Locale.getString(VantarKey.ADMIN_DATA_LIST), "/admin/data/list?" + VantarParam.DTO + "=" + dtoName)
                    .addBlockLink(Locale.getString(VantarKey.ADMIN_NEW_RECORD), "/admin/data/insert?" + VantarParam.DTO + "=" + dtoName)
                    .addBlockLink(Locale.getString(VantarKey.ADMIN_IMPORT), "/admin/data/import?" + VantarParam.DTO + "=" + dtoName)
                    .addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), "/admin/data/purge?" + VantarParam.DTO + "=" + dtoName)
                    .containerEnd();
            });

            ui.containerEnd();
        });

        // > > >
        ui.beginBox(Locale.getString(VantarKey.ADMIN_DATABASE_TITLE));

        ui.beginFloatBox("system-box", "SQL");
        ui.addBlockLink("Indexes", "/admin/database/sql/indexes");
        ui.containerEnd();

        ui.beginFloatBox("system-box", "MONGO");
        ui.addBlockLink("Indexes", "/admin/database/mongo/indexes");
        ui.addBlockLink("Sequences", "/admin/database/mongo/sequences");
        ui.containerEnd();

        ui.finish();
    }

    public static void list(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_DATA_LIST), params, response, dtoInfo);
        if (ui == null) {
            return;
        }

        Dto dto = dtoInfo.getDtoInstance();
        QueryData queryData = params.getQueryData(VantarParam.JSON_SEARCH);
        QueryBuilder q;

        if (queryData == null) {
            q = new QueryBuilder(dto)
                .page(params.getInteger(VantarParam.PAGE, 1), params.getInteger(VantarParam.COUNT, N_PER_PAGE))
                .sort(params.getString(VantarParam.SORT_FIELD, "id") + ":" + params.getString(VantarParam.SORT_POS, "desc"));

            String field = params.getString(VantarParam.SEARCH_FIELD);
            String string = Persian.Number.toLatin(params.getString(VantarParam.SEARCH_VALUE));
            if (field != null && string != null) {
                if (field.equals("all")) {
                    q.condition().phrase(string);
                } else {
                    Class<?> type = dto.getPropertyType(field);
                    if (string.contains("*")) {
                        q.condition().like(field, StringUtil.remove(string, '*'));
                    } else if (type == null) {
                        q.condition().equal(field, params.getString(VantarParam.SEARCH_VALUE));
                    } else if (type == String.class) {
                        q.condition().like(field, string);
                    } else {
                        q.condition().equal(field, params.getObject(VantarParam.SEARCH_VALUE, type));
                    }
                }
            }

        } else {
            try {
                queryData.setDto(dto);
                q = new QueryBuilder(queryData);
            } catch (InputException e) {
                ui.write().addErrorMessage(e).finish();
                return;
            }
        }

        dto.setQueryDeleted(
            params.isChecked(VantarParam.LOGICAL_DELETED) ? Dto.QueryDeleted.SHOW_DELETED : Dto.QueryDeleted.SHOW_NOT_DELETED
        );

        if (dtoInfo.getDtoClass().equals("User") && !Services.get(ServiceAuth.class).hasAccess(params, AdminUserRole.ROOT)) {
            q.condition().notEqual("isRoot", true);
        }

        PageData data = null;
        try {
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                if (SqlConnection.isUp) {
                    try (SqlConnection connection = new SqlConnection()) {
                        SqlSearch search = new SqlSearch(connection);
                        data = search.getPage(q);
                    }
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                if (MongoConnection.isUp) {
                    data = MongoSearch.getPage(q);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                if (ElasticConnection.isUp) {
                    data = ElasticSearch.getPage(q);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                }
            }
        } catch (NoContentException ignore) {

        } catch (DatabaseException e) {
            ui.addErrorMessage(ObjectUtil.throwableToString(e));
            log.error("! {}", dto, e);
        }

        ui.addDtoListWithHeader(data, dtoInfo, q.getDtoResult().getProperties());

        ui.finish();
    }

    public static void delete(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_DELETE), params, response, dtoInfo);
        if (ui == null) {
            return;
        }

        Dto dto = dtoInfo.getDtoInstance();
        dto.setQueryDeleted(Dto.QueryDeleted.SHOW_ALL);

        if (!params.isChecked("f")) {
            dto.setDeleteLogical(false);
            QueryBuilder q = new QueryBuilder(dto);
            q.condition().inNumber("id", params.getLongList(VantarParam.ID));

            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                if (SqlConnection.isUp) {
                    try (SqlConnection connection = new SqlConnection()) {
                        CommonRepoSql repo = new CommonRepoSql(connection);
                        ui.addDeleteForm(repo.getData(q), dtoInfo.present);
                    } catch (NoContentException x) {
                        ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
                    } catch (DatabaseException e) {
                        ui.addErrorMessage(e);
                        log.error("! {}", dto, e);
                    }
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                if (MongoConnection.isUp) {
                    try {
                        ui.addDeleteForm(CommonRepoMongo.getData(q), dtoInfo.present);
                    } catch (NoContentException x) {
                        ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
                    } catch (DatabaseException e) {
                        ui.addErrorMessage(e);
                        log.error("! {}", dto, e);
                    }
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                if (ElasticConnection.isUp) {
                    try {
                        ui.addDeleteForm(CommonRepoElastic.getData(q), dtoInfo.present);
                    } catch (NoContentException x) {
                        ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
                    } catch (DatabaseException e) {
                        ui.addErrorMessage(e);
                        log.error("! {}", dto, e);
                    }
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                }
            }

            ui.finish();
            return;
        }

        if (event != null) {
            event.beforeDelete(dto);
        }

        try {
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                if (SqlConnection.isUp) {
                    ui.addMessage(CommonModelSql.deleteBatch(params, dto.getClass()).message);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                if (MongoConnection.isUp) {
                    if (params.isChecked(VantarParam.LOGICAL_DELETED_UNDO)) {
                        ui.addMessage(CommonModelMongo.unDeleteBatch(params, dto.getClass()).message);
                    } else {
                        dto.setDeleteLogical(params.isChecked(VantarParam.LOGICAL_DELETED));
                        ui.addMessage(CommonModelMongo.deleteBatch(params, dto.getClass()).message);
                    }
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                if (ElasticConnection.isUp) {
                    ui.addMessage(CommonModelElastic.deleteBatch(params, dto.getClass()).message);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                }
            }

            if (event != null) {
                event.afterDelete(dto);
            }

        } catch (ServerException | InputException e) {
            ui.addErrorMessage(e);
            log.error("! {}", dto, e);
        }

        if (dtoInfo.broadcastMessage != null) {
            Services.messaging.broadcast(dtoInfo.broadcastMessage);
        }

        ui.finish();
    }

    public static void purge(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), params, response, dtoInfo);
        if (ui == null) {
            return;
        }
        Dto dto = dtoInfo.getDtoInstance();

        if (!params.isChecked("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui.addPurgeForm();
        } else {
            try {
                if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                    if (SqlConnection.isUp) {
                        ui.addMessage(CommonModelSql.purgeData(dto.getStorage()).message);
                    } else {
                        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
                    }
                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                    if (MongoConnection.isUp) {
                        ui.addMessage(CommonModelMongo.purgeData(ui.params, dto.getStorage()).message);
                    } else {
                        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
                    }
                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                    if (ElasticConnection.isUp) {
                        ui.addMessage(CommonModelElastic.purgeData(dto.getStorage()).message);
                    } else {
                        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                    }
                }
            } catch (ServerException e) {
                ui.addErrorMessage(e);
                log.error("! {}", dto, e);
            }

            if (dtoInfo.broadcastMessage != null) {
                Services.messaging.broadcast(dtoInfo.broadcastMessage);
            }
        }

        ui.finish();
    }

    public static void update(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_UPDATE), params, response, dtoInfo);
        if (ui == null) {
            return;
        }

        Dto dto = dtoInfo.getDtoInstance();

        if (!params.contains("f")) {
            dto.setId(params.getLong(VantarParam.ID));
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                if (SqlConnection.isUp) {
                    try (SqlConnection connection = new SqlConnection()) {
                        CommonRepoSql repo = new CommonRepoSql(connection);
                        ui.addDtoUpdateForm(
                            repo.getById(dto),
                            params.getString("root") == null ? dto.getProperties(dtoInfo.getUpdateExclude()) : dto.getProperties()
                        );
                    } catch (DatabaseException e) {
                        ui.addErrorMessage(e);
                        log.error("! {}", dto, e);
                    } catch (NoContentException e) {
                        ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
                    }
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                if (MongoConnection.isUp) {
                    try {
                        ui.addDtoUpdateForm(
                            CommonRepoMongo.getById(dto),
                            params.getString("root") == null ? dto.getProperties(dtoInfo.getUpdateExclude()) : dto.getProperties()
                        );
                    } catch (DatabaseException e) {
                        ui.addErrorMessage(e);
                        log.error("! {}", dto, e);
                    } catch (NoContentException e) {
                        ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
                    }
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                if (ElasticConnection.isUp) {
                    try {
                        ui.addDtoUpdateForm(
                            CommonRepoElastic.getById(dto),
                            params.getString("root") == null ? dto.getProperties(dtoInfo.getUpdateExclude()) : dto.getProperties()
                        );
                    } catch (DatabaseException e) {
                        ui.addErrorMessage(e);
                        log.error("! {}", dto, e);
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
                if (SqlConnection.isUp) {
                    CommonModelSql.updateJson(params.getString("asjson"), dto);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                if (MongoConnection.isUp) {
                    CommonModelMongo.updateJson(params, "asjson", dto);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                if (ElasticConnection.isUp) {
                    CommonModelElastic.updateJson(params.getString("asjson"), dto);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                }
            }

            if (event != null) {
                event.afterUpdate(dto);
            }

            ui.addMessage(Locale.getString(VantarKey.UPDATE_SUCCESS));

        } catch (InputException | ServerException e) {
            ui.addErrorMessage(e);
            log.error("! {}", dto, e);
        }

        if (dtoInfo.broadcastMessage != null) {
            Services.messaging.broadcast(dtoInfo.broadcastMessage);
        }

        ui.finish();
    }

    public static void insert(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_NEW_RECORD), params, response, dtoInfo);
        if (ui == null) {
            return;
        }
        Dto dto = dtoInfo.getDtoInstance();

        if (!params.isChecked("f")) {
            ui.addDtoAddForm(dto, params.getString("root") == null ? dto.getProperties(dtoInfo.getInsertExclude()) : dto.getProperties());
            ui.finish();
            return;
        }

        if (event != null) {
            event.beforeInsert(dto);
        }

        try {
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                if (SqlConnection.isUp) {
                    CommonModelSql.insert(params, dto);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                if (MongoConnection.isUp) {
                    CommonModelMongo.insertJson(params, "asjson", dto);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
                }
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                if (ElasticConnection.isUp) {
                    CommonModelElastic.insert(params, dto);
                } else {
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                }
            }

            if (event != null) {
                event.afterInsert(dto);
            }

            ui.addMessage(Locale.getString(VantarKey.INSERT_SUCCESS));

        } catch (InputException | ServerException e) {
            ui.addErrorMessage(e);
            log.error("! {}", dto, e);
        }

        if (dtoInfo.broadcastMessage != null) {
            Services.messaging.broadcast(dtoInfo.broadcastMessage);
        }

        ui.finish();
    }

    public static void importData(Params params, HttpServletResponse response, DtoDictionary.Info dtoIndex) {
        if (dtoIndex == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_IMPORT), params, response, dtoIndex);
        if (ui == null) {
            return;
        }
        Dto dto = dtoIndex.getDtoInstance();

        if (!params.isChecked("f")) {
            ui.addImportForm(dtoIndex.getImportData()).finish();
            return;
        }

        if (dtoIndex.dbms.equals(DtoDictionary.Dbms.SQL)) {
            if (SqlConnection.isUp) {
                CommonModelSql.importDataAdmin(
                    params.getString("import"),
                    dto,
                    dtoIndex.present,
                    params.isChecked(PARAM_DELETE_ALL),
                    ui
                );
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
            }
        } else if (dtoIndex.dbms.equals(DtoDictionary.Dbms.MONGO)) {
            if (MongoConnection.isUp) {
                CommonModelMongo.importDataAdmin(
                    params.getString("import"),
                    dto,
                    dtoIndex.present,
                    params.isChecked(PARAM_DELETE_ALL),
                    ui
                );
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
            }
        } else if (dtoIndex.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
            if (ElasticConnection.isUp) {
                CommonModelElastic.importDataAdmin(
                    params.getString("import"),
                    dto,
                    dtoIndex.present,
                    params.isChecked(PARAM_DELETE_ALL),
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

    public static void statusSql(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_STATUS, "SQL"), params, response);
        if (ui == null) {
            return;
        }
        statusSql(ui);
        ui.finish();
    }

    public static void statusSql(WebUi ui) {
        if (!SqlConnection.isUp) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "SQL"));
            return;
        }
        ui.beginBox(Locale.getString(VantarKey.ADMIN_RECORD_COUNT, "SQL")).write();

        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repo = new CommonRepoSql(connection);
            for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.SQL)) {
                ui.addKeyValue(info.dtoClass.getSimpleName(), repo.count(info.getDtoInstance().getStorage()) + " records");
            }
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
            log.error("!", e);
        }

        ui.containerEnd().containerEnd().write();
    }

    public static void statusMongo(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_STATUS, "Mongo"), params, response);
        if (ui == null) {
            return;
        }
        statusMongo(ui);
        ui.finish();
    }

    public static void statusMongo(WebUi ui) {
        if (!MongoConnection.isUp) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "Mongo"));
            return;
        }
        ui.beginBox(Locale.getString(VantarKey.ADMIN_RECORD_COUNT, "Mongo")).write();

        try {
            for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
                ui.addKeyValue(info.dtoClass.getSimpleName(), CommonRepoMongo.count(info.getDtoInstance().getStorage()) + " records");
            }
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
            log.error("!", e);
        }

        ui.containerEnd().containerEnd().write();
    }

    public static void statusElastic(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_STATUS, "Elastic"), params, response);
        if (ui == null) {
            return;
        }
        statusElastic(ui);
        ui.finish();
    }

    public static void statusElastic(WebUi ui) {
        if (!ElasticConnection.isUp) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
            return;
        }
        ui.beginBox(Locale.getString(VantarKey.ADMIN_RECORD_COUNT, "Elastic")).write();

        try {
            for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.ELASTIC)) {
                ui.addKeyValue(info.dtoClass.getSimpleName(), CommonRepoElastic.count(info.getDtoInstance().getStorage()) + " records");
            }
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
            log.error("!", e);
        }

        ui.containerEnd().containerEnd().write();
    }


    public interface Event {

        void beforeInsert(Dto dto);
        void afterInsert(Dto dto);

        void beforeUpdate(Dto dto);
        void afterUpdate(Dto dto);

        void beforeDelete(Dto dto);
        void afterDelete(Dto dto);
    }
}