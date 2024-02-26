package com.vantar.admin.model.service;

import com.vantar.admin.model.database.*;
import com.vantar.admin.model.index.Admin;
import com.vantar.admin.model.queue.AdminQueue;
import com.vantar.common.*;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.queue.Queue;
import com.vantar.service.Services;
import com.vantar.service.healthmonitor.ServiceHealthMonitor;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.*;
import java.util.*;


public class AdminService {

    private static final int DEFAULT_DELAY_SECONDS = 1;
    private static final int DEFAULT_MAX_TRIES = 20;


    public static void startServices(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_SERVICE_START, params, response, true);
        if (!params.isChecked("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addInput(VantarKey.ADMIN_DELAY, "delay", Integer.toString(DEFAULT_DELAY_SECONDS), null, "ltr")
                .addInput(VantarKey.ADMIN_TRIES, "tries", Integer.toString(DEFAULT_MAX_TRIES), null, "ltr")
                .addCheckbox(VantarKey.ADMIN_SERVICE_INCLUDE_ALL_SERVICES, "all")
                .addCheckbox(VantarKey.ADMIN_SERVICE_INCLUDE_ALL_DB_SERVICES, "events")
                .addCheckbox(VantarKey.ADMIN_CONFIRM, WebUi.PARAM_CONFIRM)
                .addSubmit(VantarKey.ADMIN_SERVICE_START)
                .finish();
            return;
        }
        startServices(ui, params.isChecked("events"), params.isChecked("all"), params.getInteger("delay"), params.getInteger("tries"));
    }


