<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7908
 * Created  2013/02/05
 * Updated  2013/02/05
 */

//namespace arta\exception;

/**
 * Not an instance of IDbAdvanced.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.4.0
 */
class IDbAdvancedRequired extends ErrorException {

    protected $code = 79081;

    public function __construct() {
        $this->message = 'The action Expected an instance of IDbAdvanced.';
    }
}
