package com.vantar.admin.database.dbms.importdata;

import com.vantar.admin.index.Admin;
import com.vantar.business.*;
import com.vantar.business.importexport.ImportMongo;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.nosql.mongo.MongoConnection;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.FinishException;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * imports init data
 * /data/import/dto-name-kebab-case
 */
public class AdminImportData {

    public static void importData(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_IMPORT, params, response, true);
        if (!(DtoDictionary.Dbms.MONGO.equals(dbms) ? MongoConnection.isUp()
            : (DtoDictionary.Dbms.SQL.equals(dbms) ? SqlConnection.isUp() : ElasticConnection.isUp()))) {
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
                .addCheckbox(VantarKey.ADMIN_DATA_PURGE, "rm", true)
                .addCheckbox(VantarKey.ADMIN_CONFIRM, "confirm")
                .addSubmit(VantarKey.ADMIN_IMPORT)
                .finish();
            return;
        }

        boolean remove = params.isChecked("rm");
        String ex = params.getString("ex");
        String in = params.getString("in");
        Set<String> excludes = ex == null ? null : StringUtil.splitToSet(StringUtil.trim(ex, ','), ',');
        Set<String> includes = in == null ? null : StringUtil.splitToSet(StringUtil.trim(in, ','), ',');

        if (DtoDictionary.Dbms.MONGO.equals(dbms)) {
            importMongo(ui, remove, excludes, includes);
        } else if (DtoDictionary.Dbms.SQL.equals(dbms)) {
            importSql(ui, remove, excludes, includes);
        } else if (DtoDictionary.Dbms.ELASTIC.equals(dbms)) {
            importElastic(ui, remove, excludes, includes);
        }

        ui.finish();
    }

    public static void importMongo(WebUi ui, boolean deleteAll, Set<String> excludes, Set<String> includes) {
        if (!MongoConnection.isUp()) {
            return;
        }

        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
            Dto dto = info.getDtoInstance();
            if (ObjectUtil.isNotEmpty(excludes) && excludes.contains(info.getDtoClassName())) {
                continue;
            }
            if (ObjectUtil.isNotEmpty(includes) && !includes.contains(info.getDtoClassName())) {
                continue;
            }

            String data = info.getImportData();
            if (StringUtil.isEmpty(data)) {
                continue;
            }

            ImportMongo.importDtoData(
                data,
                dto,
                dto.getPresentationPropertyNames(),
                deleteAll,
                ui
            );
        }
    }

    public static void importSql(WebUi ui, boolean deleteAll, Set<String> excludes, Set<String> includes) {
        if (!SqlConnection.isUp()) {
            return;
        }

        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.SQL)) {
            Dto dto = info.getDtoInstance();
            if (ObjectUtil.isNotEmpty(excludes) && excludes.contains(info.getDtoClassName())) {
                continue;
            }
            if (ObjectUtil.isNotEmpty(includes) && !includes.contains(info.getDtoClassName())) {
                continue;
            }

            String data = info.getImportData();
            if (StringUtil.isEmpty(data)) {
                continue;
            }

            CommonModelSql.importDataAdmin(
                data,
                dto,
                dto.getPresentationPropertyNames(),
                deleteAll,
                ui
            );
        }
    }

    public static void importElastic(WebUi ui, boolean deleteAll, Set<String> excludes, Set<String> includes) {
        if (!ElasticConnection.isUp()) {
            return;
        }

        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.ELASTIC)) {
            Dto dto = info.getDtoInstance();
            if (ObjectUtil.isNotEmpty(excludes) && excludes.contains(info.getDtoClassName())) {
                continue;
            }
            if (ObjectUtil.isNotEmpty(includes) && !includes.contains(info.getDtoClassName())) {
                continue;
            }

            String data = info.getImportData();
            if (StringUtil.isEmpty(data)) {
                continue;
            }

            CommonModelElastic.importDataAdmin(
                data,
                dto,
                dto.getPresentationPropertyNames(),
                deleteAll,
                ui
            );
        }
    }

}
