<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7221
 * Created  2007/09/18
 * Updated  2013/04/07
 */

//namespace arta\db\engine;

require_once ARTA_DIR . 'interface/IDbAdvanced.php';
require_once 'MySqliAbstract.php';

/**
 * MySQL advanced class. To create instances of "IDbAbstract" for connecting
 * and accessing PostgreSQL databases. Plus implementing the advanced database
 * access methods of "IDbAdvanced" such as creating/altering tables and getting 
 * meta data.<br/>
 * The classes that implement the "IDbAdvanced" interface create a uniform cross
 * DBMS meta-data access and table create/alter platform to let the Artaengine data
 * modeling system have a robust control over the database.<br/>
 * It is possible to conver an "IDbAbstract" instance to "IDbAdvanced":<br/>
 * $advancedDbObj = Database::upgrade($abstractDbObj);<br/>
 * Or to create an "IDbAdvanced" with the same configs as an existing "IDbAbstract"<br/>
 * instance:<br/>
 * $advancedDbObj = MySQLAdvanced($abstractDbObj);<br/>
 * This class is not included in the autoload as it is recommended to create a
 * database connection and grab a database access instance from the "arta\Database"
 * object factory.<br/>
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.2.1
 * @since      1.0.0
 * @link       http://artaengine.com/api/MySqliAdvanced
 * @example    http://artaengine.com/examples/database
 * @see        http://artaengine.com/api/Database
 */
class MySqliAdvanced extends MySqliAbstract implements IDbAdvanced {

    /** true=sort the output of "metaTable()" and "metaView()" by dependency */
    public $sortMeta       = true;
    /** Put meaningful constraint names */
    public $nameConstraint = true;

    private $__meta = array();

    /**
     * Get info about the dbms and class.
     *
     * @return array Info array [@MySql.advanced.info.return]
     * @since Artaengine 1.1.0
     */
    public function info() {
        $info = parent::info();
        $info['class']   = 'Arta.MySqliAdvanced';
        $info['version'] = '1.2.1';
        return $info;
    }

