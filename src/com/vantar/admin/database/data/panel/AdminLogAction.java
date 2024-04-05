package com.vantar.admin.database.data.panel;

import com.vantar.admin.index.Admin;
import com.vantar.business.*;
import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticSearch;
import com.vantar.database.nosql.mongo.MongoQuery;
import com.vantar.database.query.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.log.dto.UserLog;
import com.vantar.util.json.Json;
import com.vantar.util.object.ClassUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminLogAction {

    public static void search(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_USER_LOG), params, response, dtoInfo);
        if (dtoInfo == null) {
            return;
        }
        if (!DataUtil.isUp(dtoInfo.dbms, ui)) {
            ui.finish();
            return;
        }
        String dtoName = dtoInfo.dtoClass.getSimpleName();

        QueryBuilder q = params.getQueryBuilder("jsonsearch", new UserLog.Mini());
        if (q == null) {
            q = new QueryBuilder(new UserLog.Mini())
                .page(params.getInteger("page", 1), params.getInteger("page-length", DataUtil.N_PER_PAGE))
                .sort(params.getString("sort", VantarParam.ID) + ":" + params.getString("sortpos", "desc"));
        } else {
            List<ValidationError> errors = q.getErrors();
            if (errors != null) {
                ui.write().addErrorMessage(ValidationError.toString(errors)).finish();
                return;
            }
        }
        q.condition().equal("classNameSimple", dtoName);
        Long id = params.getLong("id");
        if (id != null) {
            q.condition().equal("objectId", id);
        }

        PageData data = null;
        try {
            // > > > MONGO
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                data = MongoQuery.getPage(q, null);
                // > > > SQL
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                try (SqlConnection connection = new SqlConnection()) {
                    SqlSearch search = new SqlSearch(connection);
                    data = search.getPage(q);
                }
                // > > > ELASTIC
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                data = ElasticSearch.getPage(q);
            }

        } catch (NoContentException ignore) {

        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        WebUi.DtoListOptions options = new WebUi.DtoListOptions();
        options.actionLink = false;
        options.archive = false;
        options.pagination = true;
        options.search = true;
        options.fields = new String[] {"id", "action", "time", "userName", "userId", "objectId", "url", "threadId"};
        options.colOptionCount = 3;
        options.checkListFormUrl = "/admin/data/log/action/differences";
        options.event = new WebUi.DtoListOptions.Event() {
            @Override
            public void checkListFormContent() {
                ui.addEmptyLine();
                ui.direction = "ltr".equalsIgnoreCase(ui.direction) ? "rtl" : "ltr";
                ui.alignKey = "left".equalsIgnoreCase(ui.alignKey) ? "right" : "left";
                ui.addSubmit(VantarKey.ADMIN_LOG_DIFFERENCES, "log-change-button", "log-change-button");
                ui.direction = "ltr".equalsIgnoreCase(ui.direction) ? "rtl" : "ltr";
                ui.alignKey = "left".equalsIgnoreCase(ui.alignKey) ? "right" : "left";
            }

            @Override
            public List<WebUi.DtoListOptions.ColOption> getColOptions(Dto dtoX) {
                UserLog.Mini log = (UserLog.Mini) dtoX;
                List<WebUi.DtoListOptions.ColOption> colOptions = new ArrayList<>(3);

                WebUi.DtoListOptions.ColOption deleteCheckBox = new WebUi.DtoListOptions.ColOption();
                deleteCheckBox.containerClass = "delete-option";
                deleteCheckBox.content = ui.getCheckbox("delete-check", false, dtoX.getId(), "delete-check", false);
                colOptions.add(deleteCheckBox);

                WebUi.DtoListOptions.ColOption action = new WebUi.DtoListOptions.ColOption();
                if ("DELETE".equalsIgnoreCase(log.action)) {
                    action.content = ui.getHref(
                        VantarKey.ADMIN_UNDELETE,
                        "/admin/data/undelete?dto=UserLog&id=" + dtoX.getId(), true, false, null
                    );
                } else {
                    action.content = ui.getHref(
                        VantarKey.ADMIN_REVERT,
                        "/admin/data/log/action/revert?dto=UserLog&id=" + dtoX.getId(), true, false, null
                    );
                }
                colOptions.add(action);

                WebUi.DtoListOptions.ColOption view = new WebUi.DtoListOptions.ColOption();
                view.content = ui.getHref(
                    VantarKey.ADMIN_VIEW,
                    "/admin/data/view?dto=UserLog&id=" + dtoX.getId(), true, false, null
                );
                colOptions.add(view);

                return colOptions;
            }
        };

        ui.addDtoListWithHeader(data, dtoInfo, options);
        ui.finish();
    }

    public static void revert(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_REVERT), params, response, dtoInfo);
        if (dtoInfo == null) {
            return;
        }
        if (!DataUtil.isUp(dtoInfo.dbms, ui)) {
            ui.finish();
            return;
        }
        UserLog userLog = new UserLog();
        userLog.setId(params.getLong("id"));
        if (userLog.getId() == null) {
            ui.finish();
            return;
        }

        if (params.isChecked("confirm")) {
            revertDto(ui, userLog);
            ui.finish();
            return;
        }

        try {
            userLog = ModelMongo.getById(userLog);
        } catch (VantarException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        ui  .addHeading(3, userLog.classNameSimple + " " + userLog.objectId)
            .addBlock("pre", Json.d.toJsonPretty(Json.d.toJson(userLog.objectX)), "view-pre")
            .addEmptyLine()
            .beginFormPost()
            .addCheckbox(VantarKey.ADMIN_CONFIRM, "confirm")
            .addSubmit(VantarKey.ADMIN_REVERT)
            .finish();
    }

    @SuppressWarnings("unchecked")
    private static void revertDto(WebUi ui, UserLog dto) {
        try {
            UserLog userLog = ModelMongo.getById(dto);
            Class<? extends Dto> dtoClass = (Class<? extends Dto>) ClassUtil.getClass(userLog.className);
            Dto dtoUndelete = Json.d.fromJson(Json.d.toJson(userLog.objectX), dtoClass);

            ModelMongo.update(new ModelCommon.Settings(dtoUndelete));

            ui.addMessage(Locale.getString(VantarKey.ADMIN_REVERTED, userLog.classNameSimple, userLog.objectId));
        } catch (VantarException e) {
            ui.addErrorMessage(e);
        }
    }
}