package com.vantar.admin.database.data.panel;

import com.vantar.admin.index.Admin;
import com.vantar.business.*;
import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.service.auth.CommonUser;
import com.vantar.service.log.dto.*;
import com.vantar.util.json.Json;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminDataUpdate {

    public static void update(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        WebUi ui = Admin.getUiDto(VantarKey.ADMIN_UPDATE, params, response, dtoInfo);
        if (dtoInfo == null) {
            return;
        }
        if (!DataUtil.isUp(dtoInfo.dbms, ui)) {
            ui.finish();
            return;
        }
        Dto dto = dtoInfo.getDtoInstance();
        DataUtil.Event event = DataUtil.getEvent();
        if (event != null) {
            Dto dtoX = event.dtoExchange(dto, "update");
            if (dtoX != null) {
                dto = dtoX;
            }
        }

        if (!params.contains("f")) {
            dto.setId(params.getLong(VantarParam.ID));
            try {
                if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                    dto = ModelMongo.getById(dto);
                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                    try (SqlConnection connection = new SqlConnection()) {
                        dto = (new CommonRepoSql(connection)).getById(dto);
                    }
                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                    dto = CommonRepoElastic.getById(dto);
                }

                boolean isLog = Log.class.equals(dto.getClass())
                    || UserWebLog.class.equals(dto.getClass())
                    || UserLog.class.equals(dto.getClass());
                if (!isLog) {
                    ui.addDtoItemLinks(dto, false, true);
                }
                ui.addDtoUpdateForm(dto, dto.getProperties());
            } catch (NoContentException e) {
                ui.addMessage(VantarKey.NO_CONTENT);
            } catch (VantarException e) {
                ui.addErrorMessage(e);
            }
            ui.finish();
            return;
        }

        if (event != null) {
            event.beforeUpdate(dto);
        }
        try {
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                ModelMongo.update(new ModelCommon.Settings(
                    params,
                    dto,
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

            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                CommonModelSql.updateJson(params.getString("asjson"), dto);
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                CommonModelElastic.updateJson(params.getString("asjson"), dto);
            }
            if (event != null) {
                event.afterUpdate(dto);
            }
            ui.addMessage(VantarKey.UPDATE_SUCCESS);

        } catch (VantarException e) {
            ui.addErrorMessage(e);
        }

        if (dtoInfo.broadcastMessage != null) {
            Services.messaging.broadcast(dtoInfo.broadcastMessage);
        }

        ui.finish();
    }
}