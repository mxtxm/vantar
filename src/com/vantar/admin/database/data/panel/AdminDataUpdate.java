package com.vantar.admin.database.data.panel;

import com.vantar.business.*;
import com.vantar.common.VantarParam;
import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.service.auth.CommonUser;
import com.vantar.util.json.Json;
import com.vantar.web.Params;
import javax.servlet.http.HttpServletResponse;


public class AdminDataUpdate {

    public static void update(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDtoItem(VantarKey.ADMIN_UPDATE, "update", params, response, info);

        DataUtil.Event event = DataUtil.getEvent();
        if (event != null) {
            Dto dtoX = event.dtoExchange(u.dto, "update");
            if (dtoX != null) {
                u.dto = dtoX;
            }
        }

        if (!params.contains("f")) {
            u.dto.setId(params.getLong(VantarParam.ID));
            try {
                if (info.dbms.equals(Db.Dbms.MONGO)) {
                    u.dto = Db.modelMongo.getById(u.dto);
                } else if (info.dbms.equals(Db.Dbms.SQL)) {
                    try (SqlConnection connection = new SqlConnection()) {
                        u.dto = (new CommonRepoSql(connection)).getById(u.dto);
                    }
                } else if (info.dbms.equals(Db.Dbms.ELASTIC)) {
                    u.dto = CommonRepoElastic.getById(u.dto);
                }

                u.ui.addDtoUpdateForm(u.dto, u.dto.getProperties());
            } catch (NoContentException e) {
                u.ui.addMessage(VantarKey.NO_CONTENT);
            } catch (VantarException e) {
                u.ui.addErrorMessage(e);
            }
            u.ui.finish();
            return;
        }

        if (event != null) {
            event.beforeUpdate(u.dto);
        }
        try {
            if (info.dbms.equals(Db.Dbms.MONGO)) {
                Db.modelMongo.update(new ModelCommon.Settings(params, u.dto)
                    .isJson("asjson")
                    .setEventAfterWrite(dto -> {
                        if (dto instanceof CommonUser) {
                            Db.modelMongo.insertPassword(
                                dto,
                                Json.d.extract(params.getString("asjson"), "password", String.class)
                            );
                        }
                    })
                );

            } else if (info.dbms.equals(Db.Dbms.SQL)) {
                CommonModelSql.updateJson(params.getString("asjson"), u.dto);
            } else if (info.dbms.equals(Db.Dbms.ELASTIC)) {
                CommonModelElastic.updateJson(params.getString("asjson"), u.dto);
            }
            if (event != null) {
                event.afterUpdate(u.dto);
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