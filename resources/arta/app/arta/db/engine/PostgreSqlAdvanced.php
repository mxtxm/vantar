<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7211
 * Created  2007/09/18
 * Updated  2013/04/07
 */

//namespace arta\db\engine;

require_once ARTA_DIR . 'interface/IDbAdvanced.php';
require_once 'PostgreSqlAbstract.php';

/**
 * PostgreSQL advanced class. To create instances of "IDbAbstract" for connecting
 * and accessing PostgreSQL databases. Plus implementing the advanced database
 * access methods of "IDbAdvanced" such as creating/altering tables and getting 
 * meta data.<br/>
 * The classes that implement the "IDbAdvanced" interface create a uniform cross
 * DBMS meta-data access and table create/alter platform to let the Artaengine data
 * modeling system have a robust control over the database.<br/>
 * It is possible to conver an "IDbAbstract" instance to "IDbAdvanced":<br/>
 * $advancedDbObj = Database::upgrade($abstractDbObj);<br/>
 * Or to create an "IDbAdvanced" with the same configs as an existing "IDbAbstract"
 * instance:<br/>
 * $advancedDbObj = PostgreSqlAdvanced($abstractDbObj);<br/>
 * This class is not included in the autoload as it is recommended to create a
 * database connection and grab a database access instance from the "arta\Database"
 * object factory.<br/>
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.2.1
 * @since      1.0.0
 * @link       http://artaengine.com/api/PostgreSqlAdvanced
 * @example    http://artaengine.com/examples/database
 * @see        http://artaengine.com/api/Database
 */
class PostgreSqlAdvanced extends PostgreSqlAbstract implements IDbAdvanced {

    /** true=sort the output of "metaTable()" and "metaView()" by dependency */
    public $sortMeta       = true;
    /** Put meaningful consraint names */
    public $nameConstraint = true;

    private $__meta = array();

    /**
     * Get info about the dbms and class.
     *
     * @return array Info array [@PostgreSql.advanced.info.return]
     * @since Artaengine 1.1.0
     */
    public function info() {
        $info = parent::info();
        $info['class']      = 'Arta.PostgreSqlAdvanced';
        $info['version']    = '1.2.1';
        return $info;
    }

    /**
     * Connect to a database. Either connection configs or an "DBIAbstract"
     * instance must be passed. If an instance of "DBIAbstract" is passed a new
     * database connection will not be made but the old connection related to the
     * instance will be set for the new "DBIAdvanced" object.
     *
     * @param IDbAbstract|array $param An instance of "IDbAbstract" or a connection configs array [@PostgreSql.connection]
     * @throws DatabaseError
     */
    public function __construct($param) {
        if ($param instanceof IDbAbstract) {
            $this->pg = &$param->pg;
        } elseif (is_array($param)) {
            parent::__construct($param);
        } else {
            throw new DatabaseError(
                DatabaseError::INVALID_CONSTRUCTOR_PARAM_ERROR_MSG,
                DatabaseError::INVALID_CONSTRUCTOR_PARAM_ERROR_CODE,
                1,
                __FILE__,
                __LINE__
            );
        }
    }

    /**
     * Copy table data to an array.
     *
     * @param string $table          Table name
     * @param string $delimiter="\t" Separates col data in each row
     * @param string $null='\N'      Uses this string instead of null values
     * @return array Table data
     */
    public function copyT2A($table, $delimiter="\t", $null='\N') {
        return pg_copy_to($this->pg, $table, $delimiter);
    }

    /**
     * Copy data of an array into a table.
     *
     * @param string $table          Table name
     * @param array  $rows           Data rows
     * @param string $delimiter="\t" Separates col data in each row
     * @param string $null='\N'      Uses this string instead of null values
     */
    public function copyA2T($table, array $rows, $delimiter="\t", $null='\N') {
        pg_copy_from($this->pg, $table, $rows, $delimiter);
    }

    /**
     * Get a dictionary of database data types mapped to Artaengine data types.
     *
     * @return array {db-type-string: T::TYPE_CONST,}
     */
    public function dbType2ArtaType() {
        return $this->artaType2DbType(true);
    }

    /**
     * Get a dictionary of Artaengine data types mapped to database data types.
     *
     * @return array {T::TYPE_CONST: db-type-string,}
     */
    public function artaType2DbType($pos=false) {
        $type = array(
            /* BOOL, IP */
            'BOOL',       'BOOLEAN',    'CIDR',
            /* INT */
            'INT8',       'BIGINT',     'SERIAL8',    'BIGSERIAL',
            'INT',        'INT4',       'INTEGER',    'SERIAL4',   'SERIAL',
            'INT2',       'SMALLINT',
            /* TEXT VCHAR */
            'TEXT',       'TEXT',       'TEXT',       'TEXT',      'CHARACTER VARYING',
            'VARCHAR',    'VARCHAR',    'VARCHAR',    'VARCHAR',
            /* CHAR */
            'BPCHAR',     'CHARACTER',  'CHAR',
            /* FLOAT */
            'FLOAT4',     'REAL',       'FLOAT8',     'DOUBLE PRECISION',
            /* NUMMERIC */
            'DECIMAL',    'NUMERIC',
            /* DATE */
            'DATE',       'TIME',       'TIMETZ',     'TIMESTAMP',
            'TIMESTAMP',  'TIMESTAMPTZ',
            'TIME WITHOUT TIME ZONE',   'TIME WITH TIME ZONE',
            'TIMESTAMP WITH TIME ZONE', 'TIMESTAMP WITHOUT TIME ZONE',

        );
        $const = array(
            /* BOOL, IP */
             BOOL,         BOOL,         IP,
            /* INT */
             LONG,         LONG,         SERIAL_LONG,  SERIAL_LONG,
             INTEGER,      INTEGER,      INTEGER,      SERIAL,     SERIAL,
             SMALL,        SMALL,
            /* TEXT VCHAR */
             BLOB,         DICTIONARY,   WYSIWYG,      TEXT,       STRING,
             EMAIL,        URL,          PASSWORD,     STRING,
            /* CHAR */
             CHAR,         CHAR,         CHAR,
            /* FLOAT */
             FLOAT,        FLOAT,        DOUBLE,       DOUBLE,
            /* NUMMERIC */
             NUMERIC,      NUMERIC,
            /* DATE */
             DATE,         TIME,         TIMEZ,        TIMESTAMP,
             DATETIME,     TIMESTAMPZ,
             TIME,                       TIMEZ,
             TIMESTAMPZ,                 TIMESTAMP,
        );
        return $pos? array_combine($type, $const): array_combine($const, $type);
    }

    /**
     * Get the database data type for virtual data types such as PASSWORD/EMAIL, etc.
     *
     * @param const $type T::TYPE_CONST (An Artaengine data type)
     * @return const|array Artaengine data type | [artaengine-data-type, len]
     */
    public function realType($type) {
        $map = array(
            /* INT */
            /* TEXT */
            DICTIONARY => TEXT,
            WYSIWYG    => TEXT,
            /* CHAR VCHAR */
            EMAIL      => array(STRING, 255),
            URL        => array(STRING, 255),
            PASSWORD   => array(STRING, 100),
            /* DATE */
            TIMEZ      => TIME,
            TIMESTAMPZ => TIMESTAMP,
            /* FLOAT */
            /* BOOL, IP */
            IP         => array(STRING, 20),
            /* NUMMERIC */
            /* BLOB */
        );
        return isset($map[$type])? $map[$type]: $type;
    }

