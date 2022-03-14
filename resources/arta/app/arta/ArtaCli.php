<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2013::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7609
 * Created  2013/12/01
 * Updated  2015/03/28
 */

//namespace arta;

require_once 'interface/ICli.php';
require_once 'Arta.php';
Arta::$htmlDebug = false;

/**
 * A frame for command line applications. May be used to create command line tools
 * for an Artaengine based web application. When you create an CLI application your
 * main class can inherit this class and use the provided methods. This class can
 * be used to access an Artaengine based web application's resources and can use
 * most Artaengine libraries.
 *
 * @copyright  ::COPYRIGHT2013::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.1
 * @since      1.4.0
 * @link       http://artaengine.com/api/Cli
 * @example    http://artaengine.com/examples/cli
 * @throws UrlFactoryMapNotExists
 */
class ArtaCli extends Arta implements ICli {

    /**
     * The string used for indents
     * @var string
     */
    public  $indentStr        = "\t";

    private $__indentWidth    = 0;
    private $__cursorLocation = null;
    private $__supportsColor;

    public function __toString() {
        return '[arta\ArtaCli instance: Artaengine CLI frame]';
    }

    public function __construct() {

    }

    /**
     * @overload
     * Get a command param value by index.
     * @param int $index The index of the param value to be returned
     * @return string Value of the param index or null if param not exists
     *
     * @overload
     * Get a command param value by param name.
     * @param string $index return the value of the param e.g:  "myapp.php -f foo"
     *        passinf "-f" will return "foo"
     * @return string Value of the param or null if param not found
     *
     * @overload
     * Get all the command params.
     * @return array An array of all params
     */
    static public function getParam($index=null) {
        if ($index === null) {
            return isset($_SERVER['argv'])? $_SERVER['argv']: array();
        }
        if (!isset($_SERVER['argv'])) {
            return null;
        }
        if (is_int($index)) {
            return isset($_SERVER['argv'][$index])? $_SERVER['argv'][$index]: null;
        }
        foreach ($_SERVER['argv'] as $i => $v) {
            if ($v === $index) {
                return isset($_SERVER['argv'][$i+1])? $_SERVER['argv'][$i+1]: null;
            }
        }
    }

    /**
     * Check if a CLI param value exists inside the proviced params.
     *
     * @param string $param Param value
     * @return bool If param exists
     */
    static public function hasParam($param) {
        return in_array($param, $_SERVER['argv']);
    }

    /**
     * Print a new line.
     *
     * @param int $i Number of new lines
     * @return ArtaCli For method chaining
     */
    public function nl($i=1) {
        for ($j = 0; $j < $i; $j++) {
            print "\n";
        }
        return $this;
    }

    /**
     * Write text.
     *
     * @param string|array $data        Data to be written
     * @param string|array $color=null  "fore-color,back-color" or [fore-color, back-color]
     * @param string|array $cursor=null "column,line" or [column, line]
     * @return ArtaCli For method chaining
     */
    public function write($data, $color=null, $cursor=null) {
        static $dataLen;

        if ($cursor) {
            if (!is_array($cursor)) {
                $cursor = explode(',', $cursor);
            }
            print $this->__moveCursor(trim($cursor[0]), isset($cursor[1])? trim($cursor[1]): null);

        } elseif ($this->__cursorLocation) {
            print $this->__cursorLocation;
            if ($dataLen) {
                for ($i=0; $i<$dataLen; $i++) {
                    print ' ';
                }
            }
            print $this->__cursorLocation;
        }

        if ($color) {
            if (!is_array($color)) {
                $color = explode(',', $color);
            }
            $this->setColor(trim($color[0]), isset($color[1])? trim($color[1]): null);
        }

        if ($this->__indentWidth) {
            for ($i=0; $i<$this->__indentWidth; $i++) {
                print $this->indentStr;
            }
        }

        $data = is_array($data)? implode('', $data): $data;
        print $data;
        $dataLen = strlen($data);

        return $this;
    }

