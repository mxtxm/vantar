package com.vantar.admin.database.data.panel;

import com.vantar.common.VantarParam;
import com.vantar.database.common.*;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticSearch;
import com.vantar.database.query.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.log.dto.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminLogWeb {

    /**
     * type a filter by user.id
     * type b filter by log-action.id
     */
    public static void search(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        String type = params.getString("type", "a");
        boolean isTypeA = "a".equals(type);
        boolean isTypeB = "b".equals(type);
        DataUtil.Ui u = DataUtil.initDtoItem(VantarKey.ADMIN_LOG_WEB, "log-web", params, response, info);

        QueryBuilder q = params.getQueryBuilder("jsonsearch", new UserWebLog.Mini());
        if (q == null) {
            q = new QueryBuilder(new UserWebLog.Mini())
                .page(params.getInteger("page", 1), params.getInteger("page-length", DataUtil.N_PER_PAGE))
                .sort(params.getString("sort", VantarParam.ID) + ":" + params.getString("sortpos", "desc"));
        } else {
            List<ValidationError> errors = q.getErrors();
            if (errors != null) {
                u.ui.write().addErrorMessage(ValidationError.toString(errors)).finish();
                return;
            }
        }
        if (isTypeA) {
            q.condition().equal("userId", u.dto.getId());
        } else {
            UserLog.Mini userLog;
            try {
                userLog = Db.modelMongo.getById(params, new UserLog.Mini());
            } catch (VantarException e) {
                u.ui.addErrorMessage(e).finish();
                return;
            }

            q.condition()
                .equal("threadId", userLog.threadId)
                .equal("action", "REQUEST");
            if (params.getBoolean("old", false)) {
                q.condition()
                    .equal("timeDay", userLog.timeDay)
                    .equal("userId", userLog.userId)
                    .equal("url", userLog.url)
                    .equal("objectId", userLog.objectId);
            }
        }

        PageData data = null;
        try {
            // > > > MONGO
            if (info.dbms.equals(Db.Dbms.MONGO)) {
                data = Db.mongo.getPage(q, null);
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
        options.fields =
            new String[] {"id", "action", "time", "classNameSimple", "objectId", "url", "requestType", "status", "ip", "threadId",};
        options.colOptionCount = 1;
        options.event = new WebUi.DtoListOptions.Event() {
            @Override
            public void checkListFormContent() {

            }

            @Override
            public List<WebUi.DtoListOptions.ColOption> getColOptions(Dto dtoX) {
                UserWebLog.Mini log = (UserWebLog.Mini) dtoX;

                List<WebUi.DtoListOptions.ColOption> colOptions = new ArrayList<>(3);
                if ("REQUEST".equalsIgnoreCase(log.action)) {
                    WebUi.DtoListOptions.ColOption action = new WebUi.DtoListOptions.ColOption();
                    action.content = u.ui.getHref(
                        VantarKey.ADMIN_LIST_OPTION_USER_ACTIVITY,
                        "/admin/data/log/action/search?type=d&dto=UserLog&id=" + dtoX.getId()
                            + "&un=" + params.getString("un") + "&ufn=" + params.getString("ufn"),
                        true,
                        false,
                        null
                    );
                    colOptions.add(action);
                }

                WebUi.DtoListOptions.ColOption view = new WebUi.DtoListOptions.ColOption();
                view.content = u.ui.getHref(
                    VantarKey.ADMIN_VIEW,
                    "/admin/data/view?dto=UserWebLog&id=" + dtoX.getId(), true, false, null
                );
                colOptions.add(view);

                return colOptions;
            }
        };

        u.ui.beginBlock("div", "web-logs")
            .addDtoListWithHeader(data, info, options)
            .finish();
    }

    public static UserWebLog getData(Params params) throws VantarException {
        UserLog.Mini userLog = Db.modelMongo.getById(params, new UserLog.Mini());

        QueryBuilder q = new QueryBuilder(new UserWebLog());

        q.condition()
            .equal("action", "REQUEST")
            .equal("threadId", userLog.threadId);
        if (params.getBoolean("old", false)) {
            q.condition()
                .equal("timeDay", userLog.timeDay)
                .equal("userId", userLog.userId)
                .equal("url", userLog.url)
                .equal("objectId", userLog.objectId);
        }

        List<UserWebLog> data = Db.modelMongo.getData(q);
        if (data.size() == 1) {
            return data.get(0);
        }
        TreeMap<Long, UserWebLog> sortByTime = new TreeMap<>();
        for (UserWebLog d : data) {
            sortByTime.put(d.time.diffSeconds(userLog.time), d);
        }
        return sortByTime.firstEntry().getValue();
    }
}