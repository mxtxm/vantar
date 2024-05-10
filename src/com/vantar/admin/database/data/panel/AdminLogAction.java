package com.vantar.admin.database.data.panel;

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
import com.vantar.service.log.dto.*;
import com.vantar.util.json.Json;
import com.vantar.util.object.ClassUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminLogAction {

    /**
     * type a dto           -> get action logs filtered by dto
     * type b dto id un ufn -> get action logs filtered by dto.id
     * type c dto id un ufn -> get user activities filtered by User.id
     * type d  -  id un ufn -> get logs related to a web log
     */
    public static void search(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        String type = params.getString("type", "a");
        boolean isTypeA = "a".equals(type);
        boolean isTypeB = "b".equals(type);
        boolean isTypeC = "c".equals(type);
        boolean isTypeD = "d".equals(type);
        DataUtil.Ui u = isTypeA ?
            DataUtil.initDto(VantarKey.ADMIN_ACTION_LOG, "log-action", params, response, info) :
            DataUtil.initDtoItem(
                isTypeC ? VantarKey.ADMIN_USER_ACTIVITY : VantarKey.ADMIN_ACTION_LOG,
                isTypeC ? "log-activity" : "log-action",
                params,
                response,
                info
            );

        QueryBuilder q = params.getQueryBuilder("jsonsearch", new UserLog.Mini());
        if (q == null) {
            q = new QueryBuilder(new UserLog.Mini())
                .page(params.getInteger("page", 1), params.getInteger("page-length", DataUtil.N_PER_PAGE))
                .sort(params.getString("sort", VantarParam.ID) + ":" + params.getString("sortpos", "desc"));
        } else {
            List<ValidationError> errors = q.getErrors();
            if (errors != null) {
                u.ui.write().addErrorMessage(ValidationError.toString(errors)).finish();
                return;
            }
        }

        String dtoName = info.dtoClass.getSimpleName();
        if (isTypeC) {
            q.condition().equal("userId", u.dto.getId());

        } else if (isTypeD) {
            UserWebLog.Mini webLog;
            try {
                webLog = ModelMongo.getById(params, new UserWebLog.Mini());
            } catch (VantarException e) {
                u.ui.addErrorMessage(e).finish();
                return;
            }
            q.condition().equal("threadId", webLog.threadId);
            if (params.getBoolean("old", false)) {
                q.condition()
                    .equal("timeDay", webLog.timeDay)
                    .equal("userId", webLog.userId)
                    .equal("url", webLog.url)
                    .equal("objectId", webLog.objectId);
            }

        } else {
            q.condition().equal("classNameSimple", dtoName);
            if (u.dto.getId() != null) {
                q.condition().equal("objectId", u.dto.getId());
            }
        }

        PageData data = null;
        try {
            // > > > MONGO
            if (info.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                data = MongoQuery.getPage(q, null);

            // > > > SQL
            } else if (info.dbms.equals(DtoDictionary.Dbms.SQL)) {
                try (SqlConnection connection = new SqlConnection()) {
                    SqlSearch search = new SqlSearch(connection);
                    data = search.getPage(q);
                }
                // > > > ELASTIC
            } else if (info.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                data = ElasticSearch.getPage(q);
            }

        } catch (NoContentException ignore) {

        } catch (DatabaseException e) {
            u.ui.addErrorMessage(e);
        }

        WebUi.DtoListOptions options = new WebUi.DtoListOptions();
        options.archive = false;
        options.pagination = true;
        options.search = true;
        options.fields = isTypeC || isTypeD ?
            new String[] {"id", "action", "time", "classNameSimple", "objectId", "url", "threadId"} :
            new String[] {"id", "action", "time", "userName", "userId", "objectId", "url", "threadId"};
        options.colOptionCount = 4;
        if (isTypeC || isTypeD) {
            --options.colOptionCount;
        } else {
            options.checkListFormUrl = "/admin/data/log/action/differences";
        }
        options.event = new WebUi.DtoListOptions.Event() {
            @Override
            public void checkListFormContent() {
                if (!isTypeC && !isTypeD) {
                    u.ui.addEmptyLine();
                    u.ui.direction = "ltr".equalsIgnoreCase(u.ui.direction) ? "rtl" : "ltr";
                    u.ui.alignKey = "left".equalsIgnoreCase(u.ui.alignKey) ? "right" : "left";
                    u.ui.addSubmit(VantarKey.ADMIN_LOG_DIFFERENCES, "log-change-button", "log-change-button");
                    u.ui.direction = "ltr".equalsIgnoreCase(u.ui.direction) ? "rtl" : "ltr";
                    u.ui.alignKey = "left".equalsIgnoreCase(u.ui.alignKey) ? "right" : "left";
                }
            }

            @Override
            public List<WebUi.DtoListOptions.ColOption> getColOptions(Dto dtoX) {
                UserLog.Mini log = (UserLog.Mini) dtoX;
                List<WebUi.DtoListOptions.ColOption> colOptions = new ArrayList<>(3);

                if (!isTypeC && !isTypeD) {
                    WebUi.DtoListOptions.ColOption deleteCheckBox = new WebUi.DtoListOptions.ColOption();
                    deleteCheckBox.containerClass = "delete-option";
                    deleteCheckBox.content = u.ui.getCheckbox("delete-check", false, dtoX.getId(), "delete-check", false);
                    colOptions.add(deleteCheckBox);
                }

                WebUi.DtoListOptions.ColOption action = new WebUi.DtoListOptions.ColOption();
                if ("DELETE".equalsIgnoreCase(log.action)) {
                    action.content = u.ui.getHref(
                        VantarKey.ADMIN_UNDELETE,
                        "/admin/data/undelete?dto=" + dtoName + "&id=" + dtoX.getId(), true, false, null
                    );
                } else {
                    action.content = u.ui.getHref(
                        VantarKey.ADMIN_REVERT,
                        "/admin/data/log/action/revert?dto=" + dtoName + "&id=" + dtoX.getId(), true, false, null
                    );
                }
                colOptions.add(action);

                WebUi.DtoListOptions.ColOption view = new WebUi.DtoListOptions.ColOption();
                view.content = u.ui.getHref(
                    VantarKey.ADMIN_VIEW,
                    "/admin/data/view?dto=UserLog&id=" + dtoX.getId(), true, false, null
                );
                colOptions.add(view);

                WebUi.DtoListOptions.ColOption logWeb = new WebUi.DtoListOptions.ColOption();
                logWeb.content = u.ui.getHref(
                    VantarKey.ADMIN_WEB,
                    "/admin/data/log/web/search?type=b&dto=UserLog&id=" + dtoX.getId()
                        + "&un=" + params.getString("un") + "&ufn=" + params.getString("ufn"),
                    true, false, null
                );
                colOptions.add(logWeb);

                return colOptions;
            }
        };

        u.ui.beginBlock("div", isTypeC || isTypeD ? "user-activities" : "action-logs")
            .addDtoListWithHeader(data, info, options)
            .finish();
    }

    public static void revert(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDto(VantarKey.ADMIN_REVERT, "revert", params, response, info);

        UserLog userLog = new UserLog();
        userLog.setId(params.getLong("id"));
        if (userLog.getId() == null) {
            u.ui.finish();
            return;
        }

        if (params.isChecked("confirm")) {
            revertDto(u.ui, userLog);
            u.ui.finish();
            return;
        }

        try {
            userLog = ModelMongo.getById(userLog);
        } catch (VantarException e) {
            u.ui.addErrorMessage(e).finish();
            return;
        }

        u.ui.addHeading(3, userLog.classNameSimple + " " + userLog.objectId)
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