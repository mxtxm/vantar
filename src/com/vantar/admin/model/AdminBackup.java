package com.vantar.admin.model;

import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.nosql.elasticsearch.ElasticBackup;
import com.vantar.database.nosql.mongo.MongoBackup;
import com.vantar.database.query.QueryBuilder;
import com.vantar.database.sql.SqlBackup;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.backup.ServiceBackup;
import com.vantar.util.datetime.*;
import com.vantar.util.file.FileUtil;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;


public class AdminBackup {

    private static final String DUMP_FILE_EXT = ".dump";


    public static void backup(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_BACKUP_CREATE), params, response, true);
        ui.beginBox(dbms.toString());

        ServiceBackup backup;
        try {
            backup = Services.get(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e);
            ui.write();
            return;
        }

        DateTimeRange dateRange = params.getDateRange("datemin", "datemax");
        String dbDumpFilename = params.getString(
            "dumpfile",
            backup.getPath() + dbms.toString().toLowerCase() + "-"
                + (new DateTime().formatter().getDateTimeSimple()) + DUMP_FILE_EXT
        );

        if (!params.isChecked("f")) {
            ui  .addMessage(Locale.getString(VantarKey.ADMIN_BACKUP_MSG1))
                .addMessage(Locale.getString(VantarKey.ADMIN_BACKUP_MSG2))
                .beginFormPost()
                .addInput(Locale.getString(VantarKey.ADMIN_BACKUP_FILE_PATH), "dumpfile", dbDumpFilename)
                .addInput(Locale.getString(VantarKey.ADMIN_DATE_FROM), "datemin",
                    dateRange.dateMin == null ? "" : dateRange.dateMin.formatter().getDateTimePersian())
                .addInput(Locale.getString(VantarKey.ADMIN_DATE_TO), "datemax",
                    dateRange.dateMax == null ? "" : dateRange.dateMax.formatter().getDateTimePersian())
                .addSubmit(Locale.getString(VantarKey.ADMIN_BACKUP_CREATE_START))
                .addBlockLink("Query", "/admin/data/backup/mongo/q")
                .finish();
            return;
        }

        if (dateRange == null || dateRange.dateMin == null) {
            dateRange = null;
        }
        if (dbms.equals(DtoDictionary.Dbms.SQL)) {
            SqlBackup.dump(dbDumpFilename, dateRange, ui);
        } else if (dbms.equals(DtoDictionary.Dbms.MONGO)) {
            MongoBackup.dump(dbDumpFilename, dateRange, ui);
        } else if (dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
            ElasticBackup.dump(dbDumpFilename, dateRange, ui);
        }

        ui.finish();
    }

    public static void backupQuery(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_BACKUP_CREATE), params, response, true);
        ui.beginBox(dbms.toString());

        ServiceBackup backup;
        try {
            backup = Services.get(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e);
            ui.write();
            return;
        }

        String dbDumpFilename = params.getString(
            "dumpfile",
            backup.getPath() + dbms.toString().toLowerCase() + "-query-"
                + (new DateTime().formatter().getDateTimeSimple()) + DUMP_FILE_EXT
        );

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addInput(Locale.getString(VantarKey.ADMIN_BACKUP_FILE_PATH), "dumpfile", dbDumpFilename)
                .addTextArea("Query", "query")
                .addSubmit(Locale.getString(VantarKey.ADMIN_BACKUP_CREATE_START))
                .finish();
            return;
        }

        try {
            QueryBuilder q = new QueryBuilder(params.getQueryData("query"));
            MongoBackup.dumpQuery(dbDumpFilename, q, ui);
        } catch (InputException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }

    public static void restore(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_BACKUP_RESTORE), params, response, true);

        ui.beginBox(dbms.toString());

        ServiceBackup backup;
        try {
            backup = Services.get(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e);
            ui.write();
            return;
        }

        String dbDumpFilename = params.getString("dumpfile");

        if (!params.isChecked("f")) {
            List<String> files = new ArrayList<>(10);
            String dbmsName = dbms.toString().toLowerCase();
            for (String path : FileUtil.getDirectoryFiles(backup.getPath())) {
                if (!StringUtil.contains(path, dbmsName)) {
                    continue;
                }
                files.add(path);
            }

            ui  .addMessage(Locale.getString(VantarKey.ADMIN_RESTORE_MSG1))
                .addMessage(Locale.getString(VantarKey.ADMIN_RESTORE_MSG2))
                .addMessage(Locale.getString(VantarKey.ADMIN_RESTORE_MSG3))
                .beginFormPost()
                .addSelect(Locale.getString(VantarKey.ADMIN_BACKUP_FILE_PATH), "dumpfile", files.toArray(new String[0]))
                .addCheckbox(Locale.getString(VantarKey.ADMIN_RESTORE_DELETE_CURRENT_DATA), "deleteall", true)
                .addCheckbox("camelCaseProperties", "camelcase", false)
                .addSubmit(Locale.getString(VantarKey.ADMIN_BACKUP_RESTORE))
                .finish();
            return;
        }

        if (dbms.equals(DtoDictionary.Dbms.SQL)) {
            SqlBackup.restore(dbDumpFilename, params.isChecked("deleteall"), ui);
        } else if (dbms.equals(DtoDictionary.Dbms.MONGO)) {
            MongoBackup.restore(dbDumpFilename, params.isChecked("deleteall"), params.isChecked("camelcase"), ui);
        } else if (dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
            ElasticBackup.restore(dbDumpFilename, params.isChecked("deleteall"), ui);
        }

        ui.finish();
    }

    public static void backupFiles(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_BACKUP_FILES), params, response, true);

        ServiceBackup backup;
        try {
            backup = Services.get(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e);
            ui.write();
            return;
        }

        ui.beginBox(dbms.toString() + Locale.getString(VantarKey.ADMIN_BACKUP_FILES));

        List<String> paths = params.getStringList("delete");
        if (!ObjectUtil.isEmpty(paths) && params.isChecked(WebUi.PARAM_CONFIRM)) {
            for (String path : paths) {
                String[] parts = StringUtil.split(path, '/');
                String filename = parts[parts.length - 1];

                File file = new File(path);
                if (file.delete()) {
                    ui.addMessage(filename + ": " + Locale.getString(VantarKey.DELETE_SUCCESS));
                } else {
                    ui.addErrorMessage(filename + ": " + Locale.getString(VantarKey.DELETE_FAIL));
                }
            }
            ui.finish();
            return;
        }

        ui  .beginFormPost()
            .addEmptyLine();

        for (String path : FileUtil.getDirectoryFiles(backup.getPath())) {
            if (!path.endsWith(DUMP_FILE_EXT)) {
                continue;
            }
            String[] parts = StringUtil.split(path, '/');
            String filename = parts[parts.length - 1];

            ui.addBoxWithNoEscape(
                ui.getCheckbox("delete", path) +
                ui.getLink("download", "/admin/data/backup/download?" + "file" + "=" + filename, false, false) +
                filename + " (" + FileUtil.getSizeMb(path) + "Mb)"
            ).write();
        }

        ui  .addEmptyLine(3)
            .addCheckbox(Locale.getString(VantarKey.ADMIN_DELETE_DO), WebUi.PARAM_CONFIRM)
            .addSubmit(Locale.getString(VantarKey.ADMIN_DELETE))
            .finish();
    }

    public static void upload(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_BACKUP_UPLOAD), params, response, true);

        if (!params.isChecked("f")) {
            ui  .beginUploadForm()
                .addFile(Locale.getString(VantarKey.ADMIN_BACKUP_UPLOAD_FILE), "file")
                .addSubmit(Locale.getString(VantarKey.ADMIN_SUBMIT))
                .finish();
            return;
        }

        ServiceBackup backup;
        try {
            backup = Services.get(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e);
            ui.write();
            return;
        }

        try (Params.Uploaded uploaded = params.upload("file")) {
            if (!uploaded.isUploaded() || uploaded.isIoError()) {
                ui.addErrorMessage(Locale.getString(VantarKey.REQUIRED, "file")).finish();
                return;
            }

            if (!uploaded.moveTo(backup.getPath(), uploaded.getOriginalFilename())) {
                ui.addMessage(Locale.getString(VantarKey.UPLOAD_FAIL)).finish();
                return;
            }
        }

        ui.addMessage(Locale.getString(VantarKey.UPLOAD_SUCCESS)).finish();
    }

    public static void logs(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_BACKUP), params, response, true);

        try {
            ServiceBackup serviceBackup = Services.get(ServiceBackup.class);
            for (String log : serviceBackup.getLogs()) {
                ui.addPre(log);
            }
        } catch (ServiceException ignore) {

        }

        ui.finish();
    }
}
