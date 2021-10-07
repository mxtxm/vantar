package com.vantar.database.dto;

import com.vantar.database.common.ValidationError;
import com.vantar.web.Params;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;


public interface Dto {

    String ID = "id";

    // must implement > > >

    String getStorage();

    boolean isEmpty();

    Long getId();

    void setId(Long id);

    // implemented in base > > >

    Field[] getFields();

    void setLang(String lang);

    String getLang();

    boolean hasAnnotation(String property, Class<? extends Annotation> annotation);

    boolean hasAnnotation(Class<? extends Annotation> annotation);

    <T extends Annotation> T getAnnotation(String property, Class<T> annotation);

    List<String> annotatedProperties(Class<? extends Annotation> annotation);

    Field getField(String name);

    boolean contains(String fieldName);

    Set<String> getExclude();

    void setInclude(String... include);

    void setExclude(String... exclude);

    Set<String> getNullProperties();

    boolean isNull(String name);

    void addNullProperties(String... nullProperties);

    void setNullProperties(String... nullProperties);

    String[] getProperties();

    String[] getProperties(String... exclude);

    Map<String, Object> getPropertyValues(String... include);

    Map<String, Object> getPropertyValuesIncludeNulls(String... include);

    Map<String, Object> getPropertyValues(boolean includeNulls, boolean snakeCase, Map<String, String> propertyNameMap, String... include);

    List<DataInfo> getFieldValues();

    List<ManyToManyDefinition> getManyToManyFieldValues(long id);

    Map<String, Class<?>> getPropertyTypes();

    Class<?>[] getPropertyGenericTypes(String name);

    void setCreateTime(boolean setCreateTime);

    void setUpdateTime(boolean setUpdateTime);

    void setDefaults();

    void setDefaultsWhenNull();

    void beforeJson();

    boolean beforeInsert();

    boolean beforeUpdate();

    void afterSetData();

    void afterFetchData(long i);

    void afterFetchData();

    Object getPropertyValue(String name);

    String getPresentationValue();

    Class<?> getPropertyType(String name);

    Object getDefaultValue(String name);

    String getSequenceName();

    String[] getIndexes();

    boolean setPropertyValue(String name, Object value);

    List<ValidationError> setPropertyValue(String name, Object value, Action action);

    void reset();

    long getSequenceInitValue();

    Dto getClone();

    void set(Dto dto, String... locales);

    List<ValidationError> set(String json, Dto.Action action);

    List<ValidationError> set(Map<String, Object> params, Action action);

    List<ValidationError> set(Params params, Action action);

    List<ValidationError> set(Params params, Action action, String prefix, String suffix);

    List<ValidationError> validate(Action action);

    boolean isDeleteLogicalEnabled();
    void setDeleteLogical(boolean deleteLogical);
    boolean deleteLogical();
    void setQueryDeleted(QueryDeleted policy);
    QueryDeleted queryDeleted();


    enum Action {
        INSERT,
        UPDATE,
        UPDATE_STRICT,
        STRICT_WITH_NULLS,
        DELETE,
        GET,
        UN_DELETE,
        PURGE,
        IMPORT,
    }


    enum QueryDeleted {
        SHOW_DELETED,
        SHOW_NOT_DELETED,
        SHOW_ALL,
    }
}