package com.vantar.admin.database.data.panel;

import com.vantar.business.*;
import com.vantar.common.VantarParam;
import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.util.json.Json;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.ClassUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminDataInsert {

    public static void insert(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDto(VantarKey.ADMIN_INSERT, "insert", params, response, info);

        DataUtil.Event event = DataUtil.getEvent();
        if (event != null) {
            Dto dtoX = event.dtoExchange(u.dto, "insert");
            if (dtoX != null) {
                u.dto = dtoX;
            }
        }

        if (!params.contains("f")) {
            u.ui.addDtoAddForm(u.dto, u.dto.getProperties());
            u.ui.finish();
            return;
        }

        if (NumberUtil.isIdInvalid(u.dto.getId())) {
            u.dto.autoIncrementOnInsert(false);
        }
        if (event != null) {
            event.beforeInsert(u.dto);
        }
        try {
            if (info.dbms.equals(Db.Dbms.MONGO)) {
                Db.modelMongo.insert(new ModelCommon.Settings(params, u.dto)
                    .isJson("asjson")
                    .setEventAfterWrite(dto -> {
                        if (dto instanceof CommonUser) {
                            Db.modelMongo.insertPassword(
                                dto,
                                Json.d.extract(params.getString("asjson"), VantarParam.PASSWORD_FORM, String.class)
                            );
                        }
                    })
                );

            } else if (info.dbms.equals(Db.Dbms.SQL)) {
                CommonModelSql.insert(params, u.dto);
            } else if (info.dbms.equals(Db.Dbms.ELASTIC)) {
                CommonModelElastic.insert(params, u.dto);
            }
            if (event != null) {
                event.afterInsert(u.dto);
            }
            u.ui.addMessage(VantarKey.SUCCESS_INSERT);

        } catch (VantarException e) {
            u.ui.addErrorMessage(e);
        }

        if (info.broadcastMessage != null) {
            Services.messaging.broadcast(info.broadcastMessage);
        }

        u.ui.finish();
    }
}