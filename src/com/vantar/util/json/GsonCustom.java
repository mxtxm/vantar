package com.vantar.util.json;

import com.google.gson.*;
import com.google.gson.stream.*;
import com.vantar.database.datatype.Location;
import com.vantar.exception.DateTimeException;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;


public class GsonCustom {

    public static final TypeAdapter<String> typeAdapterString = new TypeAdapter<String>() {

        @Override
        public String read(JsonReader reader) throws IOException {
            JsonToken jsonToken = reader.peek();
            switch (jsonToken) {
                case NUMBER:
                case STRING:
                    String v = reader.nextString();
                    return StringUtil.isEmpty(v) ? null : v.trim();
                case NULL:
                    reader.nextNull();
                    return null;
                case BOOLEAN:
                    reader.nextBoolean();
                    return null;
                default:
                    throw new JsonSyntaxException("! expecting number, got: " + jsonToken);
            }
        }

        @Override
        public void write(JsonWriter writer, String value) throws IOException {
            writer.value(value);
        }
    };

    public static final TypeAdapter<Number> typeAdapterInteger = new TypeAdapter<Number>() {

        @Override
        public Number read(JsonReader reader) throws IOException {
            JsonToken jsonToken = reader.peek();
            switch (jsonToken) {
                case NUMBER:
                case STRING:
                    return StringUtil.toInteger(reader.nextString());
                case NULL:
                    reader.nextNull();
                    return null;
                case BOOLEAN:
                    reader.nextBoolean();
                    return null;
                default:
                    throw new JsonSyntaxException("! expecting number, got: " + jsonToken);
            }
        }

        @Override
        public void write(JsonWriter writer, Number value) throws IOException {
            writer.value(value);
        }
    };

    public static final TypeAdapter<Number> typeAdapterLong = new TypeAdapter<Number>() {
        @Override
        public Number read(JsonReader reader) throws IOException {
            JsonToken jsonToken = reader.peek();
            switch (jsonToken) {
                case NUMBER:
                case STRING:
                    return StringUtil.toLong(reader.nextString());
                case NULL:
                    reader.nextNull();
                    return null;
                case BOOLEAN:
                    reader.nextBoolean();
                    return null;
                default:
                    throw new JsonSyntaxException("! expecting number, got: " + jsonToken);
            }
        }

        @Override
        public void write(JsonWriter writer, Number value) throws IOException {
            writer.value(value);
        }
    };

    public static final TypeAdapter<Number> typeAdapterDouble = new TypeAdapter<Number>() {
        @Override
        public Number read(JsonReader reader) throws IOException {
            JsonToken jsonToken = reader.peek();
            switch (jsonToken) {
                case NUMBER:
                case STRING:
                    return StringUtil.toDouble(reader.nextString());
                case NULL:
                    reader.nextNull();
                    return null;
                case BOOLEAN:
                    reader.nextBoolean();
                    return null;
                default:
                    throw new JsonSyntaxException("! expecting number, got: " + jsonToken);
            }
        }

        @Override
        public void write(JsonWriter writer, Number value) throws IOException {
            writer.value(value);
        }
    };

    public static final TypeAdapter<Number> typeAdapterFloat = new TypeAdapter<Number>() {
        @Override
        public Number read(JsonReader reader) throws IOException {
            JsonToken jsonToken = reader.peek();
            switch (jsonToken) {
                case NUMBER:
                case STRING:
                    return StringUtil.toFloat(reader.nextString());
                case NULL:
                    reader.nextNull();
                    return null;
                case BOOLEAN:
                    reader.nextBoolean();
                    return null;
                default:
                    throw new JsonSyntaxException("! expecting number, got: " + jsonToken);
            }
        }

        @Override
        public void write(JsonWriter writer, Number value) throws IOException {
            writer.value(value);
        }
    };

    public static final TypeAdapter<Boolean> typeAdapterBoolean = new TypeAdapter<Boolean>() {
        @Override
        public Boolean read(JsonReader reader) throws IOException {
            JsonToken jsonToken = reader.peek();
            switch (jsonToken) {
                case NUMBER:
                case STRING:
                    return StringUtil.toBoolean(reader.nextString());
                case NULL:
                    reader.nextNull();
                    return null;
                case BOOLEAN:
                    return reader.nextBoolean();
                default:
                    throw new JsonSyntaxException("! expecting boolean, got: " + jsonToken);
            }
        }

        @Override
        public void write(JsonWriter writer, Boolean value) throws IOException {
            writer.value(value);
        }
    };

