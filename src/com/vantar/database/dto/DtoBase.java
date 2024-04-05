package com.vantar.database.dto;

import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.datatype.Location;
import com.vantar.exception.DateTimeException;
import com.vantar.locale.Locale;
import com.vantar.locale.VantarKey;
import com.vantar.service.dbarchive.ServiceDbArchive;
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
    private transient boolean clearIdOnInsert = true;
    private transient String colPrefix;


    public void setClearIdOnInsert(boolean clearIdOnInsert) {
        this.clearIdOnInsert = clearIdOnInsert;
    }

    public boolean getClearIdOnInsert() {
        return clearIdOnInsert;
    }

    public Action getAction(Action defaultAction) {
        return bAction == null ? defaultAction : bAction;
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

    public Set<String> getNullProperties() {
        return nullProperties;
    }

    public boolean isNull(String name) {
        return nullProperties != null && nullProperties.contains(name);
    }

    public boolean isEmpty() {
        for (Field field : getClass().getFields()) {
            try {
                if (isDataField(field) && !ObjectUtil.isEmpty(field.get(this))) {
                    return false;
                }
            } catch (IllegalAccessException ignore) {

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
        try {
            for (Field field : getClass().getFields()) {
                if (isDataField(field)) {
                    field.set(this, null);
                }
            }
        } catch (IllegalAccessException e) {
            log.error(" !! ({}, {})\n", getClass().getName(), this, e);
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
        try {
            for (Field field : getClass().getFields()) {
                if (!field.isAnnotationPresent(Default.class) || isNotDataField(field) || (whenNull && field.get(this) != null)) {
                    continue;
                }
                field.set(this, getDefaultValue(field));
            }
        } catch (IllegalAccessException e) {
            log.error(" !! ({}, {})\n", getClass().getName(), this, e);
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

    public String[] getFieldNames() {
        Field[] f = getClass().getFields();
        Set<String> fields = new HashSet<>(f.length);
        for (Field field : f) {
            if (!isDataField(field)) {
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
        if (field == null) {
            return null;
        }
        try {
            return field.get(this);
        } catch (IllegalAccessException e) {
            return null;
        }
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
        try {
            for (String propertyName : getPresentationPropertyNames()) {
                Field field = getField(propertyName);
                Object value = field.get(this);
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
        } catch (IllegalAccessException e) {
            log.error(" !! ({}, {})\n", getClass().getName(), this, e);
        }
        return null;
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

            Object value;
            try {
                value = field.get(this);
            } catch (IllegalAccessException e) {
                log.error(" !! ({}, {}, {})\n", getClass().getName(), fieldName, this, e);
                continue;
            }

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
        try {
            for (Field field : f) {
                if (isNotDataField(field)) {
                    continue;
                }
                String name = field.getName();
                if (isExcluded(name)) {
                    continue;
                }

                boolean isNull = isNull(name);
                Object value = field.get(this);
                Class<?> type = field.getType();

                if (value == null) {
                    if ((setCreateTime && field.isAnnotationPresent(CreateTime.class))
                        || (setUpdateTime && field.isAnnotationPresent(UpdateTime.class))) {

                        isNull = false;
                        value = new DateTime();
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
        } catch (IllegalAccessException e) {
            log.error(" !! ({}, {})\n", getClass().getName(), this, e);
        }
        return data;
    }

    // todo
    @SuppressWarnings({"unchecked"})
    public List<ManyToManyDefinition> getManyToManyFieldValues(long id) {
        String dtoFk = StringUtil.toSnakeCase(getStorage()) + "_id";
        List<ManyToManyDefinition> params = null;
        try {
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
                manyToManyDefinition.fkRightValue = (List<Long>) field.get(this);

                if (params == null) {
                    params = new ArrayList<>();
                }
                params.add(manyToManyDefinition);
            }
        } catch (IllegalAccessException e) {
            log.error(" !! ({}, {})\n", getClass().getName(), this, e);
        }
        return params;
    }

    // > > > set

    private Object getCleanValue(Object value) {
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
        try {
            for (Field field : getClass().getFields()) {
                if (isNotDataField(field)) {
                    continue;
                }
                String name = field.getName();
                if (CollectionUtil.contains(include, name)) {
                    field.set(this, getCleanValue(dto.getPropertyValue(name)));
                }
            }
        } catch (IllegalAccessException e) {
            log.error(" !! ({}, {})\n", getClass().getName(), this, e);
        }
    }

    /**
     * 1. dto.null are ignored (Action != SET_STRICT)
     * 2. dto.null are set (Action == SET_STRICT)
     * 3. dto.nullProperties are added to current nullProperties
     * 4. dto.excludeProperties and excludeProperties are ignored
     * 5. if field type is dto and is not null, then set will be performed recursively
     * 6. default action is "SET"
     */
    public List<ValidationError> set(Dto dto, String... locales) {
        return set(dto, Action.SET, locales);
    }

    @SuppressWarnings({"unchecked"})
    public List<ValidationError> set(Dto dto, Action action, String... locales) {
        if (dto == null) {
            return validate(action);
        }

        List<ValidationError> errors = null;
        boolean areSameType = dto.getClass().equals(getClass());
        try {
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
                    field.set(this, null);
                    continue;
                }

                if (areSameType) {
                    Object value = field.get(dto);
                    if (value == null) {
                        if (action.equals(Action.SET_STRICT)) {
                            field.set(this, null);
                        }
                        continue;
                    }
                    if (value instanceof Dto) {
                        Dto currentValue = (Dto) field.get(this);
                        if (currentValue != null) {
                            currentValue.set((Dto) value, action);
                            continue;
                        }
                    }
                    field.set(this, value);
                    continue;
                }

                Object value = dto.getPropertyValue(name);
                if (value == null) {
                    if (action.equals(Action.SET_STRICT)) {
                        field.set(this, null);
                    }
                    continue;
                }

                if (value instanceof Dto) {
                    Dto currentValue = (Dto) field.get(this);
                    if (currentValue != null) {
                        currentValue.set((Dto) value, action);
                        continue;
                    }
                }

                if (field.isAnnotationPresent(DeLocalized.class)) {
                    if (!(value instanceof Map)) {
                        log.error(" !! invalid @DeLocalized expected(Map<String, String>) actual({}.{} = {})",
                            dto.getClass().getSimpleName(), name, value);
                        continue;
                    }
                    if (locales == null || locales.length == 0) {
                        log.error(" !! invalid @DeLocalized (no locale) ({}.{} = {})", dto.getClass().getName(), name, value);
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
                    field.set(this, getCleanValue(value));
                } else {
                    if (errors == null) {
                        errors = new ArrayList<>(10);
                        errors.add(new ValidationError(name, VantarKey.INVALID_VALUE_TYPE));
                    }
                    log.warn(" !! type mismatch ({}.{}({}) = {}({}))",
                        getClass().getName(), field.getType(), name, value, value.getClass());
                    try {
                        field.set(this, getCleanValue(value));
                    } catch (Exception e) {
                        log.warn(" !! type mismatch ", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error(" !! ({}, {})\n", getClass().getName(), this, e);
        }
        if (errors == null) {
            return validate(action);
        }
        errors.addAll(validate(action));
        return errors;
    }

    @SuppressWarnings("unchecked")
    public List<ValidationError> set(String json, Action action) {
        if (StringUtil.isEmpty(json)) {
            return validate(action);
        }
        json = json.trim();
        if (!json.startsWith("{") && !json.endsWith("}")) {
            List<ValidationError> errors = new ArrayList<>(1);
            errors.add(new ValidationError(VantarKey.INVALID_JSON_DATA));
            return errors;
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
        if (map != null) {
            setDtoSetConfigs(
                (Collection<String>) map.get(VantarParam.EXCLUDE_PROPERTIES),
                (Collection<String>) map.get(VantarParam.NULL_PROPERTIES),
                (String) map.get(VantarParam.SET_ACTION),
                action
            );
        }
        return setX(map, action, null, null);
    }

    public List<ValidationError> set(Params params, Action action) {
        return set(params, action, null, null);
    }

    public List<ValidationError> set(Params params, Action action, String prefix, String suffix) {
        setDtoSetConfigs(
            params.getStringSet(VantarParam.EXCLUDE_PROPERTIES),
            params.getStringSet(VantarParam.NULL_PROPERTIES),
            params.getString(VantarParam.SET_ACTION),
            action
        );
        return setX(params.getAll(), action, prefix, suffix);
    }

    public List<ValidationError> set(Map<String, Object> map, Action action) {
        return set(map, action, null, null);
    }

    @SuppressWarnings({"unchecked"})
    public List<ValidationError> set(Map<String, Object> map, Action action, String prefix, String suffix) {
        if (map == null) {
            return validate(action);
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

    private List<ValidationError> setX(Map<String, Object> map, Action action, String prefix, String suffix) {
        if (map == null) {
            return validate(action);
        }

        Field[] f = getClass().getFields();
        List<ValidationError> errors = new ArrayList<>(f.length);
        for (Field field : f) {
            if (isNotDataField(field)) {
                continue;
            }

            String key = field.getName();
            if (suffix != null) {
                key += suffix;
            }
            if (prefix != null) {
                key = prefix + key;
            }
            Object value = map.get(key);

            if (excludeProperties != null && excludeProperties.contains(key)) {
                validateField(field, value, action, errors);
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
            } else if (value == null) {
                value = map.get(key);
            }

            setPropertyValue(field, value, action, errors);
        }

        validateGroup(errors, action);
        afterSetData();
        return errors;
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

    public List<ValidationError> setPropertyValue(String name, Object value) {
        return setPropertyValue(name, value, Action.SET);
    }

    public List<ValidationError> setPropertyValue(String name, Object value, Action action) {
        List<ValidationError> errors = new ArrayList<>(8);
        Field field = getField(name);
        if (field == null) {
            errors.add(new ValidationError(name, VantarKey.INVALID_FIELD));
            return errors;
        }
        setPropertyValue(field, value, action, errors);
        return errors;
    }

    @SuppressWarnings("unchecked")
    private void setPropertyValue(Field field, Object value, Action action, List<ValidationError> errors) {
        Class<?> type = field.getType();
        String name = field.getName();

        try {
            if (isNull(name)) {
                field.set(this, null);
                validateField(field, null, action, errors);
                return;
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
                            errors.add(new ValidationError(name, VantarKey.INVALID_VALUE_TYPE));
                            return;
                        }
                    }
                } else if (type.equals(DateTimeRange.class)) {
                    if (!(value instanceof DateTimeRange)) {
                        try {
                            value = DateTimeRange.toDateTimeRange(value);
                        } catch (DateTimeException e) {
                            errors.add(new ValidationError(name, VantarKey.INVALID_VALUE_TYPE));
                            return;
                        }
                    }
                } else if (type.isEnum()) {
                    try {
                        value = EnumUtil.getEnumValueThrow(value.toString(), type);
                    } catch (IllegalArgumentException e) {
                        errors.add(new ValidationError(name, VantarKey.INVALID_VALUE));
                        return;
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
                            Map<String, String> v = (Map<String, String>) field.get(this);
                            if (v == null) {
                                v = new HashMap<>(1, 1);
                            }
                            v.put(Locale.getSelectedLocale(), (String) value);
                            value = v;
                        }
                    } else {
                        Class<?>[] types = getPropertyGenericTypes(name);
                        value = CollectionUtil.toMap(value, types[0], types[1]);
                    }
                } else if (ClassUtil.isInstantiable(type, Dto.class) && !(value instanceof Dto)) {
                    if (value instanceof Map) {
                        Dto innerDto = (Dto) field.get(this);
                        if (innerDto == null) {
                            innerDto = DtoDictionary.get(type).getDtoInstance();
                        }
                        innerDto.set((Map<String, Object>) value, action);
                        value = innerDto;
                    } else {
                        log.error(" !! type mismatch {}>{} ({}, {})\n", name, value, getClass().getName(), this);
                        return;
                    }
                } else if (!(ClassUtil.isInstantiable(type, value.getClass())
                    || ClassUtil.isInstantiable(value.getClass(), type))) {
                    log.error(" !! type mismatch {}>{} ({}, {})\n", name, value, getClass().getName(), this);
                    return;
                }
            }

            if (action.equals(Action.SET) && nullProperties != null) {
                nullProperties.remove(name);
            }

            if (value == null) {
                if (action.equals(Action.INSERT) || action.equals(Action.IMPORT)
                    || action.equals(Action.UPDATE_ALL_COLS) || action.equals(Action.UPDATE_ALL_COLS_NO_ID)) {

                    Object defaultValue = getDefaultValue(field);
                    if (defaultValue != null) {
                        field.set(this, getCleanValue(defaultValue));
                    }
                    if (field.get(this) == null && !name.equals(ID)) {
                        addNullProperties(name);
                    }
                } else if (action.equals(Action.SET)) {
                    field.set(this, null);
                } else if (action.equals(Action.UPDATE_FEW_COLS) || action.equals(Action.UPDATE_FEW_COLS_NO_ID)) {
                    Object defaultValue = getDefaultValue(field);
                    if (defaultValue != null) {
                        field.set(this, getCleanValue(defaultValue));
                    }
                }
            } else {
                field.set(this, getCleanValue(value));
            }
        } catch (Exception e) {
            log.error(" !! {}>{} ({}, {})\n", name, value, getClass().getName(), this, e);
            errors.add(new ValidationError(name, VantarKey.ILLEGAL_FIELD));
        }

        validateField(field, value, action, errors);
    }

    // > > > validate

    public List<ValidationError> validate(Action action) {
        Field[] f = getClass().getFields();
        List<ValidationError> errors = new ArrayList<>(f.length);
        for (Field field : f) {
            if (isDataField(field)) {
                validateField(field, null, action, errors);
            }
        }
        validateGroup(errors, action);
        return errors;
    }

    private void validateField(Field field, Object paramValue, Action action, List<ValidationError> errors) {
        String name = colPrefix == null ? field.getName() : (colPrefix + field.getName());

        if ((action.equals(Action.UPDATE_ALL_COLS) || action.equals(Action.UPDATE_FEW_COLS)
            || action.equals(Action.DELETE) || action.equals(Action.UN_DELETE)) && name.equals(ID)) {

            if (NumberUtil.isIdInvalid(getId())) {
                errors.add(new ValidationError(VantarParam.ID, VantarKey.INVALID_ID));
            }
            return;
        }
        if (action.equals(Action.GET) || action.equals(Action.DELETE) || action.equals(Action.UN_DELETE)
            || action.equals(Action.PURGE)) {

            return;
        }

        Object value;
        try {
            value = field.get(this);
        } catch (IllegalAccessException e) {
            errors.add(new ValidationError(name, VantarKey.ILLEGAL_FIELD));
            return;
        }

        Class<?> type = field.getType();
        if (ObjectUtil.isEmpty(value)) {
            value = null;
        }

        if (value == null) {
            if (ObjectUtil.isNotEmpty(paramValue) && isNull(name)) {
                errors.add(new ValidationError(name, VantarKey.INVALID_VALUE_TYPE));
            } else if (
                (
                    action.equals(Action.INSERT)
                    || action.equals(Action.UPDATE_ALL_COLS)
                    || action.equals(Action.UPDATE_ALL_COLS_NO_ID)
                    || ((action.equals(Action.UPDATE_FEW_COLS) || action.equals(Action.UPDATE_FEW_COLS_NO_ID)) && isNull(name))
                )
                && field.isAnnotationPresent(Required.class)
                && !field.isAnnotationPresent(Default.class)) {

                errors.add(new ValidationError(name, VantarKey.REQUIRED));
            }

            return;
        }

        if (field.isAnnotationPresent(Regex.class) && !value.toString().matches(field.getAnnotation(Regex.class).value())) {
            errors.add(new ValidationError(name, VantarKey.INVALID_FORMAT));
        }

        if (type.equals(Location.class) && !((Location) value).isValid()) {
            errors.add(new ValidationError(name, VantarKey.INVALID_GEO_LOCATION));
        }

        if (field.isAnnotationPresent(Limit.class)) {
            String[] minMax = StringUtil.splitTrim(field.getAnnotation(Limit.class).value(),
                VantarParam.SEPARATOR_COMMON, VantarParam.SEPARATOR_KEY_VAL);

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

        if (value instanceof Dto) {
            ((Dto) value).setColPrefix(name + ".");
            List<ValidationError> errorsX = ((Dto) value).validate(action);
            if (ObjectUtil.isNotEmpty(errorsX)) {
                errors.addAll(errorsX);
            }
            ((Dto) value).setColPrefix(null);
        }

        if (value instanceof List) {
            if (!ClassUtil.implementsInterface(getPropertyGenericTypes(field)[0], Dto.class)) {
                return;
            }
            for (Dto dtoX : (List<? extends Dto>) value) {
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