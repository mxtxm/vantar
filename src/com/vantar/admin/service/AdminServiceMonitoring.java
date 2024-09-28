package com.vantar.admin.service;

import com.vantar.admin.index.Admin;
import com.vantar.database.common.Db;
import com.vantar.exception.FinishException;
import com.vantar.locale.VantarKey;
import com.vantar.queue.common.Que;
import com.vantar.service.Services;
import com.vantar.service.log.Beat;
import com.vantar.util.datetime.DateTimeFormatter;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminServiceMonitoring {

    public static void servicesIndex(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_SERVICES_STATUS, params, response, true);

        ui.beginBox(VantarKey.ADMIN_SERVICES_RUNNING_DATA_SOURCE);
        plotDataSourcesStatus(ui);
        ui.blockEnd();

        ui.beginBox(VantarKey.ADMIN_SERVICES_RUNNING_ME);
        plotServicesStatus(ui);
        ui.blockEnd();

        ui.beginBox(VantarKey.ADMIN_SERVICES_RUNNING_OTHER);
        plotServicesOnOtherServersStatus(ui);
        ui.blockEnd();

        ui.beginBox(VantarKey.ADMIN_SERVICES_RUNNING_LOGS);
        plotServicesLogs(ui);
        ui.blockEnd();

        ui.beginBox(VantarKey.ADMIN_SERVICES_BEAT, null, null, "solid-box-clear-inner");
        plotServicesBeats(ui);
        ui.blockEnd();

        ui.finish();
    }

    public static void plotDataSourcesStatus(WebUi ui) {
        ui
            .addKeyValue(
                Que.Engine.RABBIT.name(),
                Services.isEnabled(Que.Engine.RABBIT) ? (Services.isUp(Que.Engine.RABBIT) ? "up" : "down") : "disabled"
            )
            .addKeyValue(
                Db.Dbms.MONGO.name() + " ",
                Services.isEnabled(Db.Dbms.MONGO) ? (Services.isUp(Db.Dbms.MONGO) ? "up" : "down") : "disabled"
            )
            .addKeyValue(
                Db.Dbms.SQL.name() + " ",
                Services.isEnabled(Db.Dbms.SQL) ? (Services.isUp(Db.Dbms.SQL) ? "up" : "down") : "disabled"
            )
            .addKeyValue(
                Db.Dbms.ELASTIC.name() + " ",
                Services.isEnabled(Db.Dbms.ELASTIC) ? (Services.isUp(Db.Dbms.ELASTIC) ? "up" : "down") : "disabled"
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