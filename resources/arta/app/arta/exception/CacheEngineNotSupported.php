<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7902 
 * Created  2009/01/27
 * Updated  2013/02/05
 */
 
//namespace arta\exception;

/**
 * Cache engine is not supported. Feel free to contribute an ICacheAbstract
 * implementation for it :)
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.4.0
 */
class CacheEngineNotSupported extends Exception {

    protected $code = 79021;

    public function __construct($engine) {
        $this->message = 'Cache engine "'.$engine.'" is not supported by this version of Artaengine.';
    }
}
