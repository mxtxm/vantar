package com.vantar.service.backup;

import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.nosql.elasticsearch.ElasticBackup;
import com.vantar.database.nosql.mongo.MongoBackup;
import com.vantar.database.sql.SqlBackup;
import com.vantar.exception.DateTimeException;
import com.vantar.service.Services;
import com.vantar.service.log.LogEvent;
import com.vantar.util.collection.FixedArrayList;
import com.vantar.util.datetime.*;
import com.vantar.util.file.*;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.*;


public class ServiceBackup implements Services.Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceBackup.class);
    private ScheduledExecutorService schedule;
    private final List<String> logs = new FixedArrayList<>(100);
    private DateTime lastRun;
    private DateTime startDateTime;

    public Boolean onEndSetNull;
    public String dbms;
    public Integer startHour;
    public Integer intervalHour;
    public Integer deleteOldFilesAfterDays;
    public String path;


    public void start() {
        schedule = Executors.newSingleThreadScheduledExecutor();

        DateTime now = new DateTime();
        DateTimeFormatter f = now.formatter();
        try {
            startDateTime = new DateTime(f.year + "-" + f.month + "-" + f.day + " " + startHour + ":0:0");
        } catch (DateTimeException ignore) {

        }
        while (startDateTime.isBefore(now)) {
            startDateTime.addHours(intervalHour);
        }

        schedule.scheduleAtFixedRate(this::create, startDateTime.diffMinutes(now), intervalHour * 60, TimeUnit.MINUTES);
        log.info(
            "    >> backup: runs at {} ({}minutes) > repeats every {}hours",
            startDateTime.formatter().getDateTime(),
            startDateTime.diffMinutes(now),
            intervalHour
        );
    }

    public void stop() {
        schedule.shutdown();
        try {
            schedule.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {

        }
    }

    public boolean onEndSetNull() {
        return onEndSetNull;
    }

    private void create() {
        log.info(" >> start creating database backup");
        startDateTime = null;
        lastRun = new DateTime();
        dbms = dbms.toUpperCase();
        String tail = "-" + (lastRun.formatter().getDateTimeSimple()) + ".dump";
        logs.add("begin: " + lastRun.formatter().getDateTime());

        try {
            if (StringUtil.contains(dbms, DtoDictionary.Dbms.MONGO.toString())) {
                LogEvent.beat(this.getClass(), "Mongo backed-up start...");
                MongoBackup.dump(path + "mongo" + tail, null, null);
                LogEvent.beat(this.getClass(), "Mongo backed-up");
                logs.add("success: " + lastRun.formatter().getDateTime() + " mongo");
            }
            if (StringUtil.contains(dbms, DtoDictionary.Dbms.SQL.toString())) {
                LogEvent.beat(this.getClass(), "SQL backed-up start...");
                SqlBackup.dump(path + "sql" + tail, null, null);
                LogEvent.beat(this.getClass(), "SQL backed-up");
                logs.add("success: " + lastRun.formatter().getDateTime() + " sql");
            }
            if (StringUtil.contains(dbms, DtoDictionary.Dbms.ELASTIC.toString())) {
                LogEvent.beat(this.getClass(), "Elastic backed-up start...");
                ElasticBackup.dump(path + "elastic" + tail, null, null);
                LogEvent.beat(this.getClass(), "Elastic backed-up");
                logs.add("success: " + lastRun.formatter().getDateTime() + " elastic");
            }

            if (deleteOldFilesAfterDays != null) {
                DateTime thresholdDate = lastRun.decreaseDays(deleteOldFilesAfterDays);
                DirUtil.browseByExtension(path, "dump", file -> {
                    String[] parts = StringUtil.split(
                        StringUtil.remove(
                            file.getName(),
                            ".dump", ".zip", "elastic-", "sql-", "mongo-"
                        ),
                        '-'
                    );
                    try {
                        DateTime fileDate = new DateTime(parts[0] + '-' + parts[1] + '-' + parts[2]);
                        if (fileDate.isBefore(thresholdDate)) {
                            FileUtil.removeFile(file.getAbsolutePath());
                            logs.add("removed: " + file.getAbsolutePath());
                            log.info(" > removed old backup {}", file.getAbsolutePath());
                        }
                    } catch (Exception ignore) {

                    }
                });

                log.info(" << end creating database backup");
            }
        } catch (Exception e) {
            logs.add("FAILED: " + lastRun.formatter().getDateTime());
            log.error(" ! creating backup failed", e);
        }
    }

    public String getPath() {
        return path;
    }

    public List<String> getLogs() {
        return logs;
    }

    public String getLastRun() {
        return lastRun == null ? "Run pending..." : lastRun.formatter().getDateTime();
    }

    public String getNextRun() {
        return startDateTime == null ?
            lastRun.addHours(intervalHour).formatter().getDateTime() :
            startDateTime.formatter().getDateTime();
    }
}