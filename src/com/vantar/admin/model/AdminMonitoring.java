package com.vantar.admin.model;

import com.vantar.common.Settings;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.log.LogEvent;
import com.vantar.util.datetime.DateTimeFormatter;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.*;
import java.util.*;


public class AdminMonitoring {

    public static final Map<String, List<String>> links = new LinkedHashMap<>();


    public static void index(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_MENU_MONITORING), params, response);
        if (ui == null) {
            return;
        }

        List<String> items = new ArrayList<>(1);
        items.add(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS) + ":/admin/system/errors");
        links.put(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS), items);

        items = new ArrayList<>(2);
        items.add(Locale.getString(VantarKey.ADMIN_SERVICES_LAST_RUN) + ":/admin/services/status");
        items.add(Locale.getString(VantarKey.ADMIN_SERVICES_STATUS) + ":/admin/services/run");
        links.put(Locale.getString(VantarKey.ADMIN_SERVICES), items);

        items = new ArrayList<>(1);
        items.add(Locale.getString(VantarKey.ADMIN_QUEUE_STATUS) + ":/admin/queue/status");
        links.put(Locale.getString(VantarKey.ADMIN_QUEUE), items);

        items = new ArrayList<>(3);
        items.add(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS_PARAM, "SQL") + ":/admin/data/sql/status");
        items.add(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS_PARAM, "Mongo") + ":/admin/data/mongo/status");
        items.add(Locale.getString(VantarKey.ADMIN_DATABASE_STATUS_PARAM, "Elastic") + ":/admin/data/elastic/status");
        links.put(Locale.getString(VantarKey.ADMIN_DATABASE_TITLE), items);

        items = new ArrayList<>(1);
        items.add(Locale.getString(VantarKey.ADMIN_ONLINE_USERS) + ":/admin/users/online");
        links.put(Locale.getString(VantarKey.ADMIN_USERS), items);

        items = new ArrayList<>(1);
        items.add(Locale.getString(VantarKey.ADMIN_CACHE) + ":/admin/cache/index");
        links.put(Locale.getString(VantarKey.ADMIN_CACHE), items);

        String appPackage = Settings.getAppPackage();
        if (StringUtil.isNotEmpty(appPackage)) {
            try {
                Class<?> tClass = Class.forName(appPackage + ".business.admin.model.AdminApp");
                Method method = tClass.getMethod("extendMonitoringLinks");
                method.invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

            }
        }

        links.forEach((cat, i) -> {
            ui.beginBox(cat);
            for (String item : i) {
                String[] parts = StringUtil.split(item, ':');
                ui.addBlockLink(parts[0], parts[1]);
            }
            ui.containerEnd();
        });

        ui.finish();
    }

    public static void servicesLastRun(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_SERVICES_LAST_RUN), params, response);
        if (ui == null) {
            return;
        }

        LogEvent.getBeats().forEach((service, logs) -> {
            ui.beginBox(service);
            logs.forEach((comment, time) ->
                ui.addKeyValue(comment, time.toString() + " ("
                    + DateTimeFormatter.secondsToDateTime(Math.abs(time.secondsFromNow())) + ")")
            );
            ui.containerEnd();
        });

        ui.finish();
    }

    public static void servicesStatus(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_SERVICES_STATUS), params, response);
        if (ui == null) {
            return;
        }

        ui  .beginBox(Locale.getString(VantarKey.ADMIN_ENABLED_SERVICES_THIS))
            .addKeyValue(Locale.getString(VantarKey.ADMIN_SERVICE), Locale.getString(VantarKey.ADMIN_IS_ON));

        synchronized (Services.upServices) {
            for (String service : Services.getEnabledServices()) {
                Services.ServiceInfo info = Services.upServices.get(service);
                ui.addKeyValue(service, info == null ? "false" : Boolean.toString(info.isRunningOnThisServer));
            }
            ui.containerEnd();

            ui  .beginBox(Locale.getString(VantarKey.ADMIN_RUNNING_SERVICES))
                .addKeyValue(Locale.getString(VantarKey.ADMIN_RUNNING_SERVICES_COUNT),
                    Locale.getString(VantarKey.ADMIN_RUNNING_SERVICES_ON_THIS_SERVER));
            Services.upServices.forEach((service, info) ->
                ui.addKeyValue(service + " (" + info.instanceCount + ")", info.isEnabledOnThisServer));
        }

        ui.finish();
    }
}
