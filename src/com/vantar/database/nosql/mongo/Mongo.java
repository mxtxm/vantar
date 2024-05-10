package com.vantar.database.nosql.mongo;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.vantar.common.VantarParam;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import org.bson.Document;
import org.slf4j.*;
import java.lang.reflect.Field;
import java.util.*;


public class Mongo {

    protected static final Logger log = LoggerFactory.getLogger(Mongo.class);
    public static final String ID = "_id";


    public static synchronized void renameCollection(String from, String to) throws DatabaseException {
        try {
            MongoConnection.getDatabase().getCollection(from)
                .renameCollection(new MongoNamespace(MongoConnection.config.getMongoDatabase(), to));

            Sequence.setToMax(to);
        } catch (Exception e) {
            log.error("! rename collection", e);
            throw new DatabaseException(e);
        }
    }

//    public static synchronized void cloneCollection(String from, String to) throws DatabaseException {
//        List<Document> agg = new ArrayList<>();
//        agg.add(new Document("$out", to));
//        try {
//            MongoConnection.getDatabase().getCollection(from).aggregate(agg);
//            Sequence.setToMax(to);
//        } catch (Exception e) {
//            log.error("! clone collection", e);
//            throw new DatabaseException(e);
//        }
//    }

    /**
     *  SEQUENCE > > >
     */
    public static class Sequence {

        private static final String COUNT_FIELD = "c";
        private static final String COLLECTION_FIELD = "n";
        public static final String COLLECTION = "_sequence";

        public static final long INIT_SEQUENCE_VALUE = 1L;
        private static final long INC_VALUE = 1L;


        public static Map<String, Long> getAll() throws DatabaseException {
            Map<String, Long> all = new HashMap<>(100, 1);
            try {
                for (Document document : MongoConnection.getDatabase().getCollection(COLLECTION).find()) {
                    all.put(document.getString(COLLECTION_FIELD), document.getLong(COUNT_FIELD));
                }
            } catch (Exception e) {
                log.error("! getall seq", e);
                throw new DatabaseException(e);
            }
            return all;
        }

        public static long getNext(Dto dto) throws DatabaseException {
            return getNextValue(dto.getSequenceName(), dto.getSequenceInitValue());
        }

        public static long getNext(String sequenceName) throws DatabaseException {
            return getNextValue(sequenceName, INIT_SEQUENCE_VALUE);
        }

        /**
         * existing sequence must exists, if may not exists then use getNextValue to set value instead
         */
        public static synchronized void reset(Dto dto) throws DatabaseException {
            try {
                MongoConnection.getDatabase().getCollection(COLLECTION).findOneAndUpdate(
                    new Document(COLLECTION_FIELD, dto.getSequenceName()),
                    new Document(COUNT_FIELD, dto.getSequenceInitValue())
                );
            } catch (Exception e) {
                log.error("! reset seq", e);
                throw new DatabaseException(e);
            }
        }

        public static synchronized void reset(String sequenceName) throws DatabaseException {
            try {
                MongoConnection.getDatabase().getCollection(COLLECTION).findOneAndUpdate(
                    new Document(COLLECTION_FIELD, sequenceName),
                    new Document(COUNT_FIELD, INIT_SEQUENCE_VALUE)
                );
            } catch (Exception e) {
                log.error("! reset seq", e);
                throw new DatabaseException(e);
            }
        }

        public static synchronized long setToMax(Dto dto) throws DatabaseException {
            return setToMax(dto.getSequenceName());
        }

        public static synchronized long setToMax(String sequenceName) throws DatabaseException {
            try {
                MongoCursor<Document> it = MongoConnection.getDatabase().getCollection(sequenceName)
                    .find().sort(new Document(ID, -1)).limit(1).iterator();
                if (!it.hasNext()) {
                    return 0;
                }
                long max = ((Number) it.next().get(ID)).longValue();
                return set(sequenceName, max + 1);
            } catch (Exception e) {
                log.error("! reset seq", e);
                throw new DatabaseException(e);
            }
        }

