package com.vantar.admin.model.database.data.panel;

import com.vantar.admin.model.index.Admin;
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
import com.vantar.service.log.ServiceLog;
import com.vantar.service.log.dto.*;
import com.vantar.util.json.Json;
import com.vantar.util.object.ClassUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminDataUnDelete {

    public static void index(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_UNDELETE), params, response, dtoInfo);
        if (dtoInfo == null) {
            return;
        }
        if (!DataUtil.isUp(dtoInfo.dbms, ui)) {
            ui.finish();
            return;
        }
        String dtoName = dtoInfo.dtoClass.getSimpleName();

        QueryBuilder q = params.getQueryBuilder("jsonsearch", new UserLog());
        if (q == null) {
            q = new QueryBuilder(new UserLog())
                .page(params.getInteger("page", 1), params.getInteger("page-length", DataUtil.N_PER_PAGE))
                .sort(params.getString("sort", VantarParam.ID) + ":" + params.getString("sortpos", "desc"));
        } else {
            List<ValidationError> errors = q.getErrors();
            if (errors != null) {
                ui.write().addErrorMessage(ValidationError.toString(errors)).finish();
                return;
            }
        }
        q.condition()
            .equal("classNameSimple", dtoName)
            .equal("action", "DELETE");

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
        options.fields = new String[]{"id", "time", "userName", "userId", "objectId", "url",};
        options.colOptionCount = 2;
        options.checkListFormUrl = "/admin/data/undelete/many?dto" + dtoName;
        options.event = new WebUi.DtoListOptions.Event() {
            @Override
            public void checkListFormContent() {
                ui.addEmptyLine();
                ui.direction = "ltr".equalsIgnoreCase(ui.direction) ? "rtl" : "ltr";
                ui.alignKey = "left".equalsIgnoreCase(ui.alignKey) ? "right" : "left";
                ui.addCheckbox(VantarKey.SELECT_ALL, "delete-select-all");
                ui.addCheckbox(VantarKey.ADMIN_CONFIRM, "confirm-delete");
                ui.addSubmit(VantarKey.ADMIN_UNDELETE, "undelete-button", "undelete-button");
                ui.direction = "ltr".equalsIgnoreCase(ui.direction) ? "rtl" : "ltr";
                ui.alignKey = "left".equalsIgnoreCase(ui.alignKey) ? "right" : "left";
            }

            @Override
            public List<WebUi.DtoListOptions.ColOption> getColOptions(Dto dtoX) {
                List<WebUi.DtoListOptions.ColOption> colOptions = new ArrayList<>(3);

                WebUi.DtoListOptions.ColOption undeleteCheckBox = new WebUi.DtoListOptions.ColOption();
                undeleteCheckBox.containerClass = "delete-option";
                undeleteCheckBox.content = ui.getCheckbox("delete-check", false, dtoX.getId(), "delete-check", false);
                colOptions.add(undeleteCheckBox);

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

    public static void updateMany(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_UNDELETE), params, response, dtoInfo);
        if (dtoInfo == null) {
            return;
        }
        if (!params.isChecked("confirm-delete")) {
            ui.addMessage(VantarKey.DELETE_FAIL).finish();
            return;
        }
        if (!DataUtil.isUp(dtoInfo.dbms, ui)) {
            ui.finish();
            return;
        }

        UserLog userLog = new UserLog();
        for (Long id : params.getLongList("delete-check")) {
            userLog.reset();
            userLog.setId(id);
            unDelete(ui, userLog);
        }

        ui.finish();
    }

    @SuppressWarnings("unchecked")
    private static void unDelete(WebUi ui, UserLog dto) {
        try {
            UserLog userLog = ModelMongo.getById(dto);
            Class<? extends Dto> dtoClass = (Class<? extends Dto>) ClassUtil.getClass(userLog.className);
            Dto dtoUndelete = Json.d.fromJson(Json.d.toJson(userLog.objectX), dtoClass);
            dtoUndelete.setClearIdOnInsert(false);

            ModelMongo.insert(new ModelCommon.Settings(dtoUndelete));
            ModelMongo.delete(new ModelCommon.Settings(dto));

            ui.addMessage(Locale.getString(VantarKey.ADMIN_UNDELETED, userLog.classNameSimple, userLog.objectId));
        } catch (VantarException e) {
            ui.addErrorMessage(e);
        }
    }
}