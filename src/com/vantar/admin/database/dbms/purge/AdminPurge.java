package com.vantar.admin.database.dbms.purge;

import com.vantar.admin.index.Admin;
import com.vantar.business.*;
import com.vantar.common.VantarParam;
import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.service.Services;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.*;


public class AdminPurge {

    public static void purge(Params params, HttpServletResponse response, Db.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_DATA_PURGE, params, response, true);
        if (!(Db.Dbms.MONGO.equals(dbms) ? Services.isUp(Db.Dbms.MONGO)
            : (Db.Dbms.SQL.equals(dbms) ? Services.isUp(Db.Dbms.SQL) : ElasticConnection.isUp()))) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, dbms)).finish();
            return;
        }

        ui  .addHeading(2, dbms)
            .addEmptyLine(2).write();

        if (!params.contains("f") || !params.isChecked("confirm")) {
            List<DtoDictionary.Info> dtoList = DtoDictionary.getAll();
            Collection<Object> dtos = new TreeSet<>();
            for (DtoDictionary.Info i : dtoList) {
                dtos.add(i.getDtoClassName());
            }

            ui  .beginFormPost()
                .addInputSelectable(VantarKey.ADMIN_EXCLUDE, "ex", dtos)
                .addInputSelectable(VantarKey.ADMIN_INCLUDE, "in", dtos)
                .addCheckbox("DROP TABLE/COLLECTION", "dt", true)
                .addCheckbox(VantarKey.ADMIN_CONFIRM, "confirm")
                .addSubmit(VantarKey.ADMIN_DELETE)
                .finish();
            return;
        }

        boolean dt = params.isChecked("dt");
        String ex = params.getString("ex");
        String in = params.getString("in");
        Set<String> excludes = ex == null ? null : StringUtil.splitToSet(StringUtil.trim(ex, ','), ',');
        Set<String> includes = in == null ? null : StringUtil.splitToSet(StringUtil.trim(in, ','), ',');

        if (Db.Dbms.MONGO.equals(dbms)) {
            purgeMongo(ui, includes, excludes);
        } else if (Db.Dbms.SQL.equals(dbms)) {
            purgeSql(ui, includes, excludes, dt);
        } else if (Db.Dbms.ELASTIC.equals(dbms)) {
            purgeElastic(ui, includes, excludes, dt);
        }

        ui.finish();
    }

    public static void purgeMongo(WebUi ui, Set<String> excludes, Set<String> includes) {
        if (!Services.isUp(Db.Dbms.MONGO)) {
            return;
        }

        for (DtoDictionary.Info info : DtoDictionary.getAll(Db.Dbms.MONGO)) {
            Dto dto = info.getDtoInstance();
            if (ObjectUtil.isNotEmpty(excludes) && excludes.contains(info.getDtoClassName())) {
                continue;
            }
            if (ObjectUtil.isNotEmpty(includes) && !includes.contains(info.getDtoClassName())) {
                continue;
            }

            long count = 1;
            try {
                long total = Db.modelMongo.count(dto.getStorage());
                int i = 0;
                while (count > 0 && ++i < 20) {
                    Db.modelMongo.purge(dto);
                    count = Db.mongo.count(dto.getStorage());
                }
                if (ui != null) {
                    ui.addKeyValue(
                        info.getDtoClassName(),
                        Locale.getString(count == 0 ? VantarKey.SUCCESS_DELETE : VantarKey.FAIL_DELETE)
                            + ": " + total + " > " + count
                    ).write();
                }
            } catch (VantarException e) {
                if (ui != null) {
                    ui.addErrorMessage(e).write();
                }
            }
        }
    }

    public static void purgeSql(WebUi ui, Set<String> excludes, Set<String> includes, boolean dropTable) {
        if (!Services.isUp(Db.Dbms.SQL)) {
            return;
        }

        try (SqlConnection connection = new SqlConnection()) {
            connection.startTransaction();
            CommonRepoSql repo = new CommonRepoSql(connection);

            for (DtoDictionary.Info info : DtoDictionary.getAll(Db.Dbms.SQL)) {
                Dto dto = info.getDtoInstance();
                if (ObjectUtil.isNotEmpty(excludes) && excludes.contains(info.getDtoClassName())) {
                    continue;
                }
                if (ObjectUtil.isNotEmpty(includes) && !includes.contains(info.getDtoClassName())) {
                    continue;
                }

                try {
                    long total = repo.count(dto.getStorage());
                    long count = 0;
                    if (dropTable) {
                        repo.purge(dto.getStorage());
                    } else {
                        repo.purgeData(dto.getStorage());
                        count = repo.count(dto.getStorage());
                    }
                    if (ui != null) {
                        ui.addKeyValue(
                            info.getDtoClassName(),
                            Locale.getString(VantarKey.SUCCESS_DELETE) + ": " + total + " > " + count
                        ).write();
                    }
                } catch (VantarException e) {
                    if (ui != null) {
                        ui.addErrorMessage(e);
                    }
                }

                for (Field field : dto.getFields()) {
                    if (field.isAnnotationPresent(ManyToManyStore.class)) {
                        String[] parts = StringUtil.splitTrim(
                            field.getAnnotation(ManyToManyStore.class).value(),
                            VantarParam.SEPARATOR_NEXT
                        );
                        String table = parts[0];

                        try {
                            long total = repo.count(table);
                            long count = 0;
                            if (dropTable) {
                                repo.purge(table);
                            } else {
                                repo.purgeData(table);
                                count = repo.count(table);
                            }
                            if (ui != null) {
                                ui.addKeyValue(
                                    info.getDtoClassName(),
                                    Locale.getString(VantarKey.SUCCESS_DELETE) + ": " + total + " > " + count
                                ).write();
                            }
                        } catch (VantarException e) {
                            if (ui != null) {
                                ui.addErrorMessage(e);
                            }
                        }
                    }
                }
            }
            connection.commit();
        } catch (VantarException e) {
            if (ui != null) {
                ui.addErrorMessage(e);
            }
        }
    }

    public static void purgeElastic(WebUi ui, Set<String> excludes, Set<String> includes, boolean dropCollection) {
        if (!Services.isUp(Db.Dbms.ELASTIC)) {
            return;
        }

        for (DtoDictionary.Info info : DtoDictionary.getAll(Db.Dbms.ELASTIC)) {
            Dto dto = info.getDtoInstance();
            if (ObjectUtil.isNotEmpty(excludes) && excludes.contains(info.getDtoClassName())) {
                continue;
            }
            if (ObjectUtil.isNotEmpty(includes) && !includes.contains(info.getDtoClassName())) {
                continue;
            }

            try {
                long count = 1;
                long total = CommonRepoElastic.count(dto.getStorage());

                if (dropCollection) {
                    CommonModelElastic.purge(dto.getStorage());
                    count = 0;
                } else {
                    CommonModelElastic.purgeData(dto.getStorage());
                }
                if (ui != null) {
                    ui.addKeyValue(
                        info.getDtoClassName(),
                        Locale.getString(count == 0 ? VantarKey.SUCCESS_DELETE : VantarKey.FAIL_DELETE)
                            + ": " + total + " > " + count
                    ).write();
                }
            } catch (VantarException e) {
                if (ui != null) {
                    ui.addErrorMessage(e);
                }
            }
        }
    }
}