    /**
     * Write text and goto new line.
     *
     * @param string|array $data        Data to be written. Array items will be separated by new lines
     * @param string|array $color=null  "fore-color,back-color" or [fore-color, back-color]
     * @param string|array $cursor=null "column,line" or [column, line]
     * @return ArtaCli For method chaining
     */
    public function writeLn($data, $color=null, $cursor=null) {
        $this->write(is_array($data)? implode("\n", $data): $data, $color, $cursor);
        print "\n";
        return $this;
    }

    /**
     * Add one or more indents.
     *
     * @param int    $indent=1 Number of indents to be added
     * @param string $str=tab  Indent string
     * @return ArtaCli For method chaining
     */
    public function indent($indent=1, $str="\t") {
        $this->__indentWidth += $indent;
        $this->indentStr      = $str;
        return $this;
    }

    /**
     * Remove one or more indents.
     *
     * @param int $indent=1 Number of indents to be removed
     * @return ArtaCli For method chaining
     */
    public function removeIndent($indent=1) {
        $this->__indentWidth -= $indent;
        if ($this->__indentWidth < 0) {
            $this->__indentWidth = 0;
        }
        return $this;
    }

    /**
     * Set fore and back color. To reset to default fore and back colors call this
     * method without params.
     *
     * @param string $fore=null Color name
     * @param string $back=null Color name
     * @return ArtaCli For method chaining
     */
    public function setColor($fore=null, $back=null) {
        if ($this->__supportsColor) {
            if ($fore === null && $back === null) {
                print "\e[0m";
            } else {
                if ($fore !== null) {
                    $this->setForeColor($fore);
                }
                if ($back !== null) {
                    $this->setBackColor($back);
                }
            }
        }
        return $this;
    }

    /**
     * Set text color and style.
     *
     * @param string $color  Color name
     * @return ArtaCli For method chaining
     */
    public function setForeColor($color) {
        if ($this->__supportsColor) {
            print "\e[" . $this->getColorByName($color) . 'm';
        }
        return $this;
    }

    /**
     * Set text back-ground color.
     *
     * @param string $color Color name
     * @return ArtaCli For method chaining
     */
    public function setBackColor($color) {
        if ($this->__supportsColor) {
            print "\e[" . $this->getColorByName($color, true) . 'm';
        }
        return $this;
    }

    /**
     * Get colors by name
     */
    private function getColorByName($colorName, $bg=false) {
        if ($bg) {
            $map = array(
                'black'  => '40',
                'red'    => '41',
                'green'  => '42',
                'yellow' => '43',
                'blue'   => '44',
                'purple' => '45',
                'cyan'   => '46',
                'white'  => '47',
            );
        } else {
            $map = array(
                'underline-black'   => '4;30',
                'black'             => '0;30',
                'gray'              => '1;30',
                'light-gray'        => '0;37',
                'white'             => '1;37',
                'underline-white'   => '4;37',
                'blue'              => '0;34',
                'light-blue'        => '1;34',
                'underline-blue'    => '4;34',
                'green'             => '0;32',
                'light-green'       => '1;32',
                'underline-green'   => '4;31',
                'red'               => '0;31',
                'light-red'         => '1;31',
                'underline-red'     => '4;31',
                'purple'            => '0;35',
                'light-purple'      => '1;35',
                'underline-purple'  => '4;35',
                'cyan'              => '0;36',
                'light-cyan'        => '1;36',
                'underline-cyan'    => '4;36',
                'yellow'            => '0;33',
                'brown'             => '1;33',
                'underline-yellow'  => '4;33',
            );
        }
        return isset($map[$colorName])? $map[$colorName]: $colorName;
    }

    /**
     * Move cursor.
     *
     * @param int $column=0  move cursor to this column, null=do nothing
     * @param int $line=null move cursor to this line, null=do nothing
     * @return ArtaCli For method chaining
     */
    public function moveCursor($column=0, $line=null) {
        print $this->__moveCursor($column, $line);
        return $this;
    }

    /**
     * Return ANSI move cursor string
     */
    private function __moveCursor($column=0, $line=null) {
        $cursorLocation = '';
        if ($column !== null) {
            $cursorLocation .= "\e[" . $column . 'G';
        }
        if ($line) {
            $cursorLocation .= "\e[" . $line . 'A';
        }
        return $cursorLocation;
    }