        public static synchronized long set(String sequenceName, long value) throws DatabaseException {
            remove(sequenceName);
            return getNextValue(sequenceName, value);
        }

        public static synchronized void remove() throws DatabaseException {
            try {
                MongoConnection.getDatabase().getCollection(COLLECTION).drop();
            } catch (Exception e) {
                log.error("! remove seq", e);
                throw new DatabaseException(e);
            }
        }

        public static synchronized void remove(String sequenceName) throws DatabaseException {
            try {
                MongoConnection.getDatabase().getCollection(COLLECTION).deleteOne(new Document(COLLECTION_FIELD, sequenceName));
            } catch (Exception e) {
                log.error("! remove seq", e);
                throw new DatabaseException(e);
            }
        }

        public static synchronized long getNextValue(String sequenceName, long startValue) throws DatabaseException {
            try {
                Document document = MongoConnection.getDatabase().getCollection(COLLECTION).findOneAndUpdate(
                    new Document(COLLECTION_FIELD, sequenceName),
                    new Document("$inc", new Document(COUNT_FIELD, INC_VALUE))
                );
                if (document != null) {
                    return document.getLong(COUNT_FIELD);
                }

                Document item = MongoConnection.getDatabase().getCollection(COLLECTION).find().sort(new Document(ID, -1))
                    .limit(1).first();

                long id = item == null ? 0L : NumberUtil.toNumber(item.get(ID), Long.class);

                MongoConnection.getDatabase().getCollection(COLLECTION).insertOne(
                    new Document(ID, id + 1L)
                        .append(COLLECTION_FIELD, sequenceName)
                        .append(COUNT_FIELD, startValue + INC_VALUE)
                );

                MongoConnection.getDatabase().getCollection(COLLECTION).createIndex(new Document(COLLECTION_FIELD, 1));
                return startValue;

            } catch (Exception e) {
                log.error("! next seq", e);
                throw new DatabaseException(e);
            }
        }

        public static synchronized long getCurrentValue(String sequenceName) throws DatabaseException {
            try {
                Document item = MongoConnection.getDatabase().getCollection(COLLECTION)
                    .find(new Document(COLLECTION_FIELD, sequenceName))
                    .sort(new Document(ID, -1))
                    .limit(1).first();

                if (item != null) {
                    return item.getLong(COUNT_FIELD);
                }

            } catch (Exception e) {
                log.error("! next seq", e);
                throw new DatabaseException(e);
            }
            return 0;
        }
    }

    /* SEQUENCE < < < */


//    public static Collation getCollation() {
//        return Collation.builder()
//            .locale("en")
//            .collationStrength(CollationStrength.SECONDARY)
//            .numericOrdering(true)
//            .normalization(false)
//            .build();
//    }


    /**
     * INDEX > > >
     * "col1:1,col2:-1"
     */
    public static class Index {

