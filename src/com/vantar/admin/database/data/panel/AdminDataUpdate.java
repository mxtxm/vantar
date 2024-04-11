package com.vantar.admin.database.data.panel;

import com.vantar.business.*;
import com.vantar.common.VantarParam;
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
                if (info.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                    u.dto = ModelMongo.getById(u.dto);
                } else if (info.dbms.equals(DtoDictionary.Dbms.SQL)) {
                    try (SqlConnection connection = new SqlConnection()) {
                        u.dto = (new CommonRepoSql(connection)).getById(u.dto);
                    }
                } else if (info.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
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
            if (info.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                ModelMongo.update(new ModelCommon.Settings(
                    params,
                    u.dto,
                    new ModelCommon.WriteEvent() {
                        @Override
                        public void beforeWrite(Dto dto) {

                        }

                        @Override
                        public void afterWrite(Dto dto) throws ServerException {
                            if (dto instanceof CommonUser) {
                                ModelCommon.insertPassword(
                                    dto,
                                    Json.d.extract(params.getString("asjson"), "password", String.class)
                                );
                            }
                        }
                    }
                ).isJson("asjson"));

            } else if (info.dbms.equals(DtoDictionary.Dbms.SQL)) {
                CommonModelSql.updateJson(params.getString("asjson"), u.dto);
            } else if (info.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                CommonModelElastic.updateJson(params.getString("asjson"), u.dto);
            }
            if (event != null) {
                event.afterUpdate(u.dto);
            }
            u.ui.addMessage(VantarKey.UPDATE_SUCCESS);

        } catch (VantarException e) {
            u.ui.addErrorMessage(e);
        }

        if (info.broadcastMessage != null) {
            Services.messaging.broadcast(info.broadcastMessage);
        }

        u.ui.finish();
    }
}