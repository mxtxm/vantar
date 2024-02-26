package com.vantar.admin.model.heath;

import com.vantar.admin.model.database.AdminData;
import com.vantar.admin.model.index.Admin;
import com.vantar.common.Settings;
import com.vantar.exception.FinishException;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.healthmonitor.ServiceHealthMonitor;
import com.vantar.util.json.Json;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.*;
import java.util.*;


public class AdminMonitoring {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_MENU_MONITORING), params, response, true);

        Map<String, List<String>> links = new LinkedHashMap<>(10, 1);

        List<String> items = new ArrayList<>(1);
        items.add(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS) + ":/admin/system/errors/index");
        links.put(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS), items);

        items = new ArrayList<>(2);
        items.add(Locale.getString(VantarKey.ADMIN_SERVICES_STATUS) + ":/admin/services/index");
        links.put(Locale.getString(VantarKey.ADMIN_SERVICES), items);

        items = new ArrayList<>(1);
        items.add(Locale.getString(VantarKey.ADMIN_QUEUE_STATUS) + ":/admin/queue/index");
        links.put(Locale.getString(VantarKey.ADMIN_QUEUE), items);

        items = new ArrayList<>(1);
        items.add(Locale.getString(VantarKey.ADMIN_ONLINE_USERS) + ":/admin/users/online");
        links.put(Locale.getString(VantarKey.ADMIN_USERS), items);

        items = new ArrayList<>(1);
        items.add(Locale.getString(VantarKey.ADMIN_CACHE) + ":/admin/cache/index");
        links.put(Locale.getString(VantarKey.ADMIN_CACHE), items);

        items = new ArrayList<>(3);
        items.add(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS_PARAM, "Mongo") + ":/admin/data/mongo/status");
        items.add(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS_PARAM, "SQL") + ":/admin/data/sql/status");
        items.add(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS_PARAM, "Elastic") + ":/admin/data/elastic/status");
        links.put(Locale.getString(VantarKey.ADMIN_DATABASE_TITLE), items);

        String adminApp = Settings.getAdminApp();
        if (StringUtil.isNotEmpty(adminApp)) {
            try {
                Class<?> tClass = Class.forName(adminApp);
                Method method = tClass.getMethod("extendMonitoringLinks", Map.class);
                method.invoke(null, links);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

            }
        }

        links.forEach((cat, i) -> {
            ui.beginBox(cat);
            for (String item : i) {
                String[] parts = StringUtil.splitTrim(item, ':');
                ui.addHrefBlock(parts[0], parts[1]);
            }
            ui.blockEnd();
        });

        // monitoring settings
        ServiceHealthMonitor monitor = Services.get(ServiceHealthMonitor.class);
        if (monitor != null) {
            ui.beginBox(VantarKey.ADMIN_SETTINGS);
            ui.addKeyValue("Check interval", monitor.intervalMin + " minutes");
            ui.addKeyValue("Warn free disk",  NumberUtil.getReadableByteSize(monitor.warnFreeDiskBytes));
            ui.addKeyValue("Warn free memory",  NumberUtil.getReadableByteSize(monitor.warnFreeMemoryBytes));
            ui.addKeyValue("Warn memory threshold", monitor.warnMemoryThreshold + " times");
            ui.addKeyValue("Warn processor max", monitor.warnProcessorMaxPercent + "%");
            ui.addKeyValue("Warn processor threshold", monitor.warnProcessorThreshold + " times");
            ui.blockEnd();

            ui.beginBox(VantarKey.ADMIN_WEBSERVICE);
            // >
            ui.addHeading(3, "/admin/system/health");
            if (monitor.getOverallSystemHealth()) {
                ui.addMessage("OK");
            } else {
                ui.addErrorMessage("NOK");
            }
            // >
            ui.addHeading(3, "/admin/system/health/errors");
            List<String> errors = monitor.getLastErrors();
            if (errors.isEmpty()) {
                ui.addMessage("No error messages");
            } else {
                ui.addErrorMessage(errors).write();
            }
            // >
            ui.addHeading(3, "/admin/system/health/report");
            ui.addBlock("pre", Json.d.toJsonPretty(monitor.getSystemHealthReport())).write();
            // >
            ui.addHeading(3, "/admin/system/database/report");
            ui.addBlock("pre", Json.d.toJsonPretty(AdminData.getDatabaseRecordCount())).write();
            // >
            ui.addHeading(3, "/admin/system/log/tags");
            ui.addHrefBlock(VantarKey.ADMIN_SYSTEM_ERRORS, "/admin/system/errors/index");
            ui.addHeading(3, "/admin/system/log/errors");
            ui.addHrefBlock(VantarKey.ADMIN_SYSTEM_ERRORS, "/admin/system/errors/index");
            // >
            ui.blockEnd();
        }
        ui.finish();
    }
}