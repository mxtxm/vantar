package com.vantar.admin.model.database.dbms.status;

import com.vantar.admin.model.index.Admin;
import com.vantar.business.*;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminStatus {

    public static void statusMongo(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(DtoDictionary.Dbms.MONGO, params, response, true);

        if (!MongoConnection.isUp()) {
            if (MongoConnection.isEnabled()) {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ENABLED, DtoDictionary.Dbms.MONGO));
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_DISABLED, DtoDictionary.Dbms.MONGO));
            }
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, DtoDictionary.Dbms.MONGO)).finish();
            return;
        }
        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ON, DtoDictionary.Dbms.MONGO)).write();

        try {
            for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
                ui.addKeyValue(
                    info.dtoClass.getSimpleName(),
                    MongoQuery.count(info.getDtoInstance().getStorage()) + " records"
                ).write();
            }
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }


    public static void statusSql(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(DtoDictionary.Dbms.SQL, params, response, true);

        if (!SqlConnection.isUp()) {
            if (SqlConnection.isEnabled()) {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ENABLED, DtoDictionary.Dbms.SQL));
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_DISABLED, DtoDictionary.Dbms.SQL));
            }
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, DtoDictionary.Dbms.SQL)).finish();
            return;
        }
        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ON, DtoDictionary.Dbms.SQL)).write();

        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repo = new CommonRepoSql(connection);
            for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.SQL)) {
                ui.addKeyValue(
                    info.dtoClass.getSimpleName(),
                    repo.count(info.getDtoInstance().getStorage()) + " records"
                ).write();
            }
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }

    public static void statusElastic(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi("Elastic", params, response, true);

        if (!ElasticConnection.isUp()) {
            if (ElasticConnection.isEnabled()) {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ENABLED, "ElasticSearch"));
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_DISABLED, "ElasticSearch"));
            }
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ElasticSearch")).finish();;
            return;
        }
        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ON, "ElasticSearch")).write();

        try {
            for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.ELASTIC)) {
                ui.addKeyValue(
                    info.dtoClass.getSimpleName(),
                    CommonRepoElastic.count(info.getDtoInstance().getStorage()) + " records"
                ).write();
            }
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }

    /**
     * <DBMS, <table/collection, record-count>>
     */
    public static Map<String, Map<String, Long>> getDatabaseRecordCount() {
        Map<String, Map<String, Long>> databases = new HashMap<>(5, 1);

        if (MongoConnection.isUp()) {
            Map<String, Long> db = new HashMap<>(200, 1);
            try {
                for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
                    db.put(info.dtoClass.getSimpleName(), MongoQuery.count(info.getDtoInstance().getStorage()));
                }
            } catch (Exception ignore) {

            }
            databases.put(DtoDictionary.Dbms.MONGO.name(), db);
        }

        if (SqlConnection.isUp()) {
            Map<String, Long> db = new HashMap<>(200, 1);
            try (SqlConnection connection = new SqlConnection()) {
                CommonRepoSql repo = new CommonRepoSql(connection);
                for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.SQL)) {
                    db.put(info.dtoClass.getSimpleName(), repo.count(info.getDtoInstance().getStorage()));
                }
            } catch (Exception ignore) {

            }
            databases.put(DtoDictionary.Dbms.SQL.name(), db);
        }

        if (ElasticConnection.isUp()) {
            Map<String, Long> db = new HashMap<>(200, 1);
            try {
                for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.ELASTIC)) {
                    db.put(info.dtoClass.getSimpleName(), CommonRepoElastic.count(info.getDtoInstance().getStorage()));
                }
            } catch (Exception ignore) {

            }
            databases.put(DtoDictionary.Dbms.ELASTIC.name(), db);
        }

        return databases;
    }
}