package com.vantar.admin.database.data.panel;

import com.vantar.business.ModelMongo;
import com.vantar.database.dto.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.util.json.Json;
import com.vantar.util.object.ObjectUtil;
import com.vantar.web.Params;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminDataView {

    public static void view(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDtoItem(VantarKey.ADMIN_VIEW, "view", params, response, info);

        try {
            // > > > MONGO
            if (info.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                u.dto = ModelMongo.getById(u.dto);
                // > > > SQL
            } else if (info.dbms.equals(DtoDictionary.Dbms.SQL)) {
                try (SqlConnection connection = new SqlConnection()) {
                    SqlSearch search = new SqlSearch(connection);
                }
                //todo
                // > > > ELASTIC
            } else if (info.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                //todo
            }

        } catch (NoContentException ignore) {

        } catch (VantarException e) {
            u.ui.addErrorMessage(e);
        }

        u.ui.addBlock();
        for (Map.Entry<String, Object> property : u.dto.getPropertyValuesIncludeNulls().entrySet()) {
            String name = property.getKey();
            Object value = property.getValue();
            Class<?> type = u.dto.getPropertyType(name);

            String vHtml;
            if (value == null) {
                vHtml = "NULL";
            } else if (value instanceof Collection || value instanceof Map || value instanceof Dto) {
                vHtml = u.ui.getBlock("pre", Json.d.toJsonPretty(Json.d.toJson(value)), "view-pre");
            } else {
                vHtml = u.ui.getBlock("pre", ObjectUtil.toString(value), "view-pre");
            }
            u.ui.addKeyValue(name + " (" + type.getSimpleName() + ")", vHtml, null, false).write();
        }
        u.ui.finish();
    }
}