        public static void create(Dto dto) throws DatabaseException {
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
                    MongoConnection.getDatabase().getCollection(dto.getStorage()).createIndex(indexes);
                } catch (Exception e) {
                    log.error("! create index error {} {}", dto.getClass(), dto, e);
                    throw new DatabaseException(e);
                }
            }
        }

        public static void remove(Dto dto) throws DatabaseException {
            MongoConnection.getDatabase().getCollection(dto.getStorage()).dropIndexes();
        }

        public static void remove(String collectionName) throws DatabaseException {
            MongoConnection.getDatabase().getCollection(collectionName).dropIndexes();
        }

        public static List<String> getIndexes(Dto dto) throws DatabaseException {
            List<String> indexes = new ArrayList<>();
            for (Document document : MongoConnection.getDatabase().getCollection(dto.getStorage()).listIndexes()) {
                indexes.add(document.toJson());
            }
            return indexes;
        }
    }

    /* INDEX < < < */


    /* INSERT > > > */

    public static long insert(Dto dto) throws VantarException {
        dto.setToDefaultsWhenNull();
        dto.setCreateTime(true);
        dto.setUpdateTime(true);

        if (dto.isAutoIncrementOnInsert() || dto.getId() == null) {
            dto.setId(Sequence.getNext(dto));
        }

        runInnerBeforeInsert(dto);
        if (!dto.beforeInsert()) {
            throw new ServerException(VantarKey.CUSTOM_EVENT_ERROR);
        }

        Document doc = MongoMapping.asDocument(dto, Dto.Action.INSERT);
        try {
            if (dto.getId() == null) {
                throw new ServerException("DTO id is missing!");
            }
            MongoConnection.getDatabase().getCollection(dto.getStorage()).insertOne(doc);
        } catch (Exception e) {
            log.error("! {}", doc, e);
            throw new ServerException(e);
        }

        dto.afterInsert();

        return dto.getId();
    }

    public static long insert(String collectionName, Map<String, Object> data) throws VantarException {
        Long idValue = (Long) data.get(ID);
        if (idValue == null) {
            idValue = Sequence.getNextValue(collectionName, Sequence.INIT_SEQUENCE_VALUE);
            data.put(ID, idValue);
        }

        try {
            MongoConnection
                .getDatabase()
                .getCollection(collectionName)
                .insertOne(MongoMapping.mapToDocument(data, Dto.Action.INSERT));
        } catch (Exception e) {
            log.error("! insert {}", data, e);
            throw new ServerException(e);
        }
        return idValue;
    }

    public static void insert(List<? extends Dto> dtos) throws VantarException {
        if (dtos.isEmpty()) {
            return;
        }
        String collectionName = dtos.get(0).getStorage();
        List<Document> documents = new ArrayList<>();
        for (Dto dto : dtos) {
            dto.setToDefaultsWhenNull();
            dto.setCreateTime(true);
            dto.setUpdateTime(true);

            if (dto.isAutoIncrementOnInsert() || dto.getId() == null) {
                dto.setId(Sequence.getNext(dto));
            }

            runInnerBeforeInsert(dto);
            if (!dto.beforeInsert()) {
                throw new DatabaseException(VantarKey.CUSTOM_EVENT_ERROR);
            }
            documents.add(MongoMapping.asDocument(dto, Dto.Action.INSERT));
        }

        try {
            InsertManyOptions options = new InsertManyOptions();
            options.bypassDocumentValidation(true);
            options.ordered(false);
            MongoConnection.getDatabase().getCollection(collectionName).insertMany(documents, options);
        } catch (Exception e) {
            log.error("! {}", dtos, e);
            throw new ServerException(e);
        }

        for (Dto dto : dtos) {
            dto.afterInsert();
        }
    }

    @SuppressWarnings("unchecked")
    private static void runInnerBeforeInsert(Dto dto) {
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
                    runInnerBeforeInsert(innerDto);
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
                        runInnerBeforeInsert(listDto);
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
                        runInnerBeforeInsert(listDto);
                        listDto.beforeInsert();
                    }
                }
            }
        } catch (Exception e) {
            log.error("! {}", dto);
        }
    }

    public static void insert(String collectionName, List<Document> documents) throws VantarException {
        if (documents.isEmpty()) {
            return;
        }
        documents = Escape.escapeDocuments(documents);
        try {
            InsertManyOptions options = new InsertManyOptions();
            options.bypassDocumentValidation(true);
            options.ordered(false);
            MongoConnection.getDatabase().getCollection(collectionName).insertMany(documents, options);
        } catch (Exception e) {
            log.error("! {}", documents, e);
            throw new ServerException(e);
        }
    }

    /* INSERT < < < */


    /* UPDATE > > > */

    public static void update(String collection, Long id, Document update) throws VantarException {
        try {
            MongoConnection.getDatabase()
                .getCollection(collection)
                .updateOne(new Document(ID, id), new Document("$set", update));
        } catch (Exception e) {
            log.error("! update({}, {}, {})", collection, id, update, e);
            throw new ServerException(e);
        }
    }

    public static void update(String collection, Document condition, Document update) throws VantarException {
        UpdateOptions options = new UpdateOptions();
        options.upsert(true);

        try {
            MongoConnection.getDatabase()
                .getCollection(collection)
                .updateMany(condition,  new Document("$set", update), options);
        } catch (Exception e) {
            log.error("! update({}, {}, {})", collection, condition, update, e);
            throw new ServerException(e);
        }
    }

    public static void update(QueryBuilder q) throws VantarException {
        update(new MongoQuery(q).matches, q.getDto(), true);
    }

    public static void update(Dto dto, Dto condition) throws VantarException {
        update(MongoMapping.asDocument(condition, Dto.Action.GET), dto, true);
    }

    public static void update(Dto dto) throws VantarException {
        Object id = dto.getId();
        if (id != null) {
            update(new Document(ID, id), dto, false);
        }
    }

    // nulls not included
    private static void update(Document condition, Dto dto, boolean all) throws VantarException {
        dto.setCreateTime(false);
        dto.setUpdateTime(true);

        runInnerBeforeUpdate(dto);
        if (!dto.beforeUpdate()) {
            throw new ServerException(VantarKey.CUSTOM_EVENT_ERROR);
        }

        MongoCollection<Document> collection = MongoConnection.getDatabase().getCollection(dto.getStorage());
        try {
            Document toUpdate = new Document(
                "$set",
                MongoMapping.asDocument(dto, dto.getAction(Dto.Action.UPDATE_FEW_COLS))
            );
            if (all) {
                collection.updateMany(condition, toUpdate);
            } else {
                collection.updateOne(condition, toUpdate);
            }
        } catch (Exception e) {
            log.error("! update({}, {})", condition, dto, e);
            throw new ServerException(e);
        }
        dto.afterUpdate();
    }

    @SuppressWarnings("unchecked")
    private static void runInnerBeforeUpdate(Dto dto) {
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
            log.error("! {} {}", dto.getClass(), dto.getId(), e);
        }
    }

    /* UPDATE < < < */


    /* UNSET > > > */

    public static void unset(String collection, Long id, String... fields) throws DatabaseException {
        Document fieldsToUnset = new Document();
        for (String f : fields) {
            fieldsToUnset.append(f, "");
        }

        try {
            MongoConnection.getDatabase()
                .getCollection(collection)
                .updateOne(new Document(ID, id), new Document("$unset", fieldsToUnset));
        } catch (Exception e) {
            log.error("! upset({}, {}, {})", collection, id, CollectionUtil.join(fields, ','), e);
            throw new DatabaseException(e);
        }
    }

    public static void unset(Dto dto, String... fields) throws DatabaseException {
        unset(dto.getStorage(), dto.getId(), fields);
    }

    /* UNSET < < < */


    /* DELETE > > > */

    public static long delete(QueryBuilder q) throws VantarException {
        Dto dto = q.getDto();
        if (!dto.beforeDelete()) {
            throw new ServerException(VantarKey.CUSTOM_EVENT_ERROR);
        }
        long id = delete(dto.getStorage(), new MongoQuery(q).matches);
        dto.afterDelete();
        return id;
    }

    public static long delete(Dto condition) throws VantarException {
        if (!condition.beforeDelete()) {
            throw new ServerException(VantarKey.CUSTOM_EVENT_ERROR);
        }

        Object id = condition.getId();
        Document conditionDocument = id == null ?
            MongoMapping.asDocument(condition, Dto.Action.GET) :
            new Document(ID, id);

        try {
            long count = MongoConnection.getDatabase()
                .getCollection(condition.getStorage())
                .deleteMany(conditionDocument)
                .getDeletedCount();

            condition.afterDelete();
            return count;
        } catch (Exception e) {
            log.error("! delete {}", condition, e);
            throw new ServerException(e);
        }
    }

    public static long delete(String collectionName, Document query) throws VantarException {
        try {
            return MongoConnection.getDatabase()
                .getCollection(collectionName)
                .deleteMany(query)
                .getDeletedCount();
        } catch (Exception e) {
            log.error("! delete({}, {})", collectionName, query, e);
            throw new ServerException(e);
        }
    }

    public static void deleteAll(Dto dto) throws VantarException {
        deleteAll(dto.getStorage());
    }

    public static void deleteAll(String collectionName) throws VantarException {
        try {
            MongoConnection.getDatabase()
                .getCollection(collectionName)
                .drop();
        } catch (Exception e) {
            log.error("! delete all {}", collectionName, e);
            throw new ServerException(e);
        }
    }

    public static void deleteCollection(Dto dto) throws VantarException {
        deleteCollection(dto.getStorage());
    }

    public static void deleteCollection(String collectionName) throws VantarException {
        try {
            MongoConnection.getDatabase()
                .getCollection(collectionName)
                .drop();
        } catch (Exception e) {
            log.error("! delete {}", collectionName, e);
            throw new ServerException(e);
        }
    }

    /* DELETE < < < */


    /* LIST add ITEM

        user.getStorage(),
        new Document("_id", 8496L),
        "theList", 1000L

        user.getStorage(),
        new Document("_id", 8496L).append("aDto.theIdField", "theIdFieldValue"),
        "aDto.$.theInnerList", 1900L

     > > > */

    public static void addToCollection(
        String collectionName,
        Document condition,
        boolean isArray,
        String fieldName,
        Object item,
        boolean updateMany
    ) throws ServerException {

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
                MongoConnection.getDatabase().getCollection(collectionName).updateMany(condition, update);
            } else {
                MongoConnection.getDatabase().getCollection(collectionName).updateOne(condition, update);
            }
        } catch (Exception e) {
            log.error("! {} ({}) : ({} --add item--> {})", collectionName, condition, fieldName, value, e);
            throw new ServerException(VantarKey.FAIL_UPDATE);
        }
    }

    public static void removeFromCollection(
        String collectionName,
        Document condition,
        String fieldName,
        Object value,
        boolean updateMany
    ) throws ServerException {

        Document update = new Document(value instanceof Collection ? "$pullAll" : "$pull", new Document(fieldName, value));
        try {
            if (updateMany) {
                MongoConnection.getDatabase().getCollection(collectionName).updateMany(condition, update);
            } else {
                MongoConnection.getDatabase().getCollection(collectionName).updateOne(condition, update);
            }
        } catch (Exception e) {
            log.error("! {} ({}) : ({} --remove item--> {})", collectionName, condition, fieldName, value, e);
            throw new ServerException(VantarKey.FAIL_UPDATE);
        }
    }

    /* LIST add ITEM < < < */


    /* KeyVal add ITEM > > > */

    public static void addToMap(
        String collectionName,
        Document condition,
        String fieldName,
        String key,
        Object value,
        boolean updateMany
    ) throws ServerException {

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
                MongoConnection.getDatabase().getCollection(collectionName).updateMany(condition, new Document("$set", v));
            } else {
                MongoConnection.getDatabase().getCollection(collectionName).updateOne(condition, new Document("$set", v));
            }
        } catch (Exception e) {
            log.error("! {} ({}) : ({} --add k,v--> {},{})", collectionName, condition, fieldName, key, value, e);
            throw new ServerException(VantarKey.FAIL_UPDATE);
        }
    }

    /**
     * fieldNameKey = "fieldName.key1.key2.key3"
     */
    public static void removeFromMap(
        String collectionName,
        Document condition,
        String fieldNameKey,
        boolean updateMany
    ) throws ServerException {

        Document update = new Document("$unset", new Document(fieldNameKey, ""));
        try {
            if (updateMany) {
                MongoConnection.getDatabase().getCollection(collectionName).updateMany(condition, update);
            } else {
                MongoConnection.getDatabase().getCollection(collectionName).updateOne(condition, update);
            }
        } catch (Exception e) {
            log.error("! {} ({}) : (--remove k,v--> {})", collectionName, condition, fieldNameKey, e);
            throw new ServerException(VantarKey.FAIL_UPDATE);
        }
    }

    /* KeyVal add ITEM < < < */




    public static void increaseValueById(String collectionName, long idValue, List<String> fields, long value)
        throws DatabaseException {

        Document docs = new Document();
        for (String f : fields) {
            docs.append(f, value);
        }

        UpdateOptions options = new UpdateOptions();
        options.upsert(true);
        try {
            MongoConnection.getDatabase().getCollection(collectionName).updateOne(
                new Document(Mongo.ID, idValue),
                new Document("$inc", docs),
                options
            );
        } catch (Exception e) {
            log.error("! inc({}, {}, {}, {})", collectionName, idValue, fields, value, e);
            throw new DatabaseException(e);
        }
    }

    public static void increaseValueById(String collectionName, long idValue, String fieldName, long value) throws DatabaseException {
        UpdateOptions options = new UpdateOptions();
        options.upsert(true);
        try {
            MongoConnection.getDatabase().getCollection(collectionName).updateOne(
                new Document(Mongo.ID, idValue),
                new Document("$inc", new Document(fieldName, value)),
                options
            );
        } catch (Exception e) {
            log.error("! inc({}, {}, {}, {})", collectionName, fieldName, value, idValue, e);
            throw new DatabaseException(e);
        }
    }

    public static void increaseValue(Dto dto, String fieldName, long value) throws VantarException {
        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch();
        if (!MongoQuery.exists(q)) {
            insert(dto);
        }
        increaseValue(q, fieldName, value);
    }

    public static void increaseValue(QueryBuilder q, String fieldName, long value) throws DatabaseException {
        try {
            MongoConnection.getDatabase().getCollection(q.getDto().getStorage()).updateOne(
                MongoMapping.getMongoMatches(q.condition(), q.getDto()),
                new Document("$inc", new Document(fieldName, value))
            );
        } catch (Exception e) {
            log.error("! inc(q, {}, {}, {})", q, fieldName, value, e);
            throw new DatabaseException(e);
        }
    }

    public static void increaseValues(Dto dto, List<String> fields, long value) throws VantarException {
        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch();
        if (!MongoQuery.exists(q)) {
            insert(dto);
        }
        increaseValues(q, fields, value);
    }

    public static void increaseValues(QueryBuilder q, List<String> fields, long value) throws DatabaseException {
        Document docs = new Document();
        for (String f : fields) {
            docs.append(f, value);
        }

        try {
            MongoConnection.getDatabase().getCollection(q.getDto().getStorage()).updateOne(
                MongoMapping.getMongoMatches(q.condition(), q.getDto()),
                new Document("$inc", docs)
            );
        } catch (Exception e) {
            log.error("! inc(q, {}, {}, {})", q, docs, value, e);
            throw new DatabaseException(e);
        }
    }


    public static class Escape {

        public static String key(String key) {
            return StringUtil.replace(key, new char[] {'.', '$'}, "  ");
        }

        public static String getKey(String dictionary, String key) {
            return dictionary + '.' + StringUtil.replace(key, new char[] {'.', '$'}, "  ");
        }

        public static String keyForStore(String key) {
            return StringUtil.replace(StringUtil.replace(key, '.', "{DOT}"), '$', "{DOL}");
        }

        public static String keyForView(String key) {
            return StringUtil.replace(StringUtil.replace(key, "{DOT}", "."), "{DOL}", "$");
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
                out.append(keyForStore(k), v);
            });
            return out;
        }

        private static List<?> escapeObjects(List<?> objects) {
            List<Object> out = new ArrayList<>(objects.size());
            for (Object object : objects) {
                out.add(object instanceof Document ? escapeDocument((Document) object) : object);
            }
            return out;
        }
    }








    public static boolean exists(String collection, Document document) throws DatabaseException {
        try {
            return MongoConnection.getDatabase().getCollection(collection)
                .find(document)
                .allowDiskUse(true)
                .projection(Projections.include(ID))
                .limit(1)
                .iterator()
                .hasNext();
        } catch (Exception e) {
            log.error("! exists({})", collection, e);
            throw new DatabaseException(e);
        }
    }
}