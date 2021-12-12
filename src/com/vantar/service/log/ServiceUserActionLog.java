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
import com.vantar.util.json.Json;
import com.vantar.web.Params;
import org.slf4j.*;
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
    public Boolean storeEnabled;
    public String dbms;
    public Integer insertIntervalMin;
    // < < <

    public void start() {
        serviceOn = true;
        isBusy.set(false);
        if (!storeEnabled) {
            return;
        }
        schedule = Executors.newSingleThreadScheduledExecutor();
        schedule.scheduleWithFixedDelay(this::scheduledTask, insertIntervalMin, insertIntervalMin, TimeUnit.MINUTES);
    }

    public void stop() {
        serviceOn = false;
        isBusy.set(false);
        if (!storeEnabled) {
            return;
        }
        schedule.shutdown();
    }

    public boolean onEndSetNull() {
        return onEndSetNull;
    }

    public static void add(String action, Object object) {
        add(null, action, object, null);
    }

    public static void add(Params params, String action, Object object) {
        add(params, action, object, null);
    }

    public static void add(Dto.Action action, Object object) {
        add(null, action.name(), object, null);
    }

    public static void add(Params params, Dto.Action action, Object object) {
        add(params, action.name(), object, null);
    }

    public static void add(Params params, Dto.Action action, Object object, String description) {
        add(params, action.name(), object, description);
    }

    public static void add(Params params, String action, Object object, String description) {
        if (!Services.isUp(ServiceUserActionLog.class)) {
            return;
        }

        UserLog userLog = new UserLog();

        if (params != null) {
            try {
                userLog.userId = Services.get(ServiceAuth.class).getCurrentUser(params).getId();
            } catch (ServiceException | AuthException e) {
                userLog.userId = null;
            }
        }

        userLog.action = action;
        if (object != null) {
            if (object instanceof String) {
                userLog.className = (String) object;
            } else {
                userLog.className = object.getClass().getName();
                userLog.object = Json.toJson(object);
            }
            if (object instanceof Dto) {
                userLog.objectId = ((Dto) object).getId();
            }
        }
        if (params != null) {
            userLog.headers = params.getHeaders();
            userLog.url = params.getCurrentUrl();
            userLog.ip = params.getIp();
        }
        userLog.description = description;

        if (Queue.isUp) {
            Queue.add(VantarParam.QUEUE_NAME_USER_ACTION_LOG, new Packet(userLog));
            log.debug("userLog > queue({})", VantarParam.QUEUE_NAME_USER_ACTION_LOG);
        } else {

            ServiceUserActionLog serviceUserActionLog;
            try {
                serviceUserActionLog = Services.get(ServiceUserActionLog.class);
            } catch (ServiceException e) {
                log.error("! action not logged", e);
                return;
            }

            if (serviceUserActionLog.dbms.equalsIgnoreCase(DtoDictionary.Dbms.ELASTIC.name())) {
                saveOnElastic(userLog);
            } else if (serviceUserActionLog.dbms.equalsIgnoreCase(DtoDictionary.Dbms.MONGO.name())) {
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
            UserLog item = packet.getObject();
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

        log.info("> logged {} items ", packets.size());
        LogEvent.beat(this.getClass(), "insert");
    }

    private void logTaskMongo() {
        List<Packet> packets = Queue.takeAllItems(VantarParam.QUEUE_NAME_USER_ACTION_LOG);
        if (packets.isEmpty()) {
            return;
        }

        List<UserLog> items = new ArrayList<>(packets.size());

        for (Packet packet : packets) {
            items.add(packet.getObject());
        }

        try {
            CommonRepoMongo.insert(items);
        } catch (DatabaseException e) {
            LogEvent.fatal(this.getClass(), e);
            return;
        }

        log.info("> logged {} items ", packets.size());
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