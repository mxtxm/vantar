package com.vantar.service.dbarchive;

import com.vantar.business.ModelMongo;
import com.vantar.common.Settings;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.service.Services;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.datetime.*;
import com.vantar.util.file.FileUtil;
import com.vantar.util.json.Json;
import com.vantar.util.string.StringUtil;
import org.bson.Document;
import java.util.*;
import java.util.concurrent.*;

// todo: insert only to the real dto
// todo: backup restore
// todo: delete archive from webui
public class ServiceDbArchive implements Services.Service {

    private static final int BULK_ACTION_RECORD_COUNT = 1000;
    private static final int MAX_ARCHIVED_DTO_COUNT = 5;

    private volatile boolean serviceUp = false;
    private volatile boolean lastSuccess = true;
    private List<String> logs;

    private ScheduledExecutorService schedule;
    private static Map<Class<?>, String> classToStorage;
    private static String FILE_PATH;

    // > > > service params injected from config
    public String archivePath;
    public Integer startHour;
    public Integer intervalHour;
    // < < <

    // > > > service methods

    @Override
    public void start() {
        schedule = Executors.newSingleThreadScheduledExecutor();

        DateTime startDateTime;
        loadDtoStorage();

        DateTime now = new DateTime();
        DateTimeFormatter f = now.formatter();
        try {
            startDateTime = new DateTime(f.year + "-" + f.month + "-" + f.day + " " + startHour + ":0:0");
        } catch (DateTimeException ignore) {
            return;
        }
        while (startDateTime.isBefore(now)) {
            startDateTime.addHours(intervalHour);
        }

        schedule.scheduleAtFixedRate(this::scan, startDateTime.diffMinutes(now), intervalHour * 60, TimeUnit.MINUTES);
        ServiceLog.log.info(
            "  -> db archive scan: runs at {} ({}minutes) > repeats every {}hours",
            startDateTime.formatter().getDateTime(),
            startDateTime.diffMinutes(now),
            intervalHour
        );
        serviceUp = true;
    }

    @Override
    public void stop() {
        schedule.shutdown();
        serviceUp = false;
    }

    @Override
    public boolean isUp() {
        return serviceUp;
    }

    @Override
    public boolean isOk() {
        return serviceUp
            && lastSuccess
            && schedule != null
            && !schedule.isShutdown()
            && !schedule.isTerminated();
    }

    @Override
    public List<String> getLogs() {
        return logs;
    }

    private void setLog(String msg) {
        if (logs == null) {
            logs = new ArrayList<>(5);
        }
        logs.add(msg);
    }

    public static void setPath() {
        FILE_PATH = Settings.getValue("service.backup.path") + "dbarchive.json";
    }

    // service methods < < <

    private void scan() {
        ServiceLog.log.info("  --> start db-archive");

        Map<String, ArchiveInfo> archives = getArchives();

        DateTime now = new DateTime();
        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
            Archive archive = info.dtoClass.getAnnotation(Archive.class);
            if (archive == null) {
                continue;
            }

            String className = info.getDtoClassName();
            ArchiveInfo archiveInfo = archives.get(className);
            if (archiveInfo == null) {
                archiveInfo = new ArchiveInfo();
                archiveInfo.activeCollection = className;
                archiveInfo.lastCreateDateTime = new DateTime();
                archiveInfo.collections = new HashMap<>(1);
                archives.put(className, archiveInfo);
            }

            String archivePolicy = archive.value();
            if (archivePolicy.contains("D")) {
                if (now.decreaseDays(StringUtil.scrapeInteger(archivePolicy)).isBefore(archiveInfo.lastCreateDateTime)) {
                    archiveByDate(archiveInfo, className, now);
                }
            } else if (archivePolicy.contains("R")) {
                archiveByRecordCount(info.getDtoInstance(), archiveInfo, className, now, archivePolicy);
            }
            now.setToNow();
        }

