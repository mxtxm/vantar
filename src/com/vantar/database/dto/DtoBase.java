package com.vantar.database.dto;

import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.datatype.Location;
import com.vantar.exception.DateTimeException;
import com.vantar.locale.VantarKey;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.json.Json;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.util.string.*;
import com.vantar.web.Params;
import org.slf4j.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;


public abstract class DtoBase implements Dto {

    private static final Logger log = LoggerFactory.getLogger(DtoBase.class);
    private static final long INIT_SEQUENCE_VALUE = 1L;

    private boolean setCreateTime;
    private boolean setUpdateTime;
    private String lang;
    private Set<String> exclude;
    private Set<String> nullProperties;
    private boolean deleteLogical = true;
    private QueryDeleted queryDeleted = QueryDeleted.SHOW_NOT_DELETED;


    public boolean isDeleteLogicalEnabled() {
        return this.getClass().isAnnotationPresent(DeleteLogical.class);
    }

    public void setDeleteLogical(boolean deleteLogical) {
        this.deleteLogical = deleteLogical;
    }

    public boolean deleteLogical() {
        return deleteLogical && this.getClass().isAnnotationPresent(DeleteLogical.class);
    }

    public void setQueryDeleted(QueryDeleted policy) {
        queryDeleted = policy;
    }

    public QueryDeleted queryDeleted() {
        return queryDeleted;
    }


    public String getStorage() {
        return getStorage(getClass());
    }

    public static String getStorage(Class<?> dtoClass) {
        return StringUtil.toSnakeCase(
            dtoClass.isAnnotationPresent(Storage.class) ? dtoClass.getAnnotation(Storage.class).value() : dtoClass.getSimpleName()
        );
    }

    public void setId(Long id) {
        setPropertyValue(ID, id);
    }