    /**
     * Returns the database type definition of a T::TYPE_CONST (Artaengine data type).
     *
     * @param const      $type            T::TYPE_CONST (Artaengine data type)
     * @param int|array  $length=null     Max length or [min-length, max-length]
     * @param bool       $unsigned=false  Signed or Unsigned (if supported by database)
     * @return string    Database column type definition
     */
    public function toType($type, $length=null, $unsigned=false) {
        if (!is_int($type)) {
            $uType = strtoupper($type);
            if (   $uType !== 'TEXT'
                && $uType !== 'CHARACTER VARYING'
                && $uType !== 'VARCHAR'
                && $uType !== 'BPCHAR'
                && $uType !== 'CHARACTER'
                && $uType !== 'CHAR') {
                $length = null;
            }
            return $type . ($length? "($length)": '');
        }
        $len   = is_array($length)? $length[1]: $length;
        $types = $this->artaType2DbType();
        if (($type == STRING || $type == CHAR) && (int)$len > 255) {
            $type = TEXT;
        }
        //$type = (int)$type;
        switch ($type) {
            case URL:
            case EMAIL:
            case STRING:
                $len = $len? "($len)": '(255)'; break;

            case PASSWORD:
                $len = $len? "($len)": '(100)'; break;

            case INTEGER:
                switch ($len) {
                    case 1: $type = SMALL; break;
                    case 2: $type = SMALL; break;
                    case 3: $type = LONG;  break;
                    case 8: $type = LONG;  break;
                }
                $len = null;
                break;

            case FLOAT:
            case DOUBLE:
                if ($len) {
                    if (is_array($len)) {
                        $len = null;
                    } else {
                        if ($len < 0) {
                            $len = 0;
                        }
                        if ($len > 53) {
                            $len = 53;
                        }
                        $types[$type] = $len > 23? 'DOUBLE PRECISION': 'FLOAT';
                        $len = '';
                    }
                }
                break;

            case NUMERIC:
                if ($len === null) $len = 10;
                $s = 0;
                if (is_array($len)) {
                    list($p, $s) = $len;
                } else {
                    $p = $len;
                }
                $len = "($p, $s)";
                break;

            default:
                $len = null;
        }

        return $types[$type].$len;
    }

    /**
     * TABLE > Used for making multi col constraint name (by value or key)
     */
    private function __joinKeys($cols, $values=true) {
        $cols = (array)$cols;
        if ($values) {
            sort($cols);
            return implode('_', $cols);
        }
        ksort($cols);
        return implode('_', array_keys($cols));
    }

    /**
     * Make a valid database constraint name.
     *
     * @param string $prefix Such as 'PK', 'FK', 'CK', etc.
     * @param string $table  Table name
     * @param string $name   Constraint main name
     * @return string Constraint name
     */
    public function constraintName($prefix, $table, $name) {
        if (!$this->nameConstraint) {
            return '';
        }

        $max = PostgreSqlAbstract::MAX_IDENTIFIER_NAME;

        if (strlen($name=strtolower("{$prefix}_{$table}_{$name}")) > $max) {
            if (strlen($name=strtolower("{$prefix}_".sha1($table.$name))) > $max) {
                $name = substr($name, 0, $max);
            }
        }

        return $name;
    }

    /**
     * Create a check constraints.
     *
     * @param string|array $def SQL or [exp: [], col_name: name, name(optional - con name) ]
     * @return string Constraint definition
     */
    public function checkConstraint($def) {
        $exp = null;
        if (is_string($def)) {
            $exp = '('.trim($def).')';
        } elseif (is_array($def)) {
            $cols = isset($def['exp'])? $def['exp']: (isset($def['definition'])? $def['definition']: null);

            if (is_string($cols)) {
                $exp = '('.trim($cols).')';
            } elseif (is_array($cols) && isset($def['col_name'])) {
                $colStr = '';
                sort($cols);
                foreach ($cols as $col) {
                    $colStr .= ($colStr? ', ': '')."'".pg_escape_string($col)."'";
                }
                $exp = "($def[col_name] IN ($colStr))";
            }
        }
        /* constraint name */
        if (isset($def['name'])) {
            $name = $def['name'];
        } elseif ($exp) {
            $name = sprintf("%u", crc32($exp));
        }
        /* * */
        return $exp && $name? array($name, $exp): null;
    }

    /**
     * Define constraints for CREATE/ALTER TABLE
     * returns constraint sql
     */
    private function __addConstraint($table, $def, $type=null, $alter='') {
        $name = '';
        switch ($type? $type: $type=$def['type']) {
            case 'FOREIGN KEY':
                $prefix = 'fk';
                if (isset($def['reference'])) {
                    $refs = $def['reference']['cols'];
                    $rTable = $def['reference']['table'];
                } else {
                    $refs = $def['references'];
                    $rTable = $def['table'];
                }

                if (!isset($def['name']) || !is_string($def['name'])) {
                    $name = $this->__joinKeys($refs, false);
                }

                if (!isset($def['delete']) || $def['delete'] === true) {
                    $def['delete'] = 'CASCADE';
                }

                $exp = '('.implode(', ', array_keys($refs)).')'.
                    " REFERENCES $rTable (".
                    implode(', ', array_values($refs)).")".
                    (isset($def['match']) && $def['match']? " $def[match]": '').
                    (isset($def['delete']) && $def['delete'] &&
                        strtoupper($def['delete']) !== 'NO ACTION'?
                            " ON DELETE $def[delete]": '').
                    (isset($def['update']) && $def['update'] &&
                        strtoupper($def['update']) !== 'NO ACTION'?
                            " ON UPDATE $def[update]": '').
                    (isset($def['defer']) && $def['defer']? " $def[defer]": '');
                break;

            case 'PRIMARY KEY':
                $prefix = 'pk';
                if (!isset($def['name']) || !is_string($def['name'])) {
                    $name = $this->__joinKeys($def['cols']);
                }
                $exp = '('.implode(', ', (array)$def['cols']).')';
                break;

            case 'UNIQUE':
                $prefix = 'un';
                if (!isset($def['name']) || !is_string($def['name'])) {
                    $name = $this->__joinKeys($def['cols']);
                }
                $exp = '('.implode(', ', (array)$def['cols']).')';
                break;

            case 'CHECK':
                $prefix = 'ck';
                if (!($checkCon=$this->checkConstraint($def))) {
                    return null;
                }
                list($name, $exp) = $checkCon;
                break;

            default:
                return null;
        }

        return str_replace('COLUMN ', '', $alter).
            "{$alter}CONSTRAINT ".(isset($def['name']) && is_string($def['name'])?
            $def['name']: $this->constraintName($prefix, $table, $name))." $type $exp";
    }

