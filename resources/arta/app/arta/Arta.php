<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 *
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7000
 * Created  2009/01/27
 * Updated  2013/02/05
 */

//namespace arta;

require_once 'interface/IArta.php';

/**
 * Dump variable(s) without expanding objects.
 * @param  [mixed] variables One or more variables to be dumped
 * @return string HTML variable(s) dump
 */
function I() {
    $args = func_get_args();
    require_once 'Inspect.php';
    Inspect::dump($args);
}

/**
 * Dump variable(s) and expand objects
 * @param  [mixed] variables One or more variables to be dumped
 * @return string  HTML variable(s) dump
 */
function inspect() {
    $args = func_get_args();
    require_once 'Inspect.php';
    Inspect::dump($args, true);
}

/**
 * Creates a frame for an Artaengine based application. Starts the application,
 * loads basic stuff and holds configs. Dispatches URL to factory and sets error
 * handlers. Most of the properties of this class hold the configs which are used
 * by other Artaengine classes, thought they are left public to be reachable and
 * tweak-able when it's needed.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    ::VERSION::
 * @link       http://artaengine.com/api/Arta
 */
class Arta implements IArta {

    /** Artaengine version */
    const VERSION = '1.9';

    /** I(); and inspect(); output format. True = HTML, False = text. */
    static public $htmlDebug = true;
    /** To be used for storing global values instead of $_GLOBALS. */
    static public $globals   = array();
    /** List of autoload paths which will be used by the autoload handler. */
    static public $autoload  = array();
    /** Path to a PHP file containing a custom autoload function. */
    static public $customAutoload;
    /**
     * Name of the application princName of application principal class (class which checks permissions).
     * @var string defaults to the 'Skinner', Arta\Skinner class
     */
    static public $principal;
    /**
     * App configs, from app configs ini files or array.
     * @var array {section: {config,},}
     */
    static public $configs;
    /**
     * Database configurations.
     * @var array {object-name: {config,},}
     */
    static public $dbConfigs;
    /**
     * Factory and template configurations.
     * @var array {object-name: {config,},}
     */
    static public $tplConfigs;
    /**
     * Cache configurations.
     * @var array {object-name: {config,},}
     */
    static public $cacheConfigs;
    /**
     * Log configurations.
     * @var array {object-name: {config,},}
     */
    static public $logConfigs;
    /** Reference to current factory object. */
    static public $factory;
    /** Factory permissions. Used by the permission Principal. */
    static public $permissionsFactory;
    /** Class permissions. Used by the permission Principal. */
    static public $permissionsClass;
    /** Exception handler function(method). */
    static public $exceptionHandler;
    /** Special event handler access. */
    static public $unauthorizedHandler;
    /** Special event handler access. */
    static public $illegalHandler;
    /** Special event handler access. */
    static public $nofactoryHandler;
    /** Special event handler access. */
    static public $notajaxHandler;
    /** Special event handler access. */
    static public $notfoundHandler;
    /** The default date format. Set in app cofigs under [arta] to key 'date-format'. */
    static public $dateFormat = 'Y/m/d';

    public function __toString() {
        return '[arta\Arta instance: Artaengine frame]';
    }

    /**
     * Fixes/sticks a path(s) and replaces "ARTA_DIR", "APP_DIR", "ARTA_TEMP_DIR"
     * and "TEMP_DIR" inside the path with their real directory values.
     * @since Artaengine 1.1.0
     *
     * @param array|string|[string] $paths An array of paths, one paths or parts of a
     *        path to be sticked together, be fixed and constants be replaced with real paths.
     * @return string Fixed path
     */
    static public function makePath($paths) {
        if (!is_array($paths)) {
            $paths = func_get_args();
        }

        $c = count($paths);
        $i = 1;
        foreach ($paths as $k => &$v) {
            if (!$v) {
                if ($i === $c) {
                    continue;
                } else {
                    unset($paths[$k]);
                }
            }
            $v = strtr($v, array(
                'ARTA_TEMP_DIR' => ARTA_TEMP_DIR,
                'ARTA_DIR'      => ARTA_DIR,
                'TEMP_DIR'      => TEMP_DIR,
                'APP_DIR'       => APP_DIR,
                '\\'            => '/',
            ));

            $f = $i === 1  && substr($v, 0, 1) === '/'? '/': '';
            $l = $i === $c && substr($v, -1)   === '/'? '/': '';

            if (strpos($v, '/') !== false) {
                $v = self::makePath(explode('/', $v));
            }
            if ($i < $c) {
                $v = rtrim($v, '/');
            } elseif ($i > 1) {
                $v = ltrim($v, '/');
            }

            $v = "$f$v$l";
            ++$i;
        }

        return implode('/', $paths);
    }

