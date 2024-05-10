package com.vantar.service.auth;

import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.*;
import com.vantar.web.Params;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;


public class RootUser implements CommonUser {

    public String token;

    @Override
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public AccessStatus getAccessStatus() {
        return AccessStatus.ENABLED;
    }

    @Override
    public String getFullName() {
        return "Startup root";
    }

    @Override
    public String getMobile() {
        return "09121933230";
    }

    @Override
    public String getEmail() {
        return "mxtorabi@gmail.com";
    }

    @Override
    public String getUsername() {
        return "startup-root";
    }

    @Override
    public CommonUserRole getRole() {
        return new RootRole();
    }

    @Override
    public Collection<? extends CommonUserRole> getRoles() {
        Collection<CommonUserRole> roles = new HashSet<>(1, 1);
        roles.add(new RootRole());
        return roles;
    }

    @Override
    public Long getId() {
        return 1L;
    }

    @Override
    public String getStorage() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(String propertyName) {
        return false;
    }

    @Override
    public void reset() {

    }

    @Override
    public Dto getClone() {
        return null;
    }

    @Override
    public String getSequenceName() {
        return null;
    }

    @Override
    public long getSequenceInitValue() {
        return 0;
    }

    @Override
    public void setId(Long id) {

    }

    @Override
    public void setLang(String lang) {

    }

    @Override
    public String getLang() {
        return null;
    }

    @Override
    public boolean hasAnnotation(String property, Class<? extends Annotation> annotation) {
        return false;
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return false;
    }

    @Override
    public <T extends Annotation> T getAnnotation(String property, Class<T> annotation) {
        return null;
    }

    @Override
    public List<String> annotatedProperties(Class<? extends Annotation> annotation) {
        return null;
    }

    @Override
    public List<Field> annotatedFields(Class<? extends Annotation> annotation) {
        return null;
    }

    @Override
    public void setInclude(String... include) {

    }

    @Override
    public void setExclude(String... exclude) {

    }

    @Override
    public void addExclude(String... exclude) {

    }

    @Override
    public Set<String> getExclude() {
        return null;
    }

    @Override
    public boolean isExcluded(String name) {
        return false;
    }

    @Override
    public void addNullProperties(String... nullProperties) {

    }

    @Override
    public void removeNullProperties(String... nullProperties) {

    }

    @Override
    public void removeNullPropertiesNatural() {

    }

    @Override
    public void addNullPropertiesNatural() {

    }

    @Override
    public void setNullProperties(String... nullProperties) {

    }

    @Override
    public Set<String> getNullProperties() {
        return null;
    }

    @Override
    public boolean isNull(String name) {
        return false;
    }

    @Override
    public Field getField(String name) {
        return null;
    }

    @Override
    public Field[] getFields() {
        return new Field[0];
    }

    @Override
    public String[] getFieldNamesForQuery() {
        return new String[0];
    }

    @Override
    public Class<?> getPropertyType(String name) {
        return null;
    }

    @Override
    public Class<?> getPropertyType(Field field) {
        return null;
    }

    @Override
    public Map<String, Class<?>> getPropertyTypes() {
        return null;
    }

    @Override
    public Class<?>[] getPropertyGenericTypes(String name) {
        return new Class[0];
    }

    @Override
    public Class<?>[] getPropertyGenericTypes(Field field) {
        return new Class[0];
    }

    @Override
    public String[] getProperties() {
        return new String[0];
    }

    @Override
    public String[] getPropertiesEx(String... exclude) {
        return new String[0];
    }

    @Override
    public String[] getPropertiesInc(String... include) {
        return new String[0];
    }

    @Override
    public Object getPropertyValue(String name) {
        return null;
    }

    @Override
    public Map<String, Object> getPropertyValues(String... include) {
        return null;
    }

    @Override
    public Map<String, Object> getPropertyValuesIncludeNulls(String... include) {
        return null;
    }

    @Override
    public Map<String, Object> getPropertyValues(boolean includeNulls, boolean snakeCase, Map<String, String> propertyNameMap, String... include) {
        return null;
    }

    @Override
    public void setToDefaults() {

    }

    @Override
    public void setToDefaultsWhenNull() {

    }

    @Override
    public Object getDefaultValue(String name) {
        return null;
    }

    @Override
    public List<String> getPresentationPropertyNames() {
        return null;
    }

    @Override
    public String getPresentationValue() {
        return null;
    }

    @Override
    public String getPresentationValue(String separator) {
        return null;
    }

    @Override
    public void autoIncrementOnInsert(boolean autoIncrement) {

    }

    @Override
    public boolean isAutoIncrementOnInsert() {
        return false;
    }

    @Override
    public void simpleSet(Dto dto, String... include) {

    }

    @Override
    public boolean setPropertyValue(String name, Object value) {
        return false;
    }

    @Override
    public boolean setPropertyValue(String name, Object value, Action action) {
        return false;
    }

    @Override
    public boolean set(Dto dto, String... locales) {
        return false;
    }

    @Override
    public boolean set(Dto dto, Action action, String... locales) {
        return false;
    }

    @Override
    public boolean set(String json, Action action) {
        return false;
    }

    @Override
    public boolean set(String json, Action action, String prefix, String suffix) {
        return false;
    }

    @Override
    public boolean set(Map<String, Object> params, Action action) {
        return false;
    }

    @Override
    public boolean set(Map<String, Object> map, Action action, String prefix, String suffix) {
        return false;
    }

    @Override
    public boolean set(Params params, Action action) {
        return false;
    }

    @Override
    public boolean set(Params params, Action action, String prefix, String suffix) {
        return false;
    }

    @Override
    public List<ValidationError> validate(Action action) {
        return null;
    }

    @Override
    public List<ValidationError> validateProperty(String p, Action action) {
        return null;
    }

    @Override
    public void setColPrefix(String colPrefix) {

    }

    @Override
    public Action getAction(Action defaultAction) {
        return null;
    }

    @Override
    public String[] getIndexes() {
        return new String[0];
    }

    @Override
    public void setCreateTime(boolean setCreateTime) {

    }

    @Override
    public void setUpdateTime(boolean setUpdateTime) {

    }

    @Override
    public List<StorableData> getStorableData() {
        return null;
    }

    @Override
    public List<ManyToManyDefinition> getManyToManyFieldValues(long id) {
        return null;
    }

    @Override
    public void beforeJson() {

    }

    @Override
    public boolean beforeInsert() {
        return false;
    }

    @Override
    public boolean beforeUpdate() {
        return false;
    }

    @Override
    public boolean beforeDelete() {
        return false;
    }

    @Override
    public void afterInsert() {

    }

    @Override
    public void afterUpdate() {

    }

    @Override
    public void afterDelete() {

    }

    @Override
    public void afterSetData() {

    }

    @Override
    public void afterFetchData(long i) {

    }

    @Override
    public void afterFetchData() {

    }

    @Override
    public Map<String, Object> getExtraData() {
        return null;
    }
}
