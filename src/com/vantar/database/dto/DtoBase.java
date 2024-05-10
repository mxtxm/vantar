package com.vantar.database.dto;

import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.datatype.Location;
import com.vantar.exception.DateTimeException;
import com.vantar.locale.Locale;
import com.vantar.locale.VantarKey;
import com.vantar.service.dbarchive.ServiceDbArchive;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.bool.BoolUtil;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.*;
import com.vantar.util.json.*;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.util.string.*;
import com.vantar.web.Params;
import org.slf4j.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;


public abstract class DtoBase implements Dto {

    private transient static final Logger log = LoggerFactory.getLogger(DtoBase.class);
    private transient static final long INIT_SEQUENCE_VALUE = 1L;

    private transient boolean setCreateTime;
    private transient boolean setUpdateTime;
    private transient Set<String> excludeProperties;
    private transient Set<String> nullProperties;
    private transient String bLang;
    private transient Action bAction;
    private transient boolean isAutoIncrement = true;
    private transient String colPrefix;
    private transient boolean isForList = false;


    public void autoIncrementOnInsert(boolean isAutoIncrement) {
        this.isAutoIncrement = isAutoIncrement;
    }

    public boolean isAutoIncrementOnInsert() {
        return isAutoIncrement;
    }

    public Action getAction(Action defaultAction) {
        return bAction == null ? defaultAction : bAction;
    }

    /**
     * For admin list, respects NoList
     */
    public void setIsForList(boolean isForList) {
        this.isForList = isForList;
    }

    /**
     * if Archive      ---> get storage from ServiceDbArchive
     * if Storage      ---> storage = Storage.value
     * if is sub-class ---> storage = getStorage(ParentClass)
     * else            ---> storage = ClassName
     */
    public String getStorage() {
        return getStorage(getClass());
    }

    public static String getStorage(Class<?> dtoClass) {
        if (dtoClass.isAnnotationPresent(Archive.class)) {
            return ServiceDbArchive.getStorage(dtoClass);
        }
        Storage storage = dtoClass.getAnnotation(Storage.class);
        if (storage != null) {
            return storage.value();
        }
        Class<?> upperClass = dtoClass.getDeclaringClass();
        return upperClass == null ? dtoClass.getSimpleName() : getStorage(upperClass);
    }

    public void setId(Long id) {
        try {
            getField(ID).set(this, id);
        } catch (IllegalAccessException ignore) {

        }
    }

    public Long getId() {
        return (Long) getPropertyValue(ID);
    }

    public void setLang(String lang) {
        this.bLang = lang;
    }

    public String getLang() {
        return bLang == null ? (String) getPropertyValue("lang") : bLang;
    }

    public void setCreateTime(boolean setCreateTime) {
        this.setCreateTime = setCreateTime;
    }

    public void setUpdateTime(boolean setUpdateTime) {
        this.setUpdateTime = setUpdateTime;
    }

    public void setExclude(String... exclude) {
        if (exclude == null) {
            excludeProperties = null;
        } else {
            excludeProperties = new HashSet<>(exclude.length * 2, 1);
            excludeProperties.addAll(Arrays.asList(exclude));
        }
    }

    public void setInclude(String... include) {
        excludeProperties = new HashSet<>(include.length * 2, 1);
        excludeProperties.addAll(Arrays.asList(getPropertiesEx(include)));
    }

    public void addExclude(String... exclude) {
        if (excludeProperties == null) {
            excludeProperties = new HashSet<>(exclude.length * 2, 1);
        }
        excludeProperties.addAll(Arrays.asList(exclude));
    }

    public Set<String> getExclude() {
        return excludeProperties;
    }

    public boolean isExcluded(String name) {
        return excludeProperties != null && excludeProperties.contains(name);
    }

    public void addNullProperties(String... nullProperties) {
        if (this.nullProperties == null) {
            this.nullProperties = new HashSet<>(nullProperties.length * 2, 1);
        }
        this.nullProperties.addAll(Arrays.asList(nullProperties));
        for (String p : nullProperties) {
            setPropertyValue(p, null);
        }
    }

    public void setNullProperties(String... nullProperties) {
        if (nullProperties == null) {
            this.nullProperties = null;
        } else {
            this.nullProperties = new HashSet<>(nullProperties.length * 2, 1);
            this.nullProperties.addAll(Arrays.asList(nullProperties));
            for (String p : nullProperties) {
                setPropertyValue(p, null);
            }
        }
    }

    public void removeNullProperties(String... properties) {
        if (nullProperties == null) {
            return;
        }
        for (String p : properties) {
            nullProperties.remove(p);
        }
    }

    public void removeNullPropertiesNatural() {
        if (nullProperties == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : getPropertyValues().entrySet()) {
            if (entry.getValue() != null) {
                nullProperties.remove(entry.getKey());
            }
        }
    }

    public void addNullPropertiesNatural() {
        if (nullProperties == null) {
            nullProperties = new HashSet<>(20, 1);
        }
        for (Map.Entry<String, Object> entry : getPropertyValues().entrySet()) {
            if (entry.getValue() == null) {
                nullProperties.add(entry.getKey());
            }
        }
    }

    public Set<String> getNullProperties() {
        return nullProperties;
    }

    public boolean isNull(String name) {
        return nullProperties != null && nullProperties.contains(name);
    }

