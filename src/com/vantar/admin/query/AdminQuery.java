package com.vantar.admin.query;

import com.vantar.admin.index.Admin;
import com.vantar.business.*;
import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.util.json.Json;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.ObjectUtil;
import com.vantar.web.*;
import com.vantar.web.query.QueryData;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminQuery {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_MENU_QUERY_TITLE, params, response, true);

        // if query is selected
        StoredQuery storedQuery = new StoredQuery();
        storedQuery.id = ui.params.getLong(VantarParam.ID);
        if (NumberUtil.isIdValid(storedQuery.id)) {
            try {
                storedQuery = ModelMongo.getById(storedQuery);
            } catch (VantarException e) {
                ui.addErrorMessage(e).finish();
                return;
            }
        }

        // set values from param
        String paramTitle = params.getString("t");
        if (paramTitle != null) {
            storedQuery.title = paramTitle;
        }
        String paramDto = params.getString("d");
        if (paramDto != null) {
            storedQuery.dtoName = paramDto;
        }
        String paramQ = params.getString("q");
        if (paramQ != null) {
            storedQuery.q = paramQ;
        }
        if (storedQuery.q == null) {
            storedQuery.q = "{\n" +
                "    \"lang\": \"en\",\n" +
                "    \"page\": 1,\n" +
                "    \"length\": 10,\n" +
                "    \"sort\": [\"id:desc\"],\n" +
                "    \"condition\": {\n" +
                "        \"operator\": \"AND\",\n" +
                "        \"items\": [\n" +
                "            {\"type\": \"EQUAL\", \"col\": \"id\", \"value\": 1}\n" +
                "        ]\n" +
                "    }\n" +
                "}\n";
        }

        // if store
        if (params.getString("write-q") != null) {
            try {
                if (storedQuery.id == null) {
                    ModelMongo.insert(new ModelCommon.Settings(storedQuery).logEvent(false).mutex(false));
                    ui.addMessage(VantarKey.INSERT_SUCCESS);
                } else {
                    ModelMongo.update(new ModelCommon.Settings(storedQuery).logEvent(false).mutex(false));
                    ui.addMessage(VantarKey.UPDATE_SUCCESS);
                }
            } catch (VantarException e) {
                ui.addErrorMessage(e);
            }
        }

        // if delete
        if (params.getString("delete-q") != null && storedQuery.id != null) {
            try {
                ModelMongo.delete(new ModelCommon.Settings(storedQuery).force(true).logEvent(false).mutex(false));
                ui.addMessage(VantarKey.DELETE_SUCCESS);
            } catch (VantarException e) {
                ui.addErrorMessage(e);
            }
            storedQuery = new StoredQuery();
        }

        // form
        ui.addHeading(2, VantarKey.ADMIN_QUERY_NEW);
        ui.addHrefBlock(VantarKey.ADMIN_NEW, "/admin/query/index");
        List<DtoDictionary.Info> dtoList = DtoDictionary.getAll();
        Map<String, String> dtos = new TreeMap<>();
        for (DtoDictionary.Info i : dtoList) {
            dtos.put(i.getDtoClassName(), i.getDtoClassName());
        }
        ui  .beginFormPost()
            .addInput(VantarKey.ADMIN_TITLE, "t", storedQuery.title)
            .addSelect("DTO", "d", dtos, false, storedQuery.dtoName)
            .addTextArea("Query JSON", "q", storedQuery.q, "large")
            .addHidden("id", storedQuery.id);
        StringBuilder sb = new StringBuilder(200);
        sb.append(ui.getSubmit("Run", "run-q", "run-q")).append(" ");
        if (storedQuery.id == null) {
            sb.append(ui.getSubmit("Insert", "write-q", "write-q"));
        } else {
            sb.append(ui.getSubmit("Update", "write-q", "write-q")).append(" ");
            sb.append(ui.getSubmit("Delete", "delete-q", "delete-q"));
        }
        ui.addWidgetRow("", "", sb.toString());
        ui.write();

        // list stored queries
        ui.addHeading(2, VantarKey.ADMIN_QUERY);
        QueryBuilder q = new QueryBuilder(new StoredQuery());
        q.sort("id:desc");
        try {
            ModelMongo.forEach(q, dto -> {
                StoredQuery query = (StoredQuery) dto;
                ui.addHrefBlock(query.title, "/admin/query/index?id=" + query.id);
            });
        } catch (NoContentException e) {
            ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
        } catch (VantarException e) {
            ui.addErrorMessage(e);
        }

        // run query
        if (params.getString("run-q") != null && storedQuery.dtoName != null && storedQuery.q != null) {
            ui.addHeading(2, "Result");

            DtoDictionary.Info dtoInfo = DtoDictionary.get(storedQuery.dtoName);
            if (dtoInfo == null) {
                ui.addErrorMessage("invalid DTO name!").finish();
                return;
            }

            QueryData qd = Json.d.fromJson(storedQuery.q, QueryData.class);
            if (qd == null) {
                ui.addErrorMessage("invalid query!").finish();
                return;
            }
            QueryBuilder qx = qd.getQueryBuilder(dtoInfo.getDtoInstance());
            if (qx == null) {
                ui.addErrorMessage("invalid query!").finish();
                return;
            }
            List<ValidationError> errors = qx.getErrors();
            if (errors != null) {
                ui.addErrorMessage(ValidationError.toString(errors)).finish();
                return;
            }

            PageData data = null;
            try {
                if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                    qx.condition().inspect();
                    data = ModelMongo.search(qx);
                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                    // todo
                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                    // todo
                }
            } catch (NoContentException ignore) {
                ui.addMessage(Locale.getString(VantarKey.NO_CONTENT));
            } catch (VantarException e) {
                ui.addErrorMessage(ObjectUtil.throwableToString(e));
            }
            if (data != null) {
                WebUi.DtoListOptions options = new WebUi.DtoListOptions();
                options.fields = qx.getDto().getProperties();
                ui.addDtoList(data, options);
            }
        }

        ui.finish();
    }
}
