<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7220
 * Created  2007/09/18
 * Updated  2013/02/05
 */

//namespace arta\db\engine;

require_once ARTA_DIR . 'db/DbAbstract.php';

/**
 * MySQL abstract class. To create instances of IDbAbstract for connecting
 * and accessing MySQL databases.<br/>
 * This class is not included in the autoload as it is recommended to create a
 * database connection and grab a database access instance from the "arta\Database"
 * object factory.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.3
 * @since      1.0.0
 * @link       http://artaengine.com/api/MySqliAbstract
 * @example    http://artaengine.com/examples/database
 * @see        http://artaengine.com/api/Database
 */
class MySqliAbstract extends DbAbstract {

    /** Database system name */
    const DBMS                = 'mysql';
    /** PHP extension name */
    const ENGINE              = 'MySQLi';
    /** Max number of chars allowed for identifier names by this database system */
    const MAX_IDENTIFIER_NAME = 63;
 
    /**
     * MySQL connection instance
     * @var mysqli
     */
    public $mysqli;
    /**
     * Query result
     * @var mysqli_result
     */
    protected $__res;


    public function __toString() {
        return '' . Inspect::dumpText(array($this->info()), false, false);
    }

    /**
     * Get info about the dbms and class.
     *
     * @return array Info array [@MySql.info.return]
     * @since Artaengine 1.1.0
     */
    public function info() {
        return array(
            'client'     => $this->mysqli->get_client_info(),
            'server'     => $this->mysqli->get_server_info(),
            'class'      => 'Arta.MySqliAbstract',
            'version'    => '1.0.3',
            'extension'  => 'mysqli',
            'dbms'       => 'MySQL',
            'connection' => $this->__configs,
        );
    }

    /**
     * Escapes non SQL safe characters.
     *
     * @param  string $val Value to be escaped
     * @return string Escaped value
     */
    public function esc($val) {
        return $this->mysqli->real_escape_string($val);
    }

    /**
     * Wrap values inside COALESCE.
     *
     * @param  string       $col Column name
     * @param  string|array $val Value(s)
     * @return string COALESCE(col, vals, ...)
     */
    public function isnull($col, $val) {
        if (is_array($val)) {
            foreach ($val as &$v) {
                $v = $this->esc($v);
            }
            $val = implode(', ', $val);
        } else {
            $val = $this->esc($val);
        }
        return 'COALESCE(' . $this->esc($col) . ', '.$val.')';
    }

    /**
     * Pass an SQL query and get a limited SQL.
     *
     * @param  string $sql      SQL query to put limit on
     * @param  int    $limit=10 Number of rows to limit query result to
     * @param  int    $offset=0 Row number to start from (zero based)
     * @return string The SQL with LIMIT clause
     */
    public function limit($sql, $limit=10, $offset=0) {
        return $sql . ' LIMIT ' . (int)$limit . ' OFFSET ' . (int)$offset;
    }

    /**
     * Make a database concat sentence out of the passed values.
     *
     * @param  array|[mixed] $strs Strings to be concated
     * @return string Concated values.
     */
    public function concat($strs) {
        $strs = func_get_args();
        if (count($strs) === 1 && is_array($strs[0])) {
            $strs = $strs[0];
        }
        return 'CONCAT('.implode(',', $strs).')';
    }

    /**
     * Get cast date/time sentence.
     *
     * @param  string $val            A date/time string or a table column name
     * @param  string $type=TIMESTAMP Cast: TIMESTAMP, DATETIME, TIME or DATE
     * @param  bool   $isCol=false    Is "val" a string or a column name
     * @return string Cast sentence
     */
    public function castTime($val, $type='TIMESTAMP', $isCol=false) {
        $val = $this->esc($val);
        $q   = $isCol? '': "'";
        switch (strtoupper($type)) {
            case 'TIMESTAMP':
            case 'DATETIME':
            case 'TIMEDATE':
                return "CAST($q$val$q AS DATETIME)";
            case 'TIME':
                return "CAST($q$val$q AS TIME)";
            case 'DATE':
                return "CAST($q$val$q AS DATE)";
        }
        return null;
    }

    /**
     * Convert a value to an acceptable boolean value for the database.
     *
     * @param  bool|int $val Source value
     * @return string Database false/true
     */
    public function toBool($val=true) {
        return (string)(int)$val;
    }

