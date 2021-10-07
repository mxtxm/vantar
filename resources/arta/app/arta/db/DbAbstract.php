<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7201
 * Created  2013/03/17
 * Updated  2013/03/17
 */

//namespace arta\db;

require_once ARTA_DIR . 'interface/IDbAbstract.php';

/**
 * Includes common methods for the abstract database access classes.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.0.0
 * @link       http://artaengine.com/api/PostgreSqlAbstract
 * @example    http://artaengine.com/examples/database
 */
abstract class DbAbstract implements IDbAbstract {

    /**
     * Holds SQL queues and their SQLs, SQLs inside each queue can to executed in
     * a transaction or at once. Thought this property is for managed by the methods 
     * related to queues, it is made public to be hackable.
     * @var array [queue-index: [SQL,],]
     */
    public $__que__ = array();

    /**
     * Identifies the active queue index. The queue related methods (queue add, commit, etc. SQLs)
     * will perform their action on the active queue.
     * @var string
     */
    public $__activeQue__ = 'default';

    protected $__configs = array(); // connection configs
    protected $__queryStr;           // query str
    protected $__queryT;             // transaction query strs
    protected $__queryP;             // query params
    protected $__row;

    /**
     * Get info about the connection and database engine.
     *
     * @return string Info about the connection and database engine
     */
    public function __toString() {
        return Inspect::dumpText($this->info(), false, true);
    }

    /**
     * Get connection configs.
     *
     * @return array Connection configs. [@DbAbstract.connection]
     */
    public function connection() {
        return $this->__configs;
    }

    /**
     * Connect to a database.
     *
     * @param array $configs Connection configs. [@DbAbstract.connection]
     */
    public function __construct($configs) {
        $this->connect($configs);
    }

    /**
     * To query a page of data. A page is a subset of the result consisting of a
     * max_number or rows, each page is addressed by a number starting from 1.
     *
     * @param  string        $sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param  array|[mixed] $params=null Params values [@DbAbstract.sql-params]
     * @param  array         $configs     Page configs. [@DbAbstract.page.configs]
     * @return array This array can be fed to an instance of "Paging" to get the paging links [@DbAbstract.page.return]
     * @throws DatabaseError
     */
    public function page() {
        $args   = func_get_args();
        $params = array_pop($args);

        if (!is_array($params)) {
            $params = array('page' => (int)$params);
        }

        $sqlStr = array_shift($args);
        if ($args) {
            $sqlStr = $this->__pquery($sqlStr, $args);
        }

        $page = isset($params['page'])? (int)$params['page']: 1;

        $params['max_rows'] = $maxRows = isset($params['max_rows'])? (int)$params['max_rows']: 30;
        $sqlStrC = explode('ORDER BY', $sqlStr);
        $params['count'] = $count = isset($params['count'])? (int)$params['count']:
            $this->count(isset($params['count_sql'])? $params['count_sql']: $sqlStrC[0]);

        for (; $page > 1 && ((($page-1)*$maxRows) >= $count); $page--);

        if ($page < 1) {
            $page = 1;
        }

        $sqlStr = rtrim($sqlStr, ';')." LIMIT $maxRows OFFSET ".(($page-1)*$maxRows);

        if (isset($params['exec']) && !$params['exec']) {
            $params['exec'] = false;
        } else {
            $this->query($sqlStr);
            $params['exec'] = true;
        }
        $params['sqlstr'] = $sqlStr;
        $params['page']   = $page;

        return $params;
    }

    /**
     * Get data of a column. You can get the data immediately after a query, if
     * no row has been fetched yet, this will fetch a row to get the column data.
     *
     * @param string $col Column name
     * @return mixed  Data
     */
    public function __get($col) {
        if (!($row=$this->__row)) {
            $row = $this->row();
        }
        return is_array($row)? $row[$col]: (is_object($row)? $row->$col: null);
    }

    /**
     * Inspect/debug the query and the transaction queue.
     *
     * @param bool $printI=true true=print the debug info and die, false=return the debug info
     * @return void|string|array Debug info or nothing based on arguments.
     */
    public function inspect($print=true) {
        return $this->__inspect($print, null, true);
    }

    protected function __inspect($print, $error=null, $showConfigs=false) {
        $vals = array(
            'Last query'   => array($this->__queryStr, $this->__queryP),
            'SQL queues'   => $this->__que__,
            'Active queue' => $this->__activeQue__,
            'Last commit'  => $this->__queryT,
            'Connection'   => $showConfigs? $this->__configs: null,
            'Error'        => $error,
        );
        if ($print) {
            include_once ARTA_DIR . 'Inspect.php';
            Inspect::dumpDB($vals, true);
        }
        return $vals;
    }

