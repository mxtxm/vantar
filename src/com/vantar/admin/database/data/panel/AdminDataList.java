package com.vantar.admin.database.data.panel;

import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticSearch;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.query.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.service.auth.CommonUser;
import com.vantar.service.log.ServiceLog;
import com.vantar.service.log.dto.*;
import com.vantar.util.object.ClassUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminDataList {

    public static void list(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDto(VantarKey.ADMIN_DATA_LIST, "list", params, response, info);

        DataUtil.Event event = DataUtil.getEvent();
        if (event != null) {
            u.dto = event.dtoExchange(u.dto, "list");
        }

        // if serial is submitted
        Long newSerial = params.getLong("serial-i");
        if (newSerial != null) {
            try {
                Mongo.Sequence.set(u.dto.getStorage(), newSerial);
                u.ui.addMessage(VantarKey.UPDATE_SUCCESS);
            } catch (DatabaseException e) {
                u.ui.addErrorMessage(e);
            }
        }

        QueryBuilder q = params.getQueryBuilder("jsonsearch", u.dto);
        if (q == null) {
            q = new QueryBuilder(u.dto)
                .page(params.getInteger("page", 1), params.getInteger("page-length", DataUtil.N_PER_PAGE))
                .sort(params.getString("sort", VantarParam.ID) + ":" + params.getString("sortpos", "desc"));
        } else {
            q.setDto(u.dto);
            List<ValidationError> errors = q.getErrors();
            if (errors != null) {
                u.ui.write().addErrorMessage(ValidationError.toString(errors)).finish();
                return;
            }
        }

        long lastSerialId = 0;
        PageData data = null;
        try {
            // > > > MONGO
            if (info.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                data = MongoQuery.getPage(q, null);
                lastSerialId = Mongo.Sequence.getCurrentValue(u.dto.getStorage());
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

        boolean isUser = ClassUtil.implementsInterface(info.dtoClass, CommonUser.class);
        String dtoName = info.dtoClass.getSimpleName();
        boolean showLog = Services.isUp(ServiceLog.class)
            && !Log.class.equals(info.dtoClass)
            && !UserWebLog.class.equals(info.dtoClass)
            && !UserLog.class.equals(info.dtoClass);

        WebUi.DtoListOptions options = new WebUi.DtoListOptions();
        options.archive = true;
        options.pagination = true;
        options.search = true;
        options.lastSerialId = lastSerialId;
        options.fields = q.getDto().getProperties();
        options.checkListFormUrl = "/admin/data/delete/many";
        options.colOptionCount = showLog ? 3 : 2;
        if (isUser) {
            ++options.colOptionCount;
        }
        options.event = new WebUi.DtoListOptions.Event() {
            @Override
            public void checkListFormContent() {
                u.ui.addEmptyLine();
                u.ui.direction = "ltr".equalsIgnoreCase(u.ui.direction) ? "rtl" : "ltr";
                u.ui.alignKey = "left".equalsIgnoreCase(u.ui.alignKey) ? "right" : "left";
                u.ui.addCheckbox(VantarKey.SELECT_ALL, "delete-select-all");
                u.ui.addCheckbox(VantarKey.ADMIN_CONFIRM, "confirm-delete");
                u.ui.addCheckbox(VantarKey.ADMIN_DELETE_CASCADE, "cascade");
                u.ui.addCheckbox(VantarKey.ADMIN_IGNORE_DEPENDENCIES, "ignore-dependencies");
                u.ui.addSubmit(VantarKey.ADMIN_DELETE, "delete-button", "delete-button");
                u.ui.direction = "ltr".equalsIgnoreCase(u.ui.direction) ? "rtl" : "ltr";
                u.ui.alignKey = "left".equalsIgnoreCase(u.ui.alignKey) ? "right" : "left";
            }

            @Override
            public List<WebUi.DtoListOptions.ColOption> getColOptions(Dto dtoX) {
                String userParams;
                if (isUser) {
                    CommonUser user = (CommonUser) dtoX;
                    userParams = "&un=" + user.getUsername() + "&ufn=" + user.getFullName();
                } else {
                    userParams = "";
                }
                List<WebUi.DtoListOptions.ColOption> colOptions = new ArrayList<>(3);

                WebUi.DtoListOptions.ColOption deleteCheckBox = new WebUi.DtoListOptions.ColOption();
                deleteCheckBox.containerClass = "delete-option";
                deleteCheckBox.content = u.ui.getCheckbox("delete-check", false, dtoX.getId() + userParams, "delete-check", false);
                colOptions.add(deleteCheckBox);

                WebUi.DtoListOptions.ColOption update = new WebUi.DtoListOptions.ColOption();
                update.content = u.ui.getHref(
                    VantarKey.ADMIN_EDIT,
                    "/admin/data/update?dto=" + dtoName + "&id=" + dtoX.getId() + userParams, true, false, null
                );
                colOptions.add(update);

                if (isUser) {
                    WebUi.DtoListOptions.ColOption actionLog = new WebUi.DtoListOptions.ColOption();
                    actionLog.content = u.ui.getHref(
                        VantarKey.ADMIN_LIST_OPTION_USER_ACTIVITY,
                        "/admin/data/log/action/search?type=c&dto=" + dtoName + "&id=" + dtoX.getId() + userParams, true, false, null
                    );
                    colOptions.add(actionLog);
                }

                if (showLog) {
                    WebUi.DtoListOptions.ColOption actionLog = new WebUi.DtoListOptions.ColOption();
                    actionLog.content = u.ui.getHref(
                        VantarKey.ADMIN_LIST_OPTION_ACTION_LOG,
                        "/admin/data/log/action/search?type=b&dto=" + dtoName + "&id=" + dtoX.getId() + userParams, true, false, null
                    );
                    colOptions.add(actionLog);
                }

                return colOptions;
            }
        };

        u.ui.addDtoListWithHeader(data, info, options);
        u.ui.finish();
    }
}