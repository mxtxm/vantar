<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7910
 * Created  2009/01/27
 * Updated  2013/02/05
 */

//namespace arta\exception;

/**
 * Thrown when attempting to bind submitted form data to a model but an IModel
 * instance is not provided or available.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.4.0
 */
class NoIModelInstanceToBind extends Exception {

    protected $code    = 79101;
    protected $message = 'No IModel instance has been provided or is available inside the form object to bind form data to.';
}
