<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::       chdir(dirname(__FILE__));
 *
 * ClassID  7702
 * Created  2009/02/18
 * Updated  2013/03/01
 */

//namespace arta;

require_once 'interface/IModel.php';

/**
 * Every data model must inherit from this abstract class directly or indirectly.
 * A class will only be respected as a model if it has a constructor which calls
 * this class's constructor and passes the first argument to it "Model" as:<br/>
 *<pre>
 * public function __construct($params=Null) {
 *     parent::__construct('ModelGroupName', $params);
 * }
 *</pre><br/>
 * The first argument is reserved for model's internal uses.<br/>
 * A model object is a special object, any public property which is not starting and ending
 * with '__' is considered as a data property which define model's data attributes and lets
 * data to be stored and queried.<br.>
 * There are modifier properties which are left public on purpose, this properties
 * always start and end with '__' e.g. "__key__". This properties reduce the model
 * object's encapsulation at a limited level to expose hooks and hacks for model plugins.
 * It is recommended to use methods for further exposures, but you may add this type
 * of properties with trailing and leading '__' for your own uses.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.1.0
 * @since      1.0.0
 * @link       http://artaengine.com/api/Model
 * @see        http://www.artaengine.com/tutorials/models-principle
 * @example    http://artaengine.com/examples/models
 */
abstract class Model implements IModel {
    /**
     * When a model data property is set to null then the property value is ignored
     * by "add", "update", "delete" and "query" methods. To force this methods use
     * a null valued property set it to "Model::N".
     * @see http://artaengin.com
     * @since Artaengine 1.2.5
     */
    const N = 'null';
    /**
     * When querying data set a property to "Model::NN" so that null values for that
     * property would not be included in the data set.
     */
    const NN = 'not null';

    /**
     * When "Model::QUERY_FULL" is passed to method "query" the data for all model's
     * properties at any level will be queried.
     */
    const QUERY_FULL = '_full'; 
    /**
     * When "Model::QUERY_SELF" is passed to method "query" only the data for model's
     * scalar properties and the key property of Model object properties will be queried.
     */
    const QUERY_SELF = '';
    /**
     * When "Model::QUERY_L1" is passed to method "query" only the data for model's
     * scalar properties and Model object properties will be queried.
     */
    const QUERY_L1 = '_1';
    /**
     * When "Model::QUERY_deleted" is passed to method "query" only the data marked
     * as deleted will be queried.
     */
    const QUERY_DELETED = '_deleted';

    /** To address model's scalar data properties to filter an action based on the data property type. */
    const SCALAR = 1;
    /** To address model's Model object data properties to filter an action based on the data property type. */
    const OBJECT = 2; 
    /** To address model's Model object list data properties to filter an action based on the data property type. */
    const OBJECT_LIST = 3;

    /**
     * Database object name of model groups.
     * @var array {model-group-name: database-object-name,}
     */
    public static $modelDbo;
    /** Only used internally by the framework - maps app model classes to a uid {model-class-name: model-id,} */
    public static $__mdic__ = array();
    /** Only used internally by the framework - cached SQL for i18n properties. */
    public static $__langwhere__;
    /** Only used internally by the framework - unique model instance count. */
    public static $__instance__ = 0;
    /**
     * Only used internally by the framework - model optimizing level. Optimization
     * is mainly done by creating a more compact and optimal class(cache) for each model
     * class, this classes are placed in the temp and used under the hood.
     * 0 = use the optimized models, 1 = use the original models, 2 = create optimized models but use the original models.
     */
    public static $__optimize__ = 1;
    /** If model table name should be a pluralized name or underscored lower cased class name. */
    public static $__pluralize__ = true;

    /** Read-only - model group name. */
    public $__group__;
    /* Read-only - a unique id for each IModel instance used for model database transactions. */
    public $__instanceId__;
    /** After querying model data this will be set to a list of data rows. */
    public $__rows__;
    /**
     * Operator to be used between (property=value)s when querying data.
     * @var string AND or OR
     */
    public $__operator__ = 'AND';
    /**
     * Dictionary of the model's data property definitions. This provides a way for
     * utilities and plugins to get a hook to info about model's data properties,
     * and tweak them.
     * Use method "def()" to get this info.
     * @var array [@Model.__def__]
     */
    public $__def__;
    /**
     * Database access.
     * @var IDbAbstract
     */
    public $__dbo__;
    /** Database table which is used to store model data. */
    public $__table__;
    /**
     * false=Model.delete() will not physically remove the data cols from the database
     * but will mark them as deleted and the data will be hidden for queries.
     */
    public $__delete__ = true;
    /**
     * Model's current set locale(s) e.g. This will be used for querying i18n properties
     * by default it is set to user's selected LOCALE. To change use the "setLocale()" method
     * instead of setting this property, this property is left public for utilities and plugins.
     * @var string|array
     */
    public $__locale__;
    /**
     * Read-only - The property(properties) that present a model when. e.g. when model data are shown in
     * a widget the data of this property(properties) are displayed. To explicitly identify
     * one or more properties set "present" to true in their definition otherwise a property
     * will be selected automatically: if a property exists with this names: "name", "description" or
     * "title" otherwise model's key property will be selected.
     * @var array
     */
    public $__present__;
    /**
     * Read-only - key property name - all models must have a key property with
     * unique values which is used as model table primary key at database level.
     */
    public $__key__;
    /** Read-only - model key as foreign key in queries at database level. */
    public $__fk__;
    /** true=methods and utilities must ignore model properties which are objects. */
    public $__ignoreObjects__     = false;
    /** true=methods and utilities must ignore model properties which are object lists. */
    public $__ignoreObjectLists__ = false;
    /** true=assign pure database SQL conditions to "where()". */
    public $__pureSql__           = false;
    /**
     * Instance of class Paging. If "slice()" is called when querying model data
     * a Paging instance will be available through this property which is set to
     * the appropriate values (based on model's query) ad can be used to render
     * the paging links.
     * @var Paging
     */
    public $__page__;
    /**
     * A list of property names. When updating model data, the properties set to null
     * will be ignored however to update property values to null you can either set the
     * property value to "Model::NULL" or add the property name to this array.
     * @var array
     */
    public $__nulls__;
    /**
     * To make all model property values human readable after querying data set this
     * to true. This must only be used to ease displaying model data and no action should
     * be done on such data. e.g. keys are replaced by their values...
     * @see Readable
     */
    public $__readable__ = false;

