<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7210
 * Created  2007/09/18
 * Updated  2013/02/05
 */

//namespace arta\db\engine;

require_once ARTA_DIR . 'db/DbAbstract.php';

/**
 * PostgreSQL abstract class. To create instances of IDbAbstract for connecting
 * and accessing PostgreSQL databases.<br/>
 * This class is not included in the autoload as it is recommended to create a
 * database connection and grab a database access instance from the "arta\Database"
 * object factory.<br/>
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.3
 * @since      1.0.0
 * @link       http://artaengine.com/api/PostgreSqlAbstract
 * @example    http://artaengine.com/examples/database
 * @see        http://artaengine.com/api/Database
 */
class PostgreSqlAbstract extends DbAbstract {

    /** Database system name */
    const DBMS                = 'postgresql';
    /** PHP extension name */
    const ENGINE              = 'PostgreSQL';
    /** Max number of chars allowed for identifier names by this database system */
    const MAX_IDENTIFIER_NAME = 63;

    /**
     * PostgreSQL connection resource
     * @var resource
     */  
    public $pg;
    /**
     * Query result
     * @var resource
     */
    protected $__res;

    public function __tosString() {
        return '' . Inspect::dumpText(array($this->info()), false, false);
    }

    /**
     * Get info about the dbms and class.
     *
     * @return array Info array [@PostgreSql.info.return]
     * @since Artaengine 1.1.0
     */
    public function info() {
        $info = pg_version($this->pg);
        $info['class']      = 'Arta.PostgreSqlAbstract';
        $info['version']    = '1.0.3';
        $info['extension']  = 'postgresql';
        $info['dbms']       = 'PostgreSQL';
        $info['connection'] = $this->__configs;
        return $info;
    }