    /**
     * Get current time. The timezone and format are defined in app config under
     * section 'arta' with keys 'timezone' and 'date_format', if not set the default
     * values will be 'UTC' and 'Y/m/d H:i:s'.
     *
     * @see Localization To get dates based on user's selected locale
     * @since Artaengine 1.1.0
     * @param string $format=null Date/time format, null='date_format H:i:s' or 'Y/m/d H:i:s'
     * @param string $zone=null   Time zone, null=default application time zone or 'UTC'
     * @param int $timestamp=null To manually pass timestamp, null=usecurrent timestamp
     * @return string Current date-time
     */
    static public function now($format=null, $zone=null, $timestamp=null) {
        if (!$zone) {
            $zone = isset(self::$configs['arta']['timezone'])? self::$configs['arta']['timezone']: 'UTC';
        }
        date_default_timezone_set($zone);
        if (!$timestamp) {
            $timestamp = time();
        }
        return date($format? $format: self::$dateFormat.' H:i:s', $timestamp);
    }

    /**
     * Start an Artaengine application. An application can also be started by calling the
     * static method "Arta::Start(configs)" without the need to create an Arta object.
     *
     * @param string|array Sconfigs=null App configs ini file-path or array
     */
    public function __construct($configs=null) {
        if ($configs) {
            if (!is_array($configs)) {
                $configs = parse_ini_file($configs, true);
            }
            self::$configs = $configs;
        }
    }