    private $__topModel__;
    private $__rowsi__       = 0;
    private $__lastActions__ = array();
    private static $__userLocale__;

    static private function setDbIndex($group) {
        $params = Arta::$configs['model:'.$group];
        $group  = strtolower($group);

        if (DEBUG) {
            self::$__optimize__ = isset($params['optimize_debug']) && !$params['optimize_debug']? 1: 2;
        } elseif (!isset($params['optimize']) || $params['optimize']) {
            self::$__optimize__ = 0;
            Arta::$autoload[]   = ARTA_TEMP_DIR.$group;
        }
        // build in process
        if (isset(Arta::$configs['__b'])) {
            self::$__optimize__ = 2;
        }
        if (file_exists($modelDPath=ARTA_TEMP_DIR."$group-dictionary.php")) {
            require $modelDPath;
            self::$modelDbo[$group] = $params['database'];
        }
    }

    /**
     * Get a model's index used in the database views. Each model name is mapped to
     * an index, this index is used instead of table name in the db views. 
     *
     * @param string $group Model group name
     * @param string $model Model name
     * @return string The model index used in views
     */
    public static function getDbIndex($group, $model) {
        if (!isset(self::$__mdic__[$group][$model])) {
            self::setDbIndex($group);
        }
        return self::$__mdic__[$group][$model];
    }

    /**
     * Each model class must call this in it's constructor as "parent::__construct(model-group, $params);".
     * The model's constructor must have an argument "$params" and pass it here.
     *
     * @param string $group=null  Model group name
     * @param array  $params=null Reserved for internal use
     */
    public function __construct($group=null, $params=null) {
        if (!$group) {
            return;
        }

        if (!self::$__instance__) {
            self::setDbIndex($group);
        }

        $this->__group__ = $group = strtolower($group);
        $this->__instanceId__     = 'mi'.(++self::$__instance__);

        if (isset($params['ignore'])) {
            $this->ignore($params['ignore']);
        }

        $tm = null;
        if (isset($params['tm'])) {
            $this->__topModel__ = $tm = $params['tm'];
        }
        $this->__dbo__ = Database::get(self::$modelDbo[$group]);

        if (self::$__optimize__) {
            /* don't optimize the original models debug=off & optimize=on (default) */
            include 'model/construct.php';
        } else {
            /* use the cached models, don't optimize the original models debug=off & optimize=on (default) */
            if (!$this->__ignoreObjects__) {
                $msg['tm'] = get_class($this);
                foreach ($this->__def__[1] as $col => &$def) {
                    $this->$col      = $mObj = new $def['class']($msg);
                    $def['fk']       = $mObj->__fk__.$def['i'];
                    $def['name']     = strtolower(get_class($mObj));
                    $def['class_no'] = self::$__mdic__['m'][$def['class']];
                    $def['table']    = $mObj->__table__;
                }
            }
        }

        if ($this->__def__[3] && !self::$__userLocale__) {
            if (defined('LOCALE') || Localization::setupGettext()) {
                self::$__userLocale__ = LOCALE;
            }
        }

        $this->__locale__ = self::$__userLocale__;
    }

    /**
     * Reset model instancing state.
     *
     * @param string|int $keyVal=null If provided, after resetting the model's key property will be set to this value
     * @return IModel For $method chaining
     */
    public function reset($keyVal=null) {
        $this->__locale__ = self::$__userLocale__;
        unset($this->__select__, $this->__groupBy__, $this->__where__, $this->__sort__, $this->__join__, $this->__iterate__);

        foreach ($this->__def__[0]+$this->__def__[3] as $col => $def) {
            $this->$col = null;
        }

        if ($this->__ignoreObjects__) {
            foreach ($this->__def__[1] as $col => $def) {
                $this->$col = null;
            }
        } else {
            $msg['tm'] = get_class($this);
            foreach ($this->__def__[1] as $col => &$def) {
                $this->$col  = $mObj = new $def['class']($msg);
                $def['fk']   = $mObj->__fk__.$def['i'];
                $def['name'] = strtolower(get_class($mObj));
                if (isset(self::$__mdic__[$this->__group__][$def['class']])) {
                    $def['class_no'] = self::$__mdic__[$this->__group__][$def['class']];
                }
                $def['table'] = $mObj->__table__;
            }
        }

        foreach ($this->__def__[2] as $col => $def3) {
            $this->$col = null;
        }

        if ($keyVal) {
            $this->{$this->__key__} = $keyVal;
        }

        return $this;
    }

    /**
     * Get the appropriate label for a data-property, method or object-path relative
     * to the current model instance. Always use this method to get a label instead
     * of going other ways. This method traverses the object-path and returns the first
     * label found for it. Object-path e.g. "User.Group.name.".
     * @since Artaengine 1.2.2
     *
     * @param string $pm Model property-name, method-name or object-path to get label for
     * @return string The label
     */
    public function getLabel($pm) {
        if (method_exists($this, 'labels')) {
            $labels = $this->labels($this->__locale__);
            if (isset($labels[$pm])) {
                return $labels[$pm];
            }
        }

        if (property_exists($this, $pm)) {
            $label = $this->propertyDefs($pm, 'label', null);
            if ($label !== null) {
                return _($label);
            }
            if (is_object($this->$pm)) {
                return $this->$pm->getLabel($this->$pm->__present__[0]);
            }
            if (isset($this->__def__[2][$pm])) {
                $class = $this->propertyDefs($pm, 'class');
                $pm    = new $class;
                return $pm->getLabel($pm->__present__[0]);
            }
            return _($pm);
        }

        if (strpos($pm, '.') !== false) {
            $pm = explode('.', $pm, 2);
            return $this->$pm[0]->getLabel($pm[1]);
        }
        return _($pm);
    }