    public boolean isEmpty() {
        for (Field field : getClass().getFields()) {
            if (isDataField(field) && !ObjectUtil.isEmpty(getFieldValue(field))) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return ObjectUtil.toStringViewable(this);
    }

    public Dto getClone() {
        Jackson jackson = Json.getWithPrivate();
        return jackson.fromJson(jackson.toJson(this), this.getClass());
    }

    public boolean contains(String propertyName) {
        return Arrays.stream(this.getClass().getFields()).anyMatch(f -> f.getName().equals(propertyName));
    }

    public void reset() {
        setCreateTime = false;
        setUpdateTime = false;
        excludeProperties = null;
        nullProperties = null;
        for (Field field : getClass().getFields()) {
            if (isDataField(field)) {
                setFieldValue(field, null);
            }
        }
    }

    // > > > annotation

    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return this.getClass().isAnnotationPresent(annotation);
    }

    public boolean hasAnnotation(String property, Class<? extends Annotation> annotation) {
        Field field = getField(property);
        return field != null && field.isAnnotationPresent(annotation);
    }

    public <T extends Annotation> T getAnnotation(String property, Class<T> annotation) {
        Field field = getField(property);
        return field == null ? null : field.getAnnotation(annotation);
    }

    public List<String> annotatedProperties(Class<? extends Annotation> annotation) {
        Field[] f = getClass().getFields();
        List<String> properties = new ArrayList<>(f.length);
        for (Field field : f) {
            if (field.isAnnotationPresent(annotation)) {
                properties.add(field.getName());
            }
        }
        return properties;
    }

    public List<Field> annotatedFields(Class<? extends Annotation> annotation) {
        Field[] f = getClass().getFields();
        List<Field> fields = new ArrayList<>(f.length);
        for (Field field : f) {
            if (field.isAnnotationPresent(annotation)) {
                fields.add(field);
            }
        }
        return fields;
    }

    public void setToDefaults() {
        setDefaults(false);
    }

    public void setToDefaultsWhenNull() {
        setDefaults(true);
    }

    private void setDefaults(boolean whenNull) {
        for (Field field : getClass().getFields()) {
            if (!field.isAnnotationPresent(Default.class) || isNotDataField(field) || (whenNull && getFieldValue(field) != null)) {
                continue;
            }
            setFieldValue(field, getDefaultValue(field));
        }
    }

    public Object getDefaultValue(String name) {
        return getDefaultValue(getField(name));
    }

    private Object getDefaultValue(Field field) {
        if (field == null) {
            return null;
        }
        Default annotation = field.getAnnotation(Default.class);
        if (annotation == null) {
            return null;
        }
        return StringUtil.toObject(annotation.value(), field.getType());
    }

    public String getSequenceName() {
        try {
            Field field = getClass().getField(ID);
            if (field.isAnnotationPresent(Sequence.class)) {
                return field.getAnnotation(Sequence.class).value();
            }
        } catch (NoSuchFieldException ignore) {

        }
        return getStorage();
    }

    public long getSequenceInitValue() {
        try {
            Field field = getClass().getField(ID);
            if (field.isAnnotationPresent(InitValue.class)) {
                return Long.parseLong(field.getAnnotation(InitValue.class).value());
            }
        } catch (NoSuchFieldException ignore) {

        }
        return INIT_SEQUENCE_VALUE;
    }

    public String[] getIndexes() {
        return getClass().isAnnotationPresent(Index.class) ? getClass().getAnnotation(Index.class).value() : new String[]{};
    }

    // > > > field and property

    public Field getField(String name) {
        try {
            return getClass().getField(name.trim());
        } catch (NoSuchFieldException x) {
            try {
                return getClass().getField(name.trim());
            } catch (NoSuchFieldException e) {
                return null;
            }
        }
    }

    public Field[] getFields() {
        Field[] f = getClass().getFields();
        List<Field> fields = new ArrayList<>(f.length);
        for (Field field : f) {
            if (isDataField(field)) {
                fields.add(field);
            }
        }
        return fields.toArray(new Field[0]);
    }

    public String[] getFieldNamesForQuery() {
        Field[] f = getClass().getFields();
        Set<String> fields = new HashSet<>(f.length, 1);
        for (Field field : f) {
            if (!isDataField(field)) {
                continue;
            }
            if (isForList && field.isAnnotationPresent(NoList.class)) {
                continue;
            }

            FetchCache fetchCache = field.getAnnotation(FetchCache.class);
            if (fetchCache != null) {
                String v = fetchCache.field();
                fields.add(StringUtil.isEmpty(v) ? fetchCache.value() : v);
                continue;
            }

            Fetch fetch = field.getAnnotation(Fetch.class);
            if (fetch != null) {
                fields.add(fetch.value());
                continue;
            }

            FetchByFk fetchByFk = field.getAnnotation(FetchByFk.class);
            if (fetchByFk != null) {
                fields.add(fetchByFk.fk());
                continue;
            }

            fields.add(field.getName());
        }
        return fields.toArray(new String[0]);
    }

    public Object getPropertyValue(String name) {
        Field field = getField(name);
        return field == null ? null : getFieldValue(field);
    }

