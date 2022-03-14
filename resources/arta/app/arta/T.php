<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7711
 * Created  2009/02/22
 * Updated  2012/06/27
 */

//namespace arta;

/** String data type. Alias to T::STRING. To identify a string data type. If length matters in a context then length < 256. */
define('STRING',      1001);
/** String data type. Alias to T::TEXT. To identify a string data type, a textarea would be used to input data. */
define('TEXT',        2001);
/** String data type. Alias to T::WYSIWYG. To identify a string data type, a WYSIWYG widget would be used to input data. */
define('WYSIWYG',     4001);
/** Character data type. Alias to T::CHAR. */
define('CHAR',        3001);
/** String data type. Alias to T::EMAIL. To identify a string data type that is validated as an email address. */
define('EMAIL',       1101);
/** String data type. Alias to T::URL. To identify a string data type that is validated as a URL address. */
define('URL',         1201);
/** String data type. Alias to T::IP. To identify a string data type that is validated as an IP address. */
define('IP',          1301);
/** String data type. Alias to T::PASSWORD. To identify a string data type. a password widget would be used to input data. */
define('PASSWORD',    1401);
    /** String data type. Alias to T::BLOB. To identify a blob for MySql. */
define('BLOB',        2101);

/** Integer data type. Alias to T::INT. To identify an integer data type. If length matters in a context then 4bytes. */
define('INT',         1002); //4
/** Integer data type. Alias to T::INT. To identify an integer data type. If length matters in a context then 4bytes. */
define('INTEGER',     1002);
/** Integer data type. Alias to T::SERIAL. To identify an integer data type. If length matters in a context then 4bytes. Creates a serial column in database. */
define('SERIAL',      1012);
/** Integer data type. Alias to T::LONG. To identify an integer data type. If length matters in a context then 8bytes. */
define('LONG',        2002); //8
/** Integer data type. Alias to T::SERIAL_LONG. To identify an integer data type. If length matters in a context then 8bytes. Creates a serial column in database. */
define('SERIAL_LONG', 2012);
/** Integer data type. Alias to T::SMALL. To identify an integer data type. If length matters in a context then 2bytes. */
define('SMALL',       3002); //2
/** Double data type. Alias to T::DOUBLE. To identify a double data type. */
define('DOUBLE',      1003);
/** Float data type. Alias to T::FLOAT. To identify a float data type. */
define('FLOAT',       2003);
/** Nummeric data type. Alias to T::NUMERIC. To identify any nummeric data type. */
define('NUMERIC',     1004);

/** Boolean data type. Alias to T::BOOL. To identify a boolean data type. */
define('BOOL',        1006);
/** Boolean data type. Alias to T::BOOL. To identify a boolean data type. */
define('BOOLEAN',     1006);

/** Array data type. Alias to T::DICT. To identify an array data type. */
define('DICT',        1007);
/** Array data type. Alias to T::DICT. To identify an array data type. */
define('DICTIONARY',  1007);

/** Timestamp data type. Alias to T::TIMESTAMP. To identify a timestamp data type. */
define('TIMESTAMP',   1008);
/** Timestamp data type with timezone. Alias to T::TIMESTAMPZ. To identify a timestamp data type with timezone. */
define('TIMESTAMPZ',  1108);
/** Date data type. Alias to T::DATE. To identify a date data type. */
define('DATE',        3008);
/** Time data type. Alias to T::TIME. To identify a time data type. */
define('TIME',        4008);
/** Time data type  with timezone. Alias to T::TIMEZ. To identify a time data type with timezone. */
define('TIMEZ',       4108);
/** DateTime data type. Alias to T::DATETIME. To identify a date time data. */
define('DATETIME',    5008);
/** DateTime data type. Alias to T::DATETIME. To identify a date time data. */
define('TIMEDATE',    5008);

/**
 * Artaengine Data type constants. This constants are used To identify a data type
 * when needed, for example to define the data type of data model properties.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.0.0
 * @link       http://artaengine.com/api/tutorials/basics
 * @example    http://artaengine.com/examples/basics
 */
class T {

    /** String data type. To identify a string data type when needed. If length matters in a context then length < 256. */
    const STRING      = 1001;
    /** String data type. To identify a string data type when needed a textarea would be used to input data. */
    const TEXT        = 2001;
    /** String data type. To identify a string data type when needed, a WYSIWYG widget would be used to input data. */
    const WYSIWYG     = 4001;
    /** Character data type. */
    const CHAR        = 3001;
    /** String data type. To identify a string data type that is validated as an email address. */
    const EMAIL       = 1101;
    /** String data type. To identify a string data type that is validated as a URL address. */
    const URL         = 1201;
    /** String data type. To identify a string data type that is validated as an IP address. */
    const IP          = 1301;
    /** String data type. To identify a string data type. a password widget would be used to input data. */
    const PASSWORD    = 1401;
    /** String data type. To identify a blob for MySql. */
    const BLOB        = 2101;

    /** Integer data type. To identify an integer data type. If length matters in a context then 4bytes. */
    const INT         = 1002; //4
    /** Integer data type. To identify an integer data type. If length matters in a context then 4bytes. */
    const INTEGER     = 1002;
    /** Integer data type. To identify an integer data type. If length matters in a context then 4bytes. Creates a serial column in database. */
    const SERIAL      = 1012;
    /** Integer data type. To identify an integer data type. If length matters in a context then 8bytes. */
    const LONG        = 2002; //8
    /** Integer data type. To identify an integer data type. If length matters in a context then 8bytes. Creates a serial column in database. */
    const SERIAL_LONG = 2012;
    /** Integer data type. To identify an integer data type. If length matters in a context then 2bytes. */
    const SMALL       = 3002; //2
    /** Double data type. To identify a double data type. */
    const DOUBLE      = 1003;
    /** Float data type. To identify a float data type. */
    const FLOAT       = 2003;
    /** Nummeric data type. To identify any nummeric data type. */
    const NUMERIC     = 1004;

    /** Boolean data type. To identify a boolean data type. */
    const BOOL        = 1006;
    /** Boolean data type. To identify a boolean data type. */
    const BOOLEAN     = 1006;

    /** Array data type. To identify an array data type. */
    const DICT        = 1007;
    /** Array data type. To identify an array data type. */
    const DICTIONARY  = 1007;

    /** Timestamp data type. To identify a timestamp data type. */
    const TIMESTAMP   = 1008;
    /** Timestamp data type with timezone. To identify a timestamp data type with timezone. */
    const TIMESTAMPZ  = 1108;
    /** Date data type. To identify a date data type. */
    const DATE        = 3008;
    /** Time data type. To identify a time data type. */
    const TIME        = 4008;
    /** Time data type with timezone. To identify a time data type with timezone. */
    const TIMEZ       = 4108;
    /** DateTime data type. To identify a date time data. */
    const DATETIME    = 5008;
    /** DateTime data type. To identify a date time data. */
    const TIMEDATE    = 5008;
}
