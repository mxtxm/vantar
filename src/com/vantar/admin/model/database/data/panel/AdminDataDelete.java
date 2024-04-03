package com.vantar.admin.model.database.data.panel;

import com.vantar.admin.model.index.Admin;
import com.vantar.business.*;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.nosql.mongo.MongoConnection;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.log.ServiceLog;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminDataDelete {

    public static void purge(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        if (dtoInfo == null) {
            return;
        }
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_DATA_PURGE), params, response, dtoInfo);
        Dto dto = dtoInfo.getDtoInstance();

        if (!params.isChecked("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addErrorMessage(VantarKey.ADMIN_DELETE)
                .addCheckbox(VantarKey.ADMIN_DELETE_ALL_CONFIRM, WebUi.PARAM_CONFIRM)
                .addSubmit(VantarKey.ADMIN_DELETE)
                .blockEnd();
        } else {
            try {
                if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                    if (MongoConnection.isUp()) {
                        ui.addMessage(ModelMongo.purge(dto).message);
                    } else {
                        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, DtoDictionary.Dbms.MONGO));
                    }
                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                    if (SqlConnection.isUp()) {
                        ui.addMessage(CommonModelSql.purgeData(dto.getStorage()).message);
                    } else {
                        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, DtoDictionary.Dbms.SQL));
                    }
                } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                    if (ElasticConnection.isUp()) {
                        ui.addMessage(CommonModelElastic.purgeData(dto.getStorage()).message);
                    } else {
                        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch"));
                    }
                }
            } catch (VantarException e) {
                ui.addErrorMessage(e);
                ServiceLog.log.error("! {}", dto, e);
            }

            if (dtoInfo.broadcastMessage != null) {
                Services.messaging.broadcast(dtoInfo.broadcastMessage);
            }
        }

        ui.finish();
    }

    public static void deleteMany(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        WebUi ui = Admin.getUiDto(VantarKey.ADMIN_DELETE, params, response, dtoInfo);
        if (dtoInfo == null) {
            return;
        }
        if (!params.isChecked("confirm-delete")) {
            ui.addMessage(VantarKey.DELETE_FAIL).finish();
            return;
        }
        if (!DataUtil.isUp(dtoInfo.dbms, ui)) {
            ui.finish();
            return;
        }
        Dto dto = dtoInfo.getDtoInstance();
        DataUtil.Event event = DataUtil.getEvent();
        if (event != null) {
            Dto dtoX = event.dtoExchange(dto, "delete");
            if (dtoX != null) {
                dto = dtoX;
            }
        }

        boolean ignoreDependencies = params.isChecked("ignore-dependencies");
        boolean cascade = params.isChecked("cascade");

        for (Long id : params.getLongList("delete-check")) {
            dto.reset();
            dto.setId(id);
            delete(
                ui,
                dtoInfo,
                dto,
                event,
                ignoreDependencies,
                cascade
            );
        }

        ui.finish();
    }

    public static void deleteOne(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        WebUi ui = Admin.getUiDto(VantarKey.ADMIN_DELETE, params, response, dtoInfo);
        if (dtoInfo == null) {
            return;
        }
        long id;
        try {
            id = params.getLongRequired("id");
        } catch (InputException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        if (!params.contains("f")) {
            ui  .addEmptyLine()
                .beginFormPost()
                .addCheckbox(VantarKey.ADMIN_DELETE_CASCADE, "cascade")
                .addCheckbox(VantarKey.ADMIN_IGNORE_DEPENDENCIES, "ignore-dependencies")
                .addCheckbox(VantarKey.ADMIN_CONFIRM, "confirm")
                .addHidden("dto", dtoInfo.dtoClass.getSimpleName())
                .addHidden("id", id)
                .addSubmit()
                .finish();
            return;
        }

        if (!params.isChecked("confirm-delete")) {
            ui.addMessage(VantarKey.DELETE_FAIL).finish();
            return;
        }
        if (!DataUtil.isUp(dtoInfo.dbms, ui)) {
            ui.finish();
            return;
        }
        Dto dto = dtoInfo.getDtoInstance();
        DataUtil.Event event = DataUtil.getEvent();
        if (event != null) {
            Dto dtoX = event.dtoExchange(dto, "delete");
            if (dtoX != null) {
                dto = dtoX;
            }
        }

        boolean ignoreDependencies = params.isChecked("ignore-dependencies");
        boolean cascade = params.isChecked("cascade");
        delete(
            ui,
            dtoInfo,
            dto,
            event,
            ignoreDependencies,
            cascade
        );
        ui.finish();
    }

    private static void delete(
        WebUi ui,
        DtoDictionary.Info dtoInfo,
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
            if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
                // > > > MONGO
                ResponseMessage resp = ModelMongo.delete(settings);
                ui.addMessage(resp.message);
                ui.addMessage("Deleted: " + resp.value + "records");
                // > > > SQL
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
                //ui.addMessage(CommonModelSql.deleteBatch(params, dto.getClass()).message);
                // > > > ELASTIC
            } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
                //ui.addMessage(CommonModelElastic.deleteBatch(params, dto.getClass()).message);
            }

            if (event != null) {
                event.afterDelete(dto);
            }
        } catch (VantarException e) {
            ui.addErrorMessage(e);
        }

        if (dtoInfo.broadcastMessage != null) {
            Services.messaging.broadcast(dtoInfo.broadcastMessage);
        }
    }
}