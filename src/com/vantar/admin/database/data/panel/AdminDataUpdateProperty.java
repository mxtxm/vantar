package com.vantar.admin.database.data.panel;

import com.vantar.business.*;
import com.vantar.database.dto.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.util.json.Json;
import com.vantar.util.object.ClassUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.Params;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.*;


public class AdminDataUpdateProperty {

    public static void update(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDtoItem(VantarKey.ADMIN_UPDATE, "update", params, response, info);

        String property = params.getString("p");
        String value = params.getString("v");
        String nulls = params.getString("n");

        if (!params.contains("f")) {
            u.ui.beginFormPost()
                .addInput("Dto", "dto", u.dto.getClass().getSimpleName())
                .addInput("id", "id", u.dto.getId())
                .addInput("property", "p", property)
                .addTextArea("value", "v", value)
                .addTextArea("nulls", "n", nulls)
                .addSubmit();
            u.ui.finish();
            return;
        }

        try {
            if (info.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                u.dto = ModelMongo.getById(params, u.dto);
                Field f = u.dto.getField(property);
                if (f == null) {
                    u.ui.addErrorMessage("Property not found").finish();
                    return;
                }
                Class<?> type = f.getType();
                if (nulls != null) {
                    u.dto.setNullProperties(StringUtil.splitTrim(nulls, ','));
                }

                if (ClassUtil.isInstantiable(type, List.class)) {
                    u.dto.setPropertyValue(property, Json.d.listFromJson(value, u.dto.getPropertyGenericTypes(f)[0]));
                } else if (ClassUtil.isInstantiable(type, Set.class)) {
                    u.dto.setPropertyValue(property, Json.d.setFromJson(value, u.dto.getPropertyGenericTypes(f)[0]));
                } else if (ClassUtil.isInstantiable(type, Map.class)) {
                    Class<?>[] g = u.dto.getPropertyGenericTypes(f);
                    u.dto.setPropertyValue(property, Json.d.mapFromJson(value, g[0], g[1]));
                } else if (ClassUtil.isInstantiable(type, Dto.class)) {
                    u.dto.setPropertyValue(property, Json.d.fromJson(value, type));
                } else {
                    u.dto.setPropertyValue(property, value);
                }

                ModelMongo.update(new ModelCommon.Settings(u.dto));

            } else if (info.dbms.equals(DtoDictionary.Dbms.SQL)) {

            } else if (info.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {

            }
            u.ui.addMessage(VantarKey.SUCCESS_UPDATE);

        } catch (VantarException e) {
            u.ui.addErrorMessage(e);
        }

        if (info.broadcastMessage != null) {
            Services.messaging.broadcast(info.broadcastMessage);
        }

        u.ui.finish();
    }
}