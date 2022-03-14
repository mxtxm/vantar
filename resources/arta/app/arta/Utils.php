<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2010::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7003
 * Created  2010/11/16
 * Updated  2013/02/16
 */

//namespace arta;

require_once 'interface/IUtils.php';

/**
 * Contains general uncategorized static methods mostly used by other Artaengine
 * classes.
 *
 * @copyright  ::COPYRIGHT2010::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.3
 * @since      1.0.0
 * @link       http://artaengine.com/api/Utils
 * @example
 */
class Utils implements IUtils {
    /**
     * Pluralize an English word.
     * used methods from http://blog.eval.ca/2007/03/03/php-pluralize-method/
     * and http://kuwamoto.org
     *
     * @param string $word Word to be pluralized
     * @return string Pluralized word
     */
    static public function pluralize($word) {
        $wordL = strtolower($word);
        /* singular and plural are the same */
        if (in_array($wordL, array('sheep', 'fish', 'series', 'species', 'money', 'rice', 'information', 'equipment',))) {
            return $word;
        }
        /* irregular singular forms */
        foreach (array(
                'person' => 'people',
                'move'   => 'moves',
                'foot'   => 'feet',
                'goose'  => 'geese',
                'sex'    => 'sexes',
                'child'  => 'children',
                'man'    => 'men',
                'tooth'  => 'teeth',
            ) as $noun => $plural) {
            if ($wordL === $noun) {
                return $plural;
            }
        }
        /* regular expressions */
        foreach (array(
            '/(quiz)$/i'                     => '$1zes',
            '/^(ox)$/i'                      => '$1en',
            '/([m|l])ouse$/i'                => '$1ice',
            '/(matr|vert|ind)ix|ex$/i'       => '$1ices',
            '/(x|ch|ss|sh)$/i'               => '$1es',
            '/([^aeiouy]|qu)y$/i'            => '$1ies',
            '/(hive)$/i'                     => '$1s',
            '/(?:([^f])fe|([lr])f)$/i'       => '$1$2ves',
            '/(shea|lea|loa|thie)f$/i'       => '$1ves',
            '/sis$/i'                        => 'ses',
            '/([ti])um$/i'                   => '$1a',
            '/(tomat|potat|ech|her|vet)o$/i' => '$1oes',
            '/(bu)s$/i'                      => '$1ses',
            '/(alias)$/i'                    => '$1es',
            '/(octop)us$/i'                  => '$1i',
            '/(ax|test)is$/i'                => '$1es',
            '/(us)$/i'                       => '$1es',
            '/s$/i'                          => 's',
            '/$/'                            => 's',
            ) as $pattern => $rep) {
            if (preg_match($pattern, $word)) {
                return preg_replace($pattern, $rep, $word);
            }
        }

        return $word;
    }

    /**
     * Return a string containing n space characters.
     *
     * @param int $n Number of space characters
     * @return string String with n space characters
     */
    static public function space($n) {
        return str_repeat(' ', $n);
    }

    /**
     * Is an array a list or a dictionary, list means keys start from 0 and are
     * numeric and sequential.
     *
     * @since Artaengine 1.1.0
     * @param array $array Array to be checked
     * @return bool true=is list
     */
    static public function isList($array) {
        return array_keys($array) === array_keys(array_keys($array));
    }

    /**
     * Flatten an array into a string for display purposes.
     *
     * @param array $array        Array to render
     * @param array $configs=null Settings [@Utils.arraytostring]
     * @param bool  $first=true   For internal use
     * @return string The flattened array
     */
    static public function array2string($array, $configs=null, $first=true) {
        $php        = isset($configs['php'])?    $configs['php']:    true;
        $model      = isset($configs['model'])?  $configs['model']:  false;
        $nokey      = isset($configs['nokey'])?  $configs['nokey']:  false;
        $i          = isset($configs['i'])?      $configs['i']:      1;
        $constants  = isset($configs['const'])?  $configs['const']:  array();
        $constantsK = isset($configs['constk'])? $configs['constk']: array();
        $isList     = self::isList($array);

        if ($model && $first) {
            $constantsK['type'] = 'type';
            foreach ($array as $k => &$refV) {
                if ($k === 'type') {
                    $refV = self::typ2str($refV);
                }
            }
        }

        if ($php) {
            $str   = "array(\n";
            $op    = ' => ';
            $close = ')';
        } else {
            $str   = ($isList? '[': '{')."\n";
            $op    = ' : ';
            $close = $isList? ']': '}';
        }

        $space1 = str_repeat(' ', $i*4);
        $space2 = $space1.str_repeat(' ', 4);

        foreach ($array as $k => $v) {
            if (is_array($v)) {
                $configs['i'] = $i+1;
                $v = self::array2string($v, $configs, false);
            } elseif ($v === null){
                $v = "null,\n";
            } elseif (is_bool($v)) {
                $v = $v? "true,\n": "false,\n";
            } elseif (!is_numeric($v) && !in_array($v, $constants, true) && !in_array($k, $constantsK, true)) {
                $v = "'".str_replace("'", "\'", $v)."',\n";
            } else {
                $v = "$v,\n";
            }

            $str .= $space2.($isList||($nokey&&is_numeric($k))? '': (is_numeric($k)? "$k$op": "'$k'$op")).$v;
        }

        return $str.$space1.$close.($first? '': ",\n");
    }

