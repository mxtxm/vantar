package com.vantar.admin.model;

import com.vantar.admin.Dto.QueryDictionary;
import com.vantar.business.CommonRepoMongo;
import com.vantar.common.VantarParam;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.nosql.elasticsearch.ElasticSearch;
import com.vantar.database.nosql.mongo.MongoSearch;
import com.vantar.database.query.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.util.object.ObjectUtil;
import com.vantar.web.*;
import org.slf4j.*;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


public class AdminQuery {

    private static final Logger log = LoggerFactory.getLogger(AdminQuery.class);
    private static final String PARAM_GROUP = "g";
    private static final String PARAM_TITLE = "t";
    private static final String PARAM_QUERY = "q";


    public static void index(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_MENU_QUERY_TITLE), params, response);
        if (ui == null) {
            return;
        }

        ui.addBlockLink(Locale.getString(VantarKey.ADMIN_QUERY_NEW), "/admin/query/write").addEmptyLine().addEmptyLine().addEmptyLine();

        QueryBuilder q = new QueryBuilder(new QueryDictionary());
        q.sort("group:asc");
        try {
            String group = null;
            List<QueryDictionary> items = CommonRepoMongo.getData(q);
            for (QueryDictionary item: items) {
                if (group == null || !group.equals(item.group)) {
                    group = item.group;
                    if (group == null) {
                        ui.containerEnd();
                    }
                    ui.beginBox(group);
                }
                ui.addBlockLink(item.title, "/admin/query/get?" + VantarParam.ID + "=" + item.id);
            }
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        } catch (NoContentException e) {
            ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
        }

        ui.finish();
    }

    public static void write(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_QUERY_WRITE), params, response);
        if (ui == null) {
            return;
        }

        QueryDictionary item = getQueryDictionary(ui, true);
        if (item == null) {
            item = new QueryDictionary();
        }

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addInput(Locale.getString(VantarKey.ADMIN_GROUP), PARAM_GROUP, item.group)
                .addInput(Locale.getString(VantarKey.ADMIN_TITLE), PARAM_TITLE, item.title)
                .addTextArea("Query JSON", PARAM_QUERY, item.q);

            if (item.id != null) {
                ui.addHidden(VantarParam.ID, item.id.toString());
            }

            ui  .addSubmit()
                .addEmptyLine()
                .addEmptyLine()
                .addBlockLinkNewPage(Locale.getString(VantarKey.ADMIN_HELP), "/admin/document/show?document=document--webservice--search.md")
                .finish();

            return;
        }

        item.group = params.getString(PARAM_GROUP);
        item.title = params.getString(PARAM_TITLE);
        item.q = params.getString(PARAM_QUERY);
        try {
            if (item.id == null) {
                CommonRepoMongo.insert(item);
            } else {
                CommonRepoMongo.update(item);
            }
            ui.addMessage(Locale.getString(VantarKey.INSERT_SUCCESS));
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }

    public static void delete(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_QUERY_DELETE_TITLE), params, response);
        if (ui == null) {
            return;
        }

        QueryDictionary item = getQueryDictionary(ui, false);
        if (item == null) {
            return;
        }

        if (!params.isChecked("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addHeading(3, item.group + " - " + item.title)
                .addTextArea("", "", "bpp"+item.q)
                .addCheckbox(Locale.getString(VantarKey.ADMIN_CONFIRM), WebUi.PARAM_CONFIRM);

            if (item.id != null) {
                ui.addHidden(VantarParam.ID, item.id.toString());
            }

            ui  .addSubmit()
                .finish();

            return;
        }

        try {
            CommonRepoMongo.delete(item);
            ui.addMessage(Locale.getString(VantarKey.DELETE_SUCCESS));
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }

    public static void get(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_QUERY), params, response);
        if (ui == null) {
            return;
        }

        QueryDictionary item = getQueryDictionary(ui, false);
        if (item == null) {
            return;
        }

        ui  .beginBox(item.group + " - " + item.title)
            .addLinks(
                Locale.getString(VantarKey.ADMIN_IX), "/admin/query/index",
                Locale.getString(VantarKey.ADMIN_NEW), "/admin/query/write",
                Locale.getString(VantarKey.ADMIN_UPDATE), "/admin/query/write?" + VantarParam.ID + "=" + item.id,
                Locale.getString(VantarKey.ADMIN_DELETE2), "/admin/query/delete?" + VantarParam.ID + "=" + item.id
            )
            .beginFormPost()
            .addTextArea("Query JSON", PARAM_QUERY, params.getString(PARAM_QUERY, item.q), "small")
            .addSubmit()
            .addEmptyLine()
            .addEmptyLine()
            .addBlockLinkNewPage(Locale.getString(VantarKey.ADMIN_HELP), "/admin/document/show?document=document--webservice--search.md")
            .containerEnd().containerEnd().write();

        if (params.isChecked("f")) {
            QueryBuilder q = null;
            try {
                q = new QueryBuilder(params.getQueryData(PARAM_QUERY));
            } catch (InputException e) {
                ui.write().addErrorMessage(e).finish();
                return;
            }

            DtoDictionary.Info dtoInfo = DtoDictionary.get(q.getDto().getClass().getSimpleName());
            if (dtoInfo == null) {
                return;
            }

            PageData data = null;
            try {
                if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                    try (SqlConnection connection = new SqlConnection()) {
                        SqlSearch search = new SqlSearch(connection);
                        data = search.getPage(q);
                    }
                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                    data = MongoSearch.getPage(q);
                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                    data = ElasticSearch.getPage(q);
                }
            } catch (NoContentException ignore) {
                ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
            } catch (DatabaseException e) {
                ui.addErrorMessage(ObjectUtil.throwableToString(e));
                log.error("! ", e);
            }

            if (data != null) {
                ui.addDtoList(data, true, q.getDtoResult().getProperties());
            }
        }

        ui.finish();
    }

    private static QueryDictionary getQueryDictionary(WebUi ui, boolean writeMode) {
        QueryDictionary item = new QueryDictionary();
        item.id = ui.params.getLong(VantarParam.ID);

        if (item.id == null) {
            if (!writeMode) {
                ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
            }
            return null;
        }

        try {
            return CommonRepoMongo.getFirst(item);
        } catch (DatabaseException | NoContentException e) {
            ui.addErrorMessage(e).finish();
            return null;
        }
    }
}