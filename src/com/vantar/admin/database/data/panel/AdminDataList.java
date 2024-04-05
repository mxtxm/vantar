package com.vantar.admin.database.data.panel;

import com.vantar.admin.index.Admin;
import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.*;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.query.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.log.ServiceLog;
import com.vantar.service.log.dto.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminDataList {

    public static void list(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        WebUi ui = Admin.getUiDto(VantarKey.ADMIN_DATA_LIST, params, response, dtoInfo);
        if (dtoInfo == null) {
            return;
        }
        if (!DataUtil.isUp(dtoInfo.dbms, ui)) {
            ui.finish();
            return;
        }
        Dto dto = dtoInfo.getDtoInstance();
        DataUtil.Event event = DataUtil.getEvent();
        if (event != null) {
            dto = event.dtoExchange(dto, "list");
        }

        // if serial is submitted
        Long newSerial = params.getLong("serial-i");
        if (newSerial != null) {
            try {
                Mongo.Sequence.set(dto.getStorage(), newSerial);
                ui.addMessage(VantarKey.UPDATE_SUCCESS);
            } catch (DatabaseException e) {
                ui.addErrorMessage(e);
            }
        }

        QueryBuilder q = params.getQueryBuilder("jsonsearch", dto);
        if (q == null) {
            q = new QueryBuilder(dto)
                .page(params.getInteger("page", 1), params.getInteger("page-length", DataUtil.N_PER_PAGE))
                .sort(params.getString("sort", VantarParam.ID) + ":" + params.getString("sortpos", "desc"));
        } else {
            q.setDto(dto);
            List<ValidationError> errors = q.getErrors();
            if (errors != null) {
                ui.write().addErrorMessage(ValidationError.toString(errors)).finish();
                return;
            }
        }

        long lastSerialId = 0;
        PageData data = null;
        try {
            // > > > MONGO
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                data = MongoQuery.getPage(q, null);
                lastSerialId = Mongo.Sequence.getCurrentValue(dto.getStorage());
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

        String dtoName = dtoInfo.dtoClass.getSimpleName();
        boolean showLog = Services.isUp(ServiceLog.class)
                && !Log.class.equals(dtoInfo.dtoClass)
                && !UserWebLog.class.equals(dtoInfo.dtoClass)
                && !UserLog.class.equals(dtoInfo.dtoClass);

        WebUi.DtoListOptions options = new WebUi.DtoListOptions();
        options.actionLink = true;
        options.archive = true;
        options.pagination = true;
        options.search = true;
        options.lastSerialId = lastSerialId;
        options.fields = q.getDto().getProperties();
        options.colOptionCount = showLog ? 3 : 2;
        options.checkListFormUrl = "/admin/data/delete/many";
        options.event = new WebUi.DtoListOptions.Event() {
            @Override
            public void checkListFormContent() {
                ui.addEmptyLine();
                ui.direction = "ltr".equalsIgnoreCase(ui.direction) ? "rtl" : "ltr";
                ui.alignKey = "left".equalsIgnoreCase(ui.alignKey) ? "right" : "left";
                ui.addCheckbox(VantarKey.SELECT_ALL, "delete-select-all");
                ui.addCheckbox(VantarKey.ADMIN_CONFIRM, "confirm-delete");
                ui.addCheckbox(VantarKey.ADMIN_DELETE_CASCADE, "cascade");
                ui.addCheckbox(VantarKey.ADMIN_IGNORE_DEPENDENCIES, "ignore-dependencies");
                ui.addSubmit(VantarKey.ADMIN_DELETE, "delete-button", "delete-button");
                ui.direction = "ltr".equalsIgnoreCase(ui.direction) ? "rtl" : "ltr";
                ui.alignKey = "left".equalsIgnoreCase(ui.alignKey) ? "right" : "left";
            }

            @Override
            public List<WebUi.DtoListOptions.ColOption> getColOptions(Dto dtoX) {
                List<WebUi.DtoListOptions.ColOption> colOptions = new ArrayList<>(3);

                WebUi.DtoListOptions.ColOption deleteCheckBox = new WebUi.DtoListOptions.ColOption();
                deleteCheckBox.containerClass = "delete-option";
                deleteCheckBox.content = ui.getCheckbox("delete-check", false, dtoX.getId(), "delete-check", false);
                colOptions.add(deleteCheckBox);

                WebUi.DtoListOptions.ColOption update = new WebUi.DtoListOptions.ColOption();
                update.content = ui.getHref(
                    VantarKey.ADMIN_EDIT,
                    "/admin/data/update?dto=" + dtoName + "&id=" + dtoX.getId(), true, false, null
                );
                colOptions.add(update);

                if (showLog) {
                    WebUi.DtoListOptions.ColOption actionLog = new WebUi.DtoListOptions.ColOption();
                    actionLog.content = ui.getHref(
                        VantarKey.ADMIN_ACTION_LOG,
                        "/admin/data/log/action/search?dto=" + dtoName + "&id=" + dtoX.getId(), true, false, null
                    );
                    colOptions.add(actionLog);
                }

                return colOptions;
            }
        };

        ui.addDtoListWithHeader(data, dtoInfo, options);
        ui.finish();
    }
}