    /**
     * Convert a value to a valid database timestamp value.
     *
     * @param  int|array|string $val int=timestamp, array={y:, m:, d:, h:, i:, s:} or a subset of it, string=date/time
     *         timestamp | array: {y:, m:, d:, h:, i:, s:} or a subset | string: date/time
     * @return string Database timestamp value
     */
    public function toTimestamp($val) {
        if (is_int($val) || is_numeric($val)) {
            return date('Y-m-d H:i:s', (int)$val);
        } elseif (is_array($val)) {
            return (isset($v['d'])? (int)$v['d']: 0) . '-' . (isset($v['m'])? (int)$v['m']: 0) . '-' .
                   (isset($v['y'])? (int)$v['y']: 0) . ' ' . (isset($v['h'])? (int)$v['h']: 0) . ':' .
                   (isset($v['i'])? (int)$v['i']: 0) . ':' . (isset($v['s'])? (int)$v['s']: 0);
        } else {
            return strtotime($val) !== false?
                $this->esc($val):
                (strtolower($val) === 'now'? date('Y-m-d H:i:s', time()): date('Y-m-d H:i:s', $val));
        }
    }

    /**
     * Affected row count.
     *
     * @return int Row count
     */
    public function affected() {
        return $this->mysqli->affected_rows;
    }

    /**
     * Query field count.
     *
     * @return int Field count
     */
    public function fieldCount() {
        return $this->__res->field_count;
    }

    /**
     * Get query columns.
     *
     * @param  string $glue=null null=return an array of column names, string=return a
     *         string of column names glued with the string
     * @param  string $esc="`" Column name escape character
     * @return array|string A list of column names or a string of glued column names
     */
    public function fields($glue=null, $esc='`') {
        $fields = array();

        while ($finfo=$this->__res->fetch_field()) {
            $fields[] = $glue ? $esc . $finfo->name . $esc : $finfo->name;
        }

        return $glue? implode($glue, $fields): $fields;
    }

    /**
     * Move to a row.
     *
     * @param  int $row=0 Zero based row index
     */
    public function seek($row=0) {
        $this->__res->data_seek($row);
    }

    /**
     * Connect or reconnect to a database.
     *
     * @param  array $configs=array() Connection configs, if empty then connect using the previous connection configs. [@MySql.connection]
     * @throws DatabaseError
     */
    public function connect(array $configs=array()) {
        $configs = $configs + $this->__configs;

        if (!isset($configs['server'])) {
            $configs['server'] = 'localhost';
        }
        if (!isset($configs['port'])) {
            $configs['port'] = '3306';
        }
        if (!$configs['port']) {
            $configs['port'] = '3306';
        }
        if (!isset($configs['dbname'])) {
            $configs['dbname'] = '';
        }
        if (!isset($configs['user'])) {
            $configs['user'] = '';
        }
        if (!isset($configs['password'])) {
            $configs['password'] = '';
        }

        $this->mysqli = @new mysqli($configs['server'], $configs['user'], $configs['password'], $configs['dbname'], $configs['port']);

        if ($this->mysqli->connect_error) {
            $this->throwDatabaseException(DatabaseError::CONNECTION_ERROR_MSG, DatabaseError::CONNECTION_ERROR_CODE);
        }

        $this->query('SET NAMES utf8');
        $this->__configs = $configs;
    }

    /**
     * Free query result resource.
     */
    public function free() {
        if ($this->__res instanceof mysqli_result) {
            $this->__res->free();
        }
    }

    /**
     * Close database connection.
     */
    public function close() {
        $this->mysqli->close();
    }

    /**
     * Execute an SQL query. Use parameters for for values which need to be SQL escaped/safe.
     *
     * @param  string        $sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param  array|[mixed] $params=null Params values [@DbAbstract.sql-params]
     * @throws DatabaseError
     */
    public function query() {
        $this->__row = null;
        $args   = func_get_args();
        $sqlStr = array_shift($args);
        if ($args) {
            $sqlStr = $this->__pquery($sqlStr, $args);
        }
        /* * */
        $this->__queryStr = $sqlStr;
        $this->__queryP   = $args;
        if ($sqlStr) {
            $this->__query($sqlStr, $args);
        }
    }

    private function __query($sqlStr, $args) {
        if ($this->mysqli->multi_query($sqlStr)) {
            do {
                $result = $this->mysqli->store_result();
            } while ($this->mysqli->more_results() && $this->mysqli->next_result());

            if ($this->mysqli->errno) {
                $this->throwDatabaseException();
            }
            $this->__res = $result;
            return;
        }

        $this->connect();
        $this->__queryStr = $sqlStr;
        $this->__queryP   = $args;

        if ($this->mysqli->multi_query($sqlStr))  {
            do {
                $result = $this->mysqli->store_result();
            } while ($this->mysqli->more_results() && $this->mysqli->next_result());

            if ($this->mysqli->errno) {
                $this->throwDatabaseException();
            }
            $this->__res = $result;
            return;
        }

        $this->throwDatabaseException();
    }