    /**
     * Define column for CREATE/ALTER TABLE
     * changes reference
     */
    private function __addColumn($table, $def, $name, &$sqls, &$constraints, $alter='') {
        $type = isset($def['type'])? $def['type']: null;

        if (($isPK=isset($def['key'])) && !$type) {
            $type = SERIAL;
        }

        if (!$type) {
            $type = STRING;
        }
        /* constraint - null */
        $nullable = $isPK? null: ((isset($def['nullable'])? $def['nullable']:
            (isset($def['required'])? !$def['required']: true)) ? 'NULL': 'NOT NULL');
        /* default */
        $default = null;
        $typeU   = strtoupper($type);
        if (   $type  === SERIAL
            || $type  === SERIAL_LONG
            || $typeU === 'SERIAL8'
            || $typeU === 'BIGSERIAL'
            || $typeU === 'SERIAL4'
            || $typeU === 'SERIAL') {
            unset($def['default']);
        }

        if ($type === TIMESTAMP && !array_key_exists('default', $def)) {
            $def['default'] = null;
        }

        if (array_key_exists('default', $def)) {
            if (($default=$def['default']) === null) {
                $default = $nullable=='NULL'? 'DEFAULT NULL': '';
            } else {
                if ($type === BOOL) {
                    $default = $default? 't': 'f';
                }
                /* bad */
                $cot = "'";
                $defaultL = strtolower($default);
                if (   strpos($default, ':') !== false
                    || (strpos($default, '(')!==false && strpos($default, ')')!==false)
                    || $defaultL === 'current_date'
                    || $defaultL === 'current_time'
                    || $defaultL === 'current_timestamp'
                    || $defaultL === 'localtime'
                    || $defaultL === 'localtimestamp') {
                    $cot = '';
                }
                $default = 'DEFAULT '.$cot.$default.$cot;
            }
        }
        /* db def of type */
        $type   = $this->toType($type, isset($def['len'])? $def['len']: null);
        $sqls[] = ($alter? $alter: '    ')."\"$name\" $type $nullable $default";
        /* constraint check */
        if (isset($def['check'])) {
            if ($conSql=$this->__addConstraint($table, $def['check'], 'CHECK', $alter)) {
                $constraints[] = $conSql;
            }
        }
        /* constraints pk */

        /* constraint unique */
        if (isset($def['unique']) && $def['unique'] !== false) {
            $constraints[] = $this->__addConstraint(
                $table,
                array(
                    'cols' => $name,
                    'name' => is_string($def['unique'])? $def['unique']: null,
                ),
                'UNIQUE', $alter
            );
        }
        /* constraint fk */
        if (isset($def['reference'])) {
            $ref = $def['reference'];
            $ref['references'] = array(
                $name => isset($ref['col'])?
                    $ref['col']:
                    (isset($ref['reference'])?
                        $ref['reference']:
                        $ref['references']));

            $constraints[] = $this->__addConstraint($table, $ref, 'FOREIGN KEY', $alter);
        }
    }

    /**
     * Create table.
     *
     * @param string       $table            Table name
     * @param array        $cols=null        Column definitions [@DbAdvanced.createTable.cols]
     * @param array        $constraints=null Constraint definitions [@DbAdvanced.createTable.cons]
     * @param string|array $extra=null       Extra DBMS specific table parameters
     * @param bool         $exe=true         true=execute the CREATE TABLE on database
     * @return string CREATE TABLE SQL
     * @throws DatabaseError
     */
    public function createTable($table, array $cols=null, array $constraints=null, $extra=null, $exe=true) {
        $cons   = array();
        $pks    = array();
        $pkName = null;
        /* extra constraints */
        if ($constraints) {
            foreach ((array)$constraints as $constraint) {
                if (is_array($constraint)) {
                    if ($constraint['type'] === 'PRIMARY KEY') {
                        if (isset($constraint['name']))  {
                            $pkName = $constraint['name'];
                        }
                        $pks = $constraint['cols'];
                        continue;
                    }
                    if ($constraint=$this->__addConstraint($table, $constraint)) {
                        $cons[] = $constraint;
                    }
                } else {
                    $cons[] = $constraint;
                }
            }
        }
        /* PK > */
        foreach ($pks as $col) {
            $cols[$col]['required'] = !($cols[$col]['nullable']=false);
        }
        foreach ($cols as $col => &$def) {
            if (isset($def['key']) && $def['key'] !== false) {
                if (!isset($def['type']) && $pks) {
                    $def['type'] = INTEGER;
                }
                if (isset($def['P'])) {
                    $pkName = is_string($def['key'])? $def['key']: null;
                }
                $pks[] = $col;
            }
        }
        if ($pks) {
            $cons[] = $this->__addConstraint(
                $table,
                array('cols' => $pks, 'name' => $pkName),
                'PRIMARY KEY'
            );
        }
        /* PK < */
        $sqls = array();
        foreach ($cols as $col => $def2) {
            $this->__addColumn($table, $def2, $col, $sqls, $cons, '');
        }
        foreach ($cons as &$con) {
            $con = '    ' . $con;
        }
        /* * */
        if (is_array($extra)) {
            foreach ($extra as $param => &$val) {
                $param = strtoupper($param);
                $p1 = $p2 = '';
                if ($param === 'ONCOMMIT') {
                    $param = 'ON COMMIT';
                }
                if ($param === 'INHERITS' || $param === 'WITH') {
                    $p1 = '(';
                    $p2 = ')';
                }
                $val = (is_numeric($param)? '': $param) . ' ' . $p1 . (is_array($val)? implode(', ', $val): $val) . $p2;
            }
            $extra = ' ' . implode(' ', $extra);
        }

        $sql = 'CREATE TABLE "' . strtolower($table) . "\" (\n" . implode(",\n", array_merge($sqls, $cons)) . "\n)$extra;";
        if ($exe) {
            $this->query($sql);
        }

        return $sql;
    }

    // TODO synch issue ALTER TABLE "claims" ALTER COLUMN "update_d" TYPE TIMESTAMP WITHOUT TIME ZONE;