    /**
     * Get a list of model data properties.
     *
     * @return array [property-name,]
     */
    public function properties() {
        return array_keys($this->propertyDefs());
    }

    /**
     * Get a dictionary of all model data properties and their definitions. Arguments
     * can be passed to filter the result.
     *
     * @param string $property=null null=return all, property-name=only return info for this property
     * @param string $key=null      Return the value of a certain key in property's definition array
     * @param string $default=null  If key not exists in the array then return this value instead
     * @return mixed not-array=value of a property definition key, array=[@Model.propertyDefs.return]
     */
    public function propertyDefs($property=null, $key=null, $default=null) {
        $defs = $this->__def__[0] + $this->__def__[3] + $this->__def__[1] + $this->__def__[2];
        if ($property) {
            $defs = $defs[$property];
        }
        return $key? (isset($defs[$key])? $defs[$key]: $default): $defs;
    }

    /**
     * Ignore fetching model object property data after a query. If you don't use data properties
     * which are objects or lists then ignoring them may improve performance.
     *
     * @param const $item=null Property types to ignore: can be "Model::OBJECT" or "Model::OBJECT_LIST" or
     *        "Model::OBJECT+Model::OBJECT_LIST" or null=undo ignore
     * @return IModel For method chaining
     */
    public function ignore($item=null) {
        $this->__ignoreObjects__     = $item === 2 || $item === 5;
        $this->__ignoreObjectLists__ = $item === 3 || $item === 5;
        return $this;
    }

    /**
     * Set model locale(s). A data property will be localized if "i18n" is set to true
     * in it's definition. Query and actions on this properties will be locale aware.
     * By default the model locale is set to user's selected LOCALE, you can manually
     * change it with this method. If you set an array of locales then the data will be
     * queried for all those locales. Same thing is valid for other actions.
     * All app's available locales="Arta::$globals['locales']", users locale="LOCALE".
     *
     * @param string|array|[string] $locale=null Locale(s) e.g. "en-US", "fa-IR"
     * @return IModel For method chaining
     */
    public function setLocale($locale) {
        $this->__locale__ = func_num_args() > 1? func_get_args(): $locale;
        return $this;
    }

    /**
     * Get the data of model's presenter property(s). Each model can be presented by
     * one or more properties. Presenting means when in a form or table a list or column
     * is going to show a model's data as an object of another model then which property(s)'s
     * data must be displayed.
     *
     * @param string $sep=null Separator. string=return a string of data glued with this string
     * @return array|string {property-name: data,} | "data" or "data1SEPdata2SEPdata3"
     */
    public function present($sep=null) {
        $vals = array();
        foreach ($this->__present__ as $col) {
            $vals[$col] = $this->$col;
        }
        return $sep? ($vals? implode($sep, $vals): null): $vals;
    }

    /**
     * Get the value of model's key property and set the value to the property.<br/>
     * if model's key property is already set to a value return it.<br/>
     * if model's key property is serial(auto inc) and newKey=true return the last inserted value<br/>
     * query the model based on set property values and set conditions and return the key values(s)
     *
     * @param bool $newKey=true true=if key type is SERIAL return the last inserted key value
     * @return mixed|array Value or an array of values (if a query is done and result has more than one rows)
     */
    public function keyVal($newKey=true) {
        $pk = $this->__key__;
        if ($this->$pk) {
            return $this->$pk;
        }
        /* if after insert & serial */
        $m1 = $this->__def__[0];
        if ($newKey && (!isset($m1[$pk]['type']) || $m1[$pk]['type'] === SERIAL || $m1[$pk]['type'] === SERIAL_LONG)) {
            return $this->$pk=$this->__dbo__->serial($this->__table__, $pk);
        }
        /* query */
        $this->query(self::QUERY_SELF);
        if ($this->__rows__) {
            if (count($rows=$this->__rows__) === 1) {
                return $this->$pk = $rows[0][$pk];
            }
            $pks = array();
            foreach ($rows as $row) {
                $pks[] = $row[$pk];
            }
            return $this->$pk = $pks;
        }
        return $this->$pk = null;
    }

    /**
     * Make and/or where from self properties. translate: dont translate class
     */
    protected function __conditionMaker($translate=true, $store=false) {
        $wStr = $wArray = array();

        foreach ($this->__def__[0]+$this->__def__[3] as $col => $def) {
            if ($val=self::midware($this->$col, $def, 2)) {
                if (is_array($val)) {
                    if ($store) {
                        continue;
                    }
                    $wStr[] = "$col IN ($$col)";
                    $wArray[$col] = $val;
                } elseif (strtolower($val) === self::N) {
                    $wStr[] = "$col IS NULL";
                } elseif (strtolower($val) === self::NN) {
                    $wStr[] = "$col IS NOT NULL";
                } elseif ($val === true || $val === false) {
                    $wStr[] = $this->__dbo__->toBool($val);
                } else {
                    $wStr[] = "$col = $$col";
                    $wArray[$col] = $val;
                }
            }
        }

        foreach ($this->__def__[1] as $col => $def) {
            if ($that=$this->$col) {
                $fk = isset($def['crawl']) || !$translate? $def['fk']: ('c' . $def['class_no'] . '_' . $that->__key__ . $def['i']);

                if (is_object($that)) {
                    if (!($val=$that->{$that->__key__})) {
                        continue;
                    }
                    if (strtolower($val) === self::N) {
                        $where = " IS NULL";
                    }
                    if (strtolower($val) === self::NN) {
                        $where = " IS NOT NULL";
                    } else {
                        $where = is_array($val)? " IN ($$fk)": " = $$fk";
                        $wArray[$fk] = $val;
                    }
                } else {
                    $where       = is_array($that)? " IN ($$fk)": " = $$fk";
                    $wArray[$fk] = $that;
                }
                $wStr[] = $fk.$where;
            }
        }

        foreach ($this->__def__[2] as $col => $def) {
            if (($vals=$this->$col) && ($w=$this->__objectList('q', $vals, $def))) {
                $wStr[] = $w[0];
                $wArray = $wArray+$w[1];
            }
        }

        return array(implode(" $this->__operator__ ", $wStr), $wArray, 3);
    }

