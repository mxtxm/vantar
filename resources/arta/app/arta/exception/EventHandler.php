<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7906
 * Created  2009/01/27
 * Updated  2013/02/05
 */

//namespace arta\exception;

/**
 * Exceptions raised by "Artaengine.RaiseHandler". When a a factory (url handler)
 * is not found.
 *
 * @copyright  Copyright (C) 2009 - 2013 by Mehdi Torabi, Artaengine
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.2.2
 */
class EventHandler extends Exception {

    /** Factory/handler is not defined. */
    const HANDLER_NOT_DEFINED       = 79061;
    /** Factory/handler file not exists. */
    const HANDLER_FILE_NOT_EXISTS   = 79062;
    /** Factory/handler method not exists in the class. */
    const HANDLER_METHOD_NOT_EXISTS = 79063;
    /** Factory/handler class not exists in the file. */
    const HANDLER_CLASS_NOT_EXISTS  = 79064;

    /**
     * Constructs EventHandler object.
     * @param const  code Error code
     * @param string msg  Error message
     * @return void
     */
    public function __construct($code, $msg=null) {
        $this->code    = $code;
        $this->message = $this->msg($code, $msg);
    }
    /**
     * Returns error message. 
     * @param  const  code Error code
     * @param  string msg  Error message
     * @return string Error message
     */
    public function msg($code, $msg) {
        switch ($code) {
            case self::HANDLER_NOT_DEFINED:
                return
                    '"'.$msg.'" handler has not been defined '.
                    'see: http://artaengine.com/tutorials/url-mapping#handle';

            case self::HANDLER_FILE_NOT_EXISTS:
                return
                    '"'.$msg[0].'" is defined but file "'.$msg[1].'" is missing. '.
                    'see: http://artaengine.com/tutorials/url-mapping#handle';

            case self::HANDLER_METHOD_NOT_EXISTS:
                return
                    '"'.$msg[0].'" is set to method "'.$msg[2].'" but method does '.
                    'not exists in file "'.$msg[1].
                    '" see: http://artaengine.com/tutorials/url-mapping#handle';

            case self::HANDLER_CLASS_NOT_EXISTS:
                return
                    '"'.$msg[0].'" is set to class "'.$msg[2].'" but class does '.
                    'not exists in file "'.$msg[1].
                    '" see: http://artaengine.com/tutorials/url-mapping#handle';
        }
    }
}
