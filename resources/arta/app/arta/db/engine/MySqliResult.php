<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2015::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7222
 * Created  2015/02/07
 * Updated  2015/02/07
 */

//namespace arta\db\engine;

require_once ARTA_DIR . 'interface/IDbResult.php';

/**
 * MySQL query result class. Get an IDbResult instance from the IDbAbstract object using the getResult()
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
class MySqliResult implements IDbResult {

    private $__res; // query res object
    private $__key = 0;
    private $__row;
    private $__fetched = false;

    /**
     * Create an IDbResult instance.
     *
     * @param mysqli_result $res Database query result.
     */
    public function __construct(mysqli_result $res) {
        $this->__res = $res;
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
            $fields[] = $glue? $esc.$finfo->name.$esc: $finfo->name;
        }

        return $glue? implode($glue, $fields): $fields;
    }

    /**
     * Move to a row.
     *
     * @param  int $row=0 Zero based row index
     */
    public function seek($row=0) {
        $this->__key = 0;
        $this->__res->data_seek($row);
    }

    /**
     * Free query result resource.
     */
    public function free() {
        $this->__key = 0;
        is_resource($this->__res)? $this->__res->free(): null;
    }

    /**
     * Get row count.
     *
     * @return int Row count
     */
    public function count() {
        return $this->__res->num_rows;
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
        return $this->__row = $this->__res->fetch_assoc();
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
        return $this->__row = $this->__res->fetch_object();
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