    /**
     * Object list manager for insert/query - used by this.add & this.update & this.query
     */
    private function __objectList($action, $vals, $def, $thisRef=null) {
        $dbo     = $this->__dbo__;
        $pk      = $def['obj']['pk'];
        $fk      = $def['obj']['fk'];
        $table   = $def['table'];
        $newVals = array();
        $vals    = (array)$vals;
        unset($vals['__add__'], $vals['__delete__'], $vals['__update__']);

        foreach ($vals as $val) {
            if (is_object($val)) {
                $val = $val->$pk;
            } elseif (is_array($val)) {
                if (is_object($juncion=isset($val[1])? $val[1]: null)) {
                    $newVals = $juncion->values();
                } elseif (is_array($juncion)) {
                    $newVals = $juncion;
                }
                if (is_object($val=$val[0])) {
                    $val = $val->$pk;
                }
            }

            if ($action === 'q') {
                if ($val) {
                    $newVals[$fk][] = $val;
                }
                continue;
            }

            if ($val && $action === 'Add') {
                $newVals[$fk] = $val;
                if ($thisRef) {
                    $dbo->insert($table, $newVals, $thisRef);
                } else {
                    foreach ((array)$this->{$this->__key__} as $keyVal) {
                        $newVals[$this->__fk__] = $keyVal;
                        $dbo->insert($table, $newVals);
                    }
                }
            }
        }

        if ($action === 'q') {
            if (!$newVals || !$newVals[$fk]) {
                return false;
            }
            return array(
                "($this->__key__ IN (SELECT $this->__fk__ FROM $table WHERE $fk IN ($$fk)))",
                array($fk => $newVals[$fk])
            );
        }

        return true;
    }
 
    /**
     * Object list update/delete - used by this.update
     */
    private function __objectListUD($action, $vals, $def, $wStr, $wArray, $byVal) {
        $fk = $def['obj']['fk'];
        foreach ($vals as $old => $new) {
            $val = $action === 'Update'? $old: $new;
            $wArray[$fk] = $val;
            $wStrX = $byVal? array($this->__fk__ => $this->{$this->__key__}, $fk => $val): "$wStr AND $fk = $$fk";
            $action === 'Update'?
                $this->__dbo__->update($def['table'], array($fk => $new), $wStrX, $wArray):
                $this->__dbo__->delete($def['table'], $wStrX, $wArray);
        }
        return true;
    }

    /**
     * A midware can be set or each data property (setting "midware-classname" to the "midware" key in the property's definitions).
     * A midware class may contain static methods "afterNext", "beforeStore" and "beforeQuery", when the event happens on the property
     * the appropriate midware method (if exists) will be called passing property-value and using the returned value instead.
     * This method is used internally to check for a midware, pass and get the values however it is left public to be freely used.
     * @since Artaengine 1.2.0
     *
     * @param mixed $value  Property value to be passed to the midware method
     * @param array $def    Property definition
     * @param int   $type=0 Mid-ware method to be called, 0=afterNext() 1=beforeStore() 2=beforeQuery()
     * @return mixed The value returned by the midware 
     */
    public static function midware($value, $def, $type=0) {
        if (isset($def['midware'])) {
            if ($type === 0) {
                if (method_exists($def['midware'], 'afterNext')) {
                    /* PHP5.3 > */
                    return $def['midware']::afterNext($value);
                    /* PHP5.2 */
                    // return call_user_func(array($def['midware'], 'afterNext'), $value)
                }
            } elseif ($type === 1) {
                if (method_exists($def['midware'], 'beforeStore')) {
                    /* PHP5.3 */
                    return $def['midware']::beforeStore($value);
                    /* PHP5.2 */
                    // return call_user_func(array($def['midware'], 'beforeStore'), $value);
                }
            } elseif ($type === 2) {
                if (method_exists($def['midware'], 'beforeQuery')) {
                    /* PHP5.3 */
                    return $def['midware']::beforeQuery($value);
                    /* PHP5.2 */
                    // return call_user_func(array($def['midware'], 'beforeQuery'), $value);
                }
            }
        }
        return $value;
    }

    /**
     * Commit model data manipulations to the database. Actions add/update/delete are
     * performed on the database only after calling this method.
     *
     * @param bool $transaction=true true=execute the SQLs in a database transaction, false=execute SQLs outside a transaction
     * @return bool State of success
     * @throws DatabaseError
     */
    public function commit($transaction=true) {
        $this->__nulls__ = null;
        $ok = $this->__dbo__->commit($transaction, $this->__instanceId__);
        if ($this->__lastActions__) {
            foreach ($this->__lastActions__ as $lastAction) {
                $method = "after$lastAction[0]";
                $this->$method($ok, $lastAction[1]);
            }
            $this->__lastActions__ = array();
        }
        return $ok;
    }

    /**
     * Cancel all uncommitted add/update/delete.
     *
     * @return IModel For method chaining
     */
    public function cancel() {
        $this->__lastActions__[] = array();
        $this->__dbo__->rollback($this->__instanceId__);
        return $this;
    }

