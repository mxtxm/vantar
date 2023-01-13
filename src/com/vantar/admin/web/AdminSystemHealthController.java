package com.vantar.admin.web;

import com.vantar.business.*;
import com.vantar.common.VantarParam;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.nosql.mongo.MongoConnection;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.*;
import com.vantar.queue.Queue;
import com.vantar.service.Services;
import com.vantar.service.log.LogEvent;
import com.vantar.service.scheduler.ServiceScheduler;
import com.vantar.util.datetime.*;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@WebServlet({
    "/admin/system/health/report",
    "/admin/system/database/report",
})
public class AdminSystemHealthController extends RouteToMethod {

    public Map<String, Object> health = new HashMap<>(10, 1);


    public void systemHealthReport(Params params, HttpServletResponse response) {
        // resources
        Map<String, String> resources = new HashMap<>(5, 1);
        resources.put("DesignatedMemory", NumberUtil.round(Runtime.getRuntime().maxMemory() / (1024D * 1024D), 1) + "MB");
        resources.put("AllocatedMemory", NumberUtil.round(Runtime.getRuntime().totalMemory() / (1024D * 1024D), 1) + "MB");
        resources.put("FreeMemory", NumberUtil.round(Runtime.getRuntime().freeMemory() / (1024D * 1024D), 1) + "MB");
        resources.put(
            "UsedMemory",
            NumberUtil.round((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024D * 1024D), 1) + "MB"
        );
        health.put("Resources", resources);

        // db/queue services
        Map<String, String> dataServices = new HashMap<>(5, 1);
        dataServices.put("Mongo ", MongoConnection.isEnabled() ? (MongoConnection.isUp() ? "on" : "off") : "na");
        dataServices.put("ElasticSearch ", ElasticConnection.isEnabled() ? (ElasticConnection.isUp() ? "on" : "off") : "na");
        dataServices.put("Sql ", SqlConnection.isEnabled() ? (SqlConnection.isUp() ? "on" : "off") : "na");
        dataServices.put("RabbitMQ ", Queue.isEnabled() ? (Queue.isUp() ? "on" : "off") : "na");
        health.put("DataServices", dataServices);

        // other services
        Map<String, ServiceStatus> services = new HashMap<>(10, 1);
        synchronized (Services.upServices) {
            Services.upServices.forEach((service, info) -> {
                if (!"ServiceScheduler".equals(service)) {
                    services.put(service, new ServiceStatus(info));
                }
            });
        }
        LogEvent.getBeats().forEach((service, logs) -> {
            ServiceStatus serviceStatus = services.get(service.getSimpleName());
            if (serviceStatus == null || "ServiceScheduler".equals(service.getSimpleName())) {
                return;
            }
            serviceStatus.className = service.getName();
            serviceStatus.timings = new ArrayList<>(7);
            logs.forEach((title, time) -> serviceStatus.timings.add(new Timing(title, time)));
        });
        health.put("Services", services);

        // queue status
        if (Queue.isUp()) {
            String[] queues = Queue.connection.getQueues();
            if (queues != null) {
                Map<String, Object> qs = new HashMap<>(20);
                for (String q : queues) {
                    String queueName = StringUtil.split(q, VantarParam.SEPARATOR_COMMON)[0];
                    qs.put(queueName, Queue.count(queueName));
                }
                health.put("Queues", qs);
            }
        }

        // schedules
        try {
            ServiceScheduler sch = Services.get(ServiceScheduler.class);
            if (Services.isUp(ServiceScheduler.class) && sch != null) {
                List<ScheduleStatus> schStatuses = new ArrayList<>(10);
                for (ServiceScheduler.ScheduleInfo info : sch.getToRuns()) {
                    StringBuilder startAtComment = new StringBuilder();
                    startAtComment.append(info.startAt).append(" ");
                    int c = StringUtil.countMatches(info.startAt, ':');
                    if (c == 1) {
                        startAtComment.append("(H:M)");
                    } else if (c == 2) {
                        startAtComment.append("(H:M:S)");
                    } else {
                        startAtComment.append("(after service startup)");
                    }

                    StringBuilder repeatAtComment = new StringBuilder();
                    repeatAtComment.append(info.repeatAt).append(" ");
                    if (info.repeat) {
                        c = StringUtil.countMatches(info.repeatAt, ':');
                        if (c == 1) {
                            repeatAtComment.append("(H:M)");
                        } else if (c == 2) {
                            repeatAtComment.append("(H:M:S)");
                        } else {
                            repeatAtComment.append("(every)");
                        }
                    }

                    ScheduleStatus scheduleStatus = new ScheduleStatus();
                    scheduleStatus.className = info.getName();
                    scheduleStatus.lastRun = LogEvent.getBeat(ServiceScheduler.class, "run:" + info.getName());
                    scheduleStatus.start = startAtComment.toString();
                    scheduleStatus.repeat = repeatAtComment.toString();
                    schStatuses.add(scheduleStatus);
                }

                health.put("ScheduleTasks", schStatuses);
            }
        } catch (ServiceException ignore) {

        }

        Response.writeJsonPretty(response, health);
    }


    public static class ServiceStatus {

        public String className;
        public List<Timing> timings;
        public boolean isEnabledOnThisServer;
        public int instanceCount;

        public ServiceStatus(Services.ServiceInfo serviceInfo) {
            isEnabledOnThisServer = serviceInfo.isEnabledOnThisServer;
            instanceCount = serviceInfo.instanceCount;
        }
    }


    public static class Timing {

        public String title;
        public DateTime time;
        public String lastRunMinutes;

        public Timing(String title, DateTime time) {
            this.title = title;
            this.time = time;
            lastRunMinutes = DateTimeFormatter.secondsToDateTime(Math.abs(time.secondsFromNow()));
        }
    }


    public static class ScheduleStatus {

        public String className;
        public String start;
        public String repeat;
        public DateTime lastRun;

    }


    public static void systemDatabaseReport(Params params, HttpServletResponse response) {
        Map<String, Map<String, Long>> databases = new HashMap<>(5, 1);

        if (MongoConnection.isUp()) {
            Map<String, Long> stats = new HashMap<>(100, 1);
            try {
                for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.MONGO)) {
                    stats.put(
                        info.dtoClass.getSimpleName(),
                        CommonRepoMongo.count(info.getDtoInstance().getStorage())
                    );
                }
            } catch (DatabaseException ignore) {

            }
            databases.put("Mongo", stats);
        }

        if (ElasticConnection.isUp()) {
            Map<String, Long> stats = new HashMap<>(100, 1);
            try {
                for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.ELASTIC)) {
                    stats.put(
                        info.dtoClass.getSimpleName(),
                        CommonRepoElastic.count(info.getDtoInstance().getStorage())
                    );
                }
            } catch (DatabaseException ignore) {

            }
            databases.put("Elastic", stats);
        }

        if (SqlConnection.isUp()) {
            Map<String, Long> stats = new HashMap<>(100, 1);
            try (SqlConnection connection = new SqlConnection()) {
                CommonRepoSql repo = new CommonRepoSql(connection);
                for (DtoDictionary.Info info : DtoDictionary.getAll(DtoDictionary.Dbms.SQL)) {
                    stats.put(
                        info.dtoClass.getSimpleName(),
                        repo.count(info.getDtoInstance().getStorage())
                    );
                }
            } catch (DatabaseException ignore) {

            }
            databases.put("Sql", stats);
        }

        Response.writeJsonPretty(response, databases);
    }

}