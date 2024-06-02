package com.vantar.admin.database.dbms.importdata;

import com.vantar.admin.index.Admin;
import com.vantar.business.*;
import com.vantar.business.importexport.MongoImport;
import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.exception.FinishException;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.service.Services;
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

    public static void importData(Params params, HttpServletResponse response, Db.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_IMPORT, params, response, true);
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

        if (Db.Dbms.MONGO.equals(dbms)) {
            importMongo(ui, remove, excludes, includes);
        } else if (Db.Dbms.SQL.equals(dbms)) {
            importSql(ui, remove, excludes, includes);
        } else if (Db.Dbms.ELASTIC.equals(dbms)) {
            importElastic(ui, remove, excludes, includes);
        }

        ui.finish();
    }

    public static void importMongo(WebUi ui, boolean deleteAll, Set<String> excludes, Set<String> includes) {
        if (!Services.isUp(Db.Dbms.MONGO)) {
            return;
        }

        List<DtoDictionary.Info> items = DtoDictionary.getAll(Db.Dbms.MONGO);
        items.sort(Comparator.comparingInt(o -> o.order));
        for (DtoDictionary.Info info : items) {
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

            MongoImport.importDtoData(
                ui,
                data,
                dto,
                dto.getPresentationPropertyNames(),
                deleteAll,
                Db.mongo
            );
        }
    }

    public static void importSql(WebUi ui, boolean deleteAll, Set<String> excludes, Set<String> includes) {
        if (!Services.isUp(Db.Dbms.SQL)) {
            return;
        }

        List<DtoDictionary.Info> items = DtoDictionary.getAll(Db.Dbms.SQL);
        items.sort(Comparator.comparingInt(o -> o.order));
        for (DtoDictionary.Info info : items) {
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
                ui,
                data,
                dto,
                dto.getPresentationPropertyNames(),
                deleteAll
            );
        }
    }

    public static void importElastic(WebUi ui, boolean deleteAll, Set<String> excludes, Set<String> includes) {
        if (!Services.isUp(Db.Dbms.ELASTIC)) {
            return;
        }

        List<DtoDictionary.Info> items = DtoDictionary.getAll(Db.Dbms.ELASTIC);
        items.sort(Comparator.comparingInt(o -> o.order));
        for (DtoDictionary.Info info : items) {
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
                ui,
                data,
                dto,
                dto.getPresentationPropertyNames(),
                deleteAll
            );
        }
    }
}
