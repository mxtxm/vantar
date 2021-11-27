package com.vantar.admin.model;

import com.vantar.common.*;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.service.Services;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.*;
import java.util.*;


public class AdminService {

    private static final String PARAM_DELAY = "delay";
    private static final String PARAM_TRIES = "tries";
    private static final String PARAM_ALL = "all";
    private static final String PARAM_EVENTS = "events";
    private static final String PARAM_EXCLUDE = "exclude";
    private static final String PARAM_SERVICES_START = "ss";
    private static final int DEFAULT_DELAY_SECONDS = 1;
    private static final int DEFAULT_MAX_TRIES = 20;


    public static void stopServices(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(Locale.getString(VantarKey.ADMIN_SERVICE_STOP)), params, response);
        if (ui == null) {
            return;
        }

        if (!params.isChecked("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addInput(Locale.getString(Locale.getString(VantarKey.ADMIN_DELAY)), PARAM_DELAY, Integer.toString(DEFAULT_DELAY_SECONDS), "ltr")
                .addInput(Locale.getString(Locale.getString(VantarKey.ADMIN_TRIES)), PARAM_TRIES, Integer.toString(DEFAULT_MAX_TRIES), "ltr")
                .addCheckbox(Locale.getString(VantarKey.ADMIN_SERVICE_INCLUDE_ALL_SERVICES), PARAM_ALL)
                .addCheckbox(Locale.getString(VantarKey.ADMIN_SERVICE_INCLUDE_ALL_DB_SERVICES), PARAM_EVENTS)
                .addCheckbox(Locale.getString(VantarKey.ADMIN_CONFIRM), WebUi.PARAM_CONFIRM)
                .addSubmit(Locale.getString(VantarKey.ADMIN_SERVICE_STOP))
                .finish();
            return;
        }
        stopServices(ui, params.isChecked(PARAM_EVENTS), params.isChecked(PARAM_ALL), params.getInteger(PARAM_DELAY), params.getInteger(PARAM_TRIES));
    }

    public static void startServices(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(Locale.getString(VantarKey.ADMIN_SERVICE_START)), params, response);
        if (ui == null) {
            return;
        }

        if (!params.isChecked("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addInput(Locale.getString(Locale.getString(VantarKey.ADMIN_DELAY)), PARAM_DELAY, Integer.toString(DEFAULT_DELAY_SECONDS), "ltr")
                .addInput(Locale.getString(Locale.getString(VantarKey.ADMIN_TRIES)), PARAM_TRIES, Integer.toString(DEFAULT_MAX_TRIES), "ltr")
                .addCheckbox(Locale.getString(VantarKey.ADMIN_SERVICE_INCLUDE_ALL_SERVICES), PARAM_ALL)
                .addCheckbox(Locale.getString(VantarKey.ADMIN_SERVICE_INCLUDE_ALL_DB_SERVICES), PARAM_EVENTS)
                .addCheckbox(Locale.getString(VantarKey.ADMIN_CONFIRM), WebUi.PARAM_CONFIRM)
                .addSubmit(Locale.getString(VantarKey.ADMIN_SERVICE_START))
                .finish();
            return;
        }
        startServices(ui, params.isChecked(PARAM_EVENTS), params.isChecked(PARAM_ALL), params.getInteger(PARAM_DELAY), params.getInteger(PARAM_TRIES));
    }

