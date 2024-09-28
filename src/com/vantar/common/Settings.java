package com.vantar.common;

import com.vantar.database.nosql.elasticsearch.ElasticConfig;
import com.vantar.database.nosql.mongo.MongoConfig;
import com.vantar.database.sql.SqlConfig;
import com.vantar.exception.DateTimeException;
import com.vantar.locale.LocaleConfig;
import com.vantar.queue.rabbit.RabbitConfig;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.string.StringUtil;
import com.vantar.web.WebConfig;
import java.util.*;


public class Settings {

    // stable
    public static Common config;
    // tunable
    public static Common tune;
    public static Class<?> configClass;
    public static Class<?> tuneClass;


    public static void setConfig(Class<?> cClass, Common c) {
        configClass = cClass;
        config = c;
    }

    public static void setTune(Class<?> tClass, Common t) {
        tuneClass = tClass;
        tune = t;
    }

    public static Set<String> getKeys() {
        Set<String> k1 = config.propertyNames();
        Set<String> k2 = tune.propertyNames();
        Set<String> keys = new HashSet<>(20, 1);
        if (k1 != null) {
            keys.addAll(k1);
        }
        if (k2 != null) {
            keys.addAll(k2);
        }
        return keys;
    }

    public static String getValue(String key) {
        String v = config.getProperty(key);
        return v == null ? tune.getProperty(key) : v;
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T getValue(String key, Class<T> tClass) {
        String v = getValue(key);
        if (v == null) {
            return null;
        }
        if (String.class.equals(tClass)) {
            return (T) v.trim();
        }
        if (Integer.class.equals(tClass)) {
            return (T) StringUtil.toInteger(v);
        }
        if (Long.class.equals(tClass)) {
            return (T) StringUtil.toLong(v);
        }
        if (Double.class.equals(tClass)) {
            return (T) StringUtil.toDouble(v);
        }
        if (Boolean.class.equals(tClass)) {
            return (T) StringUtil.toBoolean(v);
        }
        if (Character.class.equals(tClass)) {
            return (T) StringUtil.toCharacter(v);
        }
        if (DateTime.class.equals(tClass)) {
            try {
                return (T) new DateTime(v);
            } catch (DateTimeException ignore) {

            }
        }
        return null;
    }

    public static MongoConfig mongo() {
        if (config instanceof MongoConfig) {
            return (MongoConfig) config;
        }
        if (tune instanceof MongoConfig) {
            return (MongoConfig) tune;
        }
        return null;
    }

    public static SqlConfig sql() {
        if (config instanceof SqlConfig) {
            return (SqlConfig) config;
        }
        if (tune instanceof SqlConfig) {
            return (SqlConfig) tune;
        }
        return null;
    }

    public static ElasticConfig elastic() {
        if (config instanceof ElasticConfig) {
            return (ElasticConfig) config;
        }
        if (tune instanceof ElasticConfig) {
            return (ElasticConfig) tune;
        }
        return null;
    }

    public static RabbitConfig rabbit() {
        if (config instanceof RabbitConfig) {
            return (RabbitConfig) config;
        }
        if (tune instanceof RabbitConfig) {
            return (RabbitConfig) tune;
        }
        return null;
    }

    public static LocaleConfig locale() {
        if (config instanceof LocaleConfig) {
            return (LocaleConfig) config;
        }
        if (tune instanceof LocaleConfig) {
            return (LocaleConfig) tune;
        }
        return null;
    }

    public static WebConfig web() {
        if (tune instanceof WebConfig) {
            return (WebConfig) tune;
        }
        if (config instanceof WebConfig) {
            return (WebConfig) config;
        }
        return null;
    }

    public static boolean isLocal() {
        return ((CommonConfig) config).isLocal();
    }

    public static String getAppPackage() {
        return ((CommonConfig) config).getAppPackage();
    }

    public static String getAdminApp() {
        return ((CommonConfig) config).getAdminApp();
    }


    public interface Common extends org.aeonbits.owner.Reloadable, org.aeonbits.owner.Accessible, org.aeonbits.owner.Mutable {

    }


    public interface CommonConfig {

        @org.aeonbits.owner.Config.DefaultValue("false")
        @org.aeonbits.owner.Config.Key("is.local")
        boolean isLocal();

        @org.aeonbits.owner.Config.DefaultValue("")
        @org.aeonbits.owner.Config.Key("package")
        String getAppPackage();

        @org.aeonbits.owner.Config.DefaultValue("")
        @org.aeonbits.owner.Config.Key("admin.app")
        String getAdminApp();
    }
}
