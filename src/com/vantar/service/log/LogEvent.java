package com.vantar.service.log;

import com.rabbitmq.client.*;
import com.vantar.business.CommonRepoMongo;
import com.vantar.database.dto.Dto;
import com.vantar.database.nosql.mongo.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.DatabaseException;
import com.vantar.queue.QueueExceptionHandler;
import com.vantar.service.log.dto.Log;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.ObjectUtil;
import org.bson.Document;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class LogEvent {

    private static final Logger log = LoggerFactory.getLogger(LogEvent.class);
    private static final int MAX_ERROR_FETCH = 100;
    private static final String DEBUG = "DEBUG";
    private static final String INFO = "INFO";
    private static final String ERROR = "ERROR";
    private static final String FATAL = "FATAL";
    private static final String DEFAULT_BEAT_TAG = "run";
    private static final Map<String, Map<String, DateTime>> beats = new ConcurrentHashMap<>();


    public static void beat(Class<?> service) {
        beat(service, DEFAULT_BEAT_TAG);
    }

    public static void beat(Class<?> service, String tag) {
        String key = service.getSimpleName();
        synchronized (beats) {
            Map<String, DateTime> logs = beats.get(key);
            if (logs == null) {
                logs = new ConcurrentHashMap<>();
            }
            logs.put(tag, new DateTime());
            beats.put(key, logs);
        }
    }

    public static Map<String, Map<String, DateTime>> getBeats() {
        return beats;
    }

    public static Map<String, DateTime> getBeats(Class<?> service) {
        return beats.get(service.getSimpleName());
    }

    public static DateTime getBeat(Class<?> service, String tag) {
        return beats.get(service.getSimpleName()).get(tag);
    }

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
        //todo fix log. message
        try {
            StringBuilder storeMessage = new StringBuilder();
            StringBuilder logMessage = new StringBuilder(">>> " + tag + "\n");
            for (Object value : values) {
                String message;
                if (value == null) {
                    message = "null";
                } else {
                    if (value.getClass().isArray()) {
                        message = "array";
                    } else if (value instanceof Throwable) {
                        message = ObjectUtil.throwableToString((Throwable) value);
                    } else {
                        message = value.toString();
                        logMessage.append("{}\n");
                    }
                }
                storeMessage.append(message).append("\n");
            }

            log.error(logMessage.toString(), values);
            CommonRepoMongo.insert(new Log(tag, level, storeMessage.toString()));
        } catch (Exception e) {
            log.error("! {} {}", tag, values, e);
        }
    }

    public static List<String> get(Class<?> tag) {
        return get(tag.getName());
    }

    public static List<String> get(String tag) {
        Log log = new Log();
        log.tag = tag;
        QueryBuilder q = new QueryBuilder(log)
            .sort("id:desc")
            .page(1, MAX_ERROR_FETCH);
        q.setConditionFromDtoEqualTextMatch();

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
            log.error("! ! ! error tags", e);
            return new ArrayList<>();
        }
    }


    public static class QueueExceptionHandlerCustom implements QueueExceptionHandler {

        public void handleReturnListenerException(Channel channel, Throwable e) {
            fatal(com.vantar.queue.QueueExceptionHandlerBase.class, "handleReturnListenerException", channel, e);
        }

        public void handleChannelRecoveryException(Channel channel, Throwable e) {
            fatal(com.vantar.queue.QueueExceptionHandlerBase.class, "handleChannelRecoveryException", channel, e);
        }

        public void handleConfirmListenerException(Channel channel, Throwable e) {
            fatal(com.vantar.queue.QueueExceptionHandlerBase.class, "handleConfirmListenerException", channel, e);
        }

        public void handleConnectionRecoveryException(Connection conn, Throwable e) {
            fatal(com.vantar.queue.QueueExceptionHandlerBase.class, "handleConnectionRecoveryException", conn, e);
        }

        public void handleConsumerException(Channel channel, Throwable e, Consumer consumer, String consumerTag, String methodName) {
            fatal(com.vantar.queue.QueueExceptionHandlerBase.class, "handleConsumerException", consumerTag, methodName, channel, e);
        }

        public void handleTopologyRecoveryException(Connection conn, Channel channel, TopologyRecoveryException e) {
            fatal(com.vantar.queue.QueueExceptionHandlerBase.class, "handleTopologyRecoveryException", conn, channel, e);
        }

        public void handleUnexpectedConnectionDriverException(Connection conn, Throwable e) {
            fatal(com.vantar.queue.QueueExceptionHandlerBase.class, "handleUnexpectedConnectionDriverException", conn, e);
        }
    }
}