    public List<String> getPresentationPropertyNames() {
        Field[] f = getClass().getFields();
        List<String> propertyNames = new ArrayList<>(f.length);
        for (Field field : f) {
            if (field.isAnnotationPresent(Present.class)) {
                propertyNames.add(field.getName());
            }
        }
        if (!propertyNames.isEmpty()) {
            return propertyNames;
        }
        for (Field field : getClass().getFields()) {
            if (field.getName().equals("name")) {
                propertyNames.add("name");
                return propertyNames;
            }
        }
        for (Field field : getClass().getFields()) {
            if (field.getName().equals("title")) {
                propertyNames.add("title");
                return propertyNames;
            }
        }
        for (Field field : getClass().getFields()) {
            if (field.getName().equals("code")) {
                propertyNames.add("code");
                return propertyNames;
            }
        }
        for (Field field : getClass().getFields()) {
            if (field.getName().equals("value")) {
                propertyNames.add("value");
                return propertyNames;
            }
        }
        propertyNames.add(VantarParam.ID);
        return propertyNames;
    }

    public String getPresentationValue() {
        return getPresentationValue("\n");
    }

    @SuppressWarnings("unchecked")
    public String getPresentationValue(String separator) {
        StringBuilder sb = new StringBuilder();
        for (String propertyName : getPresentationPropertyNames()) {
            Field field = getField(propertyName);
            Object value = getFieldValue(field);
            sb  .append(
                    value == null ?
                        "" :
                        (
                            field.isAnnotationPresent(Localized.class) && bLang != null ?
                                ((Map<String, String>) value).get(bLang) :
                                value.toString()
                        )
                )
                .append(separator);
        }
        sb.setLength(sb.length() - separator.length());
        return sb.toString();
    }

    public Class<?> getPropertyType(String name) {
        Field field = getField(name);
        return field == null ? null : field.getType();
    }

    public Class<?> getPropertyType(Field field) {
        return field == null ? null : field.getType();
    }

    public Class<?>[] getPropertyGenericTypes(String name) {
        return ClassUtil.getGenericTypes(getField(name));
    }

    public Class<?>[] getPropertyGenericTypes(Field field) {
        return ClassUtil.getGenericTypes(field);
    }

    public Map<String, Class<?>> getPropertyTypes() {
        Field[] f = getClass().getFields();
        Map<String, Class<?>> types = new LinkedHashMap<>(f.length, 1);
        for (Field field : f) {
            if (isDataField(field)) {
                types.put(field.getName(), field.getType());
            }
        }
        return types;
    }

    public String[] getProperties() {
        Field[] f = getClass().getFields();
        List<String> properties = new ArrayList<>(f.length);
        for (Field field : f) {
            if (isDataField(field)) {
                properties.add(field.getName());
            }
        }
        return properties.toArray(new String[0]);
    }

    public String[] getPropertiesEx(String... exclude) {
        Field[] f = getClass().getFields();
        List<String> properties = new ArrayList<>(f.length);
        for (Field field : f) {
            if (isDataField(field) && !CollectionUtil.contains(exclude, field.getName())) {
                properties.add(field.getName());
            }
        }
        return properties.toArray(new String[0]);
    }

    public String[] getPropertiesInc(String... include) {
        Field[] f = getClass().getFields();
        List<String> properties = new ArrayList<>(f.length);
        for (Field field : f) {
            if (isDataField(field) && CollectionUtil.contains(include, field.getName())) {
                properties.add(field.getName());
            }
        }
        return properties.toArray(new String[0]);
    }

    /**
     * {camelCasePropertyName: value}
     * ignores final and static and annotations
     */
    public Map<String, Object> getPropertyValues(String... include) {
        return getPropertyValues(false, false, null, include);
    }

    public Map<String, Object> getPropertyValuesIncludeNulls(String... include) {
        return getPropertyValues(true, false, null, include);
    }

    public Map<String, Object> getPropertyValues(
        boolean includeNulls,
        boolean snakeCase,
        Map<String, String> propertyNameMap,
        String... include) {

        Set<String> includeSet;
        if (include.length == 0) {
            includeSet = null;
        } else {
            includeSet = new HashSet<>(include.length, 1);
            for (String includeProperty : include) {
                String[] split = StringUtil.splitTrim(includeProperty, ':');
                includeSet.add(split[0]);
                if (split.length == 2) {
                    if (propertyNameMap == null) {
                        propertyNameMap = new HashMap<>();
                    }
                    propertyNameMap.put(split[0], split[1]);
                }
            }
        }

        Field[] f = getClass().getFields();
        Map<String, Object> properties = new LinkedHashMap<>(f.length);
        for (Field field : f) {
            if (isNotDataField(field)) {
                continue;
            }
            String fieldName = field.getName();
            if (includeSet != null && !includeSet.contains(fieldName)) {
                continue;
            }

            if (propertyNameMap != null && propertyNameMap.containsKey(fieldName)) {
                fieldName = propertyNameMap.get(fieldName);
            } else if (snakeCase) {
                fieldName = field.getName();
            }

            Object value = getFieldValue(field);
            if (value == null) {
                if (includeNulls) {
                    properties.put(fieldName, null);
                }
            } else {
                if (field.isAnnotationPresent(Timestamp.class)) {
                    ((DateTime) value).setType(DateTime.TIMESTAMP);
                } else if (field.isAnnotationPresent(Date.class)) {
                    ((DateTime) value).setType(DateTime.DATE);
                } else if (field.isAnnotationPresent(Time.class)) {
                    ((DateTime) value).setType(DateTime.TIME);
                }
                properties.put(fieldName, value);
            }
        }

        return properties;
    }