    public static void stopServices(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_SERVICE_STOP), params, response, true);
        if (!params.isChecked("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addInput(VantarKey.ADMIN_DELAY, "delay", Integer.toString(DEFAULT_DELAY_SECONDS), null, "ltr")
                .addInput(VantarKey.ADMIN_TRIES, "tries", Integer.toString(DEFAULT_MAX_TRIES), null, "ltr")
                .addCheckbox(VantarKey.ADMIN_SERVICE_INCLUDE_ALL_SERVICES, "all")
                .addCheckbox(VantarKey.ADMIN_SERVICE_INCLUDE_ALL_DB_SERVICES, "events")
                .addCheckbox(VantarKey.ADMIN_CONFIRM, WebUi.PARAM_CONFIRM)
                .addSubmit(VantarKey.ADMIN_SERVICE_STOP)
                .finish();
            return;
        }
        stopServices(ui, params.isChecked("events"), params.isChecked("all"), params.getInteger("delay"), params.getInteger("tries"));
    }

    public static void factoryReset(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_FACTORY_RESET, params, response, true);
        if (!params.isChecked("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addInput(VantarKey.ADMIN_DELAY, "delay", Integer.toString(DEFAULT_DELAY_SECONDS), null, "ltr")
                .addInput(VantarKey.ADMIN_TRIES, "tries", Integer.toString(DEFAULT_MAX_TRIES), null, "ltr")
                .addInput(VantarKey.ADMIN_SERVICE_CLASSES_TO_RESERVE, "exclude", "", "ltr")
                .addCheckbox(VantarKey.ADMIN_SERVICE_START_SERVICES_AT_END, "ss")
                .addCheckbox(VantarKey.ADMIN_CONFIRM, WebUi.PARAM_CONFIRM)
                .addSubmit(VantarKey.ADMIN_DO)
                .blockEnd();

            getSystemObjects(ui);
            ui.finish();
            return;
        }

        String adminApp = Settings.getAdminApp();
        if (StringUtil.isNotEmpty(adminApp)) {
            try {
                Class<?> tClass = Class.forName(adminApp);
                Method method = tClass.getMethod("factoryResetBefore");
                method.invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

            }
        }

        int delay = params.getInteger("delay");
        int tries = params.getInteger("tries");
        Set<String> exclude = params.getStringSet("exclude");

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

        if (params.isChecked("ss")) {
            startServices(ui, false, true, delay, tries);
        } else {
            ui  .beginBox(VantarKey.ADMIN_SERVICES_ARE_STOPPED)
                .addHrefBlock(VantarKey.ADMIN_SERVICE_START, "/admin/services/start")
                .blockEnd();
        }

        if (StringUtil.isNotEmpty(adminApp)) {
            try {
                Class<?> tClass = Class.forName(adminApp);
                Method method = tClass.getMethod("factoryResetAfter");
                method.invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

            }
        }

        ui.finish();
    }

    public static void gc(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi("GC", params, response, true);

        if ("run".equals(params.getString("gc"))) {
            plotMemoryStatus(ui);
            ui.addEmptyLine().addHrefBlock("GarbageCollect", "?gc=run");
            System.gc();
        } else {
            ui.addHeading(2, VantarKey.ADMIN_MEMORY);
        }

        ui.addEmptyLine(3).addHeading(2, "After GC");

        plotMemoryStatus(ui);
        ui.finish();
    }

    public static void plotMemoryStatus(WebUi ui) {
        try {
            ServiceHealthMonitor monitor = Services.getService(ServiceHealthMonitor.class);
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
        } catch (ServiceException ignore) {

        }
    }

    private static void startServices(WebUi ui, boolean runEvents, boolean allServers, int delay, int tries) {
//        ui  .beginBox(VantarKey.ADMIN_SERVICE_START)
//            .beginBlock("pre")
//            .write();
//
//        // on this server
//        if (runEvents) {
//            Services.startServer();
//        } else {
//            Services.startServices();
//        }
//
//        // on other servers
//        if (allServers) {
//            Services.messaging.broadcast(VantarParam.MESSAGE_SERVICES_START, Boolean.toString(runEvents));
//        }
//
//        ui.sleep(delay * 1000 * 2);
//        List<String> enabled = Services.getEnabledServices();
//        int expectedCount = enabled.size();
//        for (int i = 0; i < tries; ++i) {
//            ui.sleep(delay * 1000);
//
//            Collection<Services.Service> s = Services.getServices();
//            int startedCount = s == null ? 0 : s.size();
//
//            if (startedCount == expectedCount) {
//                ui.addTextLine(VantarKey.ADMIN_SERVICE_ALL_STARTED).write();
//                break;
//            } else {
//                ui.addTextLine(startedCount + " of " + expectedCount + " services started...").write();
//            }
//        }
//
//        ui.blockEnd().write();
    }

    private static void stopServices(WebUi ui, boolean runEvents, boolean allServers, int delay, int tries) {
//        ui  .beginBox(VantarKey.ADMIN_SERVICE_STOP)
//            .beginBlock("pre")
//            .write();
//
//        // on this server
//        if (runEvents) {
//            Services.stop();
//        } else {
//            Services.stopServices();
//        }
//
//        // on other servers
//        if (allServers) {
//            Services.messaging.broadcast(VantarParam.MESSAGE_SERVICES_STOP, Boolean.toString(runEvents));
//        }
//
//        ui.sleep(delay * 1000 * 2);
//        List<String> enabled = Services.getEnabledServices();
//        int total = enabled.size();
//        for (int i = 0; i < tries; ++i) {
//            ui.sleep(delay * 1000);
//
//            Collection<Services.Service> s = Services.getServices();
//            int startedCount = s == null ? 0 : s.size();
//
//            if (startedCount == total) {
//                ui.addTextLine(VantarKey.ADMIN_SERVICE_ALL_STOPPED).write();
//                break;
//            } else {
//                ui.addTextLine(startedCount + " of " + total + " services not stopped yet...").write();
//            }
//        }
//
//        ui.blockEnd().blockEnd().write();
    }

    private static void getSystemObjects(WebUi ui) {
        ui  .beginBox(VantarKey.ADMIN_SYSYEM_CLASSES);

        if (Queue.connection != null) {
            String[] queues = Queue.connection.getQueues();
            if (queues != null) {
                ui.addKeyValue("RabbitMq", CollectionUtil.join(queues, "\n"));
                ui.addKeyValue(" ", " ");
            }
        }

        DtoDictionary.getStructure().forEach((dbms, info) -> {
            List<String> items = new ArrayList<>(14);
            info.forEach((objectName, i) -> items.add(objectName));
            ui.addKeyValue(dbms, CollectionUtil.join(items, "\n"));
            ui.addKeyValue(" ", " ");
        });

        ui.blockEnd();
    }
}
