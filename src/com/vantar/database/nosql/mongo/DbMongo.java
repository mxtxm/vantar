package com.vantar.database.nosql.mongo;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import org.bson.*;
import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.*;


public class DbMongo implements Closeable {

    public static final String ID = "_id";

    protected static final String AUTO_INCREMENT_COLLECTION = "_sequence";
    private static final String AUTO_INCREMENT_FIELD_COUNT = "c";
    private static final String AUTO_INCREMENT_FIELD_COLLECTION = "n";
    private static final long AUTO_INCREMENT_INIT_VALUE = 1L;
    private static final long AUTO_INCREMENT_INC_VALUE = 1L;

    private final MongoConfig config;
    private boolean isTest = false;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private boolean isUp = false;
    private boolean isShutdown = false;

    // > > > service

    public boolean isUp() {
        return isUp;
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public void shutdown() {
        close();
    }

    // service < < <

    // > > > connection

    public DbMongo(MongoConfig config) {
        this.config = config;
        connect();
    }

    public void connect() {
        if (isShutdown) {
            return;
        }

        String db = config.getMongoDatabase();
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(config.getMongoConnectionString()))
            .build();

        try {
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase(db);
            database.runCommand(new Document("ping", new BsonInt64(1)));
            isUp = true;
            ServiceLog.log.info(" >> connected to mongo {}", db);
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), " !! connect to mongo {}", db, e);
        }
    }

    @Override
    public void close() {
        isShutdown = true;
        mongoClient.close();
        isUp = false;
        ServiceLog.log.info(" << disconnected from mongo");
    }

    // connection < < <

    // > > > database

    public void switchToTest() {
        database = mongoClient.getDatabase(config.getMongoDatabaseTest());
        isTest = true;
        ServiceLog.log.info(" >> mongo switched to test database");
    }

    public void switchToProduction() {
        database = mongoClient.getDatabase(config.getMongoDatabase());
        isTest = false;
        ServiceLog.log.info(" >> mongo switched to production database");
    }

    public boolean isTest() {
        return isTest;
    }

    public MongoDatabase getDatabase() throws VantarException {
        if (isShutdown) {
            throw new ServerException("mongo is down");
        }
        if (!isUp || mongoClient == null || database == null) {
            connect();
        }
        if (!isUp) {
            throw new ServerException("mongo is down");
        }
        return database;
    }

    public MongoIterable<String> getCollections() throws VantarException {
        return getDatabase().listCollectionNames();
    }

    public synchronized void renameCollection(String from, String to) throws VantarException {
        try {
            getDatabase().getCollection(from).renameCollection(
                new MongoNamespace(isTest ? config.getMongoDatabaseTest() : config.getMongoDatabase(), to)
            );
            autoIncrementSetToMax(to);
        } catch (Exception e) {
            throw new ServerException(e);
        }
    }

    public static List<Document> escapeDocuments(List<Document> documents) {
        List<Document> out = new ArrayList<>(documents.size());
        for (Document document : documents) {
            out.add(escapeDocument(document));
        }
        return out;
    }

    public static Document escapeDocument(Document document) {
        final Document out = new Document();
        document.forEach((k, v) -> {
            if (v instanceof List) {
                v = escapeObjects((List<?>) v);
            }
            if (v instanceof Document) {
                v = escapeDocument((Document) v);
            }
            out.append(escapeKeyForWrite(k), v);
        });
        return out;
    }

    public static String escapeKeyForWrite(String key) {
        return StringUtil.replace(StringUtil.replace(key, '.', "{DOT}"), '$', "{DOL}");
    }

    public static String escapeKeyForRead(String key) {
        return StringUtil.replace(StringUtil.replace(key, "{DOT}", "."), "{DOL}", "$");
    }

    private static List<?> escapeObjects(List<?> objects) {
        List<Object> out = new ArrayList<>(objects.size());
        for (Object object : objects) {
            out.add(object instanceof Document ? escapeDocument((Document) object) : object);
        }
        return out;
    }

    // database < < <

    // > > > auto-increment

    public Map<String, Long> autoIncrementGetAll() throws VantarException {
        Map<String, Long> all = new HashMap<>(50, 1);
        try {
            for (Document document : getDatabase().getCollection(AUTO_INCREMENT_COLLECTION).find()) {
                all.put(document.getString(AUTO_INCREMENT_FIELD_COLLECTION), document.getLong(AUTO_INCREMENT_FIELD_COUNT));
            }
        } catch (Exception e) {
            throw new ServerException(e);
        }
        return all;
    }

    public long autoIncrementGetNext(Dto dto) throws VantarException {
        return autoIncrementGetNextValue(dto.getSequenceName(), dto.getSequenceInitValue());
    }

    public long autoIncrementGetNext(String sequenceName) throws VantarException {
        return autoIncrementGetNextValue(sequenceName, AUTO_INCREMENT_INIT_VALUE);
    }

    /**
     * existing sequence must exists, if may not exists then use getNextValue to set value instead
     */
    public synchronized void autoIncrementReset(Dto dto) throws VantarException {
        try {
            getDatabase().getCollection(AUTO_INCREMENT_COLLECTION).findOneAndUpdate(
                new Document(AUTO_INCREMENT_FIELD_COLLECTION, dto.getSequenceName()),
                new Document(AUTO_INCREMENT_FIELD_COUNT, dto.getSequenceInitValue())
            );
        } catch (Exception e) {
            throw new ServerException(e);
        }
    }

    public synchronized void autoIncrementReset(String sequenceName) throws VantarException {
        try {
            getDatabase().getCollection(AUTO_INCREMENT_COLLECTION).findOneAndUpdate(
                new Document(AUTO_INCREMENT_FIELD_COLLECTION, sequenceName),
                new Document(AUTO_INCREMENT_FIELD_COUNT, AUTO_INCREMENT_INIT_VALUE)
            );
        } catch (Exception e) {
            throw new ServerException(e);
        }
    }

    public synchronized long autoIncrementSetToMax(Dto dto) throws VantarException {
        return autoIncrementSetToMax(dto.getSequenceName());
    }

    public synchronized long autoIncrementSetToMax(String sequenceName) throws VantarException {
        try {
            MongoCursor<Document> it = getDatabase().getCollection(sequenceName)
                .find().sort(new Document(ID, -1)).limit(1).iterator();
            if (!it.hasNext()) {
                return 0;
            }
            long max = ((Number) it.next().get(ID)).longValue();
            return autoIncrementSet(sequenceName, max + 1);
        } catch (Exception e) {
            throw new ServerException(e);
        }
    }

    public synchronized long autoIncrementSet(String sequenceName, long value) throws VantarException {
        autoIncrementRemove(sequenceName);
        return autoIncrementGetNextValue(sequenceName, value);
    }

    public synchronized void autoIncrementRemove() throws VantarException {
        try {
            getDatabase().getCollection(AUTO_INCREMENT_COLLECTION).drop();
        } catch (Exception e) {
            throw new ServerException(e);
        }
    }

    public synchronized void autoIncrementRemove(String sequenceName) throws VantarException {
        try {
            getDatabase().getCollection(AUTO_INCREMENT_COLLECTION)
                .deleteOne(new Document(AUTO_INCREMENT_FIELD_COLLECTION, sequenceName));
        } catch (Exception e) {
            throw new ServerException(e);
        }
    }

    public synchronized long autoIncrementGetNextValue(String sequenceName, long startValue) throws VantarException {
        try {
            Document document = getDatabase().getCollection(AUTO_INCREMENT_COLLECTION).findOneAndUpdate(
                new Document(AUTO_INCREMENT_FIELD_COLLECTION, sequenceName),
                new Document("$inc", new Document(AUTO_INCREMENT_FIELD_COUNT, AUTO_INCREMENT_INC_VALUE))
            );
            if (document != null) {
                return document.getLong(AUTO_INCREMENT_FIELD_COUNT);
            }

            Document item = getDatabase().getCollection(AUTO_INCREMENT_COLLECTION).find().sort(new Document(ID, -1))
                .limit(1).first();

            long id = item == null ? 0L : NumberUtil.toNumber(item.get(ID), Long.class);

            getDatabase().getCollection(AUTO_INCREMENT_COLLECTION).insertOne(
                new Document(ID, id + 1L)
                    .append(AUTO_INCREMENT_FIELD_COLLECTION, sequenceName)
                    .append(AUTO_INCREMENT_FIELD_COUNT, startValue + AUTO_INCREMENT_INC_VALUE)
            );

            getDatabase().getCollection(AUTO_INCREMENT_COLLECTION).createIndex(new Document(AUTO_INCREMENT_FIELD_COLLECTION, 1));
            return startValue;
        } catch (Exception e) {
            throw new ServerException(e);
        }
    }

    public synchronized long autoIncrementGetCurrentValue(String sequenceName) throws VantarException {
        try {
            Document item = getDatabase().getCollection(AUTO_INCREMENT_COLLECTION)
                .find(new Document(AUTO_INCREMENT_FIELD_COLLECTION, sequenceName))
                .sort(new Document(ID, -1))
                .limit(1).first();
            if (item != null) {
                return item.getLong(AUTO_INCREMENT_FIELD_COUNT);
            }
        } catch (Exception e) {
            throw new ServerException(e);
        }
        return 0;
    }

    // auto-increment < < <

    // > > > index

    public void indexCreate(Dto dto) throws VantarException {
        for (String item : dto.getIndexes()) {
            Document indexes = new Document();
            for (String item2 : StringUtil.splitTrim(item, VantarParam.SEPARATOR_COMMON)) {
                String[] parts = StringUtil.splitTrim(item2, VantarParam.SEPARATOR_KEY_VAL);
                if (parts.length == 1) {
                    indexes.append(parts[0], 1);
                } else if (parts[1].equals("-1") || parts[1].equals("1")) {
                    indexes.append(parts[0], StringUtil.toInteger(parts[1]));
                } else {
                    indexes.append(parts[0], parts[1]);
                }
            }
            try {
                getDatabase().getCollection(dto.getStorage()).createIndex(indexes);
            } catch (Exception e) {
                throw new ServerException(e);
            }
        }
    }

    public void indexRemove(Dto dto) throws VantarException {
        getDatabase().getCollection(dto.getStorage()).dropIndexes();
    }

    public void indexRemove(String collection) throws VantarException {
        getDatabase().getCollection(collection).dropIndexes();
    }

    public List<String> indexGetAll(Dto dto) throws VantarException {
        List<String> indexes = new ArrayList<>(14);
        for (Document document : getDatabase().getCollection(dto.getStorage()).listIndexes()) {
            indexes.add(document.toJson());
        }
        return indexes;
    }

    // index < < <

    // > > > insert

    public void insert(String collection, List<Document> documents) throws VantarException {
        if (documents.isEmpty()) {
            return;
        }
        documents = escapeDocuments(documents);
        try {
            InsertManyOptions options = new InsertManyOptions();
            options.bypassDocumentValidation(true);
            options.ordered(false);
            getDatabase().getCollection(collection).insertMany(documents, options);
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! insert {} <- {}", collection, documents, e);
            throw new ServerException(e);
        }
    }

    public long insert(Dto dto) throws VantarException {
        dto.setToDefaultsWhenNull();
        dto.setCreateTime(true);
        dto.setUpdateTime(true);

        if (dto.isAutoIncrementOnInsert() || dto.getId() == null) {
            dto.setId(autoIncrementGetNext(dto));
        }

        runInnerDtoBeforeInsert(dto);
        if (!dto.beforeInsert()) {
            throw new ServerException(VantarKey.CUSTOM_EVENT_ERROR);
        }
        if (dto.getId() == null) {
            throw new ServerException("id is missing!");
        }

        Document document = MongoMapping.asDocument(dto, Dto.Action.INSERT);
        try {
            InsertManyOptions options = new InsertManyOptions();
            options.bypassDocumentValidation(true);
            options.ordered(false);
            getDatabase().getCollection(dto.getStorage()).insertOne(document);
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! insert {} <- {}", dto.getStorage(), document, e);
            throw new ServerException(e);
        }

        dto.afterInsert();

        return dto.getId();
    }

    public void insert(List<? extends Dto> dtos) throws VantarException {
        if (dtos.isEmpty()) {
            return;
        }
        String collection = dtos.get(0).getStorage();
        List<Document> documents = new ArrayList<>(dtos.size());
        for (Dto dto : dtos) {
            dto.setToDefaultsWhenNull();
            dto.setCreateTime(true);
            dto.setUpdateTime(true);

            if (dto.isAutoIncrementOnInsert() || dto.getId() == null) {
                dto.setId(autoIncrementGetNext(dto));
            }

            runInnerDtoBeforeInsert(dto);
            if (!dto.beforeInsert()) {
                throw new ServerException(VantarKey.CUSTOM_EVENT_ERROR);
            }
            if (dto.getId() == null) {
                throw new ServerException("id is missing!");
            }

            documents.add(MongoMapping.asDocument(dto, Dto.Action.INSERT));
        }
        try {
            InsertManyOptions options = new InsertManyOptions();
            options.bypassDocumentValidation(true);
            options.ordered(false);
            getDatabase().getCollection(collection).insertMany(documents, options);
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! insert {} <- {}", collection, documents, e);
            throw new ServerException(e);
        }

        for (Dto dto : dtos) {
            dto.afterInsert();
        }
    }

    @SuppressWarnings("unchecked")
    private void runInnerDtoBeforeInsert(Dto dto) {
        try {
            for (Field field : dto.getFields()) {
                Class<?> t = field.getType();
                if (ClassUtil.implementsInterface(t, Dto.class)) {
                    Dto innerDto;
                    try {
                        innerDto = (Dto) field.get(dto);
                    } catch (IllegalAccessException e) {
                        continue;
                    }
                    if (innerDto == null) {
                        continue;
                    }
                    runInnerDtoBeforeInsert(innerDto);
                    innerDto.beforeInsert();
                } else if (ClassUtil.implementsInterface(t, Collection.class)) {
                    Class<?>[] gTypes = ClassUtil.getGenericTypes(field);
                    if (gTypes == null || gTypes.length == 0 || !ClassUtil.implementsInterface(gTypes[0], Dto.class)) {
                        continue;
                    }
                    Collection<? extends Dto> innerDtos;
                    try {
                        innerDtos = (Collection<? extends Dto>) field.get(dto);
                    } catch (IllegalAccessException e) {
                        continue;
                    }
                    if (innerDtos == null) {
                        continue;
                    }
                    for (Dto listDto : innerDtos) {
                        runInnerDtoBeforeInsert(listDto);
                        listDto.beforeInsert();
                    }
                } else if (ClassUtil.implementsInterface(t, Map.class)) {
                    Class<?>[] gTypes = ClassUtil.getGenericTypes(field);
                    if (gTypes == null || gTypes.length < 2 || !ClassUtil.implementsInterface(gTypes[1], Dto.class)) {
                        continue;
                    }
                    Map<?, ? extends Dto> innerDtos;
                    try {
                        innerDtos = (Map<?, ? extends Dto>) field.get(dto);
                    } catch (IllegalAccessException e) {
                        continue;
                    }
                    if (innerDtos == null) {
                        continue;
                    }
                    for (Dto listDto : innerDtos.values()) {
                        runInnerDtoBeforeInsert(listDto);
                        listDto.beforeInsert();
                    }
                }
            }
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! insert {}", dto, e);
        }
    }

    // insert < < <

    // > > > update

    public void update(String collection, Long id, Document update) throws VantarException {
        try {
            getDatabase()
                .getCollection(collection)
                .updateOne(new Document(ID, id), new Document("$set", update));
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! update ({}, {}) <- {}", collection, id, update, e);
            throw new ServerException(e);
        }
    }

    public void update(QueryBuilder q) throws VantarException {
        update(new MongoQuery(q).matches, q.getDto(), true);
    }

    public void update(Dto dto) throws VantarException {
        Object id = dto.getId();
        if (id != null) {
            update(new Document(ID, id), dto, false);
        }
    }

    // nulls not included
    private void update(Document condition, Dto dto, boolean all) throws VantarException {
        dto.setCreateTime(false);
        dto.setUpdateTime(true);

        runInnerBeforeUpdate(dto);
        if (!dto.beforeUpdate()) {
            throw new ServerException(VantarKey.CUSTOM_EVENT_ERROR);
        }

        MongoCollection<Document> collection = getDatabase().getCollection(dto.getStorage());
        try {
            Document data = new Document("$set", MongoMapping.asDocument(dto, dto.getAction(Dto.Action.UPDATE_FEW_COLS)));
            if (all) {
                collection.updateMany(condition, data);
            } else {
                collection.updateOne(condition, data);
            }
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! update {} <- {}", collection, dto, e);
            throw new ServerException(e);
        }
        dto.afterUpdate();
    }

    @SuppressWarnings("unchecked")
    private void runInnerBeforeUpdate(Dto dto) {
        try {
            for (Field field : dto.getFields()) {
                Class<?> t = field.getType();
                if (ClassUtil.implementsInterface(t, Dto.class)) {
                    Dto innerDto;
                    try {
                        innerDto = (Dto) field.get(dto);
                    } catch (IllegalAccessException e) {
                        continue;
                    }
                    if (innerDto == null) {
                        continue;
                    }
                    runInnerBeforeUpdate(innerDto);
                    innerDto.beforeUpdate();
                } else if (ClassUtil.implementsInterface(t, Collection.class)) {
                    Class<?>[] gTypes = ClassUtil.getGenericTypes(field);
                    if (gTypes == null || gTypes.length == 0 || !ClassUtil.implementsInterface(gTypes[0], Dto.class)) {
                        continue;
                    }
                    Collection<? extends Dto> innerDtos;
                    try {
                        innerDtos = (Collection<? extends Dto>) field.get(dto);
                    } catch (IllegalAccessException e) {
                        continue;
                    }
                    if (innerDtos == null) {
                        continue;
                    }
                    for (Dto listDto : innerDtos) {
                        runInnerBeforeUpdate(listDto);
                        listDto.beforeUpdate();
                    }
                } else if (ClassUtil.implementsInterface(t, Map.class)) {
                    Class<?>[] gTypes = ClassUtil.getGenericTypes(field);
                    if (gTypes == null || gTypes.length < 2 || !ClassUtil.implementsInterface(gTypes[1], Dto.class)) {
                        continue;
                    }
                    Map<?, ? extends Dto> innerDtos;
                    try {
                        innerDtos = (Map<?, ? extends Dto>) field.get(dto);
                    } catch (IllegalAccessException e) {
                        continue;
                    }
                    if (innerDtos == null) {
                        continue;
                    }
                    for (Dto listDto : innerDtos.values()) {
                        runInnerBeforeUpdate(listDto);
                        listDto.beforeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! update {} <- {}", dto.getClass(), dto, e);
        }
    }

    // update < < <

    // > > > delete

    public long delete(QueryBuilder q) throws VantarException {
        Dto dto = q.getDto();
        if (!dto.beforeDelete()) {
            throw new ServerException(VantarKey.CUSTOM_EVENT_ERROR);
        }
        long id = delete(dto.getStorage(), new MongoQuery(q).matches);
        dto.afterDelete();
        return id;
    }

    public long delete(Dto condition) throws VantarException {
        if (!condition.beforeDelete()) {
            throw new ServerException(VantarKey.CUSTOM_EVENT_ERROR);
        }
        Object id = condition.getId();
        Document conditionDocument = id == null ? MongoMapping.asDocument(condition, Dto.Action.GET) : new Document(ID, id);
        try {
            long count = getDatabase().getCollection(condition.getStorage()).deleteMany(conditionDocument).getDeletedCount();
            condition.afterDelete();
            return count;
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! delete {}", condition, e);
            throw new ServerException(e);
        }
    }

    public long delete(String collection, Document query) throws VantarException {
        try {
            return getDatabase().getCollection(collection).deleteMany(query).getDeletedCount();
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! delete {} <- {}", collection, query, e);
            throw new ServerException(e);
        }
    }

    public void deleteAll(Dto dto) throws VantarException {
        deleteAll(dto.getStorage());
    }

    public void deleteAll(String collection) throws VantarException {
        try {
            getDatabase().getCollection(collection).drop();
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! deleteAll {} <- {}", collection, e);
            throw new ServerException(e);
        }
    }

    // delete < < <

    /* > > > LIST add ITEM
        user.getStorage(),
        new Document("_id", 8496L),
        "theList", 1000L

        user.getStorage(),
        new Document("_id", 8496L).append("aDto.theIdField", "theIdFieldValue"),
        "aDto.$.theInnerList", 1900L
     */

    public void addToCollection(
        String collectionName,
        Document condition,
        boolean isArray,
        String fieldName,
        Object item,
        boolean updateMany
    ) throws VantarException {

        Object value = item;
        if (item instanceof Collection) {
            if (ObjectUtil.isNotEmpty(item)) {
                List<?> list = new ArrayList<>((Collection<?>) item);
                if (list.get(0) instanceof Dto) {
                    List<Document> documents = new ArrayList<>(20);
                    for (Object obj : list) {
                        documents.add(MongoMapping.asDocument((Dto) obj, Dto.Action.UPDATE_FEW_COLS));
                    }
                    value = new Document("$each", documents);
                } else {
                    value = new Document("$each", item);
                }
            }
        } else if (item instanceof Dto) {
            value = MongoMapping.asDocument((Dto) item, Dto.Action.UPDATE_FEW_COLS);
        }

        Document update = new Document(isArray ? "$push" : "$addToSet", new Document(fieldName, value));

        try {
            if (updateMany) {
                getDatabase().getCollection(collectionName).updateMany(condition, update);
            } else {
                getDatabase().getCollection(collectionName).updateOne(condition, update);
            }
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! {} ({}) : ({} --add item--> {})", collectionName, condition, fieldName, value, e);
            throw new ServerException(VantarKey.FAIL_UPDATE);
        }
    }

    public void removeFromCollection(
        String collectionName,
        Document condition,
        String fieldName,
        Object value,
        boolean updateMany
    ) throws VantarException {

        Document update = new Document(value instanceof Collection ? "$pullAll" : "$pull", new Document(fieldName, value));
        try {
            if (updateMany) {
                getDatabase().getCollection(collectionName).updateMany(condition, update);
            } else {
                getDatabase().getCollection(collectionName).updateOne(condition, update);
            }
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! {} ({}) : ({} --remove item--> {})", collectionName, condition, fieldName, value, e);
            throw new ServerException(VantarKey.FAIL_UPDATE);
        }
    }

    // LIST add ITEM < < <

    // > > > KeyVal add ITEM

    public void addToMap(
        String collectionName,
        Document condition,
        String fieldName,
        String key,
        Object value,
        boolean updateMany
    ) throws VantarException {

        Document v = new Document();
        Document x = v;
        for (String k : StringUtil.splitTrim(fieldName, '.')) {
            Document vLast = new Document();
            x.append(k, vLast);
            x = vLast;
        }

        x.append(
            key,
            value instanceof Dto ? MongoMapping.asDocument((Dto) value, Dto.Action.UPDATE_FEW_COLS) : value
        );
        try {
            if (updateMany) {
                getDatabase().getCollection(collectionName).updateMany(condition, new Document("$set", v));
            } else {
                getDatabase().getCollection(collectionName).updateOne(condition, new Document("$set", v));
            }
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! {} ({}) : ({} --add k, v--> {},{})", collectionName, condition
                , fieldName, key, value, e);
            throw new ServerException(VantarKey.FAIL_UPDATE);
        }
    }

    /**
     * fieldNameKey = "fieldName.key1.key2.key3"
     */
    public void removeFromMap(
        String collectionName,
        Document condition,
        String fieldNameKey,
        boolean updateMany
    ) throws VantarException {

        Document update = new Document("$unset", new Document(fieldNameKey, ""));
        try {
            if (updateMany) {
                getDatabase().getCollection(collectionName).updateMany(condition, update);
            } else {
                getDatabase().getCollection(collectionName).updateOne(condition, update);
            }
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! {} ({}) : (--remove k, v--> {})", collectionName, condition, fieldNameKey, e);
            throw new ServerException(VantarKey.FAIL_UPDATE);
        }
    }

    // KeyVal add ITEM < < <

    // > > > other write

    public void unset(Dto dto, String... fields) throws VantarException {
        unset(dto.getStorage(), dto.getId(), fields);
    }

    public void unset(String collection, Long id, String... fields) throws VantarException {
        Document fieldsToUnset = new Document();
        for (String f : fields) {
            fieldsToUnset.append(f, "");
        }
        try {
            getDatabase()
                .getCollection(collection)
                .updateOne(new Document(ID, id), new Document("$unset", fieldsToUnset));
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! exists ({}, {}) <- {}", collection, id, CollectionUtil.join(fields, ','), e);
            throw new ServerException(e);
        }
    }

    public void increaseValueById(String collection, long idValue, List<String> fields, long value) throws VantarException {
        Document docs = new Document();
        for (String f : fields) {
            docs.append(f, value);
        }
        UpdateOptions options = new UpdateOptions();
        options.upsert(true);
        try {
            getDatabase().getCollection(collection).updateOne(
                new Document(ID, idValue),
                new Document("$inc", docs),
                options
            );
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! inc ({}, {}, {}, {})", collection, idValue, fields, value, e);
            throw new ServerException(e);
        }
    }

    public void increaseValueById(String collection, long idValue, String fieldName, long value) throws VantarException {
        UpdateOptions options = new UpdateOptions();
        options.upsert(true);
        try {
            getDatabase().getCollection(collection).updateOne(
                new Document(ID, idValue),
                new Document("$inc", new Document(fieldName, value)),
                options
            );
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! inc ({}, {}, {}, {})", collection, idValue, fieldName, value, e);
            throw new ServerException(e);
        }
    }

    public void increaseValue(Dto dto, String fieldName, long value) throws VantarException {
        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch();
        if (!exists(q)) {
            insert(dto);
        }
        increaseValue(q, fieldName, value);
    }

    public void increaseValue(QueryBuilder q, String fieldName, long value) throws VantarException {
        try {
            getDatabase().getCollection(q.getDto().getStorage()).updateOne(
                MongoMapping.getMongoMatches(q.condition(), q.getDto(), this),
                new Document("$inc", new Document(fieldName, value))
            );
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! inc ({}, {})", fieldName, value, e);
            throw new ServerException(e);
        }
    }

    public void increaseValues(Dto dto, List<String> fields, long value) throws VantarException {
        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch();
        if (!exists(q)) {
            insert(dto);
        }
        increaseValues(q, fields, value);
    }

    public void increaseValues(QueryBuilder q, List<String> fields, long value) throws VantarException {
        Document docs = new Document();
        for (String f : fields) {
            docs.append(f, value);
        }
        try {
            getDatabase().getCollection(q.getDto().getStorage()).updateOne(
                MongoMapping.getMongoMatches(q.condition(), q.getDto(), this),
                new Document("$inc", docs)
            );
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! inc ({}, {})", docs, value, e);
            throw new ServerException(e);
        }
    }

    // other write < < <

    // > > > read

    public QueryResult getData(QueryBuilder q) throws VantarException {
        return getData(new MongoQuery(q));
    }

    public QueryResult getData(MongoQuery q) throws VantarException {
        FindIterable<Document> find = getResult(q);
        try {
            if (q.columns != null && q.columns.length > 0) {
                find.projection(Projections.fields(Projections.include(q.columns)));
            }
            return new MongoQueryResult(find, q.dto, this);
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), " ! count --> {}", q.dto, e);
            throw new ServerException(e);
        }
    }

    public FindIterable<Document> getResult(MongoQuery q) throws VantarException {
        try {
            FindIterable<Document> find = (q.matches == null || q.matches.isEmpty()) ?
                getDatabase().getCollection(q.storage).find() :
                getDatabase().getCollection(q.storage).find(q.matches);

            find.collation(
                Collation.builder()
                    .locale("fa")
                    .collationStrength(CollationStrength.PRIMARY)
                    .caseLevel(false)
                    .build()
            );

            if (q.sort != null) {
                find.sort(q.sort);
            }
            find.allowDiskUse(true);
            if (q.skip != null) {
                find.skip(q.skip);
            }
            if (q.limit != null) {
                find.limit(q.limit);
            }
            if (q.columns != null && q.columns.length > 0) {
                find.projection(Projections.fields(Projections.include(q.columns)));
            }
            return find;
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), " ! query --> {}", q.storage, e);
            throw new ServerException(e);
        }
    }

    public QueryResult getDataByAggregate(MongoQuery q) throws VantarException {
        return new MongoQueryResult(getAggregate(q), q.dto, this);
    }

    public AggregateIterable<Document> getAggregate(QueryBuilder q) throws VantarException {
        return getAggregate(new MongoQuery(q));
    }

    public AggregateIterable<Document> getAggregate(MongoQuery q) throws VantarException {
        List<Document> query = new ArrayList<>(10);
        if (q.matches != null && !q.matches.isEmpty()) {
            query.add(new Document("$match", q.matches));
        }
        if (q.groups != null && !q.groups.isEmpty()) {
            for (Document group : q.groups) {
                query.add(new Document("$group", group));
            }
        }
        if (q.union != null) {
            for (String collection : q.union) {
                query.add(new Document("$unionWith", collection));
            }
        }
        if (q.sort != null) {
            query.add(new Document("$sort", q.sort));
        }
        if (q.skip != null) {
            query.add(new Document("$skip", q.skip));
        }
        if (q.limit != null) {
            query.add(new Document("$limit", q.limit));
        }
        try {
            return getDatabase().getCollection(q.storage).aggregate(query).allowDiskUse(true);
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), " ! aggr --> {}", q.storage, e);
            throw new ServerException(e);
        }
    }

    public <T extends Dto> T getDto(Class<T> tClass, long id, String... locales) {
        T dto = ClassUtil.getInstance(tClass);
        if (dto == null) {
            return null;
        }
        try {
            MongoQueryResult result = new MongoQueryResult(
                getDatabase().getCollection(dto.getStorage()).find(new Document(DbMongo.ID, id)).allowDiskUse(true),
                dto,
                this
            );
            if (locales != null && locales.length > 0) {
                result.setLocale(locales);
            }
            return result.first();
        } catch (Exception e) {
            return null;
        }
    }

    public QueryResult getAllData(Dto dto, String... sort) throws VantarException {
        try {
            return new MongoQueryResult(
                getDatabase().getCollection(dto.getStorage()).find().sort(MongoMapping.sort(sort)).allowDiskUse(true),
                dto,
                this
            );
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), " ! query --> {}", dto.getClass().getSimpleName(), e);
            throw new ServerException(e);
        }
    }

    public PageData getPage(QueryBuilder q, QueryResultBase.Event event, String... locales) throws VantarException {
        MongoQuery mongoQuery = new MongoQuery(q);
        long total = q.getTotal();
        if (total == 0) {
            total = count(q);
        }

        QueryResult result = getData(mongoQuery);
        if (locales.length > 0) {
            result.setLocale(locales);
        }
        if (event != null) {
            result.setEvent(event);
        }

        Integer limit = q.getLimit();
        List<Dto> dtos = result.asList();
        return new PageData(
            dtos,
            q.getPageNo(),
            limit == null ? dtos.size() : limit,
            total
        );
    }

    public PageData getPageForeach(QueryBuilder q, QueryResultBase.EventForeach event, String... locales) throws VantarException {
        MongoQuery mongoQuery = new MongoQuery(q);
        long total = q.getTotal();
        if (total == 0) {
            total = count(q);
        }

        QueryResult result = getData(mongoQuery);
        if (locales.length > 0) {
            result.setLocale(locales);
        }

        Integer limit = q.getLimit();
        try {
            result.forEach(event);
        } catch (VantarException ignore) {
            // todo handle
        }

        PageData pageData = new PageData();
        pageData.page = q.getPageNo();
        pageData.length = limit == null ? 0 : limit;
        pageData.total = total;
        return pageData;
    }

    // read < < <

    // > > > other read

    /**
     * Check by property values
     * if dto.id is set, it will be taken into account thus (id, property) must be unique
     */
    public boolean isUnique(Dto dto, String... properties) throws VantarException {
        Document condition = new Document();
        for (String property : properties) {
            property = property.trim();
            if (property.equals(VantarParam.ID) || property.equals(DbMongo.ID)) {
                continue;
            }
            Object v = dto.getPropertyValue(property);
            if (properties.length == 1 && ObjectUtil.isEmpty(v)) {
                return true;
            }
            condition.append(property, v);
        }

        if (condition.isEmpty()) {
            return true;
        }

        Long id = dto.getId();
        if (id != null) {
            condition.append(DbMongo.ID, new Document("$ne", id));
        }
        try {
            return !getDatabase().getCollection(dto.getStorage())
                .find(condition)
                .allowDiskUse(true)
                .projection(Projections.include(DbMongo.ID))
                .limit(1)
                .iterator()
                .hasNext();
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! isUnique ({}, {})", dto.getClass().getSimpleName(), dto.getId(), e);
            throw new ServerException(e);
        }
    }

    public boolean existsByDto(Dto dto) throws VantarException {
        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch();
        return exists(q);
    }

    public boolean existsById(Dto dto) throws VantarException {
        return exists(dto, VantarParam.ID);
    }

    public boolean exists(Dto dto, String property) throws VantarException {
        return exists(
            DtoBase.getStorage(dto.getClass()),
            new Document(property.equals(VantarParam.ID) ? DbMongo.ID : property, dto.getPropertyValue(property))
        );
    }

    public boolean exists(String collection, Document document) throws VantarException {
        try {
            return getDatabase().getCollection(collection)
                .find(document)
                .allowDiskUse(true)
                .projection(Projections.include(ID))
                .limit(1)
                .iterator()
                .hasNext();
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! exists {} <- {}", collection, document, e);
            throw new ServerException(e);
        }
    }

    public boolean exists(QueryBuilder q) throws VantarException {
        q.limit(1);
        return count(q) > 0;
    }

    public long count(String collection) throws VantarException {
        try {
            return getDatabase().getCollection(collection).estimatedDocumentCount();
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! count <- {}", collection, e);
            throw new ServerException(e);
        }
    }

    public long count(QueryBuilder q) throws VantarException {
        return count(new MongoQuery(q));
    }

    public long count(MongoQuery q) throws VantarException {
        try {
            return q.matches == null || q.matches.isEmpty() ?
                getDatabase().getCollection(q.storage).estimatedDocumentCount() :
                getDatabase().getCollection(q.storage).countDocuments(q.matches);
        } catch (Exception e) {
            ServiceLog.error(this.getClass(), "! count <- {}", q.dto, e);
            throw new ServerException(e);
        }
    }

    public double getAverage(QueryBuilder q) throws VantarException {
        AggregateIterable<Document> documents = getAggregate(new MongoQuery(q));
        for (Document document : documents) {
            Double average = document.getDouble("average");
            if (average != null) {
                return average;
            }
        }
        return 0;
    }

    // other read < < <
}
