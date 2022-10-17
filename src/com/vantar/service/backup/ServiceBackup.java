package com.vantar.service.backup;

import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.nosql.elasticsearch.ElasticBackup;
import com.vantar.database.nosql.mongo.MongoBackup;
import com.vantar.database.sql.SqlBackup;
import com.vantar.service.Services;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.file.*;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.*;


public class ServiceBackup implements Services.Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceBackup.class);
    private ScheduledExecutorService schedule;

    public Boolean onEndSetNull;
    public String dbms;
    public Integer intervalHour;
    public Integer deleteOldFilesAfterDays;
    public String path;


    public void start() {
        schedule = Executors.newSingleThreadScheduledExecutor();
        long midnight = LocalDateTime.now().until(LocalDate.now().plusDays(1).atStartOfDay(), ChronoUnit.MINUTES);
        schedule.scheduleAtFixedRate(this::create, midnight, intervalHour * 60, TimeUnit.MINUTES);
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
        String tail = "-" + (new DateTime().formatter().getDateTimeSimple()) + ".dump";

        dbms = dbms.toUpperCase();
        if (StringUtil.contains(dbms, DtoDictionary.Dbms.MONGO.toString())) {
            MongoBackup.dump(path + "mongo" + tail, null, null);
        }
        if (StringUtil.contains(dbms, DtoDictionary.Dbms.SQL.toString())) {
            SqlBackup.dump(path + "sql" + tail, null, null);
        }
        if (StringUtil.contains(dbms, DtoDictionary.Dbms.ELASTIC.toString())) {
            ElasticBackup.dump(path + "elastic" + tail, null, null);
        }

        if (deleteOldFilesAfterDays != null) {
            DateTime thresholdDate = new DateTime().decreaseDays(deleteOldFilesAfterDays);
            DirUtil.browseByExtension(path, "dump", file -> {
                String[] parts = StringUtil.split(
                    StringUtil.remove(
                        file.getName(),
                        ".dump", ".zip", "elastic-", "sql-", "mongo-"
                    ),
                    '-'
                );
                String s = parts[0] + '-' + parts[1] + '-' + parts[2];
                try {
                    DateTime fileDate = new DateTime(s);
                    if (fileDate.isBefore(thresholdDate)) {
                        FileUtil.removeFile(file.getAbsolutePath());
                        log.info(" > removed old backup {}", file.getAbsolutePath());
                    }
                } catch (Exception ignore) {

                }
            });

        }
    }

    public String getPath() {
        return path;
    }
}