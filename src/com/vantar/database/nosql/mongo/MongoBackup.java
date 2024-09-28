package com.vantar.database.nosql.mongo;

import com.mongodb.client.*;
import com.vantar.admin.database.dbms.indexing.AdminDatabaseIndex;
import com.vantar.common.VantarParam;
import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.service.Services;
import com.vantar.service.backup.ServiceBackup;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.datetime.*;
import com.vantar.util.file.FileUtil;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.WebUi;
import org.bson.Document;
import org.bson.json.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;


public class MongoBackup {

    public static void dump(WebUi ui, String dumpPath, DateTimeRange dateRange, Collection<String> excludes
        , Collection<String> includes, JsonMode jsonMode) {

        dump(ui, dumpPath, dateRange, excludes, includes, Db.mongo, jsonMode);
    }

    public static void dump(WebUi ui, String dumpPath, DateTimeRange dateRange, Collection<String> excludes
        , Collection<String> includes, DbMongo db, JsonMode jsonMode) {

        if (jsonMode == null) {
            jsonMode = JsonMode.EXTENDED;
        }

        MongoDatabase database;
        try {
            database = db.getDatabase();
        } catch (VantarException e) {
            if (ui != null) {
                ui.addErrorMessage(e).write();
            }
            return;
        }

        if (ui != null) {
            ui.addHeading(3, database.getName() + " > " + dumpPath).write();
        }

        long r = 0;
        long startTime = System.currentTimeMillis();
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dumpPath)))) {
            for (String collection : db.getCollections()) {
                if (ObjectUtil.isNotEmpty(excludes) && excludes.contains(collection)) {
                    continue;
                }
                if (ObjectUtil.isNotEmpty(includes) && !includes.contains(collection)) {
                    continue;
                }

                long startCollectionTime = System.currentTimeMillis();
                zip.putNextEntry(new ZipEntry(collection + ".dump"));
                int l = 0;
                FindIterable<Document> q;

                if (dateRange == null) {
                    q = database.getCollection(collection).find();
                } else {
                    DtoDictionary.Info info = DtoDictionary.get(collection);

                    String dateField = null;
                    if (info != null) {
                        Dto dto = info.getDtoInstance();
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

                    if (info == null || dateField == null) {
                        q = database.getCollection(collection).find();
                    } else {
                        q = database.getCollection(collection).find(
                            new Document(dateField, new Document("$gte", dateRange.dateMin.getAsTimestamp())
                                .append("$lte", dateRange.dateMax.getAsTimestamp()))
                        );
                    }
                }
                q.allowDiskUse(true);
                for (Document document : q) {
                    zip.write((document.toJson(JsonWriterSettings.builder().outputMode(jsonMode).build()) + "\n").getBytes());
                    ++l;
                }
                long elapsed = (System.currentTimeMillis() - startCollectionTime) / 1000;
                if (ui != null) {
                    ui.addKeyValue(collection, l + " records,    " + (elapsed * 10 / 10d) + "s").write();
                }
                r += l;
            }
            zip.closeEntry();

            if (ui != null) {
                ui  .addMessage(r + " records")
                    .addMessage(FileUtil.getSizeReadable(dumpPath))
                    .addMessage(DateTimeFormatter.secondsToDateTime((System.currentTimeMillis() - startTime) / 1000));
            }
        } catch (IOException | VantarException e) {
            if (ui != null) {
                ui.addErrorMessage(e).write();
            }
        }
    }

    public static void dumpQuery(WebUi ui, String dumpPath, QueryBuilder q) {
        dumpQuery(ui, dumpPath, q, Db.mongo);
    }

    public static void dumpQuery(WebUi ui, String dumpPath, QueryBuilder q, DbMongo db) {
        MongoDatabase database;
        try {
            database = db.getDatabase();
        } catch (VantarException e) {
            if (ui != null) {
                ui.addErrorMessage(e).write();
            }
            return;
        }

        if (ui != null) {
            ui.addHeading(3, database.getName() + " > " + dumpPath).write();
        }

        int r = 0;
        long startTime = System.currentTimeMillis();
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dumpPath)))) {

            zip.putNextEntry(new ZipEntry(q.getDto().getStorage() + ".dump"));
            for (Document document : db.getResult(new MongoQuery(q))) {
                zip.write((document.toJson(JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build()) + "\n").getBytes());
                ++r;
            }
            zip.closeEntry();

            if (ui != null) {
                ui  .addMessage(r + " records")
                    .addMessage(FileUtil.getSizeReadable(dumpPath))
                    .addMessage(DateTimeFormatter.secondsToDateTime((System.currentTimeMillis() - startTime) / 1000));
            }
        } catch (IOException | VantarException e) {
            if (ui != null) {
                ui.addErrorMessage(e).write();
            }
        }
    }

    public static void restore(WebUi ui, String zipPath, boolean deleteData, boolean toCamelCase
        , Collection<String> excludes, Collection<String> includes) {

        restore(ui, zipPath, deleteData, toCamelCase, excludes, includes, Db.mongo);
    }

    public static void restore(WebUi ui, String zipPath, boolean deleteData, boolean toCamelCase
        , Collection<String> excludes, Collection<String> includes, DbMongo db) {

        MongoDatabase database;
        try {
            database = db.getDatabase();
        } catch (VantarException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        ServiceBackup serviceBackup;
        try {
            serviceBackup = Services.getService(ServiceBackup.class);
        } catch (ServiceException e) {
            ui.addErrorMessage("ServiceBackup is off").finish();
            return;
        }

        ServiceLog.log.info("---> RESTORING database");
        ServiceLog logService;
        try {
            logService = Services.getService(ServiceLog.class);
            logService.pause();
        } catch (ServiceException e) {
            logService = null;
        }

        long startTime = System.currentTimeMillis();
        long r = 0;
        Integer bulkActionRecordCount = serviceBackup.bulkActionRecordCount;
        if (bulkActionRecordCount == null) {
            bulkActionRecordCount = 10;
        }

        if (deleteData) {
            ui.addHeading(3, VantarKey.ADMIN_DATA_PURGE).write();
            try {
                for (String collection : db.getCollections()) {
                    if (ObjectUtil.isNotEmpty(excludes) && excludes.contains(collection)) {
                        continue;
                    }
                    if (ObjectUtil.isNotEmpty(includes) && !includes.contains(collection)) {
                        continue;
                    }
                    try {
                        db.deleteAll(collection);
                        ui.addMessage("Purged: " + collection).write();
                    } catch (VantarException e) {
                        ui.addErrorMessage(e).write();
                    }
                }
            } catch (VantarException e) {
                ui.addErrorMessage(e).write();
            }
        }

        ui.addHeading(3, zipPath + " > " + database.getName()).write();
        try (ZipFile zipFile = new ZipFile(zipPath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                long startItemTime = System.currentTimeMillis();
                ZipEntry entry = entries.nextElement();
                if (entry.getName().contains("_sequence")) {
                    continue;
                }

                String[] parts = StringUtil.split(entry.getName(), '/');
                String collection = StringUtil.replace(parts[parts.length - 1], ".dump", "");
                collection = StringUtil.toStudlyCase(collection);
                if (excludes != null && excludes.contains(collection)) {
                    continue;
                }
                if (includes != null && !includes.contains(collection)) {
                    continue;
                }
                ServiceLog.log.info("   {} -> db", collection);

                String line = null;
                try (
                    InputStream stream = zipFile.getInputStream(entry);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {

                    int i = 1;
                    List<Document> documents = new ArrayList<>(bulkActionRecordCount);
                    while (reader.ready()) {
                        if (i++ % bulkActionRecordCount == 0) {
                            db.insert(collection, documents);
                            documents = new ArrayList<>(bulkActionRecordCount);
                        }
                        line = reader.readLine();
                        if (toCamelCase) {
                            line = jsonKeysToCamelCase(line);
                        }
                        documents.add(Document.parse(line));
                    }
                    db.insert(collection, documents);
                    long seq = db.autoIncrementSetToMax(collection);

                    long elapsed = (System.currentTimeMillis() - startItemTime) / 1000;
                    ui.addKeyValue(
                        collection,
                        "records=" + (i - 1) + " - seq=" + seq + " - " + (elapsed * 10 / 10d) + "s"
                    ).write();

                    r += i - 1;
                } catch (Exception e) {
                    ServiceLog.log.info("! {}\n{}\n", entry.getName(), line, e);
                    ui.addErrorMessage(e).write();
                }
            }
        } catch (IOException e) {
            ui.addErrorMessage(e).write();
        }
        ServiceLog.log.info("<--- finished RESTORING database");

        ui  .addMessage(r + " records")
            .addMessage(FileUtil.getSizeReadable(zipPath))
            .addMessage(DateTimeFormatter.secondsToDateTime((System.currentTimeMillis() - startTime) / 1000))
            .write();

        try {
            ui.addHeading(3, Locale.getString(VantarKey.ADMIN_DATABASE_INDEX_CREATE, Db.Dbms.MONGO)).write();
            AdminDatabaseIndex.createIndexMongo(ui, true, excludes, includes);
        } catch (Exception e) {
            ServiceLog.log.error("! restore failed to create database indexes.", e);
            ui.addErrorMessage(e);
        }

        if (logService != null) {
            logService.resume();
        }
    }

    private static String jsonKeysToCamelCase(String json) {
        json = StringUtil.replace(json, "\\\"", "==-+-=-=-+-==");
        StringBuilder buffer = new StringBuilder();
        boolean isIn = false;
        StringBuilder tokenBuffer = new StringBuilder();
        char[] charArray = json.toCharArray();
        for (int i = 0, l = charArray.length; i < l; i++) {
            char c = charArray[i];
            if (!isIn && c == '"') {
                isIn = true;
                tokenBuffer = new StringBuilder();
                continue;
            }

            if (isIn) {
                if (c == '"') {
                    isIn = false;
                    char peek = charArray[i+1];
                    if (peek != VantarParam.SEPARATOR_KEY_VAL) {
                        buffer.append('"');
                        buffer.append(tokenBuffer);
                        buffer.append('"');
                        continue;
                    }

                    String token = tokenBuffer.toString();
                    if (!token.startsWith("_") && !token.startsWith("$")) {
                        token = StringUtil.toCamelCase(token);
                    }
                    buffer.append('"');
                    buffer.append(token);
                    buffer.append("\"");
                    continue;
                }
                tokenBuffer.append(c);
                continue;
            }

            buffer.append(c);
        }
        return StringUtil.replace(buffer.toString(), "==-+-=-=-+-==", "\\\"");
    }
}