    public List<StorableData> getStorableData() {
        Field[] f = getClass().getFields();
        List<StorableData> data = new ArrayList<>(f.length);
        for (Field field : f) {
            if (isNotDataField(field)) {
                continue;
            }
            String name = field.getName();
            if (isExcluded(name)) {
                continue;
            }

            boolean isNull = isNull(name);
            Object value = getFieldValue(field);
            Class<?> type = field.getType();

            if (ObjectUtil.isEmpty(value)) {
                if ((setCreateTime && field.isAnnotationPresent(CreateTime.class))
                    || (setUpdateTime && field.isAnnotationPresent(UpdateTime.class))) {

                    isNull = false;
                    value = new DateTime();
                } else {
                    value = null;
                }
            } else if (field.isAnnotationPresent(StoreString.class)) {
                type = String.class;
                value = Json.d.toJson(value);
            } else if (type.isEnum()) {
                type = String.class;
                value = ((Enum<?>) value).name();
            } else if (field.isAnnotationPresent(DataType.class)) {
                type = String.class;
                String dataType = field.getAnnotation(DataType.class).value();
                if (dataType.equals("keyword")) {
                    value = SearchUtil.normalizeKeywords(value.toString());
                } else if (dataType.equals("text")) {
                    value = SearchUtil.normalizeFullText(value.toString(), getLang());
                }
            }

            data.add(new StorableData(name, type, value, isNull, field.getAnnotations()));
        }
        return data;
    }

    // todo
    @SuppressWarnings({"unchecked"})
    public List<ManyToManyDefinition> getManyToManyFieldValues(long id) {
        String dtoFk = StringUtil.toSnakeCase(getStorage()) + "_id";
        List<ManyToManyDefinition> params = null;
        for (Field field : getClass().getFields()) {
            if (isNotDataField(field) || !field.isAnnotationPresent(ManyToManyStore.class)) {
                continue;
            }

            String[] storage = StringUtil.splitTrim(field.getAnnotation(ManyToManyStore.class).value(), VantarParam.SEPARATOR_NEXT);
            String fieldName = StringUtil.toSnakeCase(field.getName());

            ManyToManyDefinition manyToManyDefinition = new ManyToManyDefinition();
            manyToManyDefinition.storage = storage[0];
            manyToManyDefinition.fkLeft = dtoFk;
            manyToManyDefinition.fkRight = fieldName;
            manyToManyDefinition.fkLeftValue = id;
            manyToManyDefinition.fkRightValue = (List<Long>) getFieldValue(field);

            if (params == null) {
                params = new ArrayList<>();
            }
            params.add(manyToManyDefinition);
        }
        return params;
    }

    // > > > set

    private Object valueOrNull(Object value) {
        return ObjectUtil.isEmpty(value) ? null : value;
    }

    /**
     * can do:
     * sets include values from dto to current object
     * 1. null values are set
     * 2. nullProperties is ignored
     * 3. excludeProperties is ignored
     * 4. stops if exception
     */
    public void simpleSet(Dto dto, String... include) {
        for (Field field : getClass().getFields()) {
            if (isNotDataField(field)) {
                continue;
            }
            String name = field.getName();
            if (CollectionUtil.contains(include, name)) {
                setFieldValue(field, dto.getPropertyValue(name));
            }
        }
    }

    /**
     * 1. dto.null are ignored (Action != SET_STRICT)
     * 2. dto.null are set (Action == SET_STRICT)
     * 3. dto.nullProperties are added to current nullProperties
     * 4. dto.excludeProperties and excludeProperties are ignored
     * 5. if field type is dto and is not null, then set will be performed recursively
     * 6. default action is "SET"
     * 7. locales --> for DeLocalized fields set to first found locale from locales list
     */
    public boolean set(Dto dto, String... locales) {
        return set(dto, Action.SET, locales);
    }

    @SuppressWarnings({"unchecked"})
    public boolean set(Dto dto, Action action, String... locales) {
        if (dto == null) {
            return false;
        }

        if (dto.getClass().equals(getClass())) {
            for (Field field : getClass().getFields()) {
                if (isNotDataField(field)) {
                    continue;
                }
                String name = field.getName();
                if (isExcluded(name) || dto.isExcluded(name)) {
                    continue;
                }
                if (isNull(name) || dto.isNull(name)) {
                    addNullProperties(name);
                    setFieldValue(field, null);
                    continue;
                }

                Object value;
                try {
                    value = field.get(dto);
                } catch (IllegalAccessException ignore) {
                    continue;
                }
                if (value == null) {
                    if (action.equals(Action.SET_STRICT)) {
                        setFieldValue(field, null);
                    }
                    continue;
                }
                if (value instanceof Dto) {
                    Dto currentValue = (Dto) getFieldValue(field);
                    if (currentValue != null) {
                        currentValue.set((Dto) value, action);
                        continue;
                    }
                }
                setFieldValue(field, value);
           }
        }

        for (Field field : getClass().getFields()) {
            if (isNotDataField(field)) {
                continue;
            }
            String name = field.getName();
            if (isExcluded(name) || dto.isExcluded(name)) {
                continue;
            }
            if (isNull(name) || dto.isNull(name)) {
                addNullProperties(name);
                setFieldValue(field, null);
                continue;
            }

            Object value = dto.getPropertyValue(name);
            if (value == null) {
                if (action.equals(Action.SET_STRICT)) {
                    setFieldValue(field, null);
                }
                continue;
            }
            if (value instanceof Dto) {
                Dto currentValue = (Dto) getFieldValue(field);
                if (currentValue != null) {
                    currentValue.set((Dto) value, action);
                    continue;
                }
            }

            if (field.isAnnotationPresent(DeLocalized.class)) {
                if (!(value instanceof Map)) {
                    ServiceLog.log.error("! invalid @DeLocalized expected(Map<String, String>) actual({}.{} = {})"
                        , dto.getClass().getSimpleName(), name, value);
                    continue;
                }
                if (locales == null || locales.length == 0) {
                    ServiceLog.log.error("! invalid @DeLocalized (no locales provided) ({}.{} = {})"
                        , dto.getClass().getName(), name, value);
                    continue;
                }
                Map<String, String> localedValues = (Map<String, String>) value;
                for (String locale : locales) {
                    value = localedValues.get(locale);
                    if (value != null) {
                        break;
                    }
                }
                if (value == null) {
                    continue;
                }
            }

            if (ClassUtil.implementsInterface(value.getClass(), field.getType())) {
                setFieldValue(field, value);
            } else {
                ServiceLog.log.warn("! type mismatch {}.{} : {}({})-->({})"
                    , getClass().getName(), name, value, value.getClass(), field.getType());
                try {
                    field.set(this, value);
                } catch (Exception ignore) {

                }
            }
        }
        return true;
    }

