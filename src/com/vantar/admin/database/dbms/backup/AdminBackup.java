package com.vantar.admin.database.dbms.backup;

import com.vantar.admin.index.Admin;
import com.vantar.database.common.*;
import com.vantar.database.dto.*;
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
import com.vantar.util.file.*;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.bson.json.JsonMode;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;


public class AdminBackup {

    private static final String DUMP_FILE_EXT = ".dump";


    public static void backup(Params params, HttpServletResponse response, Db.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_BACKUP_CREATE, params, response, true);
        ui.addHeading(2, dbms);

        ServiceBackup backup;
        try {
            backup = Services.getService(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        if (!params.contains("f")) {
            List<DtoDictionary.Info> dtoList = DtoDictionary.getAll();
            Collection<Object> dtos = new TreeSet<>();
            for (DtoDictionary.Info i : dtoList) {
                dtos.add(i.getDtoClassName());
            }
            ui  .beginFormPost()
                .addInput(
                    VantarKey.ADMIN_BACKUP_FILE_PATH,
                    "df",
                    backup.getDir() + dbms.toString().toLowerCase() + "-"
                        + (new DateTime().formatter().getDateTimeSimple()) + DUMP_FILE_EXT
                )
                .addInputSelectable(VantarKey.ADMIN_EXCLUDE, "ex", dtos, backup.exclude)
                .addInputSelectable(VantarKey.ADMIN_INCLUDE, "in", dtos, backup.include);

            if (dbms.equals(Db.Dbms.MONGO)) {
                ui.addSelect("JSON mode", "jsonmode", new String[] {"EXTENDED", "RELAXED", "SHELL",});
            }

            ui  .addInput(VantarKey.ADMIN_DATE_FROM, "da")
                .addInput(VantarKey.ADMIN_DATE_TO, "db")
                .addSubmit(VantarKey.ADMIN_BACKUP_CREATE_START)
                .addHrefBlock("Backup query result", "/admin/data/backup/mongo/q")
                .finish();
            return;
        }

        String dbDumpFilename = params.getString("df");
        DateTimeRange dateRange = params.getDateRange("da", "db");
        String excludes = params.getString("ex");
        String includes = params.getString("in");
        if (dateRange == null || !dateRange.isValid()) {
            dateRange = null;
        }
        if (dbms.equals(Db.Dbms.MONGO)) {
            JsonMode jsonMode;
            switch (params.getString("jsonmode", "EXTENDED")) {
                case "RELAXED":
                    jsonMode = JsonMode.RELAXED;
                    break;
                case "SHELL":
                    jsonMode = JsonMode.SHELL;
                    break;
                default:
                    jsonMode = JsonMode.EXTENDED;
            }
            MongoBackup.dump(
                ui,
                dbDumpFilename,
                dateRange,
                excludes == null ? null : StringUtil.splitToSet(StringUtil.trim(excludes, ','), ','),
                includes == null ? null : StringUtil.splitToSet(StringUtil.trim(includes, ','), ','),
                jsonMode
            );
        } else if (dbms.equals(Db.Dbms.SQL)) {
            SqlBackup.dump(ui, dbDumpFilename, dateRange);
        } else if (dbms.equals(Db.Dbms.ELASTIC)) {
            ElasticBackup.dump(ui, dbDumpFilename, dateRange);
        }
        ui.finish();
    }

    public static void backupQuery(Params params, HttpServletResponse response, Db.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_BACKUP_CREATE, params, response, true);
        ui.addHeading(2, dbms);

        ServiceBackup backup;
        try {
            backup = Services.getService(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        if (!params.contains("f")) {
            List<DtoDictionary.Info> dtoList = DtoDictionary.getAll();
            Map<String, String> dtos = new HashMap<>(dtoList.size(), 1);
            for (DtoDictionary.Info i : dtoList) {
                dtos.put(i.getDtoClassName(), i.getDtoClassName());
            }
            ui  .beginFormPost()
                .addInput(
                    VantarKey.ADMIN_BACKUP_FILE_PATH,
                    "df",
                    backup.getDir() + dbms.toString().toLowerCase() + "-dto-query" + DUMP_FILE_EXT
                )
                .addSelect("DTO", "dc", dtos)
                .addTextArea(
                    "Query",
                    "query",
                    "{\n" +
                    "    \"pagination\": false,\n" +
                    "    \"page\": 1,\n" +
                    "    \"length\": 100,\n" +
                    "    \"condition\": {\n" +
                    "        \"operator\": \"AND\",\n" +
                    "        \"items\": [\n" +
                    "            {\n" +
                    "                \"col\": \"id\",\n" +
                    "                \"type\": \"BETWEEN\",\n" +
                    "                \"values\": [1,10000]  \n" +
                    "            }\n" +
                    "        ]\n" +
                    "    }\n" +
                    "}")
                .addSubmit(VantarKey.ADMIN_BACKUP_CREATE_START)
                .finish();
            return;
        }

        String dbDumpFilename = params.getString("df");
        QueryBuilder q;
        String dtoClass = params.getString("dc");
        if (dtoClass != null) {
            DtoDictionary.Info info = DtoDictionary.get(dtoClass);
            if (info != null) {
                q = params.getQueryBuilder("query", info.getDtoInstance());
            } else {
                q = params.getQueryBuilder("query");
            }
        } else {
            q = params.getQueryBuilder("query");
        }
        if (q == null) {
            ui.write().addErrorMessage("q=EMPTY").finish();
            return;
        }
        List<ValidationError> errors = q.getErrors();
        if (errors != null) {
            ui.write().addErrorMessage(ValidationError.toString(errors)).finish();
            return;
        }

        MongoBackup.dumpQuery(ui, dbDumpFilename, q);

        ui.finish();
    }

    public static void restore(Params params, HttpServletResponse response, Db.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_RESTORE, params, response, true);
        ui.addHeading(2, dbms);

        ServiceBackup backup;
        try {
            backup = Services.getService(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        if (!params.contains("f")) {
            List<DtoDictionary.Info> dtoList = DtoDictionary.getAll();
            Collection<Object> dtos = new TreeSet<>();
            for (DtoDictionary.Info i : dtoList) {
                dtos.add(i.getDtoClassName());
            }

            String dbmsName = dbms.toString().toLowerCase();
            String[] paths = DirUtil.getDirectoryFiles(backup.getDir());
            List<String> files = new ArrayList<>(paths.length);
            for (int i = paths.length - 1; i >= 0; --i) {
                String path = paths[i];
                if (!StringUtil.contains(path, dbmsName)) {
                    continue;
                }
                files.add(path);
            }

            ui  .beginFormPost()
                .addSelect(VantarKey.ADMIN_BACKUP_FILE_PATH, "df", files)
                .addInputSelectable(VantarKey.ADMIN_EXCLUDE, "ex", dtos)
                .addInputSelectable(VantarKey.ADMIN_INCLUDE, "in", dtos)
                .addCheckbox(VantarKey.ADMIN_RESTORE_DELETE_CURRENT_DATA, "da", true)
                .addCheckbox("JSON keys are not camelCase", "cc", false)
                .addSubmit(VantarKey.ADMIN_RESTORE)
                .finish();
            return;
        }

        String dbDumpFilename = params.getString("df");
        String excludes = params.getString("ex");
        String includes = params.getString("in");
        boolean deleteData = params.isChecked("da");
        boolean camelCaseProperties = params.isChecked("cc");
        if (dbms.equals(Db.Dbms.MONGO)) {
            MongoBackup.restore(
                ui,
                dbDumpFilename,
                deleteData,
                camelCaseProperties,
                excludes == null ? null : StringUtil.splitToSet(StringUtil.trim(excludes, ','), ','),
                includes == null ? null : StringUtil.splitToSet(StringUtil.trim(includes, ','), ',')
            );
        } else if (dbms.equals(Db.Dbms.SQL)) {
            SqlBackup.restore(ui, dbDumpFilename, deleteData);
        } else if (dbms.equals(Db.Dbms.ELASTIC)) {
            ElasticBackup.restore(ui, dbDumpFilename, deleteData);
        }
        ui.finish();
    }

    public static void backupFiles(Params params, HttpServletResponse response, Db.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_BACKUP_FILES, params, response, true);

        ServiceBackup backup;
        try {
            backup = Services.getService(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        ui.addHeading(2, dbms.toString());

        List<String> pathsToDelete = params.getStringList("d");
        if (!ObjectUtil.isEmpty(pathsToDelete) && params.isChecked("confirm")) {
            for (String path : pathsToDelete) {
                String[] parts = StringUtil.splitTrim(path, '/');
                String filename = parts[parts.length - 1];
                File file = new File(path);
                if (file.delete()) {
                    ui.addMessage(Locale.getString(VantarKey.SUCCESS_DELETE) + ": " + filename);
                } else {
                    ui.addErrorMessage(Locale.getString(VantarKey.FAIL_DELETE) + ": " + filename);
                }
            }
            ui.finish();
            return;
        }

        ui  .beginFormPost()
            .addEmptyLine();

        String[] paths = DirUtil.getDirectoryFiles(backup.getDir());
        String dbmsName = dbms.toString().toLowerCase();
        for (int i = paths.length - 1; i >= 0; --i) {
            String path = paths[i];
            if (!StringUtil.contains(path, dbmsName)) {
                continue;
            }
            String[] parts = StringUtil.splitTrim(path, '/');
            String filename = parts[parts.length - 1];

            ui.addBlockNoEscape(
                "div",
                ui.getCheckbox("d", false, path)
                    + ui.getHref("download", "/admin/data/backup/download?" + "file" + "=" + filename, false, false, "")
                    + filename + " " + FileUtil.getSizeReadable(path),
                "box-content"
            ).write();
        }

        ui  .addEmptyLine(2)
            .addCheckbox(VantarKey.ADMIN_DELETE_CONFIRM, "confirm")
            .addSubmit(VantarKey.ADMIN_DELETE)
            .finish();
    }

    public static void upload(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_BACKUP_UPLOAD, params, response, true);

        if (!params.contains("f")) {
            ui  .addEmptyLine(2)
                .beginUploadForm()
                .addFile(VantarKey.ADMIN_BACKUP_UPLOAD_FILE, "file")
                .addSubmit(VantarKey.ADMIN_SUBMIT)
                .finish();
            return;
        }

        ServiceBackup backup;
        try {
            backup = Services.getService(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        try (Params.Uploaded uploaded = params.upload("file")) {
            if (!uploaded.isUploaded() || uploaded.isIoError()) {
                ui.addErrorMessage(Locale.getString(VantarKey.REQUIRED, "file")).finish();
                return;
            }
            if (!uploaded.moveTo(backup.getDir(), uploaded.getOriginalFilename())) {
                ui.addMessage(VantarKey.FAIL_UPLOAD).finish();
                return;
            }
        }

        ui.addMessage(VantarKey.SUCCESS_UPLOAD).finish();
    }

    public static void logs(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_BACKUP, params, response, true);
        try {
            ServiceBackup backup = Services.getService(ServiceBackup.class);
            List<String> logs = backup.getLogs();
            if (ObjectUtil.isEmpty(logs)) {
                ui.addMessage(VantarKey.NO_CONTENT);
            } else {
                for (String log : logs) {
                    ui.addBlock("pre", log);
                }
            }
        } catch (ServiceException e) {
            ui.addErrorMessage(e);
        }
        ui.finish();
    }
}
