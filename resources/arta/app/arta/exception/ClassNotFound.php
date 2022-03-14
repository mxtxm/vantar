<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7903
 * Created  2009/01/27
 * Updated  2013/02/05
 */

//namespace arta\exception;

/**
 * Autoloader could not find the requested class.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.4.0
 */
class ClassNotFound extends Exception {

    protected $code = 79031;

    public function __construct($class) {
        $this->message = 'Autoloader can not find class "'.$class.'".';
    }
}
