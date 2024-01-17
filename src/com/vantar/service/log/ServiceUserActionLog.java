package com.vantar.service.log;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticWrite;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.exception.*;
import com.vantar.queue.Queue;
import com.vantar.queue.*;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.service.log.dto.*;
import com.vantar.util.json.*;
import com.vantar.web.*;
import org.slf4j.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class ServiceUserActionLog implements Services.Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceUserActionLog.class);
    private ScheduledExecutorService schedule;
    private volatile boolean serviceOn = false;
    private final AtomicBoolean isBusy = new AtomicBoolean();
    private boolean pause = false;

    // > > > service params injected from config
    public Boolean onEndSetNull;
    public String dbms;
    public Boolean delayedStoreEnabled;
    public Integer insertIntervalMin;
    // < < <

    public void start() {
        serviceOn = true;
        isBusy.set(false);
        if (!delayedStoreEnabled) {
            return;
        }
        schedule = Executors.newSingleThreadScheduledExecutor();
        schedule.scheduleWithFixedDelay(this::scheduledTask, insertIntervalMin, insertIntervalMin, TimeUnit.MINUTES);
    }

    public void stop() {
        serviceOn = false;
        isBusy.set(false);
        if (!delayedStoreEnabled) {
            return;
        }
        schedule.shutdown();
    }

    public synchronized void pause() {
        pause = true;
    }

    public synchronized void resume() {
        pause = false;
    }

    public boolean onEndSetNull() {
        return onEndSetNull;
    }

    public static void add(Dto.Action action, Object object) {
        add(action.name(), object);
    }

    public static void add(String action, Object object) {
        ServiceUserActionLog userLogService;
        try {
            userLogService = Services.get(ServiceUserActionLog.class);
        } catch (ServiceException e) {
            return;
        }

        UserLog userLog = new UserLog();
        Params params = Params.getThreadParams();
        if (params != null) {
            userLog.url = params.request.getRequestURI();
            if (userLog.url.startsWith("/admin/")) {
                return;
            }
            userLog.headers = params.getHeaders();
            userLog.requestType = params.getMethod() + ": " +  params.type.name();
            userLog.ip = params.getIp();
            userLog.uploadedFiles = params.getUploadFiles();
            try {
                CommonUser user = Services.get(ServiceAuth.class).getCurrentUser(params);
                if (user != null) {
                    userLog.userId = user.getId();
                    userLog.extraData = user.getExtraData();
                }
            } catch (Exception e) {
                userLog.userId = null;
            }
        }
        userLog.action = action;
        userLog.threadId = Thread.currentThread().getId();

        if (object != null) {
            Class<?> cl = object.getClass();
            if (object instanceof Dto) {
                userLog.className = cl.getName();
                userLog.classNameSimple = cl.getSimpleName();
                userLog.object = Json.d.toJson(object);
                userLog.objectId = ((Dto) object).getId();
            } else if (object instanceof String) {
                userLog.object = (String) object;
            } else if (object instanceof DtoLogAction) {
                DtoLogAction logAction = (DtoLogAction) object;
                userLog.className = logAction.dtoClass.getName();
                userLog.classNameSimple = logAction.dtoClass.getSimpleName();
                userLog.object = Json.d.toJson(logAction.object);
                userLog.objectId = logAction.id;
            } else {
                userLog.className = cl.getName();
                userLog.classNameSimple = cl.getSimpleName();
                userLog.object = Json.d.toJson(object);
            }
        }

        if (userLogService.delayedStoreEnabled && Services.isUp(Queue.class)) {
            Queue.add(VantarParam.QUEUE_NAME_USER_ACTION_LOG, new Packet(userLog));
            log.debug(" userLog > queue({})", VantarParam.QUEUE_NAME_USER_ACTION_LOG);
        } else {
            if (userLogService.dbms.equalsIgnoreCase(DtoDictionary.Dbms.ELASTIC.name())) {
                saveOnElastic(userLog);
            } else if (userLogService.dbms.equalsIgnoreCase(DtoDictionary.Dbms.MONGO.name())) {
                saveOnMongo(userLog);
            }
        }
    }

    public static void addRequest(Params params) {
        ServiceUserActionLog userLogService;
        try {
            userLogService = Services.get(ServiceUserActionLog.class);
        } catch (ServiceException e) {
            return;
        }

        UserWebLog userLog = new UserWebLog();
        userLog.url = params.request.getRequestURI();
        if (userLog.url.startsWith("/admin/")) {
            return;
        }
        userLog.headers = params.getHeaders();
        userLog.requestType = params.getMethod() + ": " +  params.type.name();
        userLog.ip = params.getIp();
        userLog.uploadedFiles = params.getUploadFiles();
        try {
            CommonUser user = Services.get(ServiceAuth.class).getCurrentUser(params);
            if (user != null) {
                userLog.userId = user.getId();
                userLog.extraData = user.getExtraData();
            }
        } catch (Exception e) {
            userLog.userId = null;
        }

        userLog.action = "REQUEST";
        userLog.threadId = Thread.currentThread().getId();
        userLog.className = "Params";
        userLog.classNameSimple = Params.class.getSimpleName();
        userLog.params = params.toJsonString();
        userLog.objectId = params.getLong("id");
        if (userLog.objectId == null) {
            try {
                userLog.objectId = params.extractFromJson("id", Long.class);
            } catch (Exception ignore) {

            }
        }

        if (userLogService.delayedStoreEnabled && Services.isUp(Queue.class)) {
            Queue.add(VantarParam.QUEUE_NAME_USER_ACTION_LOG, new Packet(userLog));
        } else {
            if (userLogService.dbms.equalsIgnoreCase(DtoDictionary.Dbms.ELASTIC.name())) {
                saveOnElastic(userLog);
            } else if (userLogService.dbms.equalsIgnoreCase(DtoDictionary.Dbms.MONGO.name())) {
                saveOnMongo(userLog);
            }
        }
    }

    public static void addResponse(Object object, HttpServletResponse response) {
        ServiceUserActionLog userLogService;
        try {
            userLogService = Services.get(ServiceUserActionLog.class);
        } catch (ServiceException e) {
            return;
        }

        UserWebLog userLog = new UserWebLog();
        Params params = Params.getThreadParams();
        if (params != null) {
            userLog.url = params.request.getRequestURI();
            if (userLog.url.startsWith("/admin/")) {
                return;
            }
            log.debug(" ! {} <", userLog.url);
        }
        userLog.action = "RESPONSE";
        userLog.status = response.getStatus();
        userLog.threadId = Thread.currentThread().getId();
        userLog.headers = Response.getHeaders(response);

        if (object != null) {
            if (object instanceof Dto) {
                userLog.objectId = ((Dto) object).getId();
            } if (object instanceof String) {
                userLog.params = (String) object;
            } else {
                userLog.params = Json.d.toJson(object);
            }
            userLog.className = object.getClass().getName();
            userLog.classNameSimple = object.getClass().getSimpleName();
        }

        if (userLogService.delayedStoreEnabled && Services.isUp(Queue.class)) {
            Queue.add(VantarParam.QUEUE_NAME_USER_ACTION_LOG, new Packet(userLog));
        } else {
            if (userLogService.dbms.equalsIgnoreCase(DtoDictionary.Dbms.ELASTIC.name())) {
                saveOnElastic(userLog);
            } else if (userLogService.dbms.equalsIgnoreCase(DtoDictionary.Dbms.MONGO.name())) {
                saveOnMongo(userLog);
            }
        }
    }

    private void scheduledTask() {
        if (pause) {
            return;
        }
        try {
            if (!serviceOn || isBusy.get()) {
                LogEvent.beat(this.getClass(), "timer-busy");
                return;
            }
            isBusy.set(true);
            LogEvent.beat(this.getClass(), "timer");

            if (dbms.equalsIgnoreCase(DtoDictionary.Dbms.ELASTIC.name())) {
                logTaskElastic();
            } else if (dbms.equalsIgnoreCase(DtoDictionary.Dbms.MONGO.name())) {
                logTaskMongo();
            }

        } catch (Exception e) {
            log.error("! could not save log ", e);
        } finally {
            isBusy.set(false);
        }
    }

    private void logTaskElastic() {
        List<Packet> packets = Queue.takeAllItems(VantarParam.QUEUE_NAME_USER_ACTION_LOG);
        if (packets.isEmpty()) {
            return;
        }

        ElasticWrite write = new ElasticWrite();
        write.startTransaction();

        for (Packet packet : packets) {
            UserLog item = packet.getObject();
            try {
                write.insert(item);
            } catch (DatabaseException e) {
                log.error("! could not save log ", e);
            }
        }

        try {
            write.commit();
        } catch (DatabaseException e) {
            log.error("! could not save log ", e);
            return;
        }

        log.info(" >> logged {} items", packets.size());
        LogEvent.beat(this.getClass(), "insert");
    }

    private void logTaskMongo() {
        List<Packet> packets = Queue.takeAllItems(VantarParam.QUEUE_NAME_USER_ACTION_LOG);
        if (packets.isEmpty()) {
            return;
        }

        List<UserLog> items = new ArrayList<>(packets.size());
        List<UserWebLog> wItems = new ArrayList<>(packets.size());

        for (Packet packet : packets) {
            Object obj = packet.getObject();
            if (obj instanceof UserWebLog) {
                wItems.add((UserWebLog) obj);
            } else {
                items.add((UserLog) obj);
            }
        }

        try {
            Mongo.insert(items);
            Mongo.insert(wItems);
        } catch (DatabaseException e) {
            log.info("! could not save log {}", e.getMessage());
            return;
        }

        log.info(" >> logged {} items", packets.size());
        LogEvent.beat(this.getClass(), "insert");
    }

    private static void saveOnElastic(Dto item) {
        ElasticWrite write = new ElasticWrite();
        write.startTransaction();
        try {
            write.insert(item);
            write.commit();
        } catch (DatabaseException e) {
            log.error("! could not save log {}", item);
        }
    }

    private static void saveOnMongo(Dto item) {
        try {
            Mongo.insert(item);
        } catch (Exception e) {
            log.error("! could not save log {}", item);
        }
    }

    public boolean isOk() {
        return true;
    }


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
}