    /**
     * Add model's data. Requires "commit()" to take effect on the database.
     *
     * @param bool $exists=false true=do not insert data if data exists
     * @return IModel For method chaining
     */
    public function add($exists=false) {
        if (method_exists($this, 'beforeAdd') && $this->beforeAdd() === false) {
            return $this;
        }
        if (method_exists($this, 'afterAdd')) {
            $this->__lastActions__[] = array('Add', Binder::pull($this));
        }

        $nulls   = is_array($this->__nulls__)? $this->__nulls__: array();
        $newVals = array();
        /* self attributes */
        foreach ($this->__def__[0] as $col => $def) {
            if (!isset($nulls[$col]) && ($val=self::midware($this->$col, $def, 1)) !== null) {
                $newVals[$col] = strtolower($val=is_array($val)? serialize($val): $val) === self::N? null: $val;
            }
        }
        /* object attributes */
        foreach ($this->__def__[1] as $col => $def) {
            if (!isset($nulls[$col]) && ($that=$this->$col) !== null && ($val=is_object($that)?
                $that->{$that->__key__}: (is_array($that)? null: $that))) {
                $newVals[$def['fk']] = strtolower($val)===self::N? null: $val;
            }
        }
        /* add to dbo execute/transaction que */
        $dbo = $this->__dbo__;
        $dbo->__activeQue__ = $this->__instanceId__;
        $table = $this->__table__;
        if ($exists && $dbo->exists($table, $newVals)) {
            return $this;
        }
        $dbo->insert($table, $newVals);
        /* locale/object list */
        if (($m3=$this->__def__[2]) || $this->__def__[3]) {
            $thisRef[$this->__fk__] = $dbo->serialSequence($table, $this->__key__);
            /* object list */
            if ($m3) {
                foreach ($m3 as $col => $def) {
                    if (!isset($nulls[$col]) && ($vals=$this->$col)) {
                        $this->__objectList('Add', $vals, $def, $thisRef);
                    }
                }
            }
            /* locale */
            if ($m4=$this->__def__[3]) {
                foreach ((array)$this->__locale__ as $locale) {
                    $newVals = array();
                    foreach ($m4 as $col => $def) {
                        if (!isset($nulls[$col]) && ($val=self::midware($this->$col, $def, 1)) !== null) {
                            $newVals[$col] = is_array($val)?
                                (isset($val[$locale])? $val[$locale]: null):
                                (strtolower($val)===self::N? null: $val);
                        }
                    }
                    $newVals['lang'] = $locale;
                    $dbo->insert($table.'_lang', $newVals, $thisRef);
                }
            }
        }
        return $this;
    }

    /**
     * Update model's data. Requires "commit()" to take effect on the database.
     * To update data you have to either set model's key(pk) to a value that would mean
     * "UPDATE ... WHERE key=value" or pass conditions to this method manually.
     *
     * @param string        $conditions=null      The condition string. [@Model.condition-str]
     * @param array|[mixed] $conditionParams=null Condition params {param: val,} | [val,] | val1, val2,... [@Model.condition-params]
     * @return IModel For method chaining
     */
    public function update() {
        if (method_exists($this, 'beforeUpdate') && $this->beforeUpdate() === false) {
            return $this;
        }
        if (method_exists($this, 'afterUpdate')) {
            $this->__lastActions__[] = array('Update', Binder::pull($this));
        }

        $nulls   = is_array($this->__nulls__)? $this->__nulls__: array();
        $newVals = array();
        /* self attributes */
        foreach ($this->__def__[0] as $col => $def) {
            if (($val=isset($nulls[$col])? self::N: self::midware($this->$col, $def, 1)) !== null) {
                $newVals[$col] = strtolower($val=is_array($val)? serialize($val): $val) === self::N? null: $val;
            }
        }
        /* object attributes */
        foreach ($this->__def__[1] as $col => $def) {
            $that = $this->$col;
            $val = isset($nulls[$col])? self::N: (is_object($that)? $that->{$that->__key__}: (is_array($that)? null: $that));

            if ($that !== null && $val) {
               $newVals[$def['fk']] = strtolower($val) === self::N? null: $val;
            }
        }
        /* conditions */
        $wStr = null;
        $wArray = null;
        $args = func_get_args();
        if ($args && ($w=$this->__argstoSql($args))) {
            list($wStr, $wArray) = $w;
        } elseif (!($keyVal=$this->{$this->__key__})) {
            return $this;
        }
        /* add to dbo execute/transaction que */
        $dbo = $this->__dbo__;
        $dbo->__activeQue__ = $this->__instanceId__;
        $table = $this->__table__;
        if ($byKey=isset($keyVal)) {
            $wStr   = $this->__key__ . (is_array($keyVal)? ' IN ($m_key)': ' = $m_key');
            $wArray = array('m_key' => $keyVal);
            unset($newVals[$this->__key__]);
        }
        if ($newVals) {
            $dbo->update($table, $newVals, $wStr, $wArray);
        }
        /* locale/object list */
        if (($m3=$this->__def__[2]) || $this->__def__[3]) {
            if (!$byKey) {
                if (is_array($wStr)) {
                    list($wStr, $wArray) = $dbo->arrayWhere($wStr, 'AND', 'm_');
                }
                $wStr = "$this->__fk__ IN (SELECT $this->__key__ FROM $table ".($wStr? " WHERE $wStr": '').')';
            }
            /* locale */
            if ($m4=$this->__def__[3]) {
                foreach ((array)$this->__locale__ as $locale) {
                    $newVals = array();
                    foreach ($m4 as $col => $def) {
                        if (!isset($nulls[$col]) && ($this->$col !== null)) {
                            $newVals[$col] = is_array($val=$this->$col)?
                                (isset($val[$locale])? $val[$locale]: null): (strtolower($val)===self::N? null: $val);
                        }
                    }
                    if ($newVals) {
                        $dbo->update(
                            $table.'_lang',
                            $newVals,
                            $byKey?
                                array('lang' => $locale, $this->__fk__ => $wArray['m_key']):
                                ("lang = '" . $dbo->esc($locale) . "' AND $wStr"),
                            $wArray
                        );
                    }
                }
            }
            /* object list - only if key is set */
            foreach ($m3 as $col => $def) {
                if (($vals=isset($nulls[$col])? array(): $this->$col) !== null) {
                    $vals = $vals===self::N? array(): (array)$vals;
                    $x = false;
                    if (isset($vals['__delete__'])) {
                        $x = $this->__objectListUD('Delete', $vals['__delete__'], $def, $wStr, $wArray, $byKey);
                    }
                    if (isset($vals['__update__'])) {
                        $x = $this->__objectListUD('Update', $vals['__update__'], $def, $wStr, $wArray, $keyVal);
                    }
                    if (isset($vals['__add__']) && $byKey) {
                        $x = $this->__objectList('Add', $vals['__add__'], $def);
                    }
                    if (!$x && $byKey) {
                        $dbo->delete($def['table'], array($this->__fk__ => $wArray['m_key']));
                        $this->__objectList('Add', $vals, $def);
                    }
                }
            }
        }

        return $this;
    }

