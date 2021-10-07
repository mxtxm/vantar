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
 * Interface for database engines. To make a database usable in Artaengine two
 * classes must be created, one must implement IDbAbstract and one IDbAdvanced. 
 *
 * @author Mehdi Torabi <mehdi @ artaengine.com>
 */
interface IDbAbstract {

    /**
     * Get connection configs.
     *
     * @throws DatabaseError
     */
    public function connection();

    /**
     * To query a page of data. A page is a subset of the result consisting of a
     * max_number or rows, each page is addressed by a number starting from 1.
     *
     * @param  string        sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param  array|[mixed] params=null Params values [@DbAbstract.sql-params]
     * @param  array         configs     Page configs. [@DbAbstract.page.configs]
     * @return array This array can be fed to an instance of "Paging" to get the paging links [@DbAbstract.page.return]
     * @throws DatabaseError
     */
    public function page();

    /**
     * Inspect/debug the query and the transaction queue.
     *
     * @param  bool printI=true true=print the debug info and die, false=return the debug info
     * @return void|string|array Debug info or nothing based on arguments.
     */
    public function inspect($print=true);
    /**
     * Create a new SQL queue and set it as the active queue. You can have
     * one or more queues, you can make a queue active by method "toggle()"
     * and add SQL statements to the active queue using the ("add()", "update(), insert()"
     * and "delete()") methods and finally rollback or commit a queue as a database
     * transaction or one by one. If you do not need more than one queues
     * you do not have to use this method and toggle, simply use the action methods
     * and when it's call "commit()" or "rollback()".
     *
     * @param  string q='default' Queue ID. You can use this ID to address a specific queue,
     *         if you never created new queues then there is only one queue named "default"
     * @return IDbAbstract For method chaining
     */
    public function begin($q='default');
    /**
     * Rollback, cancel and remove a queue.
     *
     * @param  sting q=null Queue ID to be canceled, null=cancel the active queue
     * @return IDbAbstract For method chaining
     */
    public function rollback($q=null);

    /**
     * Toggle queues (activate a queue).
     *
     * @param  sting q='default' Queue ID to become active
     * @return IDbAbstract For method chaining
     */
    public function toggle($q='default');
    /**
     * Add an SQL to the active queue.
     *
     * @param  string        sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param  array|[mixed] params=null Params values [@DbAbstract.sql-params]
     * @return IDbAbstract For method chaining
     */
    public function add();

    /**
     * Add an SQL to the active queue. Alias for "add()".
     *
     * @param  string        sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param  array|[mixed] params=null Params values [@DbAbstract.sql-params]
     * @return IDbAbstract For method chaining
     */
    public function transaction();

    /**
     * Get queue(s) (SQL list(s)).
     *
     * @param  string|false q=null Queue ID to be returned, false= return all queues, null=return the active queue
     * @param  int          i=null Return only the i'th SQL of the queue
     * @return array|string An SQL string or a list of SQL strings
     */
    public function que($q=null, $i=null);

    /**
     * Add an INSERT statement to the active SQL queue.
     *
     * @param  string table Table name
     * @param  array newVals Array of columns and their values. Values will be SQL escaped.
     *         To not escape a value put an ! at the beginning of the column name.
     *         {col-name: val-do-escaped, !col-name: val-do-not-escape,}
     * @param  array newValsX=null Array of columns and their values. Values will NOT be SQL escaped.
     *         An alternative way to adding ! to the col-name in newVals
     *         {col: val-do-not-escape,}
     * @return IDbAbstract For method chaining
     */
    public function insert($table, array $newVals, array $newValsX=null);
    /**
     * Add an UPDATE statement to the active SQL queue.
     *
     * @param  string table Table name
     * @param  array newVals Array of columns and their update values. Values will be SQL escaped.
     *         To not escape a value put an ! at the beginning of the column name.
     *         {col-name: val-do-escaped, !col-name: val-do-not-escape,}
     * @param  string        where=null  SQL condition string (may contain params) [@DbAbstract.cond-str]
     * @param  array|[mixed] params=null Param values [@DbAbstract.cond-params]
     * @return IDbAbstract For method chaining
     */
    public function update();
    /**
     * Add an DELETE statement to the active SQL queue.
     *
     * @param  string        table       Table name
     * @param  string        where=null  SQL condition string (may contain params) [@DbAbstract.cond-str]
     * @param  array|[mixed] params=null Param values [@DbAbstract.cond-params]
     * @return IDbAbstract For method chaining
     */
    public function delete();

    /**
     * Creates an SQL condition from a map. This method is used by the Model class.
     * Turns {column1: value1, column2: value2} into column1=value1 AND column2=value2
     *
     * @param array  array       {column1: value1,}
     * @param string op          Operator to be used between each part
     * @param string prefix=null Prefix to be added to each column name
     * @return string SQL condition to be used in WHERE    
     */
    public function arrayWhere(array $array, $op='AND', $prefix=null);

