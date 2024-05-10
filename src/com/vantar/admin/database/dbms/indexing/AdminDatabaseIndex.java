package com.vantar.admin.database.dbms.indexing;

import com.vantar.admin.index.Admin;
import com.vantar.business.CommonRepoSql;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.sql.*;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminDatabaseIndex {

    public static void listIndexes(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_DATABASE_INDEX, params, response, true);
        if (!(DtoDictionary.Dbms.MONGO.equals(dbms) ? MongoConnection.isUp() : SqlConnection.isUp())) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, dbms)).finish();
            return;
        }

        ui  .addHeading(2, dbms)
            .addHrefBlock(
                VantarKey.ADMIN_DATABASE_INDEX_CREATE,
                "/admin/database/" + dbms.name().toLowerCase() + "/index/create"
            )
            .addEmptyLine(2).write();

        try {
            for (DtoDictionary.Info info : DtoDictionary.getAll(dbms)) {
                List<String> indexes;
                if (DtoDictionary.Dbms.MONGO.equals(dbms)) {
                    indexes = Mongo.Index.getIndexes(info.getDtoInstance());
                } else {
                    try (SqlConnection connection = new SqlConnection()) {
                        SqlExecute exe = new SqlExecute(connection);
                        indexes = exe.getIndexes(info.getDtoInstance());
                    } catch (DatabaseException e) {
                        ui.addErrorMessage(e).write();
                        continue;
                    }
                }

                ui.addKeyValue(
                    info.getDtoClassName(),
                    ui.getBlock("pre", CollectionUtil.join(indexes, '\n')),
                    null,
                    false
                ).write();
            }
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }

    public static void createIndex(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_DATABASE_INDEX_CREATE, params, response, true);
        if (!(DtoDictionary.Dbms.MONGO.equals(dbms) ? MongoConnection.isUp() : SqlConnection.isUp())) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, dbms)).finish();
            return;
        }

        ui  .addHeading(2, dbms)
            .addHrefBlock(
                VantarKey.ADMIN_DATABASE_INDEX,
                "/admin/database/" + dbms.name().toLowerCase() + "/index"
            )
            .addEmptyLine(2).write();

        if (!params.contains("f")) {
            List<DtoDictionary.Info> dtoList = DtoDictionary.getAll();
            Collection<Object> dtos = new TreeSet<>();
            for (DtoDictionary.Info i : dtoList) {
                dtos.add(i.getDtoClassName());
            }
            ui  .beginFormPost()
                .addCheckbox(VantarKey.ADMIN_DATABASE_INDEX_REMOVE, "deleteindex")
                .addInputSelectable(VantarKey.ADMIN_EXCLUDE, "ex", dtos)
                .addInputSelectable(VantarKey.ADMIN_INCLUDE, "in", dtos)
                .addSubmit(VantarKey.ADMIN_DATABASE_INDEX_CREATE_START)
                .finish();
            return;
        }

        boolean deleteindex = params.isChecked("deleteindex");
        String excludes = params.getString("ex");
        String includes = params.getString("in");
        ui.beginBox(VantarKey.ADMIN_DATABASE_INDEX_CREATE).write();
        if (DtoDictionary.Dbms.MONGO.equals(dbms)) {
            createIndexMongo(
                ui,
                deleteindex,
                excludes == null ? null : StringUtil.splitToSet(StringUtil.trim(excludes, ','), ','),
                includes == null ? null : StringUtil.splitToSet(StringUtil.trim(includes, ','), ',')
            );
        } else {
            createIndexSql(ui, deleteindex);
        }
        ui.finish();
    }

    public static void getIndexes(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_DATABASE_INDEX, params, response, true);
        if (!(DtoDictionary.Dbms.MONGO.equals(dbms) ? MongoConnection.isUp() : SqlConnection.isUp())) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, dbms)).finish();
            return;
        }

        DtoDictionary.Info info = DtoDictionary.get(params.getString("dto"));
        if (info == null) {
            ui.finish();
            return;
        }

        ui  .addHeading(2, dbms)
            .addHeading(3, info.dtoClass)
            .addEmptyLine(2)
            .addHrefBlock(
                VantarKey.ADMIN_DATABASE_INDEX_CREATE,
                "/admin/database/" + dbms.name().toLowerCase() + "/index/create?f=1&deleteindex=true&dto="
                    + info.dtoClass.getSimpleName()
            )
            .addEmptyLine(2)
            .write();

        try {
            List<String> indexes = null;
            if (DtoDictionary.Dbms.MONGO.equals(dbms)) {
                indexes = Mongo.Index.getIndexes(info.getDtoInstance());
            } else {
                try (SqlConnection connection = new SqlConnection()) {
                    SqlExecute exe = new SqlExecute(connection);
                    indexes = exe.getIndexes(info.getDtoInstance());
                } catch (DatabaseException e) {
                    ui.addErrorMessage(e).write();
                }
            }

            ui.addBlock("pre", CollectionUtil.join(indexes, '\n'));
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }

    public static void createIndexMongo(WebUi ui, boolean deleteIfExists, Collection<String> excludes, Collection<String> includes) {
        if (!MongoConnection.isUp()) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, DtoDictionary.Dbms.MONGO)).finish();
            return;
        }
        String dtoName = ui.params.getString("dto");
        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
            if (dtoName != null && !dtoName.equalsIgnoreCase(info.dtoClass.getSimpleName())) {
                continue;
            }
            if (ObjectUtil.isNotEmpty(excludes) && excludes.contains(info.dtoClass.getSimpleName())) {
                continue;
            }
            if (ObjectUtil.isNotEmpty(includes) && !includes.contains(info.dtoClass.getSimpleName())) {
                continue;
            }

            Dto dto = info.getDtoInstance();
            if (deleteIfExists) {
                try {
                    Mongo.Index.remove(dto);
                    ui.addMessage(" > deleted: " + dto.getClass().getSimpleName()).write();
                } catch (DatabaseException e) {
                    ui.addErrorMessage(e).write();
                }
            }
            try {
                Mongo.Index.create(dto);
                ui.addMessage(" > created: " + dto.getClass().getSimpleName()).write();
            } catch (DatabaseException e) {
                ui.addErrorMessage(e).write();
            }
        }
    }

    public static void createIndexSql(WebUi ui, boolean deleteIfExists) {
        if (!SqlConnection.isUp()) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, DtoDictionary.Dbms.SQL)).finish();
            return;
        }
        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repoSql = new CommonRepoSql(connection);
            repoSql.createAllDtoIndexes(deleteIfExists);
            ui.addMessage(" > created all indexes").write();
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
        }
    }
}
