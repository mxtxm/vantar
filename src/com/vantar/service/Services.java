package com.vantar.service;

import com.vantar.business.ModelMongo;
import com.vantar.common.*;
import com.vantar.database.common.Db;
import com.vantar.database.nosql.mongo.DbMongo;
import com.vantar.exception.ServiceException;
import com.vantar.queue.common.Que;
import com.vantar.queue.rabbit.Rabbit;
import com.vantar.service.log.*;
import com.vantar.service.messaging.ServiceMessaging;
import com.vantar.service.patch.Patcher;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import java.util.*;


public class Services {

    public static final String ID = UUID.randomUUID().toString();

    public static ServiceMessaging messaging;

    private static Event event;
    private static Map<Class<?>, Service> upServicesMe;
    // <serverID, List<service>>
    private static Map<String, List<String>> upServicesOther;


    public static void setEvents(Event e) {
        event = e;
    }

    /**
     * Class simple names
     */
    public static List<String> getEnabledServices() {
        List<String> services = new ArrayList<>(14);
        for (String key : Settings.getKeys()) {
            if (key.startsWith("service.enabled")) {
                key = StringUtil.replace(key, "service.enabled", "service");
                services.add(StringUtil.toStudlyCase(key));
            }
        }
        return services;
    }

    public static List<Class<?>> getEnabledServiceClasses() {
        List<Class<?>> services = new ArrayList<>(14);
        for (String key : Settings.getKeys()) {
            if (key.startsWith("service.enabled")) {
                key = StringUtil.replace(key, "service.enabled", "service");
                String packageName = StringUtil.trim(Settings.getValue(key + ".package"), '.');
                String className = StringUtil.toStudlyCase(key);
                try {
                    services.add(Class.forName(packageName + "." + className));
                } catch (ClassNotFoundException e) {
                    ServiceLog.log.error(" ! invalid service {}", packageName + className);
                }
            }
        }
        return services;
    }

    public static void startServices() {
        startDataSources();

        Map<Integer, Service> orderedServices = new TreeMap<>();
        Set<String> setParams = new HashSet<>(100, 1);
        for (String key : Settings.getKeys()) {
            if (!key.startsWith("service.enabled")) {
                continue;
            }
            Integer priority = Settings.getValue(key, Integer.class);
            if (priority == null) {
                ServiceLog.log.error(" ! invalid priority > service({})", key);
                continue;
            }

            key = StringUtil.replace(key, "service.enabled", "service");
            String packageName = StringUtil.trim(Settings.getValue(key + ".package"), '.');
            String className = StringUtil.toStudlyCase(key);
            Service service = ClassUtil.getInstance(packageName + '.' + className);
            if (service == null) {
                ServiceLog.log.error(" ! failed to create service instance({}.{})", packageName, className);
                continue;
            }

            key += '.';
            for (String keyParam : Settings.getKeys()) {
                if (!keyParam.startsWith(key) || setParams.contains(keyParam) || keyParam.endsWith(".package")) {
                    continue;
                }
                String propertyName = StringUtil.toCamelCase(StringUtil.remove(keyParam, key));
                ObjectUtil.setPropertyValueIgnoreNull(service, propertyName, Settings.getValue(keyParam));
                setParams.add(keyParam);
            }

            orderedServices.put(priority, service);
        }

        if (event != null) {
            event.beforeStart();
        }

        messaging = new ServiceMessaging();
        messaging.start();

        upServicesMe = new LinkedHashMap<>(orderedServices.size(), 1);
        for (Service service : orderedServices.values()) {
            if (service != null) {
                startService(service);
            }
        }

        if (event != null) {
            event.afterStart();
        }
        Patcher.run();
    }

    private static void startService(Service service) {
        String className = service.getClass().getSimpleName();
        try {
            service.start();
            Beat.set(service.getClass(), "start");
            upServicesMe.put(service.getClass(), service);
            messaging.broadcast(VantarParam.MESSAGE_SERVICE_STARTED, className);
            ServiceLog.log.info(" > '{}' started", className);
        } catch (Exception e) {
            ServiceLog.log.error(" ! '{}' failed to start\n", className, e);
        }
    }

    /**
     * Get service start message from other servers
     */
    public synchronized static void onServiceStarted(String serverId, String service) {
        if (upServicesOther == null) {
            upServicesOther = new HashMap<>(10, 1);
        }
        List<String> services = upServicesOther.computeIfAbsent(serverId, k -> new ArrayList<>(10));
        services.add(service);
    }

    public static void stopServices() {
        if (event != null) {
            event.beforeStop();
        }

        upServicesMe.entrySet().removeIf(entry -> {
            stopService(entry.getValue());
            return true;
        });

        messaging.stop();
        ServiceLog.log.info(" > '{}' stopped gracefully", ServiceMessaging.class.getSimpleName());

        if (event != null) {
            event.afterStop();
        }

        stopDataSources();
    }

