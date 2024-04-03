package com.vantar.admin.model.database.dbms.backup;

import com.vantar.admin.model.index.Admin;
import com.vantar.database.common.ValidationError;
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
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;


public class AdminBackup {

    private static final String DUMP_FILE_EXT = ".dump";


    public static void backup(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_BACKUP_CREATE, params, response, true);
        ui.addHeading(2, dbms);

        ServiceBackup backup;
        try {
            backup = Services.getService(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        if (!params.isChecked("f")) {
            List<DtoDictionary.Info> dtoList = DtoDictionary.getAll();
            Collection<Object> dtos = new TreeSet<>();
            for (DtoDictionary.Info i : dtoList) {
                dtos.add(i.getDtoClassName());
            }
            ui  .beginFormPost()
                .addInput(
                    VantarKey.ADMIN_BACKUP_FILE_PATH,
                    "df",
                    backup.getPath() + dbms.toString().toLowerCase() + "-"
                        + (new DateTime().formatter().getDateTimeSimple()) + DUMP_FILE_EXT
                )
                .addInputSelectable(VantarKey.ADMIN_EXCLUDE, "ex", dtos, backup.exclude)
                .addInputSelectable(VantarKey.ADMIN_INCLUDE, "in", dtos, backup.include)
                .addInput(VantarKey.ADMIN_DATE_FROM, "da")
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
        if (dbms.equals(DtoDictionary.Dbms.MONGO)) {
            MongoBackup.dump(
                dbDumpFilename,
                dateRange,
                excludes == null ? null : StringUtil.splitToSet(StringUtil.trim(excludes, ','), ','),
                includes == null ? null : StringUtil.splitToSet(StringUtil.trim(includes, ','), ','),
                ui
            );
        } else if (dbms.equals(DtoDictionary.Dbms.SQL)) {
            SqlBackup.dump(dbDumpFilename, dateRange, ui);
        } else if (dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
            ElasticBackup.dump(dbDumpFilename, dateRange, ui);
        }
        ui.finish();
    }

    public static void backupQuery(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_BACKUP_CREATE, params, response, true);
        ui.addHeading(2, dbms);

        ServiceBackup backup;
        try {
            backup = Services.getService(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        if (!params.isChecked("f")) {
            List<DtoDictionary.Info> dtoList = DtoDictionary.getAll();
            Map<String, String> dtos = new HashMap<>(dtoList.size(), 1);
            for (DtoDictionary.Info i : dtoList) {
                dtos.put(i.getDtoClassName(), i.getDtoClassName());
            }
            ui  .beginFormPost()
                .addInput(
                    VantarKey.ADMIN_BACKUP_FILE_PATH,
                    "df",
                    backup.getPath() + dbms.toString().toLowerCase() + "-dto-query" + DUMP_FILE_EXT
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

        MongoBackup.dumpQuery(dbDumpFilename, q, ui);

        ui.finish();
    }

    public static void restore(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_RESTORE, params, response, true);
        ui.addHeading(2, dbms);

        ServiceBackup backup;
        try {
            backup = Services.getService(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        if (!params.isChecked("f")) {
            List<DtoDictionary.Info> dtoList = DtoDictionary.getAll();
            Collection<Object> dtos = new TreeSet<>();
            for (DtoDictionary.Info i : dtoList) {
                dtos.add(i.getDtoClassName());
            }

            List<String> files = new ArrayList<>(100);
            String dbmsName = dbms.toString().toLowerCase();
            for (String path : DirUtil.getDirectoryFiles(backup.getPath())) {
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
        if (dbms.equals(DtoDictionary.Dbms.MONGO)) {
            MongoBackup.restore(
                dbDumpFilename,
                deleteData,
                camelCaseProperties,
                excludes == null ? null : StringUtil.splitToSet(StringUtil.trim(excludes, ','), ','),
                includes == null ? null : StringUtil.splitToSet(StringUtil.trim(includes, ','), ','),
                ui
            );
        } else if (dbms.equals(DtoDictionary.Dbms.SQL)) {
            SqlBackup.restore(dbDumpFilename, deleteData, ui);
        } else if (dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
            ElasticBackup.restore(dbDumpFilename, deleteData, ui);
        }
        ui.finish();
    }

    public static void backupFiles(Params params, HttpServletResponse response, DtoDictionary.Dbms dbms) throws FinishException {
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
                String[] parts = StringUtil.split(path, '/');
                String filename = parts[parts.length - 1];
                File file = new File(path);
                if (file.delete()) {
                    ui.addMessage(Locale.getString(VantarKey.DELETE_SUCCESS) + ": " + filename);
                } else {
                    ui.addErrorMessage(Locale.getString(VantarKey.DELETE_FAIL) + ": " + filename);
                }
            }
            ui.finish();
            return;
        }

        ui  .beginFormPost()
            .addEmptyLine();

        for (String path : DirUtil.getDirectoryFiles(backup.getPath())) {
            if (!path.endsWith(DUMP_FILE_EXT)) {
                continue;
            }
            String[] parts = StringUtil.split(path, '/');
            String filename = parts[parts.length - 1];

            ui.addBlockNoEscape(
                "div",
                ui.getCheckbox("d", false, path)
                    + ui.getHref("download", "/admin/data/backup/download?" + "file" + "=" + filename, false, false, "")
                    + filename + " (" + FileUtil.getSizeReadable(path) + "Mb)",
                "box-content"
            ).write();
        }

        ui  .addEmptyLine(2)
            .addCheckbox(VantarKey.ADMIN_DELETE_DO, "confirm")
            .addSubmit(VantarKey.ADMIN_DELETE)
            .finish();
    }

    public static void upload(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_BACKUP_UPLOAD, params, response, true);

        if (!params.isChecked("f")) {
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
            if (!uploaded.moveTo(backup.getPath(), uploaded.getOriginalFilename())) {
                ui.addMessage(VantarKey.UPLOAD_FAIL).finish();
                return;
            }
        }

        ui.addMessage(VantarKey.UPLOAD_SUCCESS).finish();
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
