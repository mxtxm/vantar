package com.vantar.util.json;

import com.google.gson.*;
import com.google.gson.internal.bind.TypeAdapters;
import com.vantar.database.datatype.Location;
import com.vantar.service.auth.*;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.lang.reflect.*;
import java.util.*;


public class JsonAllProps {

    private static final Logger log = LoggerFactory.getLogger(JsonAllProps.class);
    private static Gson gson;


    public static Gson gson() {
        if (gson == null) {
            gson = getBuilder().create();
        }
        return gson;
    }
    private synchronized static GsonBuilder getBuilder() {
        return new GsonBuilder()
            .registerTypeHierarchyAdapter(Collection.class, new GsonCustom.CollectionDeserializer())
            .registerTypeHierarchyAdapter(Map.class, new GsonCustom.MapDeserializerAllProps())
            .registerTypeAdapterFactory(TypeAdapters.newFactory(String.class, String.class, GsonCustom.typeAdapterString))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(int.class, Integer.class, GsonCustom.typeAdapterInteger))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(long.class, Long.class, GsonCustom.typeAdapterLong))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(boolean.class, Boolean.class, GsonCustom.typeAdapterBoolean))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(double.class, Double.class, GsonCustom.typeAdapterDouble))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(float.class, Float.class, GsonCustom.typeAdapterFloat))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(DateTime.class, GsonCustom.typeAdapterDateTime))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(Location.class, GsonCustom.typeAdapterLocation))
//            .registerTypeAdapter(CommonUser.class, new GsonCustom.InterfaceAdapter())
//            .registerTypeAdapter(CommonUserRole.class, new GsonCustom.InterfaceAdapter())
            .excludeFieldsWithModifiers(Modifier.STATIC)
            .setDateFormat("yyyy-MM-dd hh:mm:ss");
    }

    public static void reset() {
        gson = null;
    }

//    public static void addKoon(Class<?> interfaceType, Class<?> classType) {
//        gson = getBuilder()
//            .registerTypeAdapter(interfaceType, new GsonCustom.InterfaceAdapterX<CommonUser>(classType))
//            .create();
//    }

    public static void addInterface(Class<?>... interfaces) {
//        GsonBuilder builder = getBuilder();
//        for (Class<?> i : interfaces) {
//            builder.registerTypeAdapter(i, new GsonCustom.InterfaceAdapter());
//        }
//        gson = builder.create();
    }



    // > > > TO STRING



    public static String toJson(Object object) {
        try {
            return gson().toJson(object);
        } catch (Exception e) {
            log.warn("! failed to create JSON ({})\n", object, e);
            return null;
        }
    }



    public static String makePretty(String jsonString) {
        return StringUtil.isEmpty(jsonString) ? "" : toJsonPretty(JsonParser.parseString(jsonString).getAsJsonObject());
    }
    public static String toJsonPretty(Object object) {
        return object == null ? "" : getBuilder().setPrettyPrinting().create().toJson(object);
    }



    // > > > TO OBJECT




    public static <T> T fromJson(String value, Class<T> typeClass) {
        try {
            return gson().fromJson(value, typeClass);
        } catch (JsonParseException e) {
            log.warn("! failed to open JSON ({}, {})\n", typeClass, value, e);
            return null;
        }
    }

    public static <T> T fromJson(String value, Type type) {
        try {
            return gson().fromJson(value, type);
        } catch (JsonParseException e) {
            log.warn("! failed to open JSON ({}, {})\n", type, value, e);
            return null;
        }
    }

    public static <T> List<T> listFromJson(String value, Class<T> typeClass) {
        if (StringUtil.isNotEmpty(value) && !value.trim().startsWith("[")) {
            log.warn("! invalid JSON list (List<{}>, {})\n", typeClass, value);
            return null;
        }
        try {
            return gson().fromJson(value, new ListOfJson<>(typeClass));
        } catch (JsonParseException e) {
            log.warn("! failed to open JSON (List<{}>, {})\n", typeClass, value, e);
            return null;
        }
    }

    public static <K, V> Map<K, V> mapFromJson(String value, Class<K> typeClassKey, Class<V> typeClassValue) {
        if (StringUtil.isNotEmpty(value) && !value.trim().startsWith("{")) {
            log.warn("! invalid JSON list (Map<{}, {}>, {})\n", typeClassKey, typeClassValue, value);
            return null;
        }
        try {
            return gson().fromJson(value, new MapOfJson<>(typeClassKey, typeClassValue));
        } catch (JsonParseException e) {
            log.warn("! failed to open JSON (Map<{}, {}>, {})\n", typeClassKey, typeClassValue, value);
            return null;
        }
    }
}