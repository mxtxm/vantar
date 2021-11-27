package com.vantar.database.nosql.mongo;

import com.mongodb.MongoCommandException;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.vantar.common.VantarParam;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.*;
import com.vantar.exception.DatabaseException;
import com.vantar.locale.VantarKey;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import org.bson.Document;
import org.slf4j.*;
import java.util.*;


public class Mongo {

    protected static final Logger log = LoggerFactory.getLogger(Mongo.class);
    public static final String ID = "_id";
    public final static String LOGICAL_DELETE_FIELD = "is_deleted";
    public final static Object LOGICAL_DELETE_VALUE = "Y";

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
            Map<String, Long> all = new HashMap<>(100);
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
                    new Document(COLLECTION_FIELD, dto.getStorage()),
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
            try {
                MongoCursor<Document> it = MongoConnection.getDatabase().getCollection(dto.getStorage())
                    .find().sort(new Document(ID, -1)).limit(1).iterator();
                if (!it.hasNext()) {
                    return 0;
                }
                long max = ((Number) it.next().get(ID)).longValue();

                getNextValue(dto.getStorage(), max + 1);

                return max;
            } catch (Exception e) {
                log.error("! reset seq", e);
                throw new DatabaseException(e);
            }
        }

        public static synchronized void set(String sequenceName, long value) throws DatabaseException {
            getNextValue(sequenceName, value);
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

                long id = item == null ? 0L : ObjectUtil.toLong(item.get(ID));

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
    }

    /* SEQUENCE < < < */


    /**
     * INDEX > > >
     * "col1:1,col2:-1"
     */
    public static class Index {

        public static void create(Dto dto) throws DatabaseException {
//            CreateCollectionOptions options = new CreateCollectionOptions();
//            options.collation(Collation.builder().locale("en_US").collationStrength(CollationStrength.SECONDARY).build());
//            try {
//                MongoConnection.getDatabase().createCollection(dto.getStorage(), options);
//            } catch (MongoCommandException e) {
//                log.error("! index error {} {}", dto.getClass(), dto, e);
//                throw new DatabaseException(e);
//            }

            for (String item : dto.getIndexes()) {
                Document indexes = new Document();
                for (String item2 : StringUtil.split(item, VantarParam.SEPARATOR_COMMON)) {
                    String[] parts = StringUtil.split(item2, VantarParam.SEPARATOR_KEY_VAL);
                    if (parts.length == 1) {
                        indexes.append(StringUtil.toSnakeCase(parts[0]), 1);
                    } else if (parts[1].equals("-1") || parts[1].equals("1")) {
                        indexes.append(StringUtil.toSnakeCase(parts[0]), StringUtil.toInteger(parts[1]));
                    } else {
                        indexes.append(StringUtil.toSnakeCase(parts[0]), parts[1]);
                    }
                }
                try {
//                    IndexOptions option = new IndexOptions();
//                    option.collation(Collation.builder().locale("en_US").collationStrength(CollationStrength.SECONDARY).build());
                    MongoConnection.getDatabase().getCollection(dto.getStorage()).createIndex(indexes);
                } catch (MongoCommandException e) {
                    log.error("! index error {} {}", dto.getClass(), dto, e);
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

    public static long insert(Dto dto) throws DatabaseException {
        dto.setDefaultsWhenNull();
        dto.setCreateTime(true);
        dto.setUpdateTime(true);

        if (dto.getId() == null) {
            dto.setId(Sequence.getNext(dto));
        }

        if (!dto.beforeInsert()) {
            throw new DatabaseException(VantarKey.EVENT_REJECT);
        }

        try {
            MongoConnection.getDatabase().getCollection(dto.getStorage()).insertOne(MongoMapping.getFieldValues(dto, Dto.Action.INSERT));
        } catch (Exception e) {
            log.error("! insert {}", MongoMapping.getFieldValues(dto, Dto.Action.INSERT).getClass(), e);
            throw new DatabaseException(e);
        }
        return dto.getId();
    }

    public static long insert(String collectionName, Map<String, Object> data) throws DatabaseException {
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
            throw new DatabaseException(e);
        }
        return idValue;
    }

    public static void insert(List<? extends Dto> dtos) throws DatabaseException {
        if (dtos.isEmpty()) {
            return;
        }
        String collectionName = dtos.get(0).getStorage();
        List<Document> documents = new ArrayList<>();
        for (Dto dto : dtos) {
            dto.setDefaultsWhenNull();
            dto.setCreateTime(true);
            dto.setUpdateTime(true);

            if (dto.getId() == null) {
                dto.setId(Sequence.getNext(dto));
            }

            if (!dto.beforeInsert()) {
                throw new DatabaseException(VantarKey.EVENT_REJECT);
            }
            documents.add(MongoMapping.getFieldValues(dto, Dto.Action.INSERT));
        }

        try {
            InsertManyOptions options = new InsertManyOptions();
            options.bypassDocumentValidation(true);
            options.ordered(false);
            MongoConnection.getDatabase().getCollection(collectionName).insertMany(documents, options);
        } catch (Exception e) {
            log.error("! insert {}", dtos, e);
            throw new DatabaseException(e);
        }
    }

    public static void insert(String collectionName, List<Document> documents) throws DatabaseException {
        if (documents.isEmpty()) {
            return;
        }
        try {
            InsertManyOptions options = new InsertManyOptions();
            options.bypassDocumentValidation(true);
            options.ordered(false);
            MongoConnection.getDatabase().getCollection(collectionName).insertMany(documents, options);
        } catch (Exception e) {
            log.error("! insert {}", documents, e);
            throw new DatabaseException(e);
        }
    }

    /* INSERT < < < */


    /* UPDATE > > > */

    public static void update(String collection, Long id, Document update) throws DatabaseException {
        try {
            MongoConnection.getDatabase()
                .getCollection(collection)
                .updateOne(new Document(ID, id), new Document("$set", update));
        } catch (Exception e) {
            log.error("! update({}, {}, {})", collection, id, update, e);
            throw new DatabaseException(e);
        }
    }

    public static void update(String collection, Document condition, Document update) throws DatabaseException {
        UpdateOptions options = new UpdateOptions();
        options.upsert(true);

        try {
            MongoConnection.getDatabase()
                .getCollection(collection)
                .updateMany(condition,  new Document("$set", update), options);
        } catch (Exception e) {
            log.error("! update({}, {}, {})", collection, condition, update, e);
            throw new DatabaseException(e);
        }
    }

    public static void update(QueryBuilder q) throws DatabaseException {
        update(new MongoQuery(q).matches, q.getDto(), false, true);
    }

    public static void update(Dto dto, Dto condition) throws DatabaseException {
        update(MongoMapping.getFieldValues(condition, Dto.Action.GET), dto, false, true);
    }

    public static void update(Dto dto) throws DatabaseException {
        Object id = dto.getId();
        if (id != null) {
            update(new Document(ID, id), dto, false, false);
        }
    }

    public static void upsert(Dto dto) throws DatabaseException {
        Object id = dto.getId();
        if (id == null) {
            update(MongoMapping.getFieldValues(dto, Dto.Action.GET), dto, true, true);
            return;
        }
        update(new Document(ID, id), dto, true, false);
    }

    // nulls not included
    private static void update(Document condition, Dto dto, boolean upsert, boolean all) throws DatabaseException {
        dto.setCreateTime(false);
        dto.setUpdateTime(true);

        UpdateOptions options = new UpdateOptions();
        options.upsert(upsert);

        if (!dto.beforeUpdate()) {
            throw new DatabaseException(VantarKey.EVENT_REJECT);
        }
        MongoCollection<Document> collection = MongoConnection.getDatabase().getCollection(dto.getStorage());
        try {
            if (all) {
                collection.updateMany(condition, new Document("$set", MongoMapping.getFieldValues(dto, Dto.Action.UPDATE)), options);
            } else {
                collection.updateOne(condition, new Document("$set", MongoMapping.getFieldValues(dto, Dto.Action.UPDATE)), options);
            }
        } catch (Exception e) {
            log.error("! update({}, {})", condition, dto, e);
            throw new DatabaseException(e);
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

    public static long delete(QueryBuilder q) throws DatabaseException {
        Dto dto = q.getDto();
        String storage = dto.getStorage();
        if (dto.deleteLogical()) {
            update(storage, new MongoQuery(q).matches, new Document(LOGICAL_DELETE_FIELD, LOGICAL_DELETE_VALUE));
            return 1L;
        }
        return delete(storage, new MongoQuery(q).matches);
    }

    public static long delete(Dto condition) throws DatabaseException {
        Object id = condition.getId();
        Document conditionDocument = id == null ? MongoMapping.getFieldValues(condition, Dto.Action.GET) : new Document(ID, id);

        if (condition.deleteLogical()) {
            update(condition.getStorage(), conditionDocument, new Document(LOGICAL_DELETE_FIELD, LOGICAL_DELETE_VALUE));
            return 1L;
        }

        try {
            return MongoConnection.getDatabase()
                .getCollection(condition.getStorage())
                .deleteMany(conditionDocument)
                .getDeletedCount();
        } catch (Exception e) {
            log.error("! delete {}", condition, e);
            throw new DatabaseException(e);
        }
    }

    public static long delete(String collectionName, Document query) throws DatabaseException {
        try {
            return MongoConnection.getDatabase()
                .getCollection(collectionName)
                .deleteMany(query)
                .getDeletedCount();
        } catch (Exception e) {
            log.error("! delete({}, {})", collectionName, query, e);
            throw new DatabaseException(e);
        }
    }

    public static void deleteAll(Dto dto) throws DatabaseException {
        deleteAll(dto.getStorage());
    }

    public static void deleteAll(String collectionName) throws DatabaseException {
        try {
            MongoConnection.getDatabase()
                .getCollection(collectionName)
                .drop();
        } catch (Exception e) {
            log.error("! delete all {}", collectionName, e);
            throw new DatabaseException(e);
        }
    }

    public static void deleteCollection(Dto dto) throws DatabaseException {
        deleteCollection(dto.getStorage());
    }

    public static void deleteCollection(String collectionName) throws DatabaseException {
        try {
            MongoConnection.getDatabase()
                .getCollection(collectionName)
                .drop();
        } catch (Exception e) {
            log.error("! delete {}", collectionName, e);
            throw new DatabaseException(e);
        }
    }

    /* DELETE < < < */

    public static void increaseValueById(String collectionName, long idValue, List<String> fields, long value) throws DatabaseException {
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

    public static void increaseValue(Dto dto, String fieldName, long value) throws DatabaseException {
        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch();
        if (!MongoSearch.exists(q)) {
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

    public static void increaseValues(Dto dto, List<String> fields, long value) throws DatabaseException {
        QueryBuilder q = new QueryBuilder(dto);
        q.setConditionFromDtoEqualTextMatch();
        if (!MongoSearch.exists(q)) {
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




    public static void addValueToSet(String collectionName, Document condition, String fieldName, Object item)
        throws DatabaseException {

        addValuesToSet(collectionName, condition, fieldName, item, null);
    }

    public static void addValuesToSet(String collectionName, Document condition, String fieldName, Collection<?> items)
        throws DatabaseException {

        addValuesToSet(collectionName, condition, fieldName, null, items);
    }

    private static void addValuesToSet(String collectionName, Document condition, String fieldName, Object item,
        Collection<?> items) throws DatabaseException {

        UpdateOptions options = new UpdateOptions();
        options.upsert(true);
        try {
            MongoConnection.getDatabase().getCollection(collectionName).updateOne(
                condition,
                new Document("$addToSet", item == null ?
                    new Document(fieldName, new Document("$each", items)) : new Document("$addToSet", item)),
                options
            );
        } catch (Exception e) {
            log.error("! addlist({}, {}, {}, {})", collectionName, fieldName, items, condition, e);
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
    }
}