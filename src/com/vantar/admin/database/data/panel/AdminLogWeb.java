package com.vantar.admin.database.data.panel;

import com.vantar.business.ModelMongo;
import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticSearch;
import com.vantar.database.nosql.mongo.MongoQuery;
import com.vantar.database.query.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.log.ServiceLog;
import com.vantar.service.log.dto.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminLogWeb {

    /**
     * filter by user.id
     */
    public static void search(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDtoItem(VantarKey.ADMIN_WEB_LOG, "log-web-search", params, response, info);

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
        q.condition().equal("userId", u.dto.getId());

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
                        "/admin/data/log/action/search?type=d&dto=UserLog&id=" + dtoX.getId() + "&un=" + params.getString("un") + "&ufn=" + params.getString("unf"),
                        true,
                        false,
                        null
                    );
                    colOptions.add(action);
                }

                return colOptions;
            }
        };

        u.ui.beginBlock("div", "web-logs")
            .addDtoListWithHeader(data, info, options)
            .finish();
    }

    public static UserWebLog getData(Params params) throws VantarException {
        UserLog.Mini userLog = ModelMongo.getById(params, new UserLog.Mini());

        QueryBuilder q = new QueryBuilder(new UserWebLog());
        q.condition()
            .equal("timeDay", userLog.timeDay)
            .equal("threadId", userLog.threadId)
            .equal("userId", userLog.userId)
            .equal("url", userLog.url)
            .equal("objectId", userLog.objectId)
            .equal("action", "REQUEST");

        List<UserWebLog> data = ModelMongo.getData(q);
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