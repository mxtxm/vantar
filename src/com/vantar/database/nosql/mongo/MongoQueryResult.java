package com.vantar.database.nosql.mongo;

import com.google.gson.reflect.TypeToken;
import com.mongodb.*;
import com.mongodb.client.*;
import com.vantar.common.VantarParam;
import com.vantar.database.common.KeyValueData;
import com.vantar.database.datatype.Location;
import com.vantar.database.dto.Date;
import com.vantar.database.dto.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.util.string.*;
import org.bson.Document;
import java.lang.reflect.*;
import java.util.*;


@SuppressWarnings({"unchecked"})
public class MongoQueryResult extends QueryResultBase implements QueryResult, AutoCloseable {

    private final Iterator<Document> iterator;
    public FindIterable<Document> cursor;
    public MongoCursor<Document> cursorA;


    public MongoQueryResult(FindIterable<Document> cursor, Dto dto) {
        this.dto = dto;
        this.cursor = cursor;
        iterator = cursor.iterator();
        fields = dto.getClass().getFields();
        exclude = dto.getExclude();
    }

    public MongoQueryResult(AggregateIterable<Document> cursor, Dto dto) {
        this.dto = dto;
        cursor.allowDiskUse(true);
        this.cursorA = cursor.cursor();
        iterator = cursor.iterator();
        fields = dto.getClass().getFields();
        exclude = dto.getExclude();
    }

    public void close() {

    }

    public Object peek(String field) throws NoContentException, DatabaseException {
        try {
            Document data = cursor.first();
            if (data == null) {
                throw new NoContentException();
            }
            return data.get(StringUtil.toSnakeCase(field));
        } catch (MongoException e) {
            throw new DatabaseException(e);
        }
    }

    public boolean next() throws DatabaseException {
        try {
            if (iterator.hasNext()) {
                mapRecordToDto(iterator.next());
                return true;
            }
        } catch (MongoException e) {
            throw new DatabaseException(e);
        }
        return false;
    }

    public <T extends Dto> T first() throws NoContentException, DatabaseException {
        try {
            Document data = cursor.first();
            if (data == null) {
                throw new NoContentException();
            }
            mapRecordToDto(data);
            return (T) dto;
        } catch (MongoException e) {
            throw new DatabaseException(e);
        }
    }

    public Map<String, String> asKeyValue(String keyField, String valueField) throws NoContentException, DatabaseException {
        if (keyField.equals("id")) {
            keyField = Mongo.ID;
        }
        if (valueField.equals("id")) {
            valueField = Mongo.ID;
        }
        Map<String, String> result = new HashMap<>();
        String locale = getLocale();
        try {
            while (iterator.hasNext()) {
                Document document = iterator.next();
                Object k = document.get(keyField);
                if (k == null) {
                    continue;
                }
                result.put(DbUtil.getKv(k, locale), DbUtil.getKv(document.get(valueField), locale));
            }
        } catch (MongoException e) {
            throw new DatabaseException(e);
        }
        if (result.isEmpty()) {
            throw new NoContentException();
        }
        return result;
    }

    public Map<String, String> asKeyValue(KeyValueData definition) throws NoContentException, DatabaseException {
        String keyField = definition.getKeyField();
        String valueField = definition.getValueField();
        if (keyField.equals("id")) {
            keyField = Mongo.ID;
        }
        if (valueField.equals("id")) {
            valueField = Mongo.ID;
        }
        Map<String, String> result = new HashMap<>();
        String locale = getLocale();
        try {
            while (iterator.hasNext()) {
                Document document = iterator.next();
                Object k = document.get(keyField);
                if (k == null) {
                    continue;
                }
                result.put(DbUtil.getKv(k, locale), DbUtil.getKv(document.get(valueField), locale));
            }
        } catch (MongoException e) {
            throw new DatabaseException(e);
        }
        if (result.isEmpty()) {
            throw new NoContentException();
        }
        return result;
    }

    private void mapRecordToDto(Document document) {
        mapRecordToObject(document, dto, fields);
    }