    public boolean set(Map<String, Object> map, Action action) {
        return set(map, action, null, null);
    }

    @SuppressWarnings({"unchecked"})
    public boolean set(Map<String, Object> map, Action action, String prefix, String suffix) {
        if (map == null) {
            return false;
        }

        Set<String> nulls = null;
        Object o = map.get(VantarParam.NULL_PROPERTIES);
        if (o != null) {
            if (o.getClass().isArray()) {
                nulls = new HashSet<>(Arrays.asList((String[]) o));
            } else if (o instanceof Collection<?>) {
                nulls = new HashSet<>((Collection<String>) o);
            } else if (o instanceof String) {
                nulls = StringUtil.splitToSetTrim((String) o, ',');
            }
        }

        Set<String> excludes = null;
        o = map.get(VantarParam.EXCLUDE_PROPERTIES);
        if (o != null) {
            if (o.getClass().isArray()) {
                excludes = new HashSet<>(Arrays.asList((String[]) o));
            } else if (o instanceof Collection<?>) {
                excludes = new HashSet<>((Collection<String>) o);
            } else if (o instanceof String) {
                excludes = StringUtil.splitToSetTrim((String) o, ',');
            }
        }

        setDtoSetConfigs(excludes, nulls, (String) map.get(VantarParam.SET_ACTION), action);
        return setX(map, action, prefix, suffix);
    }

    public boolean set(String json, Action action) {
        return set(json, action, null, null);
    }

    @SuppressWarnings("unchecked")
    public boolean set(String json, Action action, String prefix, String suffix) {
        if (StringUtil.isEmpty(json)) {
            return false;
        }
        json = json.trim();
        if (!json.startsWith("{") && !json.endsWith("}")) {
            ServiceLog.log.error("! {} invalid JSON={}", getClass().getName(), json);
            return false;
        }

        Dto dto = Json.d.fromJsonSilent(json, getClass());
        if (dto != null) {
            DtoSetConfigs dtoSetConfigs = Json.d.fromJson(json, DtoSetConfigs.class);
            if (dtoSetConfigs != null) {
                setDtoSetConfigs(
                    dtoSetConfigs.__excludeProperties,
                    dtoSetConfigs.__nullProperties,
                    dtoSetConfigs.__action,
                    action
                );
            }
            return set(dto, action);
        }

        Map<String, Object> map = Json.d.mapFromJson(json, String.class, Object.class);
        if (map == null) {
            return false;
        }
        setDtoSetConfigs(
            (Collection<String>) map.get(VantarParam.EXCLUDE_PROPERTIES),
            (Collection<String>) map.get(VantarParam.NULL_PROPERTIES),
            (String) map.get(VantarParam.SET_ACTION),
            action
        );
        return setX(map, action, prefix, suffix);
    }

    public boolean set(Params params, Action action) {
        return set(params, action, null, null);
    }

    public boolean set(Params params, Action action, String prefix, String suffix) {
        setDtoSetConfigs(
            params.getStringSet(VantarParam.EXCLUDE_PROPERTIES),
            params.getStringSet(VantarParam.NULL_PROPERTIES),
            params.getString(VantarParam.SET_ACTION),
            action
        );
        return setX(params.getAll(), action, prefix, suffix);
    }

    private boolean setX(Map<String, Object> map, Action action, String prefix, String suffix) {
        if (map == null) {
            return false;
        }

        Field[] f = getClass().getFields();
        for (Field field : f) {
            if (isNotDataField(field)) {
                continue;
            }
            String key = field.getName();
            if (isExcluded(key)) {
                continue;
            }
            if (isNull(key)) {
                setFieldValue(field, null);
                continue;
            }

            if (suffix != null) {
                key += suffix;
            }
            if (prefix != null) {
                key = prefix + key;
            }

            Object value = map.get(key);
            if (value == null) {
                if (action.equals(Action.SET_STRICT)) {
                    setFieldValue(field, null);
                }
                continue;
            }

            if (field.getType().equals(Location.class)) {
                Object v = map.get(key);
                if (v == null) {
                    value = new Location(
                        NumberUtil.toNumber(map.get(key + "_latitude"), Double.class),
                        NumberUtil.toNumber(map.get(key + "_longitude"), Double.class),
                        NumberUtil.toNumber(map.get(key + "_altitude"), Double.class),
                        ObjectUtil.toString(map.get(key + "_countryCode"))
                    );
                } else {
                    value = Location.toLocation(v);
                }
            }
            setPropertyValue(field, value, action);
        }

        afterSetData();
        return true;
    }

