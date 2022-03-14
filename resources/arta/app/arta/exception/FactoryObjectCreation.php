<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2015::
 *
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7921
 * Created  2015/06/21
 * Updated  2015/06/21
 */

//namespace arta\exception;

/**
 * When a factory could not create an object.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      2.2.0
 */
class FactoryObjectCreation extends Exception {

    protected $code = 7921;

    public function __construct($msg) {
        $this->message = $msg;
    }
}