    /**
     * Alter table.
     *
     * @param string       $table            Table name
     * @param array        $cols=null        Column definitions [@DbAdvanced.alterTable.cols]
     * @param array        $constraints=null Constraint definitions [@DbAdvanced.alterTable.cons]
     * @param string|array $extra=null       Extra DBMS specific table parameters
     * @param bool         $exe=true         true=execute the ALTER TABLE on database
     * @return array ALTER TABLE SQLs
     * @throws DatabaseError
     */
    public function alterTable($table, array $cols=null, array $constraints=null, $extra=null, $exe=true) {
        $a = isset($cols['add'])?    $cols['add']:    array();
        $d = isset($cols['drop'])?   $cols['drop']:   array();
        $l = isset($cols['alter'])?  $cols['alter']:  array();
        $r = isset($cols['rename'])? $cols['rename']: array();
        /* constraints > > > */
        if (isset($constraints['alter'])) {
            foreach ($constraints['alter'] as $cName => $cDef) {
                $d[] = array('subject' => 'CONSTRAINT', 'name' => $cName);
                $cDef['subject'] = 'CONSTRAINT';
                $a[] = $cDef;
            }
        }

        if (isset($constraints['add'])) {
            foreach ($constraints['add'] as $cName => $cDef) {
                if (!(isset($cDef['name'])) && !is_numeric($cName)) {
                    $cDef['name'] = $cName;
                }
                $cDef['subject'] = 'CONSTRAINT';
                $a[] = $cDef;
            }
        }

        if (isset($constraints['drop'])) {
            foreach ($constraints['drop'] as $cName) {
                $d['drop'] = array('subject' => 'CONSTRAINT', 'name' => $cName);
            }
        }
        /* < < < constraints */
        $cons = $sqls1 = $sqls2 = array();
        /* DROP column/constraint */
        foreach ($d as $def) {
            if (is_array($def)) {
                $subject = isset($def['subject'])? $def['subject']: 'COLUMN';
                if ($subject === 'COLUMN') {
                    $sqls2[] = "DROP COLUMN \"$def[name]\"".
                        (isset($def['cascade']) || isset($def['CASCADE'])? ' CASCADE':
                        (isset($def['restrict']) || isset($def['RESTRICT'])? ' RESTRICT': ''));
                } else {
                    $sqls1[] = "DROP $subject \"$def[name]\"".
                        (isset($def['cascade']) || isset($def['CASCADE'])? ' CASCADE':
                        (isset($def['restrict']) || isset($def['RESTRICT'])? ' RESTRICT': ''));
                }
            } else {
                $sqls2[] = "DROP COLUMN \"$def\"";
            }
        }
        $sqls = array_merge(array(null), $sqls1, $sqls2);
        /* ADD */
        foreach ($a as $col => $def) {
            if (is_array($def)) {
                // add constraint
                if ((isset($def['subject'])? $def['subject']: null) === 'CONSTRAINT') {
                    if ($def=$this->__addConstraint($table, $def)) {
                        $cons[] = 'ADD ' . trim($def);
                    }
                }
                // add column
                else {
                    $this->__addColumn($table, $def, $col, $sqls, $cons, 'ADD COLUMN ');
                }
            }
        }
        /* ALTER */
        foreach ($l as $col => $def) {
            if (isset($def['type'])) {
                $sqls[] = "  ALTER COLUMN \"$col\" TYPE ".
                    $this->toType($def['type'], isset($def['len'])? $def['len']: 255);
            }
            if (isset($def['default'])) {
                $sqls[] = strtolower($def['default']) === 'drop'?
                    "  ALTER COLUMN \"$col\" DROP DEFAULT":
                    "  ALTER COLUMN \"$col\" SET DEFAULT " . ($def['default'] == null ||
                        strtolower($def['default']) =='null'? 'NULL': "'$def[default]'");
            }
            if (isset($def['nullable'])) {
                $sqls[] = "  ALTER COLUMN \"$col\" " . ($def['nullable']? 'DROP': 'SET') . ' NOT NULL';
            } elseif (isset($def['required'])) {
                $sqls[] = "  ALTER COLUMN \"$col\" " . ($def['required']? 'SET': 'DROP') . ' NOT NULL';
            }
        }
        /* RENAME */
        foreach ($r as $col => $new) {
            if (is_numeric($col)) {
                if (!is_array($extra)) {
                    $extra = array();
                }
                $extra['RENAME'] = $new;
            } else {
                $sqls[] = "RENAME COLUMN $col TO $new";
            }
        }
        if (!$sqls[0]) {
            unset($sqls[0]);
        }
        /* extra table configs */
        $altStrs = array();
        $ex = array();
        if (is_array($extra)) {
            foreach ($extra as $param => $val) {
                $ex[strtoupper($param)] = $val;
            }
            if (isset($ex['RENAME'])) {
                $altStrs[] = "ALTER TABLE \"$table\" RENAME TO \"$ex[RENAME]\";";
                $table = $ex['RENAME'];
            }
            if (isset($ex['SCHEMA'])) {
                $altStrs[] = "ALTER TABLE \"$table\" SET SCHEMA $ex[SCHEMA];";
            }
            if (isset($ex['OWNER'])) {
                $altStrs[] = "ALTER TABLE \"$table\" OWNER TO $ex[OWNER];";
            }
            if (isset($ex['TABLESPACE'])) {
                $altStrs[] = "ALTER TABLE \"$table\" SET TABLESPACE $ex[TABLESPACE];";
            }
            if (isset($ex['INHERIT'])) {
                foreach (is_array($ex['INHERIT'])? $ex['INHERIT']: explode(',', $ex['INHERIT']) as $inherit) {
                    $altStrs[] = "ALTER TABLE \"$table\" INHERIT \"" . trim($inherit) . '";';
                }
            }
            if (isset($ex['NO INHERIT'])) {
                foreach (is_array($ex['NO INHERIT'])? $ex['NO INHERIT']: explode(',', $ex['NO INHERIT']) as $inherit) {
                    $altStrs[] = "ALTER TABLE \"$table\" NO INHERIT \"".trim($inherit) . '";';
                }
            }
            if (isset($ex['WITH'])) {
                if (isset($ex['WITH']['OIDS'])) {
                    $ex['OIDS'] = $ex['WITH']['OIDS'];
                    unset($ex['WITH']['OIDS']);
                }
                $ex['SET'] = $ex['WITH'];
            }
            if (isset($ex['OIDS'])) {
                if (strtoupper($ex['OIDS']) === 'WITH OIDS') {
                    $ex['OIDS'] = true;
                }
                if (strtoupper($ex['OIDS']) === 'WITHOUT OIDS') {
                    $ex['OIDS'] = false;
                }
                $altStrs[] = "ALTER TABLE \"$table\" SET WITH" . ($ex['OIDS']? ' OIDS': 'OUT OIDS') . ';';
            }
            if (isset($ex['CLUSTER'])) {
                $altStrs[] = "ALTER TABLE \"$table\" SET WITH" . ($ex['CLUSTER']? ' OIDS': 'OUT CLUSTER') . ';';
            }
            if (isset($ex['SET'])) {
                foreach ($ex['SET'] as $setParam => &$setVal) {
                    if (!is_numeric($setParam)) {
                        $setVal = "$setParam = $setVal";
                    }
                }
                $altStrs[] = "ALTER TABLE \"$table\" SET(" . implode(', ', $ex['SET']) . ');';
            }
            if (isset($ex['RESET'])) {
                $altStrs[] = "ALTER TABLE \"$table\" RESET(" . implode(', ', $ex['RESET']) . ');';
            }

            unset($ex['CLUSTER'],    $ex['OIDS'],   $ex['RENAME'], $ex['WITH'], $ex['INHERIT'], $ex['TABLESPACE'],
                  $ex['NO INHERIT'], $ex['SCHEMA'], $ex['OWNER'],  $ex['SET'],  $ex['RESET']);

            foreach ($ex as $param => $val) {
                $altStrs[] = "ALTER TABLE \"$table\" " . (is_numeric($param)? $val: "$param $val") . ';';
            }
        }
        /* QUERY */
        foreach (array_merge($sqls, $cons) as $change) {
            $altStrs[] = "ALTER TABLE \"$table\" $change;";
        }

        $altStrs = implode("\n", $altStrs);
        if ($exe) {
            $this->query($altStrs);
        }
        return array($altStrs);
    }

