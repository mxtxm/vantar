package com.vantar.admin.database.data.panel;

import com.vantar.business.*;
import com.vantar.database.common.Db;
import com.vantar.database.dto.*;

import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.log.ServiceLog;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminDataDelete {

    public static void purge(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDto(VantarKey.ADMIN_DATA_PURGE, "purge", params, response, info);

        if (!params.contains("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            u.ui.beginFormPost()
                .addErrorMessage(VantarKey.ADMIN_DELETE)
                .addCheckbox(VantarKey.ADMIN_DELETE_ALL_CONFIRM, WebUi.PARAM_CONFIRM)
                .addSubmit(VantarKey.ADMIN_DELETE)
                .blockEnd();
        } else {
            try {
                if (info.dbms.equals(Db.Dbms.MONGO)) {
                    if (Services.isUp(Db.Dbms.MONGO)) {
                        u.ui.addMessage(Db.modelMongo.purge(u.dto).message);
                    } else {
                        u.ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, Db.Dbms.MONGO));
                    }
                } else if (info.dbms.equals(Db.Dbms.SQL)) {
                    if (Services.isUp(Db.Dbms.SQL)) {
                        u.ui.addMessage(CommonModelSql.purgeData(u.dto.getStorage()).message);
                    } else {
                        u.ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, Db.Dbms.SQL));
                    }
                } else if (info.dbms.equals(Db.Dbms.ELASTIC)) {
                    if (Services.isUp(Db.Dbms.ELASTIC)) {
                        u.ui.addMessage(CommonModelElastic.purgeData(u.dto.getStorage()).message);
                    } else {
                        u.ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                    }
                }
            } catch (VantarException e) {
                u.ui.addErrorMessage(e);
                ServiceLog.log.error("! {}", u.dto, e);
            }

            if (info.broadcastMessage != null) {
                Services.messaging.broadcast(info.broadcastMessage);
            }
        }

        u.ui.finish();
    }

    public static void deleteMany(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDto(VantarKey.ADMIN_DELETE, "delete", params, response, info);
        
        if (!params.isChecked("confirm-delete")) {
            u.ui.addMessage(VantarKey.FAIL_DELETE).finish();
            return;
        }
        DataUtil.Event event = DataUtil.getEvent();
        if (event != null) {
            Dto dtoX = event.dtoExchange(u.dto, "delete");
            if (dtoX != null) {
                u.dto = dtoX;
            }
        }

        boolean ignoreDependencies = params.isChecked("ignore-dependencies");
        boolean cascade = params.isChecked("cascade");

        for (Long id : params.getLongList("delete-check")) {
            u.dto.reset();
            u.dto.setId(id);
            delete(
                u.ui,
                info,
                u.dto,
                event,
                ignoreDependencies,
                cascade
            );
        }

        u.ui.finish();
    }

    public static void deleteOne(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDtoItem(VantarKey.ADMIN_DELETE, "delete", params, response, info);

        if (!params.contains("f")) {
            u.ui.addEmptyLine()
                .beginFormPost()
                .addCheckbox(VantarKey.ADMIN_DELETE_CASCADE, "cascade")
                .addCheckbox(VantarKey.ADMIN_DELETE_IGNORE_DEPENDENCIES, "ignore-dependencies")
                .addCheckbox(VantarKey.ADMIN_CONFIRM, "confirm")
                .addHidden("dto", info.dtoClass.getSimpleName())
                .addHidden("id", u.dto.getId())
                .addSubmit()
                .finish();
            return;
        }

        if (!params.isChecked("confirm-delete")) {
            u.ui.addMessage(VantarKey.FAIL_DELETE).finish();
            return;
        }
        if (!DataUtil.isUp(u.ui, info.dbms)) {
            u.ui.finish();
            return;
        }
        DataUtil.Event event = DataUtil.getEvent();
        if (event != null) {
            Dto dtoX = event.dtoExchange(u.dto, "delete");
            if (dtoX != null) {
                u.dto = dtoX;
            }
        }

        boolean ignoreDependencies = params.isChecked("ignore-dependencies");
        boolean cascade = params.isChecked("cascade");
        delete(
            u.ui,
            info,
            u.dto,
            event,
            ignoreDependencies,
            cascade
        );
        u.ui.finish();
    }

    private static void delete(
        WebUi ui,
        DtoDictionary.Info info,
        Dto dto,
        DataUtil.Event event,
        boolean force,
        boolean cascade) {

        if (event != null) {
            event.beforeDelete(dto);
        }

        ModelCommon.Settings settings = new ModelCommon.Settings(dto)
            .cascade(cascade)
            .force(force)
            .logEvent(true)
            .mutex(true);

        try {
            if (info.dbms.equals(Db.Dbms.MONGO)) {
                // > > > MONGO
                ResponseMessage resp = Db.modelMongo.delete(settings);
                ui.addMessage(resp.message);
                ui.addMessage("Deleted: " + resp.value + "records");
                // > > > SQL
            } else if (info.dbms.equals(Db.Dbms.SQL)) {
                //ui.addMessage(CommonModelSql.deleteBatch(params, dto.getClass()).message);
                // > > > ELASTIC
            } else if (info.dbms.equals(Db.Dbms.ELASTIC)) {
                //ui.addMessage(CommonModelElastic.deleteBatch(params, dto.getClass()).message);
            }

            if (event != null) {
                event.afterDelete(dto);
            }
        } catch (VantarException e) {
            ui.addErrorMessage(e);
        }

        if (info.broadcastMessage != null) {
            Services.messaging.broadcast(info.broadcastMessage);
        }
    }
}