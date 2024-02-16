package com.vantar.service.log;

import com.vantar.util.datetime.DateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store service actions
 */
public abstract class Beat {

    private static final String DEFAULT_BEAT_TAG = "run";
    private static Map<Class<?>, Map<String, DateTime>> beats;


    public static void set(Class<?> service) {
        set(service, DEFAULT_BEAT_TAG);
    }

    public static void set(Class<?> service, String tag) {
        if (beats == null) {
            beats = new ConcurrentHashMap<>(16, 1);
        }
        synchronized (beats) {
            Map<String, DateTime> logs = beats.get(service);
            if (logs == null) {
                logs = new ConcurrentHashMap<>(5, 1);
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
}