    /**
     * Create a new SQL queue and set it as the active queue. You can have
     * one or more queues, you can make a queue active by method "toggle()"
     * and add SQL statements to the active queue using the ("add()", "update(), insert()"
     * and "delete()") methods and finally rollback or commit a queue as a database
     * transaction or one by one. If you do not need more than one queues
     * you do not have to use this method and toggle, simply use the action methods
     * and when it's call "commit()" or "rollback()".
     *
     * @param  string $q='default' Queue ID. You can use this ID to address a specific queue,
     *         if you never created new queues then there is only one queue named "default"
     * @return IDbAbstract For method chaining
     */
    public function begin($q='default') {
        $this->__que__[$this->__activeQue__=$q] = array();
        return $this;
    }

    /**
     * Rollback, cancel and remove a queue.
     *
     * @param  string $q=null Queue ID to be canceled, null=cancel the active queue
     * @return IDbAbstract For method chaining
     */
    public function rollback($q=null) {
        if (!$q) {
            $q = $this->__activeQue__;
        }

        if ($q === 'default') {
            $this->__que__['default'] = array();
        } else {
            unset($this->__que__[$q]);
        }

        return $this;
    }

    /**
     * Toggle queues (activate a queue).
     *
     * @param  string $q='default' Queue ID to become active
     * @return IDbAbstract For method chaining
     */
    public function toggle($q='default') {
        $this->__activeQue__ = $q;
        return $this;
    }

    /**
     * Add an SQL to the active queue.
     *
     * @param  string        $sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param  array|[mixed] $params=null Params values [@DbAbstract.sql-params]
     * @return IDbAbstract For method chaining
     */
    public function add() {
        $args = func_get_args();
        $this->__que__[$this->__activeQue__][] = ($sql=$this->__queryable($args, false)).(substr($sql, -1)===';'? '': ';');
        return $this;
    }

    /**
     * Add an SQL to the active queue. Alias for "add()".
     *
     * @param  string        $sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param  array|[mixed] $params=null Params values [@DbAbstract.sql-params]
     * @return IDbAbstract For method chaining
     */
    public function transaction() {
        $args = func_get_args();
        $this->__que__[$this->__activeQue__][] = ($sql=$this->__queryable($args, false)).(substr($sql, -1)===';'? '': ';');
        return $this;
    }

    /**
     * Get queue(s) (SQL list(s)).
     *
     * @param  string|false $q=null Queue ID to be returned, false= return all queues, null=return the active queue
     * @param  int          $i=null Return only the i'th SQL of the queue
     * @return array|string An SQL string or a list of SQL strings
     */
    public function que($q=null, $i=null) {
        if ($q === false) {
            return $this->__que__;
        }
        if ($q === null) {
            $q = $this->__activeQue__;
        }
        return $i === null? $this->__que__[$q]: $this->__que__[$q][$i];
    }

    /**
     * Add an INSERT statement to the active SQL queue.
     *
     * @param  string $table Table name
     * @param  array $newVals Array of columns and their values. Values will be SQL escaped.
     *         To not escape a value put an ! at the beginning of the column name.
     *         {col-name: val-do-escaped, !col-name: val-do-not-escape,}
     * @param  array $newValsX=null Array of columns and their values. Values will NOT be SQL escaped.
     *         An alternative way to adding ! to the col-name in newVals
     *         {col: val-do-not-escape,}
     * @return IDbAbstract For method chaining
     */
    public function insert($table, array $newVals, array $newValsX=null) {
        $cols = $vals = '';
        foreach ($newVals as $col => $val) {
            if ($col[0] === '!') {
                $cols .= ($cols? ', ': '').ltrim($col, '!');
                $vals .= ($vals? ', ': '').$val;
            } else {
                $cols .= ($cols? ', ': '').$col;
                $vals .= ($vals? ', ': '').($val === null? 'NULL': (is_numeric($val)? "'$val'":
                    (is_bool($val)? ($this->toBool($val)): "'".$this->esc($val)."'")));
            }
        }
        /* dont parameterize */
        if ($newValsX) {
            foreach ($newValsX as $col => $val) {
                $cols .= ($cols? ', ': '').$col;
                $vals .= ($vals? ', ': '').$val;
            }
        }

        $this->__que__[$this->__activeQue__][] = "INSERT INTO $table ($cols) VALUES ($vals);";
        return $this;
    }

