package com.vantar.admin.modelw.service;

import com.vantar.admin.model.database.*;
import com.vantar.admin.model.index.Admin;
import com.vantar.admin.modelw.queue.AdminQueue;
import com.vantar.common.*;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.queue.Queue;
import com.vantar.service.Services;
import com.vantar.service.healthmonitor.ServiceHealthMonitor;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.*;
import java.util.*;


public class AdminService {

    public static void serviceAction(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUiAllowIfAuthOff(VantarKey.ADMIN_SERVICE_ACTION, params, response);

        String service = params.getString("s");
        String action = params.getString("a");
        if (!params.isChecked("f") || StringUtil.isEmpty(service) || StringUtil.isEmpty(action)
            || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addSelect(VantarKey.ADMIN_SERVICE, "s", getServiceNames())
                .addSelect(VantarKey.ADMIN_ACTION, "a", new String[] {"Stop", "Start", "Restart"})
                .addInput(VantarKey.ADMIN_DELAY, "delay", 1, null, "ltr")
                .addInput(VantarKey.ADMIN_TRIES, "tries", 20, null, "ltr")
                .addCheckbox(VantarKey.ADMIN_SERVICE_ALL_DB_SERVICES, "runEvents")
                .addCheckbox(VantarKey.ADMIN_SERVICE_ALL_SERVERS, "allServers")
                .addCheckbox(VantarKey.ADMIN_CONFIRM, WebUi.PARAM_CONFIRM)
                .addSubmit(VantarKey.ADMIN_SERVICE_START)
                .finish();
            return;
        }

        Integer delay = params.getInteger("delay");
        Integer tries = params.getInteger("tries");
        boolean allServers = params.isChecked("allServers");
        boolean runEvents = params.isChecked("runEvents");

        if (allServers) {
            Services.messaging.broadcast(VantarParam.MESSAGE_SERVICES_ACTION, service + "|" + action + "|" + runEvents);
        }

        // > > > on one service
        if (!"A".equals(service)) {
            try {
                Services.Service s = Services.getService(service);
                // > > > STOP
                if (action.equalsIgnoreCase("stop")) {
                    for (int i = 0; i < tries; ++i) {
                        s.stop();
                        ui.sleep(delay * 1000);
                        if (!s.isUp()) {
                            ui.addMessage(VantarKey.ADMIN_SERVICE_STOPPED).finish();
                            return;
                        }
                        ui.addMessage((i + 1) + " of " + tries + " try...").write();
                    }

                // > > > START
                } else if (action.equalsIgnoreCase("start")) {
                    for (int i = 0; i < tries; ++i) {
                        s.start();
                        ui.sleep(delay * 1000);
                        if (s.isUp()) {
                            ui.addMessage(VantarKey.ADMIN_SERVICE_STARTED).finish();
                            return;
                        }
                        ui.addMessage((i + 1) + " of " + tries + " try...").write();
                    }

                // > > > RESTART
                } else if (action.equalsIgnoreCase("restart")) {
                    boolean stopped = false;
                    for (int i = 0; i < tries; ++i) {
                        s.stop();
                        ui.sleep(delay * 1000);
                        if (!s.isUp()) {
                            ui.addMessage(VantarKey.ADMIN_SERVICE_STOPPED).write();
                            stopped = true;
                            break;
                        }
                        ui.addMessage((i + 1) + " of " + tries + " try...").write();
                    }
                    if (stopped) {
                        for (int i = 0; i < tries; ++i) {
                            s.start();
                            ui.sleep(delay * 1000);
                            if (s.isUp()) {
                                ui.addMessage(VantarKey.ADMIN_SERVICE_STARTED).finish();
                                return;
                            }
                            ui.addMessage((i + 1) + " of " + tries + " try...").write();
                        }
                    }
                }

                ui.addErrorMessage(VantarKey.ADMIN_FAILED).write();
            } catch (Exception e) {
                ui.addErrorMessage(e);
            }
            ui.finish();
            return;
        }
        // on one service < < <

        // > > > on all services
        // > > > STOP
        try {
            if (action.equalsIgnoreCase("stop")) {
                for (int i = 0; i < tries; ++i) {
                    Services.stopServices(runEvents);
                    ui.sleep(delay * 1000);
                    Collection<Services.Service> up = Services.getServices();
                    if (ObjectUtil.isEmpty(up)) {
                        for (String enabledService : Services.getEnabledServices()) {
                            ui.addMessage("  > stopped: " + enabledService).write();
                        }
                        ui.addMessage(VantarKey.ADMIN_SERVICE_STOPPED).finish();
                        return;
                    }
                    ui.addMessage((i + 1) + " of " + tries + " try...").write();
                    for (Services.Service s : up) {
                        ui.addMessage("  ... pending to stop: " + s.getClass().getSimpleName()).write();
                    }
                }

            // > > > START
            } else if (action.equalsIgnoreCase("start")) {
                for (int i = 0; i < tries; ++i) {
                    Services.startServices(runEvents);
                    ui.sleep(delay * 1000);
                    boolean allAreUp = true;
                    for (Class<?> enabledService : Services.getEnabledServiceClasses()) {
                        if (Services.isUp(enabledService)) {
                            ui.addMessage("  > running: " + enabledService);
                        } else {
                            ui.addMessage("  ... pending to start: " + enabledService);
                            allAreUp = false;
                        }
                    }
                    if (allAreUp) {
                        ui.addMessage(VantarKey.ADMIN_SERVICE_STARTED).finish();
                        return;
                    }
                    ui.addMessage((i + 1) + " of " + tries + " try...").write();
                }

            // > > > RESTART
            } else if (action.equalsIgnoreCase("restart")) {
                boolean stopped = false;
                for (int i = 0; i < tries; ++i) {
                    Services.stopServices(runEvents);
                    ui.sleep(delay * 1000);
                    Collection<Services.Service> up = Services.getServices();
                    if (ObjectUtil.isEmpty(up)) {
                        for (String enabledService : Services.getEnabledServices()) {
                            ui.addMessage("  > stopped: " + enabledService).write();
                        }
                        ui.addMessage(VantarKey.ADMIN_SERVICE_STOPPED).finish();
                        stopped = true;
                        break;
                    }
                    ui.addMessage((i + 1) + " of " + tries + " try...").write();
                    for (Services.Service s : up) {
                        ui.addMessage("  ... pending to stop: " + s.getClass().getSimpleName()).write();
                    }
                }
                if (stopped) {
                    for (int i = 0; i < tries; ++i) {
                        Services.startServices(runEvents);
                        ui.sleep(delay * 1000);
                        boolean allAreUp = true;
                        for (Class<?> enabledService : Services.getEnabledServiceClasses()) {
                            if (Services.isUp(enabledService)) {
                                ui.addMessage("  > running: " + enabledService);
                            } else {
                                ui.addMessage("  ... pending to start: " + enabledService);
                                allAreUp = false;
                            }
                        }
                        if (allAreUp) {
                            ui.addMessage(VantarKey.ADMIN_SERVICE_STARTED).finish();
                            return;
                        }
                        ui.addMessage((i + 1) + " of " + tries + " try...").write();
                    }
                }
            }

            ui.addErrorMessage(VantarKey.ADMIN_FAILED).write();
        } catch (Exception e) {
            ui.addErrorMessage(e);
        }
        ui.finish();
        // on all services < < <
    }
