    public static final TypeAdapter<DateTime> typeAdapterDateTime = new TypeAdapter<DateTime>() {
        @Override
        public DateTime read(JsonReader reader) throws IOException {
            JsonToken jsonToken = reader.peek();
            switch (jsonToken) {
                case NUMBER:
                case STRING:
                    String value = reader.nextString();
                    try {
                        return StringUtil.isEmpty(value) ? null : new DateTime(value);
                    } catch (DateTimeException e) {
                        return null;
                    }
                case NULL:
                    reader.nextNull();
                    return null;
                case BOOLEAN:
                    reader.nextBoolean();
                    return null;
                default:
                    throw new JsonSyntaxException("! expecting DateTime, got: " + jsonToken);
            }
        }

        @Override
        public void write(JsonWriter writer, DateTime value) throws IOException {
            writer.value(value == null ? null : value.toString());
        }
    };


    public static final TypeAdapter<Location> typeAdapterLocation = new TypeAdapter<Location>() {
        @Override
        public Location read(JsonReader reader) throws IOException {
            JsonToken jsonToken = reader.peek();
            switch (jsonToken) {
                case STRING:
                    String value = reader.nextString();

                    if (StringUtil.isEmpty(value)) {
                        return null;
                    }

                    if (value.startsWith("{")) {
                        return new GsonBuilder().create().fromJson(value, Location.class);
                    }

                    String[] latLng = StringUtil.split(value, ',');
                    if (latLng.length != 2) {
                        return null;
                    }
                    Double lat = StringUtil.toDouble(latLng[0]);
                    Double lng = StringUtil.toDouble(latLng[1]);
                    return lat == null || lng == null ? null : new Location(lat, lng);
                case NUMBER:
                    reader.nextNull();
                    return null;
                case BOOLEAN:
                    reader.nextBoolean();
                    return null;
                case BEGIN_OBJECT:
                    reader.beginObject();
                    String nameA = reader.nextName();
                    double vA = reader.nextDouble();
                    String nameB = reader.nextName();
                    double vB = reader.nextDouble();
                    reader.endObject();

                    double latitude;
                    double longitude;
                    if (nameA.equals("latitude")) {
                        latitude = vA;
                        longitude = vB;
                    } else {
                        latitude = vB;
                        longitude = vA;
                    }
                    return new Location(latitude, longitude);
                default:
                    throw new JsonSyntaxException("! expecting Location, got: " + jsonToken);
            }
        }

        @Override
        public void write(JsonWriter writer, Location value) throws IOException {
            if (value == null || value.isEmpty()) {
                String v = null;
                writer.value(v);
                return;
            }
            writer.beginObject();
            writer.name("latitude");
            writer.value(value.latitude);
            writer.name("longitude");
            writer.value(value.longitude);
            writer.endObject();
        }
    };


    public static class CollectionDeserializer implements JsonDeserializer<Collection<?>> {

        @Override
        public Collection<?> deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull() || json.isJsonPrimitive()) {
                return null;
            }

            Class<?> collectionType = ClassUtil.typeToClass(type);
            if (collectionType == null) {
                return null;
            }

            Type genericType;
            try {
                genericType = ((ParameterizedType) type).getActualTypeArguments()[0];
            } catch (ClassCastException e) {
                genericType = Object.class;
            }

