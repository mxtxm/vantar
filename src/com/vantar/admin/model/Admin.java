package com.vantar.admin.model;

import com.vantar.admin.web.AdminSigninController;
import com.vantar.common.*;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.nosql.mongo.MongoConnection;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.queue.Queue;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.slf4j.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.*;
import java.util.*;


public class Admin {

    public static final Logger log = LoggerFactory.getLogger(AdminSigninController.class);


    private static Map<String, String> getMenus(WebUi ui) throws FinishException {
        Map<String, String> menu = new LinkedHashMap<>();
        menu.put(Locale.getString(VantarKey.ADMIN_MENU_HOME), "/admin");
        if (AdminAuth.isRoot(ui)) {
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_MONITORING), "/admin/monitoring");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_DATA), "/admin/data");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_ADVANCED), "/admin/advanced");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_SCHEDULE), "/admin/schedule");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_QUERY), "/admin/query/index");
        }
        menu.put(Locale.getString(VantarKey.ADMIN_MENU_DOCUMENTS), "/admin/document/index");

        // custom menu
        String adminApp = Settings.getAdminApp();
        if (StringUtil.isNotEmpty(adminApp)) {
            try {
                Class<?> tClass = Class.forName(adminApp);
                Method method = tClass.getMethod("extendMenu", Params.class, Map.class);
                method.invoke(null, ui.params, menu);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                log.error("! admin dashboard '{}.extendMenu(params, Map<menu,url>)'", adminApp, e);
            }
        }

        return menu;
    }

    public static WebUi getUi(String title, Params params, HttpServletResponse response, boolean requiresRoot)
        throws FinishException {

        WebUi ui = new WebUi(params, response);

        if (requiresRoot && !AdminAuth.isRoot(ui)) {
            ui.addErrorMessage(Locale.getString(VantarKey.ADMIN_AUTH_TOKEN)).finish();
            throw new FinishException();
        }

        return ui
            .addMenu(getMenus(ui), getOnlineUserTitle(params))
            .addPageTitle(Settings.config.getProperty("title") + " : " + title).beginMain();
    }

    public static WebUi getUiDto(String title, Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo)
        throws FinishException {

        WebUi ui = new WebUi(params, response);

        if (!AdminAuth.isRoot(ui)) {
            ui.addErrorMessage(Locale.getString(VantarKey.ADMIN_AUTH_TOKEN)).finish();
            throw new FinishException();
        }

        return ui
            .addMenu(getMenus(ui), getOnlineUserTitle(params))
            .setBreadcrumb(Settings.config.getProperty("title") + " : " + title, dtoInfo).beginMain();

    }

    private static String getOnlineUserTitle(Params params) {
        try {
            return Services.get(ServiceAuth.class).getCurrentUser(params).getFullName();
        } catch (ServiceException ignore) {
            return "";
        }
    }

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        try {
            ServiceAuth.getCurrentSignedInUser(params);
        } catch (ServiceException e) {
            Response.redirect(response, "/admin/signin");
            return;
        }

        WebUi ui = getUi(Locale.getString(VantarKey.ADMIN_SYSTEM_ADMIN), params, response, false);

        Map<String, List<String>> shortcuts = new LinkedHashMap<>();
        List<String> defaultLinks = new ArrayList<>(20);
        if (AdminAuth.isRoot(ui)) {
            defaultLinks.add("");
            defaultLinks.add(Locale.getString(VantarKey.ADMIN_USERS) + ":/admin/data/list?dto=User");
            defaultLinks.add("");
            defaultLinks.add(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS) + ":/admin/system/errors");
            defaultLinks.add(Locale.getString(VantarKey.ADMIN_SERVICES_LAST_RUN) + ":/admin/services/status");
            defaultLinks.add(Locale.getString(VantarKey.ADMIN_SERVICES_STATUS) + ":/admin/services/run");
            defaultLinks.add("");
            defaultLinks.add(Locale.getString(VantarKey.ADMIN_BACKUP_SQL) + ":/admin/data/backup/sql");
            defaultLinks.add(Locale.getString(VantarKey.ADMIN_BACKUP_MONGO) + ":/admin/data/backup/mongo");
            defaultLinks.add(Locale.getString(VantarKey.ADMIN_BACKUP_ELASTIC) + ":/admin/data/backup/elastic");
            shortcuts.put(Locale.getString(VantarKey.ADMIN_SHORTCUTS), defaultLinks);
        }

        // custom shortcuts
        String adminApp = Settings.getAdminApp();
        if (StringUtil.isNotEmpty(adminApp)) {
            try {
                Class<?> tClass = Class.forName(adminApp);
                Method method = tClass.getMethod("extendShortcuts", Params.class, Map.class);
                method.invoke(null, ui.params, shortcuts);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
                log.info(" ! admin dashboard title not set '{}.extendShortcuts()'", adminApp);
            }
        }

        shortcuts.forEach((cat, items) -> {
            ui.addHeading(3, cat);
            for (String item : items) {
                if (StringUtil.isEmpty(item)) {
                    ui.addEmptyLine();
                    continue;
                }
                String[] parts = StringUtil.split(item, ':');
                ui.addBlockLink(parts[0], parts[1]);
            }
        });

        ui.addEmptyLine(2);

        if (AdminAuth.isRoot(ui)) {
            ui  .addEmptyLine()
                .addHeading(3, Locale.getString(VantarKey.ADMIN_MEMORY))
                .addKeyValue("Designated memory", NumberUtil.round(Runtime.getRuntime().maxMemory() / (1024D * 1024D), 1) + "MB")
                .addKeyValue("Allocated memory", NumberUtil.round(Runtime.getRuntime().totalMemory() / (1024D * 1024D), 1) + "MB")
                .addKeyValue("Free memory", NumberUtil.round(Runtime.getRuntime().freeMemory() / (1024D * 1024D), 1) + "MB")
                .addKeyValue(
                    "Used memory",
                    NumberUtil.round((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024D * 1024D), 1) + "MB"
                )
                .addEmptyLine();

            ui  .addKeyValue("Mongo ", MongoConnection.isUp() ? "on" : "off")
                .addKeyValue("ElasticSearch ", ElasticConnection.isUp() ? "on" : "off")
                .addKeyValue("Sql ", SqlConnection.isUp() ? "on" : "off")
                .addKeyValue("RabbitMQ ", Queue.isUp() ? "on" : "off");

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
        }

        ui.addEmptyLine(8).addHeading(3, "Vantar system administration: " + VantarParam.VERSION).finish();
    }
}