    /**
     * Delete model data. If model has dependencies, dependencies will be removed too.
     * Requires "commit()" to take effect on the database. To delete data you have to
     * either set model's key(pk) to a value that would mean "DELETE ... WHERE key=value"
     * or pass conditions to this method manually.

     * @param string        $conditions=null      The condition string. [@Model.condition-str]
     * @param array|[mixed] $conditionParams=null Condition params {param: val,} | [val,] | val1, val2,... [@Model.condition-params]
     * @return IModel For method chaining
     */
    public function delete() {
        if (method_exists($this, 'beforeDelete') && ($u=$this->beforeDelete()) === false) {
            return $this;
        }
        if (method_exists($this, 'afterDelete')) {
            $this->__lastActions__[] = array('Delete', Binder::pull($this));
        }

        /* conditions */
        $args = func_get_args();
        if ($args && ($w=$this->__argstoSql($args))) {
            list($wStr, $wArray) = $w;
        } elseif ($keyVal=$this->{$this->__key__}) {
            $wStr = $this->__key__.(is_array($keyVal)? ' IN ($m_key)': ' = $m_key');
            $wArray = array('m_key' => $keyVal);
        } else {
            list($wStr, $wArray) = $this->__conditionMaker(false, true);
        }

        $t = $this->__table__;
        $this->__dbo__->__activeQue__ = $this->__instanceId__;

        if (isset($this->__delete__) && !$this->__delete__) {
            $newVals = isset($u) && is_array($u)? $u: array();
            $newVals['!deleted'] = 'NOW()';
            $this->__dbo__->update($t, $newVals, $wStr, $wArray);
        } else {
            if ($wStr) {
                foreach ($this->__def__[2] as $def) {
                    $this->__dbo__->delete(
                        $def['table'],
                        "$this->__fk__ IN (SELECT $this->__key__ FROM $t".($wStr? " WHERE $wStr": '').")",
                        $wArray
                    );
                }
                if ($this->__def__[3]) {
                    $this->__dbo__->delete(
                        $t.'_lang',
                        "$this->__fk__ IN (SELECT $this->__key__ FROM $t".($wStr? " WHERE $wStr": '').")",
                        $wArray
                    );
                }
            }
            $this->__dbo__->delete($t, $wStr, $wArray);
        }

        return $this;
    }

    /**
     * Queries data from the database to be filled into the model.
     *
     * @param string $view=Model::QUERY_FULL The database view can be "Model::QUERY_SELF" or "Model::QUERY_FULL" "Model::QUERY_L1"
     * @return IModel For method chaining
     */
    public function query($view=null) {
        if (!$view) {
            $view = self::QUERY_FULL;
        }
        $this->__rows__ = null;
        if (method_exists($this, 'beforeQuery') && $this->beforeQuery() === false) {
            return $this;
        }

        /* conditions */
        list($wStr, $wArray) = $this->__conditionMaker($view !== self::QUERY_SELF);
        if (isset($this->__where__)) {
            list($wStrX, $wArrayX) = $this->__where__;
            if ($wStrX) {
                $wStr = $wStr? "($wStr) AND ($wStrX)": "($wStrX)";
                if (is_array($wArrayX)) {
                    $wArray += $wArrayX;
                }
            }
        }
        if (isset(self::$__langwhere__[get_class($this)])) {
            $wStr .= ($wStr? ' AND ': '').self::$__langwhere__[get_class($this)];
            $wArray['_L_'] = !$this->__locale__ || is_array($this->__locale__)? self::$__userLocale__: $this->__locale__;
        }
        /* query */
        $queryStr = "SELECT T.*".
            (isset($this->__select__)? (',' . implode(',', $this->__select__)): '') .
            " FROM v_$this->__table__$view AS T " .
            (isset($this->__join__)? $this->__join__: '') .
            ($wStr? " WHERE $wStr ": '') .
            (isset($this->__groupBy__)? (' GROUP BY ' . implode(', ', $this->__groupBy__)): '') .
            (isset($this->__sort__)? $this->__sort__: '');
        /* page */
        if (isset($this->__page__)) {
            $pData = $this->__dbo__->page($queryStr, $wArray, $this->__page__->getConfigs());
            $this->__page__->count = $pData['count'];
        } else {
            $this->__dbo__->query(
                isset($this->__limit__)?
                    $this->__dbo__->limit($queryStr, $this->__limit__[0], $this->__limit__[1]):
                    $queryStr,
                $wArray
            );
        }
        /* fetch */
        $this->__rows__  = $this->__dbo__->rows();
        $this->__rowsi__ = 0;
        /* object list - why is this needed ?! */
        foreach ($this->__def__[2] as $objName => $objDef) {
            $this->$objName = array();
        }
        /* cleanup */
        if (method_exists($this, 'afterQuery')) {
            $this->afterQuery();
        }

        unset($this->__select__, $this->__groupBy__, $this->__where__, $this->__sort__, $this->__join__, $this->__iterate__);

        return $this;
    }

    /**
     * Fill the current row of the queried data into model's properties and move to next row.
     *
     * @param int $i=null Fill model with the i'th row data, null=current row
     * @return bool true=next row exists
     */
    public function next($i=null) {
        return isset($this->__iterate__)? true: $this->__next($i);
    }

