package com.vantar.service.log;

import com.vantar.common.VantarParam;
import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.queue.*;
import com.vantar.queue.Queue;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.service.log.dto.*;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.json.Json;
import com.vantar.web.*;
import org.bson.Document;
import org.slf4j.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class ServiceLog implements Services.Service  {

    public static final Logger log = LoggerFactory.getLogger(ServiceLog.class);

    private volatile boolean pause = false;
    private static final int MAX_ERROR_FETCH = 100;
    private static final String DEBUG = "DEBUG";
    private static final String INFO = "INFO";
    private static final String ERROR = "ERROR";
    private static final String FATAL = "FATAL";
    private static Boolean DELAYED_STORE_ENABLED;

    private volatile boolean serviceUp = false;
    private volatile boolean lastSuccess = true;
    private List<String> logs;
    private final AtomicBoolean isBusy = new AtomicBoolean();
    private Event event;

    private ScheduledExecutorService scheduleDelayedLogStore;

    // > > > service params injected from config
    public String dbms;
    public Boolean delayedStoreEnabled;
    public Integer insertIntervalMin;
    public Boolean request;
    public Boolean response;
    // < < <

    // > > > service methods

    @Override
    public void start() {
        serviceUp = true;
        pause = false;
        isBusy.set(false);
        if (!delayedStoreEnabled) {
            return;
        }

        scheduleDelayedLogStore = Executors.newSingleThreadScheduledExecutor();
        scheduleDelayedLogStore.scheduleWithFixedDelay(
            this::scheduleLogStore,
            insertIntervalMin,
            insertIntervalMin,
            TimeUnit.MINUTES
        );
    }

    @Override
    public void stop() {
        isBusy.set(false);
        if (!delayedStoreEnabled) {
            return;
        }
        scheduleDelayedLogStore.shutdown();
        serviceUp = false;
    }

    @Override
    public synchronized void pause() {
        pause = true;
    }

    @Override
    public synchronized void resume() {
        pause = false;
    }

    @Override
    public boolean isUp() {
        return serviceUp;
    }

    @Override
    public boolean isOk() {
        return serviceUp
            && lastSuccess
            && scheduleDelayedLogStore != null
            && !scheduleDelayedLogStore.isShutdown()
            && !scheduleDelayedLogStore.isTerminated();
    }

    @Override
    public List<String> getLogs() {
        return logs;
    }

    private void setLog(String msg) {
        if (logs == null) {
            logs = new ArrayList<>(5);
        }
        logs.add(msg);
    }

    public ServiceLog setEvent(Event event) {
        this.event = event;
        return this;
    }

    private static boolean getDelayedStoreEnabled() {
        if (DELAYED_STORE_ENABLED == null) {
            try {
                DELAYED_STORE_ENABLED = Services.getService(ServiceLog.class).delayedStoreEnabled;
            } catch (ServiceException e) {
                DELAYED_STORE_ENABLED = false;
            }
        }
        return DELAYED_STORE_ENABLED != null && DELAYED_STORE_ENABLED;
    }

    // service methods < < <

    // public logger > > >

    public static void debug(Class<?> tag, String msg, Object... values) {
        log(DEBUG, tag.getSimpleName(), msg, values);
    }
    public static void debug(String tag, String msg, Object... values) {
        log(DEBUG, tag, msg, values);
    }

    public static void info(Class<?> tag, String msg, Object... values) {
        log(INFO, tag.getSimpleName(), msg, values);
    }
    public static void info(String tag, String msg, Object... values) {
        log(INFO, tag, msg, values);
    }

    public static void error(Class<?> tag, String msg, Object... values) {
        log(ERROR, tag.getSimpleName(), msg, values);
    }
    public static void error(String tag, String msg, Object... values) {
        log(ERROR, tag, msg, values);
    }

    public static void fatal(Class<?> tag, String msg, Object... values) {
        log(FATAL, tag.getSimpleName(), msg, values);
    }
    public static void fatal(String tag, String msg, Object... values) {
        log(FATAL, tag, msg, values);
    }

    private static void log(String level, String tag, String msg, Object... values) {
        log.info(tag + " : " + level + " --> " + msg, values);
        Log pLog = new Log(tag, level, msg, values);

        if (getDelayedStoreEnabled()) {
            Queue.add(VantarParam.QUEUE_NAME_USER_ACTION_LOG, new Packet(pLog));
        } else {
            try {
                Db.mongo.insert(pLog);
            } catch (Exception e) {
                log.error(" ! failed to store log {}", pLog);
                ServiceLog s = Services.get(ServiceLog.class);
                if (s != null) {
                    s.lastSuccess = false;
                    s.setLog(e.getMessage());
                }
            }
        }
    }

    // public logger < < <

    // user logger > > >

    public static void addAction(Dto.Action action, Object object) {
        addAction(action.name(), object);
    }

    public static void addAction(String action, Object object) {
        UserLog userLog = new UserLog();
        userLog.action = action;
        userLog.threadId = Thread.currentThread().getId() + Params.serverUpCount;
        Params params = Params.getThreadParams();
        if (params != null) {
            userLog.url = params.request.getRequestURI();
            CommonUser user = ServiceAuth.getCurrentSignedInUser(params);
            if (user != null) {
                userLog.userId = user.getId();
                userLog.userName = user.getUsername();
                userLog.extraData = user.getExtraData();
            }
        }
        if (object != null) {
            Class<?> cl = object.getClass();
            if (object instanceof Dto) {
                userLog.className = cl.getName();
                userLog.classNameSimple = cl.getSimpleName();
                userLog.objectX = ((Dto) object).getPropertyValues();
                userLog.objectId = ((Dto) object).getId();
            } else if (object instanceof DtoLogAction) {
                DtoLogAction logAction = (DtoLogAction) object;
                userLog.className = logAction.dtoClass.getName();
                userLog.classNameSimple = logAction.dtoClass.getSimpleName();
                userLog.object = Json.d.toJson(logAction.object);
                userLog.objectId = logAction.id;
            } else if (object instanceof String) {
                userLog.object = (String) object;
            } else {
                userLog.className = cl.getName();
                userLog.classNameSimple = cl.getSimpleName();
                userLog.object = Json.d.toJson(object);
            }
        }

        if (getDelayedStoreEnabled()) {
            Queue.add(VantarParam.QUEUE_NAME_USER_ACTION_LOG, new Packet(userLog));
        } else {
            try {
                userLog.time = new DateTime();
                userLog.timeDay = new DateTime().truncateTime().getAsTimestamp();
                Db.mongo.insert(userLog);
            } catch (Exception e) {
                log.error(" ! failed to store log {}", userLog);
                ServiceLog s = Services.get(ServiceLog.class);
                if (s != null) {
                    s.lastSuccess = false;
                    s.setLog(e.getMessage());
                }
            }
        }
    }

    public static void addRequest(Params params) {
        UserWebLog userLog = new UserWebLog();
        userLog.url = params.request.getRequestURI();
        userLog.action = "REQUEST";
        userLog.classNameSimple = "Params";
        userLog.threadId = Thread.currentThread().getId() + Params.serverUpCount;
        userLog.ip = params.getIp();
        userLog.requestType = params.getMethod() + ": " +  params.type.name();
        userLog.headers = params.getHeaders();
        userLog.uploadedFiles = params.getUploadFiles();
        userLog.params = params.toJsonString();
        userLog.paramsX = params.toMap();
        userLog.objectId = params.getLong("id");
        if (userLog.objectId == null) {
            try {
                userLog.objectId = params.extractFromJson("id", Long.class);
            } catch (Exception ignore) {

            }
        }
        CommonUser user = ServiceAuth.getCurrentSignedInUser(params);
        if (user != null) {
            userLog.userId = user.getId();
            userLog.userName = user.getUsername();
            userLog.extraData = user.getExtraData();
        }

        if (getDelayedStoreEnabled()) {
            Queue.add(VantarParam.QUEUE_NAME_USER_ACTION_LOG, new Packet(userLog));
        } else {
            try {
                userLog.time = new DateTime();
                userLog.timeDay = new DateTime().truncateTime().getAsTimestamp();
                Db.mongo.insert(userLog);
            } catch (Exception e) {
                log.error(" ! failed to store log {}", userLog);
                ServiceLog s = Services.get(ServiceLog.class);
                if (s != null) {
                    s.lastSuccess = false;
                    s.setLog(e.getMessage());
                }
            }
        }
    }

    public static void addResponse(Object object, HttpServletResponse response) {
        UserWebLog userLog = new UserWebLog();
        Params params = Params.getThreadParams();
        if (params != null) {
            userLog.url = params.request.getRequestURI();
            if (userLog.url.startsWith("/admin/")) {
                return;
            }
        }
        userLog.action = "RESPONSE";
        userLog.status = response.getStatus();
        userLog.threadId = Thread.currentThread().getId() + Params.serverUpCount;
        userLog.headers = Response.getHeaders(response);
        if (object != null) {
            if (object instanceof Dto) {
                userLog.objectId = ((Dto) object).getId();
                userLog.paramsX = ((Dto) object).getPropertyValues();
            } if (object instanceof String) {
                userLog.params = (String) object;
            } else {
                userLog.params = Json.d.toJson(object);
            }
            userLog.className = object.getClass().getName();
            userLog.classNameSimple = object.getClass().getSimpleName();
        }

        if (getDelayedStoreEnabled()) {
            Queue.add(VantarParam.QUEUE_NAME_USER_ACTION_LOG, new Packet(userLog));
        } else {
            try {
                userLog.time = new DateTime();
                userLog.timeDay = new DateTime().truncateTime().getAsTimestamp();
                Db.mongo.insert(userLog);
            } catch (Exception e) {
                log.error(" ! failed to store log {}", userLog);
                ServiceLog s = Services.get(ServiceLog.class);
                if (s != null) {
                    s.lastSuccess = false;
                    s.setLog(e.getMessage());
                }
            }
        }
    }

    // user logger < < <

    // db > > >

    private void scheduleLogStore() {
        if (pause) {
            return;
        }
        lastSuccess = true;
        try {
            if (!serviceUp || isBusy.get()) {
                Beat.set(this.getClass(), "timer-busy");
                return;
            }
            isBusy.set(true);
            Beat.set(this.getClass(), "timer");

            logTaskMongo();

        } catch (Exception e) {
            log.error(" ! failed to store logs", e);
            lastSuccess = false;
            setLog(e.getMessage());
        } finally {
            isBusy.set(false);
        }
    }

    private void logTaskMongo() throws VantarException {
        if (pause) {
            return;
        }
        List<Packet> packets = Queue.takeAllItems(VantarParam.QUEUE_NAME_USER_ACTION_LOG);
        if (packets.isEmpty()) {
            return;
        }

        DateTime time = new DateTime();
        Long timeDay = new DateTime().truncateTime().getAsTimestamp();

        List<UserWebLog> webItems = new ArrayList<>(packets.size());
        List<UserLog> userLogItems = new ArrayList<>(packets.size());
        List<Log> logItems = new ArrayList<>(packets.size());
        for (Packet packet : packets) {
            Object obj = packet.getObject();
            if (obj instanceof UserWebLog) {
                UserWebLog l = (UserWebLog) obj;
                l.time = time;
                l.timeDay = timeDay;
                webItems.add(l);
            } else if (obj instanceof UserLog) {
                UserLog l = (UserLog) obj;
                l.time = time;
                l.timeDay = timeDay;
                userLogItems.add(l);
            } else {
                Log l = (Log) obj;
                l.createT = time;
                logItems.add((Log) obj);
            }
        }

        if (!webItems.isEmpty()) {
            Db.mongo.insert(webItems);
        }
        if (!userLogItems.isEmpty()) {
            Db.mongo.insert(userLogItems);
        }
        if (!logItems.isEmpty()) {
            Db.mongo.insert(logItems);
            if (event != null) {
                for (Log l : logItems) {
                    try {
                        if (FATAL.equals(l.level)) {
                            event.alert(l);
                        }
                    } catch (Throwable t) {
                        log.error(" !! {}", l, t);
                        lastSuccess = false;
                        setLog(t.getMessage());
                    }
                }
            }
        }

        log.info(" -> logged {} items", packets.size());
        Beat.set(this.getClass(), "insert");
    }

    public static List<String> getStoredLogs(Class<?> tag) {
        return getStoredLogs(tag.getName());
    }

    public static List<String> getStoredLogs(String tag) {
        QueryBuilder q = new QueryBuilder(new Log())
            .sort("id:desc")
            .page(1, MAX_ERROR_FETCH);
        q.condition().equal("tag", tag);

        List<String> logs = new ArrayList<>(MAX_ERROR_FETCH + 1);
        try {
            Db.mongo.getData(q).forEach(dto -> {
                Log logItem = (Log) dto;
                logs.add(
                    logItem.createT.toString() + "  " + logItem.level + "\n"
                        + logItem.message + (logItem.objects == null ? "" : Json.d.toJsonPretty(logItem.objects))
                );
                return true;
            });
        } catch (Exception e) {
            log.error(" !", e);
        }
        return logs;
    }

    public static List<String> getLogTags() {
        try {
            QueryBuilder q = new QueryBuilder(new Log());
            q.addGroup("tag", DbMongo.ID);
            List<String> result = new ArrayList<>(30);
            for (Document document : Db.mongo.getAggregate(q)) {
                result.add((String) document.get(DbMongo.ID));
            }
            return result;
        } catch (VantarException e) {
            log.error(" !", e);
            return new ArrayList<>(1);
        }
    }

    // db < < <


    public static class DtoLogAction {

        public Class<? extends Dto> dtoClass;
        public Long id;
        public Object object;

        public DtoLogAction(Class<? extends Dto> dtoClass, Long id, Object object) {
            this.dtoClass = dtoClass;
            this.id = id;
            this.object = object;
        }
    }


    public interface Event {

        void alert(Log logItems);
    }
}