    /**
     * Get table constraints meta data.
     *
     * @param string $table=null      Table name, null=get all constraints that exist in the database
     * @param string $constraint=null Constraint name to get info about, null=get info about all constraints
     * @return array  table=null then [{def},] or table!=null then {constraint-name: {def},} if constraint!=null then {def} def=
     *         [@DbAdvanced.getMetaConstraints]
     * @throws DatabaseError
     */
    public function getMetaConstraints($table=null, $constraint=null) {
        if (isset($this->__meta[$cacheKey="C_$table$constraint"])) {
            return $this->__meta[$cacheKey];
        }

        $db = $this->__configs['dbname'];

        $sqlStr = "
        SELECT
            tc.constraint_name    AS name,
            tc.constraint_type    AS type,
            tc.table_name         AS table,
            kcu.column_name       AS col_name,
            tc.is_deferrable      AS is_deferrable,
            tc.initially_deferred AS initially_deferred,
            rc.update_rule        AS on_update,
            rc.delete_rule        AS on_delete,
            rc.match_option       AS match_type,
            ccu.table_name        AS ref_table,
            ccu.column_name       AS ref_col,
            pc.consrc             AS src
        FROM
            information_schema.table_constraints tc
                LEFT JOIN information_schema.key_column_usage kcu ON
                        kcu.constraint_catalog = '$db'
                    AND kcu.table_catalog      = '$db'
                    AND kcu.constraint_schema  = tc.constraint_schema
                    AND kcu.constraint_name    = tc.constraint_name
                LEFT JOIN information_schema.referential_constraints rc ON
                        rc.constraint_catalog        = '$db'
                    AND rc.unique_constraint_catalog = '$db'
                    AND rc.constraint_schema         = tc.constraint_schema
                    AND rc.constraint_name           = tc.constraint_name
                LEFT JOIN information_schema.constraint_column_usage ccu ON
                        ccu.constraint_catalog = '$db'
                    AND ccu.table_catalog      = '$db'
                    AND ccu.constraint_schema  = rc.unique_constraint_schema
                    AND ccu.constraint_name    = rc.unique_constraint_name
                LEFT OUTER JOIN pg_constraint pc ON
                        pc.conname = tc.constraint_name
        WHERE
                tc.table_catalog      = '$db'
            AND tc.constraint_catalog = '$db'";

        if ($constraint) {
            $sqlStr .= " AND tc.constraint_name = '" . pg_escape_string($constraint) . "'";
        } elseif ($table) { 
            $sqlStr .= " AND tc.table_name = '" . pg_escape_string($table) . "'";
        }

        $this->query($sqlStr);
        $cons = array();

        foreach ($this->rows() as $row) {
            $name = strtolower($row['name']);
            if ((int)$name[0]) {
                continue;
            }
            if (isset($cons[$name])) {
                $def = $cons[$name];
            } else {
                $def = array(
                    'name'      => $name,
                    'type'      => $row['type'],
                    'table'     => $row['table'],
                    'cols'      => array(),
                    'delete'    => $row['on_delete']==='NO ACTION'? '': $row['on_delete'],
                    'update'    => $row['on_update']==='NO ACTION'? '': $row['on_update'],
                    'reference' => array(
                        'table' => $row['ref_table'],
                        'cols'  => array(),
                    ),
                );
                if ($row['type'] === 'FOREIGN KEY') {
                    if (($match=strtolower($row['match_type'])) === 'full') {
                        $match = 'MATCH FULL';
                    } elseif ($match === 'partial') {
                        $match = 'MATCH PARTIAL';
                    } else {
                        $match = '';
                    }
                    $def['match'] = $match;
                    $def['is_deferrable']      = $d1 = (strtolower($row['is_deferrable']) != 'no');
                    $def['initially_deferred'] = $d2 = strtolower($row['initially_deferred']) != 'no';
                    $def['defer'] = $d1? ($d2? 'DEFERRABLE INITIALLY DEFERRED': 'DEFERRABLE'): '';
                }
            }
            // cols
            if ($row['col_name']) {
                $def['cols'][$row['col_name']] = $row['col_name'];
            }
            // reference cols
            if ($row['ref_table']) {
                $def['reference']['cols'][$row['col_name']] = $row['ref_col'];
            }
            // cols - check constraint
            if ($row['type'] == 'CHECK') {
                $this->query("
                SELECT
                    attname
                FROM
                    pg_attribute a 
                    INNER JOIN pg_class c ON a.attrelid = c.oid
                WHERE
                    c.relname = '" . pg_escape_string($table) . "'
                    AND array[a.attnum] <@
                    (   SELECT conkey
                        FROM pg_constraint
                        WHERE conname = '" . pg_escape_string($name) . "')");

                $colsC = array();
                foreach ($this->rows() as $rowC) {
                    $colsC[$rowC['attname']] = $rowC['attname'];
                }
                $def['cols'] = $colsC;
                $def['definition'] = $row['src'];
            }
            if ($table) {
                $cons[$name] = $def;
            } else {
                $cons[] = $def;
            }
        }

        if ($constraint) {
            $cons = $cons[$constraint];
        }
        return $this->__meta[$cacheKey]=$cons;
    }

    /**
     * Sort table names: parents to children
     */
    private function __sortTables(&$sorted, $table) {
        $db = $this->__configs['dbname'];

        $this->query("
            SELECT
                ccu.table_name AS parent
            FROM
                information_schema.key_column_usage kcu
                    INNER JOIN information_schema.referential_constraints rc ON
                            rc.constraint_catalog        = '$db'
                        AND rc.unique_constraint_catalog = '$db'
                        AND rc.constraint_schema         = kcu.constraint_schema
                        AND rc.constraint_name           = kcu.constraint_name
                    INNER JOIN information_schema.constraint_column_usage ccu ON
                            ccu.constraint_catalog = '$db'
                        AND ccu.table_catalog      = '$db'
                        AND ccu.constraint_schema  = rc.unique_constraint_schema
                        AND ccu.constraint_name    = rc.unique_constraint_name
            WHERE
                    kcu.constraint_catalog = '$db'
                AND kcu.table_catalog      = '$db'
                AND ccu.table_name IS NOT NULL
                AND kcu.table_name = '$table'
            ORDER BY ccu.table_name;");

        foreach ($this->rows() as $parent) {
            if (!isset($sorted[$parent['parent']])) {
                $this->__sortTables($sorted, $parent['parent']);
            }
        }

        if (!isset($sorted[$table])) {
            $sorted[$table] = $table;
        }
    }

    /**
     * Get table primary keys.
     *
     * @param string $table Table name
     * @return array Array of primary key column names {pk-col-name: pk-col-name,}
     * @throws DatabaseError
     */
    public function getTablePrimaryKeys($table) {
        $db = $this->__configs['dbname'];

        $this->query("
        SELECT
            kcu.column_name AS col
        FROM
            information_schema.table_constraints tc
                LEFT JOIN information_schema.key_column_usage kcu ON
                        kcu.constraint_catalog = '$db'
                    AND kcu.table_catalog      = '$db'
                    AND kcu.constraint_schema  = tc.constraint_schema
                    AND kcu.constraint_name    = tc.constraint_name
        WHERE
                tc.constraint_type = 'PRIMARY KEY'
            AND tc.table_catalog      = '$db'
            AND tc.constraint_catalog = '$db'
            AND tc.table_name = '$table'");

        $rows  = array();
        while ($row=$this->row()) {
            $rows[$row['col']] = $row['col'];
        }

        return $rows;        
    }

    /**
     * Get metadata of tables.
     *
     * @param string $table=null Table name, null=return a list of database tables
     * @param int    $shape=2 1=return column definitions only, 2=return column definitions
     *                and constrain statuses, 3=return column definitions and constraint definitions
     * @return array if table=null {table-name: table-name,} else {col-name: {meta},} where meta is: [DbAdvanced.getMetaTable]
     * @throws DatabaseError
     */
    public function getMetaTable($table=null, $shape=2) {
        if (isset($this->__meta[$cacheKey="T_$table$shape"])) {
            return $this->__meta[$cacheKey];
        }

        $meta = array();
        if ($table == null) {
            $this->query("
            SELECT table_name, table_schema
            FROM   information_schema.tables
            WHERE  table_type = 'BASE TABLE' AND table_catalog = '".$this->__configs['dbname']."'
                   AND table_schema NOT IN ('pg_catalog', 'information_schema');");

            foreach ($this->rows() as $row) {
                $meta[$row['table_name']] = $row['table_name'];
            }

            if ($this->sortMeta) {
                $sorted = array();
                foreach ($meta as $table => $x) {
                    $this->__sortTables($sorted, $table);
                }
                return $this->__meta[$cacheKey]=$sorted;
            }

            return $this->__meta[$cacheKey]=$meta;
        }

        $pks = $uns = $uns = $cks = $fks = array();

        if ($shape > 1) {
            $cons    = $this->getMetaConstraints($table);
            $pkCount = 0;
            foreach ($cons as $con) {
                switch ($con['type']) {
                    case 'FOREIGN KEY':
                        $fks += $con['cols'];
                        break;

                    case 'UNIQUE':
                        $uns += $con['cols'];
                        break;

                    case 'CHECK':
                        $cks += $con['cols'];
                        break;

                    case 'PRIMARY KEY':
                        $pks += $con['cols'];
                        $pkCount = count($pks);
                }
            }
        }

        $pgTypeAliases = array(
            'bpchar'                      => 'char',
            'character'                   => 'char',
            'character varying'           => 'varchar',
            'integer'                     => 'int',
            'int4'                        => 'int',
            'int8'                        => 'bigint',
            'int2'                        => 'smallint',
            'serial4'                     => 'serial',
            'serial8'                     => 'bigserial',
            'float8'                      => 'double precision',
            'float4'                      => 'real',
            'boolean'                     => 'bool',
            'timestamp without time zone' => 'timestamp',
            'timestamptz'                 => 'timestamp with time zone',
            'time without time zone'      => 'time',
            'timetz'                      => 'time with time zone',
        );

        $this->query("
        SELECT
            a.attnum     AS number,
            a.attname    AS name,
            t.typname    AS type,
            a.attlen     AS len,
            a.atttypmod  AS max_len,
            a.attnotnull AS null,
            a.atthasdef  AS default,
            d.adsrc      AS def_val
        FROM
            pg_class c
                INNER JOIN pg_attribute a ON a.attrelid = c.oid
                INNER JOIN pg_type t      ON a.atttypid = t.oid
                INNER JOIN pg_attrdef d   ON c.oid = d.adrelid
        WHERE
            a.attnum > 0
            AND d.adnum = a.attnum
            AND a.atthasdef = 't'
            AND lower(c.relname) = '" . pg_escape_string($table) . "'
        UNION
        SELECT
            a.attnum     AS number,
            a.attname    AS name,
            t.typname    AS type,
            a.attlen     AS len,
            a.atttypmod  AS max_len,
            a.attnotnull AS null,
            a.atthasdef  AS default,
            ''           AS def_val
        FROM
            pg_class c
            INNER JOIN pg_attribute a ON a.attrelid = c.oid
            INNER JOIN pg_type t ON a.atttypid = t.oid
        WHERE
            a.attnum > 0
            AND a.atthasdef = 'f'
            AND lower(c.relname) = '" . pg_escape_string($table) . "';");

        $types = $this->dbType2ArtaType();

        foreach ($this->rows() as $row) {
            $type = $row['type'];
            $type = isset($pgTypeAliases[$type])? $pgTypeAliases[$type]: $type;
            if (isset($types[strtoupper($type)])) {
                $postgres = array();
                $name = $row['name'];
                $default = '';

                if ($hasDefault=(strtolower($row['default']) === 't')) {
                    $postgres['default'] = $default = $row['def_val'];

                    if (substr($default, -1) !== ')') {
                        $default = explode('::', $row['def_val']);
                        $default = $default[0];
                        if (is_string($default))
                            $default = trim($default, "'");
                    }

                    if ($default === "nextval('{$table}_{$name}_seq'::regclass)" && ($type == 'int' || $type == 'bigint')) {
                        $hasDefault = false;
                        $default = null;
                        $postgres['sequence'] = true;

                        if ($type == 'int') {
                            $type = 'serial';
                        } elseif ($type == 'bigint') {
                            $type = 'bigserial';
                        }
                    } else {
                        $postgres['sequence'] = false;
                    }
                }
                $postgres['type'] = $type;
                $meta[$name] = array(
                    'dbms'        => $postgres,
                    'type'        => $types[strtoupper($type)],
                    'unsigned'    => false,
                    'len'         => $row['len']>0? $row['len']: ($row['max_len']>5? $row['max_len']-4: null),
                    'nullable'    => $nullable=(strtolower($row['null']) !== 't'),
                    'required'    => !$nullable,
                    'has_default' => $hasDefault,
                    'default'     => $default,
                );

                if ($shape > 1) {
                    $meta[$name]['P']  = $pk=isset($pks[$name]);
                    $meta[$name]['PC'] = $pkCount;
                    $meta[$name]['S']  = $pk? $this->isSerial($table, $name): false;
                    $meta[$name]['U']  = isset($uns[$name]);
                    $meta[$name]['C']  = isset($cks[$name]);
                    $meta[$name]['F']  = isset($fks[$name]);
                }
            }
        }

        if ($shape == 3) {
            $meta['_constraints'] = $cons;
        }

        return $this->__meta[$cacheKey]=$meta;
    }

    /**
     * Sort views names: parents to children
     */
    private function __sortViews(&$sorted, &$views, $view) {
        $def = $views[$view]['definition'];
        foreach (explode(' ', str_replace(array('"', ';'), '', substr($def, strpos($def, 'from')+4))) as $token) {
            $token = trim($token);
            if ($token && strpos($token, '.') !== false) {
                $viewP = explode('.', $token);
                if (isset($views[$viewP=$viewP[1]]) && !isset($sorted[$viewP])) {
                    $this->__sortViews($sorted, $views, $viewP);
                }
            }
        }

        if (!isset($sorted[$view])) {
            $sorted[$view] = $views[$view];
        }
    }

    /**
     * Get database view list or view(s) meta data.
     *
     * @param string $view=null  View name, null=return for all views
     * @param bool   $thin=false true=return only a list of view names
     * @return array if thin=true return {view-name: view-name,} if view name is specified return
     *         {meta} if view name is not specified return {view-name: {meta},} meta= [@DbAdvanced.getMetaViews]
     * @throws DatabaseError
     */
    public function getMetaViews($view=null, $thin=false) {
        if (isset($this->__meta[$cacheKey="V_$view"])) {
            return $this->__meta[$cacheKey];
        }

        $wStr = '';
        if ($view !== null) {
            $wStr = strpos($view, '.') === false?
                ("AND viewname = '" . pg_escape_string($view) . "'"):
                ("AND schemaname || '.' || viewname = '" . pg_escape_string($view) . "'");
        }

        $this->query("
        SELECT
            schemaname,
            viewname,
            viewowner,
            definition
        FROM
            pg_views
        WHERE
            schemaname NOT IN ('pg_catalog', 'information_schema')
            AND viewname !~ '^pg_' $wStr;");

        $views = array();

        if ($thin) {
            foreach ($this->rows() as $row) {
                $views[$row['viewname']] = $row['viewname'];
            }
            return $views;
        }

        foreach ($this->rows() as $row) {
            $this->query("SELECT column_name FROM information_schema.columns WHERE table_name = '$row[viewname]';");
            $cols = array();
            foreach ($this->rows() as $col) {
                $cols[] = $col['column_name'];
            }

            $views[$row['viewname']] = array(
                'schema'     => $row['schemaname'],
                'name'       => $row['viewname'],
                'owner'      => $row['viewowner'],
                'definition' => $row['definition'],
                'cols'       => $cols,
            );
        }

        if ($view) {
            $views = $views[$view];
        } elseif ($this->sortMeta) {
            $sorted = array();
            foreach ($views as $view => $def) {
                $this->__sortViews($sorted, $views, $view);
            }
            return $this->__meta[$cacheKey]=$sorted;
        }

        return $this->__meta[$cacheKey]=$views;
    }

    /**
     * Get database user's meta data.
     *
     * @return array {user-name: {meta},} meta= [@DbAdvanced.metaUsers]
     * @throws DatabaseError
     */
    public function getMetaUsers() {
        $this->query("SELECT * FROM pg_user");
        $users = array();
        foreach ($this->rows() as $row) {
            $users[$row['usename']] = array(
                'name'     => $row['usename'],
                'sysid'    => $row['usesysid'],
                'createdb' => $row['usecreatedb']=='t',
                'super'    => $row['usesuper']=='t',
                'catupd'   => $row['usecatupd']=='t',
            );
        }
        return $users;
    }

    /**
     * Get table indexes meta data (Experimental - subject to change).
     *
     * @param string $table table name, null=return all
     * @return array array {index-name: {meta},} meta= [@DbAdvanced.getMetaIndexes]
     * @throws DatabaseError
     */
    public function getMetaIndexes($table=null) {
        if (isset($this->__meta[$cacheKey="I_$table"])) {
            return $this->__meta[$cacheKey];
        }

        $this->query("
        SELECT
            c.relname  AS name,
            i.indkey   AS indexes,
            x.indexdef AS definition
        FROM
            pg_class c
            INNER JOIN pg_index i ON c.oid = i.indexrelid
            INNER JOIN pg_indexes x ON x.indexname = c.relname
        WHERE 
            c.oid IN (
                SELECT indexrelid
                FROM   pg_index i2 INNER JOIN pg_class c2 ON c2.oid = i2.indrelid
                WHERE  indisunique != 't'
                       AND indisprimary != 't' " . ($table? ("AND c2.relname = '" . pg_escape_string($table) . "'"): '') . ')');

        $meta = array();

        foreach ($this->rows() as $row) {
            $meta[$row['name']] = array(
                'name'       => $row['name'],
                'indexes'    => $row['indexes'],
                'definition' => $row['definition'].';',
            );
        }

        return $this->__meta[$cacheKey]=$meta;
    }

    /**
     * Get trigger(s) meta data (Experimental - subject to change).
     *
     * @param string $table=null Table name, null=return all triggers
     * @return array  {trigger-name: {meta},} meta= [@DbAdvanced.metaTriggers]
     * @throws DatabaseError
     */
    public function getMetaTriggers($table=null) {
        if (isset($this->__meta[$cacheKey="TR_$table"])) {
            return $this->__meta[$cacheKey];
        }

        $this->query("
        SELECT DISTINCT *
        FROM   information_schema.triggers
        WHERE  trigger_schema NOT IN ('pg_catalog', 'information_schema') " .
            ($table? ("AND event_object_table = '" . pg_escape_string($table) . "'"): ''));

        $meta = array();

        foreach ($this->rows() as $row) {
            $name = $row['trigger_name'];
            if (!isset($meta[$name])) {
                $meta[$name] = array(
                    'name'         => $name,
                    'events'       => array(),
                    'table'        => $row['event_object_table'],
                    'action'       => $row['action_statement'],
                    'time'         => $row['condition_timing'],
                    'orientation'  => $row['action_orientation'],
                    'table_schema' => $row['event_object_schema'],
                    'schema'       => $row['trigger_schema'],
                );
            }
            $meta[$name]['events'][] = $row['event_manipulation'];
            $meta[$name]['definition'] = "CREATE TRIGGER $name ".$meta[$name]['time'] . ' ' .
                (implode(' OR ', $meta[$name]['events'])."\n    ON " . $meta[$name]['table']) .
                (" FOR EACH {$meta[$name]['orientation']}\n    " . $meta[$name]['action'] . ';');
        }

        return $this->__meta[$cacheKey]=$meta;
    }

    /**
     * Get Procedure(s)/Function(s) meta data (Experimental - subject to change).
     *
     * @param string $proc=null Procedure/Function name
     * @return array [@DbAdvanced.getMetaProcedures]
     * @throws DatabaseError
     */
    public function getMetaProcedures($proc=null, $type=null) {
        if (isset($this->__meta[$cacheKey="P_$proc"])) {
            return $this->__meta[$cacheKey];
        }

        $selectStr = $wStr = '';

        if ($proc) {
            $wStr = $type?
                (" AND type_udt_name = '" . pg_escape_string($type) . "'"):
                (" AND routine_name = '" . pg_escape_string($proc) . "'");
        }

        if ($canDdef=$this->exists('information_schema.routines', "routine_name = 'pg_get_functiondef'")) {
            $selectStr = ", pg_catalog.pg_get_functiondef(CAST(replace(specific_name, routine_name || '_', '') AS int)) AS realdef";
        }

        $this->query("
        SELECT
            routine_schema,
            routine_name,
            routine_type,
            external_language,
            type_udt_name,
            data_type ,
            routine_definition,
            routine_body,
            replace(specific_name, routine_name || '_', '') AS oid
            $selectStr
        FROM
            information_schema.routines
        WHERE
            specific_schema NOT IN ('pg_catalog', 'information_schema')$wStr");

        $meta = array();

        foreach ($this->rows() as $row) {
            $meta[$row['routine_name']] = array(
                'schema'     => $row['routine_schema'],
                'name'       => $row['routine_name'],
                'type'       => $row['routine_type'],
                'language'   => $row['external_language'],
                'udt_type'   => $row['type_udt_name'],
                'data_type'  => $row['data_type'],
                'body'       => $row['routine_body'],
                'def'        => $row['routine_definition']? $row['routine_definition']: '-- NEEDS SUPER USER PRIVILEGE --',
            );
            if ($canDdef && $row['realdef']) {
                $meta[$row['routine_name']]['definition'] = $row['realdef'];
            }
        }

        return $this->__meta[$cacheKey]=$meta;
    }

    /**
     * Get information about a table. The result is DBMS specific.
     *
     * @param string $table=null Table name
     * @return array [@PostgeSql.getMetaTableStatus]
     * @throws DatabaseError
     */
    public function getMetaTableStatus($table=null) {
        if (!$table) {
            return array(
                'parents'       => array(),
                'options'       => array(),
                'has_oids'      => null,
                'is_shared'     => null,
                'commit_action' => null,
                'tablespace'    => null,
            );
        }

        if (isset($this->__meta[$cacheKey="TS_$table"])) {
            return $this->__meta[$cacheKey];
        }

        $table    = pg_escape_string($table);
        $inherits = array();

        $this->query("
        SELECT
            P.relname AS parent
        FROM
            pg_inherits   AS I
            JOIN pg_class AS C ON I.inhrelid  = C.oid
            JOIN pg_class AS P ON I.inhparent = P.oid
        WHERE
            C.relname = '$table';
        ");

        foreach ($this->rows() as $row) {
            $inherits[] = $row['parent'];
        }

        $this->query("
        SELECT
            T.tablespace    AS tablespace,
            I.commit_action AS commit_action,
            C.reloptions    AS options,
            C.relhasoids    AS has_oids,
            C.relisshared   AS is_shared
        FROM
            pg_class C
            INNER JOIN pg_tables T                 ON T.tablename = C.relname
            INNER JOIN information_schema.tables I ON T.tablename = I.table_name
        WHERE
                C.relname    = '$table'
            AND I.table_name = '$table'
            AND T.tablename  = '$table'
        ");

        if ($row=$this->row()) {
            $options = array();
            $optionsStr = '';

            if ($row['options']) {
                foreach (explode(',', $optionsStr=substr($row['options'], 1, -1)) as $option) {
                    $option = explode('=', $option);
                    if (isset($option[1])) {
                        $options[trim($option[0])] = trim($option[1]);
                    } elseif (trim($option[0])) {
                        $options[] = trim($option[0]);
                    }
                }
            }

            return $this->__meta[$cacheKey] = array(
                'parents'       => $inherits,
                'options'       => $optionsStr,
                'options_array' => $options,
                'has_oids'      => $row['has_oids'] === 't',
                'is_shared'     => $row['is_shared'] === 't',
                'commit_action' => $row['commit_action'] == 'PRESERVE ROWS'? '': $row['commit_action'],
                'tablespace'    => $row['tablespace'],
            );

        } else {
            return array(
                'parents'       => array(),
                'options'       => array(),
                'has_oids'      => null,
                'is_shared'     => null,
                'commit_action' => null,
                'tablespace'    => null,
            );
        }
    }

    /**
     * Get truncate/delete SQL string for all database tables.
     *
     * @param bool $delete=false bool false=TRUNCATE TABLE, true=DELETE FROM
     * @return string TRUNCATE/DELETE SQL
     */
    public function truncate($delete=false) {
        $sqls = array();
        $tables = $this->getMetaTable();
        foreach (array_reverse($tables) as $table) {
            $sqls[] = $delete? "DELETE FROM $table;": "TRUNCATE TABLE $table;";
        }
        return implode("\n", $sqls);
    }

    /**
     * BACKUP RESTORE > database to filename
     */
    private function __sortTablesByRef($key) {
        if (isset($this->__temp[$key])) {
            return;
        }

        $this->__temp[$key] = $key;
        foreach ($this->__tmeta[$key]['_constraints'] as $c) {
            if ($c['type'] == 'FOREIGN KEY') {
                $this->__sortTablesByRef($c['table']);
            }
        }

        $this->__sorted[] = $key;
    }

    /**
     * Dump database to file. (Experimental).
     *
     * @param string|bool $filename=null File-path to write dump to or false=return dump or
     *        null=dump to file in temp directory as "database-name.sql"
     * @param array $configs=array [@PostgeSQLAdvanced.backup.configs]
     * @return void|string if filename=null returns query
     * @throws DatabaseError
     */
    public function backup($filename=null, array $configs=array()) {
        ini_set('memory_limit', '7000M');
        set_time_limit(0);

        $type = isset($configs['type'])? $configs['type']: 'full';
        $drop = isset($configs['drop'])? $configs['drop']: false;
        $pg   = pg_version($this->pg);

        $sqls = array(
            '-- ',
            '-- Artaengine PostgreSQL database dump',
            '-- Created by Artaengine DbAdvanced (PostgreSQL) 1.2.1',
            '-- PostgreSQL Server: '.(isset($pg['server'])? $pg['server']: '').
                ' Client: '.(isset($pg['client'])? $pg['client']: ''),
            '-- Database: '.$this->__configs['dbname'],
            '-- Create time: '.date('Y/m/d H:i:s'),
            '-- ',
            null,
            "-- \n",
            "START TRANSACTION;",
        );
        /* TABLES */
        $tables      = $this->getMetaTable();
        $statistics  = '-- '.count($tables).' Tables; ';
        $constraints = array();
        $sequences   = array();
        $tableDefs   = array();

        if ($type === 'full') {
            if ($drop) {
                $sqls[] = "\n-- \n-- Type: DROP TABLES;\n-- ";
                foreach (array_reverse($tables) as $table) {
                    $sqls[] = "DROP TABLE $table;";
                }
            }
            foreach ($tables as $table) {
                $sqls[] = "\n-- \n-- Type: TABLE; Name: $table;\n-- ";
                $tableDef = $this->getMetaTable($table, 3);
                $cons = $tableDef['_constraints'];
                unset($tableDef['_constraints']);
                $tableDefs[$table] = $tableDef;
                // sequences - to be added after constraints
                foreach ($tableDef as $col => &$def) {
                    if (isset($def['dbms']['sequence'])) {
                        $this->query("SELECT COALESCE(max($col), 1) AS value FROM $table;");
                        $seqStart    = $this->value;
                        $seqName     = "{$table}_{$col}_seq";
                        $sequences[] = "\n-- \n-- Type: SEQUENCE; Name: $seqName; Table: $table; Column: $col;\n-- ";
                        if ($drop) {
                            $sequences[] = "DROP SEQUENCE $seqName IF EXISTS CASCADE;";
                        }
                        $sequences[] = "CREATE SEQUENCE $seqName\n    INCREMENT BY 1\n    START 1\n    NO MAXVALUE\n    NO MINVALUE\n    CACHE 1;\n";
                        $sequences[] = "SELECT pg_catalog.setval('$seqName', $seqStart, true);\n";
                        $sequences[] = "ALTER TABLE $table ALTER COLUMN $col SET DEFAULT nextval('$seqName'::regclass);";
                    }
                }
                $sqls[] = $this->createTable($table, $tableDef, null, null, false);
                // constraints - to be added after data
                foreach ($cons as &$con) {
                    $con['subject'] = 'CONSTRAINT';
                }
                $constraints[] = "\n-- \n-- Type: CONSTRAINT; Table: $table;\n-- ";
                $constraints[] = implode("\n", $this->alterTable($table, array('add' => $cons), false));
                // indexes
                if (count($this->getMetaIndexes($table)) > 0) {
                    $constraints[] = "\n-- \n-- Type: INDEX; Table: $table;\n--";
                    foreach ($this->getMetaIndexes($table) as $ix) {
                        if ($drop) {
                            $constraints[] = "DROP INDEX $ix[name] IF EXISTS CASCADE;";
                        }
                        $constraints[] = $ix['definition'];
                    }
                }
                // triggers
                if (count($this->getMetaTriggers($table)) > 0) {
                    $sqls[] = "-- \n-- Type: TRIGGER; Table: $table;\n-- ";
                    foreach ($this->getMetaTriggers($table) as $tr) {
                        if ($drop) {
                            $sqls[] = "DROP TRIGGER $tr[name] IF EXISTS CASCADE;";
                        }
                        $sqls[] = $tr['definition'];
                    }
                }
            }
            /* Views */
            $views = $this->getMetaViews(null);
            $statistics .= count($views).' Views; ';
            foreach ($views as $view => $def) {
                $sqls[] = "\n-- \n-- Type: VIEW; Name: $view;\n-- ";
                $sqls[] = 'CREATE ' . ($drop? 'OR REPLACE ': '') . "VIEW $view AS";
                $sqls[] = "    $def[definition]";
            }
            /* Functions procs */
            $procs       = $this->getMetaProcedures();
            $statistics .= count($procs).' Functions and Procedures; ';
            foreach ($procs as $proc => $def) {
                $sqls[] = "\n-- \n-- Type: FUNCTION; Name: $proc;\n-- ";
                if ($drop) {
                    $sqls[] = "DROP FUNCTION IF EXISTS $proc CASCADE;";
                }
                $sqls[] = isset($v['definition'])? $v['definition']: '-- Needs PostgreSQL 8.4 or higher to do this';
            }
        }
        /* DATA */
        if ($type !== 'structure') {
            $rowCount = 0;
            foreach ($tables as $table) {
                $this->query("SELECT * FROM $table;");
                if (($count=$this->count()) > 0) {
                    $backup = array();
                    while ($row=$this->row()) {
                        $data = array();
                        foreach ($row as $v) {
                            $data[] = $v === null? 'NULL': ("'" . pg_escape_string($v) . "'");
                        }
                        $backup[] = '('.implode(', ', $data).')';
                    }
                    $sqls[] = "\n-- \n-- Type: TABLE DATA; Name: $table; Records: $count;\n-- ";
                    if ($backup) {
                        $sqls[] = "INSERT INTO \"$table\" (" . $this->fields(', ') . ") VALUES\n" . implode(",\n", $backup) . ';';
                    }
                    $rowCount += $count;
                }
            }
            $statistics .= "$rowCount Records; ";
        }
        /* TABLES - CONSTRAINTS AND SEQUENCES */
        if ($type !== 'data') {
            $sqls[] = implode("\n", $constraints) . implode("\n", $sequences);
        }
        /* * */
        $sqls[7] = $statistics;
        $sqls[] = "\nCOMMIT;";
        /* Write to file */
        $query = implode("\n", $sqls);

        if ($filename === false) {
            return $query;
        }

        return file_put_contents(
            $filename? $filename: (defined('TEMP_DIR')? TEMP_DIR: '') . 'backup ' . $this->__configs['dbname'] . date(' Y-m-d H-i-s') . '.sql',
            $query
        );
    }

    /**
     * Restore dump/SQL file to database.
     *
     * @param string $filename=null Filename to open or null=assume the filename is "TEMP_DIR/database-name.sql"
     */
    public function restore($filename=null) {
        ini_set('memory_limit', '7000M');
        set_time_limit(0);

        if (!$filename) {
            $filename = (defined('TEMP_DIR')? TEMP_DIR: '') . $this->__configs['dbname'] . '.sql';
        }

        $this->query(file_get_contents($filename));
    }
}
