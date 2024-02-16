package com.vantar.admin.model.service;

import com.vantar.admin.model.index.Admin;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.nosql.mongo.MongoConnection;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.FinishException;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.queue.Queue;
import com.vantar.service.Services;
import com.vantar.service.log.Beat;
import com.vantar.util.datetime.DateTimeFormatter;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminServiceMonitoring {

    public static void servicesStatus(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_SERVICES_STATUS), params, response, true);

        ui  .beginBox(Locale.getString(VantarKey.ADMIN_RUNNING_SERVICES_DEPENDENCIES))
            .addEmptyLine();
        AdminServiceMonitoring.plotDependencyServicesStatus(ui);
        ui.blockEnd();

        ui  .beginBox(Locale.getString(VantarKey.ADMIN_RUNNING_SERVICES_ME))
            .addEmptyLine();
        AdminServiceMonitoring.plotServicesStatus(ui);
        ui.blockEnd();

        ui  .beginBox(Locale.getString(VantarKey.ADMIN_RUNNING_SERVICES_OTHER))
            .addEmptyLine();
        AdminServiceMonitoring.plotServicesOnOtherServersStatus(ui);
        ui.blockEnd();

        ui  .beginBox(Locale.getString(VantarKey.ADMIN_RUNNING_SERVICES_LOGS))
            .addEmptyLine();
        AdminServiceMonitoring.plotServicesLogs(ui);
        ui.blockEnd();

        ui  .beginBox(Locale.getString(VantarKey.ADMIN_SERVICES_LAST_RUN))
            .addEmptyLine();
        AdminServiceMonitoring.plotServicesLastRun(ui);
        ui.blockEnd();

        ui.finish();
    }

    public static void plotDependencyServicesStatus(WebUi ui) {
        ui.addKeyValue(
            "Mongo ",
            Services.isDependencyEnabled(MongoConnection.class) ?
                (Services.isUp(MongoConnection.class) ? "up" : "down") : "disabled"
        )
            .addKeyValue(
                "SQL ",
                Services.isDependencyEnabled(SqlConnection.class) ?
                    (Services.isUp(SqlConnection.class) ? "up" : "down") : "disabled"
            )
            .addKeyValue(
                "ElasticSearch ",
                Services.isDependencyEnabled(ElasticConnection.class) ?
                    (Services.isUp(ElasticConnection.class) ? "up" : "down") : "disabled"
            )
            .addKeyValue(
                "RabbitMQ ",
                Services.isDependencyEnabled(Queue.class) ?
                    (Services.isUp(Queue.class) ? "up" : "down") : "disabled"
            );
    }

    public static void plotServicesStatus(WebUi ui) {
        Collection<Services.Service> s = Services.getServices();
        if (s == null) {
            return;
        }
        for (Services.Service service : s) {
            ui.addKeyValue(
                service.getClass().getSimpleName(),
                service.isUp() ? (service.isOk() ? "up and ok" : "up with failures") : "down"
            );
        }
    }

    public static void plotServicesLogs(WebUi ui) {
        Collection<Services.Service> s = Services.getServices();
        if (s == null) {
            return;
        }
        for (Services.Service service : s) {
            List<String> logs = service.getLogs();
            if (logs != null) {
                ui.addHeading(3, service.getClass().getSimpleName());
                for (String log : logs) {
                    ui.addBlock("pre", log);
                }
            }
        }
    }

    public static void plotServicesOnOtherServersStatus(WebUi ui) {
        Map<String, List<String>> s = Services.getServicesOnOtherServers();
        if (s == null) {
            return;
        }
        for (Map.Entry<String, List<String>> servers : s.entrySet()) {
            ui.addHeading(3, servers.getKey());
            for (String service : servers.getValue()) {
                ui.addKeyValue(service.getClass().getSimpleName(), "up");
            }
        }
    }

    public static void plotServicesLastRun(WebUi ui) {
        Beat.getBeats().forEach((service, logs) -> {
            ui.addHeading(3, service.getName());
            logs.forEach((comment, time) ->
                ui.addKeyValue(comment, time.toString() + " ("
                    + DateTimeFormatter.secondsToDateTime(Math.abs(time.secondsFromNow())) + ")")
            );
        });
    }
}