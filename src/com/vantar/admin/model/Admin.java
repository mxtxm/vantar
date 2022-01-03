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
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.slf4j.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.*;
import java.util.*;


public class Admin {

    public static final Logger log = LoggerFactory.getLogger(AdminSigninController.class);

    public static String appTitle = "";
    public static Map<String, String> menu;
    public static Map<String, List<String>> shortcuts;


    private static void setMenus(WebUi ui) throws FinishException {
        menu = new LinkedHashMap<>();
        shortcuts = new LinkedHashMap<>();
        boolean isRoot = AdminAuth.isRoot(ui);

        String appPackage = Settings.getAppPackage();
        String adminApp = StringUtil.isEmpty(appPackage) ? null : appPackage + ".business.admin.model.AdminApp";

        // admin dashboard title
        if (StringUtil.isNotEmpty(adminApp)) {
            try {
                Class<?> tClass = Class.forName(adminApp);
                Method method = tClass.getMethod("setTitle");
                method.invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
                log.info("> > > admin dashboard title not set '{}.setTitle()'", adminApp);
            }
        }

        menu.put(Locale.getString(VantarKey.ADMIN_MENU_HOME), "/admin");
        if (isRoot) {
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_MONITORING), "/admin/monitoring");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_DATA), "/admin/data");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_ADVANCED), "/admin/advanced");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_SCHEDULE), "/admin/schedule");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_QUERY), "/admin/query/index");
        }
        menu.put(Locale.getString(VantarKey.ADMIN_MENU_DOCUMENTS), "/admin/document/index");

        // custom menu
        if (StringUtil.isNotEmpty(adminApp)) {
            try {
                Class<?> tClass = Class.forName(adminApp);
                Method method = tClass.getMethod("extendMenu", Params.class);
                method.invoke(null, ui.params);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
                log.info("> > > admin dashboard title not set '{}.extendMenu()'", adminApp);
            }
        }

        List<String> shortCuts = new ArrayList<>(20);
        if (isRoot) {
            shortCuts.add("");
            shortCuts.add(Locale.getString(VantarKey.ADMIN_USERS) + ":/admin/data/list?dto=User");
            shortCuts.add("");
            shortCuts.add(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS) + ":/admin/system/errors");
            shortCuts.add(Locale.getString(VantarKey.ADMIN_SERVICES_LAST_RUN) + ":/admin/services/status");
            shortCuts.add(Locale.getString(VantarKey.ADMIN_SERVICES_STATUS) + ":/admin/services/run");
            shortCuts.add("");
            shortCuts.add(Locale.getString(VantarKey.ADMIN_BACKUP_SQL) + ":/admin/data/backup/sql");
            shortCuts.add(Locale.getString(VantarKey.ADMIN_BACKUP_MONGO) + ":/admin/data/backup/mongo");
            shortCuts.add(Locale.getString(VantarKey.ADMIN_BACKUP_ELASTIC) + ":/admin/data/backup/elastic");
            shortcuts.put(Locale.getString(VantarKey.ADMIN_SHORTCUTS), shortCuts);
        }

        // custom shortcuts
        if (StringUtil.isNotEmpty(adminApp)) {
            try {
                Class<?> tClass = Class.forName(adminApp);
                Method method = tClass.getMethod("extendShortcuts");
                method.invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
                log.info("> > > admin dashboard title not set '{}.extendShortcuts()'", adminApp);
            }
        }
    }

    public static WebUi getUi(String title, Params params, HttpServletResponse response, boolean requiresRoot)
        throws FinishException {

        WebUi ui = new WebUi(params, response);

        if (requiresRoot && !AdminAuth.isRoot(ui)) {
            ui  .addErrorMessage(Locale.getString(VantarKey.ADMIN_AUTH_TOKEN))
                .finish();
            throw new FinishException();
        }

        setMenus(ui);

        ui  .addMenu(menu, getOnlineUserTitle(params))
            .addPageTitle(appTitle + " : " + title).beginMain();

        return ui;
    }

    public static WebUi getUiDto(String title, Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo)
        throws FinishException {

        WebUi ui = new WebUi(params, response);

        if (!AdminAuth.isRoot(ui)) {
            ui.addErrorMessage(Locale.getString(VantarKey.ADMIN_AUTH_TOKEN)).finish();
            throw new FinishException();
        }

        setMenus(ui);

        ui  .addMenu(menu, getOnlineUserTitle(params))
            .setBreadcrumb(appTitle + " : " + title, dtoInfo).beginMain();

        return ui;
    }

    private static String getOnlineUserTitle(Params params) {
        try {
            return Services.get(ServiceAuth.class).getCurrentUser(params).getFullName();
        } catch (AuthException | ServiceException ignore) {
            return "";
        }
    }

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        try {
            ServiceAuth.getCurrentSignedInUser(params);
        } catch (ServiceException | AuthException e) {
            Response.redirect(response, "/admin/signin");
            return;
        }

        WebUi ui = getUi(Locale.getString(VantarKey.ADMIN_SYSTEM_ADMIN), params, response, false);

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

        ui.addEmptyLine(2);

        if (AdminAuth.isRoot(ui)) {
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

        ui  .addEmptyLine(8).addHeading(3, "Vantar system administration: " + VantarParam.VERSION);

        ui.finish();
    }
}
