<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7909
 * Created  2009/01/27
 * Updated  2013/02/05
 */

//namespace arta\exception;

/**
 * Log writer is not supported. Feel free to contribute an ILogWriter
 * implementation for it :)
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.4.0
 */
class LogWriterEngineNotSupported extends Exception {

    protected $code = 79091;

    public function __construct($writer) {
        $this->message = 'Log writer "'.$writer.'" is not supported by this version of Artaengine.';
    }
}
