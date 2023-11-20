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

    void setInclude(String... include);
    void setExclude(String... exclude);
    void addExclude(String... exclude);
    Set<String> getExclude();
    boolean isExcluded(String name);

    void addNullProperties(String... nullProperties);
    void removeNullProperties(String... nullProperties);
    void removeNullPropertiesNatural();
    void setNullProperties(String... nullProperties);
    Set<String> getNullProperties();
    boolean isNull(String name);

    Field getField(String name);
    Field[] getFields();
    String[] getFieldNames();

    Class<?> getPropertyType(String name);
    Class<?> getPropertyType(Field field);
    Map<String, Class<?>> getPropertyTypes();
    Class<?>[] getPropertyGenericTypes(String name);
    Class<?>[] getPropertyGenericTypes(Field field);

    String[] getProperties();
    String[] getProperties(String... exclude);

    Object getPropertyValue(String name);
    Map<String, Object> getPropertyValues(String... include);
    Map<String, Object> getPropertyValuesIncludeNulls(String... include);
    Map<String, Object> getPropertyValues(boolean includeNulls, boolean snakeCase, Map<String, String> propertyNameMap, String... include);

    void setToDefaults();
    void setToDefaultsWhenNull();
    Object getDefaultValue(String name);

    List<String> getPresentationPropertyNames();
    String getPresentationValue();
    String getPresentationValue(String separator);

    void setClearIdOnInsert(boolean clearIdOnInsert);
    boolean getClearIdOnInsert();

    void simpleSet(Dto dto, String... include);
    List<ValidationError> setPropertyValue(String name, Object value);
    List<ValidationError> setPropertyValue(String name, Object value, Action action);
    List<ValidationError> set(Dto dto, String... locales);
    List<ValidationError> set(Dto dto, Action action, String... locales);
    List<ValidationError> set(String json, Dto.Action action);
    List<ValidationError> set(Map<String, Object> params, Action action);
    List<ValidationError> set(Params params, Action action);
    List<ValidationError> set(Params params, Action action, String prefix, String suffix);
    List<ValidationError> validate(Action action);

    void setDeleteLogical(boolean deleteLogical);
    boolean getDeleteLogicalState();
    boolean isDeleteLogicalEnabled();
    void setDeletedQueryPolicy(QueryDeleted policy);
    QueryDeleted getDeletedQueryPolicy();

    Dto.Action getAction(Dto.Action defaultAction);
    String[] getIndexes();
    void setCreateTime(boolean setCreateTime);
    void setUpdateTime(boolean setUpdateTime);
    List<StorableData> getStorableData();
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

        DELETE,
        UN_DELETE,
        PURGE,

        UPLOAD,
    }


    enum QueryDeleted {
        SHOW_DELETED,
        SHOW_NOT_DELETED,
        SHOW_ALL,
    }
}