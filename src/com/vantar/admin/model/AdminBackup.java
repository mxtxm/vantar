package com.vantar.admin.model;

import com.vantar.common.Settings;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.nosql.elasticsearch.ElasticBackup;
import com.vantar.database.nosql.mongo.MongoBackup;
import com.vantar.database.sql.SqlBackup;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.util.datetime.*;
import com.vantar.util.file.FileUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;


public class AdminBackup {

    private static final String DUMP_FILE = "dumpfile";
    private static final String DELETE_ALL = "deleteall";
    private static final String DATE_TIME_MIN = "datemin";
    private static final String DATE_TIME_MAX = "datemax";
    private static final String PARAM_FILE = "file";


    public static void backup(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_BACKUP_CREATE), params, response);
        if (ui == null) {
            return;
        }
        ui.beginBox(dbms.toString());

        DateTimeRange dateRange = params.getDateRange(DATE_TIME_MIN, DATE_TIME_MAX);
        String dbDumpFilename = params.getString(
            DUMP_FILE,
            Settings.backup().getBackupDir() + dbms.toString().toLowerCase() + "-"
                + (new DateTime().formatter().getDateTimeSimple()) + ".dump"
        );

        if (!params.isChecked("f")) {
            ui  .addMessage(Locale.getString(VantarKey.ADMIN_BACKUP_MSG1))
                .addMessage(Locale.getString(VantarKey.ADMIN_BACKUP_MSG2))
                .beginFormPost()
                .addInput(Locale.getString(VantarKey.ADMIN_BACKUP_FILE_PATH), DUMP_FILE, dbDumpFilename)
                .addInput(Locale.getString(VantarKey.ADMIN_DATE_FROM), DATE_TIME_MIN,
                    dateRange.dateMin == null ? "" : dateRange.dateMin.formatter().getDateTimePersian())
                .addInput(Locale.getString(VantarKey.ADMIN_DATE_TO), DATE_TIME_MAX,
                    dateRange.dateMax == null ? "" : dateRange.dateMax.formatter().getDateTimePersian())
                .addSubmit(Locale.getString(VantarKey.ADMIN_BACKUP_CREATE_START))
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

    public static void restore(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_BACKUP_RESTORE), params, response);
        if (ui == null) {
            return;
        }
        ui.beginBox(dbms.toString());

        String dbDumpFilename = params.getString(DUMP_FILE);

        if (!params.isChecked("f")) {
            List<String> files = new ArrayList<>();
            String dbmsName = dbms.toString().toLowerCase();
            for (String path : FileUtil.getDirectoryFiles(Settings.backup().getBackupDir())) {
                if (!StringUtil.contains(path, dbmsName)) {
                    continue;
                }
                files.add(path);
            }

            ui  .addMessage(Locale.getString(VantarKey.ADMIN_RESTORE_MSG1))
                .addMessage(Locale.getString(VantarKey.ADMIN_RESTORE_MSG2))
                .addMessage(Locale.getString(VantarKey.ADMIN_RESTORE_MSG3))
                .beginFormPost()
                .addSelect(Locale.getString(VantarKey.ADMIN_BACKUP_FILE_PATH), DUMP_FILE, files.toArray(new String[0]))
                .addCheckbox(Locale.getString(VantarKey.ADMIN_RESTORE_DELETE_CURRENT_DATA), DELETE_ALL, true)
                .addSubmit(Locale.getString(VantarKey.ADMIN_BACKUP_RESTORE))
                .finish();
            return;
        }

        if (dbms.equals(DtoDictionary.Dbms.SQL)) {
            SqlBackup.restore(dbDumpFilename, params.isChecked(DELETE_ALL), ui);
        } else if (dbms.equals(DtoDictionary.Dbms.MONGO)) {
            MongoBackup.restore(dbDumpFilename, params.isChecked(DELETE_ALL), ui);
        } else if (dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
            ElasticBackup.restore(dbDumpFilename, params.isChecked(DELETE_ALL), ui);
        }

        ui.finish();
    }

    public static void backupFiles(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_BACKUP_FILES), params, response);
        if (ui == null) {
            return;
        }
        ui.beginBox(dbms.toString() + Locale.getString(VantarKey.ADMIN_DOWNLOAD));
        String dbmsName = dbms.toString().toLowerCase();

        for (String path : FileUtil.getDirectoryFiles(Settings.backup().getBackupDir())) {
            if (!StringUtil.contains(path, dbmsName)) {
                continue;
            }
            String[] parts = StringUtil.split(path, '/');
            String filename = parts[parts.length - 1];
            ui.addBlockLink(filename + " (" + FileUtil.getSizeMb(path) + "Mb)",
                "/admin/data/backup/download?" + PARAM_FILE + "=" + filename);
        }

        ui.containerEnd().beginBox(dbms.toString() + Locale.getString(VantarKey.ADMIN_REMOVE_BACKUP_FILE));

        for (String path : FileUtil.getDirectoryFiles(Settings.backup().getBackupDir())) {
            if (!StringUtil.contains(path, dbmsName)) {
                continue;
            }
            String[] parts = StringUtil.split(path, '/');
            String filename = parts[parts.length - 1];
            ui.addBlockLink(filename + " (" + FileUtil.getSizeMb(path) + "Mb)",
                "/admin/data/backup/delete?" + PARAM_FILE + "=" + path);
        }

        ui.finish();
    }

    public static void deleteFile(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_REMOVE_BACKUP_FILE), params, response);
        if (ui == null) {
            return;
        }
        String filepath = params.getString(PARAM_FILE);
        ui.beginBox(filepath);

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addHidden(PARAM_FILE, filepath)
                .addCheckbox(Locale.getString(VantarKey.ADMIN_DELETE_DO), DELETE_ALL)
                .addSubmit(Locale.getString(VantarKey.ADMIN_DELETE_DO))
                .finish();
            return;
        }

        File file = new File(filepath);
        if (file.delete()) {
            ui.addMessage(Locale.getString(VantarKey.DELETE_SUCCESS));
        } else {
            ui.addErrorMessage(Locale.getString(VantarKey.DELETE_FAIL));
        }

        ui.finish();
    }
}
