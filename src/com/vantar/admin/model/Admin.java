package com.vantar.admin.model;

import com.vantar.common.*;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.nosql.mongo.MongoConnection;
import com.vantar.database.sql.SqlConnection;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.queue.Queue;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.*;
import java.util.*;


public class Admin {

    public static String appTitle = "";
    public static Map<String, String> menu;
    public static Map<String, List<String>> shortcuts;


    private static void setMenus(Params params) {
        menu = new LinkedHashMap<>();
        shortcuts = new LinkedHashMap<>();

        String appPackage = Settings.getAppPackage();
        if (StringUtil.isNotEmpty(appPackage)) {
            try {
                Class<?> tClass = Class.forName(appPackage + ".business.admin.model.AdminApp");
                Method method = tClass.getMethod("setTitle");
                method.invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

            }
        }

        menu.put(Locale.getString(VantarKey.ADMIN_MENU_HOME), "/admin");
        if (AdminAuth.hasAccess(params, AdminUserRole.ADMIN)) {
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_MONITORING), "/admin/monitoring");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_DATA), "/admin/data");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_ADVANCED), "/admin/advanced");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_SCHEDULE), "/admin/schedule");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_QUERY), "/admin/query/index");
        }
        menu.put(Locale.getString(VantarKey.ADMIN_MENU_DOCUMENTS), "/admin/document/index");

        if (StringUtil.isNotEmpty(appPackage)) {
            try {
                Class<?> tClass = Class.forName(appPackage + ".business.admin.model.AdminApp");
                Method method = tClass.getMethod("extendMenu", Params.class);
                method.invoke(null, params);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

            }
        }

        List<String> items = new ArrayList<>(1);
        if (AdminAuth.hasAccess(params, AdminUserRole.ADMIN)) {
            items.add("");
            items.add(Locale.getString(VantarKey.ADMIN_USERS) + ":/admin/data/list?dto=User");
            items.add("");
            items.add(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS) + ":/admin/system/errors");
            items.add(Locale.getString(VantarKey.ADMIN_SERVICES_LAST_RUN) + ":/admin/services/status");
            items.add(Locale.getString(VantarKey.ADMIN_SERVICES_STATUS) + ":/admin/services/run");
            items.add("");
            items.add(Locale.getString(VantarKey.ADMIN_BACKUP_SQL) + ":/admin/data/backup/sql");
            items.add(Locale.getString(VantarKey.ADMIN_BACKUP_MONGO) + ":/admin/data/backup/mongo");
            items.add(Locale.getString(VantarKey.ADMIN_BACKUP_ELASTIC) + ":/admin/data/backup/elastic");
            shortcuts.put(Locale.getString(VantarKey.ADMIN_SHORTCUTS), items);
        }

        if (StringUtil.isNotEmpty(appPackage)) {
            try {
                Class<?> tClass = Class.forName(appPackage + ".business.admin.model.AdminApp");
                Method method = tClass.getMethod("extendShortcuts");
                method.invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

            }
        }
    }

    public static WebUi getUi(String title, Params params, HttpServletResponse response) {
        return getUi(title, params, response, null);
    }

    public static WebUi getUiAdminAccess(String title, Params params, HttpServletResponse response) {
        return getUi(title, params, response, AdminUserRole.ADMIN);
    }

    public static WebUi getUi(String title, Params params, HttpServletResponse response, CommonUserRole role) {
        setMenus(params);
        WebUi ui = new WebUi(params, response);
        ui  .addMenu(menu, getOnlineUserTitle(params))
            .addPageTitle(appTitle + " : " + title).beginMain();

        if (!AdminAuth.hasAccess(params, role)) {
            ui.redirect("/admin/signin");
            return null;
        }

        return ui;
    }

    public static WebUi getUiDto(String title, Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) {
        setMenus(params);
        WebUi ui = new WebUi(params, response);
        ui  .addMenu(menu, getOnlineUserTitle(params))
            .setBreadcrumb(appTitle + " : " + title, dtoInfo).beginMain();

        if (!AdminAuth.hasAccess(params, AdminUserRole.ADMIN)) {
            ui.redirect("/admin/signin");
            return null;
        }

        return ui;
    }

    private static String getOnlineUserTitle(Params params) {
        ServiceAuth auth = Services.get(ServiceAuth.class);
        if (auth != null) {
            CommonUser user = auth.getCurrentUser(params);
            if (user != null) {
                return user.getFullName();
            }
        }
        return "";
    }

    public static void index(Params params, HttpServletResponse response) {
        WebUi ui = getUi(Locale.getString(VantarKey.ADMIN_SYSTEM_ADMIN), params, response);
        if (ui == null) {
            return;
        }

        if (AdminAuth.hasAccess(params, AdminUserRole.ADMIN)) {
            shortcuts.forEach((cat, items) -> {
                ui.addHeading(cat);
                for (String item : items) {
                    if (StringUtil.isEmpty(item)) {
                        ui.addEmptyLine();
                        continue;
                    }
                    String[] parts = StringUtil.split(item, ':');
                    ui.addBlockLink(parts[0], parts[1]);
                }
            });

            ui  .addEmptyLine()
                .addEmptyLine();

            synchronized (Services.upServices) {
                ui  .addEmptyLine()
                    .addHeading(3, Locale.getString(VantarKey.ADMIN_RUNNING_SERVICES))
                    .addEmptyLine()
                    .addKeyValue(
                        Locale.getString(VantarKey.ADMIN_RUNNING_SERVICES_COUNT),
                        Locale.getString(VantarKey.ADMIN_RUNNING_SERVICES_ON_THIS_SERVER)
                    );
                Services.upServices.forEach((service, info) ->
                    ui.addKeyValue(service + " (" + info.instanceCount + ")", info.isEnabledOnThisServer));
            }

            ui  .addKeyValue("Mongo ", MongoConnection.isUp ? "on" : "off")
                .addKeyValue("ElasticSearch ", ElasticConnection.isUp ? "on" : "off")
                .addKeyValue("Sql ", SqlConnection.isUp ? "on" : "off")
                .addKeyValue("RabbitMQ ", Queue.isUp ? "on" : "off");
        }

        ui  .addEmptyLine().addEmptyLine().addEmptyLine().addEmptyLine().addEmptyLine().addEmptyLine().addEmptyLine().addEmptyLine()
            .addHeading(3, "Vantar system administration: " + VantarParam.VERSION);

        ui.finish();
    }
}