    /**
     * Add an UPDATE statement to the active SQL queue.
     *
     * @param  string $table Table name
     * @param  array $newVals Array of columns and their update values. Values will be SQL escaped.
     *         To not escape a value put an ! at the beginning of the column name.
     *         {col-name: val-do-escaped, !col-name: val-do-not-escape,}
     * @param  string        $where=null  SQL condition string (may contain params) [@DbAbstract.cond-str]
     * @param  array|[mixed] $params=null Param values [@DbAbstract.cond-params]
     * @return IDbAbstract For method chaining
     */
    public function update() {
        $args    = func_get_args();
        $table   = array_shift($args);
        $newVals = array_shift($args);
        $uStr    = '';

        if (is_array($newVals)) {
            foreach ($newVals as $col => $val) {
                if ($col[0] === '!') {
                    $uStr .= ($uStr? ', ': '').ltrim($col, '!')." = $val";
                } else {
                    $uStr .= ($uStr? ', ': '')."$col = ".($val === null? 'NULL':
                        (is_numeric($val)? "'$val'": (is_bool($val)? $this->toBool($val):
                        ($val === 'NOW()'? 'NOW()': "'".$this->esc($val)."'"))));
                }
            }
            $uStr .= $this->__queryable($args);
        } else {
            if (!$args) {
                $uStr = $newVals;
            } elseif (is_array($args[0])) {
                $uStr = $newVals.$this->__queryable($args);
            } else {
                $args[0] = "$newVals WHERE $args[0]";
                $uStr = $this->__queryable($args, false);
            }
        }

        $this->__que__[$this->__activeQue__][] = "UPDATE $table SET $uStr;";
        return $this;
    }

    /**
     * Add an DELETE statement to the active SQL queue.
     *
     * @param  string        $table       Table name
     * @param  string        $where=null  SQL condition string (may contain params) [@DbAbstract.cond-str]
     * @param  array|[mixed] $params=null Param values [@DbAbstract.cond-params]
     * @return IDbAbstract For method chaining
     */
    public function delete() {
        $args = func_get_args();
        $this->__que__[$this->__activeQue__][] = 'DELETE FROM '.array_shift($args).$this->__queryable($args).';';
        return $this;
    }

    /**
     * Creates an SQL condition from a map. This method is used by the Model class.
     * Turns {column1: value1, column2: value2} into column1=value1 AND column2=value2
     *
     * @param array  $array       {column1: value1,}
     * @param string $op          Operator to be used between each part
     * @param string $prefix=null Prefix to be added to each column name
     * @return string SQL condition to be used in WHERE    
     */
    public function arrayWhere(array $array, $op='AND', $prefix=null) {
        $array2 = array();

        foreach ($array as $k => $v)
            $array2[$prefix.$k] = $v;

        return array($this->__queryable($array2, false, $op), array());
    }

    /**
     * UTILITY > join query and params - used by this.update & this.delete
     * [col: int/float/bool/string/null/IS NULL/IS NOT NULL]
     */
    protected function __queryable(&$args, $addWhere=true, $op='AND') {
        $where = array_shift($args);

        if (is_array($where)) {
            $sql = '';
            foreach ($where as $col => $val) {
                if (is_array($val)) {
                    $ins = '';
                    foreach ($val as $val2) {
                        $ins .= ($ins? ', ': '').($val2 === null? 'NULL':
                            (is_numeric($val2)? "'$val2'":(is_bool($val2)? $this->toBool($val2):
                            "'".$this->esc($val2)."'")));
                    }
                    if ($ins) {
                        $sql .= ($sql? " $op ": '')." $col IN ($ins)";
                    }
                } else {
                    $sql .= ($sql? " $op ": '')." $col ".($val === null? 'IS NULL':
                        (is_numeric($val)? "= '$val'":(is_bool($val)? $this->toBool($val):
                        ($val === 'IS NULL' || $val  === 'IS NOT NULL'? "= $val":
                        "= '".$this->esc($val)."'"))));
                }
            }
            return $sql && $addWhere? " WHERE $sql": $sql;

        } elseif ($args) {
            $where = $this->__pquery($where, $args);
        }
        return $where && $addWhere? " WHERE $where": $where;
    }

    /**
     * UTILITY > join query string and params
     */
    protected function __pquery($sqlStr, $params=null) {
        if ($params) {
            $params = count($params) == 1 && is_array($params[0]) && !isset($params[0][0])? $params[0]: $params;
            $addOne = false;
            $replace = array();

            foreach (is_array($params)? $params: (array)$params as $k => $v) {
                if (is_array($v)) {
                    foreach ($v as &$vv) {
                        $vv = $vv === null? 'NULL':(is_numeric($vv)? "'$vv'":(
                             is_bool($vv)? $this->toBool($vv):
                             "'".$this->esc($vv)."'"));
                    }
                    $v = implode(',', $v);

                } else {
                    $v = $v === null? 'NULL':(is_numeric($v)? "'$v'":(
                         is_bool($v)? $this->toBool($v):
                         "'".str_replace('$', '[}#\{]', $this->esc($v))."'"));
                }

                if (is_numeric($k) && $k == 0) {
                    $addOne = true;
                }

                $replace['$'.($addOne? ++$k: $k)] = $v;
            }
            /* * */
            uksort($replace, array($this, '__lenSort'));
            $replace['[}#\{]'] = '$';
            $sqlStr = str_replace(array_keys($replace), array_values($replace), $sqlStr);
        }
        return $sqlStr;
    }

    protected function __lenSort($a, $b) {
        return strlen($b) - strlen($a);
    }
}
