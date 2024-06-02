package com.vantar.database.nosql.mongo;

import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.vantar.common.VantarParam;
import com.vantar.database.common.*;
import com.vantar.database.datatype.Location;
import com.vantar.database.dto.Date;
import com.vantar.database.dto.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.util.collection.ExtraUtils;
import com.vantar.util.datetime.*;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import org.bson.Document;
import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings({"unchecked"})
public class MongoQueryResult extends QueryResultBase implements QueryResult, AutoCloseable {

    private final DbMongo db;
    private final Iterator<Document> iterator;
    private boolean newIteration;
    private Map<String, Dto> fetchByFkCache;

    public FindIterable<Document> cursor;
    public MongoCursor<Document> cursorA;


    public MongoQueryResult(FindIterable<Document> cursor, Dto dto, DbMongo db) {
        this.db = db;
        cursor.noCursorTimeout(true);
        this.dto = dto;
        this.cursor = cursor;
        iterator = cursor.iterator();
        fields = dto.getClass().getFields();
        exclude = dto.getExclude();
    }

    public MongoQueryResult(AggregateIterable<Document> cursor, Dto dto, DbMongo db) {
        this.db = db;
        this.dto = dto;
        this.cursorA = cursor.cursor();
        iterator = cursor.iterator();
        fields = dto.getClass().getFields();
        exclude = dto.getExclude();
    }

    public void close() {

    }

    public Object peek(String field) throws VantarException {
        try {
            Document data = cursor.first();
            if (data == null) {
                throw new NoContentException();
            }
            return data.get(field);
        } catch (MongoException e) {
            throw new ServerException(e);
        }
    }

    public boolean next() throws ServerException {
        try {
            if (iterator.hasNext()) {
                mapRecordToDto(iterator.next());
                return true;
            }
        } catch (MongoException e) {
            throw new ServerException(e);
        }
        return false;
    }

    public <T extends Dto> T first() throws VantarException {
        try {
            Document data = cursor.first();
            if (data == null) {
                throw new NoContentException();
            }
            mapRecordToDto(data);
            return (T) dto;
        } catch (MongoException e) {
            throw new ServerException(e);
        }
    }