    /**
     * Escapes non SQL safe characters.
     *
     * @param  string $val Value to be escaped
     * @return string Escaped value
     */
    public function esc($val) {
        return pg_escape_string($val);
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
        return 'COALESCE(' . $this->esc($col) . ', ' . $val . ')';
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
        return implode('||', $strs);
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
        $val = pg_escape_string($val);
        $q   = $isCol? null: "'";
        switch (strtoupper($type)) {
            case 'TIMESTAMP':
            case 'DATETIME':
            case 'TIMEDATE':
                return "CAST($q$val$q AS TIMESTAMP)";
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
        return $val? "'t'": "'f'";
    }

    /**
     * Convert a value to a valid database timestamp value.
     *
     * @param  int|array|string $val int=timestamp, array={y:, m:, d:, h:, i:, s:} or
     *         a subset of it, string=date/time
     *         timestamp | array: {y:, m:, d:, h:, i:, s:} or a subset | string: date/time
     * @return string Database timestamp value
     */
    public function toTimestamp($val) {
        if (is_int($val) || is_numeric($val)) {
            return date('Y-m-d H:i:s', (int)$val);
        } elseif (is_array($val)) {
            return (isset($v['d'])? (int)$v['d']: 0).'-'.(isset($v['m'])? (int)$v['m']: 0).'-'.
                   (isset($v['y'])? (int)$v['y']: 0).' '.(isset($v['h'])? (int)$v['h']: 0).':'.
                   (isset($v['i'])? (int)$v['i']: 0).':'.(isset($v['s'])? (int)$v['s']: 0);
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
        return pg_affected_rows($this->__res);
    }

    /**
     * Query field count.
     *
     * @return int Field count
     */
    public function fieldCount() {
        return pg_num_fields($this->__res);
    }

    /**
     * Get query columns.
     *
     * @param  string $glue=null null=return an array of column names, string=return a
     *         string of column names glued with the string
     * @param  string $esc='"' Column name escape character
     * @return array|string A list of column names or a string of glued column names
     */
    public function fields($glue=null, $esc='"') {
        $fields = array();
        $i = pg_num_fields($this->__res);

        for ($j=0; $j < $i; ++$j) {
            $fields[] = $glue ? ($esc . pg_field_name($this->__res, $j) . $esc): pg_field_name($this->__res, $j);
        }

        return $glue? implode($glue, $fields): $fields;
    }

    /**
     * Move to a row.
     *
     * @param int $row=0 Zero based row index
     */
    public function seek($row=0) {
        pg_result_seek($this->__res, $row);
    }

    /**
     * Connect or reconnect to a database.
     *
     * @param  array $configs=array() Connection configs, if empty then connect using the previous connection configs. [@PostgreSql.connection]
     * @throws DatabaseError
     */
    public function connect(array $configs=array()) {
        if (!function_exists('pg_connect')) {
            throw new DatabaseExtension(DatabaseExtension::POSTGRESQL);
        }

        $configs = $configs + $this->__configs;

        if (!isset($configs['server']) || !$configs['server']) {
            $configs['server'] = 'localhost';
        }
        if (!isset($configs['port']) || !$configs['port']) {
            $configs['port'] = '5432';
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

        if (!($this->pg=pg_connect("host=$configs[server] port=$configs[port] ".
            "dbname=$configs[dbname] user=$configs[user] password=$configs[password]"))) {
            $this->throwDatabaseException(DatabaseError::CONNECTION_ERROR_MSG, DatabaseError::CONNECTION_ERROR_CODE);
        }

        $this->__configs = $configs;
    }

    /**
     * Free query result resource.
     */
    public function free() {
        if (is_resource($this->__res)) {
            pg_free_result($this->__res);
        }
    }

    /**
     * Close database connection.
     */
    public function close() {
        if (is_resource($this->pg)) {
            pg_close($this->pg);
        }
    }

    /**
     * Execute an SQL query. Use parameters for for values which need to be SQL escaped/safe.
     *
     * @param string        $sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param array|[mixed] $params=null Params values [@DbAbstract.sql-params]
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

        if (!$sqlStr) {
            return;
        }

        if (!($this->__res = @pg_query($this->pg, $sqlStr))) {
            $this->connect();
            $this->__queryStr  = $sqlStr;
            $this->__queryP    = $args;
            if (!($this->__res = @pg_query($this->pg, $sqlStr))) {
                $this->throwDatabaseException();
            }
        }
    }

    /**
     * Get row count of the last query (without arguments) or a specified a query.
     *
     * @param string        $sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param array|[mixed] $params=null Params values [@DbAbstract.sql-params]
     * @return int Row count
     * @throws DatabaseError
     */
    public function count() {
        $args = func_get_args();

        if (!$args) {
            return pg_num_rows($this->__res);
        }
        /* query */
        $sqlStr = array_shift($args);
        if ($args) {
            $sqlStr = $this->__pquery($sqlStr, $args);
        }
        $sqlStr = ltrim($sqlStr);

        $this->query(
            strtoupper(substr($sqlStr, 0, 6)) === 'SELECT'?
                substr_replace($sqlStr, ' COUNT(*) AS c ', 6, stripos($sqlStr, 'from')-6): "SELECT COUNT(*) AS c FROM $sqlStr;"
        );

        if ($row=$this->next(false)) {
            return $row['c'];
        }

        return 0;
    }

    /**
     * Fetch row as object (default) or array.
     *
     * @param bool $object=true true=fetch object, false=fetch array
     * @return object|array The fetched row
     */
    public function next($object=true) {
        return $this->__row = ($object? pg_fetch_object($this->__res): pg_fetch_assoc($this->__res));
    }

    /**
     * Fetch row as array.
     *
     * @return array The fetched row
     */
    public function row() {
        return $this->__row = pg_fetch_assoc($this->__res);
    }

    /**
     * Fetch all rows in an arrays.
     *
     * @return array List of array rows [{row-array},]
     */
    public function rows() {
        return is_array($rows=pg_fetch_all($this->__res))? $rows: array();
    }

    private function throwDatabaseException($msg=null, $errorNo=null) {
        throw new DatabaseError(
            Inspect::dumpDB($this->__inspect(false, $msg? $msg: pg_last_error($this->pg)), false),
            $errorNo? $errorNo: DatabaseError::DATABASE_ERROR_CODE,
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
     * @param  string $q=null Queue ID. null=commit the active queue. Provide this argument
     *         to commit a specific queue by it's ID
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
        $query = implode(' ', $this->__queryT = $this->__que__[$q]);
        if ($transaction) {
            $query = "BEGIN; $query COMMIT;";
        }
        $this->query($query);
        $this->rollback($q);
        return true;
    }

    /**
     * Check if a table(when only the first argument is provided) exists or if a query has results.
     *
     * @param string        $table       Database table name or FROM clause
     * @param string        $sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param array|[mixed] $params=null Params values [@DbAbstract.sql-params]
     * @return bool Table or result existence 
     * @throws DatabaseError
     */
    public function exists() {
        $args  = func_get_args();
        $table = pg_escape_string(array_shift($args));
        $this->query(
            $args?
                ("SELECT * FROM $table " . $this->__queryable($args) . ';'):
                "SELECT * FROM pg_tables WHERE schemaname = 'public' AND tablename = '$table';"
        );
        return $this->next()? true: false;
    }

    /**
     * Get value of auto increment(serial) column.
     *
     * @param string $table              Database table name
     * @param string $col=id             Serial(auto increment) column name
     * @param string $position='current' 'current' or 'next'
     * @return mixed Value of the next/current auto increment(serial) column
     * @throws DatabaseError
     */
    public function serial($table, $col='id', $position='current') {
        $this->query(
            'SELECT ' . ($position === 'next'? 'last_value + increment_by': 'last_value') .
            ' FROM ' . pg_escape_string($table) . '_' . pg_escape_string($col) . '_seq;'
        );
        $row = pg_fetch_row($this->__res);
        return is_array($row)? $row[0]: null;
    }

    public function serialSequence($table, $col='id') {
        return "(SELECT CURRVAL(pg_get_serial_sequence('" . pg_escape_string($table) . "', '" . pg_escape_string($col) . "')))";
    }

    /**
     * Check if a column is serial(auto increment).
     *
     * @param string $table  Database table name
     * @param string $col=id Serial(auto increment) column name
     * @return bool true=the column is serial
     * @throws DatabaseError
     */
    public function isSerial($table, $col) {
        $this->query("SELECT pg_get_serial_sequence('".(pg_escape_string($table))."', '". (pg_escape_string($col))."');");
        return isset(pg_fetch_object($this->__res)->pg_get_serial_sequence);
    }

    /**
     * Get the query result as an IDbResult. 
     *
     * @return IDbResult An MySqliResult instance
     */
    public function getResult() {
        require_once 'PostgreSqlResult.php';
        $res = new PostgreSqlResult($this->__res);
        return $res;
    }
}