    /**
     * Connect to a database. Either connection configs or an "DBIAbstract"
     * instance must be passed. If an instance of "DBIAbstract" is passed a new
     * database connection will not be made but the old connection related to the
     * instance will be set for the new "DBIAdvanced" object.
     *
     * @param IDbAbstract|array $param An instance of "IDbAbstract" or a connection configs array [@MySql.connection]
     * @throws DatabaseError
     */
    public function __construct($param) {
        if ($param instanceof IDbAbstract) {
            $this->mysqli = &$param->mysqli;
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
        $a = array();
        $this->query('SELECT * FROM '.$this->mysqli->real_escape_string($table));
        while ($row=$this->row()) {
            foreach ($row as &$v) {
                if (is_null($v)) {
                    $v = $null;
                }
            }
            $a[] = implode($delimiter, $row);
        }
        return $a;
    }

    /**
     * Copy data of an array into a table.
     *
     * @param string $table          Table name
     * @param array  $rows           Data rows
     * @param string $delimiter="\t" Separates col data in each row
     * @param string $null='\N'      Uses this string instead of null values
     * @throws DatabaseError
     */
    public function copyA2T($table, array $rows, $delimiter="\t", $null='\N') {
        foreach ($rows as $v) {
            $v = explode($delimiter, $v);
            foreach ($v as &$m) {
                $m = $m == $null ? 'NULL' : ("'" . $this->mysqli->real_escape_string($m) . "'");
            }
            $this->query('INSERT INTO ' . $this->mysqli->real_escape_string($table) . ' VALUES (' . implode(', ', $v) .")");
        }
    }

    /**
     * Get a dictionary of database data types mapped to Artaengine data types.
     *
     * @return array {db-type-string: T::TYPE_CONST,}
     * @throws DatabaseError
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
            'TINYINT',    'VARCHAR',
            /* INT */
            'BIGINT',     'BIGINT',     'INT',        'INT',        'MEDIUMINT',  'SMALLINT',
            /* TEXT */
            'TINYTEXT',   'TEXT',       'LONGTEXT',   'MEDIUMTEXT', 'TEXT',       'TEXT',
            /* CHAR VCHAR */
            'VARCHAR',    'VARCHAR',    'VARCHAR',    'VARCHAR',    'CHAR',
            /* DATE */
            'DATE',       'TIME',       'TIME',       'TIMESTAMP',  'TIMESTAMP',  'DATETIME',
            /* FLOAT */
            'FLOAT',      'DOUBLE',
            /* NUMMERIC */
            'DECIMAL',    'NUMERIC',
            /* BLOB */
            'MEDIUMBLOB', 'LONGBLOB',   'BLOB',
        );
        $const = array(
            /* BOOL, IP */
             BOOL,         IP,
            /* INT */
             SERIAL_LONG,  LONG,         SERIAL,       INT,          INT,          SMALL,
            /* TEXT */
             TEXT,         DICTIONARY,   TEXT,         TEXT,         WYSIWYG,      TEXT,
            /* CHAR VCHAR */
             EMAIL,        URL,          PASSWORD,     STRING,       CHAR,
            /* DATE */
             DATE,         TIMEZ,        TIME,         TIMESTAMPZ,   TIMESTAMP,    DATETIME,
            /* FLOAT */
             FLOAT,        DOUBLE,
            /* NUMMERIC */
             NUMERIC,      NUMERIC,
            /* BLOB */
             BLOB,         BLOB,         BLOB,
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
     * @param const     $type            T::TYPE_CONST (Artaengine data type)
     * @param int|array $length=null     Max length or [min-length, max-length]
     * @param bool      $unsigned=false  Signed or Unsigned (if supported by database)
     * @return string Database column type definition
     */
    public function toType($type, $length=null, $unsigned=false) {
        if (!is_int($type)) {
            return $type . ($length ? "($length)" : '') . ($unsigned ? ' UNSIGNED' : '');
        }

        $len   = is_array($length)? $length[1]: $length;
        $types = $this->artaType2DbType();

        if (($type == STRING || $type == CHAR) && (int)$len > 255) {
            $type = TEXT;
        }

        switch ($type) {
            case URL:
            case EMAIL:
            case STRING:
                $len = $len? "($len)": '(255)';
                break;

            case PASSWORD:
                $len = $len? "($len)": '(100)';
                break;

            case CHAR:
                $len = '(1)';
                break;

            case TEXT:
                if ($len) {
                    if    ($len < 256)       $types[$type] = 'TINYTEXT';
                    elseif ($len < 65536)    $types[$type] = 'TEXT';
                    elseif ($len < 16777216) $types[$type] = 'MEDIUMTEXT';
                    else                     $types[$type] = 'LONGTEXT';
                }
                $len = null;
                break;

            case BLOB:
                if ($len) {
                    if     ($len < 65536)    $types[$type] = 'BLOB';
                    elseif ($len < 16777216) $types[$type] = 'MEDIUMBLOB';
                    else                     $types[$type] = 'LONGBLOB';
                }
                $len = null;
                break;

            case SERIAL:
                $lenTmp = ' UNSIGNED NOT NULL AUTO_INCREMENT';
            case INTEGER:
                switch ($len) {
                    case 1:  $types[$type] = 'TINYINT';   break;
                    case 2:  $types[$type] = 'SMALLINT';  break;
                    case 3:  $types[$type] = 'MEDIUMINT'; break;
                    case 8:  $types[$type] = 'BIGINT';    break;
                    default: $types[$type] = 'INT';
                }
                $len = isset($lenTmp)? $lenTmp: ($unsigned? ' UNSIGNED': null);
                break;

            case LONG:
            case SMALL:
                $len = $unsigned? ' UNSIGNED': null;
                break;

            case SERIAL_LONG:
                $len = ' UNSIGNED NOT NULL AUTO_INCREMENT';
                break;

            case FLOAT:
            case DOUBLE:
                if ($len !== null && $len !== '') {
                    if (is_array($length)) {
                        $len = '('.implode(', ', $length).')';
                    } else {
                        if ($len < 0)  $len = 0;
                        if ($len > 53) $len = 53;
                        $types[$type] = $len>23? 'DOUBLE': 'FLOAT';
                        $len = "($len)";
                    }
                }
                if ($unsigned) {
                    $len .= ' UNSIGNED';
                }
                break;

            case NUMERIC:
                if ($len === null) {
                    $len = 10;
                }
                $s = 0;
                if (is_array($length)) {
                    list($p, $s) = $length;
                } else {
                    $p = $len;
                }
                $len = "($p, $s)";
                break;

            case IP:
                $len = '(20)';
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
     * @param  string $prefix Such as 'PK', 'FK', 'CK', etc.
     * @param  string $table  Table name
     * @param  string $name   Constraint main name
     * @return string Constraint name
     */
    public function constraintName($prefix, $table, $name) {
        if (!$this->nameConstraint) {
            return '';
        }

        $max = MySqliAbstract::MAX_IDENTIFIER_NAME;
        if (strlen($name = strtolower("{$prefix}_{$table}_{$name}")) > $max) {
            if (strlen($name = strtolower("{$prefix}_" . sha1($table.$name))) > $max) {
                $name = substr($name, 0, $max);
            }
        }

        return "`$name`";
    }

    private function __addConstraint($table, $def, $type=null, $alter='') {
        $cols = isset($def['cols'])? (array)$def['cols']: array();
        if ($cols) {
            foreach ($cols as &$c) {
                $c = "`$c`";
            }
        }

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

                foreach ($refs as &$r) {
                    $r = "`$r`";
                }

                $rKeys = array_keys($refs);
                foreach ($rKeys as &$rk)
                    $rk = "`$rk`";

                if (!isset($def['name']) || !is_string($def['name'])) {
                    $name = $this->__joinKeys($refs, false);
                }

                if (!isset($def['delete'])) {
                    $def['delete'] = 'CASCADE';
                }

                $exp = '('.implode(', ', $rKeys).')'.
                    " REFERENCES `$rTable` (".
                    implode(', ', $refs).")".
                    /*(isset($def['match']) && $def['match']? " $def[match]": '').*/
                    (isset($def['delete']) && $def['delete'] &&
                        strtoupper($def['delete']) !== 'NO ACTION' &&
                        strtoupper($def['delete']) !== 'RESTRICT'?
                            " ON DELETE $def[delete]": '').
                    (isset($def['update']) && $def['update'] &&
                        strtoupper($def['update']) !== 'NO ACTION' &&
                        strtoupper($def['update']) !== 'RESTRICT'?
                            " ON UPDATE $def[update]": '')/*.
                    (isset($def['defer']) && $def['defer']? " $def[defer]": '')*/;

                return str_replace('COLUMN ', '', $alter).
                    "FOREIGN KEY ".(isset($def['name']) && is_string($def['name'])?
                    "`$def[name]`": $this->constraintName($prefix, $table, $name))." $exp";

            case 'PRIMARY KEY':
                $prefix = 'pk';
                if (!isset($def['name']) || !is_string($def['name'])) {
                    $name = $this->__joinKeys($def['cols']);
                }
                $exp = '('.implode(', ', $cols).')';
                break;

            case 'UNIQUE':
                $prefix = 'un';
                if (!isset($def['name']) || !is_string($def['name'])) {
                    $name = $this->__joinKeys($def['cols']);
                }
                $exp = '('.implode(', ', (array)$cols).')';
                break;

            default:
                return null;
        }

        return str_replace('COLUMN ', '', $alter).
            "CONSTRAINT ".(isset($def['name']) && is_string($def['name'])?
            "`$def[name]`": $this->constraintName($prefix, $table, $name))." $type $exp";
    }

    /**
     * TABLE > Define column for CREATE/ALTER TABLE
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
        if ($type === SERIAL || $type === SERIAL_LONG) {
            unset($def['default']);
        }

        if ($type == TIMESTAMP && !array_key_exists('default', $def)) {
            $def['default'] = null;
        }

        if (array_key_exists('default', $def)) {
            if (($default=$def['default']) === null) {
                $default = $type === TIMESTAMP?
                    ($nullable == 'NULL'? 'DEFAULT NULL': 'DEFAULT 0'):
                    ($nullable == 'NULL'? 'DEFAULT NULL': '');
            } else {
                if ($type === BOOL) {
                    $default = $default ? '0' : '1';
                }
                /* bad */
                $cot = "'";
                $defaultL = strtolower($default);
                if (strpos($default, ':')!==false ||
                  (strpos($default, '(')!==false && strpos($default, ')')!==false) ||
                  $defaultL === 'current_date' ||
                  $defaultL === 'current_time' ||
                  $defaultL === 'current_timestamp' ||
                  $defaultL === 'localtime' ||
                  $defaultL === 'localtimestamp') {
                    $cot = '';
                }
                $default = 'DEFAULT '.$cot.$default.$cot;
            }
        }
        /* db def of type */
        $type = $this->toType(
            $type,
            isset($def['len'])?      $def['len']: null,
            isset($def['unsigned'])? (bool)$def['unsigned']: false
        );
        $sqls[] = ($alter? $alter: '    ')."`{$name}` $type $nullable $default";
        /* constraint check */

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
                    (isset($ref['reference'])? $ref['reference']: $ref['references'])
            );
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
                        if (isset($constraint['name'])) {
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
                    $def['unsigned'] = true;
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
                $c = $param == 'CONNECTION'      || $param == 'DATA DIRECTORY' ||
                     $param == 'INDEX DIRECTORY' || $param == 'PASSWORD' ||
                     $param == 'COMMENT'? "'": '';
                $val = (is_numeric($param)? '': "$param=") . $c . (is_array($val)? implode(', ', $val): $val) . $c;
            }
            $extra = ' ' . implode(' ', $extra);
        }

        $sql = 'CREATE TABLE `' . strtolower($table) . "` (\n" . implode(",\n", array_merge($sqls, $cons)) . "\n)$extra;";

        return $exe? $this->query($sql): $sql;
    }

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
        $sqls    = array();
        $cons    = array();
        $sqlStrs = array();
        /* DROP */
        foreach ($d as $def) {
            if (is_array($def)) {
                // drop constraint
                if (isset($def['subject']) && strtoupper($def['subject']) === 'CONSTRAINT') {
                    $meta = $this->getMetaConstraints($table);
                    if (isset($meta[$def['name']])) {
                        switch ($meta[$def['name']]['type']) {
                            case 'PRIMARY KEY':
                                $sqls[] = 'DROP PRIMARY KEY';
                                break;
                            case 'FOREIGN KEY':
                                $sqls[] = "DROP FOREIGN KEY `$def[name]`";
                                break;
                            default:
                                $sqls[] = "DROP INDEX `$def[name]`";
                        }
                    }
                }
                // drop column
                elseif (!isset($def['subject']) || strtoupper($def['subject']) === 'COLUMN') {
                    $sqls[] = "DROP COLUMN `$def[name]`";
                }
            } else {
                $sqls[] = "DROP COLUMN `$def`";
            }
        }
        /* ADD */
        foreach ($a as $col => $def) {
            if (is_array($def)) {
                // add constraint
                if ((isset($def['subject'])? $def['subject']: null) === 'CONSTRAINT') {
                    if ($def=$this->__addConstraint($table, $def)) {
                        $cons[] = 'ADD '.trim($def);
                    }
                }
                // add column
                else {
                    $this->__addColumn($table, $def, $col, $sqls, $cons, 'ADD COLUMN ');
                }
            }
        }
        /* RENAME */
        foreach ($r as $i => $def) {
            if (is_numeric($i)) {
                $sqlStrs[] = "ALTER TABLE `$table` RENAME TO `$def`;";
                $table = $def;
            }
        }
        /* ALTER */
        if (is_array($extra)) {
            foreach ($extra as $param => $val) {
                $param = strtoupper($param);
                $c = $param=='CONNECTION'      || $param=='DATA DIRECTORY' ||
                     $param=='INDEX DIRECTORY' || $param=='PASSWORD' ||
                     $param=='COMMENT'? "'": '';
                $sqls[] = (is_numeric($param)? '': "$param=") . $c . (is_array($val)? implode(', ', $val): $val) . $c;
            }
        }
        foreach ($l as $col => $def) {
            // mysql engine
            if (isset($def['engine'])) {
                $sqls[] = "  ENGINE=$def[engine]";
            }
            // default
            if (isset($def['default'])) {
                $defaultL = strtolower($def['default']);
                if ($defaultL === 'now()' || $defaultL === 'now' || $defaultL === 'current_timestamp') {
                
                } elseif (strtolower($def['default']) === 'drop') {
                    $sqlStrs[] = "ALTER TABLE `$table` ALTER COLUMN `$col` DROP DEFAULT;";
                    unset($def['default']);
                } else {
                    $sqlStrs[] = "ALTER TABLE `$table` ALTER COLUMN `$col` SET DEFAULT " .
                        ($def['default']===null || $defaultL==='null'? 'NULL': "'$def[default]'") . ';';
                    unset($def['default']);
                }
            }
            // type
            if (isset($def['type'])) {
                $change = ' '.$this->toType(
                    $def['type'],
                    isset($def['len'])?      $def['len']:      null,
                    isset($def['unsigned'])? $def['unsigned']: false
                );
            } elseif (isset($def['nullable']) || isset($def['required']) || isset($def['default'])) {
                // needs old def - mysql way
                $meta = $this->getMetaTable($table, 1);
                $change = ' ' . $meta[$col]['dbms']['def'];
            } else {
                continue;
            }
            // nullable
            if (isset($def['nullable'])) {
                $change .= $def['nullable']? ' NULL': ' NOT NULL';
            } elseif (isset($def['required'])) {
                $change .= $def['nullable']? ' NOT NULL': ' NULL';
            }
            // default
            if (isset($def['default'])) {
                $change .=
                    ' DEFAULT '.($defaultL === 'now()' || $defaultL === 'now' || $defaultL === 'current_timestamp'?
                        'NOW()':
                        "'$def[default]'");
            }
            /* * */
            $sqls[] = (isset($r[$col])?
                ("CHANGE COLUMN `$col` `" . $r[$col] . "` ") :
                ("MODIFY COLUMN `$col` ") . $change);
        }
        /* QUERY */
        $sqls = array_merge($sqls, $cons);
        if ($sqls) {
            foreach ($sqls as &$sql) {
                $sql = '    ' . $sql;
            }
            $sqlStrs[] = "ALTER TABLE `$table`\n" . implode(",\n", $sqls) . ';';
        }

        if ($exe) {
            $ok = true;
            foreach ($sqlStrs as $sqlStr) {
                if ($sqlStr) {
                    $this->query($sqlStr)? null: $ok=false;
                }
            }
            return $ok;
        } else {
            return $sqlStrs;
        }
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
        $this->query(
            "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_NAME IN
             ('REFERENTIAL_CONSTRAINTS') AND TABLE_SCHEMA='information_schema'"

        );
        $rc = $this->row();
        $sqlStr = "
        SELECT distinct
            tc.CONSTRAINT_NAME AS name,
            tc.CONSTRAINT_TYPE AS type,
            tc.TABLE_NAME      AS xtable,";
        if ($rc) {
            $sqlStr .= "
                rc.UPDATE_RULE    AS on_update,
                rc.DELETE_RULE     AS on_delete,
                rc.MATCH_OPTION    AS match_type,";
        }
        $sqlStr .= "
            kcu.COLUMN_NAME    AS col_name,
            kcu.REFERENCED_TABLE_NAME  AS ref_table,
            kcu.REFERENCED_COLUMN_NAME AS ref_field
        FROM
            information_schema.TABLE_CONSTRAINTS tc ";
        if ($rc) {
            $sqlStr .= "
            LEFT JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
                ON  tc.TABLE_NAME        = rc.TABLE_NAME
                AND tc.CONSTRAINT_SCHEMA = rc.CONSTRAINT_SCHEMA
                AND tc.CONSTRAINT_NAME   = rc.CONSTRAINT_NAME ";
        }
        $sqlStr .= "
            LEFT JOIN information_schema.KEY_COLUMN_USAGE kcu
                ON  tc.TABLE_NAME        = kcu.TABLE_NAME
                AND tc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
                AND tc.CONSTRAINT_NAME   = kcu.CONSTRAINT_NAME
        WHERE
            tc.TABLE_SCHEMA = '".$this->__configs['dbname']."'";
        if ($constraint) {
            $sqlStr .= " AND tc.CONSTRAINT_NAME = '" . $this->mysqli->real_escape_string($constraint) . "'";
        }
        if ($table) {
            $sqlStr .= " AND tc.TABLE_NAME = '" . $this->mysqli->real_escape_string($table) . "'";
        }
        $this->query($sqlStr);
        $cons = array();
        while ($row=$this->row()) {
            $name = strtolower($row['name']);
            if (isset($cons[$name])) {
                $def = $cons[$name];
            } else {
                $def = array(
                    'name'      => $name,
                    'type'      => $row['type'],
                    'table'     => $row['xtable'],
                    'cols'      => array(),
                    'reference' => array(
                        'table' => $row['ref_table'],
                        'cols'  => array(),
                    ),
                );
                if ($rc) {
                    $def['delete'] = $row['on_delete'] === 'NO ACTION'? '': $row['on_delete'];
                    $def['update'] = $row['on_update'] === 'NO ACTION'? '': $row['on_update'];
                }
                if ($row['type'] === 'FOREIGN KEY') {
                    if ($rc && ($match=strtolower($row['match_type'])) === 'full') {
                        $match = 'MATCH FULL';
                    } elseif ($rc &&  $match === 'partial') {
                        $match = 'MATCH PARTIAL';
                    } else { 
                        $match = '';
                    }
                    $def['match'] = $match;
                    $def['is_deferrable'] = '';
                    $def['initially_deferred'] = '';
                    $def['defer'] = '';
                }
            }
            // cols
            if ($row['col_name']) {
                $def['cols'][$row['col_name']] = $row['col_name'];
            }
            // reference cols
            if ($row['ref_table']) {
                $def['reference']['cols'][$row['col_name']] = $row['ref_field'];
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
        $this->query("
            SELECT
                referenced_table_name parent
            FROM
                information_schema.KEY_COLUMN_USAGE
            WHERE
                    referenced_table_name IS NOT NULL
                AND REFERENCED_TABLE_SCHEMA = '".$this->__configs['dbname']."'
                AND table_name = '$table'
            ORDER BY
                referenced_table_name;");

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
        $this->query("
        SELECT distinct
            kcu.COLUMN_NAME AS col
        FROM
            information_schema.TABLE_CONSTRAINTS tc
            LEFT JOIN information_schema.KEY_COLUMN_USAGE kcu
                ON  tc.TABLE_NAME        = kcu.TABLE_NAME
                AND tc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
                AND tc.CONSTRAINT_NAME   = kcu.CONSTRAINT_NAME
        WHERE
                tc.TABLE_SCHEMA = '" . $this->__configs['dbname'] . "'
            AND tc.TABLE_NAME = '$table'
            AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'");

        $rows  = array();
        while ($row=$this->row()) {
            $rows[$row['col']] = $row['col'];
        }

        return $rows;        
    }

    /**
     * Get metadata of tables.
     *
     * @param  string $table=null Table name, null=return a list of database tables
     * @param  int    $shape=2 1=return column definitions only, 2=return column definitions
     *                and constrain statuses, 3=return column definitions and constraint definitions
     * @return array if table=null {table-name: table-name,} else {col-name: {meta},} where meta is: [DbAdvanced.getMetaTable]
     * @throws DatabaseError
     */
    public function getMetaTable($table=null, $shape=2) {
        if (isset($this->__meta[$cacheKey="T_$table$shape"])) {
            return $this->__meta[$cacheKey];
        }

        $meta = array();
        if (!$table) {
            $this->query("
            SELECT TABLE_NAME, TABLE_SCHEMA
            FROM   information_schema.TABLES
            WHERE  TABLE_TYPE = 'BASE TABLE' AND TABLE_SCHEMA = '" . $this->__configs['dbname'] . "' ORDER BY TABLE_NAME");

            while ($row=$this->row()) {
                $meta[$row['TABLE_NAME']] = $row['TABLE_NAME'];
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

        $pks = array();
        $uns = array();
        $cks = array();
        $fks = array();

        if ($shape > 1) {
            $cons = $this->getMetaConstraints($table);
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

                    case 'PRIMARY KEY': $pks += $con['cols'];
                        $pkCount = count($pks);
                }
            }
        }

        $this->query("
        SELECT
            ORDINAL_POSITION         AS number,
            COLUMN_NAME              AS name,
            DATA_TYPE                AS type,
            CHARACTER_MAXIMUM_LENGTH AS len,
            IS_NULLABLE              AS xnull,
            COLUMN_DEFAULT           AS xdefault,
            COLUMN_TYPE              AS def
        FROM
            information_schema.COLUMNS
        WHERE
            TABLE_SCHEMA = '" . $this->__configs['dbname'] . "'" .
            " AND lower(TABLE_NAME) = '" . $this->mysqli->real_escape_string(strtolower($table)) . "'");

        $types = $this->dbType2artaType();

        foreach ($this->rows() as $row) {
            $type = strtoupper($row['type']);
            $name = strtolower($row['name']);

            switch ($type) {
                case 'INT':
                    $len = 4;
                    break;

                case 'TINYINT':
                    $len = 1;
                    break;

                case 'SMALLINT':
                    $len = 2;
                    break;

                case 'MEDIUMINT':
                    $len = 3;
                    break;

                case 'BIGINT':
                    $len = 8;
                    break;

                default:
                    $len = $row['len'];
            }

            $lenDb = null;

            if (strpos($row['def'], '(') !== false) {
                $lenDb = explode('(', $row['def']);
                $lenDb = explode(')', $lenDb[1]);
                $lenDb = $lenDb[0];
            }

            $meta[$name] = array(
                'dbms'     => array(
                    'type' => $type,
                    'def'  => $row['def'],
                    'len'  => $lenDb,
                ),
                'unsigned'    => strpos($row['def'], 'unsigned')!==false,
                'type'        => $types[trim(str_replace('UNSIGNED', '', strtoupper($type)))],
                'len'         => $len,
                'nullable'    => strtolower($row['xnull']) != 'no',
                'has_default' => !is_null($row['xdefault']),
                'default'     => trim($row['xdefault'], "'"),
            );

            $meta[$name]['required'] = !$meta[$name]['nullable'];

            if ($shape > 1) {
                $meta[$name]['P']  = $pk=isset($pks[$name]);
                $meta[$name]['PC'] = $pkCount;
                $meta[$name]['S']  = $serial = $pk? $this->isSerial($table, $name): false;
                $meta[$name]['U']  = isset($uns[$name]);
                $meta[$name]['C']  = isset($cks[$name]);
                $meta[$name]['F']  = isset($fks[$name]);
                if ($serial) {
                    if ($meta[$name]['type'] == INT) {
                        $meta[$name]['type'] = SERIAL;
                    } elseif ($meta[$name]['type'] == LONG) {
                        $meta[$name]['type'] = SERIAL_LONG;
                    }
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

        foreach (explode(' ', str_replace(array('`', ';'), '', substr($def, strpos($def, 'from')+4))) as $token) {
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
     *         {view-name: {meta},} and if view name is not specified return: [@DbAdvanced.getMetaViews]
     * @throws DatabaseError
     */
    public function getMetaViews($view=null, $thin=false) {
        if (isset($this->__meta[$cacheKey="V_$view"])) {
            return $view ? $this->__meta[$cacheKey][$view] : $this->__meta[$cacheKey];
        }
        $wStr = '';
        if ($view) {
            $wStr = strpos($view, '.') === false?
                ("AND TABLE_NAME = '" . $this->mysqli->real_escape_string($view) . "'"):
                ("AND CONCAT(TABLE_SCHEMA, '.', TABLE_NAME) = '" . $this->mysqli->real_escape_string($view) . "'");
        }

        $this->query("
        SELECT
            TABLE_SCHEMA,
            TABLE_NAME,
            DEFINER,
            VIEW_DEFINITION
        FROM
            information_schema.VIEWS
        WHERE
            TABLE_SCHEMA = '" . $this->__configs['dbname'] . "' $wStr;");

        $views = array();

        if ($thin) {
            foreach ($this->rows() as $row)
                $views[$row['TABLE_NAME']] = $row['TABLE_NAME'];
            return $views;
        }

        foreach ($this->rows() as $row) {
            $this->query("SHOW COLUMNS FROM $row[TABLE_NAME]");
            $cols = array();
            foreach ($this->rows() as $col) {
                $cols[] = $col['Field'];
            }
            $views[$row['TABLE_NAME']] = array(
                'schema'     => $row['TABLE_SCHEMA'],
                'name'       => $row['TABLE_NAME'],
                'owner'      => $row['DEFINER'],
                'definition' => $row['VIEW_DEFINITION'],
                'cols'       => $cols,
            );
        }

        $this->__meta[$cacheKey]=$views;

        if ($view) {
            $views = $views[$view];
        } elseif ($this->sortMeta) {
            $sorted = array();
            foreach ($views as $view => $def) {
                $this->__sortViews($sorted, $views, $view);
            }
            return $this->__meta[$cacheKey]=$sorted;
        }

        return $views;
    }

    /**
     * Get database user's meta data.
     *
     * @return array {user-name: {meta},} meta= [@DbAdvanced.metaUsers]
     * @throws DatabaseError
     */
    public function getMetaUsers() {
        $this->query("SELECT * FROM mysql.user");
        $users = array();
        foreach ($this->rows() as $row) {
            $users[$row['User']] = array(
                'name'     => $row['User'],
                'sysid'    => $row['User'],
                'createdb' => $row['Create_priv']=='Y',
                'super'    => $row['Super_priv']=='Y',
                'catupd'   => $row['Update_priv']=='Y',
            );
        }
        return $users;
    }

    /**
     * Get table indexes meta data (Experimental - subject to change).
     *
     * @param  string table table name, null=return all
     * @return array array {index-name: {meta},} meta= [@DbAdvanced.getMetaIndexes]
     * @throws DatabaseError
     */
    public function getMetaIndexes($table=null) {
        if (!$table) {
            return null;
        }
        if (isset($this->__meta[$cacheKey = "I_$table"])) {
            return $this->__meta[$cacheKey];
        }

        $this->query('SHOW CREATE TABLE '.$this->mysqli->real_escape_string($table));
        $meta = array();
        $row  = $this->row();

        foreach (explode("\n", $row['Create Table']) as $v) {
            $v = trim($v);
            $type =
                substr($v, 0, 12)=='FULLTEXT KEY'? ' FULLTEXT': (
                    substr($v, 0, 11)=='SPATIAL KEY'? ' SPATIAL': (
                        substr($v, 0, 3)=='KEY'? '': null));

            if ($type !== null) {
                $vA = explode(' ', $v);
                $str = strpos($v, 'USING HASH')!==false? 'USING HASH':
                    (strpos($v, 'USING RTREE')!==false? 'USING RTREE': 'USING BTREE');
                if ($vA[2][$l=strlen($vA[2])-1] == ',') {
                    $vA[2] = substr($vA[2], 0, $l);
                }
                $meta[$v['name']] = array(
                    'name'       => $vA[1],
                    'indexes'    => null,
                    'definition' => "CREATE$type INDEX $vA[1] $str ON `$table` $vA[2];",
                );
            }
        }

        return $this->__meta[$cacheKey]=$meta;
    }

    /**
     * Get trigger(s) meta data (Experimental - subject to change).
     *
     * @param string $table=null Table name, null=return all triggers
     * @return array {trigger-name: {meta},} meta= [@DbAdvanced.getMetaTriggers]
     * @throws DatabaseError
     */
    public function getMetaTriggers($table=null) {
        if (isset($this->__meta[$cacheKey="T_$table"])) {
            return $this->__meta[$cacheKey];
        }

        $this->query("
        SELECT DISTINCT *
        FROM   information_schema.TRIGGERS
        WHERE  TRIGGER_SCHEMA = '".$this->__configs['dbname']."' ".
            ($table !== null? "AND EVENT_OBJECT_TABLE = '".
                $this->mysqli->real_escape_string($table)."'": ''));

        $meta = array();

        foreach ($this->rows() as $row) {
            if (!isset($meta[$name=$row['TRIGGER_NAME']])) {
                $meta[$name] = array(
                    'name'         => $name,
                    'events'       => array(),
                    'table'        => $row['EVENT_OBJECT_TABLE'],
                    'action'       => $row['ACTION_STATEMENT'],
                    'time'         => $row['ACTION_TIMING'],
                    'orientation'  => $row['ACTION_ORIENTATION'],
                    'table_schema' => $row['EVENT_OBJECT_SCHEMA'],
                    'schema'       => $row['TRIGGER_SCHEMA'],
                );
            }
            $meta[$name]['events'][] = $row['EVENT_MANIPULATION'];
            $meta[$name]['definition'] = "CREATE TRIGGER $name " . $meta[$name]['time'] . ' ' .
                implode(' OR ', $meta[$name]['events'])."\n    ON " . $meta[$name]['table'] .
                " FOR EACH {$meta[$name]['orientation']}\n    " . $meta[$name]['action'] . ';';
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

        $wStr = '';

        if ($proc !== null) {
            $wStr = $type===null?
                ('AND ROUTINE_NAME = ' . $this->mysqli->real_escape_string($proc)):
                ('AND ROUTINE_TYPE = ' . $this->mysqli->real_escape_string($type));
        }

        $this->query("
        SELECT
            ROUTINE_SCHEMA,
            ROUTINE_NAME,
            ROUTINE_TYPE,
            EXTERNAL_LANGUAGE,
            DTD_IDENTIFIER,
            ROUTINE_DEFINITION,
            ROUTINE_BODY
        FROM
            information_schema.ROUTINES
        WHERE
            ROUTINE_SCHEMA = '" . $this->__configs['dbname'] . "' $wStr");

        $meta = array();

        foreach ($this->rows() as $row) {
            $definition = '';
            if ($row['ROUTINE_TYPE'] == 'FUNCTION') {
                $this->query("SHOW CREATE FUNCTION $row[ROUTINE_NAME]");
                $row = $this->row();
                list(, $definition) = explode('FUNCTION', $row['Create Function']);
                $definition = "CREATE FUNCTION$definition;";
            } elseif ($row['ROUTINE_TYPE'] == 'PROCEDURE') {
                $this->query("SHOW CREATE PROCEDURE $row[ROUTINE_NAME]");
                $row = $this->row();
                list(, $definition) = explode('PROCEDURE', $row['Create Procedure']);
                $definition = "DELIMITER //\n\n CREATE PROCEDURE$definition; \n\nDELIMITER ;";
            }

            list($datatype,) = explode(' CHARSET ', $row['DTD_IDENTIFIER']);

            $datatype = trim($datatype);
            $meta[$row['ROUTINE_NAME']] = array(
                'schema'     => $row['ROUTINE_SCHEMA'],
                'name'       => $row['ROUTINE_NAME'],
                'type'       => $row['ROUTINE_TYPE'],
                'language'   => $row['EXTERNAL_LANGUAGE'],
                'udt_type'   => $datatype,
                'data_type'  => $datatype,
                'body'       => $row['ROUTINE_BODY'],
                'def'        => $row['ROUTINE_DEFINITION']? $row['ROUTINE_DEFINITION']:
                    '-- NEEDS SUPER USER PRIVILEGE --',
                'definition' => $definition,
            );
        }

        return $this->__meta[$cacheKey]=$meta;
    }

    /**
     * Get information about a table. The result is DBMS specific.
     *
     * @param string $table=null Table name
     * @return array [@MySql.getMetaTableStatus]
     * @throws DatabaseError
     */
    public function getMetaTableStatus($table=null) {
        if (!$table) {
            return array(
                'engine'          => null,
                'version'         => null,
                'row_format'      => null,
                'rows'            => null,
                'avg_row_length'  => null,
                'data_length'     => null,
                'max_data_length' => null,
                'index_length'    => null,
                'data_free'       => null,
                'serial'          => null,
                'create_time'     => null,
                'update_time'     => null,
                'check_time'      => null,
                'collate'         => null,
                'charset'         => null,
                'checksum'        => null,
                'options'         => null,
                'comment'         => null,
                'tablespace'      => null,
            );
        }

        if (isset($this->__meta[$cacheKey="TS_$table"])) {
            return $this->__meta[$cacheKey];
        }

        $table = $this->mysqli->real_escape_string($table);
        $tablespace = null;
        if ($this->mysqli->server_version >= 50160) {
            $this->query("
            SELECT
                TABLESPACE_NAME
            FROM
                information_schema.FILES
            WHERE 
                    TABLE_SCHEMA = '" . $this->__configs['dbname'] . "'
                AND TABLE_NAME = '$table';
            ");

            if ($row=$this->row()) {
                $tablespace = $row['TABLESPACE_NAME'];
            }
        }

        $this->query("
        SELECT
            *
        FROM
            information_schema.TABLES T,
            information_schema.COLLATION_CHARACTER_SET_APPLICABILITY C
        WHERE 
                C.collation_name = T.table_collation
            AND T.TABLE_SCHEMA = '" . $this->__configs['dbname'] . "'
            AND T.TABLE_NAME = '$table';
        ");

        if ($row=$this->row()) {
            return $this->__meta[$cacheKey] = array(
                'engine'          => $row['ENGINE'],
                'version'         => $row['VERSION'],
                'row_format'      => strtoupper($row['ROW_FORMAT']),
                'rows'            => $row['TABLE_ROWS'],
                'avg_row_length'  => $row['AVG_ROW_LENGTH'],
                'data_length'     => $row['DATA_LENGTH'],
                'max_data_length' => $row['MAX_DATA_LENGTH'],
                'index_length'    => $row['INDEX_LENGTH'],
                'data_free'       => $row['DATA_FREE'],
                'serial'          => $row['AUTO_INCREMENT'],
                'create_time'     => $row['CREATE_TIME'],
                'update_time'     => $row['UPDATE_TIME'],
                'check_time'      => $row['CHECK_TIME'],
                'collate'         => $row['TABLE_COLLATION'],
                'charset'         => $row['CHARACTER_SET_NAME'],
                'checksum'        => $row['CHECKSUM'],
                'options'         => $row['CREATE_OPTIONS'],
                'comment'         => $row['TABLE_COMMENT'],
                'tablespace'      => $tablespace,
            );
        } else {
            return array(
                'engine'          => null,
                'version'         => null,
                'row_format'      => null,
                'rows'            => null,
                'avg_row_length'  => null,
                'data_length'     => null,
                'max_data_length' => null,
                'index_length'    => null,
                'data_free'       => null,
                'serial'          => null,
                'create_time'     => null,
                'update_time'     => null,
                'check_time'      => null,
                'collate'         => null,
                'charset'         => null,
                'checksum'        => null,
                'options'         => null,
                'comment'         => null,
                'tablespace'      => null,
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
     * Dump database to file. (Experimental).
     *
     * @param string|bool $filename=null File-path to write dump to or false=return dump or
     *        null=dump to file in temp directory as "database-name.sql"
     * @param array $configs=array [@MySQLAdvanced.backup.configs]
     * @return void|string if filename=null returns query
     * @throws DatabaseError
     */
    public function backup($filename=null, array $configs=array()) {
        ini_set('memory_limit', '7000M');
        set_time_limit(0);
        $type = isset($configs['type'])? $configs['type']: 'full';
        $drop = isset($configs['drop'])? $configs['drop']: false;
        $sqls = array(
            '-- ',
            '-- Artaengine MySQL database dump',
            '-- Created by Artaengine DbAdvanced (MySQLi) 1.2.1',
            '-- MySQL Server: ' . $this->mysqli->server_version,
            '-- Database: ' . $this->__configs['dbname'],
            '-- Create time: ' . date('Y/m/d H:i:s'),
            '-- ',
            null,
            "-- \n",
            'SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT;',
            'SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS;',
            'SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION;',
            'SET NAMES utf8;',
            'SET AUTOCOMMIT = 0;',
            'SET FOREIGN_KEY_CHECKS=0;',
            "\n-- START TRANSACTION;",
        );
        /* TABLES */
        $tables = $this->getMetaTable();
        $statistics = '-- ' . count($tables) . ' Tables; ';
        if ($type !== 'data') {
            if ($drop) {
                $sqls[] = "\n-- \n-- Type: DROP TABLES;\n-- ";
                foreach (array_reverse($tables) as $table) {
                    $sqls[] = "DROP TABLE $table;";
                }
            }
            foreach ($tables as $table) {
                $sqls[] = "\n-- \n-- Type: TABLE; Name: $table;\n-- ";
                $this->query("SHOW CREATE TABLE `$table`;");
                $row = $this->row();
                $sqls[] = $row['Create Table'].';';
                // triggers
                if (count($this->getMetaTriggers($table)) > 0) {
                    $sqls[] = "-- \n-- Type: TRIGGER; Table: $table;\n-- ";
                    foreach ($this->getMetaTriggers($table) as $tr) {
                        if ($drop) {
                            $sqls[] = "DROP TRIGGER $tr[name] IF EXISTS;";
                        }
                        $sqls[] = $tr['definition'].';';
                    }
                }
            }
            /* Views */
            $views = $this->getMetaViews(null);
            $statistics .= count($views).' Views; ';
            foreach ($views as $view => $def) {
                $sqls[] = "\n-- \n-- Type: VIEW; Name: $view;\n-- ";
                $sqls[] = 'CREATE '.($drop? 'OR REPLACE ': '')."VIEW $view AS";
                $sqls[] = '    '.$def['definition'].';';
            }
            /* Functions procs */
            $procs = $this->getMetaProcedures();
            $statistics .= count($procs).' Functions and Procedures; ';
            foreach ($procs as $proc => $def) {
                $sqls[] = "\n-- \n-- Type: $def[type]; Name: $proc;\n-- ";
                if ($drop) {
                    $sqls[] = "DROP $proc[type] IF EXISTS $proc;";
                }
                $sqls[] = $def['definition'].';';
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
                            $data[] = $v===null? 'NULL': "'".
                                $this->mysqli->real_escape_string($v)."'";
                        }
                        $backup[] = '('.implode(', ', str_replace('	', '\t', $data)).')';
                    }
                    $sqls[] = "\n-- \n-- Type: TABLE DATA; Name: $table; Records: $count;\n-- ";
                    if ($backup) {
                        $sqls[] = "INSERT INTO `$table` (".$this->fields(', ').
                            ") VALUES\n".implode(",\n", $backup).";";
                    }
                    $rowCount += $count;
                }
            }
            $statistics .= "$rowCount Records; ";
        }
        /* * */
        $sqls[7] = $statistics;
        $sqls[] = "\n-- COMMIT;";
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
     * @throws DatabaseError
     */
    public function restore($filename=null) {
        ini_set('memory_limit', '7000M');
        set_time_limit(0);
        if (!$filename) {
            $filename = (defined('TEMP_DIR')? TEMP_DIR: '').
                $this->__configs['dbname'].'.sql';
        }
        $this->query(file_get_contents($filename));
    }
}