    public Map<String, String> asKeyValue(String keyField, String valueField) throws VantarException {
        if (keyField.equals(VantarParam.ID)) {
            keyField = DbMongo.ID;
        }
        if (valueField.equals(VantarParam.ID)) {
            valueField = DbMongo.ID;
        }
        Map<String, String> result = new HashMap<>(1000, 1);
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
            throw new ServerException(e);
        }
        if (result.isEmpty()) {
            throw new NoContentException();
        }
        return result;
    }

    public Map<String, String> asKeyValue(KeyValueData definition) throws VantarException {
        String keyField = definition.getKeyField();
        String valueField = definition.getValueField();
        if (keyField.equals(VantarParam.ID)) {
            keyField = DbMongo.ID;
        }
        if (valueField.equals(VantarParam.ID)) {
            valueField = DbMongo.ID;
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
            throw new ServerException(e);
        }
        if (result.isEmpty()) {
            throw new NoContentException();
        }
        return result;
    }

    private void mapRecordToDto(Document document) {
        mapRecordToObject(document, dto, fields);
        if (event != null) {
            try {
                event.afterSetData(dto);
            } catch (VantarException ignore) {

            }
        }
    }

    private void mapRecordToObject(Document document, Dto dto, Field[] fields) {
        newIteration = true;
        long i = 0;
        for (Field field : fields) {
            String name = field.getName();
            if (DtoBase.isNotDataField(field) || (exclude != null && exclude.contains(name))) {
                continue;
            }

            if (name.equals(VantarParam.ID) && document.containsKey(DbMongo.ID)) {
                name = DbMongo.ID;
            }
            Class<?> type = field.getType();

            try {
                if (field.isAnnotationPresent(FetchCache.class)) {
                    if (fetchFromCache(document, dto, field, type)) {
                        continue;
                    }
                }
                if (field.isAnnotationPresent(Fetch.class)) {
                    fetchFromDatabase(document, dto, field, type);
                    continue;
                }
                if (field.isAnnotationPresent(FetchByFk.class)) {
                    fetchByFk(document, field);
                    continue;
                }
                if (field.isAnnotationPresent(DeLocalized.class)) {
                    deLocalize(document, dto, field, name);
                    continue;
                }
                if (field.isAnnotationPresent(StoreString.class)) {
                    String v = document.getString(name);
                    field.set(dto, v == null ? null : Json.d.fromJson(v, type));
                    continue;
                }

                if (type.isEnum()) {
                    EnumUtil.setEnumValue(document.getString(name), type, dto, field);
                    continue;
                }

                if (type == Location.class) {
                    Document point = (Document) document.get(name);
                    if (point == null) {
                        setFieldNullValue(dto, field);
                        continue;
                    }
                    List<Double> coordinates = (List<Double>) point.get(VantarParam.COORDINATE);
                    if (coordinates.size() == 2) {
                        field.set(dto, new Location(coordinates.get(0), coordinates.get(1)));
                    } else if (coordinates.size() == 3) {
                        field.set(dto, new Location(coordinates.get(0), coordinates.get(1), coordinates.get(2)));
                    }
                    continue;
                }

                if (type == List.class || type == Set.class) {
                    Class<?>[] g = ClassUtil.getGenericTypes(field);
                    if (g == null || g.length == 0) {
                        log.warn(" ! type/value miss-match ({}.{})", dto.getClass().getName(), field.getName());
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
                        || listType.getSuperclass() == Double.class
                        || listType.getSuperclass() == Float.class) {

                        try {
                            v = document.getList(name, listType);
                            field.set(dto, v != null && type == Set.class ? new HashSet<>(v) : v);
                        } catch (ClassCastException e) {
                            List<?> theList = document.getList(name, Object.class);
                            if (theList != null) {
                                List<Object> cList = new ArrayList<>(theList.size());
                                for (Object o : theList) {
                                    cList.add(ObjectUtil.convert(o, listType));
                                }
                                field.set(dto, type == Set.class ? new HashSet<>(cList) : cList);
                            }
                        } catch (Exception e) {
                            log.warn(
                                " !! can not get List<{}> from database ({}:{}) > ({}, {})\n",
                                listType.getSimpleName(), name, document.get(name), dto.getClass().getName(), dto, e
                            );
                        }

                    } else if (listType == Dto.class || listType.getSuperclass() == DtoBase.class) {
                        List<Document> docs;
                        try {
                            docs = document.getList(name, Document.class);
                        } catch (Exception e) {
                            docs = null;
                            log.warn(
                                " ! can not get List<{}> from database ({}:{}) > ({}, {})\n",
                                listType.getSimpleName(), name, document.get(name), dto.getClass().getName(), dto, e
                            );
                        }
                        if (docs == null) {
                            setFieldNullValue(dto, field);
                            continue;
                        }
                        Collection<Dto> coll = type == List.class ? new ArrayList<>(docs.size()) : new HashSet<>(docs.size(), 1);
                        for (Document d : docs) {
                            Dto obj = (Dto) ClassUtil.getInstance(listType);
                            if (obj == null) {
                                continue;
                            }
                            mapRecordToObject(d, obj, obj.getFields());
                            coll.add(obj);
                        }
                        field.set(dto, coll);
                    } else {
                        v = Json.d.listFromJson(Json.d.toJson(document.get(name)), listType);
                        field.set(dto, v != null && type == Set.class ? new HashSet<>(v) : v);
                    }
                    continue;
                }

                if (type == Map.class) {
                    Document v = (Document) document.get(name);
                    if (v == null) {
                        setFieldNullValue(dto, field);
                    } else {
                        Class<?>[] g = ClassUtil.getGenericTypes(field);
                        if (g == null || g.length < 2) {
                            log.warn(" ! type/value miss-match ({}.{})", dto.getClass().getName(), field.getName());
                            continue;
                        }
                        field.set(
                            dto,
                            documentToMap(
                                v,
                                g[0],
                                g[1],
                                g.length > 2 ? Arrays.stream(g, 2, g.length).toArray(Class<?>[]::new) : null
                            )
                        );
                    }
                    continue;
                }

                Object value = document.get(name);
                if (value == null) {
                    setFieldNullValue(dto, field);
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
                if (type == DateTimeRange.class) {
                    if (value instanceof Document) {
                        Object dateMin = ((Document) value).get("dateMin");
                        Object dateMax = ((Document) value).get("dateMax");
                        if (dateMax == null || dateMin == null) {
                            continue;
                        }
                        field.set(dto, new DateTimeRange(dateMin.toString(), dateMax.toString()));
                    } else {
                        field.set(dto, new DateTimeRange(value.toString()));
                    }
                    continue;
                }
                if (ClassUtil.isInstantiable(type, Dto.class)) {
                    Dto obj = (Dto) ClassUtil.getInstance(type);
                    if (obj != null) {
                        mapRecordToObject((Document) value, obj, obj.getFields());
                    }
                    field.set(dto, obj);
                    continue;
                }

                field.set(dto, value);

            } catch (Exception e) {
                log.warn(" !! ({}:{}) > ({}, {})\n", name, document.get(name), dto.getClass(), dto, e);
                try {
                    setFieldNullValue(dto, field);
                } catch (Exception ignore) {

                }
            }
        }

        dto.afterFetchData();
        dto.afterFetchData(++i);
    }

    private static void setFieldNullValue(Dto dto, Field field) throws IllegalAccessException {
        Default annotation = field.getAnnotation(Default.class);
        if (annotation == null) {
            field.set(dto, null);
            return;
        }
        field.set(dto, StringUtil.toObject(annotation.value(), field.getType()));
    }

    private <K, V> Map<K, V> documentToMap(Document document, Class<K> kClass, Class<V> vClass, Class<?>[] innerGenerics) {
        if (document == null) {
            return null;
        }

        Map<K, V> map = new HashMap<>(document.size(), 1);
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            if (ClassUtil.isInstantiable(vClass, Dto.class)) {
                V vDto = ClassUtil.getInstance(vClass);
                if (vDto == null) {
                    continue;
                }
                mapRecordToObject((Document) entry.getValue(), (Dto) vDto, ((Dto) vDto).getFields());
                map.put(
                    ObjectUtil.convert(DbMongo.escapeKeyForRead(entry.getKey()), kClass),
                    vDto
                );
                continue;
            }

            map.put(
                ObjectUtil.convert(DbMongo.escapeKeyForRead(entry.getKey()), kClass),
                ObjectUtil.convert(entry.getValue(), vClass, innerGenerics)
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
        fieldX.set(dtoX, v == null ? null : ExtraUtils.getStringFromMap(v, getLocales()));
    }

    private boolean fetchFromCache(Document document, Dto dtoX, Field fieldX, Class<?> type) throws IllegalAccessException {
        ServiceDtoCache cache;
        try {
            cache = Services.getService(ServiceDtoCache.class);
        } catch (ServiceException e) {
            log.warn(" ! cache service is off");
            return false;
        }

        String fk = fieldX.getAnnotation(FetchCache.class).value();
        Class<? extends Dto> cachedClass;
        boolean straightFromCache;
        if (fk.isEmpty()) {
            straightFromCache = false;
            FetchCache cacheInfo = fieldX.getAnnotation(FetchCache.class);
            fk = cacheInfo.field();
            cachedClass = cacheInfo.dto();
        } else {
            straightFromCache = true;
            cachedClass = (Class<? extends Dto>) fieldX.getType();
        }

        boolean isCollection = type == List.class || type == Set.class;
        if (isCollection) {
            Class<?>[] types = ClassUtil.getGenericTypes(fieldX);
            if (types == null || types.length == 0) {
                log.warn(" ! can not get generic type to fetch d={} dto={} f={} t={}", document, dtoX, fieldX, type);
                return true;
            }
            if (straightFromCache) {
                cachedClass = (Class<? extends Dto>) types[0];
            } else {
                type = types[0];
            }
        }

        if (!cachedClass.isAnnotationPresent(Cache.class)) {
            log.warn(" ! d={} f={} {}>{} is not cached"
                , dto.getClass().getSimpleName(), fieldX.getName(), dtoX.getClass().getSimpleName(), cachedClass.getSimpleName());
            return false;
        }

        if (isCollection) {
            List<Long> ids = document.getList(fk, Long.class);
            if (ids == null) {
                return true;
            }

            Collection<Object> dtos = type == List.class ? new ArrayList<>(ids.size()) : new HashSet<>(ids.size(), 1);
            for (Long id : ids) {
                Dto targetDto = cache.getDto(cachedClass, id);
                if (targetDto == null) {
                    log.warn(" ! data missing ({}, id={})", cachedClass, id);
                    continue;
                }
                if (straightFromCache) {
                    dtos.add(targetDto);
                    continue;
                }

                Dto baseDto = (Dto) ClassUtil.getInstance(type);
                if (baseDto == null) {
                    log.warn(" ! can not create object ({})", type);
                    return true;
                }

                baseDto.set(targetDto, getLocales());
                for (Field f : baseDto.getFields()) {
                    if (f.isAnnotationPresent(FetchCache.class)) {
                        String fk2 = f.getAnnotation(FetchCache.class).value();
                        if (fk2.isEmpty()) {
                            fk2 = f.getAnnotation(FetchCache.class).field();
                        }
                        Document d = new Document(fk2, targetDto.getPropertyValue(fk2));
                        fetchFromCache(d, baseDto, f, f.getType());
                    }
                    if (f.isAnnotationPresent(Fetch.class)) {
                        String fk2 = f.getAnnotation(Fetch.class).value();
                        Document d = new Document(fk2, targetDto.getPropertyValue(fk2));
                        fetchFromDatabase(d, baseDto, f, f.getType());
                    }
                }
                dtos.add(baseDto);
            }
            fieldX.set(dtoX, dtos);
            return true;
        }

        Long id = document.getLong(fk);
        if (id == null) {
            return true;
        }
        if (straightFromCache) {
            fieldX.set(dtoX, cache.getDto(cachedClass, id));
            return true;
        }

        Dto baseDto = (Dto) ClassUtil.getInstance(fieldX.getType());
        if (baseDto == null) {
            log.warn(" ! can not create object ({})", type);
            return true;
        }
        Dto targetDto = cache.getDto(cachedClass, id);

        if (targetDto == null) {
            //log.warn(" ! data missing ({}, id={})", cachedClass, id);
            return true;
        }

        baseDto.set(targetDto, getLocales());

        for (Field f : baseDto.getFields()) {
            if (f.isAnnotationPresent(FetchCache.class)) {
                String fk2 = f.getAnnotation(FetchCache.class).field();
                Document d = new Document(fk2, targetDto.getPropertyValue(fk2));
                fetchFromCache(d, baseDto, f, f.getType());
            }

            if (f.isAnnotationPresent(Fetch.class)) {
                String fk2 = f.getAnnotation(FetchCache.class).field();
                Document d = new Document(fk2, targetDto.getPropertyValue(fk2));
                fetchFromDatabase(d, baseDto, f, f.getType());
            }
        }

        fieldX.set(dtoX, baseDto);
        return true;
    }

    private void fetchFromDatabase(Document document, Dto dtoX, Field fieldX, Class<?> type) throws IllegalAccessException {
        if (type == List.class || type == Set.class) {
            Class<?>[] types = ClassUtil.getGenericTypes(fieldX);
            if (types == null || types.length == 0) {
                log.warn(" ! can not get generic type to fetch d={} dto={} f={} t={}", document, dtoX, fieldX, type);
                return;
            }
            Class<?> typeGeneric = types[0];

            List<Long> ids = document.getList(fieldX.getAnnotation(Fetch.class).value(), Long.class);
            if (ids == null) {
                return;
            }

            Collection<Object> dtos = type == List.class ? new ArrayList<>(ids.size()) : new HashSet<>(ids.size(), 1);
            for (Long id : ids) {
                Dto item = db.getDto((Class<? extends Dto>) typeGeneric, id, getLocales());
                if (item != null) {
                    dtos.add(item);
                }
            }
            fieldX.set(dtoX, dtos);
            return;
        }

        Long id = document.getLong(fieldX.getAnnotation(Fetch.class).value());
        if (id == null) {
            return;
        }

        fieldX.set(dtoX, db.getDto((Class<? extends Dto>) fieldX.getType(), id, getLocales()));
    }

    private void fetchByFk(Document document, Field field) throws IllegalAccessException {
        FetchByFk fkData = field.getAnnotation(FetchByFk.class);
        if (newIteration) {
            newIteration = false;
            Long id = document.getLong(fkData.fk());
            if (fetchByFkCache == null) {
                fetchByFkCache = new HashMap<>(4);
            }
            if (id != null) {
                Dto item = db.getDto(fkData.dto(), id, getLocales());
                if (item != null) {
                    fetchByFkCache.put(fkData.fk(), item);
                }
            }
        }

        Dto dtoX = fetchByFkCache.get(fkData.fk());
        field.set(dto, dtoX == null ? null : dtoX.getPropertyValue(fkData.field()));
    }
}