    private void setDtoSetConfigs(Collection<String> excludes, Collection<String> nulls, String actionString, Action defaultAction) {
        if (excludes != null) {
            if (excludeProperties == null) {
                excludeProperties = new HashSet<>(excludes.size() * 2, 1);
            }
            excludeProperties.addAll(excludes);
        }

        if (nulls != null) {
            if (nullProperties == null) {
                nullProperties = new HashSet<>(nulls.size() * 2, 1);
            }
            nullProperties.addAll(nulls);
        }

        try {
            bAction = StringUtil.isNotEmpty(actionString) ? Action.valueOf(actionString) : defaultAction;
        } catch (IllegalArgumentException e) {
            bAction = defaultAction;
        }
    }

    public boolean setPropertyValue(String name, Object value) {
        Field field = getField(name);
        return field != null && setPropertyValue(field, value, Action.SET);
    }

    public boolean setPropertyValue(String name, Object value, Action action) {
        Field field = getField(name);
        return field != null && setPropertyValue(field, value, action);
    }

    @SuppressWarnings("unchecked")
    private boolean setPropertyValue(Field field, Object value, Action action) {
        Class<?> type = field.getType();
        String name = field.getName();

        if (isNull(name)) {
            setFieldValue(field, null);
            return true;
        }

        if (value != null) {
            if (type.equals(String.class)) {
                value = ObjectUtil.toString(value);
                if (StringUtil.isEmpty((String) value)) {
                    value = null;
                }
            } else if (ClassUtil.isInstantiable(type, Number.class)) {
                value = NumberUtil.toNumber(value, type);
            } else if (type.equals(Boolean.class)) {
                value = BoolUtil.toBoolean(value);
            } else if (type.equals(Character.class)) {
                value = StringUtil.toCharacter(value);
            } else if (type.equals(DateTime.class)) {
                if (!(value instanceof DateTime)) {
                    try {
                        value = DateTime.toDateTime(value);
                    } catch (DateTimeException e) {
                        return false;
                    }
                }
            } else if (type.equals(DateTimeRange.class)) {
                if (!(value instanceof DateTimeRange)) {
                    try {
                        value = DateTimeRange.toDateTimeRange(value);
                    } catch (DateTimeException e) {
                        return false;
                    }
                }
            } else if (type.isEnum()) {
                try {
                    value = EnumUtil.getEnumValueThrow(value.toString(), type);
                } catch (IllegalArgumentException e) {
                    return false;
                }
            } else if (type.equals(Location.class)) {
                value = Location.toLocation(value);
            } else if (ClassUtil.isInstantiable(type, List.class)) {
                value = CollectionUtil.toList(value, getPropertyGenericTypes(name)[0]);
            } else if (ClassUtil.isInstantiable(type, Set.class)) {
                value = new HashSet<>(CollectionUtil.toList(value, getPropertyGenericTypes(name)[0]));
            } else if (ClassUtil.isInstantiable(type, Map.class)) {
                if (field.isAnnotationPresent(Localized.class) && value instanceof String) {
                    value = ((String) value).trim();
                    if (((String) value).startsWith("{") && ((String) value).endsWith("}")) {
                        value = Json.d.mapFromJson((String) value, String.class, String.class);
                    } else {
                        Map<String, String> v = (Map<String, String>) getFieldValue(field);
                        if (v == null) {
                            v = new HashMap<>(2, 1);
                        }
                        v.put(Locale.getSelectedLocale(), (String) value);
                        value = v;
                    }
                } else {
                    Class<?>[] types = getPropertyGenericTypes(name);
                    value = CollectionUtil.toMap(value, types[0], types[1]);
                }
            } else if (ClassUtil.isInstantiable(type, Dto.class)) {
                if  (value instanceof Dto) {
                    ((Dto) value).addNullPropertiesNatural();
                } else if (value instanceof Map) {
                    Dto innerDto = DtoDictionary.get(type).getDtoInstance();
                    innerDto.set((Map<String, Object>) value, Action.SET_STRICT);
                    value = innerDto;
                } else {
                    return false;
                }
            } else if (!(ClassUtil.isInstantiable(type, value.getClass()) || ClassUtil.isInstantiable(value.getClass(), type))) {
                return false;
            }
        }

        if (action.equals(Action.SET) && nullProperties != null) {
            nullProperties.remove(name);
        }

        if (value == null) {
            if (action.equals(Action.INSERT) || action.equals(Action.IMPORT)
                || action.equals(Action.UPDATE_ALL_COLS) || action.equals(Action.UPDATE_ALL_COLS_NO_ID)) {

                Object defaultValue = getDefaultValue(field);
                if (defaultValue == null) {
                    if (!name.equals(ID)) {
                        addNullProperties(name);
                    }
                } else {
                    setFieldValue(field, defaultValue);
                }

            } else if (action.equals(Action.SET) || action.equals(Action.SET_STRICT)) {
                setFieldValue(field, null);

            } else if (action.equals(Action.UPDATE_FEW_COLS) || action.equals(Action.UPDATE_FEW_COLS_NO_ID)) {
                if (isNull(name)) {
                    Object defaultValue = getDefaultValue(field);
                    if (defaultValue != null) {
                        setFieldValue(field, defaultValue);
                    }
                }
            }
        } else {
            setFieldValue(field, value);
        }

        return true;
    }


