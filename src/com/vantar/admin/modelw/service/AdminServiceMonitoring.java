package com.vantar.admin.modelw.service;

import com.vantar.admin.model.index.Admin;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.nosql.mongo.MongoConnection;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.FinishException;
import com.vantar.locale.*;
import com.vantar.queue.Queue;
import com.vantar.service.Services;
import com.vantar.service.log.Beat;
import com.vantar.util.datetime.DateTimeFormatter;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminServiceMonitoring {

    public static void servicesIndex(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_SERVICES_STATUS, params, response, true);

        ui.beginBox(VantarKey.ADMIN_RUNNING_SERVICES_DEPENDENCIES);
        plotDependencyServicesStatus(ui);
        ui.blockEnd();

        ui.beginBox(VantarKey.ADMIN_RUNNING_SERVICES_ME);
        plotServicesStatus(ui);
        ui.blockEnd();

        ui.beginBox(VantarKey.ADMIN_RUNNING_SERVICES_OTHER);
        plotServicesOnOtherServersStatus(ui);
        ui.blockEnd();

        ui.beginBox(VantarKey.ADMIN_RUNNING_SERVICES_LOGS);
        plotServicesLogs(ui);
        ui.blockEnd();

        ui.beginBox(VantarKey.ADMIN_SERVICES_BEAT, null, null, "solid-box-clear-inner");
        plotServicesBeats(ui);
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
                ui.addHeading(2, service.getClass().getSimpleName());
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

    public static void plotServicesBeats(WebUi ui) {
        Beat.getBeats().forEach((service, logs) -> {
            ui.addHeading(3, service.getSimpleName());
            logs.forEach((comment, time) ->
                ui.addKeyValue(
                    time.toString() + " (" + DateTimeFormatter.secondsToDateTime(Math.abs(time.secondsFromNow())) + ")",
                    comment
                )
            );
        });
    }
}