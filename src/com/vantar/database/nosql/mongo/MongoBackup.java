package com.vantar.database.nosql.mongo;

import com.mongodb.client.*;
import com.vantar.admin.model.database.AdminDatabase;
import com.vantar.database.dto.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.service.Services;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.datetime.DateTimeRange;
import com.vantar.util.file.FileUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.WebUi;
import org.bson.Document;
import java.io.*;
import java.util.*;
import java.util.zip.*;


public class MongoBackup {

    private static final int BULK_ACTION_RECORD_COUNT = 1000;


    public static void dump(String dumpPath, WebUi ui) {
        dump(dumpPath, null, null, ui);
    }

    public static void dump(String dumpPath, DateTimeRange dateRange, Set<String> excludeDtos, WebUi ui) {
        MongoDatabase database;
        try {
            database = MongoConnection.getDatabase();
        } catch (DatabaseException e) {
            if (ui != null) {
                ui.addErrorMessage(e).write();
            }
            return;
        }

        if (ui != null) {
            ui.addHeading(2, "Mongo " + database.getName() + " > " + dumpPath).write();
        }

        long r = 0;
        long startTime = System.currentTimeMillis();
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dumpPath)))) {
            for (String collection : MongoConnection.getCollections()) {
                if (excludeDtos != null && excludeDtos.contains(collection)) {
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

                for (Document document : q) {
                    zip.write((document.toJson() + "\n").getBytes());
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
                ui.addBlock("pre",
                    "finished in: " + ((System.currentTimeMillis() - startTime) / 1000) + "s" +
                    "\n" + r + " records" + "\n" + FileUtil.getSizeReadable(dumpPath)
                );
            }
        } catch (IOException | DatabaseException e) {
            if (ui != null) {
                ui.addErrorMessage(e);
            }
        }

        if (ui != null) {
            ui.finish();
        }
    }

    public static void dumpQuery(String dumpPath, QueryBuilder q, WebUi ui) {
        MongoDatabase database;
        try {
            database = MongoConnection.getDatabase();
        } catch (DatabaseException e) {
            if (ui != null) {
                ui.addErrorMessage(e).write();
            }
            return;
        }

        if (ui != null) {
            ui.addHeading(2, "Mongo " + database.getName() + " > " + dumpPath).write();
        }

        int r = 0;
        long startTime = System.currentTimeMillis();
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dumpPath)))) {

            zip.putNextEntry(new ZipEntry(q.getDto().getStorage() + ".dump"));
            MongoQuery mongoQuery = new MongoQuery(q);
            for (Document document : mongoQuery.getResult()) {
                zip.write((document.toJson() + "\n").getBytes());
                ++r;
            }
            zip.closeEntry();

            if (ui != null) {
                ui.addBlock("pre",
                    "finished in: " + ((System.currentTimeMillis() - startTime) / 1000) + "s" +
                        "\n" + r + " records" + "\n" + FileUtil.getSizeReadable(dumpPath)
                );
            }
        } catch (IOException | DatabaseException e) {
            if (ui != null) {
                ui.addErrorMessage(e);
            }
        }

        if (ui != null) {
            ui.finish();
        }
    }

    public static void restore(String zipPath, boolean deleteData) {
        restore(zipPath, deleteData, false, null);
    }

    public static void restore(String zipPath, boolean deleteData, boolean toCamelCase, WebUi ui) {
        ServiceLog.log.info("---> RESTORING database");
        MongoDatabase database;
        try {
            database = MongoConnection.getDatabase();
        } catch (DatabaseException e) {
            if (ui != null) {
                ui.addErrorMessage(e).write();
            }
            return;
        }

        ServiceLog logService;
        try {
            logService = Services.getService(ServiceLog.class);
            logService.pause();
        } catch (ServiceException e) {
            logService = null;
        }

        if (ui != null) {
            ui.addHeading(2, "Mongo " + zipPath + " > " + database.getName()).write();
        }

        long startTime = System.currentTimeMillis();
        long r = 0;

        if (deleteData) {
            try {
                for (String s : MongoConnection.getCollections()) {
                    try {
                        Mongo.deleteAll(s);
                    } catch (DatabaseException e) {
                        if (ui != null) {
                            ui.addErrorMessage(e);
                        }
                    }
                }
            } catch (DatabaseException e) {
                if (ui != null) {
                    ui.addErrorMessage(e);
                }
            }
        }

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
                String line = null;
                try (
                    InputStream stream = zipFile.getInputStream(entry);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {

                    int i = 1;
                    List<Document> documents = new ArrayList<>(BULK_ACTION_RECORD_COUNT);
                    while (reader.ready()) {
                        if (i++ % BULK_ACTION_RECORD_COUNT == 0) {
                            Mongo.insert(collection, documents);
                            documents = new ArrayList<>(BULK_ACTION_RECORD_COUNT);
                        }
                        line = reader.readLine();
                        if (toCamelCase) {
                            line = jsonToCamelCaseProperties(line);
                        }
                        documents.add(Document.parse(line));
                    }
                    Mongo.insert(collection, documents);
                    long seq = Mongo.Sequence.setToMax(collection);

                    if (ui != null) {
                        long elapsed = (System.currentTimeMillis() - startItemTime) / 1000;
                        ui.addKeyValue(collection, (i - 1) + " records - seq=" + seq + ",    "
                            + (elapsed * 10 / 10d) + "s").write();
                        r += i - 1;
                    }
                } catch (Exception e) {
                    ServiceLog.log.info("! {}\n{}\n", entry.getName(), line, e);
                    if (ui != null) {
                        ui.addErrorMessage(e);
                    }
                }
            }
        } catch (IOException e) {
            if (ui != null) {
                ui.addErrorMessage(e).write();
            }
        }

        if (logService != null) {
            logService.resume();
        }

        if (ui != null) {
            ServiceLog.log.info("<--- finished RESTORING database");
            ui .addBlock("pre",
                    "finished in: " + ((System.currentTimeMillis() - startTime) / 1000) + "s" +
                    "\n" + r + " records" +
                    "\n" + FileUtil.getSizeReadable(zipPath)
                )
                .blockEnd()
                .write();

            try {
                AdminDatabase.createMongoIndex(ui, true);
            } catch (Exception e) {
                ServiceLog.log.error("! restore failed to create database indexes.", e);
                ui.addErrorMessage("Failed to create indexes.");
            }

            ui.finish();
        }
    }

    private static String jsonToCamelCaseProperties(String json) {
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
                    if (peek != ':') {
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
