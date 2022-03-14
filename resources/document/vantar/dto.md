## annotations ##

* Required
* Unique
* Present : present dto by this field
* CreateTime : automatically set to create time 
* UpdateTime : automatically set to update time
* Date : store as date
* Timestamp : store as date-time
* Time : store as time
* StoreString : store value as JSON string 
* DbIgnore : dont store this property
* Default
    @Default("default-value")
* Index
    @Index("name:1username:-1") separated index on each field
    @Index("name:1,username:-1") index on more than one field
* InitValue : sequence init value (long)
    @InitValue("1")
* Sequence : use another sequence name from another collection
    @Sequence("collection-name")
* PresentBy : present value by another field
    @PresentBy(Param.PACKAGE_DTO + ".base.Agency")
* Tag: tag a field for something or things, for example WebUi dto forms checks this to include or not include  

## methods ##
String EXCLUDE = "ex"

### must implement ###
* String getStorage()
* boolean isEmpty()
* Object getId()
* void setId(Object id)

### implemented in base ###

#### database field case, default is snake case ####
* static void setUseSnakeCaseFieldNames()
* static void setUseCamelCaseFieldNames()
* static boolean useSnakeCaseFieldNames()

#### exclude, include ####
* void setExclude(Set<String> exclude)
    set a list of property names which are excluded from dto in data actions
* void setExclude(String... exclude)
    set a list of property names which are excluded from dto in data actions
* Set<String> getExclude()
    get a list of property names which are excluded from dto in data actions
    used by other libraries like mongo or sql
* void setNullProperties(String... fields)
    set properties which must be set to null
    used by getFieldValues()
* String[] getNullProperties()
    set properties which must be set to null

#### property names, types and values ####
* Map<String, Class<?>> getPropertyTypes()
    type of all properties
* Class<?> getPropertyType(String name)
    get property type by name    
* String[] getProperties()
    get a list of data property names
* String[] getProperties(String... exclude)
    get a list of data property names, exclude some
* Map<String, Object> getPropertyValues()
    get {propertyName: value}, exclude null properties
* Map<String, Object> getPropertyValuesIncludeNulls()
    get {propertyName: value}, include null properties
* Map<String, Object> getPropertyValues(boolean includeNulls, boolean snakeCase, Map<String, String> propertyNameMap)
    get {propertyName: value}
* Map<String, Object> getFieldValues()
    use only for storing in database {field_name: value} snake case/camel case is based on case config (useSnakeCaseFieldNames)
    sets DateTime createtime and updateBatch time for database, sets type
    ignores final and static and nulls
* Object getPropertyValue(String name)
    get value by property name (camelCase)
* Object getFieldPresentationValue(String name)
    get field value, if is presented by another object get value of that, if is null and has default then return default
* String getPresentValue()
    get value of presentation field of this dto
* Object getDefaultValue(String name)
    get the default value of a property by name
    
#### set ####
* List<ValidationError> set(Params params)
    set property values from request, request param key must be exact property name
    key "ex" in request to exclude properties "id,name,age"
    return is a list of errors if a property value is bad, error types:
    VantarKeys.DATA_TYPE
    VantarKeys.REQUIRED
    VantarKeys.ILLEGAL
    fires afterSetData()
* List<ValidationError> set(Params params, String suffix)
    same as above, can set a suffix
* void set(Map<String, Object> params)
    same as above but from map instead of request
* boolean setPropertyValue(String name, Object value)
    set a value to a property by exact name
* void setCreateTime(boolean setCreateTime)
    make set methods set/not set create time fields - default is not set
    must be called by database driver
* void setUpdateTime(boolean setUpdateTime)
    make set methods set/not set update time fields - default is not set
    must be called by database driver
* void setDefaults()
    set property values to default, overwrites current value
* void setDefaultsWhenNull()
    set property values to default, only if current value is null
* void setEmptyToNull()
    set property values that are empty string to null

#### trigger ####
* void beforeJson()
* void beforeInsert()
* void beforeUpdate()
* void afterSetData(long i)
* void afterSetData()

#### sequence ####
* String getSequenceName()
    get name of sequence, default is storage name unless "id" property has @Sequence("collection-name") 
* long getSequenceInitValue()
    get the init value of sequence
* String[] getIndexes()
    get list of dto indexes, used by mongo or sql
* void reset()
    set data properties to null


## DtoUtil ##
* String toJson(Dto dto)
    dto to json string, keys are property names, null properties not included

* String toJson(Dto dto, Map<String, String> propertyNameMap)
    dto to json string, keys can be exchanged using propertyNameMap(propertyName -> key-name), null properties not included

* String toJsonSnakeCaseKeys(Dto dto, Map<String, String> propertyNameMap)
    dto to json string, keys are snake case property names, null properties not included

* String toJson(List<? extends Dto> dtos)
    json will include keys that are properties from different dtos
    list of dtos to json string, keys are property names, null properties not included

* String toJsonSnakeCaseKeys(List<? extends Dto> dtos)
    the same as toJson(List<? extends Dto> dtos) just keys are snake case property names

* String toJson(List<? extends Dto> dtos, Map<String, String> propertyNameMap)
    same as toJson(List<? extends Dto> dtos) just keys can be exchanged using propertyNameMap(propertyName -> key-name)

* String toJsonSnakeCaseKeys(List<? extends Dto> dtos, Map<String, String> propertyNameMap)
    same as toJson(List<? extends Dto> dtos, Map<String, String> propertyNameMap) just keys are snake case property names

* List<Map<String, Object>> getPropertyValues(List<? extends Dto> dtos, Map<String, String> propertyNameMap)
    get a list of {key: value}s from a list of dtos, null properties not included
    keys can be exchanged using propertyNameMap(propertyName -> key-name)
    
* List<Map<String, Object>> getPropertyValuesSnakeCaseKeys(List<? extends Dto> dtos, Map<String, String> propertyNameMap)
    same as above just keys are snake case property names