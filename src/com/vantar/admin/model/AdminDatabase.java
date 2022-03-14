package com.vantar.admin.model;

import com.vantar.business.*;
import com.vantar.common.*;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.*;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.*;


public class AdminDatabase {

    private static final int DB_DELETE_TRIES = 100;
    private static final int DELAY = 1000;

    // > > > SQL

    public static void synchSql(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_DATABASE_SYNCH_TITLE, "SQL"), params, response, true);

        if (params.isChecked("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addCheckbox(Locale.getString(VantarKey.ADMIN_DATABASE_SYNCH_CONFIRM), WebUi.PARAM_CONFIRM)
                .addSubmit(Locale.getString(VantarKey.ADMIN_DATABASE_SYNCH))
                .finish();
            return;
        }

        synchSql(ui);
        ui.finish();
    }

    public static void synchSql(WebUi ui) {
        if (!SqlConnection.isUp) {
            return;
        }

        ui.beginBox(Locale.getString(VantarKey.ADMIN_DATABASE_SYNCH_RUNNING)).write();

        SqlArtaSynch synch = new SqlArtaSynch(Settings.sql());
        try {
            synch.cleanup();
            synch.createFiles();
            ui.addPre(synch.build()).addMessage(Locale.getString(VantarKey.ADMIN_FINISHED));
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.containerEnd().write();
    }

    public static void createSqlIndex(Params params, HttpServletResponse response) throws FinishException {
        if (!SqlConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_CREATE, "SQL"), params, response, true);

        ui.addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_VIEW), "/admin/database/sql/indexes").addEmptyLine(2);

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addCheckbox(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_REMOVE), "deleteindex")
                .addSubmit(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_CREATE_START))
                .finish();

            return;
        }

        createSqlIndex(ui, params.isChecked("deleteindex"));
        ui.finish();
    }

    public static void createSqlIndex(WebUi ui, boolean deleteIfExists) {
        if (!SqlConnection.isUp) {
            return;
        }

        ui.beginBox(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_CREATE, "SQL...")).write();

        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repoSql = new CommonRepoSql(connection);
            repoSql.createAllDtoIndexes(deleteIfExists);

            ui.addMessage(Locale.getString(VantarKey.ADMIN_FINISHED));

        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.containerEnd().write();
    }

    public static void listSqlIndexes(Params params, HttpServletResponse response) throws FinishException {
        if (!SqlConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_INDEX_TITLE, "SQL"), params, response, true);

        ui.addBlockLink(
                Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_CREATE, "SQL..."),
                "/admin/database/sql/index/create"
            )
            .addEmptyLine(2)
            .write();

        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repo = new CommonRepoSql(connection);

            for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.SQL)) {
                ui  .beginBox(info.getDtoClassName())
                    .addPre(CollectionUtil.join(repo.getIndexes(info.getDtoInstance()), '\n'))
                    .containerEnd()
                    .write();
            }
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }

    public static void purgeSql(Params params, HttpServletResponse response) throws FinishException {
        if (!SqlConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_DATABASE_REMOVE, "SQL"), params, response, true);

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addInput(Locale.getString(VantarKey.ADMIN_DELAY), "delay", Integer.toString(DELAY), "ltr")
                .addInput(Locale.getString(VantarKey.ADMIN_DELETE_EXCLUDE), "exclude", "", "ltr")
                .addCheckbox(Locale.getString(VantarKey.ADMIN_DELETE_INCLUDE), "remove", false)
                .addSubmit(Locale.getString(VantarKey.ADMIN_DELETE))
                .finish();
            return;
        }

        purgeSql(ui, params.getInteger("delay", DELAY), params.isChecked("remove"), params.getStringSet("exclude"));
        ui.finish();
    }

    public static void purgeSql(WebUi ui, int delay, boolean dropTable, Set<String> exclude) {
        if (!SqlConnection.isUp) {
            return;
        }
        ui.beginBox(Locale.getString(VantarKey.ADMIN_DATABASE_REMOVE, "SQL")).write().sleep(delay);

        try (SqlConnection connection = new SqlConnection()) {
            connection.startTransaction();
            CommonRepoSql repo = new CommonRepoSql(connection);

            for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.SQL)) {
                Dto dto = info.getDtoInstance();
                if (exclude != null && (exclude.contains(dto.getStorage()) || exclude.contains(info.getDtoClassName()))) {
                    ui.addKeyValue(info.getDtoClassName(), Locale.getString(VantarKey.ADMIN_IGNORE)).write();
                    continue;
                }

                String msg;
                long count = 0;
                long total = 0;
                try {
                    total = repo.count(dto.getStorage());
                    if (dropTable) {
                        repo.purge(dto.getStorage());
                        count = 0;
                    } else {
                        repo.purgeData(dto.getStorage());
                        count = repo.count(dto.getStorage());
                    }
                    msg = Locale.getString(VantarKey.DELETE_SUCCESS);
                } catch (DatabaseException e) {
                    msg = Locale.getString(VantarKey.DELETE_FAIL);
                }

                ui.addKeyValue(info.getDtoClassName(), msg + " : " + total + " > " + count).write();

                for (Field field : dto.getFields()) {
                    if (field.isAnnotationPresent(ManyToManyStore.class)) {
                        String[] parts = StringUtil.split(field.getAnnotation(ManyToManyStore.class).value(), VantarParam.SEPARATOR_NEXT);
                        String table = parts[0];

                        try {
                            total = repo.count(table);
                            if (dropTable) {
                                repo.purge(table);
                                count = 0;
                            } else {
                                repo.purgeData(table);
                                count = repo.count(table);
                            }
                            msg = Locale.getString(VantarKey.DELETE_SUCCESS);
                        } catch (DatabaseException e) {
                            msg = Locale.getString(VantarKey.DELETE_FAIL);
                        }

                        ui.addKeyValue(table, msg + " : " + total + " > " + count).write();
                    }
                }
            }

            connection.commit();

        } catch (DatabaseException e) {
            ui.addKeyValueFail("SQL FAILED, transaction rolled back", ObjectUtil.throwableToString(e));
        }

        ui.containerEnd().containerEnd().write();
    }

    public static void importSql(Params params, HttpServletResponse response) throws FinishException {
        if (!SqlConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_IMPORT_TITLE, "SQL"), params, response, true);

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addInput(Locale.getString(VantarKey.ADMIN_DELAY), "delay", Integer.toString(DELAY), "ltr")
                .addInput(Locale.getString(VantarKey.ADMIN_IMPORT_EXCLUDE), "exclude", "", "ltr")
                .addCheckbox(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), "remove", true)
                .addSubmit(Locale.getString(VantarKey.ADMIN_IMPORT))
                .finish();
            return;
        }

        importSql(ui, params.getInteger("delay", DELAY), params.isChecked("remove"), params.getStringSet("exclude"));
        ui.finish();
    }

    public static void importSql(WebUi ui, int delay, boolean deleteAll, Set<String> exclude) {
        if (!SqlConnection.isUp) {
            return;
        }
        ui.beginBox(Locale.getString(VantarKey.ADMIN_IMPORT_TITLE, "SQL")).write().sleep(delay);

        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.SQL)) {
            String data = info.getImportData();
            if (StringUtil.isEmpty(data)) {
                continue;
            }

            Dto dto = info.getDtoInstance();
            if (exclude != null && (exclude.contains(dto.getStorage()) || exclude.contains(info.getDtoClassName()))) {
                ui.addMessage(info.getDtoClassName() + " " + Locale.getString(VantarKey.ADMIN_IGNORE)).write();
                continue;
            }

            CommonModelSql.importDataAdmin(
                data,
                dto,
                info.present,
                deleteAll,
                ui
            );
        }

        ui.containerEnd().containerEnd().write();
    }

    // > > > MONGO

    public static void createMongoIndex(Params params, HttpServletResponse response) throws FinishException {
        if (!MongoConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_CREATE, "MONGO"), params, response, true);

        ui.addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_VIEW), "/admin/database/mongo/indexes").addEmptyLine(2).addEmptyLine();

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addCheckbox(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_REMOVE), "deleteindex")
                .addSubmit(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_CREATE_START))
                .finish();
            return;
        }

        createMongoIndex(ui, params.isChecked("deleteindex"));
        ui.finish();
    }

    public static void createMongoIndex(WebUi ui, boolean deleteIfExists) {
        if (!MongoConnection.isUp) {
            return;
        }

        ui.beginBox(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_CREATE, "MONGO...")).write();

        try {
            CommonRepoMongo.createAllDtoIndexes(deleteIfExists);
            ui.addMessage(Locale.getString(VantarKey.ADMIN_FINISHED));

        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.containerEnd().write();
    }

    public static void listMongoIndexes(Params params, HttpServletResponse response) throws FinishException {
        if (!MongoConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_INDEX_TITLE, "MONGO"), params, response, true);

        ui.addBlockLink(Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_CREATE, "MONGO..."),
            "/admin/database/mongo/index/create").addEmptyLine().addEmptyLine().write();

        try {
            for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
                List<String> indexes = Mongo.Index.getIndexes(info.getDtoInstance());
                ui  .beginBox(info.getDtoClassName())
                    .addPre(CollectionUtil.join(indexes, '\n'))
                    .containerEnd()
                    .write();
            }
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }

    public static void purgeMongo(Params params, HttpServletResponse response) throws FinishException {
        if (!MongoConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_DATABASE_REMOVE, "MONGO"), params, response, true);

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addInput(Locale.getString(VantarKey.ADMIN_DELAY), "delay", Integer.toString(DELAY), "ltr")
                .addInput(Locale.getString(VantarKey.ADMIN_DELETE_EXCLUDE), "exclude", "", "ltr")
                .addSubmit(Locale.getString(VantarKey.ADMIN_DELETE))
                .finish();
            return;
        }

        purgeMongo(ui, params.getInteger("delay", DELAY), params.getStringSet("exclude"));
        ui.finish();
    }

    public static void purgeMongo(WebUi ui, int delay, Set<String> exclude) {
        if (!MongoConnection.isUp) {
            return;
        }
        ui.beginBox(Locale.getString(VantarKey.ADMIN_DATABASE_REMOVE, "MONGO")).write().sleep(delay);

        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
            Dto dto = info.getDtoInstance();
            if (exclude != null && (exclude.contains(dto.getStorage()) || exclude.contains(info.getDtoClassName()))) {
                ui.addKeyValue(info.getDtoClassName(), Locale.getString(VantarKey.ADMIN_IGNORE)).write();
                continue;
            }

            String msg;
            long count = 1;
            long total = 0;
            try {
                total = CommonRepoMongo.count(dto.getStorage());

                int i = 0;
                while (count > 0 && i++ < DB_DELETE_TRIES) {
                    CommonModelMongo.purge(dto);
                    count = CommonRepoMongo.count(dto.getStorage());
                }
                msg = Locale.getString(count == 0 ? VantarKey.DELETE_SUCCESS : VantarKey.DELETE_FAIL);
            } catch (DatabaseException | ServerException e) {
                msg = Locale.getString(VantarKey.DELETE_FAIL);
            }

            ui.addKeyValue(info.getDtoClassName(), msg + " : " + total + " > " + count).write();
        }

        ui.containerEnd().containerEnd().write();
    }

    public static void listMongoSequences(Params params, HttpServletResponse response) throws FinishException {
        if (!MongoConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi("MONGO sequences", params, response, true);

        try {
            Mongo.Sequence.getAll().forEach((key, value) -> ui.addKeyValue(key, value));
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }

    public static void importMongo(Params params, HttpServletResponse response) throws FinishException {
        if (!MongoConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_IMPORT_TITLE, "MONGO"), params, response, true);

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addInput(Locale.getString(VantarKey.ADMIN_DELAY), "delay", Integer.toString(DELAY), "ltr")
                .addInput(Locale.getString(VantarKey.ADMIN_IMPORT_EXCLUDE), "exclude", "", "ltr")
                .addCheckbox(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), "remove", true)
                .addSubmit(Locale.getString(VantarKey.ADMIN_IMPORT))
                .finish();
            return;
        }

        importMongo(ui, params.getInteger("delay", DELAY), params.isChecked("remove"), params.getStringSet("exclude"));
        ui.finish();
    }

    public static void importMongo(WebUi ui, int delay, boolean deleteAll, Set<String> exclude) {
        if (!MongoConnection.isUp) {
            return;
        }
        ui.beginBox(Locale.getString(VantarKey.ADMIN_IMPORT_TITLE, "MONGO")).write().sleep(delay);

        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
            String data = info.getImportData();
            if (StringUtil.isEmpty(data)) {
                continue;
            }

            Dto dto = info.getDtoInstance();
            if (exclude != null && (exclude.contains(dto.getStorage()) || exclude.contains(info.getDtoClassName()))) {
                ui.addMessage(info.getDtoClassName() + " " + Locale.getString(VantarKey.ADMIN_IGNORE)).write();
                continue;
            }

            CommonModelMongo.importDataAdmin(
                data,
                dto,
                info.present,
                deleteAll,
                ui
            );
        }

        ui.containerEnd().containerEnd().write();
    }

    // > > > ELASTIC

    public static void synchElastic(Params params, HttpServletResponse response) throws FinishException {
        if (!ElasticConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_DATABASE_SYNCH_TITLE, "Elastic"), params, response, true);

        if (!params.isChecked("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addCheckbox(Locale.getString(VantarKey.ADMIN_DATABASE_SYNCH_CONFIRM), WebUi.PARAM_CONFIRM)
                .addSubmit(Locale.getString(VantarKey.ADMIN_DATABASE_SYNCH))
                .finish();
            return;
        }

        synchElastic(ui);
        ui.finish();
    }

    public static void synchElastic(WebUi ui) {
        if (!ElasticConnection.isUp) {
            return;
        }

        ui.beginBox(Locale.getString(VantarKey.ADMIN_DATABASE_SYNCH_RUNNING)).write();

        try {
            ElasticIndexes.create();
            ui.addMessage(Locale.getString(VantarKey.ADMIN_FINISHED));
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.containerEnd().write();
    }

    public static void purgeElastic(Params params, HttpServletResponse response) throws FinishException {
        if (!ElasticConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_DATABASE_REMOVE, "Elastic"), params, response, true);

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addInput(Locale.getString(VantarKey.ADMIN_DELAY), "delay", Integer.toString(DELAY), "ltr")
                .addInput(Locale.getString(VantarKey.ADMIN_DELETE_EXCLUDE), "exclude", "", "ltr")
                .addCheckbox(Locale.getString(VantarKey.ADMIN_DELETE_INCLUDE), "remove", false)
                .addSubmit(Locale.getString(VantarKey.ADMIN_DELETE))
                .finish();
            return;
        }

        purgeElastic(ui, params.getInteger("delay", DELAY), params.isChecked("remove"), params.getStringSet("exclude"));
        ui.finish();
    }

    public static void purgeElastic(WebUi ui, int delay, boolean dropCollection, Set<String> exclude) {
        if (!ElasticConnection.isUp) {
            return;
        }
        ui.beginBox(Locale.getString(VantarKey.ADMIN_DATABASE_REMOVE, "Elastic")).sleep(delay).write();

        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.ELASTIC)) {
            Dto dto = info.getDtoInstance();
            if (exclude != null && (exclude.contains(dto.getStorage()) || exclude.contains(info.getDtoClassName()))) {
                ui.addKeyValue(info.getDtoClassName(), Locale.getString(VantarKey.ADMIN_IGNORE)).write();
                continue;
            }

            String msg;
            long count = 1;
            long total = 0;
            try {
                total = CommonRepoElastic.count(dto.getStorage());

                if (dropCollection) {
                    CommonModelElastic.purge(dto.getStorage());
                    count = 0;
                } else {
                    CommonModelElastic.purgeData(dto.getStorage());
                    count = CommonRepoMongo.count(dto.getStorage());
                }

                msg = Locale.getString(count == 0 ? VantarKey.DELETE_SUCCESS : VantarKey.DELETE_FAIL);
            } catch (DatabaseException | ServerException e) {
                msg = Locale.getString(VantarKey.DELETE_FAIL);
            }

            ui.addKeyValue(info.getDtoClassName(), msg + " : " + total + " > " + count).write();
        }

        ui.containerEnd().containerEnd().write();
    }

    public static void importElastic(Params params, HttpServletResponse response) throws FinishException {
        if (!ElasticConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_IMPORT_TITLE, "Elastic"), params, response, true);

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addInput(Locale.getString(VantarKey.ADMIN_DELAY), "delay", Integer.toString(DELAY), "ltr")
                .addInput(Locale.getString(VantarKey.ADMIN_IMPORT_EXCLUDE), "exclude", "", "ltr")
                .addCheckbox(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), "remove", true)
                .addSubmit(Locale.getString(VantarKey.ADMIN_IMPORT))
                .finish();
            return;
        }

        importElastic(ui, params.getInteger("delay", DELAY), params.isChecked("remove"), params.getStringSet("exclude"));
        ui.finish();
    }

    public static void importElastic(WebUi ui, int delay, boolean deleteAll, Set<String> exclude) {
        if (!ElasticConnection.isUp) {
            return;
        }
        ui.beginBox(Locale.getString(VantarKey.ADMIN_IMPORT_TITLE, "Elastic")).sleep(delay).write();

        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.ELASTIC)) {
            String data = info.getImportData();
            if (StringUtil.isEmpty(data)) {
                continue;
            }

            Dto dto = info.getDtoInstance();
            if (exclude != null && (exclude.contains(dto.getStorage()) || exclude.contains(info.getDtoClassName()))) {
                ui.addMessage(info.getDtoClassName() + " " + Locale.getString(VantarKey.ADMIN_IGNORE)).write();
                continue;
            }

            CommonModelElastic.importDataAdmin(
                data,
                dto,
                info.present,
                deleteAll,
                ui
            );
        }

        ui.containerEnd().containerEnd().write();
    }

    public static void getMappingElastic(Params params, HttpServletResponse response) throws FinishException {
        if (!ElasticConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_ELASTIC_INDEX_DEF), params, response, true);

        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.ELASTIC)) {
            Dto dto = info.getDtoInstance();
            ui.beginBox(info.getDtoClassName());
            try {
                ElasticIndexes.getMapping(dto).forEach((k, v) -> {
                    if (v instanceof Map) {
                        ui  .beginPre()
                            .addText(k + ":\n");
                        ((Map) v).forEach((m, n) -> ui.addText("    " + m + ": " + n + "\n"));
                        ui.containerEnd();
                    } else {
                        ui.addPre(k + ": " + v);
                    }
                });
            } catch (DatabaseException e) {
                ui.addErrorMessage(e);
            }
            ui.containerEnd().write();
        }

        ui.finish();
    }

    public static void actionsElastic(Params params, HttpServletResponse response) throws FinishException {
        if (!ElasticConnection.isUp) {
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_ELASTIC_SETTINGS), params, response, true);

        String target = params.getString("target", "");
        String destination = params.getString("destination", "");
        boolean shrink = params.isChecked("shrink");
        boolean refresh = params.isChecked("refresh");

        ui  .addMessage(Locale.getString(VantarKey.ADMIN_ELASTIC_SETTINGS_MSG1))
            .addEmptyLine()
            .beginFormPost()
            .addSelect(
                Locale.getString(VantarKey.ADMIN_ELASTIC_SETTINGS_CLASS_NAME),
                "target",
                DtoDictionary.getNames(DtoDictionary.Dbms.ELASTIC),
                target
            )
            .addInput(
                Locale.getString(VantarKey.ADMIN_ELASTIC_SETTINGS_DESTINATION),
                "destination",
                destination,
                "ltr"
            )
            .addCheckbox("Shrink", "shrink", shrink)
            .addCheckbox("Refresh", "refresh", refresh)
            .addSubmit("Clone")
            .containerEnd()
            .write();

        if (params.isChecked("f")) {
            if (StringUtil.isEmpty(target)) {
                ui.addErrorMessage(Locale.getString(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR1)).finish();
                return;
            }
            DtoDictionary.Info info = DtoDictionary.get(target);
            if (info == null) {
                ui.finish();
                return;
            }

            if (StringUtil.isNotEmpty(destination)) {
                try {
                    ElasticIndexes.clone(info.getDtoInstance(), destination);
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR2));
                } catch (DatabaseException e) {
                    ui.addErrorMessage(e);
                }
            }

            if (shrink) {
                try {
                    ElasticIndexes.shrink(info.getDtoInstance());
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR3));
                } catch (DatabaseException e) {
                    ui.addErrorMessage(e);
                }
            }

            if (refresh) {
                try {
                    ElasticIndexes.refresh(info.getDtoInstance());
                    ui.addMessage(Locale.getString(VantarKey.ADMIN_ELASTIC_SETTINGS_ERR4));
                } catch (DatabaseException e) {
                    ui.addErrorMessage(e);
                }
            }
        }

        ui.finish();
    }
}
