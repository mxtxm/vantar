package com.vantar.util.json;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.impl.*;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.vantar.database.datatype.Location;
import com.vantar.exception.DateTimeException;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * static: ignored
 * null-properties-to-json: ignored (default)
 * null-properties-from-json: sets property to null
 * not-existing-json-properties: ignored (object property value or default value is preserved)
 * methods/getter/setter: ignored (default)
 * private: ignored (default)
 * protected: ignored (default)
 * interface: addPolymorphism(interface, class)
 * string-to-number: works ("12.3" > double works) ("12.3" > int/long not works)
 * number-to-string: works
 * number-to-number: works (double to int/long works)
 * bool: (works true false "true" "false" 0 1 2 3) ("1", "0" not works)
 * Map: works
 * List: works
 * Set: works
 * Enum: string
 * DateTime: string
 * Location-from-json: string or {latitude, longitude, height, countryCode}
 * Location-to-json: {latitude, longitude, height, countryCode}
 */
public class Jackson {

    private static final Logger log = LoggerFactory.getLogger(Jackson.class);
    private final ObjectMapper mapper;


    public Jackson() {
        this(new Json.Config());
    }

    public Jackson(Json.Config config) {
        JsonAutoDetect.Visibility propertyAccess;
        if (config.propertyPrivate) {
            propertyAccess = JsonAutoDetect.Visibility.ANY;
        } else if (config.propertyProtected) {
            propertyAccess = JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC;
        } else {
            propertyAccess = JsonAutoDetect.Visibility.PUBLIC_ONLY;
        }

        mapper = JsonMapper.builder()
            .visibility(PropertyAccessor.FIELD, propertyAccess)
            .visibility(PropertyAccessor.GETTER, config.getter ? JsonAutoDetect.Visibility.PUBLIC_ONLY : JsonAutoDetect.Visibility.NONE)
            .visibility(PropertyAccessor.IS_GETTER, config.getter ? JsonAutoDetect.Visibility.PUBLIC_ONLY : JsonAutoDetect.Visibility.NONE)
            .visibility(PropertyAccessor.SETTER, config.setter ? JsonAutoDetect.Visibility.PUBLIC_ONLY : JsonAutoDetect.Visibility.NONE)
            .visibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.NONE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
            .build();

        if (config.skipNulls) {
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }

        SimpleModule mBoolean = new SimpleModule();
        mBoolean.addDeserializer(Boolean.class, new BooleanDeserializer());
        mapper.registerModule(mBoolean);

        SimpleModule mDateTime = new SimpleModule();
        mDateTime.addDeserializer(DateTime.class, new DateTimeDeserializer());
        mDateTime.addSerializer(DateTime.class, new DateTimeSerializer());
        mapper.registerModule(mDateTime);

        SimpleModule mLocation = new SimpleModule();
        mLocation.addDeserializer(Location.class, new LocationDeserializer());
        mLocation.addSerializer(Location.class, new LocationSerializer());
        mapper.registerModule(mLocation);

        SimpleModule mFiled = new SimpleModule();
        mFiled.addSerializer(Field.class, new FieldSerializer());
        mapper.registerModule(mFiled);
    }

    public <T> void addPolymorphism(Class<T> typeClass, Class<? extends T> objectClass) {
        mapper.registerModule(new SimpleModule().addAbstractTypeMapping(typeClass, objectClass));
    }

    public void ignore(String... properties) {
        SimpleBeanPropertyFilter filter = SimpleBeanPropertyFilter.serializeAllExcept(properties);
        mapper.setFilterProvider(new SimpleFilterProvider().addFilter("ignoreFilter", filter));
    }

    public boolean isJson(Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        try {
            mapper.readTree((String) value);
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    // > > > TO JSON

    public String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            log.warn(" ! failed to create JSON ({})\n", object, e);
            return null;
        }
    }

