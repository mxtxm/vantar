package com.vantar.service.backup;

import com.vantar.common.Settings;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.nosql.elasticsearch.ElasticBackup;
import com.vantar.database.nosql.mongo.MongoBackup;
import com.vantar.database.sql.SqlBackup;
import com.vantar.service.Services;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.string.StringUtil;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.*;


public class ServiceBackup implements Services.Service {

    private ScheduledExecutorService schedule;

    public Boolean onEndSetNull;
    public String dbms;
    public Integer intervalHour;


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
        String head = Settings.backup().getBackupDir();
        String tail = "-" + (new DateTime().formatter().getDateTimeSimple()) + ".dump";

        dbms = dbms.toUpperCase();
        if (StringUtil.contains(dbms, DtoDictionary.Dbms.MONGO.toString())) {
            MongoBackup.dump(head + "mongo" + tail, null, null);
        }
        if (StringUtil.contains(dbms, DtoDictionary.Dbms.SQL.toString())) {
            SqlBackup.dump(head + "sql" + tail, null, null);
        }
        if (StringUtil.contains(dbms, DtoDictionary.Dbms.ELASTIC.toString())) {
            ElasticBackup.dump(head + "elastic" + tail, null, null);
        }
    }
}