    public static void factoryReset(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_FACTORY_RESET), params, response);
        if (ui == null) {
            return;
        }

        if (!params.isChecked("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addInput(Locale.getString(Locale.getString(VantarKey.ADMIN_DELAY)), PARAM_DELAY, Integer.toString(DEFAULT_DELAY_SECONDS), "ltr")
                .addInput(Locale.getString(Locale.getString(VantarKey.ADMIN_TRIES)), PARAM_TRIES, Integer.toString(DEFAULT_MAX_TRIES), "ltr")
                .addInput(Locale.getString(VantarKey.ADMIN_SERVICE_CLASSES_TO_RESERVE), PARAM_EXCLUDE, "", "ltr")
                .addCheckbox(Locale.getString(VantarKey.ADMIN_SERVICE_START_SERVICES_AT_END), PARAM_SERVICES_START)
                .addCheckbox(Locale.getString(VantarKey.ADMIN_CONFIRM), WebUi.PARAM_CONFIRM)
                .addSubmit(Locale.getString(VantarKey.ADMIN_DO))
                .containerEnd();

            getSystemObjects(ui);
            ui.finish();
            return;
        }

        String appPackage = Settings.getAppPackage();
        if (StringUtil.isNotEmpty(appPackage)) {
            try {
                Class<?> tClass = Class.forName(appPackage + ".business.admin.model.AdminApp");
                Method method = tClass.getMethod("factoryResetBefore");
                method.invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

            }
        }

        int delay = params.getInteger(PARAM_DELAY);
        int tries = params.getInteger(PARAM_TRIES);
        Set<String> exclude = params.getStringSet(PARAM_EXCLUDE);

        stopServices(ui, false, true, delay, tries);

        ui.sleep(delay);

        AdminQueue.purge(ui, delay, tries, exclude);
        AdminDatabase.purgeElastic(ui, delay, true, exclude);
        AdminDatabase.purgeMongo(ui, delay, exclude);
        AdminDatabase.purgeSql(ui, delay, true, exclude);

        AdminDatabase.synchSql(ui);
        AdminDatabase.synchElastic(ui);
        AdminDatabase.createMongoIndex(ui, true);
        AdminDatabase.createSqlIndex(ui, true);

        AdminDatabase.importElastic(ui, delay, true, exclude);
        AdminDatabase.importMongo(ui, delay, true, exclude);
        AdminDatabase.importSql(ui, delay, true, exclude);

        AdminData.statusElastic(ui);
        AdminData.statusMongo(ui);
        AdminData.statusSql(ui);

        if (params.isChecked(PARAM_SERVICES_START)) {
            startServices(ui, false, true, delay, tries);
        } else {
            ui  .beginBox(Locale.getString(VantarKey.ADMIN_SERVICES_ARE_STOPPED))
                .addBlockLink(Locale.getString(VantarKey.ADMIN_SERVICE_START), "/admin/system/services/start")
                .containerEnd();
        }

        appPackage = Settings.getAppPackage();
        if (StringUtil.isNotEmpty(appPackage)) {
            try {
                Class<?> tClass = Class.forName(appPackage + ".business.admin.model.AdminApp");
                Method method = tClass.getMethod("factoryResetAfter");
                method.invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

            }
        }

        ui.finish();
    }

    public static void stopServices(WebUi ui, boolean runEvents, boolean allServers, int delay, int tries) {
        ui  .beginBox(Locale.getString(Locale.getString(VantarKey.ADMIN_SERVICE_STOP)))
            .beginPre()
            .write();

        // on this server
        if (runEvents) {
            Services.stop();
        } else {
            Services.stopServicesOnly();
        }

        // on other servers
        if (allServers) {
            Services.messaging.broadcast(VantarParam.MESSAGE_SERVICES_STOP, Boolean.toString(runEvents));
        }

        for (int i = 0; i < tries; ++i) {
            boolean isEmpty = true;
            synchronized (Services.upServices) {
                for (Map.Entry<String, Services.ServiceInfo> entry : Services.upServices.entrySet()) {
                    String name = entry.getKey();
                    Services.ServiceInfo info = entry.getValue();

                    if (!allServers) {
                        if (info.instanceCount != 0) {
                            isEmpty = false;
                            ui.addTextLine(name + ": " + info.instanceCount + " running instances, goal is 0").write();
                        }
                    } else if (info.isEnabledOnThisServer) {
                        if (info.instanceCount != 0) {
                            isEmpty = false;
                            ui.addTextLine(name + ": 1 running instances, goal is 0").write();
                        }
                    }
                }
            }

            if (isEmpty) {
                ui.addTextLine(Locale.getString(VantarKey.ADMIN_SERVICE_ALL_STOPPED)).write();
                break;
            }

            ui.sleep(delay * 1000);
        }

        ui.containerEnd().containerEnd().write();
    }

    public static void startServices(WebUi ui, boolean runEvents, boolean allServers, int delay, int tries) {
        ui  .beginBox(Locale.getString(Locale.getString(VantarKey.ADMIN_SERVICE_START)))
            .beginPre()
            .write();

        // on this server
        if (runEvents) {
            Services.start();
        } else {
            Services.startServicesOnly();
        }
        // on other servers
        //Services.resetTotalServiceCount();
        if (allServers) {
            Services.messaging.broadcast(VantarParam.MESSAGE_SERVICES_START, Boolean.toString(runEvents));
        }

        ui.sleep(delay * 1000 * 2);

        for (int i = 0; i < tries; ++i) {
            ui.sleep(delay * 1000);

            int expectedCount = 0;
            synchronized (Services.serviceCount) {
                for (Map.Entry<String, Integer>  entry : Services.serviceCount.entrySet()) {
                    if (allServers) {
                        expectedCount += entry.getValue();
                    } else if (entry.getKey().equals(Services.ID)) {
                        ++expectedCount;
                    }
                }
            }

            int count = 0;
            synchronized (Services.upServices) {
                for (Map.Entry<String, Services.ServiceInfo> entry : Services.upServices.entrySet()) {
                    String name = entry.getKey();
                    Services.ServiceInfo info = entry.getValue();

                    if (allServers) {
                        count += info.instanceCount;
                        ui.addTextLine(name + ": " + info.instanceCount + " running instances");
                    } else if (info.isEnabledOnThisServer) {
                        ++count;
                    }
                }
            }

            if (count == expectedCount) {
                ui.addTextLine(Locale.getString(VantarKey.ADMIN_SERVICE_ALL_STARTED)).write();
                break;
            } else {
                ui.addTextLine(count + " total running instances, goal is " + expectedCount).write();
            }
        }

        ui.containerEnd().write();
    }

    private static void getSystemObjects(WebUi ui) {
        ui  .beginBox(Locale.getString(VantarKey.ADMIN_SYSYEM_CLASSES));

        String[] queues = StringUtil.split(Settings.queue().getRabbitMqQueues(), VantarParam.SEPARATOR_BLOCK);
        if (queues != null) {
            List<String> items = new ArrayList<>();
            for (String q : queues) {
                items.add(StringUtil.split(q, VantarParam.SEPARATOR_COMMON)[0]);
            }
            ui.addKeyValue("RabbitMq", CollectionUtil.join(items, "\n"));
            ui.addKeyValue(" ", " ");
        }

        DtoDictionary.getStructure().forEach((dbms, info) -> {
            List<String> items = new ArrayList<>();
            info.forEach((objectName, i) -> items.add(objectName));
            ui.addKeyValue(dbms, CollectionUtil.join(items, "\n"));
            ui.addKeyValue(" ", " ");
        });

        ui.containerEnd();
    }
}
