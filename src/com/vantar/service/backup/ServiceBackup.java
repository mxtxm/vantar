package com.vantar.service.backup;

import com.vantar.database.common.Db;
import com.vantar.database.nosql.elasticsearch.ElasticBackup;
import com.vantar.database.nosql.mongo.MongoBackup;
import com.vantar.database.sql.SqlBackup;
import com.vantar.exception.DateTimeException;
import com.vantar.service.Services;
import com.vantar.service.log.*;
import com.vantar.util.collection.FixedArrayList;
import com.vantar.util.datetime.*;
import com.vantar.util.file.*;
import com.vantar.util.string.StringUtil;
import java.util.*;
import java.util.concurrent.*;


public class ServiceBackup implements Services.Service {

    private volatile boolean pause = false;
    private volatile boolean serviceUp = false;
    private volatile boolean lastSuccess = true;

    private ScheduledExecutorService schedule;
    private DateTime lastRun;
    private DateTime startDateTime;
    private List<String> logs;

    // > > > service params injected from config
    public String dbms;
    public Integer startHour;
    public Integer intervalHour;
    public Integer deleteOldFilesAfterDays;
    public String path;
    public String exclude;
    public String include;
    // < < <

    // > > > service methods

    @Override
    public void start() {
        pause = false;
        schedule = Executors.newSingleThreadScheduledExecutor();

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

        schedule.scheduleAtFixedRate(this::create, startDateTime.diffMinutes(now), intervalHour * 60, TimeUnit.MINUTES);
        ServiceLog.log.info(
            "  -> backup: runs at {} ({}minutes) > repeats every {}hours",
            startDateTime.formatter().getDateTime(),
            startDateTime.diffMinutes(now),
            intervalHour
        );
        serviceUp = true;
    }

    @Override
    public void stop() {
        schedule.shutdown();
        try {
            schedule.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {

        }
        serviceUp = false;
    }

    @Override
    public void pause() {
        pause = true;
    }

    @Override
    public void resume() {
        pause = false;
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
    public boolean isPaused() {
        return pause;
    }

    @Override
    public List<String> getLogs() {
        return logs;
    }

    private void setLogs(String msg) {
        if (logs == null) {
            logs = new FixedArrayList<>(100);
        }
        logs.add(msg);
    }

    // service methods < < <

    private void create() {
        if (pause) {
            return;
        }
        ServiceLog.log.info("  --> start creating database backup");
        lastSuccess = true;
        startDateTime = null;
        lastRun = new DateTime();
        dbms = dbms.toUpperCase();
        String tail = "-" + (lastRun.formatter().getDateTimeSimple()) + ".dump";
        setLogs("begin: " + lastRun.formatter().getDateTime());

        Set<String> excludeDtos = StringUtil.splitToSetTrim(exclude, ',');
        Set<String> includeDtos = StringUtil.splitToSetTrim(include, ',');
        try {
            if (StringUtil.contains(dbms, Db.Dbms.MONGO.toString())) {
                Beat.set(this.getClass(), "Mongo backup start...");
                MongoBackup.dump(null, path + "mongo" + tail, null, excludeDtos, includeDtos);
                Beat.set(this.getClass(), "Mongo backup");
                setLogs("success: " + lastRun.formatter().getDateTime() + " mongo");
            }
            if (StringUtil.contains(dbms, Db.Dbms.SQL.toString())) {
                Beat.set(this.getClass(), "SQL backup start...");
                SqlBackup.dump(null, path + "sql" + tail, null);
                Beat.set(this.getClass(), "SQL backup");
                setLogs("success: " + lastRun.formatter().getDateTime() + " sql");
            }
            if (StringUtil.contains(dbms, Db.Dbms.ELASTIC.toString())) {
                Beat.set(this.getClass(), "Elastic backup start...");
                ElasticBackup.dump(null, path + "elastic" + tail, null);
                Beat.set(this.getClass(), "Elastic backup");
                setLogs("success: " + lastRun.formatter().getDateTime() + " elastic");
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
                            setLogs("removed: " + file.getAbsolutePath());
                            ServiceLog.log.info("    > removed old backup {}", file.getAbsolutePath());
                        }
                    } catch (Exception ignore) {

                    }
                });

                ServiceLog.log.info("  <-- end creating database backup");
            }
        } catch (Exception e) {
            ServiceLog.log.error(" ! creating backup failed", e);
            lastSuccess = false;
            setLogs("FAILED: " + lastRun.formatter().getDateTime());
        }
    }

    public String getPath() {
        return path;
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