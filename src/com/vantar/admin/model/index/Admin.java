package com.vantar.admin.model.index;

import com.vantar.admin.model.AdminAuth;
import com.vantar.admin.model.service.AdminServiceMonitoring;
import com.vantar.admin.web.AdminSigninController;
import com.vantar.common.*;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.ServiceAuth;
import com.vantar.service.healthmonitor.ServiceHealthMonitor;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.slf4j.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.*;
import java.util.*;


public class Admin {

    public static final Logger log = LoggerFactory.getLogger(AdminSigninController.class);


    public static void index(Params params, HttpServletResponse response) throws FinishException {
        if (null == ServiceAuth.getCurrentSignedInUser(params)) {
            Response.redirect(response, "/admin/signin");
            return;
        }

        WebUi ui = getUi(Locale.getString(VantarKey.ADMIN_SYSTEM_ADMIN), params, response, false);

        Map<String, List<String>> shortcuts = new LinkedHashMap<>(14);

        // admin shortcuts
        if (AdminAuth.isRoot(ui)) {
            List<String> defaultLinks = new ArrayList<>(20);
            defaultLinks.add("");
            defaultLinks.add(Locale.getString(VantarKey.ADMIN_ONLINE_USERS) + ":/admin/users/online/admin/users/online");
            defaultLinks.add(Locale.getString(VantarKey.ADMIN_USERS) + ":/admin/data/list?dto=User");
            defaultLinks.add("");
            defaultLinks.add(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS) + ":/admin/system/errors");
            defaultLinks.add(Locale.getString(VantarKey.ADMIN_SERVICES_STATUS) + ":/admin/services/status");
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

        // plot shortcuts
        shortcuts.forEach((cat, items) -> {
            ui.addHeading(3, cat);
            for (String item : items) {
                if (StringUtil.isEmpty(item)) {
                    ui.addEmptyLine();
                    continue;
                }
                String[] parts = StringUtil.split(item, ':');
                ui.addHrefBlock(parts[0], parts[1]);
            }
        });

        ui.addEmptyLine(3);

        // show info
        if (AdminAuth.isRoot(ui)) {
            ServiceHealthMonitor monitor = Services.get(ServiceHealthMonitor.class);
            if (monitor != null) {

                // memory
                ui.addHeading(3, Locale.getString(VantarKey.ADMIN_MEMORY));
                ServiceHealthMonitor.MemoryStatus mStatus = monitor.getMemoryStatus();
                if (!mStatus.ok) {
                    ui.addErrorMessage("WARNING! LOW MEMORY");
                }
                ui  .addKeyValue("Designated memory", NumberUtil.getReadableByteSize(mStatus.max))
                    .addKeyValue("Allocated memory", NumberUtil.getReadableByteSize(mStatus.total))
                    .addKeyValue("Free memory", NumberUtil.getReadableByteSize(mStatus.free))
                    .addKeyValue("Used memory", NumberUtil.getReadableByteSize(mStatus.used))
                    .addKeyValue(
                        "Physical memory",
                        NumberUtil.getReadableByteSize(mStatus.physicalFree)
                            + " / " + NumberUtil.getReadableByteSize(mStatus.physicalTotal)
                    )
                    .addKeyValue(
                        "Swap memory",
                        NumberUtil.getReadableByteSize(mStatus.swapFree)
                            + " / " + NumberUtil.getReadableByteSize(mStatus.swapTotal)
                    );

                // disk
                for (ServiceHealthMonitor.DiskStatus s : monitor.getDiskStatus()) {
                    ui  .addEmptyLine()
                        .addHeading(3, Locale.getString(VantarKey.ADMIN_DISK_SPACE) + " " + s.name);
                    if (!s.ok) {
                        ui.addErrorMessage("WARNING! LOW DISK SPACE");
                    }
                    ui  .addKeyValue("Free", NumberUtil.getReadableByteSize(s.free))
                        .addKeyValue("Used", NumberUtil.getReadableByteSize(s.used))
                        .addKeyValue("Total", NumberUtil.getReadableByteSize(s.total));
                }

                // processor
                ui  .addEmptyLine()
                    .addHeading(3, Locale.getString(VantarKey.ADMIN_PROCESSOR));
                ServiceHealthMonitor.ProcessorStatus pStatus = monitor.getProcessorStatus();
                if (!pStatus.ok) {
                    ui.addErrorMessage("WARNING! JVM PROCESSOR USAGE IS HIGH");
                }
                ui  .addKeyValue("JVM processor usage", pStatus.jvmLoadPercent + "%")
                    .addKeyValue("System processor usage", pStatus.systemLoadPercent + "%");

                // services
                ui  .addEmptyLine()
                    .addHeading(3, Locale.getString(VantarKey.ADMIN_RUNNING_SERVICES_DEPENDENCIES));
                AdminServiceMonitoring.plotDependencyServicesStatus(ui);

                ui  .addEmptyLine()
                    .addHeading(3, Locale.getString(VantarKey.ADMIN_RUNNING_SERVICES_ME));
                AdminServiceMonitoring.plotServicesStatus(ui);

                ui.blockEnd();
            }
        }

        ui  .addEmptyLine(8)
            .addHeading(3, "Vantar system administration: " + VantarParam.VERSION)
            .addHeading(3, "Server ID: " + Services.ID)
            .finish();
    }

    public static WebUi getUi(Object title, Params params, HttpServletResponse response, boolean requiresRoot)
        throws FinishException {

        WebUi ui = new WebUi(params, response);

        if (requiresRoot && !AdminAuth.isRoot(ui)) {
            ui.addErrorMessage(Locale.getString(VantarKey.ADMIN_AUTH_TOKEN)).finish();
            throw new FinishException();
        }

        return ui
            .addMenu(getMenus(ui), getOnlineUserTitle(params))
            .addHeading(
                1,
                Settings.config.getProperty("title") + " : "
                    + (title instanceof LangKey ? Locale.getString((LangKey) title) : title.toString())
            )
            .beginBlock("main");
    }

    public static WebUi getUiDto(Object title, Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo)
        throws FinishException {

        WebUi ui = new WebUi(params, response);

        if (!AdminAuth.isRoot(ui)) {
            ui.addErrorMessage(Locale.getString(VantarKey.ADMIN_AUTH_TOKEN)).finish();
            throw new FinishException();
        }

        return ui
            .addMenu(getMenus(ui), getOnlineUserTitle(params))
            .setBreadcrumb(
                Settings.config.getProperty("title") + " : "
                    + (title instanceof LangKey ? Locale.getString((LangKey) title) : title.toString())
                ,
                dtoInfo
            )
            .beginBlock("main");
    }

    private static Map<String, String> getMenus(WebUi ui) throws FinishException {
        Map<String, String> menu = new LinkedHashMap<>(14);
        menu.put(Locale.getString(VantarKey.ADMIN_MENU_HOME), "/admin");
        if (AdminAuth.isRoot(ui)) {
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_MONITORING), "/admin/monitoring");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_DATA), "/admin/data");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_ADVANCED), "/admin/advanced");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_SCHEDULE), "/admin/schedule");
            menu.put(Locale.getString(VantarKey.ADMIN_MENU_PATCH), "/admin/patch");
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

    private static String getOnlineUserTitle(Params params) {
        try {
            return Services.getService(ServiceAuth.class).getCurrentUser(params).getFullName();
        } catch (ServiceException e) {
            return "auth service off";
        }
    }
}