    private static Map<String, Object> getServiceNames() {
        Collection<Services.Service> services = Services.getServices();
        if (services == null) {
            return new HashMap<>(1, 1);
        }
        Map<String, Object> names = new HashMap<>(services.size(), 1);
        names.put("A", "All services");
        for (Services.Service s : services) {
            names.put(s.getClass().getName(), s.getClass().getSimpleName());
        }
        return names;
    }


    public static void factoryReset(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_FACTORY_RESET, params, response, true);
        if (!params.isChecked("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addInput(VantarKey.ADMIN_DELAY, "delay", 1, null, "ltr")
                .addInput(VantarKey.ADMIN_TRIES, "tries", 20, null, "ltr")
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
            ui  .beginBox(VantarKey.ADMIN_SERVICE_STOPPED)
                .addHrefBlock(VantarKey.ADMIN_SERVICE_START, "/admin/service/start")
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
            ui.addHeading(2, "Before GC");
            plotMemoryStatus(ui);
            System.gc();
            ui.addEmptyLine(3).addHeading(2, "After GC");
            plotMemoryStatus(ui);
        } else {
            ui.addHeading(2, VantarKey.ADMIN_MEMORY);
            plotMemoryStatus(ui);
            ui.addEmptyLine().addHrefBlock("Garbage collect", "?gc=run");
        }
        ui.finish();
    }

    private static void startServices(WebUi ui, boolean runEvents, boolean allServers, int delay, int tries) {
        ui  .beginBox(VantarKey.ADMIN_SERVICE_START)
            .beginBlock("pre")
            .write();

        // on this server
        if (runEvents) {
            Services.startServer();
        } else {
            Services.startServices();
        }

        // on other servers
        if (allServers) {
            Services.messaging.broadcast(VantarParam.MESSAGE_SERVICES_START, Boolean.toString(runEvents));
        }

        ui.sleep(delay * 1000 * 2);
        List<String> enabled = Services.getEnabledServices();
        int expectedCount = enabled.size();
        for (int i = 0; i < tries; ++i) {
            ui.sleep(delay * 1000);

            Collection<Services.Service> s = Services.getServices();
            int startedCount = s == null ? 0 : s.size();

            if (startedCount == expectedCount) {
                ui.addTextLine(VantarKey.ADMIN_SERVICE_STARTED).write();
                break;
            } else {
                ui.addTextLine(startedCount + " of " + expectedCount + " services started...").write();
            }
        }

        ui.blockEnd().write();
    }

    private static void stopServices(WebUi ui, boolean runEvents, boolean allServers, int delay, int tries) {
        ui  .beginBox(VantarKey.ADMIN_SERVICE_STOP)
            .beginBlock("pre")
            .write();

        // on this server
        if (runEvents) {
            Services.stop();
        } else {
            Services.stopServices();
        }

        // on other servers
        if (allServers) {
            Services.messaging.broadcast(VantarParam.MESSAGE_SERVICES_STOP, Boolean.toString(runEvents));
        }

        ui.sleep(delay * 1000 * 2);
        List<String> enabled = Services.getEnabledServices();
        int total = enabled.size();
        for (int i = 0; i < tries; ++i) {
            ui.sleep(delay * 1000);

            Collection<Services.Service> s = Services.getServices();
            int startedCount = s == null ? 0 : s.size();

            if (startedCount == total) {
                ui.addTextLine(VantarKey.ADMIN_SERVICE_STOPPED).write();
                break;
            } else {
                ui.addTextLine(startedCount + " of " + total + " services not stopped yet...").write();
            }
        }

        ui.blockEnd().blockEnd().write();
    }

    private static void getSystemObjects(WebUi ui) {
        ui.beginBox(VantarKey.ADMIN_SYSYEM_CLASSES);

        if (Queue.connection != null) {
            String[] queues = Queue.connection.getQueues();
            if (queues != null) {
                ui.addKeyValue("RabbitMQ", CollectionUtil.join(queues, "\n"));
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
}