    public function __next($i=null, $row=null, $A=null, $level=0, $maxLevel=null) {
        $msg['tm'] = get_class($this);
        $endLevel = $maxLevel && $level == $maxLevel;

        if ($row === null) {
            if (isset($this->__rows__[$i===null? $i=$this->__rowsi__: $i])) {
                $row = $this->__rows__[$i];
            } else {
                return false;
            }
            ++$this->__rowsi__;
        }

        $cols0 = $this->__def__[0];
        $multiLocale = false;
        if ($this->__def__[3]) {
            if (!($multiLocale=(is_array($this->__locale__) && count($this->__locale__) > 0))) {
                $cols0 += $this->__def__[3];
            }
        }

        foreach ($cols0 as $col => $def) {
            $this->$col = self::midware(array_key_exists($k="$A$col", $row)?
                (isset($def['type']) && $def['type'] === DICTIONARY?
                @unserialize($row[$k]): $row[$k]): null, $def);
        }

        if ($multiLocale) {
            $dbo = $this->__dbo__;
            $dbo->query(
                'SELECT * FROM ' . $this->__table__ . "_lang WHERE $this->__fk__ = $1 AND lang IN ($2)",
                $this->{$this->__key__},
                $this->__locale__
            );

            foreach ($this->__def__[3] as $col => $def) {
                $this->$col = array();
            }
            foreach ($dbo->rows() as $row4) {
                foreach ($this->__def__[3] as $col => $def) {
                    $this->{$col}[$row4['lang']] = $row4[$col];
                }
            }
        }

        if (!$this->__ignoreObjects__ && $this->__def__[1]) {
            foreach ($this->__def__[1] as $col => $def) {
                if (!is_object($that=$this->$col)) {
                    $that = new $def['class']($msg);
                }
                if ($endLevel) {
                    $pk = $that->__key__;
                    $that->$pk = array_key_exists($k="$A$pk", $row)? $row[$k]: null;
                    continue;
                }
                $maxLevelNext = null;
                if (isset($def['crawl'])) {
                    $level = 0;
                    $maxLevelNext = $def['crawl'];
                }
                $that->__next(null, $row, ($A? $A: 'c') . $def['class_no']. ($def['i']? 'i'.$def['i']: '') . '_', $level+1, $maxLevelNext);
            }
        }

        if (!$this->__ignoreObjectLists__ && $this->__def__[2] && !$endLevel) {
            $locale = $this->__locale__ && !is_array($this->__locale__)? $this->__locale__: self::$__userLocale__;
            $dbo    = $this->__dbo__;

            foreach ($this->__def__[2] as $col => $def) {
                $objects = array();
                $dbo->query(
                    "SELECT * FROM $def[view] WHERE $this->__fk__ = $1" . ($def['i18n']? (" AND lang='" . $locale . "'"): ''),
                    $this->{$this->__key__}
                );

                foreach ($dbo->rows() as $row3) {
                    $that = new $def['class']($msg);
                    $that->__next(null, $row3);
                    if ($def['junction']) {
                        $jClass = new $def['junction']();
                        foreach ($jClass->properties() as $jCol) {
                            $jClass->$col = $row3["j_$jCol"];
                        }
                        $objects[] = array($that, $jClass);
                    } else {
                        $objects[] = $that;
                    }
                }
                $this->$col = $objects;
            }
        }

        if ($this->__readable__) {
            MakeReadable::render($this);
        }

        if (method_exists($this, 'afterNext')) {
            $this->afterNext();
        }

        return true;
    }

    /**
     * Number of queried data rows.
     *
     * @return int Number of rows
     */
    public function count() {
        return count($this->__rows__);
    }

    private function __argstoSql($args) {
        if (!$args || !isset($args[0]) || !$args[0] || (is_string($args[0]) && !trim($args[0]))) {
            return null;
        }

        $where[] = is_string($args[0])? $this->translateToSql($args[0]): $args[0];
        unset($args[0]);
        $where[] = $args? (count($args) === 1 && is_array($args[1]) && !isset($args[1][0])? $args[1]: $args): null;
        return $where;
    }

    /**
     * Set conditions for querying data. Removed previously set conditions.
     *
     * @param string        $conditions=null      The condition string. [@Model.condition-str]
     * @param array|[mixed] $conditionParams=null Condition params {param: val,} | [val,] | val1, val2,... [@Model.condition-params]
     * @return IModel For method chaining
     */
    public function where() {
        $args = func_get_args();
        $this->__where__ = $this->__argstoSql($args);
        return $this;
    }

    /**
     * Add conditions for querying data. Adds to previously set conditions.
     *
     * @param string        $conditions=null      The condition string. [@Model.condition-str]
     * @param array|[mixed] $conditionParams=null Condition params {param: val,} | [val,] | val1, val2,... [@Model.condition-params]
     * @return IModel For method chaining
     */
    public function extendWhere() {
        $args  = func_get_args();
        if ($where=$this->__argstoSql($args)) {
            if (isset($this->__where__)) {
                $this->__where__[0] = $this->__where__[0]? '('.$this->__where__[0].')'.$this->__operator__.$where[0]: $where[0];
                if (is_array($where[1])) {
                    if (!is_array($this->__where__[1])) {
                        $this->__where__[1] = array();
                    }
                    $this->__where__[1] += $where[1];
                }
            } else {
                $this->__where__ = $where;
            }
        }
        return $this;
    }

    /**
     * Sort query data by property names. e.g. "$model->sort('Group.id ASC', 'id DESC')->query();"
     *
     * @param [string] properties Can be name of a model's data property e.g. "name" or
     *        if a property is an IModel object then the name of the model and it's property to sort on e.g.
     *        "Group.id" and so on... "ASC" and "DESC" may be added to each item
     * @return IModel For method chaining
     */
    public function sort() {
        $cols = func_get_args();
        foreach ($cols as &$v) {
            if (is_array($v)) {
                $v = implode(',', $v);
            }
        }
        $this->__sortR__ = $this->translateToSql(implode(',', $cols));
        $this->__sort__ = ' ORDER BY ' . $this->__sortR__;
        return $this;
    }

    /**
     * Limit query result the same way as SQL LIMIT OFFSET. e.g. "$model->limit(20, 21)->query();"
     *
     * @param int $limit=10  Max number of rows to query
     * @param int $offset=0 Row offset to start from (first row = 0)
     * @return IModel For method chaining
     */
    public function limit($limit=10, $offset=0) {
        $this->__limit__ = array($limit, $offset);
        return $this;
    }

    /**
     * Limit query result between two rows including the rows themselves. e.g. "$model->between(5, 10)->query();"
     *
     * @param int $from=1 From row number (first row = 0)
     * @param int $to=10  To row number (first row = 0)
     * @return IModel For method chaining
     */
    public function between($from=0, $to=10) {
        $this->__limit__ = array($to-$from, $from);
        return $this;
    }