    /**
     * Get row count of the last query (without arguments) or a specified a query.
     *
     * @param  string        $sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param  array|[mixed] $params=null Params values [@DbAbstract.sql-params]
     * @return int Row count
     * @throws DatabaseError
     */
    public function count() {
        $args = func_get_args();

        if (!$args) {
            return $this->__res->num_rows;
        }
        /* query */
        $sqlStr = array_shift($args);
        if ($args) {
            $sqlStr = $this->__pquery($sqlStr, $args);
        }
        $sqlStr = ltrim($sqlStr);

        $this->query(
            strtoupper(substr($sqlStr, 0, 6)) === 'SELECT'?
            substr_replace($sqlStr, ' COUNT(*) AS c ', 6, stripos($sqlStr, 'from')-6):
            "SELECT COUNT(*) AS c FROM $sqlStr;"
        );

        if ($row=$this->next(false)) {
            return $row['c'];
        }

        return 0;
    }

    /**
     * Fetch row as object (default) or array.
     *
     * @param  bool $object=true true=fetch object, false=fetch array
     * @return object|array The fetched row
     */
    public function next($object=true) {
        return $this->__row = ($object? $this->__res->fetch_object(): $this->__res->fetch_assoc());
    }

    /**
     * Fetch row as array.
     *
     * @return array The fetched row.
     */
    public function row() {
        return $this->__row = $this->__res->fetch_assoc();
    }

    /**
     * Fetch all rows in an arrays.
     *
     * @return array List of array rows [{row-array},]
     */
    public function rows() {
        $rows = array();
        if ($this->__res) {
            while ($row=$this->__res->fetch_assoc()) {
                $rows[] = $row;
            }
        }
        return $rows;
    }

    private function throwDatabaseException($msg=null, $errorNo=null) {
        throw new DatabaseError(
            Inspect::dumpDB($this->__inspect(false, $msg? $msg: $this->mysqli->error), false),
            $errorNo? $errorNo: $this->mysqli->errno,
            1,
            __FILE__,
            0
        );
    }

    /**
     * Commit the active SQL queue. A queue ID can be passed explicitly to commit a specific queue.
     *
     * @param  bool $transaction=true true=commit the queue SQLs in a database transaction,
     *         false=run queue SQLs one by one and not in a database transaction
     * @param  string $q=null Queue ID. null=commit the active queue. Provide this argument to commit a specific queue by it's ID
     * @return bool true=successful, false=nothing done
     * @throws DatabaseError
     */
    public function commit($transaction=true, $q=null) {
        if (!$q) {
            $q = $this->__activeQue__;
        }
        if (!isset($this->__que__[$q])) {
            return false;
        }
        $this->__queryT = $que = $this->__que__[$q];

        if ($transaction) {
            $this->mysqli->autocommit(false);
            $this->query('BEGIN;');
            $que[] = 'COMMIT;';
        }
        foreach ($que as $query) {
            $this->query($query);
        }

        if ($transaction) {
            $this->mysqli->autocommit(true);
        }
        $this->rollback($q);
        return true;
    }

    /**
     * Check if a table(when only the first argument is provided) exists or if a
     * query has results.
     *
     * @param  string        $table Database table name or FROM clause
     * @param  string        $sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param  array|[mixed] $params=null Params values [@DbAbstract.sql-params]
     * @return bool Table or result existence 
     * @throws DatabaseError
     */
    public function exists() {
        $args  = func_get_args();
        $table = $this->esc(array_shift($args));
        $this->query(
            $args?
                "SELECT 1 FROM $table " . $this->__queryable($args) . ';':
                "SHOW TABLE STATUS WHERE engine IS NOT NULL AND name = '$table';"
        );
        return $this->next()? true: false;
    }

    /**
     * Get value of auto increment(serial) column.
     *
     * @param  string $table              Database table name
     * @param  string $col=id             Serial(auto increment) column name
     * @param  string $position='current' 'current' or 'next'
     * @return mixed  Value of the next/current auto increment(serial) column
     * @throws DatabaseError
     */
    public function serial($table, $col='id', $position='current') {
        $this->query(
            $position === 'next'?
                ("SHOW TABLE STATUS LIKE '" . $this->esc($table) . "'"):
                'SELECT LAST_INSERT_ID() AS Auto_increment'
        );

        $row = $this->__res->fetch_assoc();
        return $row['Auto_increment'];
    }

    public function serialSequence($table, $col='id') {
        $this->__que__[$this->__activeQue__][] = 'SET @last_id = LAST_INSERT_ID();';
        return '@last_id';
    }

    /**
     * Check if a column is serial(auto increment).
     *
     * @param  string $table  Database table name
     * @param  string $col=id Serial(auto increment) column name
     * @return bool true=the column is serial
     * @throws DatabaseError
     */
    public function isSerial($table, $col) {
        $this->query(
            'SHOW COLUMNS FROM ' . $this->esc($table) .
            " WHERE Field='" . $this->esc($col) . "' AND Extra LIKE '%auto_increment%';"
        );
        return $this->__res->fetch_assoc()? true: false;
    }

    /**
     * Get the query result as an IDbResult. 
     *
     * @return IDbResult An MySqliResult instance
     */
    public function getResult() {
        require_once 'MySqliResult.php';
        $res = new MySqliResult($this->__res);
        return $res;
    }
}