    /**
     * Fix cursor at a point. After the cursor is fixed all next writes will start
     * from this point.
     *
     * @param int $column=0  Move cursor to this column number, null=do nothing
     * @param int $line=null Move cursor to this line number, null=do nothing
     * @return ArtaCli For method chaining
     */
    public function fixCursor($column=0, $line=null) {
        if ($column !== null) {
            $this->__cursorLocation = "\e[" . $column . 'G';
        }
        if ($line) {
            print "\e[" . $line . 'A';
        }
        return $this;
    }

    /**
     * Release the fixed cursor.
     *
     * @return object this
     */
    public function releaseCursor() {
        $this->__cursorLocation = null;
        return $this;
    }

    /**
     *
     * Simulate the Artaengine framework for a CLI application.
     * Note: This is experimental, Not all Artaengine libs and functions are not
     * tested to work in CLI.
     *
     */

    /**
     * Start an Artaengine application. Simulate the Artaengine framework for a CLI application.<br/>
     * Note: This is experimental, Not all Artaengine libs and functions are not
     * tested to work in CLI.
     *
     * @param string|array $configs=null App setting file-path or array
     * @return void
     */
    static public function simulate($configs) {
        $GLOBALS['__SESSION'] = array();
        $GLOBALS['__COOKIE'] = array();

        if ($configs) {
            if (!is_array($configs)) {
                $configs = parse_ini_file($configs, true);
            }
            self::$configs = $configs;
        } else {
            $configs = &self::$configs;
        }

        $artaS = $configs['arta'];
        define('ARTA_DIR', str_replace('\\', '/', dirname(__FILE__)).'/');
        define('APP_DIR',  str_replace('\\', '/', getcwd()).'/');
        define('TEMP_DIR', str_replace(array('ARTA_DIR/', 'APP_DIR/'), array(ARTA_DIR, APP_DIR), $artaS['temp_path']).'/');
        define('ARTA_TEMP_DIR', TEMP_DIR.'arta'.'/');
        define('DEBUG', isset($artaS['debug'])? $artaS['debug']: false);

        if (isset($artaS['date_format'])) {
            self::$dateFormat = $artaS['date_format'];
        }
        date_default_timezone_set(isset($artaS['timezone'])? $artaS['timezone']: 'UTC');

        require_once 'T.php';

        self::$autoload[] = ARTA_DIR;
        self::$autoload[] = ARTA_DIR.'exception';

        /* load objects and configs */
        $pathMap = array('ARTA_DIR/' => ARTA_DIR, 'APP_DIR/' => APP_DIR);
        foreach ($configs as $section => $params) {
            $section = explode(':', $section);
            $object  = isset($section[1])? strtolower($section[1]): null;
            $section = $section[0];
            if (!isset($dontLoad[$section])) {
                if ($section === 'database') {
                    self::$dbConfigs[$object] = $params;
                } elseif ($section === 'factory' || $section === 'template') {
                    self::$tplConfigs[$object] = $params;
                } elseif ($section === 'application') {
                    self::$globals = self::$globals+$params;
                } elseif ($section === 'model') {
                    if (file_exists($modelDPath=ARTA_TEMP_DIR."$object-include.php")) {
                        require $modelDPath;
                    }
                } elseif ($section === 'cache') {
                    self::$cacheConfigs[$object] = $params;
                } elseif ($section === 'log') {
                    self::$logConfigs[$object] = $params;
                } elseif ($section === 'autoload') {
                    if (isset($params['custom'])) {
                        self::$customAutoload = strtr($params['custom'], $pathMap);
                        unset($params['custom']);
                    }
                    foreach ($params as $path) {
                        self::$autoload[] = strtr($path, $pathMap);
                    }
                } elseif ($section === 'constants') {
                    foreach ($params as $name => $val) {
                        if (!defined($name)) {
                            define($name, $val);
                        }
                    }
                } elseif ($section === 'globals') {
                    foreach ($params as $name => $val) {
                        $GLOBALS[$name] = $val;
                    }
                } elseif ($section === 'includes') {
                    foreach ($params as $path) {
                        require strtr($path, $pathMap);
                    }
                }
            }
        }

        self::startAutoloader();
    }
}