            if (collectionType == List.class || collectionType == ArrayList.class) {
                return parseCollection(json, new ArrayList<>(), genericType);
            } else if (collectionType == Set.class || collectionType == HashSet.class) {
                return parseCollection(json, new HashSet<>(), genericType);
            } else if (collectionType == LinkedHashSet.class) {
                return parseCollection(json, new LinkedHashSet<>(), genericType);
            } else if (collectionType == LinkedList.class) {
                return parseCollection(json, new LinkedList<>(), genericType);
            } else if (collectionType == Vector.class) {
                return parseCollection(json, new Vector<>(), genericType);
            } else if (collectionType == Deque.class || collectionType == ArrayDeque.class) {
                return parseCollection(json, new ArrayDeque<>(), genericType);
            } else if (collectionType == Queue.class || collectionType == PriorityQueue.class) {
                return parseCollection(json, new PriorityQueue<>(), genericType);
            } else if (collectionType == CopyOnWriteArraySet.class) {
                return parseCollection(json, new CopyOnWriteArraySet<>(), genericType);
            } else if (collectionType == ConcurrentLinkedDeque.class) {
                return parseCollection(json, new ConcurrentLinkedDeque<>(), genericType);
            } else if (collectionType == BlockingDeque.class || collectionType == LinkedBlockingDeque.class) {
                return parseCollection(json, new LinkedBlockingDeque<>(), genericType);
            } else if (collectionType == LinkedTransferQueue.class) {
                return parseCollection(json, new LinkedTransferQueue<>(), genericType);
            } else if (collectionType == BlockingQueue.class || collectionType == LinkedBlockingQueue.class) {
                return parseCollection(json, new LinkedBlockingQueue<>(), genericType);
            } else if (collectionType == SynchronousQueue.class) {
                return parseCollection(json, new SynchronousQueue<>(), genericType);
            } else if (collectionType == ConcurrentLinkedQueue.class) {
                return parseCollection(json, new ConcurrentLinkedQueue<>(), genericType);
            } else if (collectionType == PriorityBlockingQueue.class) {
                return parseCollection(json, new PriorityBlockingQueue<>(), genericType);
            }

            return null;
        }

        @SuppressWarnings("unchecked")
        public <T> Collection<T> parseCollection(JsonElement json, Collection<T> collection, T genericType) {
            Gson gson = Json.gson();
            for (JsonElement json2 : json.getAsJsonArray()) {
                collection.add((T) gson.fromJson(json2,  (Type) genericType));
            }
            return collection;
        }
    }

    public static class MapDeserializer implements JsonDeserializer<Map<?, ?>> {

        @Override
        public Map<?, ?> deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull() || json.isJsonPrimitive()) {
                return null;
            }

            Class<?> mapType = ClassUtil.typeToClass(type);
            if (mapType == null) {
                return null;
            }

            Type keyType;
            Type valueType;

            if (type instanceof ParameterizedType) {
                keyType = ((ParameterizedType) type).getActualTypeArguments()[0];
                valueType = ((ParameterizedType) type).getActualTypeArguments()[1];
            } else {
                keyType = Object.class;
                valueType = Object.class;
            }

            if (mapType == Map.class || mapType == HashMap.class) {
                return parseMap(json, new HashMap<>(), keyType, valueType);
            } else if (mapType == LinkedHashMap.class) {
                return parseMap(json, new LinkedHashMap<>(), keyType, valueType);
            } else if (mapType == ConcurrentMap.class || mapType == ConcurrentHashMap.class) {
                return parseMap(json, new ConcurrentHashMap<>(), keyType, valueType);
            } else if (mapType == IdentityHashMap.class) {
                return parseMap(json, new IdentityHashMap<>(), keyType, valueType);
            }

            return null;
        }

        @SuppressWarnings("unchecked")
        public <K, V> Map<K, V> parseMap(JsonElement json, Map<K, V> map, K k, V v) {
            Gson gson = Json.gson();
            for (Map.Entry<?, ?> entry : json.getAsJsonObject().entrySet()) {
                map.put(
                    (K) ObjectUtil.convert(entry.getKey(), ClassUtil.typeToClass((Type) k)),
                    gson.fromJson((JsonElement) entry.getValue(), (Type) v)
                );
            }
            return map;
        }
    }


    public static class InterfaceAdapter implements JsonSerializer, JsonDeserializer {

        private static final String CLASSNAME = "CLASSNAME";
        private static final String DATA = "DATA";

        @Override
        public Object deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonPrimitive prim = (JsonPrimitive) jsonObject.get(CLASSNAME);
            String className = prim.getAsString();
            Class<?> klass = getObjectClass(className);
            return jsonDeserializationContext.deserialize(jsonObject.get(DATA), klass);
        }

        @Override
        public JsonElement serialize(Object jsonElement, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(CLASSNAME, jsonElement.getClass().getName());
            jsonObject.add(DATA, jsonSerializationContext.serialize(jsonElement));
            return jsonObject;
        }

        public Class<?> getObjectClass(String className) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(e.getMessage());
            }
        }
    }
}
