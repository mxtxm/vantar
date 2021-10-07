<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7918
 * Created  2009/01/27
 * Updated  2013/02/05
 */

//namespace arta\exception;

/**
 * Database extension is not installed.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.4.0
 */
class DatabaseExtension extends Exception {

    /** PostgreSQL is not installed. */
    const POSTGRESQL = 79181;

    /**
     * Constructs Exception object.
     * @param const  code Error code
     * @param string msg  Error message
     * @return void
     */
    public function __construct($code, $msg=null) {
        $this->code    = $code;
        $this->message = self::msg($code);
    }

    /**
     * Returns error message. 
     * @param  const  code Error code
     * @return string Error message
     */
    static public function msg($code) {
        switch ($code) {
            case self::POSTGRESQL:
                return 'PHP PostgeSQL extension is not available. Install and enable PostgreSQL support for PHP.';
        }
    }
}