    /**
     * Escapes non SQL safe characters.
     *
     * @param  string val Value to be escaped
     * @return string Escaped value
     */
    public function esc($val);
    /**
     * Wrap values inside COALESCE.
     *
     * @param  string       col Column name
     * @param  string|array val Value(s)
     * @return string COALESCE(col, vals, ...)
     */
    public function isnull($col, $val);

    /**
     * Pass an SQL query and get a limited SQL.
     *
     * @param  string sql      SQL query to put limit on
     * @param  int    limit=10 Number of rows to limit query result to
     * @param  int    offset=0 Row number to start from (zero based)
     * @return string The SQL with LIMIT clause
     */
    public function limit($sql, $limit=10, $offset=0);

    /**
     * Make a database concat sentence out of the passed values.
     *
     * @param  array  strs Strings to be concated
     * @return string Concated values.
     */
    public function concat($strs);

    /**
     * Get cast date/time sentence.
     *
     * @param  string val            A date/time string or a table column name
     * @param  string type=TIMESTAMP Cast: TIMESTAMP, DATETIME, TIME or DATE
     * @param  bool   isCol=false    Is "val" a string or a column name
     * @return string Cast sentence
     */
    public function castTime($val, $type='TIMESTAMP', $isCol=false);

    /**
     * Convert a value to an acceptable boolean value for the database.
     *
     * @param  bool|int val Source value
     * @return string Database false/true
     */
    public function toBool($val=true);
    /**
     * Convert a value to a valid database timestamp value.
     *
     * @param  int|array|string val int=timestamp, array={y:, m:, d:, h:, i:, s:} or
     *         a subset of it, string=date/time
     *         timestamp | array: {y:, m:, d:, h:, i:, s:} or a subset | string: date/time
     * @return string Database timestamp value
     */
    public function toTimestamp($val);

    /**
     * Affected row count.
     *
     * @return int Row count
     */
    public function affected();

    /**
     * Query field count.
     *
     * @return int Field count
     */
    public function fieldCount();
    /**
     * Get query columns.
     *
     * @param  string glue=null null=return an array of column names, string=return a
     *         string of column names glued with the string
     * @param  string esc='"' Column name escape character
     * @return array|string A list of column names or a string of glued column names
     */
    public function fields($glue=null, $esc='"');

    /**
     * Move to a row.
     *
     * @param  int row=0 Zero based row index
     */
    public function seek($row=0);
    /**
     * Connect or reconnect to a database.
     *
     * @param  array configs=array() Connection configs, if empty then connect using the previous connection configs. [@DbAbstract.connection]
     * @throws DatabaseError
     */
    public function connect(array $configs=array());

    /**
     * Free query result resource.
     */
    public function free();
    /**
     * Close database connection.
     */
    public function close();
    /**
     * Execute an SQL query. Use parameters for for values which need to be SQL escaped/safe.
     *
     * @param  string        sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param  array|[mixed] params=null Params values [@DbAbstract.sql-params]
     * @throws DatabaseError
     */
    public function query();

    /**
     * Get row count of the last query (without arguments) or a specified a query.
     *
     * @param  string        sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param  array|[mixed] params=null Params values [@DbAbstract.sql-params]
     * @return int Row count
     */
    public function count();

    /**
     * Fetch row as array.
     *
     * @return array The fetched row
     */
    public function row();
    /**
     * Fetch all rows in an arrays.
     *
     * @return array List of array rows [{row-array},]
     */
    public function rows();

    /**
     * Commit the active SQL queue. A queue ID can be passed explicitly to commit a specific queue.
     *
     * @param  bool transaction=true true=commit the queue SQLs in a database transaction,
     *         false=run queue SQLs one by one and not in a database transaction
     * @param  string q=null Queue ID. null=commit the active queue. Provide this argument
     *         to commit a specific queue by it's ID
     * @return bool State of success
     * @throws DatabaseError
     */
    public function commit($transaction=true, $q=null);

    /**
     * Check if a table(when only the first argument is provided) exists or if a
     * query has results.
     *
     * @param  string        table       Database table name or FROM clause
     * @param  string        sql         SQL string (may continue params) [@DbAbstract.sql-str]
     * @param  array|[mixed] params=null Params values [@DbAbstract.sql-params]
     * @return bool Table or result existence 
     * @throws DatabaseError
     */
    public function exists();

    /**
     * Get value of auto increment(serial) column.
     *
     * @param  string table              Database table name
     * @param  string col=id             Serial(auto increment) column name
     * @param  string position='current' 'current' or 'next'
     * @return mixed  Value of the next/current auto increment(serial) column
     */
    public function serial($table, $col='id', $position='current');

    /**
     * For library uses only
     */
    public function serialSequence($table, $col='id');

    /**
     * Check if a column is serial(auto increment).
     *
     * @param  string table  Database table name
     * @param  string col=id Serial(auto increment) column name
     * @return bool true=the column is serial
     */
    public function isSerial($table, $col);
}