    // > > > validate


    public List<ValidationError> validate(Action action) {
        Field[] f = getClass().getFields();
        List<ValidationError> errors = new ArrayList<>(f.length);
        for (Field field : f) {
            if (isDataField(field)) {
                validateFieldX(field, action, errors);
            }
        }
        validateGroup(errors, action);
        return errors;
    }

    public List<ValidationError> validateProperty(String name, Action action) {
        List<ValidationError> errors = new ArrayList<>(5);
        validateFieldX(getField(name), action, errors);
        return errors;
    }

    private void validateFieldX(Field field, Action action, List<ValidationError> errors) {
        String name = colPrefix == null ? field.getName() : (colPrefix + field.getName());

        if (name.equals(ID) && (
            action.equals(Action.UPDATE_ALL_COLS) || action.equals(Action.UPDATE_FEW_COLS) ||
            action.equals(Action.UPDATE_ADD_ITEM) || action.equals(Action.UPDATE_REMOVE_ITEM) ||
            action.equals(Action.DELETE) || action.equals(Action.UN_DELETE))) {

            if (NumberUtil.isIdInvalid(getId())) {
                errors.add(new ValidationError(VantarParam.ID, VantarKey.INVALID_ID));
            }
            return;
        }

        if (action.equals(Action.GET) || action.equals(Action.DELETE) || action.equals(Action.UN_DELETE) || action.equals(Action.PURGE)) {
            return;
        }

        Object value;
        try {
            value = field.get(this);
        } catch (IllegalAccessException e) {
            errors.add(new ValidationError(name, VantarKey.ILLEGAL_FIELD));
            return;
        }
        if (ObjectUtil.isEmpty(value)) {
            value = null;
        }
        Class<?> type = field.getType();

        if (value == null) {
            if (field.isAnnotationPresent(Required.class) && !field.isAnnotationPresent(Default.class) && (
                action.equals(Action.INSERT)
                || action.equals(Action.UPDATE_ALL_COLS)
                || action.equals(Action.UPDATE_ALL_COLS_NO_ID)
                || ((action.equals(Action.UPDATE_FEW_COLS) || action.equals(Action.UPDATE_FEW_COLS_NO_ID)) && isNull(name)))) {

                errors.add(new ValidationError(name, VantarKey.REQUIRED));
            }
            return;
        }

        if (field.isAnnotationPresent(Regex.class) && !value.toString().matches(field.getAnnotation(Regex.class).value())) {
            errors.add(new ValidationError(name, VantarKey.INVALID_FORMAT));
        }

        if (field.isAnnotationPresent(Limit.class)) {
            String[] minMax = StringUtil.splitTrim(
                field.getAnnotation(Limit.class).value(),
                VantarParam.SEPARATOR_COMMON, VantarParam.SEPARATOR_KEY_VAL
            );
            if (value instanceof Number) {
                double n = ((Number) value).doubleValue();
                if (minMax.length == 2) {
                    if (n < StringUtil.toDouble(minMax[0])) {
                        errors.add(new ValidationError(name, VantarKey.MIN_EXCEED));
                    }
                    if (n > StringUtil.toDouble(minMax[1])) {
                        errors.add(new ValidationError(name, VantarKey.MAX_EXCEED));
                    }
                } else if (n > StringUtil.toDouble(minMax[0])) {
                    errors.add(new ValidationError(name, VantarKey.MAX_EXCEED));
                }
            } else if (value.toString().length() > StringUtil.toInteger(minMax[0])) {
                errors.add(new ValidationError(name, VantarKey.INVALID_LENGTH));
            }
        }

        if (type.equals(Location.class) && !((Location) value).isValid()) {
            errors.add(new ValidationError(name, VantarKey.INVALID_GEO_LOCATION));
        }

        if (value instanceof Dto) {
            ((Dto) value).setColPrefix(name + ".");
            List<ValidationError> errorsX = ((Dto) value).validate(action);
            if (ObjectUtil.isNotEmpty(errorsX)) {
                errors.addAll(errorsX);
            }
            ((Dto) value).setColPrefix(null);
        }

        if (value instanceof Collection) {
            if (!ClassUtil.implementsInterface(getPropertyGenericTypes(field)[0], Dto.class)) {
                return;
            }
            for (Dto dtoX : (Collection<? extends Dto>) value) {
                dtoX.setColPrefix(name + ".");
                List<ValidationError> errorsX = dtoX.validate(action);
                if (ObjectUtil.isNotEmpty(errorsX)) {
                    errors.addAll(errorsX);
                }
                dtoX.setColPrefix(null);
            }
        }

        if (value instanceof Map) {
            if (!ClassUtil.implementsInterface(getPropertyGenericTypes(field)[1], Dto.class)) {
                return;
            }
            for (Dto dtoX : ((Map<?, ? extends Dto>) value).values()) {
                dtoX.setColPrefix(name + ".");
                List<ValidationError> errorsX = dtoX.validate(action);
                if (ObjectUtil.isNotEmpty(errorsX)) {
                    errors.addAll(errorsX);
                }
                dtoX.setColPrefix(null);
            }
        }
    }

