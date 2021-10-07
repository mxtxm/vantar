<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 */

//namespace arta;

/**
 * Interface for the data model classes.
 *
 * @author Mehdi Torabi <mehdi @ artaengine.com>
 */
interface IModel extends Iterator {

    /**
     * Get a model's index used in the database views. Each model name is mapped to
     * an index, this index is used instead of table name in the db views. 
     *
     * @param  string group Model group name
     * @param  string model Model name
     * @return string The model index used in views
     */
    static public function getDbIndex($group, $model);

    /**
     * Reset model instancing state.
     *
     * @param  string|int keyVal=null If provided, after resetting the model's key property will be set to this value
     * @return IModel For method chaining
     */
    public function reset($keyVal=null);

    /**
     * Get the appropriate label for a data-property, method or object-path relative
     * to the current model instance. Always use this method to get a label instead
     * of going other ways. This method traverses the object-path and returns the first
     * label found for it. Object-path e.g. "User.Group.name.".
     * @since Artaengine 1.2.2
     *
     * @param  string pm Model property-name, method-name or object-path to get label for
     * @return string The label
     */
    public function getLabel($pm);

    /**
     * Get a list of model data properties.
     *
     * @return array [property-name,]
     */
    public function properties();

    /**
     * Get a dictionary of all model data properties and their definitions. Arguments
     * can be passed to filter the result.
     *
     * @param  string property=null null=return all, property-name=only return info for this property
     * @param  string key=null      Return the value of a certain key in property's definition array
     * @param  string default=null  If key not exists in the array then return this value instead
     * @return mixed  not-array=value of a property definition key, array=[@Model.propertyDefs.return]
     */
    public function propertyDefs($property=null, $key=null, $default=null);

    /**
     * Set model locale(s). A data property will be localized if "i18n" is set to true
     * in it's definition. Query and actions on this properties will be locale aware.
     * By default the model locale is set to user's selected LOCALE, you can manually
     * change it with this method. If you set an array of locales then the data will be
     * queried for all those locales. Same thing is valid for other actions.
     * All app's available locales="Arta::$globals['locales']", users locale="LOCALE".
     *
     * @param string|array|[string] locale=null Locale(s) e.g. "en-US", "fa-IR"
     * @return IModel For method chaining
     */
    public function setLocale($locale);

    /**
     * Get the data of model's presenter property(s). Each model can be presented by
     * one or more properties. Presenting means when in a form or table a list or column
     * is going to show a model's data as an object of another model then which property(s)'s
     * data must be displayed.
     *
     * @param  string sep=null Separator. string=return a string of data glued with this string
     * @return array|string {property-name: data,} | "data" or "data1SEPdata2SEPdata3"
     */
    public function present($sep=null);

    /**
     * Get the value of model's key property and set the value to the property.<br/>
     * if model's key property is already set to a value return it.<br/>
     * if model's key property is serial(auto inc) and newKey=true return the last inserted value<br/>
     * query the model based on set property values and set conditions and return the key values(s)
     *
     * @param  bool newKey=true true=if key type is SERIAL return the last inserted key value
     * @return mixed|array Value or an array of values (if a query is done and result has more than one rows)
     */
    public function keyVal($newKey=true);

    /**
     * A midware can be set or each data property (setting "midware-classname" to the "midware" key in the property's definitions).
     * A midware class may contain static methods "afterNext", "beforeStore" and "beforeQuery", when the event happens on the property
     * the appropriate midware method (if exists) will be called passing property-value and using the returned value instead.
     * This method is used internally to check for a midware, pass and get the values however it is left public to be freely used.
     * @since Artaengine 1.2.0
     *
     * @param  mixed value  Property value to be passed to the midware method
     * @param  array def    Property definition
     * @param  int   type=0 Mid-ware method to be called, 0=afterNext() 1=beforeStore() 2=beforeQuery()
     * @return mixed The value returned by the midware 
     */
    static public function midware($value, $def, $type=0);

    /**
     * Commit model data manipulations to the database. Actions add/update/delete are
     * performed on the database only after calling this method.
     *
     * @param  bool transaction=true true=execute the SQLs in a database transaction, false=execute SQLs outside a transaction 
     * @return bool State of success
     * @throws DatabaseError
     */
    public function commit($transaction=true);

