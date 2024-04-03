package com.vantar.admin.model.database.data.panel;

import com.vantar.admin.model.index.Admin;
import com.vantar.business.*;
import com.vantar.database.dto.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.service.auth.CommonUser;
import com.vantar.util.json.Json;
import com.vantar.util.number.NumberUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminDataInsert {

    public static void insert(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        WebUi ui = Admin.getUiDto(VantarKey.ADMIN_INSERT, params, response, dtoInfo);
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
            Dto dtoX = event.dtoExchange(dto, "insert");
            if (dtoX != null) {
                dto = dtoX;
            }
        }

        if (!params.isChecked("f")) {
            ui.addDtoAddForm(dto, dto.getProperties());
            ui.finish();
            return;
        }

        if (NumberUtil.isIdInvalid(dto.getId())) {
            dto.setClearIdOnInsert(false);
        }
        if (event != null) {
            event.beforeInsert(dto);
        }
        try {
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                ModelMongo.insert(new ModelCommon.Settings(
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
                CommonModelSql.insert(params, dto);
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                CommonModelElastic.insert(params, dto);
            }
            if (event != null) {
                event.afterInsert(dto);
            }
            ui.addMessage(VantarKey.INSERT_SUCCESS);

        } catch (VantarException e) {
            ui.addErrorMessage(e);
        }

        if (dtoInfo.broadcastMessage != null) {
            Services.messaging.broadcast(dtoInfo.broadcastMessage);
        }

        ui.finish();
    }
}