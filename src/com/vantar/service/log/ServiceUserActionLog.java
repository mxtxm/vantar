package com.vantar.service.log;

import com.vantar.business.CommonRepoMongo;
import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticWrite;
import com.vantar.exception.*;
import com.vantar.queue.Queue;
import com.vantar.queue.*;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.service.log.dto.UserLog;
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
                userLog.userId = Services.get(ServiceAuth.class).getCurrentUser(params).getId();
            } catch (ServiceException | AuthException e) {
                userLog.userId = null;
            }
        }
        userLog.action = action;
        userLog.threadId = Thread.currentThread().getId();

        if (object == null && params != null) {
            userLog.className = "Request";
            userLog.object = params.toJsonString();
        } else if (object != null) {
            if (object instanceof String) {
                userLog.object = Json.d.toJson(object);
            } else {
                userLog.className = object.getClass().getName();
                userLog.object = Json.d.toJson(object);
            }
            if (object instanceof Dto) {
                userLog.objectId = ((Dto) object).getId();
            }
        }

        if (userLogService.delayedStoreEnabled && Queue.isUp()) {
            Queue.add(VantarParam.QUEUE_NAME_USER_ACTION_LOG, new Packet(userLog));
            log.debug(" ! userLog > queue({})", VantarParam.QUEUE_NAME_USER_ACTION_LOG);
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

        UserLog userLog = new UserLog();
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
            if (object instanceof String) {
                userLog.object = Json.d.toJson(object);
            } else {
                userLog.className = object.getClass().getName();
                userLog.object = Json.d.toJson(object);
            }
        }

        if (userLogService.delayedStoreEnabled && Queue.isUp()) {
            Queue.add(VantarParam.QUEUE_NAME_USER_ACTION_LOG, new Packet(userLog));
            log.debug(" ! userLog > queue({})", VantarParam.QUEUE_NAME_USER_ACTION_LOG);
        } else {
            if (userLogService.dbms.equalsIgnoreCase(DtoDictionary.Dbms.ELASTIC.name())) {
                saveOnElastic(userLog);
            } else if (userLogService.dbms.equalsIgnoreCase(DtoDictionary.Dbms.MONGO.name())) {
                saveOnMongo(userLog);
            }
        }
    }

    private void scheduledTask() {
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
            LogEvent.error(this.getClass(), e);
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
            UserLog item = packet.getObject(UserLog.class);
            try {
                write.insert(item);
            } catch (DatabaseException e) {
                LogEvent.error(this.getClass(), e);
            }
        }

        try {
            write.commit();
        } catch (DatabaseException e) {
            LogEvent.fatal(this.getClass(), e);
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

        for (Packet packet : packets) {
            items.add(packet.getObject(UserLog.class));
        }

        try {
            CommonRepoMongo.insert(items);
        } catch (DatabaseException e) {
            LogEvent.fatal(this.getClass(), e);
            return;
        }

        log.info(" >> logged {} items", packets.size());
        LogEvent.beat(this.getClass(), "insert");
    }

    private static void saveOnElastic(UserLog item) {
        ElasticWrite write = new ElasticWrite();
        write.startTransaction();
        try {
            write.insert(item);
            write.commit();
        } catch (DatabaseException e) {
            LogEvent.fatal(ServiceUserActionLog.class, e);
        }
    }

    private static void saveOnMongo(UserLog item) {
        try {
            CommonRepoMongo.insert(item);
        } catch (DatabaseException e) {
            LogEvent.fatal(ServiceUserActionLog.class, e);
        }
    }
}