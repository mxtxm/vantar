package com.vantar.service.log;

import com.vantar.business.CommonRepoMongo;
import com.vantar.database.dto.Dto;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.DatabaseException;
import com.vantar.service.log.dto.Log;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.ObjectUtil;
import org.bson.Document;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class LogEvent {

    public static final Logger log = LoggerFactory.getLogger(LogEvent.class);
    private static final Map<Class<?>, Map<String, DateTime>> beats = new ConcurrentHashMap<>(14);
    private static final int MAX_ERROR_FETCH = 100;
    private static final String DEFAULT_BEAT_TAG = "run";
    private static final String DEBUG = "DEBUG";
    private static final String INFO = "INFO";
    private static final String ERROR = "ERROR";
    private static final String FATAL = "FATAL";


    // > > > BEAT

    public static void beat(Class<?> service) {
        beat(service, DEFAULT_BEAT_TAG);
    }

    public static void beat(Class<?> service, String tag) {
        synchronized (beats) {
            Map<String, DateTime> logs = beats.get(service);
            if (logs == null) {
                logs = new ConcurrentHashMap<>();
                beats.put(service, logs);
            }
            logs.put(tag, new DateTime());
        }
    }

    public static Map<Class<?>, Map<String, DateTime>> getBeats() {
        return beats;
    }

    public static Map<String, DateTime> getBeats(Class<?> service) {
        return beats.get(service);
    }

    public static DateTime getBeat(Class<?> service, String tag) {
        return beats.get(service).get(tag);
    }

    // BEAT < < <


    public static void debug(Class<?> tag, Object... values) {
        log(DEBUG, tag.getSimpleName(), values);
    }

    public static void info(Class<?> tag, Object... values) {
        log(INFO, tag.getSimpleName(), values);
    }

    public static void error(Class<?> tag, Object... values) {
        log(ERROR, tag.getSimpleName(), values);
    }

    public static void fatal(Class<?> tag, Object... values) {
        log(FATAL, tag.getSimpleName(), values);
    }

    public static void debug(String tag, Object... values) {
        log(DEBUG, tag, values);
    }

    public static void info(String tag, Object... values) {
        log(INFO, tag, values);
    }

    public static void error(String tag, Object... values) {
        log(ERROR, tag, values);
    }

    public static void fatal(String tag, Object... values) {
        log(FATAL, tag, values);
    }

    private static void log(String level, String tag, Object... values) {
        try {
            StringBuilder message = new StringBuilder("! " + tag + ":\n");
            for (Object value : values) {
                if (value == null) {
                    message.append("null\n");
                } else if (value.getClass().isArray()) {
                    message.append("array\n");
                } else if (value instanceof Throwable) {
                    message.append(ObjectUtil.throwableToString((Throwable) value));
                } else {
                    message.append(value).append('\n');
                }
            }
            String m = message.toString();
            if (level.equals(ERROR) || level.equals(FATAL)) {
                log.error(m);
            } else if (level.equals(INFO)) {
                log.info(m);
            } else {
                log.debug(m);
            }
            CommonRepoMongo.insert(new Log(tag, level, m));
        } catch (Exception e) {
            log.error(" !! {} {}\n", tag, values, e);
        }
    }

    public static List<String> get(Class<?> tag) {
        return get(tag.getName());
    }

    public static List<String> get(String tag) {
        Log log = new Log();
        QueryBuilder q = new QueryBuilder(log)
            .sort("id:desc")
            .page(1, MAX_ERROR_FETCH);
        q.condition().equal("tag", tag);

        List<String> logs = new ArrayList<>();
        try {
            for (Dto item : MongoSearch.getData(q).asList()) {
                Log logItem = (Log) item;
                logs.add(logItem.createT.toString() + "  " + logItem.level + "\n" + logItem.message);
            }
        } catch (Exception e) {
            // pass
        }
        return logs;
    }

    public static List<String> getErrorTags() {
        try {
            QueryBuilder q = new QueryBuilder(new Log());
            q.addGroup("tag", Mongo.ID);

            List<String> result = new ArrayList<>(30);
            for (Document document : MongoSearch.getAggregate(q)) {
                result.add((String) document.get(Mongo.ID));
            }

            return result;
        } catch (DatabaseException e) {
            log.error(" !! getErrorTags", e);
            return new ArrayList<>();
        }
    }
}