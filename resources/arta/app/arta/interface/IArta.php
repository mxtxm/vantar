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
 * Interface for the Artaengine frame class. This interface is to insures the
 * class will always be backward compatible.
 *
 * @author Mehdi Torabi <mehdi @ artaengine.com>
 */
interface IArta {

    /**
     * Check if the application is running from the command line.
     *
     * @return bool true=the application is running from the command line
     */
    static public function isCli();

    /**
     * Start an Artaengine application.
     *
     * @param  string|array configs=null App configs ini file-path or array
     * @return void
     * @throws UrlFactoryMapNotExists
     */
    static public function start($configs=null);

    /**
     * Get value of a global variable defined in Arta::$globals
     *
     * @param  string key         Variable name
     * @param  mixed default=null If key did not exist return this value
     * @return mixed Value of the global key or default
     */
    static public function g($key, $default=null);

    /**
     * Builds the application. /build
     *
     * @param  string|array configs=null App configs ini file-path or array
     * @return void
     */
    static public function build($configs);
}
