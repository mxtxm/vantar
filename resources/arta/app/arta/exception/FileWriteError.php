<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7907
 * ClassID  7000
 * Created  2009/01/27
 * Updated  2013/02/05
 */

//namespace arta\exception;

/**
 * Exception when a file can no be written to disk.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.2.2
 */
class FileWriteError extends Exception {

    protected $code = 79071;

    /**
     * Constructs FileWriteError object.
     * @param string filepath File path
     * @return void
     */
    public function __construct($filepath) {
        $this->message = 'Can not write file: "'.$filepath.'" invalid path or permissions.';
    }
}
