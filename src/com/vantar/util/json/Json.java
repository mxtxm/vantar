package com.vantar.util.json;

import com.google.gson.*;
import com.google.gson.internal.bind.TypeAdapters;
import com.vantar.database.datatype.Location;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.lang.reflect.*;
import java.util.*;


public class Json {

    private static final Logger log = LoggerFactory.getLogger(Json.class);
    private static Gson gson;


    public static Gson gson() {
        if (gson == null) {
            gson = getBuilder().create();
        }
        return gson;
    }

    public static void addInterface(Class<?>... interfaces) {
        GsonBuilder builder = gson().newBuilder();
        for (Class<?> i : interfaces) {
            builder.registerTypeAdapter(i, new GsonCustom.InterfaceAdapter());
        }
        gson = builder.create();
    }

    public static void reset() {
        gson = null;
    }

    private static GsonBuilder getBuilder() {
        return new GsonBuilder()
            .registerTypeHierarchyAdapter(Collection.class, new GsonCustom.CollectionDeserializer())
            .registerTypeHierarchyAdapter(Map.class, new GsonCustom.MapDeserializer())
            .registerTypeAdapterFactory(TypeAdapters.newFactory(String.class, String.class, GsonCustom.typeAdapterString))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(int.class, Integer.class, GsonCustom.typeAdapterInteger))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(long.class, Long.class, GsonCustom.typeAdapterLong))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(boolean.class, Boolean.class, GsonCustom.typeAdapterBoolean))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(double.class, Double.class, GsonCustom.typeAdapterDouble))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(float.class, Float.class, GsonCustom.typeAdapterFloat))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(DateTime.class, GsonCustom.typeAdapterDateTime))
            .registerTypeAdapterFactory(TypeAdapters.newFactory(Location.class, GsonCustom.typeAdapterLocation))
            .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.PRIVATE, Modifier.PROTECTED)
            .setDateFormat("yyyy-MM-dd hh:mm:ss");
    }

    public static String toJson(Object object) {
        return gson().toJson(object);
    }

    public static String makePretty(String jsonString) {
        return StringUtil.isEmpty(jsonString) ? "" : toJsonPretty(JsonParser.parseString(jsonString).getAsJsonObject());
    }

    public static String toJsonPretty(Object object) {
        return object == null ? "" : getBuilder().setPrettyPrinting().create().toJson(object);
    }

    public static <T> T fromJson(String value, Class<T> typeClass) {
        try {
            return gson().fromJson(value, typeClass);
        } catch (JsonParseException e) {
            log.warn("! ({}, {}) > ", value, typeClass, e);
            return null;
        }
    }

    public static <T> T fromJson(String value, Type type) {
        try {
            return gson().fromJson(value, type);
        } catch (JsonParseException e) {
            log.warn("! ({}, {}) > ", value, type, e);
            return null;
        }
    }

    public static <T> T fromJsonNotChecked(String value, Class<T> typeClass) {
        return gson().fromJson(value, typeClass);
    }

    public static <T> T fromJsonNotChecked(String value, Type type) {
        return gson().fromJson(value, type);
    }

    public static <T> List<T> listFromJson(String value, Class<T> typeClass) {
        if (StringUtil.isNotEmpty(value) && !value.trim().startsWith("[")) {
            log.warn("! ({}, {}) > invalid json list", value, typeClass);
            return null;
        }
        try {
            return gson().fromJson(value, new ListOfJson<>(typeClass));
        } catch (JsonParseException e) {
            log.warn("! ({}, {}) > ", value, typeClass, e);
            return null;
        }
    }

    public static <K, V> Map<K, V> mapFromJson(String value, Class<K> typeClassKey, Class<V> typeClassValue) {
        if (StringUtil.isNotEmpty(value) && !value.trim().startsWith("{")) {
            log.warn("! ({}, {}, {}) > ", value, typeClassKey, typeClassValue);
            return null;
        }
        try {
            return gson().fromJson(value, new MapOfJson<>(typeClassKey, typeClassValue));
        } catch (JsonParseException e) {
            log.warn("! ({}, {}, {}) > ", value, typeClassKey, typeClassValue, e);
            return null;
        }
    }

    public static String elementToString(JsonElement elem) {
        if (elem == null) {
            return  null;
        }
        try {
            if (elem.isJsonPrimitive()) {
                return elem.getAsString();
            } else if (elem.isJsonObject()) {
                return elem.getAsJsonObject().toString();
            } else if (elem.isJsonArray()) {
                return arrayToString(elem.getAsJsonArray());
            }
        } catch (JsonParseException | UnsupportedOperationException | IllegalStateException e) {
            log.error("! > ", e);
        }
        return null;
    }

    public static String arrayToString(JsonArray jsonArray) {
        StringBuilder sb = new StringBuilder(2000);
        for (JsonElement elem : jsonArray) {
            sb.append(elementToString(elem)).append(',');
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public static String objectToString(JsonObject jsonObject) {
        StringBuilder sb = new StringBuilder(2000);
        for (Map.Entry<String, JsonElement> item : jsonObject.entrySet()) {
            sb.append(item.getKey()).append(":").append(elementToString(item.getValue())).append(',');
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public static String getStringCaseInsensitive(JsonObject jsonObject, String key) {
        JsonElement element = getCaseInsensitive(jsonObject, key);
        if (element == null) {
            return null;
        }
        return element.getAsString();
    }

    public static JsonElement getCaseInsensitive(JsonObject jsonObject, String key) {
        for (String k : jsonObject.keySet()) {
            if (k.equalsIgnoreCase(key)) {
                return jsonObject.get(k);
            }
        }
        return null;
    }
}