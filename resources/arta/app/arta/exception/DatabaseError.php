<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7905
 * Created  2013/02/05
 * Updated  2013/02/05
 */

//namespace arta\exception;

/**
 * Database errors and exceptions.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.4.0
 */
class DatabaseError extends ErrorException {

    /** General or unknown database error */   
    const DATABASE_ERROR_CODE = 79051;

    /** An invalid param was passed to a database access "IDbAdvanced" object constructor */
    const INVALID_CONSTRUCTOR_PARAM_ERROR_CODE = 79052;
    /** An invalid param was passed to a database access "IDbAdvanced" object constructor */
    const INVALID_CONSTRUCTOR_PARAM_ERROR_MSG =
        'Can not connect to a database, an instance of "IDbAbstract" or a connection configs array must be passed.';

    /** Could not connect to database */
    const CONNECTION_ERROR_CODE = 79053;
    /** Could not connect to database */
    const CONNECTION_ERROR_MSG = 'Can not connect to the database with current configs.';
}