    /**
     * Convert "T::TYPE_CONST" (Artaengine data type) string to const. This is used for editing PHP model files.
     *
     * @param string $type=null "T::TYPE_CONST" (Artaengine data type) string
     * @return const "T::TYPE_CONST" (Artaengine data type)
     */
    static public function str2typ($type=null) {
        return self::typ2str($type, false);
    }

    /**
     * Convert "T::TYPE_CONST" (Artaengine data type) const to string. This is used for editing PHP model files.
     *
     * @param string $type=null "T::TYPE_CONST" (Artaengine data type) string
     * @return string "T::TYPE_CONST" (Artaengine data type) as string
     */
    static public function typ2str($type=null, $direction=true) {
        $types = array(
            STRING      => 'STRING',
            TEXT        => 'TEXT',
            WYSIWYG     => 'WYSIWYG',
            CHAR        => 'CHAR',
            EMAIL       => 'EMAIL',
            URL         => 'URL',
            IP          => 'IP',
            PASSWORD    => 'STRING',
            BLOB        => 'BLOB',
            /* NUMBERS */
            INT         => 'INT',
            INTEGER     => 'INTEGER',
            SERIAL      => 'SERIAL',
            LONG        => 'LONG',
            SERIAL_LONG => 'SERIAL_LONG',
            SMALL       => 'SMALL',
            DOUBLE      => 'DOUBLE',
            FLOAT       => 'FLOAT',
            NUMERIC     => 'NUMERIC',
            /* BOOL */
            BOOL        => 'BOOL',
            BOOLEAN     => 'BOOLEAN',
            /* DICTIONARY */
            DICT        => 'DICT',
            DICTIONARY  => 'DICTIONARY',
            /* DATETIME */
            TIMESTAMP   => 'TIMESTAMP',
            TIMESTAMPZ  => 'TIMESTAMPZ',
            DATE        => 'DATE',
            TIME        => 'TIME',
            TIMEZ       => 'TIMEZ',
            DATETIME    => 'DATETIME',
            TIMEDATE    => 'TIMEDATE',
        );

        if ($type === null) {
            return $types;
        }

        if (!$direction) {
            $types = array_combine(array_values($types), array_keys($types));
        }

        return isset($types[$type])? $types[$type]: $type;
    }

    /**
     * If a model data property has options, this function will replace the option
     * key which is set to the property with the option's value. This is to make
     * all property values of a model human readable (but not actionable).
     *
     * @param array|string &$data Reference to model property data to render
     * @param array        $def   Model property definition
     */
    static public function mapModelPropertyOptions(&$data, $def) {
        if (!isset($def['options'])) {
            return;
        }

        if (!is_array($options=$def['options'])) {
            $options = self::getCallStaticMethod($options);
        }
        if (isset($def['multi']) && $def['multi'] && !is_array($data)) {
            $data = explode(',', $data);
        }
        if (!is_array($data)) {
            if (isset($options[$data])) {
                $data = _(is_array($options[$data]) ? $options[$data][0] : $options[$data]);
            }
            return;
        }

        foreach ($data as &$val) {
            if (isset($options[$val])) {
                $val = _(is_array($options[$val])? $options[$val][0]: $options[$val]);
            }
        }
    }

    /**
     * Traverse/crawl a path of objects in the model and returns the last node.
     *
     * @since Artaengine 1.2.2 
     * @param IModel $model Model object to start the traverse from
     * @param string $path  Path showing model objects, last node may be a method
     * @return IModel Last model object found
     */
    static public function getModelPath($model, $path) {
        $obj  = $model;
        $obj2 = null;
        foreach (explode('.', $path) as $pm) {
            if (method_exists($obj, $pm)) {
                return $pm;
            }
            $obj = $obj->$pm;
        }
        return $obj;
    }

    /**
     * Traverse/crawl a path of objects in the model and return the data found on the last node.
     *
     * @param IModel $model Model object to start the traverse from
     * @param string $path  Path showing model objects, last node may be a method last node may be a method
     * @param bool   $render=true true=apply "Render::list2Html()" on result
     * @param array  &$def=array  Will be set to last Model object definition
     * @return mixed Whatever is found on the last node
     */
    static public function getModelPathData($model, $path, $render=true, &$def=array()) {
        $obj = $model;
        $obj2 = null;
        $method = null;

        foreach (explode('.', $path) as $method) {
            $obj2 = $obj;
            $obj  = method_exists($obj, $method)? $obj->$method(): $obj->$method;
        }

        $obj3 = method_exists($obj2, $method)? $obj2->$method(): $obj2->$method;

        if (is_object($obj3)) {
            return $render? Render::list2Html($obj->present(), 'present', false): $obj->present();
        } elseif (is_array($obj3)) {
            $vals = array();
            foreach ($obj3 as $obj) {
                if (is_object($obj)) {
                    $vals[] = $render? Render::list2Html($obj->present(), 'present', false): $obj->present();
                }
            }
            return $render? Render::list2Html($vals, 'object', false): $vals;
        }

        if (property_exists($obj2, $method)) {
            $def = $obj2->propertyDefs($method);
        }

        return $obj3;
    }

