<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2015::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7212
 * Created  2015/02/07
 * Updated  2015/02/07
 */

//namespace arta\db\engine;

require_once ARTA_DIR . 'interface/IDbResult.php';

/**
 * PostgreSql query result class. Get an IDbResult instance from the IDbAbstract object using the getResult()
 * method to be able to query database while walking through a previously queries results using the same
 * IDbAbstract object.
 *
 * @copyright  ::COPYRIGHT2015::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      2.0.0
 * @link       http://artaengine.com/api/MySqliResult
 * @example    http://artaengine.com/examples/database
 * @see        http://artaengine.com/api/Database
 */
class PostgreSqlResult implements IDbResult {

    private $__res; // query res object
    private $__key = 0;
    private $__row;
    private $__fetched = false;

    /**
     * Create an IDbResult instance.
     *
     * @param resource $res Database query result.
     */
    public function __construct($res) {
        $this->__res = $res;
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
            $fields[] = $glue? $esc.pg_field_name($this->__res, $j).$esc: pg_field_name($this->__res, $j);
        }

        return $glue? implode($glue, $fields): $fields;
    }

    /**
     * Move to a row.
     *
     * @param  int $row=0 Zero based row index
     */
    public function seek($row=0) {
        pg_result_seek($this->__res, $row);
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
     * Get row count.
     *
     * @return int Row count
     */
    public function count() {
        return pg_num_rows($this->__res);
    }

    /**
     * Fetch row as object (default) or array.
     *
     * @return object|array The fetched row
     */
    public function next() {
        if (!$this->__fetched) {
            $this->valid();
        }
        $this->__fetched = false;
        return $this->__row;
    }

    /**
     * Fetch row as array.
     *
     * @return array The fetched row.
     */
    public function row() {
        return $this->__row = pg_fetch_assoc($this->__res);
    }

    /**
     * Get data of a column. You can get the data immediately after a query, if
     * no row has been fetched yet, this will fetch a row to get the column data.
     *
     * @param  string $col Column name
     * @return mixed  Data
     */
    public function __get($col) {
        if (!($row = $this->__row)) {
            $row = $this->row();
        }
        return is_array($row)? $row[$col]: (is_object($row)? $row->$col: null);
    }

    /**
     * Iterator - valid.
     * @return bool true=is valid
     */
    public function valid() {
        $this->__key++;
        $this->__fetched = true;
        return $this->__row = pg_fetch_object($this->__res);
    }

    /**
     * Iterator - key.
     * @return int Row number
     */
    public function key() {
        return $this->__key;
    }

    /**
     * Iterator - current.
     * @return string Current header item
     */
    public function current() {
        return $this->__row;
    }

    /**
     * Iterator - rewind.
     */
    public function rewind() {
        $this->seek();
    }
}
