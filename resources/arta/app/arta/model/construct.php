<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * Created  2012/07/08
 * Updated  2012/07/08
 */
 
// Model's not optimized constructor + optimizer
// uses the original model classes (not optimized)


/* > > > THE NOT OPTIMIZED CONSTRUCTOR */

$thisClassName = get_class($this);
$thisClassNameLower = strtolower($thisClassName);

if (!isset($this->__table__)) {
    $this->__table__ = Model::$__pluralize__ ?
        Utils::pluralize($thisClassNameLower) :
        Utils::camelCaseToUnderscoreCase($thisClassName);
}

$table = $this->__table__;

/* deflate data-property definitions */
$m1 = $m2 = $m3 = $m4 = $m2i = array();

foreach (get_object_vars($this) as $col => $def) {
    if (substr_count($col, '__') < 2) {
        if (is_array($def)) {
            if ($def && !isset($def[0])) {
                /* this.property = {def} > scalar data */
                if (isset($def['i18n']) && $def['i18n']) {
                    $m4[$col] = $def; /* i18n */
                } else {
                    $m1[$col] = $def;
                    if (isset($def['key']) && $def['key']) {
                        $this->__key__ = $col;
                        $this->__fk__  = $this->__table__.'_'.$col;
                    }
                }
                if (isset($def['present']) && $def['present']) {
                    $this->__present__[] = $col;
                }
            } else {
                /* this.Class = {}, this.property = {Class} - multi object */
                $class = $def? $def[0]: $col;
                if ($class === $tm) {
                    continue;
                }

                $that = new $class(array('tm' => $thisClassName));
                $tableThat = $that->__table__;
                $postFix = $def? '_'.strtolower($col): '';

                if (isset($this->{$defCol='__'.$col.'__'})) {
                    $m3[$col] = $this->$defCol;
                }

                $m3[$col]['class'] = $class;
                $m3[$col]['table'] = ($table < $tableThat? $table."_$tableThat":
                    $tableThat."_$table").$postFix;
                $m3[$col]['junction'] = class_exists($c=$class.'_'.$thisClassName)? $c:
                    (class_exists($c=$thisClassName.'_'.$class)? $c: null);
                $m3[$col]['view']  = "v_$table"."_$tableThat".$postFix.'_j';
                $m3[$col]['obj']   = array(
                    'table' => $tableThat,
                    'pk'    => $that->__key__,
                    'fk'    => $that->__fk__,
                );
                $m3[$col]['i18n'] = $that->__def__[3]? true: false;
            }
        } else {
            /* this.Class = null, this.property = 'Class' - object */
            if (isset($this->{$defCol='__'.$col.'__'})) {
                $m2[$col] = $this->$defCol;
            }

            $m2[$col]['class'] = $c = ($def? $def: $col);
            isset($m2i[$c])? $m2[$col]['i'] = ++$m2i[$c]: $m2i[$c]=1;

            if ($def && isset($this::$__mdic__[$group][$def])) {
                $this::$__mdic__[$group][$col] = $this::$__mdic__[$group][$def];
            }
        }
    }
}

/* present */
if (!$this->__present__) {
    $present = isset($m1['name']) || isset($m4['name'])? 'name':
        (isset($m1['description']) || isset($m4['description'])? 'description':
        (isset($m1['title']) || isset($m4['title'])? 'title':
        ($this->__key__? $this->__key__: null)));

    if ($present) {
        $this->__present__[] = $present;
    } else {
        $this->__present__ = array();
    }
}
/* postfix m2 */
foreach ($m2 as $col => &$def) {
    if (!isset($def['i'])) {
        $def['i'] = $m2i[$m2[$col]['class']] > 1? 1: null;
    }
}

$this->__def__ = array($m1, $m2, $m3, $m4);
$this->reset(null);

/* < < < THE NOT OPTIMISED CONSTRUCTOR */


/* THE MODEL OPTIMISED CACHE CREATOR > > > */

