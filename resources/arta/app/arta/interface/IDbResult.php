<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2015::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 */

//namespace arta;

/**
 * Interface for database query results.
 *
 * @author Mehdi Torabi <mehdi @ artaengine.com>
 */
interface IDbResult extends Iterator {

    /**
     * Query field count.
     *
     * @return int Field count
     */
    public function fieldCount();

    /**
     * Get query columns.
     *
     * @param  string $glue=null null=return an array of column names, string=return a string of column names glued with the string
     * @param  string $esc="`" Column name escape character
     * @return array|string A list of column names or a string of glued column names
     */
    public function fields($glue=null, $esc='`');

    /**
     * Move to a row.
     *
     * @param  int $row=0 Zero based row index
     */
    public function seek($row=0);

    /**
     * Free query result resource.
     */
    public function free();

    /**
     * Get row count.
     *
     * @return int Row count
     */
    public function count();

    /**
     * Fetch row as array.
     *
     * @return array The fetched row.
     */
    public function row();
}