    private void validateGroup(List<ValidationError> errors, Action action) {
        if (!(action.equals(Action.GET) || action.equals(Action.DELETE) || action.equals(Action.UN_DELETE)
            || action.equals(Action.PURGE))) {

            if (getClass().isAnnotationPresent(RequiredGroupXor.class)) {
                for (String requiredProps : getClass().getAnnotation(RequiredGroupXor.class).value()) {
                    int notNullCount = 0;
                    for (String prop : StringUtil.splitTrim(requiredProps, ',')) {
                        Object v = getPropertyValue(prop.trim());
                        if (v == null
                            || (v instanceof String && ((String) v).isEmpty())
                            ||(v instanceof Collection && ((Collection<?>) v).isEmpty())
                            || (v instanceof Map && ((Map<?, ?>) v).isEmpty())) {

                            continue;
                        }

                        ++notNullCount;
                    }
                    if (action.equals(Action.UPDATE_FEW_COLS) || action.equals(Action.UPDATE_FEW_COLS_NO_ID)) {
                        if (notNullCount == 2) {
                            errors.add(new ValidationError(requiredProps, VantarKey.REQUIRED_XOR));
                        }
                    } else if (notNullCount != 1) {
                        errors.add(new ValidationError(requiredProps, VantarKey.REQUIRED_XOR));
                    }
                }
            }

            if (action.equals(Action.UPDATE_FEW_COLS) || action.equals(Action.UPDATE_FEW_COLS_NO_ID)) {
                return;
            }

            if (getClass().isAnnotationPresent(RequiredGroupOr.class)) {
                for (String requiredProps : getClass().getAnnotation(RequiredGroupOr.class).value()) {
                    boolean ok = false;
                    for (String prop : StringUtil.splitTrim(requiredProps, ',')) {
                        Object v = getPropertyValue(prop.trim());
                        if (v == null
                            || (v instanceof String && ((String) v).isEmpty())
                            ||(v instanceof Collection && ((Collection<?>) v).isEmpty())
                            || (v instanceof Map && ((Map<?, ?>) v).isEmpty())) {

                            continue;
                        }

                        ok = true;
                        break;
                    }
                    if (!ok) {
                        errors.add(new ValidationError(requiredProps, VantarKey.REQUIRED_OR));
                    }
                }
            }
        }

    }

    public static boolean isDataField(Field field) {
        int m = field.getModifiers();
        return !Modifier.isFinal(m) && !Modifier.isStatic(m) && !field.isAnnotationPresent(NoStore.class);
    }

    public static boolean isNotDataField(Field field) {
        int m = field.getModifiers();
        if (field.isAnnotationPresent(ManyToManyGetData.class) || field.isAnnotationPresent(ManyToManyStore.class)) {
            return false;
        }
        return Modifier.isFinal(m) || Modifier.isStatic(m) || field.isAnnotationPresent(NoStore.class);
    }

    public void setColPrefix(String colPrefix) {
        this.colPrefix = colPrefix;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }
        Dto dto = (Dto) obj;
//        Long id = this.getId();
//        if (id != null && this.getId().equals(((Dto) obj).getId())) {
//            return true;
//        }
        for (Map.Entry<String, Object> entry : getPropertyValues().entrySet()) {
            Object v1 = entry.getValue();
            Object v2 = dto.getPropertyValue(entry.getKey());
            if (v1 == null) {
                if (v2 == null) {
                    continue;
                }
                return false;
            }

            if (v1 instanceof Collection || v2 instanceof Collection) {
                if (!CollectionUtil.equalsCollection((Collection<?>) v1, (Collection<?>) v2)) {
                    return false;
                }
                continue;
            }

            if (v1 instanceof Map || v2 instanceof Map) {
                if (!CollectionUtil.equalsMap((Map<?, ?>) v1, (Map<?, ?>) v2)) {
                    return false;
                }
                continue;
            }

            if (!v1.equals(v2)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
//        Long id = getId();
//        if (id != null) {
//            return getClass().hashCode() * id.hashCode();
//        }
        int hash = getClass().hashCode();
        for (Object v : getPropertyValues().values()) {
            hash = 31 * hash + (v == null ? 0 : v.hashCode());
        }
        return hash;
    }

    private void setFieldValue(Field field, Object value) {
        try {
            field.set(this, value);
        } catch (IllegalAccessException e) {
            log.error("! {}.{}\n", getClass().getName(), field.getName(), e);
        }
    }

    private Object getFieldValue(Field field) {
        try {
            return field.get(this);
        } catch (IllegalAccessException e) {
            log.error("! {}.{}\n", getClass().getName(), field.getName(), e);
            return null;
        }
    }


    // > > > events to override

    public void afterSetData() {

    }

    public void afterFetchData() {

    }

    public void afterFetchData(long i) {

    }

    public boolean beforeInsert() {
        return true;
    }

    public boolean beforeUpdate() {
        return true;
    }

    public boolean beforeDelete() {
        return true;
    }

    public void afterInsert() {

    }

    public void afterUpdate() {

    }

    public void afterDelete() {

    }

    public void beforeJson() {

    }


    private static class DtoSetConfigs {

        public Set<String> __nullProperties;
        public Set<String> __excludeProperties;
        public String __action;

        public String toString() {
            return ObjectUtil.toStringViewable(this);
        }
    }
}