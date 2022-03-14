package com.vantar.database.nosql.mongo;

import com.mongodb.client.*;
import com.vantar.database.dto.*;
import com.vantar.exception.DatabaseException;
import com.vantar.util.datetime.DateTimeRange;
import com.vantar.util.file.FileUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.bson.Document;
import java.io.*;
import java.util.*;
import java.util.zip.*;


public class MongoBackup {

    private static final int BULK_ACTION_RECORD_COUNT = 1000;


    public static void dump(String dumpPath, WebUi ui) {
        dump(dumpPath, null, ui);
    }

    public static void dump(String dumpPath, DateTimeRange dateRange, WebUi ui) {
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
            ui.addHeading("Mongo " + database.getName() + " > " + dumpPath).write();
        }

        long r = 0;
        long startTime = System.currentTimeMillis();
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dumpPath)))) {
            for (String collection : MongoConnection.getCollections()) {
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
                ui.addPre(
                    "finished in: " + ((System.currentTimeMillis() - startTime) / 1000) + "s" +
                    "\n" + r + " records" + "\n" + FileUtil.getSizeMb(dumpPath) + "Mb"
                );
            }
        } catch (IOException | DatabaseException e) {
            if (ui != null) {
                ui.addErrorMessage(e);
            }
        }

        if (ui != null) {
            ui.containerEnd().write();
        }
    }

    public static void restore(String zipPath, boolean deleteData) {
        restore(zipPath, deleteData, null);
    }

    public static void restore(String zipPath, boolean deleteData, WebUi ui) {
        MongoDatabase database;
        try {
            database = MongoConnection.getDatabase();
        } catch (DatabaseException e) {
            if (ui != null){
                ui.addErrorMessage(e).write();
            }
            return;
        }

        if (ui != null) {
            ui.addHeading("Mongo " + zipPath + " > " + database.getName()).write();
        }

        long startTime = System.currentTimeMillis();
        long r = 0;

        try (ZipFile zipFile = new ZipFile(zipPath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                long startItemTime = System.currentTimeMillis();
                ZipEntry entry = entries.nextElement();
                String[] parts = StringUtil.split(entry.getName(), '/');
                String collection = StringUtil.replace(parts[parts.length - 1], ".dump", "");

                try (
                    InputStream stream = zipFile.getInputStream(entry);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {

                    if (deleteData) {
                        Mongo.deleteAll(collection);
                    }

                    int i = 1;
                    List<Document> documents = new ArrayList<>(BULK_ACTION_RECORD_COUNT);
                    while (reader.ready()) {
                        if (i++ % BULK_ACTION_RECORD_COUNT == 0) {
                            Mongo.insert(collection, documents);
                            documents = new ArrayList<>(BULK_ACTION_RECORD_COUNT);
                        }
                        documents.add(Document.parse(reader.readLine()));
                    }
                    Mongo.insert(collection, documents);

                    if (ui != null) {
                        long elapsed = (System.currentTimeMillis() - startItemTime) / 1000;
                        ui.addKeyValue(collection, (i - 1) + " records,    " + (elapsed * 10 / 10d) + "s").write();
                        r += i - 1;
                    }
                } catch (IOException | DatabaseException e) {
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

        if (ui != null) {
            ui .addPre(
                    "finished in: " + ((System.currentTimeMillis() - startTime) / 1000) + "s" +
                    "\n" + r + " records" +
                    "\n" + FileUtil.getSizeMb(zipPath) + "Mb"
                )
                .containerEnd()
                .write();
        }
    }
}
