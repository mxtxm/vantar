package com.vantar.admin.database.dbms.status;

import com.vantar.admin.index.Admin;
import com.vantar.business.*;
import com.vantar.database.common.Db;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.service.Services;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminStatus {

    public static void statusMongo(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Db.Dbms.MONGO, params, response, true);

        if (!Services.isUp(Db.Dbms.MONGO)) {
            if (Services.isEnabled(Db.Dbms.MONGO)) {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ENABLED, Db.Dbms.MONGO));
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_DISABLED, Db.Dbms.MONGO));
            }
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, Db.Dbms.MONGO)).finish();
            return;
        }
        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ON, Db.Dbms.MONGO)).write();

        try {
            for (DtoDictionary.Info info : DtoDictionary.getAll(Db.Dbms.MONGO)) {
                ui.addKeyValue(
                    info.dtoClass.getSimpleName(),
                    Db.modelMongo.count(info.getDtoInstance().getStorage()) + " records"
                ).write();
            }
        } catch (VantarException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }


    public static void statusSql(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Db.Dbms.SQL, params, response, true);

        if (!Services.isUp(Db.Dbms.SQL)) {
            if (Services.isEnabled(Db.Dbms.SQL)) {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ENABLED, Db.Dbms.SQL));
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_DISABLED, Db.Dbms.SQL));
            }
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, Db.Dbms.SQL)).finish();
            return;
        }
        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ON, Db.Dbms.SQL)).write();

        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repo = new CommonRepoSql(connection);
            for (DtoDictionary.Info info : DtoDictionary.getAll(Db.Dbms.SQL)) {
                ui.addKeyValue(
                    info.dtoClass.getSimpleName(),
                    repo.count(info.getDtoInstance().getStorage()) + " records"
                ).write();
            }
        } catch (VantarException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }

    public static void statusElastic(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi("Elastic", params, response, true);

        if (!Services.isUp(Db.Dbms.ELASTIC)) {
            if (Services.isEnabled(Db.Dbms.ELASTIC)) {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ENABLED, "ElasticSearch"));
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_DISABLED, "ElasticSearch"));
            }
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch")).finish();;
            return;
        }
        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ON, "ElasticSearch")).write();

        try {
            for (DtoDictionary.Info info : DtoDictionary.getAll(Db.Dbms.ELASTIC)) {
                ui.addKeyValue(
                    info.dtoClass.getSimpleName(),
                    CommonRepoElastic.count(info.getDtoInstance().getStorage()) + " records"
                ).write();
            }
        } catch (VantarException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }

    /**
     * <DBMS, <table/collection, record-count>>
     */
    public static Map<String, Map<String, Long>> getDatabaseRecordCount() {
        Map<String, Map<String, Long>> databases = new HashMap<>(5, 1);

        if (Services.isUp(Db.Dbms.MONGO)) {
            Map<String, Long> db = new HashMap<>(200, 1);
            try {
                for (DtoDictionary.Info info : DtoDictionary.getAll(Db.Dbms.MONGO)) {
                    db.put(info.dtoClass.getSimpleName(), Db.mongo.count(info.getDtoInstance().getStorage()));
                }
            } catch (Exception ignore) {

            }
            databases.put(Db.Dbms.MONGO.name(), db);
        }

        if (Services.isUp(Db.Dbms.SQL)) {
            Map<String, Long> db = new HashMap<>(200, 1);
            try (SqlConnection connection = new SqlConnection()) {
                CommonRepoSql repo = new CommonRepoSql(connection);
                for (DtoDictionary.Info info : DtoDictionary.getAll(Db.Dbms.SQL)) {
                    db.put(info.dtoClass.getSimpleName(), repo.count(info.getDtoInstance().getStorage()));
                }
            } catch (Exception ignore) {

            }
            databases.put(Db.Dbms.SQL.name(), db);
        }

        if (Services.isUp(Db.Dbms.ELASTIC)) {
            Map<String, Long> db = new HashMap<>(200, 1);
            try {
                for (DtoDictionary.Info info : DtoDictionary.getAll(Db.Dbms.ELASTIC)) {
                    db.put(info.dtoClass.getSimpleName(), CommonRepoElastic.count(info.getDtoInstance().getStorage()));
                }
            } catch (Exception ignore) {

            }
            databases.put(Db.Dbms.ELASTIC.name(), db);
        }

        return databases;
    }
}