if ($this::$__optimize__ === 2) {
    $cachePath = ARTA_TEMP_DIR.$group;
    foreach (Arta::$autoload as $path) {
        if (file_exists($p="$path/$thisClassName.php")) {
            $thisPath = $p;
        }
    }

    if (!isset($thisPath)) {
        die('Autoload is messed up!');
    }

    /* cache directories */
    if (!file_exists($cachePath)) {
        mkdir($cachePath);
    }

    /* dont do it if model is not modified since last optimize */
    $thisModified = filemtime($thisPath);
    $make = true;
    if (file_exists("$cachePath/__last-cached.php")) {
        include "$cachePath/__last-cached.php";
        if (isset($modelsLastModify[$thisClassName]) && $modelsLastModify[$thisClassName] === $thisModified) {
            $make = false;
        }
    }
    //$make=1;
    // TODO: if class is inherited from an abstract class, changing the abstract class
    //       must trigger this
    $thisPathOptimized = $cachePath.'/'.$thisClassName.'.php';
    if ($thisPathOptimized !== $thisPath) {
        if ($make || !file_exists($thisPathOptimized)) {
            $modelsLastModify[$thisClassName] = $thisModified;
            /* update last modify */
            file_put_contents(
                "$cachePath/__last-cached.php",
                "<?php /* Model cache - last model changed\n".
                "    by: Arta.ModelCache\n    at: ".Arta::now('Y/m/d H:i')." */\n\n".
                '$modelsLastModify = '.
                Utils::array2string($modelsLastModify, array('i' => 0)).';'
            );

            $base        = file_get_contents($thisPath);
            $pregReplace = array();
            $pregRemove  = array();
            $pAdd        = '';

            /* minimized model */
            foreach ($m1+$m4 as $col => $defX) {
                if (preg_match($p='%(public\s*\$'.$col.'\s*=\s*array\s*\()(.*?)(\)\s*;)%s', $base)) {
                    $pregReplace[0][] = $p;
                    $pregReplace[1][] = 'public $'.$col.' = null;';
                } else {
                    $pregRemove[] = $p;
                    $pAdd .= "\n".'    public $'.$col.' = null;';
                }
            }

            foreach ($m2 as $col => $defX) {
                if (preg_match($p='%(public\s*\$'.$col.'\s*=)(.*?)(;)%s', $base)) {
                    $pregReplace[0][] = '%(public\s*\$__'.$col.'__\s*=\s*array\s*\()(.*?)(\)\s*;)%s';
                    $pregReplace[1][] = '';
                    $pregReplace[0][] = '%(public\s*\$'.$col.'\s*=)(.*?)(;)%s';
                    $pregReplace[1][] = 'public $'.$col.' = null;';
                } else {
                    $pregRemove[] = '%(public\s*\$__'.$col.'__\s*=\s*array\s*\()(.*?)(\)\s*;)%s';
                    $pregRemove[] = '%(public\s*\$'.$col.'\s*=)(.*?)(;)%s';
                    $pAdd .= "\n".'    public $'.$col.' = null;';
                }
            }

            foreach ($m3 as $col => $defX) {
                if (preg_match($p='%(public\s*\$'.$col.'\s*=\s*array\s*\()(.*?)(\)\s*;)%s', $base)) {
                    $pregReplace[0][] = $p;
                    $pregReplace[1][] = 'public $'.$col.' = null;';
                    $pregReplace[0][] = '%(public\s*\$__'.$col.'__\s*=\s*array\s*\()(.*?)(\)\s*;)%s';
                    $pregReplace[1][] = '';
                } else {
                    $pregRemove[] = $p;
                    $pregRemove[] = '%(public\s*\$__'.$col.'__\s*=\s*array\s*\()(.*?)(\)\s*;)%s';
                    $pAdd .= "\n".'    public $'.$col.' = null;';
                }
            }
            /* optimized constructor */
            $pregReplace[0][] = '%public\s*function\s*__construct\s*\(%';
            $pregReplace[1][] = $pAdd.
            "\n".'    public $__table__ = \''.$this->__table__."';\n".
            '    public $__key__ = \''.$this->__key__."';\n".
            '    public $__fk__ = \''.$this->__fk__."';\n".
            '    public $__def__ = '.Utils::array2string($this->__def__).";\n".
            '    public $__present__ = '.Utils::array2string($this->__present__).";\n".
            "    public function __construct(";

            /* * */
            file_put_contents(
                $thisPathOptimized,
                preg_replace($pregReplace[0], $pregReplace[1], $base)
            );

            /* if inherits Model indirect */
            if (!function_exists('untillModel')) {
                function untillModel($pregRemove, $class, $cachePath) {
                    if (($class=get_parent_class($class)) === 'Model') {
                        return;
                    }

                    foreach (Arta::$autoload as $path) {
                        if (file_exists($p="$path/$class.php")) {
                            file_put_contents(
                                $cachePath.'/'.$class.'.php',
                                preg_replace($pregRemove, '', file_get_contents($p))
                            );
                            return untillModel($pregRemove, get_parent_class($class), $cachePath);
                        }
                    }
                }
            }

            untillModel($pregRemove, $thisClassName, $cachePath);
        }
    }
}

/* < < < THE MODEL OPTIMISED CACHE CREATOR */
