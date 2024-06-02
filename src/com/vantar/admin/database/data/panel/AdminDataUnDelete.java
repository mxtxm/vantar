package com.vantar.admin.database.data.panel;

import com.vantar.business.*;
import com.vantar.common.VantarParam;
import com.vantar.database.common.*;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticSearch;
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


public class AdminDataUnDelete {

    public static void search(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDto(VantarKey.ADMIN_UNDELETE, "undelete", params, response, info);
        String dtoName = info.dtoClass.getSimpleName();

        QueryBuilder q = params.getQueryBuilder("jsonsearch", new UserLog());
        if (q == null) {
            q = new QueryBuilder(new UserLog())
                .page(params.getInteger("page", 1), params.getInteger("page-length", DataUtil.N_PER_PAGE))
                .sort(params.getString("sort", VantarParam.ID) + ":" + params.getString("sortpos", "desc"));
        } else {
            List<ValidationError> errors = q.getErrors();
            if (errors != null) {
                u.ui.write().addErrorMessage(ValidationError.toString(errors)).finish();
                return;
            }
        }
        q.condition()
            .equal("classNameSimple", dtoName)
            .equal("action", "DELETE");

        PageData data = null;
        try {
            // > > > MONGO
            if (info.dbms.equals(Db.Dbms.MONGO)) {
                data = Db.mongo.getPage(q);
                // > > > SQL
            } else if (info.dbms.equals(Db.Dbms.SQL)) {
                try (SqlConnection connection = new SqlConnection()) {
                    SqlSearch search = new SqlSearch(connection);
                    data = search.getPage(q);
                }
                // > > > ELASTIC
            } else if (info.dbms.equals(Db.Dbms.ELASTIC)) {
                data = ElasticSearch.getPage(q);
            }

        } catch (NoContentException ignore) {

        } catch (VantarException e) {
            u.ui.addErrorMessage(e);
        }

        WebUi.DtoListOptions options = new WebUi.DtoListOptions();
        options.archive = false;
        options.pagination = true;
        options.search = true;
        options.fields = new String[] {"id", "time", "userName", "userId", "objectId", "url",};
        options.colOptionCount = 2;
        options.checkListFormUrl = "/admin/data/undelete/many?dto" + dtoName;
        options.event = new WebUi.DtoListOptions.Event() {
            @Override
            public void checkListFormContent(WebUi ui) {
                u.ui.addEmptyLine();
                u.ui.direction = "ltr".equalsIgnoreCase(u.ui.direction) ? "rtl" : "ltr";
                u.ui.alignKey = "left".equalsIgnoreCase(u.ui.alignKey) ? "right" : "left";
                u.ui.addCheckbox(VantarKey.SELECT_ALL, "delete-select-all");
                u.ui.addCheckbox(VantarKey.ADMIN_CONFIRM, "confirm");
                u.ui.addSubmit(VantarKey.ADMIN_UNDELETE, "undelete-button", "undelete-button");
                u.ui.direction = "ltr".equalsIgnoreCase(u.ui.direction) ? "rtl" : "ltr";
                u.ui.alignKey = "left".equalsIgnoreCase(u.ui.alignKey) ? "right" : "left";
            }

            @Override
            public List<WebUi.DtoListOptions.ColOption> getColOptions(Dto dtoX) {
                List<WebUi.DtoListOptions.ColOption> colOptions = new ArrayList<>(3);

                WebUi.DtoListOptions.ColOption undeleteCheckBox = new WebUi.DtoListOptions.ColOption();
                undeleteCheckBox.containerClass = "delete-option";
                undeleteCheckBox.content = u.ui.getCheckbox("delete-check", false, dtoX.getId(), "delete-check", false);
                colOptions.add(undeleteCheckBox);

                WebUi.DtoListOptions.ColOption view = new WebUi.DtoListOptions.ColOption();
                view.content = u.ui.getHref(
                    VantarKey.ADMIN_VIEW,
                    "/admin/data/view?dto=UserLog&id=" + dtoX.getId(), true, false, null
                );
                colOptions.add(view);

                return colOptions;
            }
        };

        u.ui.addDtoListWithHeader(data, info, options);
        u.ui.finish();
    }

    public static void undeleteMany(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDto(VantarKey.ADMIN_UNDELETE, "undelete", params, response, info);

        if (!params.isChecked("confirm")) {
            u.ui.addMessage(VantarKey.FAIL_DELETE).finish();
            return;
        }
        if (!DataUtil.isUp(u.ui, info.dbms)) {
            u.ui.finish();
            return;
        }

        UserLog userLog = new UserLog();
        for (Long id : params.getLongList("delete-check")) {
            userLog.reset();
            userLog.setId(id);
            unDelete(u.ui, userLog);
        }

        u.ui.finish();
    }

    public static void undeleteOne(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDto(VantarKey.ADMIN_UNDELETE, "undelete", params, response, info);

        UserLog userLog = new UserLog();
        userLog.setId(params.getLong("id"));
        if (userLog.getId() == null) {
            u.ui.finish();
            return;
        }

        if (params.isChecked("confirm")) {
            unDelete(u.ui, userLog);
            u.ui.finish();
            return;
        }

        try {
            userLog = Db.modelMongo.getById(userLog);
        } catch (VantarException e) {
            u.ui.addErrorMessage(e).finish();
            return;
        }

        u.ui.addHeading(3, userLog.classNameSimple + " " + userLog.objectId)
            .addBlock("pre", Json.d.toJsonPretty(Json.d.toJson(userLog.objectX)), "view-pre")
            .addEmptyLine()
            .beginFormPost()
            .addCheckbox(VantarKey.ADMIN_CONFIRM, "confirm")
            .addSubmit(VantarKey.ADMIN_UNDELETE)
            .finish();
    }

    @SuppressWarnings("unchecked")
    private static void unDelete(WebUi ui, UserLog dto) {
        try {
            UserLog userLog = Db.modelMongo.getById(dto);
            Class<? extends Dto> dtoClass = (Class<? extends Dto>) ClassUtil.getClass(userLog.className);
            Dto dtoUndelete = Json.d.fromJson(Json.d.toJson(userLog.objectX), dtoClass);
            dtoUndelete.autoIncrementOnInsert(false);

            Db.modelMongo.insert(new ModelCommon.Settings(dtoUndelete));
            Db.modelMongo.delete(new ModelCommon.Settings(dto));

            ui.addMessage(Locale.getString(VantarKey.ADMIN_UNDELETED, userLog.classNameSimple, userLog.objectId));
        } catch (VantarException e) {
            ui.addErrorMessage(e);
        }
    }
}