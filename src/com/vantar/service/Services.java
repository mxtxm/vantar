package com.vantar.service;

import com.vantar.exception.*;
import com.vantar.service.log.LogEvent;
import com.vantar.common.*;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.nosql.mongo.MongoConnection;
import com.vantar.database.sql.SqlConnection;
import com.vantar.queue.Queue;
import com.vantar.service.messaging.ServiceMessaging;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Services {

    private static final Logger log = LoggerFactory.getLogger(Services.class);

    public static final String ID = UUID.randomUUID().toString();
    public static Map<String, Integer> serviceCount;
    public static final Map<String, ServiceInfo> upServices = new ConcurrentHashMap<>(14);
    public static ServiceMessaging messaging;

    private static Event event;


    public static boolean isUp(Class<?> name) {
        return isUp(name.getSimpleName());
    }

    public static boolean isUp(String name) {
        ServiceInfo service = upServices.get(name);
        if (service ==  null) {
            return false;
        }
        return service.isRunningOnThisServer;
    }

    public static Service get(String serviceClass) throws ServiceException {
        ServiceInfo service = upServices.get(serviceClass);
        if (service == null) {
            log.error("! service '{}' not registered", serviceClass);
            throw new ServiceException(serviceClass);
        }
        return service.instance;
    }

    public static <T extends Service> T get(Class<T> serviceClass) throws ServiceException {
        ServiceInfo service = upServices.get(serviceClass.getSimpleName());
        if (service == null) {
            log.error("! service '{}' not registered", serviceClass.getSimpleName());
            throw new ServiceException(serviceClass);
        }
        return (T) service.instance;
    }

    public static void setEvents(Event e) {
        event = e;
    }

    /**
     * Do not run startup event
     */
    public static synchronized void startServicesOnly() {
        startServices(false);
    }

    public static synchronized void start() {
        startServices(true);
    }

    private static void startServices(boolean doEvents) {
        Set<Class<?>> dependencies = new HashSet<>();
        String values = Settings.getValue("service.dependencies");
        if (values != null) {
            for (String className : StringUtil.split(values, VantarParam.SEPARATOR_COMMON)) {
                try {
                    dependencies.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    log.error("! dependency('{}') not supported", className);
                }
            }
        }

        List<Service> orderedServices = new ArrayList<>();
        List<Service> notOrderedServices = new ArrayList<>();

        Set<String> usedKey = new HashSet<>();
        for (String key : Settings.getKeys()) {
            if (key.startsWith("service.enabled")) {
                Integer priority = Settings.getValue(key, Integer.class);
                if (priority == null) {
                    Boolean enabled = Settings.getValue(key, Boolean.class);
                    if (enabled == null || !enabled) {
                        continue;
                    }
                }

                key = StringUtil.replace(key, "service.enabled", "service");

                String packageName = StringUtil.trim(Settings.getValue(key + ".package"), '.');
                String className = StringUtil.toStudlyCase(key);
                Service service = ClassUtil.getInstance(packageName + '.' + className);
                if (service == null) {
                    log.error("! service('{}.{}') could not create instance", packageName, className);
                    continue;
                }

                key += '.';
                for (String keyB : Settings.getKeys()) {
                    if (usedKey.contains(keyB)) {
                        continue;
                    }

                    if (keyB.startsWith(key)) {
                        if (!StringUtil.contains(keyB, ".package")) {
                            String propertyName = StringUtil.toCamelCase(StringUtil.remove(keyB, key));
                            setFieldValue(service, propertyName, Settings.getValue(keyB));
                        }
                        usedKey.add(keyB);
                    }
                }

                if (priority == null) {
                    notOrderedServices.add(service);
                } else {
                    insertToList(orderedServices, priority, service);
                }

                if (service instanceof Dependency) {
                    dependencies.addAll(Arrays.asList(((Dependency) service).getDependencies()));
                }
            }
        }

        if (doEvents && event != null) {
            event.beforeStart(dependencies);
        }

        messaging = new ServiceMessaging();
        messaging.start();

        for (Service service : orderedServices) {
            if (service != null) {
                startService(service);
            }
        }
        for (Service service : notOrderedServices) {
            startService(service);
        }

        if (doEvents && event != null) {
            event.afterStart();
        }
    }

    private static void insertToList(List<Service> list, int i, Service service) {
        while (i-- > list.size()) {
            list.add(null);
        }
        list.add(service);
    }

    private static void setFieldValue(Object object, String name, String value) {
        try {
            Field field = object.getClass().getField(name);

            if (value == null) {
                field.set(object, null);
                return;
            }

            Class<?> type = field.getType();

            if (type.equals(String.class)) {
                field.set(object, value);
            } else if (type.equals(Integer.class)) {
                field.set(object, StringUtil.toInteger(value));
            } else if (type.equals(Long.class)) {
                field.set(object, StringUtil.toLong(value));
            } else if (type.equals(Double.class)) {
                field.set(object, StringUtil.toDouble(value));
            } else if (type.equals(Boolean.class)) {
                field.set(object, StringUtil.toBoolean(value));
            } else if (type.equals(Character.class)) {
                field.set(object, StringUtil.toCharacter(value));
            } else if (type.equals(DateTime.class)) {
                field.set(object, new DateTime(value));
            }
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException | DateTimeException e) {
            log.error("! set({}: {} < {})", object.getClass().getSimpleName(), name.trim(), value, e);
        }
    }

    private static void startService(Service service) {
        String className = service.getClass().getSimpleName();
        try {
            service.start();
            LogEvent.beat(service.getClass(), "start");
            setServiceStarted(className, null, service);
            messaging.broadcast(VantarParam.MESSAGE_SERVICE_STARTED, className);

            log.info(className + " started");
        } catch (Exception e) {
            log.error("! {} failed to start", className, e);
        }
    }

    /**
     * 1- Call after starting a service
     * 2- Call after getting a start service message from another server
     */
    public static synchronized void setServiceStarted(String tClass, String serverId, Service instance) {
        // is on this server
        if (serverId == null) {
            ServiceInfo info = new ServiceInfo();
            info.instanceCount = 1;
            info.isEnabledOnThisServer = true;
            info.isRunningOnThisServer = true;
            info.instance = instance;
            upServices.put(tClass, info);
            return;
        }

        if (ID.equals(serverId)) {
            return;
        }

        ServiceInfo info = upServices.get(tClass);
        if (tClass == null) {
            info = new ServiceInfo();
            info.instanceCount = 1;
            info.isEnabledOnThisServer = false;
            upServices.put(tClass, info);
            return;
        }
        ++info.instanceCount;
    }

    public static synchronized void stopServicesOnly() {
        stopServices(false);
    }

    public static synchronized void stop() {
        stopServices(true);
    }

    private static void stopServices(boolean doEvents) {
        if (doEvents && event != null) {
            event.beforeStop();
        }

        upServices.forEach((tClass, info) -> {
            if (info.instance != null) {
                stopService(info.instance);
            }
        });

        messaging.stop();

        if (doEvents && event != null) {
            event.afterStop();
        }
    }

    private static void stopService(Service service) {
        String className = service.getClass().getSimpleName();
        try {
            service.stop();
            LogEvent.beat(service.getClass(), "stop");

            setServiceStopped(className, null);
            messaging.broadcast(VantarParam.MESSAGE_SERVICE_STOPPED, className);

            log.info(className + " stopped gracefully");

        } catch (Exception e) {
            log.error(className + " failed to stop gracefully", e);
        }
    }

    /**
     * 1- Call after stopping a service
     * 2- Call after getting a stop service message from another server
     */
    public static synchronized void setServiceStopped(String tClass, String serverId) {
        ServiceInfo info = upServices.get(tClass);
        if (info == null) {
            return;
        }

        // on this server
        if (info.isEnabledOnThisServer && serverId == null) {
            info.isRunningOnThisServer = false;
            --info.instanceCount;
        }
        // on other service
        if (!info.isEnabledOnThisServer && serverId != null) {
            --info.instanceCount;
        }

        if (info.instance.onEndSetNull() && info.instanceCount <= 0) {
            upServices.remove(tClass);
            info.instance = null;
        }
    }

    public static synchronized Set<String> getEnabledServices() {
        Set<String> services = new HashSet<>(10);

        for (String key : Settings.getKeys()) {
            if (key.startsWith("service.enabled")) {
                Boolean enabled = Settings.getValue(key, Boolean.class);
                if (enabled == null || !enabled) {
                    continue;
                }
                services.add(StringUtil.toStudlyCase(key));
            }
        }

        return services;
    }

    /**
     * number of services running on all servers
     */
    public static synchronized int getTotalCount() {
        int count = 0;
        for (ServiceInfo serviceInfo : upServices.values()) {
            count += serviceInfo.instanceCount;
        }
        return count;
    }

    /**
     * number of services running on this server
     */
    public static synchronized int getCount() {
        int count = 0;
        for (ServiceInfo serviceInfo : upServices.values()) {
            if (serviceInfo.isEnabledOnThisServer) {
                count += serviceInfo.instanceCount;
            }
        }
        return count;
    }

    /**
     * number of services enabled on this server
     */
    public static synchronized int getEnabled() {
        return getEnabledServices().size();
    }

    public static synchronized void resetTotalServiceCount() {
        serviceCount = new ConcurrentHashMap<>();
        serviceCount.put(ID, getEnabled());
    }

    public static synchronized void setTotalServiceCount(int count, String serverId) {
        serviceCount.put(serverId, count);
    }

    /**
     * database and queues
     */
    public static void connectToDataSources(Set<Class<?>> dependencies) {
        if (dependencies.contains(Queue.class)) {
            Queue.connect(Settings.queue(), new LogEvent.QueueExceptionHandlerCustom());
        }
        if (dependencies.contains(MongoConnection.class)) {
            MongoConnection.connect(Settings.mongo());
        }
        if (dependencies.contains(SqlConnection.class)) {
            SqlConnection.start(Settings.sql());
        }
        if (dependencies.contains(ElasticConnection.class)) {
            ElasticConnection.connect(Settings.elastic());
        }
    }

    public interface Service {

        void start();
        void stop();
        boolean onEndSetNull();
    }


    public interface Dependency {

        Class<?>[] getDependencies();
    }


    public static class ServiceInfo {

        public boolean isEnabledOnThisServer;
        public boolean isRunningOnThisServer;
        public Service instance;
        public int instanceCount;

    }


    public interface Event {

        void beforeStart(Set<Class<?>> dependencies);
        void beforeStop();
        void afterStart();
        void afterStop();
    }
}