    private void mapRecordToObject(Document document, Dto dto, Field[] fields) {
        try {
            long i = 0;
            Object value;
            for (Field field : fields) {
                String name = field.getName();
                if (DtoBase.isNotDataField(field) || (exclude != null && exclude.contains(name))) {
                    continue;
                }
                value = null;

                if (name.equals(VantarParam.ID) && document.containsKey(Mongo.ID)) {
                    name = Mongo.ID;
                }
                String key = StringUtil.toSnakeCase(name);
                Class<?> type = field.getType();

                try {
                    if (field.isAnnotationPresent(FetchCache.class)) {
                        fetchFromCache(document, dto, field, type);
                        continue;
                    }

                    if (field.isAnnotationPresent(Fetch.class)) {
                        fetchFromDatabase(document, dto, field, type);
                        continue;
                    }

                    if (field.isAnnotationPresent(DeLocalized.class)) {
                        deLocalize(document, dto, field, key);
                        continue;
                    }

                    if (field.isAnnotationPresent(StoreString.class)) {
                        String v = document.getString(key);
                        field.set(dto, v == null ? null : Json.fromJson(v, TypeToken.get(type).getType()));
                        continue;
                    }

                    if (type.isEnum()) {
                        EnumUtil.setEnumValue(dto, type, field, document.getString(key));
                        continue;
                    }

                    if (type == Location.class) {
                        Document point = (Document) document.get(key);
                        if (point == null) {
                            field.set(dto, null);
                            continue;
                        }
                        List<Double> coordinates = (List<Double>) point.get(VantarParam.COORDINATE);
                        field.set(dto, new Location(coordinates.get(0), coordinates.get(1)));
                        continue;
                    }

                    if (type == List.class || type == Set.class) {
                        Class<?>[] g = ObjectUtil.getFieldGenericTypes(field);
                        if (g == null || g.length != 1) {
                            log.warn("! type/value miss-match ({}.{}, {})", dto.getClass().getName(), field.getName(), value);
                            continue;
                        }
                        Class<?> listType = g[0];
                        List<?> v;

                        if (listType == String.class
                            || listType.getSuperclass() == Number.class
                            || listType == Number.class
                            || listType == Character.class
                            || listType == Boolean.class
                            || listType.getSuperclass() == Integer.class
                            || listType.getSuperclass() == Long.class
                            || listType.getSuperclass() == Double.class) {

                            v = document.getList(key, listType);
                            field.set(dto, v != null && type == Set.class ? new HashSet<>(v) : v);

                        } else if (listType == Dto.class || listType.getSuperclass() == DtoBase.class) {
                            List<Dto> list;
                            List<Document> docs = document.getList(key, Document.class);
                            if (docs == null) {
                                field.set(dto, null);
                                continue;
                            }

                            list = new ArrayList<>();
                            for (Document d : docs) {
                                Dto obj = (Dto) ObjectUtil.getInstance(listType);
                                if (obj == null) {
                                    continue;
                                }
                                mapRecordToObject(d, obj, obj.getFields());
                                list.add(obj);
                            }
                            field.set(dto, type == Set.class ? new HashSet<>(list) : list);

                        } else {
                            v = Json.listFromJson(Json.toJson(document.get(key)), listType);
                            field.set(dto, v != null && type == Set.class ? new HashSet<>(v) : v);
                        }
                        continue;
                    }

                    if (type == Map.class) {
                        Document v = (Document) document.get(key);
                        if (v == null) {
                            field.set(dto, null);
                        } else {
                            Class<?>[] g = ObjectUtil.getFieldGenericTypes(field);
                            if (g == null || g.length != 2) {
                                log.warn("! type/value miss-match ({}.{}, {})", dto.getClass().getName(), field.getName(), value);
                                continue;
                            }
                            field.set(dto, documentToMap(v, g[0], g[1]));
                        }
                        continue;
                    }

                    value = document.get(key);
                    if (value == null) {
                        field.set(dto, null);
                        continue;
                    }
                    if (type == Character.class) {
                        field.set(dto, ((String) value).charAt(0));
                        continue;
                    }
                    if (type == DateTime.class) {
                        DateTime t = new DateTime(value.toString());
                        if (field.isAnnotationPresent(Timestamp.class)) {
                            t.setType(DateTime.TIMESTAMP);
                        } else if (field.isAnnotationPresent(Date.class)) {
                            t.setType(DateTime.DATE);
                        } else if (field.isAnnotationPresent(Time.class)) {
                            t.setType(DateTime.TIME);
                        }
                        field.set(dto, t);
                        continue;
                    }

                    if (ObjectUtil.implementsInterface(type, Dto.class)) {
                        Dto obj = (Dto) ObjectUtil.getInstance(type);
                        if (obj != null) {
                            mapRecordToObject((Document) value, obj, obj.getFields());
                        }
                        field.set(dto, obj);
                        continue;
                    }

                    field.set(dto, value);

                } catch (IllegalArgumentException e) {
                    dto.setPropertyValue(field.getName(), value);
                    log.warn("! data > {}({}:{})", dto.getClass(), key, document.get(key), e);
                } catch (DateTimeException | ClassCastException e) {
                    log.error("! data > {}({}:{})", dto.getClass(), key, document.get(key), e);
                    field.set(dto, null);
                }
            }

            dto.afterFetchData();
            dto.afterFetchData(++i);
        } catch (IllegalAccessException e) {
            log.error("! data > dto", e);
        }
    }