    /**
     * Limit query result to the first n rows. e.g. "$model->top(10)->query();"
     *
     * @param int $top=10 Top n rows
     * @return IModel For method chaining
     */
    public function top($top=10) {
        return $this->limit($top, 0);
    }

    /**
     * Slice query result into pages. Use this for querying a paging and producing paging links.
     * e.g. "$model->slice($configs)->query();". After query get paging links from. "$model->__page__->render();"
     *
     * @param array $configs Paging configs [@Paging.configs]
     * @return IModel For method chaining
     */
    public function slice($configs) {
        $this->__page__ = new Paging($configs);
        return $this;
    }

    /**
     * Experimental, to inject JOINs to model's query. Sometimes joins are more
     * efficient than conditions.
     * @param string $sql Joins
     * @return IModel For method chaining
     */
    public function join($sql) {
        $this->__join__ = $sql;
        return $this;
    }

    /**
     * Experimental, to add extra columns into the model's query SELECT.
     * @param array $cols Column list
     * @return IModel For method chaining
     */
    public function select($sql) {
        $this->__select__ = $sql;
        return $this;
    }

    /**
     * Experimental, to hack a GROUP BY statement into model's query.
     * @param array $cols GROUP BY column list
     * @return IModel For method chaining
     */
    public function groupBy($sql) {
        $this->__groupBy__ = $sql;
        return $this;
    }

    /**
     * Translate Model codition to database SQL condition. Mostly for internal uses only.
     *
     * @param string $str            Model condition
     * @param bool   $useTable=false false=use views, true=use tables
     * @return string Database query-able SQL condition string
     */
    public function translateToSql($str, $useTable=false) {
        if ($this->__pureSql__) {
            return $str;
        }

        $str = ' '.$str;
        foreach ($this->__def__[2] as $col => $def) {
            if (strpos($str, $col) !== false) {
                preg_match_all("/(\s+){$col}[^\)]+/", $str, $res);
                if (isset($res[0][0])) {
                    $str = str_replace(
                        $res[0][0],
                        "$this->__key__ IN (SELECT $this->__fk__ FROM $def[table] WHERE " . $def['obj']['fk'] . ' IN ' .
                            preg_replace("/(\s+){$col}[^\(]+/", '', $res[0][0]).')',
                        $str
                    );
                }
            }
        }

        require_once ARTA_TEMP_DIR . $this->__group__ . '-sql-t.php';
        // tc2db() and cp2db() are application specific and created by arta builder and put inside model dic file
        return $useTable? tc2db($str): cp2db($str);
    }

    /**
     * Iterator - valid. (only to be called by iterator, never call this method)
     * @return bool true=valid row
     */
    public function valid() {
        if ($valid=isset($this->__rows__[$this->__rowsi__])) {
            $this->__iterate__ = true;
        } else {
            unset($this->__iterate__);
        }
        return $valid;
    }

    /**
     * Iterator - key.
     * @return int Row number
     */
    public function key() {
        return $this->__rowsi__-1;
    }

    /**
     * Iterator - current.
     * @return IModel For method chaining
     */
    public function current() {
        $this->__next();
        return $this;
    }
 
    /**
     * Iterator - rewind.
     * @return void
     */
    public function rewind() {
        $this->__rowsi__ = 0;
    }

    /**
     * Check if a model property value is null or not. Returns true if property is set to "Model::N".
     *
     * @param string property Model property name
     * @return bool true=is null or not
     */
    public function isnull($property) {
        return $property === null || $property === self::N;
    }

    /**
     * Shuffle query result. e.g. "$model->query()->shuffle();"
     *
     * @return IModel For method chaining
     */
    public function shuffle() {
        shuffle($this->__rows__);
        return $this;
    }

    /**
     * Trim queried result. Cut off rows from the start or/and end of the result.
     * e.g. "$model->query()->trim(5, 5);"
     *
     * @param int $start Trim all rows before this row number (zero based)
     * @param int $end   Trim all rows after this row number (zero based)
     * @return IModel For method chaining
     */
    public function trim($start, $end=null) {
        $this->__rows__ = $end? array_slice($this->__rows__, $start, $end): array_slice($this->__rows__, $start);
        return $this;
    }

    /**
     * Search for a keyword inside all scalar properties. Adds a condition to
     * the current query for searching a keyword against all scalar properties.
     * e.g. "$model->has("keyword")->query();"
     *
     * @param string      $keyword       The keyword to be searched for
     * @param bool        $equals=false  false=use "LIKE" operator true=use "=" operator
     * @param const|array $type=null     To restrict to properties with a specific data type e.g. T::STRING or [T::STRING, T::EMAIL]
     * @param string      $op=OR Search operator, "OR"=filter any row with a property match "AND"=filter rows with all properties matching
     * @return IModel For method chaining
     */
    public function has($keyword, $equals=false, $type=null, $op='OR') {
        $keyword = $this->__dbo__->esc($keyword);
        $where   = array();
        $o       = 'LIKE';
        $q       = '%';

        if ($equals) {
            $o = '=';
            $q = '';
        }
        if ($type && !is_array($type)) {
            $type = array($type);
        }

        foreach ($this->__def__[0]+$this->__def__[3] as $col => $def) {
            $colType = isset($def['type'])? $def['type']: (isset($def['key']) && $def['key']? T::INT: T::STRING);
            if (!$type || in_array($colType, $type)) {
                $where[] = "$col $o '$q$keyword$q'";
            }
        }

        if (isset($this->__where__)) {
            $where = implode(" $op ", $where);
            $this->__where__[0] = $this->__where__[0]? '('.$this->__where__[0].') AND ('.$where.')': $where;
        } else {
            $this->__where__ = array(implode(" $op ", $where), array());
        }

        return $this;
    }

    /**
     * Dumps model data properties.
     *
     * @param bool $print=true false=returns an array of request
     * @return void|array If print=false returns an array of request vars
     */
    public function inspect($print=true) {
        $var = Binder::pull($this, array('full' => true));
        if ($print) {
            I($var);
        }
        return Inspect::dumpText(array($var), false, false);
    }

    public function __toString() {
        return '' . $this->inspect(false);
    }
}
