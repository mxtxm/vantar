package com.vantar.database.sql;

import com.vantar.business.CommonRepoSql;
import com.vantar.common.Settings;
import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.exception.*;
import com.vantar.util.datetime.DateTimeRange;
import com.vantar.util.file.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;


public class SqlBackup {

    public static void dump(WebUi ui, String dumpPath) {
        dump(ui, dumpPath, null);
    }

    public static void dump(WebUi ui, String dumpPath, DateTimeRange dateRange) {
        if (ui != null) {
            ui.addHeading(2, "PostgreSQL " + Settings.sql().getDbDatabase() + " > " + dumpPath).write();
        }

        String tempDir = DirUtil.makeTempDirectory();
        if (tempDir == null) {
            if (ui != null) {
                ui.addErrorMessage("امکان ساختن دایرکتوری موقت نبود.").write();
            }
            return;
        }

        long startTime = System.currentTimeMillis();

        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repo = new CommonRepoSql(connection);

            for (DtoDictionary.Info info : DtoDictionary.getAll(Db.Dbms.SQL)) {
                long startItemTime = System.currentTimeMillis();
                Dto dto = info.getDtoInstance();
                String table = dto.getStorage();

                String dateField = null;
                if (dateRange != null) {
                    String dateFieldUpdate = null;
                    for (String name : dto.getProperties()) {
                        if (dto.hasAnnotation(name, CreateTime.class)) {
                            dateField = name;
                        }
                        if (dto.hasAnnotation(name, UpdateTime.class)) {
                            dateFieldUpdate = name;
                        }
                    }
                    if (dateFieldUpdate != null) {
                        dateField = dateFieldUpdate;
                    }
                }

                try {
                    if (dateRange == null || dateField == null) {
                        repo.execute("COPY (SELECT * FROM " + table + ") TO '" + tempDir + table + ".dump';");
                    } else {
                        repo.execute(
                            "COPY (SELECT * FROM " + table + " " + dateField +
                                " BETWEEN '" + dateRange.dateMin.formatter().getDateTime() + "' AND '" +
                                dateRange.dateMax.formatter().getDateTime() + "') TO '" + tempDir + table + ".dump';"
                        );
                    }
                } catch (VantarException e) {
                    if (ui != null) {
                        ui.addErrorMessage(e);
                    }
                }

                long elapsed = (System.currentTimeMillis() - startItemTime) / 1000;
                if (ui != null) {
                    ui.addKeyValue(table, (elapsed * 10 / 10d) + "s").write();
                }
            }

            FileUtil.zip(tempDir, dumpPath);
            DirUtil.removeDirectory(tempDir);

            if (ui != null) {
                ui.addBlock("pre",
                    "finished in: " + ((System.currentTimeMillis() - startTime) / 1000) + "s" +
                        "\n" + FileUtil.getSizeReadable(dumpPath)
                );
            }
        }
        if (ui != null) {
            ui.blockEnd().write();
        }
    }

    public static void restore(WebUi ui, String zipPath, boolean deleteData) {
        ui.addHeading(2, "PostgreSQL " + zipPath + " > " + Settings.sql().getDbDatabase()).write();

        String tmpDir = DirUtil.makeTempDirectory();
        FileUtil.unzip(zipPath, tmpDir);
        DirUtil.giveAllPermissions(tmpDir);

        long startTime = System.currentTimeMillis();

        try (SqlConnection connection = new SqlConnection()) {
            CommonRepoSql repo = new CommonRepoSql(connection);

            for (String dumpPath : DirUtil.getDirectoryFiles(tmpDir)) {
                if (FileUtil.getSize(dumpPath) < 2) {
                    continue;
                }

                long startCollectionTime = System.currentTimeMillis();
                String[] parts = StringUtil.split(dumpPath, '/');
                String table = StringUtil.replace(parts[parts.length - 1], ".dump", "");

                try {
                    if (deleteData) {
                        repo.purgeData(table);
                    }
                    repo.execute("COPY " + table + " FROM '" + dumpPath + "';");
                    long elapsed = (System.currentTimeMillis() - startCollectionTime) / 1000;
                    ui.addKeyValue(table, (elapsed * 10 / 10d) + "s").write();
                } catch (VantarException e) {
                    ui.addErrorMessage(e);
                }
            }

            ui  .addBlock("pre",
                "finished in: " + ((System.currentTimeMillis() - startTime) / 1000) + "s" +
                    "\n" + FileUtil.getSizeReadable(zipPath)
                )
                .blockEnd()
                .write();
        } finally {
            DirUtil.removeDirectory(tmpDir);
        }
    }
}