        FileUtil.write(FILE_PATH, Json.d.toJson(archives));
        ServiceLog.log.info("  <-- end db-archive");
    }

    private void archiveByDate(ArchiveInfo archiveInfo, String className, DateTime now) {
        String newClassName = className + archiveInfo.collections.size();
        ServiceLog.log.info("  --> creating archive {} > {}", className, newClassName);
        try {
            Mongo.renameCollection(className, newClassName);
            archiveInfo.lastCreateDateTime = now;
            archiveInfo.collections.put(newClassName, now.formatter().getDateHm());
            ServiceLog.log.info("  <-- creating archive");
        } catch (DatabaseException e) {
            ServiceLog.log.error(" ! archive {} > {}", className, newClassName, e);
            lastSuccess = true;
            setLog(e.getMessage());
        }
    }

    private void archiveByRecordCount(Dto dto, ArchiveInfo archiveInfo, String className, DateTime now, String archivePolicy) {
        Integer maxRecords = StringUtil.scrapeInteger(archivePolicy);
        if (maxRecords == null) {
            return;
        }
        try {
            if (maxRecords * 2 >= ModelMongo.count(className)) {
                return;
            }
        } catch (VantarException ignore) {
            return;
        }

        String newClassName = className + archiveInfo.collections.size();
        ServiceLog.log.info("  --> creating archive {} > {}", className, newClassName);

        List<Document> documents = new ArrayList<>(BULK_ACTION_RECORD_COUNT);
        int i = 1;
        QueryBuilder q = new QueryBuilder(dto);
        q.sort("id:asc").page(1, maxRecords);
        try {
            MongoQuery mongoQuery = new MongoQuery(q);
            for (Document document : mongoQuery.getResult()) {
                if (i++ % BULK_ACTION_RECORD_COUNT == 0) {
                    Mongo.insert(newClassName, documents);
                    documents = new ArrayList<>(BULK_ACTION_RECORD_COUNT);
                }
                documents.add(document);
            }
            Mongo.insert(newClassName, documents);

            archiveInfo.lastCreateDateTime = now;
            archiveInfo.collections.put(newClassName, now.formatter().getDateHm());
            ServiceLog.log.info("  <-- creating archive");

            archiveByRecordCount(dto, archiveInfo, className, now, archivePolicy);

        } catch (Exception e) {
            ServiceLog.log.error(" ! archive {} > {}", className, newClassName, e);
            lastSuccess = true;
            setLog(e.getMessage());
        }
    }

    public static Map<String, ArchiveInfo> getArchives() {
        setPath();
        if (!FileUtil.exists(FILE_PATH)) {
            return new HashMap<>(MAX_ARCHIVED_DTO_COUNT, 1);
        }
        String json = FileUtil.getFileContent(FILE_PATH);
        return StringUtil.isEmpty(json) ?
            new HashMap<>(MAX_ARCHIVED_DTO_COUNT, 1) :
            Json.d.mapFromJson(json, String.class, ArchiveInfo.class);
    }

    public static void switchCollection(Class<?> dtoClass, String collectionName) {
        Map<String, ArchiveInfo> archives = getArchives();

        ArchiveInfo info = archives.get(dtoClass.getSimpleName());
        if (!dtoClass.getSimpleName().equals(collectionName)
            && (info == null || !info.collections.containsKey(collectionName))) {
            ServiceLog.log.error(" ! invalid collection {} > {}", dtoClass.getSimpleName(), collectionName);
            return;
        }
        info.activeCollection = collectionName;

        FileUtil.write(FILE_PATH, Json.d.toJson(archives));
        loadDtoStorage();
    }

    public static String getStorage(Class<?> dtoClass) {
        if (classToStorage == null) {
            loadDtoStorage();
        }
        return classToStorage.get(dtoClass);
    }

    /**
     * Load archived Dtos current used collection
     */
    private static synchronized void loadDtoStorage() {
        Map<String, ArchiveInfo> archives = getArchives();
        classToStorage = new HashMap<>(MAX_ARCHIVED_DTO_COUNT, 1);
        for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
            Archive archive = info.dtoClass.getAnnotation(Archive.class);
            if (archive == null) {
                continue;
            }
            String className = info.getDtoClassName();
            ArchiveInfo archiveInfo = archives.get(className);
            classToStorage.put(info.dtoClass, archiveInfo == null ? className : archiveInfo.activeCollection);
        }
    }


    public static class ArchiveInfo {

        public String activeCollection;
        public DateTime lastCreateDateTime;
        // <name, date>
        public Map<String, String> collections;
    }
}