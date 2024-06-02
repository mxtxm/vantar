package com.vantar.admin.service;

import com.vantar.admin.database.dbms.importdata.AdminImportData;
import com.vantar.admin.database.dbms.indexing.AdminDatabaseIndex;
import com.vantar.admin.database.dbms.purge.AdminPurge;
import com.vantar.admin.database.dbms.synch.AdminSynch;
import com.vantar.admin.index.Admin;
import com.vantar.admin.queue.AdminQueue;
import com.vantar.common.*;
import com.vantar.exception.*;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.queue.common.Que;
import com.vantar.service.Services;
import com.vantar.service.healthmonitor.ServiceHealthMonitor;
import com.vantar.service.log.ServiceLog;
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
        if (!params.contains("f") || StringUtil.isEmpty(service) || StringUtil.isEmpty(action)
            || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui.beginFormPost()
                .addSelect(VantarKey.ADMIN_SERVICE, "s", getServiceNames())
                .addSelect(VantarKey.ADMIN_ACTION, "a", new String[]{"Stop", "Start", "Restart"})
                .addInput(VantarKey.ADMIN_DELAY, "delay", 1, null, "ltr")
                .addInput(VantarKey.ADMIN_ATTEMPTS, "tries", 20, null, "ltr")
                .addCheckbox(VantarKey.ADMIN_SERVICE_ALL_SERVERS, "allServers")
                .addCheckbox(VantarKey.ADMIN_CONFIRM, WebUi.PARAM_CONFIRM)
                .addSubmit(VantarKey.ADMIN_SERVICE_START)
                .finish();
            return;
        }

        int delay = params.getInteger("delay", 1);
        int tries = params.getInteger("tries", 20);

        if (params.isChecked("allServers")) {
            Services.messaging.broadcast(
                VantarParam.MESSAGE_SERVICES_ACTION,
                action,
                service,
                delay,
                tries
            );
        }

        serviceAction(
            ui,
            action,
            service,
            delay,
            tries
        );

        ui.finish();
    }

    public static void factoryReset(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_FACTORY_RESET, params, response, true);

        if (!params.contains("f") || !params.isChecked(WebUi.PARAM_CONFIRM)) {
            ui  .beginFormPost()
                .addInput(VantarKey.ADMIN_DELAY, "delay", 1, null, "ltr")
                .addInput(VantarKey.ADMIN_ATTEMPTS, "tries", 20, null, "ltr")
                .addInput(VantarKey.ADMIN_EXCLUDE, "exclude", "", "ltr")
                .addCheckbox(VantarKey.ADMIN_CONFIRM, WebUi.PARAM_CONFIRM)
                .addSubmit(VantarKey.ADMIN_SUBMIT)
                .blockEnd();
            getSystemObjects(ui);
            ui.finish();
            return;
        }

        factoryReset(
            ui,
            params.getInteger("delay", 1),
            params.getInteger("tries", 20),
            params.getStringSet("exclude")
        );
        ui.finish();
    }

    public static void factoryReset(WebUi ui, int delay, int tries, Set<String> exclude) {
        String adminApp = Settings.getAdminApp();
        if (StringUtil.isNotEmpty(adminApp)) {
            try {
                Class<?> tClass = Class.forName(adminApp);
                Method method = tClass.getMethod("factoryResetBefore");
                method.invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

            }
        }

        if (ui != null) {
            ui.beginBox("Service pause").write();
        }
        serviceAction(
            ui,
            "pause",
            "A",
            delay,
            tries
        );
        if (ui != null) {
            ui  .blockEnd()
                .write()
                .sleepMs(delay)
                .beginBox("Data purge")
                .write();
        }
        AdminQueue.purge(ui, delay, tries, exclude);
        AdminPurge.purgeMongo(ui, exclude, null);
        AdminPurge.purgeSql(ui, exclude, null, false);
        AdminPurge.purgeElastic(ui, exclude, null, false);
        if (ui != null) {
            ui.blockEnd()
                .write()
                .sleepMs(delay)
                .beginBox("Data synch")
                .write();
        }
        AdminSynch.synchSql(ui);
        AdminSynch.synchElastic(ui);
        if (ui != null) {
            ui  .blockEnd()
                .write()
                .sleepMs(delay)
                .beginBox("Database index")
                .write();
        }
        AdminDatabaseIndex.createIndexMongo(ui, true, exclude, null);
        AdminDatabaseIndex.createIndexSql(ui, true);
        if (ui != null) {
            ui  .blockEnd()
                .write()
                .sleepMs(delay)
                .beginBox("Data import")
                .write();
        }
        AdminImportData.importMongo(ui, true, exclude, null);
        AdminImportData.importSql(ui, true, exclude, null);
        AdminImportData.importElastic(ui, true, exclude, null);
        if (ui != null) {
            ui  .blockEnd()
                .write()
                .sleepMs(delay)
                .beginBox("Service resume")
                .write();
        }
        serviceAction(
            ui,
            "resume",
            "A",
            delay,
            tries
        );
        if (ui != null) {
            ui  .blockEnd()
                .write()
                .sleepMs(delay);
        }
        if (StringUtil.isNotEmpty(adminApp)) {
            try {
                Class<?> tClass = Class.forName(adminApp);
                Method method = tClass.getMethod("factoryResetAfter");
                method.invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

            }
        }
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

    /**
     * action= stop/start/restart/pause/resume
     * service name or "A"=all services
     */
    public static void serviceAction(
        WebUi ui,
        String action,
        String service,
        Integer delay,
        Integer tries) {

        // > > > on one service
        if (!"A".equals(service)) {
            try {
                Services.Service s = Services.getService(service);
                // > > > STOP
                if (action.equalsIgnoreCase("stop")) {
                    for (int i = 0; i < tries; ++i) {
                        s.stop();
                        sleep(delay);
                        if (!s.isUp()) {
                            addMessage(ui, false, VantarKey.ADMIN_SERVICE_STOPPED);
                            return;
                        }
                        addMessage(ui, false, (i + 1) + " of " + tries + " try...");
                    }

                // > > > STAND BY
                } else if (action.equalsIgnoreCase("pause")) {
                    s.pause();
                    sleep(delay);
                    addMessage(ui, false, VantarKey.ADMIN_SERVICE_PAUSED);
                    return;

                // > > > RESUME
                } else if (action.equalsIgnoreCase("resume")) {
                    s.resume();
                    sleep(delay);
                    addMessage(ui, false, VantarKey.ADMIN_SERVICE_RESUMED);
                    return;

                // > > > START
                } else if (action.equalsIgnoreCase("start")) {
                    for (int i = 0; i < tries; ++i) {
                        s.start();
                        sleep(delay);
                        if (s.isUp()) {
                            addMessage(ui, false, VantarKey.ADMIN_SERVICE_STARTED);
                            return;
                        }
                        addMessage(ui, false, (i + 1) + " of " + tries + " try...");
                    }

                // > > > RESTART
                } else if (action.equalsIgnoreCase("restart")) {
                    boolean stopped = false;
                    for (int i = 0; i < tries; ++i) {
                        s.stop();
                        sleep(delay);
                        if (!s.isUp()) {
                            addMessage(ui, false, VantarKey.ADMIN_SERVICE_STOPPED);
                            stopped = true;
                            break;
                        }
                        addMessage(ui, false, (i + 1) + " of " + tries + " try...");
                    }
                    if (stopped) {
                        for (int i = 0; i < tries; ++i) {
                            s.start();
                            sleep(delay);
                            if (s.isUp()) {
                                addMessage(ui, false, VantarKey.ADMIN_SERVICE_STARTED);
                                return;
                            }
                            addMessage(ui, false, (i + 1) + " of " + tries + " try...");
                        }
                    }
                }

                addMessage(ui, true, VantarKey.ADMIN_FAILED);
            } catch (Exception e) {
                addMessage(ui, true, e);
            }
            return;
        }
        // on one service < < <

        // > > > on all services
        try {
            // > > > STOP
            if (action.equalsIgnoreCase("stop")) {
                for (int i = 0; i < tries; ++i) {
                    Services.stopServices();
                    sleep(delay);
                    Collection<Services.Service> up = Services.getServices();
                    if (ObjectUtil.isEmpty(up)) {
                        for (String enabledService : Services.getEnabledServices()) {
                            addMessage(ui, false, "  > stopped: " + enabledService);
                        }
                        addMessage(ui, false, VantarKey.ADMIN_SERVICE_STOPPED);
                        return;
                    }
                    addMessage(ui, false, (i + 1) + " of " + tries + " try...");
                    for (Services.Service s : up) {
                        addMessage(ui, false, "  ... pending to stop: " + s.getClass().getSimpleName());
                    }
                }

            // > > > STAND BY
            } else if (action.equalsIgnoreCase("pause")) {
                Services.pauseServices();
                sleep(delay);
                addMessage(ui, false, VantarKey.ADMIN_SERVICE_PAUSED);
                return;

            // > > > STAND BY
            } else if (action.equalsIgnoreCase("resume")) {
                Services.resumeServices();
                sleep(delay);
                addMessage(ui, false, VantarKey.ADMIN_SERVICE_RESUMED);
                return;

            // > > > START
            } else if (action.equalsIgnoreCase("start")) {
                for (int i = 0; i < tries; ++i) {
                    Services.startServices();
                    sleep(delay);
                    boolean allAreUp = true;
                    for (Class<?> enabledService : Services.getEnabledServiceClasses()) {
                        if (Services.isUp(enabledService)) {
                            addMessage(ui, false, "  > running: " + enabledService);
                        } else {
                            addMessage(ui, false, "  ... pending to start: " + enabledService);
                            allAreUp = false;
                        }
                    }
                    if (allAreUp) {
                        addMessage(ui, false, VantarKey.ADMIN_SERVICE_STARTED);
                        return;
                    }
                    addMessage(ui, false, (i + 1) + " of " + tries + " try...");
                }

            // > > > RESTART
            } else if (action.equalsIgnoreCase("restart")) {
                boolean stopped = false;
                for (int i = 0; i < tries; ++i) {
                    Services.stopServices();
                    sleep(delay);
                    Collection<Services.Service> up = Services.getServices();
                    if (ObjectUtil.isEmpty(up)) {
                        for (String enabledService : Services.getEnabledServices()) {
                            addMessage(ui, false, "  > stopped: " + enabledService);
                        }
                        addMessage(ui, false, VantarKey.ADMIN_SERVICE_STOPPED);
                        stopped = true;
                        break;
                    }
                    addMessage(ui, false, (i + 1) + " of " + tries + " try...");
                    for (Services.Service s : up) {
                        addMessage(ui, false, "  ... pending to stop: " + s.getClass().getSimpleName());
                    }
                }
                if (stopped) {
                    for (int i = 0; i < tries; ++i) {
                        Services.startServices();
                        sleep(delay);
                        boolean allAreUp = true;
                        for (Class<?> enabledService : Services.getEnabledServiceClasses()) {
                            if (Services.isUp(enabledService)) {
                                addMessage(ui, false, "  > running: " + enabledService);
                            } else {
                                addMessage(ui, false, "  ... pending to start: " + enabledService);
                                allAreUp = false;
                            }
                        }
                        if (allAreUp) {
                            addMessage(ui, false, VantarKey.ADMIN_SERVICE_STARTED);
                            return;
                        }
                        addMessage(ui, false, (i + 1) + " of " + tries + " try...");
                    }
                }
            }

            addMessage(ui, true, VantarKey.ADMIN_FAILED);
        } catch (Exception e) {
            addMessage(ui, true, e);
        }
        // on all services < < <
    }

    private static void addMessage(WebUi ui, boolean error, Object msg) {
        if (ui == null) {
            if (msg instanceof Exception) {
                ServiceLog.error(AdminService.class, "failed ", msg);
            } else if (error) {
                ServiceLog.error(AdminService.class, msg instanceof LangKey ? Locale.getString((LangKey) msg) : msg.toString());
            } else {
                ServiceLog.info(AdminService.class, msg instanceof LangKey ? Locale.getString((LangKey) msg) : msg.toString());
            }
        } else {
            if (error) {
                ui.addErrorMessage(msg).write();
            } else {
                ui.addMessage(msg).write();
            }
            ui.write();
        }
    }

    private static void sleep(int s) {
        try {
            Thread.sleep(s * 1000);
        } catch (InterruptedException ignore) {

        }
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

    private static void getSystemObjects(WebUi ui) {
        ui.beginBox(VantarKey.ADMIN_SYSYEM_OBJECTS);

        if (Services.isUp(Que.Engine.RABBIT)) {
            String[] queues = Que.rabbit.getQueues();
            if (queues != null) {
                ui.addKeyValue(Que.Engine.RABBIT.name(), CollectionUtil.join(queues, "\n"));
                ui.addKeyValue(" ", " ");
            }
        }

        DtoDictionary.getManifest().forEach((dbms, info) -> {
            List<String> items = new ArrayList<>(14);
            info.forEach((objectName, i) -> items.add(objectName));
            ui.addKeyValue(dbms, CollectionUtil.join(items, "\n"));
            ui.addKeyValue(" ", " ");
        });

        ui.blockEnd();
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
}