    public String toJsonPretty(Object object) {
        if (object instanceof String) {
            try {
                object = mapper.readTree((String) object);
            } catch (IOException e) {
                log.warn(" ! failed to open JSON ({})\n", object, e);
                return null;
            }
        }

        try {
            return mapper.writer(getPrettyPrinter()).writeValueAsString(object);
        } catch (Exception e) {
            log.warn(" ! failed to create JSON ({})\n", object, e);
            return null;
        }
    }

    public void toJson(Object object, OutputStream stream) {
        try {
            mapper.writeValue(stream, object);
        } catch (Exception e) {
            log.warn("! failed to create JSON ({})\n", object, e);
        }
    }

    public void toJsonPretty(Object object, OutputStream stream) {
        try {
            object = mapper.readTree((String) object);
        } catch (IOException e) {
            log.warn("! failed to open JSON ({})\n", object, e);
            return;
        }
        try {
            mapper.writer(getPrettyPrinter()).writeValue(stream, object);
        } catch (Exception e) {
            log.warn("! failed to create JSON ({})\n", object, e);
        }
    }

    private DefaultPrettyPrinter getPrettyPrinter() {
        DefaultPrettyPrinter.Indenter indent = new DefaultIndenter("    ", "\n");
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indent);
        printer.indentArraysWith(indent);
        return printer;
    }

    // TO JSON < < <

    // > > > FROM JSON

    @SuppressWarnings("unchecked")
    public <T> T fromJson(String json, String className) {
        try {
            return (T) fromJson(json, Class.forName(className));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public <T> T fromJson(String json, Class<T> typeClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(json, typeClass);
        } catch (MismatchedInputException e) {
            log.warn("! type missmatch ({} > {})\n", json, typeClass.getName());
            return null;
        } catch (Exception e) {
            log.warn("! failed to get object ({} > {})\n", json, typeClass.getName(), e);
            return null;
        }
    }

    public <T> T fromJsonSilent(String json, Class<T> typeClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(json, typeClass);
        } catch (Exception e) {
            return null;
        }
    }

    public <V> List<V> listFromJson(String json, Class<V> v) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, v));
        } catch (Exception e) {
            log.warn("! failed to get object ({} > List<{}>)\n", json, v.getName(), e);
            return null;
        }
    }

    public <V> Set<V> setFromJson(String json, Class<V> v) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(Set.class, v));
        } catch (Exception e) {
            log.warn("! failed to get object ({} > List<{}>)\n", json, v.getName(), e);
            return null;
        }
    }

    public <K, V> Map<K, V> mapFromJson(String json, Class<K> k, Class<V> v) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructMapType(Map.class, k, v));
        } catch (Exception e) {
            log.warn("! failed to get object ({} > Map<{},{}>)\n", json, k.getName(), v.getName(), e);
            return null;
        }
    }

    public String extractRaw(String json, String... key) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        JsonNode node = key.length == 1 ? getNode(json, key[0]) : getNodePath(json, key);
        return node == null ? null : node.toString();
    }

    public String extractString(String json, String... key) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        JsonNode node = key.length == 1 ? getNode(json, key[0]) : getNodePath(json, key);
        return node == null ? null : node.asText();
    }

    public Long extractLong(String json, String... key) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        JsonNode node = key.length == 1 ? getNode(json, key[0]) : getNodePath(json, key);
        return node == null ? null : node.asLong();
    }

    public Integer extractInteger(String json, String... key) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        JsonNode node = key.length == 1 ? getNode(json, key[0]) : getNodePath(json, key);
        return node == null ? null : node.asInt();
    }

    public Boolean extractBoolean(String json, String... key) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        JsonNode node = key.length == 1 ? getNode(json, key[0]) : getNodePath(json, key);
        return node == null ? null : node.asBoolean();
    }

    public Double extractDouble(String json, String... key) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        JsonNode node = key.length == 1 ? getNode(json, key[0]) : getNodePath(json, key);
        return node == null ? null : node.asDouble();
    }

    @SuppressWarnings("unchecked")
    public <T> T extract(String json, String key, Class<T> type) {
        if (json == null) {
            return null;
        }
        if (type.equals(String.class)) {
            return (T) extractString(json, key);
        }
        if (type.equals(Long.class)) {
            return (T) extractLong(json, key);
        }
        if (type.equals(Integer.class)) {
            return (T) extractInteger(json, key);
        }
        if (type.equals(Double.class)) {
            return (T) extractDouble(json, key);
        }
        if (type.equals(Boolean.class)) {
            return (T) extractBoolean(json, key);
        }
        return null;
    }

    private JsonNode getNode(String json, String key) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JsonNode t = mapper.readTree(json);
            if (t == null) {
                return null;
            }
            JsonNode v = t.get(key);
            return v == null ? null : t.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode getNodePath(String json, String[] keys) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        JsonNode node = null;
        for (String key : keys) {
            if (node == null) {
                node = getNode(json, key);
            } else {
                node = node.findValue(key);
                if (node == null) {
                    return null;
                }
            }
        }
        return node;
    }

    // < < < FROM JSON

    // > > > DATETIME

    public static class DateTimeDeserializer extends StdDeserializer<DateTime> {

        public DateTimeDeserializer() {
            this(null);
        }

        public DateTimeDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public DateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            try {
                return new DateTime(parser.getValueAsString());
            } catch (DateTimeException e) {
                return null;
            }
        }
    }

    public static class DateTimeSerializer extends StdSerializer<DateTime> {

        public DateTimeSerializer() {
            this(null);
        }

        public DateTimeSerializer(Class<DateTime> t) {
            super(t);
        }

        @Override
        public void serialize(DateTime dateTime, JsonGenerator generator, SerializerProvider provider) throws IOException {
            if (dateTime == null) {
                generator.writeNull();
                return;
            }
            generator.writeString(dateTime.toString());
        }
    }

    // > > > LOCATION

    public static class LocationDeserializer extends StdDeserializer<Location> {

        public LocationDeserializer() {
            this(null);
        }

        public LocationDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Location deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            ObjectCodec codec = parser.getCodec();
            JsonNode node = codec.readTree(parser);
            JsonNode v = node.get("latitude");
            if (v == null) {
                return new Location(node.asText());
            }
            Location location = new Location();
            location.latitude = v.asDouble();
            v = node.get("longitude");
            location.longitude = v == null ? null : v.asDouble();
            v = node.get("height");
            location.altitude = v == null ? null : v.asDouble();
            v = node.get("countryCode");
            location.countryCode = v == null ? null : v.asText();
            return location.isEmpty() || !location.isValid() ? null : location;
        }
    }

    public static class LocationSerializer extends StdSerializer<Location> {

        public LocationSerializer() {
            this(null);
        }

        protected LocationSerializer(Class<Location> t) {
            super(t);
        }

        @Override
        public void serialize(Location location, JsonGenerator generator, SerializerProvider provider) throws IOException {
            if (location == null || location.isEmpty() || !location.isValid()) {
                generator.writeNull();
                return;
            }
            generator.writeStartObject();
            if (location.latitude != null) {
                generator.writeNumberField("latitude", location.latitude);
            }
            if (location.longitude != null) {
                generator.writeNumberField("longitude", location.longitude);
            }
            if (location.altitude != null) {
                generator.writeNumberField("altitude", location.altitude);
            }
            if (location.countryCode != null) {
                generator.writeStringField("countryCode", location.countryCode);
            }
            generator.writeEndObject();
        }
    }

    // > > > Field

    public static class FieldSerializer extends StdSerializer<Field> {

        public FieldSerializer() {
            this(null);
        }

        protected FieldSerializer(Class<Field> t) {
            super(t);
        }

        @Override
        public void serialize(Field field, JsonGenerator generator, SerializerProvider provider) throws IOException {
            if (field == null) {
                generator.writeNull();
                return;
            }
            generator.writeString(field.getName() + " (" + field.getType().getSimpleName() + ")");
        }
    }

    // > > > BOOLEAN

    public static class BooleanDeserializer extends StdDeserializer<Boolean> {

        public BooleanDeserializer() {
            this(null);
        }

        public BooleanDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return StringUtil.toBoolean(parser.getValueAsString());
        }
    }

}