    /**
     * gettext an array.
     *
     * @param array &$aReference to the array
     * @return array Translated
     */
    static public function translateArray(&$a) {
        if (is_array($a)) {
            foreach ($a as &$v) {
                $v = _($v);
            }
        }
        return $a;
    }

    /**
     * Get class static method or static property value by string.
     *
     * @since Artaengine 1.2.2
     * @param string $source "Class::method" or "Class::property"
     * @return mixed The returned value of "Class::method()" or "Class::property"
     */
    static public function getCallStaticMethod($source) {
        list($c, $m) = explode(
            strpos($source, '::') === false? '.': '::',
            $source
        );

        if (method_exists($c, $m)) {
            $source = $c::$m();
        } else {
            /* PHP5.2 */
            //$ref    = new ReflectionClass($c);
            //$source = $ref->getStaticPropertyValue($m);
            /* PHP5.3 */
            $source = $c::$$m;
        }

        return $source;
    }

    /**
     * Get browser dictionary (front-end resource dictionary).
     *
     * @since Artaengine 1.2.2
     * @return array [{js dic}, {css dic}, {template dic}]
     */
    static public function getBrowserDictionary() {
        static $dic;

        if ($dic) {
            return $dic;
        }

        include ARTA_TEMP_DIR.'browser-dictionary.php';
        $dic = array($J, $S, $T);
        return $dic;
    }

    /**
     * Updates base array with source array. The new elements of source will be
     * added to base, the non array elements of base will be updated with the
     * elements of source if they exists in source and have the same key. Elements
     * with the same key that are array in base but not array in source will be
     * updated with the source value. The elements with the same key that are array
     * in both base and source will be updated recursively in the same way.
     *
     * @param array $base   reference to base array which will be updated
     * @param array $source source array
     */
    static public function arrayUpdate(&$base, $source) {
        foreach ($source as $k => $v) {
            if (is_array($v) && isset($base[$k]) && is_array(($base[$k]))) {
                self::arrayUpdate($base[$k], $v);
            } else {
                $base[$k] = $v;
            }
        }
    }

    /**
     * Remove directory recursively.
     *
     * @param string $dir directory to remove
     */
    public static function rmdir($dir) {
        if (is_dir($dir)) {
            $objects = scandir($dir);
            foreach ($objects as $object) {
                if ($object !== '.' && $object !== '..') {
                    if (filetype($path=$dir.'/'.$object) == 'dir') {
                        self::rmdir($path);
                    } else {
                        unlink($path);
                    }
                }
            }
            reset($objects);
            @rmdir($dir);
        } 
    }

    /**
     * Turn a camel cased string into an underscore case.
     *
     * @param string $str string to change case
     * @since Artaengine 2.1
     * @return string underscore case
     */
    public static function camelCaseToUnderscoreCase($str) {
        return ltrim(strtolower(preg_replace('/[A-Z]/', '_$0', $str)), '_');
    }

    static public function getSourceArray($source) {
        if (is_array($source)) {
            return $source;
        }

        if (is_object($source)) {
            return get_object_vars($source);
        }

        if (is_string($source)) {
            $source = strtolower($source);
        }

        if ($source === 'get' || $source === INPUT_GET) {
            return $_GET;
        } elseif ($source === 'post' || $source === INPUT_POST) {
            return $_POST;
        } elseif ($source === 'request' || $source === INPUT_REQUEST) {
            return $_REQUEST;
        }

        return $_POST;
    }

    public function __toString() {
        return '[arta\Utils instance]';
    }

    static public function slugify($text) {
        // replace non letter or digits by -
        $text = preg_replace('~[^\pL\d]+~u', '-', $text);
        // transliterate
        $text = iconv('utf-8', 'us-ascii//TRANSLIT', $text);
        // remove unwanted characters
        $text = preg_replace('~[^-\w]+~', '', $text);
        // trim
        $text = trim($text, '-');
        // remove duplicate -
        $text = preg_replace('~-+~', '-', $text);
        // lowercase
        $text = strtolower($text);
        if (empty($text)) {
            return 'n-a';
        }
        return $text;
    }

    static public function getFormToken() {
        $val  = substr(md5(uniqid(rand(), true)), 0, rand(15, 32));
        $key1 = substr(md5(session_id().$val), 0, rand(22, 31));

        $_SESSION['Arta.Form'][$key1] = $val;

        return '<input class="temp-data" type="hidden" id="' . $key1 . '" name="' . $key1 . '" value="' . $val . '"/>';
    }
}
