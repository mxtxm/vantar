package com.vantar.admin.database.data.panel;

import com.vantar.admin.index.Admin;
import com.vantar.business.ModelMongo;
import com.vantar.database.dto.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.util.json.Json;
import com.vantar.util.object.ObjectUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminDataView {

    public static void view(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_VIEW), params, response, dtoInfo);
        Dto dto = dtoInfo.getDtoInstance();
        dto.setId(params.getLong("id"));
        if (dto.getId() == null) {
            return;
        }

        try {
            // > > > MONGO
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                dto = ModelMongo.getById(dto);
                // > > > SQL
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                try (SqlConnection connection = new SqlConnection()) {
                    SqlSearch search = new SqlSearch(connection);
                }
                //todo
                // > > > ELASTIC
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                //todo
            }

        } catch (NoContentException ignore) {

        } catch (VantarException e) {
            ui.addErrorMessage(e);
        }

        ui.addDtoItemLinks(dto, true, false);
        ui  .addEmptyLine()
            .addBlock();

        for (Map.Entry<String, Object> property : dto.getPropertyValuesIncludeNulls().entrySet()) {
            String name = property.getKey();
            Object value = property.getValue();
            Class<?> type = dto.getPropertyType(name);

            ui.addHeading(3, name + " (" + type.getSimpleName() + ")");

            if (value == null) {
                ui.addBlock("pre", "NULL", "view-pre").write();
            } else if (value instanceof Collection || value instanceof Map || value instanceof Dto) {
                ui.addBlock("pre", Json.d.toJsonPretty(Json.d.toJson(value)), "view-pre").write();
            } else {
                ui.addBlock("pre", ObjectUtil.toString(value), "view-pre").write();
            }
        }

        ui.finish();
    }
}