    private static void stopService(Service service) {
        String className = service.getClass().getSimpleName();
        try {
            service.stop();
            Beat.set(service.getClass(), "stop");
            messaging.broadcast(VantarParam.MESSAGE_SERVICE_STOPPED, className);
            ServiceLog.log.info(" > '{}' stopped gracefully", className);
        } catch (Exception e) {
            ServiceLog.log.error(" > '{}' failed to stopped gracefully\n", className, e);
        }
    }

    /**
     * Get service stop message from other servers
     */
    public static synchronized void onServiceStopped(String serverId, String service) {
        if (upServicesOther == null) {
            upServicesOther = new HashMap<>(10, 1);
        }
        List<String> services = upServicesOther.get(serverId);
        if (services == null) {
            upServicesOther.remove(serverId);
        } else {
            services.remove(service);
            if (services.isEmpty()) {
                upServicesOther.remove(serverId);
            }
        }
    }

    public static void pauseServices() {
        upServicesMe.forEach((key, value) -> value.pause());
    }

    public static void resumeServices() {
        upServicesMe.forEach((key, value) -> value.resume());
    }

    public static boolean isEnabled(DataSources source) {
        if (Que.Engine.RABBIT.equals(source)) {
            return Que.rabbit != null;
        }
        if (Db.Dbms.MONGO.equals(source)) {
            return Db.mongo != null;
        }
        if (Db.Dbms.SQL.equals(source)) {
            return Db.sql != null;
        }
        if (Db.Dbms.ELASTIC.equals(source)) {
            return Db.elastic != null;
        }
        return false;
    }

    public static boolean isUp(DataSources source) {
        if (Que.Engine.RABBIT.equals(source)) {
            return Que.rabbit != null && Que.rabbit.isUp();
        }
        if (Db.Dbms.MONGO.equals(source)) {
            return Db.mongo != null && Db.mongo.isUp();
        }
        if (Db.Dbms.SQL.equals(source)) {
            return Db.sql != null;
        }
        if (Db.Dbms.ELASTIC.equals(source)) {
            return Db.elastic != null;
        }
        return false;
    }

    public static boolean isUp(Class<?> serviceClass) {
        return upServicesMe != null && upServicesMe.containsKey(serviceClass);
    }

    public static boolean isPaused(Class<?> serviceClass) {
        if (upServicesMe == null) {
            return true;
        }
        Service s = upServicesMe.get(serviceClass);
        return s == null || s.isPaused();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Service> T getService(Class<T> serviceClass) throws ServiceException {
        Service service = upServicesMe.get(serviceClass);
        if (service == null) {
            throw new ServiceException(serviceClass);
        }
        return (T) service;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Service> T getService(String serviceClassName) throws ServiceException {
        try {
            Class<?> c = Class.forName(serviceClassName);
            return (T) upServicesMe.get(c);
        } catch (Exception e) {
            throw new ServiceException(serviceClassName);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Service> T get(Class<T> serviceClass) {
        Service service = upServicesMe.get(serviceClass);
        return service == null ? null : (T) service;
    }

    public static Collection<Service> getServices() {
        return upServicesMe == null ? null : upServicesMe.values();
    }

    public static Map<String, List<String>> getServicesOnOtherServers() {
        return upServicesOther;
    }

    /**
     * database and queues
     */
    public static void startDataSources() {
        String dataSources = Settings.getValue("service.data.sources");
        if (dataSources != null) {
            dataSources = dataSources.toLowerCase();
            if (dataSources.contains("rabbit")) {
                Que.rabbit = new Rabbit(Settings.rabbit());
            }
            if (dataSources.contains("mongo")) {
                Db.mongo = new DbMongo(Settings.mongo());
                Db.modelMongo = new ModelMongo(Db.mongo);
            }
            if (dataSources.contains("sql")) {

            }
            if (dataSources.contains("elastic")) {

            }
        }
    }

    public static void stopDataSources() {
        if (Que.rabbit != null) {
            Que.rabbit.shutdown();
        }
        if (Db.mongo != null) {
            Db.mongo.shutdown();
        }
        if (Db.sql != null) {

        }
        if (Db.elastic != null) {

        }
    }


    public interface Service {

        void start();
        void stop();
        void pause();
        void resume();
        boolean isUp();
        boolean isOk();
        boolean isPaused();
        List<String> getLogs();
    }


    public interface Event {

        void beforeStart();
        void beforeStop();
        void afterStart();
        void afterStop();
    }

    public interface DataSources {

    }
}