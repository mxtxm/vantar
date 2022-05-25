package com.vantar.util.json;

import java.io.IOException;
import java.util.*;


public class Json {

    /**
     * To change default JSON configs set this first in Application
     */
    public static Config defaultConfig = new Config();
    public static final Jackson d = new Jackson(defaultConfig);

    private static Map<String, Jackson> tagged;


    public static Jackson put(String tag, Config config) {
        if (tagged == null) {
            tagged = new HashMap<>(5);
        }
        Jackson jackson = new Jackson(config);
        tagged.put(tag, jackson);
        return jackson;
    }

    public static Jackson get(String tag) {
        return tagged == null ? null : tagged.get(tag);
    }

    public static Jackson getNewDefault() {
        return new Jackson(defaultConfig);
    }

    // > > > PRESET


    public static Jackson getWithPrivate() {
        Jackson json = tagged == null ? null : tagged.get("private");
        if (json != null) {
            return json;
        }
        Config c = new Config();
        c.propertyPrivate = true;
        return put("private", c);
    }

    public static Jackson getWithProtected() {
        Jackson json = tagged == null ? null : tagged.get("protected");
        if (json != null) {
            return json;
        }
        Config c = new Config();
        c.propertyProtected = true;
        return put("protected", c);
    }

    public static Jackson getWithNulls() {
        Jackson json = tagged == null ? null : tagged.get("null");
        if (json != null) {
            return json;
        }
        Config c = new Config();
        c.skipNulls = false;
        return put("null", c);
    }

    public static boolean isJsonShallow(Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        String s = ((String) value).trim();
        return (s.startsWith("{") && s.endsWith("}")) || (s.startsWith("[") && s.endsWith("]"));
    }

    public static boolean isJson(Object value) {
        return d.isJson(value);
    }


    // PRESET < < <


    public static class Config {
        public boolean skipNulls = true;
        public boolean propertyPrivate = false;
        public boolean propertyProtected = false;
        public boolean getter = false;
        public boolean setter = false;
    }
}