    public Long getId() {
        return (Long) getPropertyValue(ID);
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getLang() {
        return lang == null ? (String) getPropertyValue("lang") : lang;
    }

    public void setCreateTime(boolean setCreateTime) {
        this.setCreateTime = setCreateTime;
    }

    public void setUpdateTime(boolean setUpdateTime) {
        this.setUpdateTime = setUpdateTime;
    }

    public void setExclude(String... exclude) {
        this.exclude = new HashSet<>();
        this.exclude.addAll(Arrays.asList(exclude));
    }

    public void setInclude(String... include) {
        this.exclude = new HashSet<>(include.length);
        this.exclude.addAll(Arrays.asList(getProperties(include)));
    }

    public Set<String> getExclude() {
        return exclude;
    }

    public void addNullProperties(String... nullProperties) {
        if (this.nullProperties == null) {
            this.nullProperties = new HashSet<>(20);
        }
        this.nullProperties.addAll(Arrays.asList(nullProperties));
    }

    public void setNullProperties(String... nullProperties) {
        if (nullProperties == null) {
            this.nullProperties = null;
        } else {
            this.nullProperties = new HashSet<>(nullProperties.length);
            this.nullProperties.addAll(Arrays.asList(nullProperties));
        }
    }

    public Set<String> getNullProperties() {
        return nullProperties;
    }

    public boolean isNull(String name) {
        return nullProperties != null && nullProperties.contains(name);
    }

    public boolean isEmpty() {
        return getId() == null;
    }

    public String toString() {
        return ObjectUtil.toString(this);
    }

    public Dto getClone() {
        Dto dto = ObjectUtil.getInstance(getClass());
        if (dto != null) {
            dto.set(this);
        }
        return dto;
    }

    public boolean contains(String propertyName) {
        return Arrays.stream(this.getClass().getFields()).anyMatch(f -> f.getName().equals(propertyName));
    }

    public void reset() {
        try {
            for (Field field : getClass().getFields()) {
                if (isDataField(field)) {
                    field.set(this, null);
                }
            }
        } catch (IllegalAccessException e) {
            log.error("! dto reset", e);
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
        List<String> properties = new ArrayList<>();
        for (Field field : getClass().getFields()) {
            if (field.isAnnotationPresent(annotation)) {
                properties.add(field.getName());
            }
        }
        return properties;
    }

    public void setDefaults() {
        setDefaults(false);
    }

    public void setDefaultsWhenNull() {
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
            log.error("! dto({})", this, e);
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
        return getClass().isAnnotationPresent(Index.class) ? getClass().getAnnotation(Index.class).value() : new String[] {};
    }

    // > > > field and property

    public Field getField(String name) {
        try {
            return getClass().getField(name.trim());
        } catch (NoSuchFieldException x) {
            try {
                return getClass().getField(StringUtil.toCamelCase(name.trim()));
            } catch (NoSuchFieldException e) {
                return null;
            }
        }
    }

    public Field[] getFields() {
        List<Field> fields = new ArrayList<>();
        for (Field f : getClass().getFields()) {
            if (isDataField(f)) {
                fields.add(f);
            }
        }
        return fields.toArray(new Field[0]);
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

    public String getPresentationValue() {
        StringBuilder sb = new StringBuilder();
        try {
            for (Field field : getClass().getFields()) {
                if (field.isAnnotationPresent(Present.class)) {
                    sb.append(field.get(this).toString()).append('\n');
                }
            }
            if (sb.length() != 0) {
                sb.setLength(sb.length() - 1);
                return sb.toString();
            }
            for (Field field : getClass().getFields()) {
                if (field.getName().equals("name") || field.getName().equals("title")) {
                    return field.get(this).toString();
                }
            }
            return getId().toString();
        } catch (IllegalAccessException e) {
            log.error("! dto({})", this, e);
        }
        return null;
    }

    public Class<?> getPropertyType(String name) {
        Field field = getField(name);
        return field == null ? null : field.getType();
    }

    public Class<?>[] getPropertyGenericTypes(String name) {
        return ObjectUtil.getFieldGenericTypes(getField(name));
    }

    public Map<String, Class<?>> getPropertyTypes() {
        Map<String, Class<?>> types = new LinkedHashMap<>();
        for (Field field : getClass().getFields()) {
            if (isDataField(field)) {
                types.put(field.getName(), field.getType());
            }
        }
        return types;
    }

    public String[] getProperties() {
        List<String> properties = new ArrayList<>();
        for (Field field : getClass().getFields()) {
            if (isDataField(field)) {
                properties.add(field.getName());
            }
        }
        return properties.toArray(new String[0]);
    }

    public String[] getProperties(String... exclude) {
        List<String> properties = new ArrayList<>();
        for (Field field : getClass().getFields()) {
            if (isDataField(field) && !CollectionUtil.contains(exclude, field.getName())) {
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

        HashSet<String> includeSet;
        if (include.length == 0) {
            includeSet = null;
        } else {
            includeSet = new HashSet<>(include.length);
            for (String includeProperty : include) {
                String[] split = StringUtil.split(includeProperty, ':');
                includeSet.add(split[0]);
                if (split.length == 2) {
                    if (propertyNameMap == null) {
                        propertyNameMap = new HashMap<>();
                    }
                    propertyNameMap.put(split[0], split[1]);
                }
            }
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        for (Field field : getClass().getFields()) {
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
                fieldName = StringUtil.toSnakeCase(field.getName());
            }

            Object value;
            try {
                value = field.get(this);
            } catch (IllegalAccessException e) {
                log.error("! {}", fieldName, e);
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

    public List<DataInfo> getFieldValues() {
        List<DataInfo> data = new ArrayList<>();
        try {
            for (Field field : getClass().getFields()) {
                if (   isNotDataField(field)
                    || field.isAnnotationPresent(ManyToManyGetData.class)
                    || field.isAnnotationPresent(ManyToManyStore.class)) {
                    continue;
                }

                String propertyName = field.getName();
                boolean isNull = isNull(propertyName);
                String fieldName = StringUtil.toSnakeCase(propertyName);
                Object value = field.get(this);

                if (value == null) {
                    if (   (setCreateTime && field.isAnnotationPresent(CreateTime.class))
                        || (setUpdateTime && field.isAnnotationPresent(UpdateTime.class))) {

                        value = new DateTime();
                    }
                    data.add(new DataInfo(fieldName, field.getType(), value, isNull, field.getAnnotations()));
                    continue;
                }

                if (field.isAnnotationPresent(StoreString.class)) {
                    data.add(new DataInfo(fieldName, String.class, Json.toJson(value), isNull, field.getAnnotations()));
                    continue;
                }

                if (field.getType().isEnum()) {
                    data.add(new DataInfo(fieldName, String.class, ((Enum<?>)value).name(), isNull, field.getAnnotations()));
                    continue;
                }

                if (field.isAnnotationPresent(DataType.class)) {
                    String dataType = field.getAnnotation(DataType.class).value();
                    if (dataType.equals("keyword")) {
                        value = StringUtil.normalizeKeywords(value.toString());
                    } else if (dataType.equals("text")) {
                        value = StringUtil.normalizeFullText(value.toString(), getLang());
                    }
                }

                data.add(new DataInfo(fieldName, field.getType(), value, isNull, field.getAnnotations()));
            }
        } catch (IllegalAccessException e) {
            log.error("! {}", this, e);
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

                String[] storage = StringUtil.split(field.getAnnotation(ManyToManyStore.class).value(), VantarParam.SEPARATOR_NEXT);
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
            log.error("! {}", this, e);
        }

        return params;
    }

    // > > > set

    /**
     * can do:
     * 1. same class copy
     * 2. dto copy fields with the same name regardless of camel case or snake case
     */
    @SuppressWarnings({"unchecked"})
    public void set(Dto dto, String... locales) {
        if (dto == null) {
            return;
        }
        nullProperties = dto.getNullProperties();
        boolean sameType = dto.getClass().equals(getClass());
        try {
            for (Field field : getClass().getFields()) {
                if (isNotDataField(field)) {
                    continue;
                }

                if (sameType) {
                    Object value = field.get(dto);
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof Dto) {
                        Dto currentDtoValue = (Dto) field.get(this);
                        if (currentDtoValue != null) {
                            currentDtoValue.set((Dto) value);
                            continue;
                        }
                    }
                    field.set(this, value);
                    continue;
                }

                Object value = dto.getPropertyValue(field.getName());
                if (value == null) {
                    value = dto.getPropertyValue(StringUtil.toSnakeCase(field.getName()));
                }
                if (value == null) {
                    continue;
                }

                if (field.isAnnotationPresent(DeLocalized.class)) {
                    if (!(value instanceof Map)) {
                        log.error("! invalid DeLocalized usage ({}.{}) is not localized as Map<String, String> but is >> ({})",
                            dto.getClass(), field.getName(), value);
                        continue;
                    }
                    if (locales == null || locales.length == 0) {
                        log.error("! invalid DeLocalized (no locale has been provided) ({}.{} = {})",
                            dto.getClass(), field.getName(), value);
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

                if (value instanceof Dto) {
                    Dto currentDtoValue = (Dto) field.get(this);
                    if (currentDtoValue != null) {
                        currentDtoValue.set((Dto) value);
                        continue;
                    }
                }

                field.set(this, value);
            }
        } catch (IllegalAccessException e) {
            log.error("! {}", this, e);
        }
    }

    public List<ValidationError> set(String json, Dto.Action action) {
        if (StringUtil.isEmpty(json)) {
            return validate(action);
        }
        json = json.trim();
        if (!json.startsWith("{") && !json.endsWith("}")) {
            return validate(action);
        }

        Dto dto = Json.fromJson(json, getClass());

        if (dto == null) {
            log.error("^^^ ignore the above error ^^^");
            return set(Json.mapFromJson(json, String.class, Object.class), action);
        }

        NullProperties np = Json.fromJson(json, NullProperties.class);
        if (np != null) {
            dto.setNullProperties(np.nullProperties);
        }

        set(dto);
        return validate(action);
    }

    public List<ValidationError> set(Params params, Dto.Action action) {
        exclude = params.getStringSet(VantarParam.EXCLUDE);
        nullProperties = params.getStringSet(VantarParam.NULLS);
        return set(params.getAll(), action, null, null);
    }

    public List<ValidationError> set(Params params, Dto.Action action, String prefix, String suffix) {
        return set(params.getAll(), action, prefix, suffix);
    }

    public List<ValidationError> set(Map<String, Object> map, Dto.Action action) {
        return set(map, action, null, null);
    }

    @SuppressWarnings({"unchecked"})
    public List<ValidationError> set(Map<String, Object> map, Dto.Action action, String prefix, String suffix) {
        List<ValidationError> errors = new ArrayList<>();
        if (map == null) {
            return validate(action);
        }

        Object nulls = map.get(VantarParam.NULLS);
        if (nulls != null) {
            if (nulls.getClass().isArray()) {
                nullProperties = new HashSet<>(Arrays.asList((String[]) nulls));
            }
            if (nulls instanceof Collection<?>) {
                nullProperties = new HashSet<>((Collection<String>) nulls);
            }
            if (nulls instanceof String) {
                nullProperties = StringUtil.splitToSet((String) nulls, ',');
            }
        }

        for (Field field : getClass().getFields()) {
            if (isNotDataField(field)) {
                continue;
            }

            String key = field.getName();
            if (exclude != null && exclude.contains(key)) {
                continue;
            }
            if (suffix != null) {
                key += suffix;
            }
            if (prefix != null) {
                key = prefix + key;
            }

            if (field.getType().equals(Location.class)) {
                Location location = map.containsKey(key) ?
                    new Location((String) map.get(key)) :
                    new Location(
                        ObjectUtil.toDouble(map.get(key + "_latitude")),
                        ObjectUtil.toDouble(map.get(key + "_longitude"))
                    );

                if (!location.isEmpty()) {
                    try {
                        setField(field, location);
                    } catch (IllegalAccessException e) {
                        log.error("! {}", field.getName(), e);
                        errors.add(new ValidationError(field.getName(), VantarKey.ILLEGAL));
                    }
                }
                continue;
            }

            Object value = map.get(key);
            if (value == null) {
                value = map.get(StringUtil.toSnakeCase(key));
            }

            setPropertyValue(field, value, action, errors);
        }

        afterSetData();
        return errors;
    }

    public boolean setPropertyValue(String name, Object value) {
        return setPropertyValue(name, value, Action.STRICT_WITH_NULLS).isEmpty();
    }

    public List<ValidationError> setPropertyValue(String name, Object value, Action action) {
        List<ValidationError> errors = new ArrayList<>();
        Field field = getField(name);
        if (field == null) {
            errors.add(new ValidationError(name, VantarKey.INVALID_FIELD));
            return errors;
        }
        setPropertyValue(field, value, action, errors);
        return errors;
    }

    private void setPropertyValue(Field field, Object value, Action action, List<ValidationError> errors) {
        Class<?> type = field.getType();
        String name = field.getName();
        try {
            if (nullProperties != null && nullProperties.contains(name)) {
                field.set(this, null);
                validateField(field, null, action, errors);
                return;
            }

            if (value != null) {
                if (type.equals(String.class)) {
                    String stringValue = value.toString();
                    if (StringUtil.isEmpty(stringValue)) {
                        value = null;
                    } else {
                        setField(field, stringValue);
                    }

                } else if (type.equals(Integer.class)) {
                    setField(field, ObjectUtil.toInteger(value));
                } else if (type.equals(Long.class)) {
                    setField(field, ObjectUtil.toLong(value));
                } else if (type.equals(Double.class)) {
                    setField(field, ObjectUtil.toDouble(value));
                } else if (type.equals(Float.class)) {
                    setField(field, ObjectUtil.toFloat(value));
                } else if (type.equals(Boolean.class)) {
                    setField(field, ObjectUtil.toBoolean(value));
                } else if (type.equals(Character.class)) {
                    setField(field, ObjectUtil.toCharacter(value));
                } else if (type.equals(DateTime.class)) {
                    if (value instanceof DateTime) {
                        setField(field, value);
                        return;
                    }
                    String stringValue = value.toString();
                    if (StringUtil.isEmpty(stringValue)) {
                        value = null;
                    } else {
                        if (stringValue.equalsIgnoreCase("now")) {
                            setField(field, new DateTime());
                        } else {
                            try {
                                setField(field, new DateTime(stringValue));
                            } catch (DateTimeException e) {
                                errors.add(new ValidationError(name, VantarKey.DATA_TYPE));
                                return;
                            }
                        }
                    }

                } else if (type.isEnum()) {
                    try {
                        EnumUtil.setEnumValue(this, type, field, value.toString());
                    } catch (IllegalArgumentException e) {
                        errors.add(new ValidationError(name, VantarKey.INVALID_VALUE));
                        return;
                    }

                } else if (type.equals(List.class) || type.equals(ArrayList.class)) {
                    setField(field, ObjectUtil.getList(value, getPropertyGenericTypes(name)[0]));

                } else if (type.equals(Set.class)) {
                    setField(field, new HashSet<>(ObjectUtil.getList(value, getPropertyGenericTypes(name)[0])));

                } else if (type.equals(Map.class)) {
                    if (value instanceof String) {
                        Class<?>[] types = getPropertyGenericTypes(name);
                        setField(field, Json.mapFromJson((String) value, types[0], types[1]));
                    } else if (value instanceof Map) {
                        setField(field, value);
                    }
                }
            }

            if ((action.equals(Action.INSERT) || action.equals(Action.UPDATE_STRICT) || action.equals(Action.STRICT_WITH_NULLS))
                && field.get(this) == null) {

                Object defaultValue = getDefaultValue(field);
                field.set(this, defaultValue);

                if (defaultValue == null && action.equals(Action.STRICT_WITH_NULLS)) {
                    if (nullProperties == null) {
                        nullProperties = new HashSet<>();
                    }
                    nullProperties.add(name);
                }
            }
        } catch (IllegalAccessException e) {
            log.error("! {}>{}", name, value, e);
            errors.add(new ValidationError(name, VantarKey.ILLEGAL));
        }

        validateField(field, value, action, errors);
    }

    // > > > validate

    public List<ValidationError> validate(Dto.Action action) {
        List<ValidationError> errors = new ArrayList<>();
        for (Field field : getClass().getFields()) {
            if (isDataField(field)) {
                validateField(field, null, action, errors);
            }
        }
        return errors;
    }

    private void validateField(Field field, Object originalValue, Dto.Action action, List<ValidationError> errors) {
        String name = field.getName();

        if ((action.equals(Action.UPDATE) || action.equals(Action.DELETE)) && name.equals(ID)) {
            if (NumberUtil.isIdInvalid(getId())) {
                errors.add(new ValidationError(VantarParam.ID, VantarKey.EMPTY_ID));
            }
            return;
        }

        Object value;
        try {
            value = field.get(this);
        } catch (IllegalAccessException e) {
            errors.add(new ValidationError(name, VantarKey.ILLEGAL));
            return;
        }

        Class<?> type = field.getType();
        if (type.equals(String.class) && StringUtil.isEmpty((String) value)) {
            value = null;
        }

        boolean valueIsEmpty = value == null
            || (value instanceof Collection && ((Collection<?>) value).isEmpty())
            || (value instanceof Map && ((Map<?, ?>) value).isEmpty());

        if (valueIsEmpty && originalValue != null && !(type.equals(String.class) && originalValue instanceof String)
            && StringUtil.isNotEmpty(originalValue.toString())) {

            errors.add(new ValidationError(name, VantarKey.DATA_TYPE));
            return;
        }

        if (action.equals(Action.INSERT) || action.equals(Action.UPDATE_STRICT)) {
            if (field.isAnnotationPresent(Required.class) && !field.isAnnotationPresent(Default.class) && valueIsEmpty) {
                errors.add(new ValidationError(name, VantarKey.REQUIRED));
            }
        }

        if (value == null) {
            return;
        }

        if (field.isAnnotationPresent(Regex.class) && !value.toString().matches(field.getAnnotation(Regex.class).value())) {
            errors.add(new ValidationError(name, VantarKey.REGEX));
        }

        if (type.equals(Location.class) && !((Location) value).isValid()) {
            errors.add(new ValidationError(name, VantarKey.INVALID_GEO_LOCATION));
        }

        if (field.isAnnotationPresent(Limit.class)) {
            String[] minMax = StringUtil.split(field.getAnnotation(Limit.class).value(),
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
                errors.add(new ValidationError(name, VantarKey.STRING_LENGTH_EXCEED));
            }
        }
    }

    private void setField(Field field, Object object) throws IllegalAccessException {
        if (object != null) {
            field.set(this, object);
        }
    }

    public static boolean isDataField(Field field) {
        int m = field.getModifiers();
        return !Modifier.isFinal(m) && !Modifier.isStatic(m) && !field.isAnnotationPresent(NoStore.class);
    }

    public static boolean isNotDataField(Field field) {
        int m = field.getModifiers();
        return Modifier.isFinal(m) || Modifier.isStatic(m) || field.isAnnotationPresent(NoStore.class);
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

    public void beforeJson() {

    }


    private static class NullProperties {

        public String[] nullProperties;

        public String toString() {
            return ObjectUtil.toString(this);
        }
    }
}