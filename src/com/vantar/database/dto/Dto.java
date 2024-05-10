package com.vantar.database.dto;

import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.web.Params;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;


public interface Dto {

    String ID = VantarParam.ID;

    String getStorage();
    boolean isEmpty();
    boolean contains(String propertyName);
    void reset();
    Dto getClone();
    String getSequenceName();
    long getSequenceInitValue();

    Long getId();
    void setId(Long id);

    void setLang(String lang);
    String getLang();

    boolean hasAnnotation(String property, Class<? extends Annotation> annotation);
    boolean hasAnnotation(Class<? extends Annotation> annotation);
    <T extends Annotation> T getAnnotation(String property, Class<T> annotation);
    List<String> annotatedProperties(Class<? extends Annotation> annotation);
    List<Field> annotatedFields(Class<? extends Annotation> annotation);

    void setInclude(String... include);
    void setExclude(String... exclude);
    void addExclude(String... exclude);
    Set<String> getExclude();
    boolean isExcluded(String name);

    void addNullProperties(String... nullProperties);
    void removeNullProperties(String... nullProperties);
    void removeNullPropertiesNatural();
    void addNullPropertiesNatural();
    void setNullProperties(String... nullProperties);
    Set<String> getNullProperties();
    boolean isNull(String name);

    Field getField(String name);
    Field[] getFields();
    String[] getFieldNamesForQuery();

    Class<?> getPropertyType(String name);
    Class<?> getPropertyType(Field field);
    Map<String, Class<?>> getPropertyTypes();
    Class<?>[] getPropertyGenericTypes(String name);
    Class<?>[] getPropertyGenericTypes(Field field);

    String[] getProperties();
    String[] getPropertiesEx(String... exclude);
    String[] getPropertiesInc(String... include);

    Object getPropertyValue(String name);
    Map<String, Object> getPropertyValues(String... include);
    Map<String, Object> getPropertyValuesIncludeNulls(String... include);
    Map<String, Object> getPropertyValues(boolean includeNulls, boolean snakeCase, Map<String, String> propertyNameMap, String... include);

    Object getDefaultValue(String name);
    List<String> getPresentationPropertyNames();
    String getPresentationValue();
    String getPresentationValue(String separator);
    Dto.Action getAction(Dto.Action defaultAction);
    String[] getIndexes();
    List<StorableData> getStorableData();

    void autoIncrementOnInsert(boolean autoIncrement);
    boolean isAutoIncrementOnInsert();

    void setToDefaults();
    void setToDefaultsWhenNull();
    void simpleSet(Dto dto, String... include);
    boolean set(Dto dto, String... locales);
    boolean set(Dto dto, Action action, String... locales);
    boolean set(String json, Dto.Action action);
    boolean set(String json, Dto.Action action, String prefix, String suffix);
    boolean set(Map<String, Object> params, Action action);
    boolean set(Map<String, Object> map, Action action, String prefix, String suffix);
    boolean set(Params params, Action action);
    boolean set(Params params, Action action, String prefix, String suffix);
    boolean setPropertyValue(String name, Object value);
    boolean setPropertyValue(String name, Object value, Action action);

    List<ValidationError> validate(Action action);
    List<ValidationError> validateProperty(String name, Action action);

    void setColPrefix(String colPrefix);
    void setCreateTime(boolean setCreateTime);
    void setUpdateTime(boolean setUpdateTime);

    List<ManyToManyDefinition> getManyToManyFieldValues(long id);

    void beforeJson();
    boolean beforeInsert();
    boolean beforeUpdate();
    boolean beforeDelete();
    void afterInsert();
    void afterUpdate();
    void afterDelete();
    void afterSetData();
    void afterFetchData(long i);
    void afterFetchData();


    enum Action {
        SET,
        SET_STRICT,
        GET,

        IMPORT,
        INSERT,

        UPDATE_ALL_COLS,
        UPDATE_ALL_COLS_NO_ID,
        UPDATE_FEW_COLS,
        UPDATE_FEW_COLS_NO_ID,
        UPDATE_ADD_ITEM,
        UPDATE_REMOVE_ITEM,

        DELETE,
        UN_DELETE,
        PURGE,

        UPLOAD,
    }
}