    /**
     * Cancel all uncommitted add/update/delete.
     *
     * @return IModel For method chaining
     */
    public function cancel();

    /**
     * Add model's data. Requires "commit()" to take effect on the database.
     *
     * @param  bool exists=false true=do not insert data if data exists
     * @return IModel For method chaining
     */
    public function add($exists=false);

    /**
     * Update model's data. Requires "commit()" to take effect on the database.
     * To update data you have to either set model's key(pk) to a value that would mean
     * "UPDATE ... WHERE key=value" or pass conditions to this method manually.
     *
     * @param  string        conditions=null      The condition string. [@Model.condition-str]
     * @param  array|[mixed] conditionParams=null Condition params {param: val,} | [val,] | val1, val2,... [@Model.condition-params]
     * @return IModel For method chaining
     */
    public function update();

    /**
     * Delete model data. If model has dependencies, dependencies will be removed too.
     * Requires "commit()" to take effect on the database. To delete data you have to
     * either set model's key(pk) to a value that would mean "DELETE ... WHERE key=value"
     * or pass conditions to this method manually.

     * @param  string        conditions=null      The condition string. [@Model.condition-str]
     * @param  array|[mixed] conditionParams=null Condition params {param: val,} | [val,] | val1, val2,... [@Model.condition-params]
     * @return IModel For method chaining
     */
    public function delete();

    /**
     * Queries data from the database to be filled into the model.
     *
     * @param  string view=Model::QUERY_FULL The database view can be "Model::QUERY_SELF" or "Model::QUERY_FULL" "Model::QUERY_L1"
     * @return IModel For method chaining
     */
    public function query($view=null);

    /**
     * Number of queried data rows.
     *
     * @return int Number of rows
     */
    public function count();

    /**
     * Set conditions for querying data. Removed previously set conditions.
     *
     * @param  string        conditions=null      The condition string. [@Model.condition-str]
     * @param  array|[mixed] conditionParams=null Condition params {param: val,} | [val,] | val1, val2,... [@Model.condition-params]
     * @return IModel For method chaining
     */
    public function where();

    /**
     * Add conditions for querying data. Adds to previously set conditions.
     *
     * @param  string        conditions=null      The condition string. [@Model.condition-str]
     * @param  array|[mixed] conditionParams=null Condition params {param: val,} | [val,] | val1, val2,... [@Model.condition-params]
     * @return IModel For method chaining
     */
    public function extendWhere();

    /**
     * Sort query data by property names. e.g. "$model->sort('Group.id ASC', 'id DESC')->query();"
     *
     * @param  [string] properties Can be name of a model's data property e.g. "name" or
     *         if a property is an IModel object then the name of the model and it's property to sort on e.g.
     *         "Group.id" and so on... "ASC" and "DESC" may be added to each item
     * @return IModel For method chaining
     */
    public function sort();

    /**
     * Limit query result the same way as SQL LIMIT OFFSET. e.g. "$model->limit(20, 21)->query();"
     *
     * @param  int limit=10  Max number of rows to query
     * @param  int $offset=0 Row offset to start from (first row = 0)
     * @return IModel For method chaining
     */
    public function limit($limit=10, $offset=0);

    /**
     * Limit query result between two rows including the rows themselves. e.g. "$model->between(5, 10)->query();"
     *
     * @param  int from=1 From row number (first row = 0)
     * @param  int to=10  To row number (first row = 0)
     * @return IModel For method chaining
     */
    public function between($from=0, $to=10);

    /**
     * Limit query result to the first n rows. e.g. "$model->top(10)->query();"
     *
     * @param  int top=10 Top n rows
     * @return IModel For method chaining
     */
    public function top($top=10);

    /**
     * Slice query result into pages. Use this for querying a paging and producing paging links.
     * e.g. "$model->slice($configs)->query();". After query get paging links from. "$model->__page__->render();"
     *
     * @param  array configs Paging configs [@Paging.configs]
     * @return IModel For method chaining
     */
    public function slice($configs);

    /**
     * Translate Model codition to database SQL condition. Mostly for internal uses only.
     *
     * @param  string str            Model condition
     * @param  bool   useTable=false false=use views, true=use tables
     * @return string Database query-able SQL condition string
     */
    public function translateToSql($str, $useTable=false);
}