    /**
     * Start an Artaengine application.
     *
     * @param string|array $configs=null App configs ini file-path or array
     * @throws UrlFactoryMapNotExists
     * @throws ResourceNotFound
     */
    static public function start($configs=null) {
        $ts = microtime(true);
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
        /* errors & exceptions handler */
        set_exception_handler(array('Arta', 'exceptionHandler'));
        set_error_handler(array('Arta', 'errorHandler'), E_ALL | E_STRICT);
        /* create request object */
        self::startAutoloader();

        require_once 'Request.php';
        self::$globals['request'] = $request = new Request(isset($configs['arta']['path_key'])? $configs['arta']['path_key']: null);
        if (isset($configs['arta']['request_filter'])) {
            $request->setFilter($configs['arta']['request_filter']);
        }
        /* load factory  maps */
        if (file_exists(ARTA_TEMP_DIR . 'factory-config.php')) {
            require ARTA_TEMP_DIR . 'factory-config.php';
        } elseif ($request->path(0) === 'build') {
            $routes = $resources = array();
        } else {
            require 'exception/UrlFactoryMapNotExists.php';
            throw new UrlFactoryMapNotExists();
        }

        $uPath = $request->path();

        $principal = isset($artaS['principal'])? $artaS['principal']: 'Skinner';
        require_once $principal . '.php';
        self::$principal = $principal;

        /* resources */
        if (isset($resources) && isset($resources[$uPath[0]])) {
            $uPath[0] = $resources[$uPath[0]];
            if (!file_exists($file=implode('/', $uPath))) {
                require 'exception/ResourceNotFound.php';
                throw new ResourceNotFound($file);
            }
            $finfo    = finfo_open(FILEINFO_MIME_TYPE);
            $fileType = finfo_file($finfo, $file);
            finfo_close($finfo);
            header("Content-type: $fileType");
            die(file_get_contents($file));
        }
        /* ROUTER > URL to factory dispatcher */
        if (isset($artaS['slash']) && $artaS['slash'] && count($uPath) > 1 && !end($uPath)) {
            array_pop($uPath);
        }
        if (isset($routes[$count=count($uPath)])) {
            foreach ($routes[$count] as $route) {
                $facArgs = array(null);
                $no = false;
                /* lookup URL against routes */
                foreach ($route[0] as $i => $path) {
                    if ($path === 2) {
                        $facArgs[] = $uPath[$i];
                    } elseif (($type=$path[0]) == 0 || $type == 1) {
                        if ($path != $type.$uPath[$i]) {
                            $no = true; break;
                        }
                    } elseif ($type == 3) {
                        if (preg_match(substr($path, 1), $uPath[$i], $out)) {
                            $facArgs[]=$out[1];
                        } else {
                            $no = true;
                            break;
                        }
                    } elseif ($type == 4) {
                        if (!isset($path[$uPath[$i]])) {
                            $no = true;
                            break;
                        }
                    } else {
                        $no = true;
                        break;
                    }
                }

                if ($no) {
                    continue;
                }

                /* process factory */
                list(, $tplObjectName, $facPath, $facClass, $facMethod, $dontLoad, $urlPermissions, $ajax) = $route;

                if (is_int($facClass)) {
                    $facPath  = str_replace('*', $facArgs[$facClass], $facPath);
                    $facClass = $facArgs[$t=$facClass];
                    unset($facArgs[$t]);
                }
                if (is_int($facMethod)) {
                    $facMethod = $facArgs[$t=$facMethod];
                    unset($facArgs[$t]);
                }

                if (isset($route[8]) && $route[8] == 1) {
                    if (isset($_GET[$facClass]) && isset($_GET[$facMethod])) {
                        $facClass  = $_GET[$facClass];
                        $facMethod = $_GET[$facMethod];
                        $facPath  .= "/$facClass.php";
                    } else {
                        unset($facMethod);
                    }
                }
                break;
            }
        }

        require_once 'T.php';
        date_default_timezone_set(isset($artaS['timezone'])? $artaS['timezone']: 'UTC');
        /* Use output buffer */
        if (!isset($configs['arta']['buffer']) || $configs['arta']['buffer']) {
            ob_start();
        }
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
                    if (file_exists($modelDicPath=ARTA_TEMP_DIR.strtolower($object)."-include.php")) {
                        require $modelDicPath;
                    }
                    if (isset($params['pluralize']) && !$params['pluralize']) {
                        Model::$__pluralize__ = false;
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

        /* Session */
        if (isset($configs['session']['startup']) && !isset($dontLoad['session']) && $configs['session']['startup']) {
            require_once 'request/Session.php';
            Session::start();
        }
        /* i18n lang is key as en, locale is as en_EN */
        if (isset($configs['i18n']) && $configs['i18n'] && !isset($dontLoad['i18n'])) {
            require_once 'Localization.php';
            Localization::start($configs['i18n']);
        }
        /* not found */
        if (!isset($facMethod)) {
            if ($uPath[0] === 'build' && isset($artaS['build']) && $artaS['build']) {
                self::build($configs);
                return;
            }
            require_once 'RaiseHandler.php';
            RaiseHandler::raise('N');
        }
        /* check ajax */
        if ($ajax && (!isset($_SERVER['HTTP_X_REQUESTED_WITH']) || strtolower($_SERVER['HTTP_X_REQUESTED_WITH']) !== 'xmlhttprequest')) {
            require_once 'RaiseHandler.php';
            RaiseHandler::raise('A', $facClass, $facMethod);
        }
        /* check URL permissions */
        if ($urlPermissions) {
            switch ($principal::url($urlPermissions)) {
                case IPrincipal::ILLEGAL:
                    require_once 'RaiseHandler.php';
                    RaiseHandler::raise('I');
                    break;
                case IPrincipal::UNAUTHORIZED:
                    require_once 'RaiseHandler.php';
                    RaiseHandler::raise('U');
            }
        }
        /* check factory permissions */
        switch ($principal::factory($facClass, $facMethod)) {
            case IPrincipal::OK:
                break;
            case IPrincipal::ILLEGAL:
                require_once 'RaiseHandler.php';
                RaiseHandler::raise('I', $facClass, $facMethod);
                break;
            case IPrincipal::UNAUTHORIZED:
                require_once 'RaiseHandler.php';
                RaiseHandler::raise('U', $facClass, $facMethod);
        }
        /* FACTORY */
        unset($routes, $configs);
        if (file_exists($facPath)) {
            require_once $facPath;
        }
        $facArgs[0] = $request;
        if ($facClass) {
            /* FACTORY > CLASS */
            if (class_exists($facClass)) {
                self::$factory = $factory = new $facClass();
                if (method_exists($factory, $facMethod)) {
                    if (property_exists($factory, '__tpl')) {
                        $factory->__tpl = $tplObjectName;
                    }
                    call_user_func_array(array(&$factory, $facMethod), $facArgs);
                } else {
                    require_once 'RaiseHandler.php';
                    RaiseHandler::raise('F', $facClass, $facMethod, $facPath);
                }
            }
            else {
                require_once 'RaiseHandler.php';
                RaiseHandler::raise('F', $facClass, $facMethod, $facPath);
            }
        } else {
            /* FACTORY > FUNCTION */
            unset($facArgs[0]);
            call_user_func_array($facMethod, array_unshift($facArgs, $request, $facArgs));
        }

        if (isset($artaS['log'])) {
            Log::request($artaS['log'], array('start' => $ts, 'Factory' => $facClass.'.'.$facMethod));
        }
    }

    /**
     * Get value of a global variable defined in Arta::$globals
     *
     * @param string $key         Variable name
     * @param mixed $default=null If key did not exist return this value
     * @return mixed Value of the global key or default
     */
    static public function g($key, $default=null) {
        return isset(self::$globals[$key])? self::$globals[$key]: $default;
    }

    /**
     * Builds the application. /build
     *
     * @param string|array $configs App configs ini file-path or array
     */
    static public function build($configs) {
        if (self::isCli()) {
            self::$htmlDebug = false;
        }
        require 'Buildout.php';
        $buildout = new Buildout($configs);
    }

    /**
     * Default error handler - converts error to exception and calls the exception handler.
     *
     * @param int    $code        Error code
     * @param string $description Error description
     * @param string $file        File which raised the error
     * @param int    $line        Line where the error happened
     * @throws ErrorException Throws the error as exception
     * @see http://artaengine.com/tutorials/url-mapping#handle
     */
    static public function errorHandler($code, $description, $file, $line) {
        self::exceptionHandler(new ErrorException($description, $code, 0, $file, $line));
    }

    /**
     * Default exception handler. If an exception handler factory is not defined
     * this method will catch all exceptions, if debug mode or PHP error reporting
     * is off then swallows the exception otherwise shows a message.
     *
     * @param Exception $exception ErrorException Object to be handled
     * @see http://artaengine.com/tutorials/url-mapping#handle
     */
    static public function exceptionHandler($exception) {
        if (!error_reporting() || !DEBUG) {
            return;
        }

        if (self::$exceptionHandler && $exception->getFile() !== __FILE__) {
            require_once 'RaiseHandler.php';
            RaiseHandler::raise('X', $exception);
        }

        require_once 'Inspect.php';
        Inspect::dumpException($exception);
    }

    /**
     * Start Artaengine's autoloader
     */
    static protected function startAutoloader() {
        self::$autoload[] = ARTA_DIR;
        self::$autoload[] = ARTA_DIR . 'exception';
        self::$autoload[] = ARTA_DIR . 'interface';
        self::$autoload[] = ARTA_DIR . 'request';
        self::$autoload[] = ARTA_DIR . 'log';

        function autoLoad($className) {
            $className = ltrim(str_replace('\\', '/', $className), '/');
            $principal = Arta::$principal;
            /* check class permissions */
            if ($principal) {
                switch ($principal::cls($className)) {
                    case IPrincipal::OK:
                        break;
                    case IPrincipal::ILLEGAL:
                        require_once 'RaiseHandler.php';
                        RaiseHandler::raise('I', $className);
                        break;
                    case IPrincipal::UNAUTHORIZED:
                        require_once 'RaiseHandler.php';
                        RaiseHandler::raise('U', $className);
                }
            }

            foreach (Arta::$autoload as $path) {
                if (file_exists($filename=rtrim($path, '\/')."/$className.php")) {
                    require_once $filename;
                    return;
                }
            }

            if (Arta::$customAutoload && file_exists(Arta::$customAutoload)) {
                require_once Arta::$customAutoload;
                customAutoload($className);
            }
            // Use this line for debug only, it can cause autoloaders of other
            // libraries to fail.
            // require 'exception/ResourceNotFound.php';
            // throw new ClassNotExists($classNAme);
        }
        spl_autoload_register('autoLoad');
    }

    /**
     * Execute a shell command and return the result.
     *
     * @param string $command the command to run
     * @param string &$returnVal=null command return value
     * @return string result from command execution
     */
    public static function exec($command, &$returnVal=null) {
        ob_start();
        system($command, $returnVal);
        return ob_get_clean();
    }

    /**
     * Check if the application is running from the command line.
     *
     * @return bool true=the application is running from the command line
     */
    static public function isCli() {
        return php_sapi_name() === 'cli';
    }
}