    private <K, V> Map<K, V> documentToMap(Document document, Class<K> kClass, Class<V> vClass) {
        if (document == null) {
            return null;
        }

        Map<K, V> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            if (ObjectUtil.implementsInterface(vClass, Dto.class)) {
                V vDto = ObjectUtil.getInstance(vClass);
                mapRecordToObject((Document) entry.getValue(), (Dto) vDto, ((Dto) vDto).getFields());
                map.put(
                    (K) ObjectUtil.convert(entry.getKey(), kClass),
                    vDto
                );
                continue;
            }

            map.put(
                (K) ObjectUtil.convert(entry.getKey(), kClass),
                (V) ObjectUtil.convert(entry.getValue(), vClass)
            );
        }
        return map;
    }

    private void deLocalize(Document document, Dto dtoX, Field fieldX, String key) throws IllegalAccessException {
        if (document.get(key) instanceof String) {
            fieldX.set(dtoX, document.get(key));
            return;
        }
        Map<String, String> v = (Map<String, String>) document.get(key);
        fieldX.set(dtoX, v == null ? null : CollectionUtil.getStringFromMap(v, getLocales()));
    }

    private void fetchFromCache(Document document, Dto dtoX, Field fieldX, Class<?> type) throws IllegalAccessException {
        String fk = fieldX.getAnnotation(FetchCache.class).value();
        Class<? extends Dto> rClass;
        if (fk.isEmpty()) {
            fk = fieldX.getAnnotation(FetchCache.class).field();
            rClass = fieldX.getAnnotation(FetchCache.class).dto();
        } else {
            rClass = null;
        }

        if (type == List.class || type == Set.class) {
            List<Long> ids = document.getList(StringUtil.toSnakeCase(fk), Long.class);
            if (ids == null) {
                ids = document.getList(fk, Long.class);
            }
            if (ids == null) {
                return;
            }
            ServiceDtoCache cache = Services.get(ServiceDtoCache.class);
            if (cache == null) {
                return;
            }
            List<Object> dtos = new ArrayList<>(ids.size());
            for (Long id : ids) {
                if (rClass == null) {
                    dtos.add(cache.getDto((Class<? extends Dto>) fieldX.getType(), id));
                    return;
                }

                Dto baseDto = (Dto) ObjectUtil.getInstance(fieldX.getType());
                if (baseDto == null) {
                    return;
                }
                Dto targetClass = cache.getDto(rClass, id);
                baseDto.set(targetClass, getLocales());
                dtos.add(baseDto);
            }
            fieldX.set(dtoX, dtos);
            return;
        }

        Long id = document.getLong(StringUtil.toSnakeCase(fk));
        if (id == null) {
            id = document.getLong(fk);
        }
        if (id == null) {
            return;
        }
        ServiceDtoCache cache = Services.get(ServiceDtoCache.class);
        if (cache == null) {
            return;
        }

        if (rClass == null) {
            fieldX.set(dtoX, cache.getDto((Class<? extends Dto>) fieldX.getType(), id));
            return;
        }

        Dto baseDto = (Dto) ObjectUtil.getInstance(fieldX.getType());
        if (baseDto == null) {
            return;
        }
        Dto targetClass = cache.getDto(rClass, id);
        baseDto.set(targetClass, getLocales());
        fieldX.set(dtoX, baseDto);
    }

    private void fetchFromDatabase(Document document, Dto dtoX, Field fieldX, Class<?> type) throws IllegalAccessException {
        if (type == List.class || type == Set.class) {
            List<Long> ids = document.getList(
                StringUtil.toSnakeCase(fieldX.getAnnotation(Fetch.class).value()),
                Long.class
            );
            if (ids == null) {
                ids = document.getList(fieldX.getAnnotation(Fetch.class).value(), Long.class);
            }
            if (ids == null) {
                return;
            }
            ServiceDtoCache cache = Services.get(ServiceDtoCache.class);
            if (cache == null) {
                return;
            }
            List<Object> dtos = new ArrayList<>(ids.size());
            for (Long id : ids) {
                try {
                    dtos.add(MongoSearch.getDto((Class<? extends Dto>) fieldX.getType(), id, getLocales()));
                } catch (NoContentException ignore) {

                }
            }
            fieldX.set(dtoX, dtos);
            return;
        }

        Long id = document.getLong(StringUtil.toSnakeCase(fieldX.getAnnotation(Fetch.class).value()));
        if (id == null) {
            id = document.getLong(fieldX.getAnnotation(Fetch.class).value());
        }

        if (id == null) {
            return;
        }
        try {
            fieldX.set(dtoX, MongoSearch.getDto((Class<? extends Dto>) fieldX.getType(), id, getLocales()));
        } catch (NoContentException e) {
            fieldX.set(dtoX, null);
        }
    }
}