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
 * Interface for the Artaengine CLI frame class. This interface is to insures the
 * class will always be backward compatible.
 *
 * @author Mehdi Torabi <mehdi @ artaengine.com>
 */
interface ICli {

    /**
     * @overload
     * Get a command param value by index.
     * @param  int index The index of the param value to be returned
     * @return string Value of the param index or null if param not exists
     *
     * @overload
     * Get a command param value by param name.
     * @param string index return the value of the param e.g:  "myapp.php -f foo"
     *        passinf "-f" will return "foo"
     * @return string Value of the param or null if param not found
     *
     * @overload
     * Get all the command params.
     * @return array An array of all params
     */
    static public function getParam($index=null);

    /**
     * Check if a CLI param value exists inside the proviced params.
     *
     * @param  string param Param value
     * @return bool If param exists
     */
    static public function hasParam($param);

    /**
     * Print a new line.
     *
     * @param  int     i Number of new lines
     * @return ArtaCli For method chaining
     */
    public function nl($i=1);

    /**
     * Write text.
     *
     * @param  string|array data        Data to be written
     * @param  string|array color=null  "fore-color,back-color" or [fore-color, back-color]
     * @param  string|array cursor=null "column,line" or [column, line]
     * @return ArtaCli For method chaining
     */
    public function write($data, $color=null, $cursor=null);

    /**
     * Write text and goto new line.
     *
     * @param  string|array data        Data to be written. Array items will be separated by new lines
     * @param  string|array color=null  "fore-color,back-color" or [fore-color, back-color]
     * @param  string|array cursor=null "column,line" or [column, line]
     * @return ArtaCli For method chaining
     */
    public function writeLn($data, $color=null, $cursor=null);

    /**
     * Add one or more indents.
     *
     * @param  int    indent=1 Number of indents to be added
     * @param  string str=tab  Indent string
     * @return ArtaCli For method chaining
     */
    public function indent($indent=1, $str="\t");

    /**
     * Remove one or more indents.
     *
     * @param  int indent=1 Number of indents to be removed
     * @return ArtaCli For method chaining
     */
    public function removeIndent($indent=1);

    /**
     * Set text color and style.
     *
     * @param  string  color  Color name
     * @return ArtaCli For method chaining
     */
    public function setForeColor($color);

    /**
     * Set text back-ground color.
     *
     * @param  string  color Color name
     * @return ArtaCli For method chaining
     */
    public function setBackColor($color);

    /**
     * Move cursor.
     *
     * @param  int     column=0  move cursor to this column, null=do nothing
     * @param  int     line=null move cursor to this line, null=do nothing
     * @return ArtaCli For method chaining
     */
    public function moveCursor($column=0, $line=null);

    /**
     * Fix cursor at a point. After the cursor is fixed all next writes will start
     * from this point.
     *
     * @param  int column=0  Move cursor to this column number, null=do nothing
     * @param  int line=null Move cursor to this line number, null=do nothing
     * @return ArtaCli For method chaining
     */
    public function fixCursor($column=0, $line=null);

    /**
     * Release the fixed cursor.
     *
     * @return object this
     */
    public function releaseCursor();

    /**
     * Start an Artaengine application. Simulate the Artaengine framework for a CLI application.<br/>
     * Note: This is experimental, Not all Artaengine libs and functions are not
     * tested to work in CLI.
     *
     * @param  string|array configs=null App setting file-path or array
     * @return void
     */
    static public function simulate($configs);

}
