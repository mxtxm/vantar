<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7001
 * Created  2009/01/27
 * Updated  2013/03/05
 *
 * params: ?cache=false:    delete all cache files except Models dictionary
 *         ?models=clear    delete all models cache files except Models dictionary
 *         ?models=clearall delete all models cache files
 *         ?models=ignore   ignore model build
 */

//namespace arta;

/**
 * This class is used by the Artaengine frame to build applications, unless you
 * want to tweak or hack something reading this API is a waste of time.
 * Builds an Artaengine application. Synchronizes database with
 * models(if used), creates cache and meta files, fetches translation
 * keys out of models, phptal or smarty templates. This class is used
 * internally by the framework, normally programmers do not need to
 * tamper with this class. Note that this class does not implement an 
 * interface, the API for this class is subject to change.
 *
 * You can build an application by:
 * 1- Calling Arta::build();
 * 2- Creating an ArtaCli app and calling $this->build('configure.ini');
 * 3- Browser, add /build//// to the application URL. Zero or more trailing slashes
 *    can be added to this path to avoid conflicts with application paths. Build
 *    path can be enabled or disabled in app configs.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.1.6
 * @since      1.0.0
 * @link       http://artaengine.com/api/Buildout
 * @see        http://artaengine.com/tutorials/getting-started
 */
class Buildout {

    /** Builder version */
    const VERSION = '1.2';

    /**
     * File-paths whch to be created by the builder {key: file-path,}
     * @var array
     */
    public $file = array(
        'phptal_i18n' => null, // PHP TAL translation keys
        'smarty_i18n' => null, // Smarty translation keys
        'browser'     => null, // Browser dictionary
        'factory'     => null, // URL map to factory
        'build'       => null, // Build info
    );

    /**
     * [[message-text, message-type,],] list of buildout messages. message-type:
     * 0  header text
     * 1  function - start text
     * 2  function - time
     * 3  end text
     * 4  end text - time
     * 5  warning
     * 6  fatal error
     * 7  action description
     * 8  action description indented
     * 9  action description indented
     * 10 end
     * @var array
     */
    public $msgs = array();

    /** Database object name to be used for Authorization */
    public $authorizeDbo = 'null';

    private $__manual;               // manual buildout
    private $__build;                // app build number
    private $__gt;                   // gettext exists
    private $__keys       = array(); // used by template extract
    private $__lastModify = null;    // used by template extract
    private $__compress   = true;    // compress resources override
    private $__specials   = array(   // {key: engine-name,}
        'notfound'     => 'notfound',
        'lost'         => 'notfound',
        'notajax'      => 'notajax',
        'exception'    => 'exception',
        'unauthorized' => 'unauthorized',
        'illegal'      => 'illegal',
        'nofactory'    => 'nofactory',
    );

    public function __toString() {
        return '[arta\Buildout instance: Artaengine application builder]';
    }

    /**
     * Fixes/sticks a path(s) and replaces "ARTA_DIR", "APP_DIR", "ARTA_TEMP_DIR"
     * and "TEMP_DIR" inside the path with their real directory values.
     *
     * @since Artaengine 1.1.0
     * @param array|strings|[string] An array of paths, one paths or parts of a
     *        path to be sticked together, be fixed and constants be replaced with real paths.
     * @return string Fixed path
     */
    static public function p($p) {
        if (!is_array($p)) {
            $p = func_get_args();
        }
        $c = count($p);
        $i = 1;
        foreach ($p as $k => &$v) {
            if (!$v) {
                if ($i == $c) {
                    continue;
                } else {
                    unset($p[$k]);
                }
            }
            $v = str_replace(
                array('\\', 'ARTA_DIR', 'APP_DIR', 'ARTA_TEMP_DIR', 'TEMP_DIR'),
                array('/',  S_ARTA_DIR, S_APP_DIR, S_TEMP_DIR,      S_TEMP_DIR),
                $v
            );
            $f = $i==1  && substr($v, 0, 1) === '/'? '/': '';
            $l = $i==$c && substr($v, -1) ===' /'? '/': '';
            if (strpos($v, '/') !== false) {
                $v = Buildout::p(explode('/', $v));
            }
            if ($i < $c) {
                $v = rtrim($v, '/');
            } elseif ($i > 1) {
                $v = ltrim($v, '/');
            }
            $v = "$f$v$l";
            ++$i;
        }
        return implode('/', $p);
    }

    /**
     * Buildout constructor.
     *
     * @param string|array $configs=null  App configs ini file or array
     * @param array        $constants=null Artaengine constant names and key and their values regarding the application which will be built
     * @param bool         $manual=false   false=constructor will build everything, true=building will be done manually
     */
    public function __construct($configs=null, $constants=null, $manual=false) {
        $this->__manual = $manual;
        /* Errors & Exceptions handler */
        set_exception_handler(array('Buildout', 'exceptionHandler'));
        set_error_handler(array('Buildout', 'errorHandler'), E_ALL | E_STRICT);
        $t1 = microtime(true);
        if (!($this->__gt=function_exists('_'))) {
            function _($string) { return $string; }
        }
        ini_set('max_execution_time', 3600);
        spl_autoload_unregister('autoLoad');
        /* validate required parameters */
        if (!defined('ARTA_DIR')) {
            define('ARTA_DIR', str_replace(array('\\', 'libs'), array('/', ''), dirname(__FILE__)).'/');
        }
        if (!defined('APP_DIR')) {
            define('APP_DIR',  str_replace('\\', '/', getcwd()).'/');
        }
        if ($configs) {
            if (!is_array($configs)) {
                $configs = parse_ini_file($configs, true);
            }
            require_once ARTA_DIR . 'Arta.php';
            Arta::$configs = $configs;
        } else {
            $configs = Arta::$configs;
        }
        // say building is in proccess
        Arta::$configs['__b'] = true;
        if (!defined('TEMP_DIR')) {
            define('TEMP_DIR', str_replace(array('ARTA_DIR/', 'APP_DIR/'), array(ARTA_DIR, APP_DIR), $configs['arta']['temp_path']).'/');
        }
        if (!defined('ARTA_TEMP_DIR')) {
            define('ARTA_TEMP_DIR', TEMP_DIR.'arta'.'/');
        }
        if (!defined('DEBUG')) {
            define('DEBUG', true);
        }
        define('BUILD', true);

        if (!$constants) {
            if (!defined('BASE_URL')) {
                if (php_sapi_name() !== 'cli') {
                    require_once ARTA_DIR.'Request.php';
                    $request = new Request();
                } else {
                    define('BASE_URL', '');
                }
            }
            $constants = array(
                'ARTA_DIR'      => ARTA_DIR,
                'APP_DIR'       => APP_DIR,
                'TEMP_DIR'      => TEMP_DIR,
                'ARTA_TEMP_DIR' => ARTA_TEMP_DIR,
                'BASE_URL'      => BASE_URL,
            );
        }
        /* Directories no use of path constants in this file - to work in Arta config page */
        define('S_TEMP_DIR'     , $constants['TEMP_DIR']);
        define('S_ARTA_TEMP_DIR', $constants['ARTA_TEMP_DIR']);
        define('S_ARTA_DIR'     , $constants['ARTA_DIR']);
        define('S_APP_DIR'      , $constants['APP_DIR']);
        /* Create required directories */
        if (!file_exists(S_TEMP_DIR)) {
            mkdir(S_TEMP_DIR);
        }
        if (!file_exists(S_ARTA_TEMP_DIR)) {
            mkdir(S_ARTA_TEMP_DIR);
        }
        if (!file_exists(S_ARTA_TEMP_DIR.'synch-logs')) {
            mkdir(S_ARTA_TEMP_DIR.'synch-logs');
        }
        /* Files */
        $this->file['phptal_i18n'] = S_TEMP_DIR.'phptal-i18n.php';
        $this->file['smarty_i18n'] = S_TEMP_DIR.'smarty-i18n.php';
        $this->file['factory']     = S_ARTA_TEMP_DIR.'factory-config.php';
        $this->file['browser']     = S_ARTA_TEMP_DIR.'browser-dictionary.php';
        $this->file['build']       = S_TEMP_DIR.'build.log';
        /* app debug mode */
        define('APP_DEBUG_MODE', isset($configs['arta']['debug'])? ($configs['arta']['debug']? true: false): true);
        /* build number */
        $fh = fopen($this->file['build'], 'a');
        fwrite($fh, Arta::now()."\n");
        fclose($fh);
        $this->__build = count(explode("\n", file_get_contents($this->file['build'])));
        /* BASE_URL */
        $this->BASE_URL = rtrim(isset($configs['base_url'])? $configs['base_url']: $constants['BASE_URL'], '/').'/';
        /* delete all files first */
        $clear = false;
        if (isset($_GET['cache']) && $_GET['cache'] === 'false') {
            if (file_exists($this->file['phptal_i18n'])) {
                unlink($this->file['phptal_i18n']);
            }
            if (file_exists($this->file['smarty_i18n'])) {
                unlink($this->file['smarty_i18n']);
            }
            if (file_exists($this->file['factory'])) {
                unlink($this->file['factory']);
            }
            if (file_exists($this->file['browser'])) {
                unlink($this->file['browser']);
            }
            $clear = true;
        }
        /* vars */
        global $buildoutAutoload;
        $buildoutAutoload[] = S_ARTA_DIR;
        $databases      = array();
        $models         = array();
        $sessionDbo     = null;
        $factoryConfigs = array(); // tplObject => [configfile, factorydir]
        $browserDics    = array();
        /* configure.ini */
        if (isset($configs['arta']['compress']) && !$configs['arta']['compress']) {
            $this->__compress = false;
        }
        foreach ($configs as $section => $params) {
            $section = explode(':', $section);
            $object  = isset($section[1])? strtolower($section[1]): null;
            switch ($section[0]) {

                case 'database':
                    if ($params) {
                        require_once 'Database.php';
                        Database::remove($object);
                        $databases[$object] = $params;
                    }
                    break;

                case 'model':
                    if ($params) {
                        $object = strtolower($object);
                        if (isset($params['path'])) {
                            $buildoutAutoload[] = $params['real_path'] = $modelPath =
                                Buildout::p($params['path']);
                            foreach (new RecursiveIteratorIterator(new RecursiveDirectoryIterator($modelPath), 2) as $path) {
                                if ($path->isDir() && $path && substr($path, -1) !== '.') {
                                    $buildoutAutoload[] = (string)$path;
                                }
                            }
                        }
                        $models[$object] = $params; 
                    }
                    break;

                case 'session':
                    if (isset($params['database'])) {
                        $sessionDbo = $params['database'];
                    }
                    break;

                case 'factory':
                case 'template':
                    // i18n template keys
                    if ($tplDirectory=isset($params['templates_path'])? Buildout::p($params['templates_path']): null) {
                        $engine = isset($params['engine'])? $params['engine']: null;
                        if ($engine === 'phptal') {
                            $phpTal = $tplDirectory;
                            if (!file_exists(S_TEMP_DIR.'phptal')) {
                                mkdir(S_TEMP_DIR.'phptal');
                            }
                        } elseif ($engine === 'smarty') {
                            $smarty = $tplDirectory;
                            if (!file_exists(S_TEMP_DIR.'smarty')) {
                                mkdir(S_TEMP_DIR.'smarty');
                            }
                            if (!file_exists(S_TEMP_DIR.'smarty/compile')) {
                                mkdir(S_TEMP_DIR.'smarty/compile');
                            }
                            if (!file_exists(S_TEMP_DIR.'smarty/configs')) {
                                mkdir(S_TEMP_DIR.'smarty/configs');
                            }
                            if (!file_exists(S_TEMP_DIR.'smarty/cache')) {
                                mkdir(S_TEMP_DIR.'smarty/cache');
                            }
                        }
                    }
                    // browser dictionary
                    $browserDics[] = isset($params['dictionary'])? Buildout::p($params['dictionary']): null;
                    // factory config
                    $factoryConfigs[] = array(
                        isset($params['factory_config'])? Buildout::p($params['factory_config']):
                            (isset($params['views_config'])? Buildout::p($params['views_config']): null),
                        isset($params['factory_path'])? Buildout::p($params['factory_path']):
                            (isset($params['views_path'])? Buildout::p($params['views_path']): null),
                        $object
                    );
            }
        }
        if (isset($configs['application']['dictionary'])) {
            $browserDics[] = Buildout::p($configs['application']['dictionary']);
        }
        /* Autoload */
        $buildoutAutoload[] = S_ARTA_DIR.'interface';
        $buildoutAutoload[] = S_ARTA_DIR.'exception';
        function buildoutAutoLoad($className) {
            global $buildoutAutoload;
            foreach ($buildoutAutoload as $v) {
                if (file_exists($filename=Buildout::p($v, "$className.php"))) {
                    require_once $filename;
                    return;
                }
            }
        }
        spl_autoload_register('buildoutAutoLoad');
        /* Manual buildout */
        if ($manual) {
            $this->buildMethodArgs = array(
                'browserDictionary' => $browserDics,
                'dataModels'        => array(
                    'databases' => $databases,
                    'models'    => $models,
                    'clear'     => $clear,
                    'sessionDbo'=> $sessionDbo,
                ),
                'factory'           => $factoryConfigs,
                'extractSmartyi18n' => isset($smarty)? $smarty: null,
                'extractTALi18n'    => isset($phpTal)? $phpTal: null,
            );
            return;
        }
        /* BUILD EVERYTHING */
        $this->addMessage();
        $this->addMessage('Preparing buildup', 1);
        $this->addMessage(round(microtime(true)-$t1, 4).'s', 2);
        /* Models and database */
        $this->dataModels($databases, $models, $clear, $sessionDbo);

        /* Database run sql */
        $this->database($databases);

        /* * */
        $this->addMessage("\nBuildup finished !", 3);
        $this->addMessage(round(microtime(true)-$t1, 4).'s', 4);
        $this->addMessage('-', 10);

        die;
    }

    /**
     * Runs SQLs if set to do so.
     *
     * @param array $databases=null  {database-object-name: {database-connection-configs},}
     * @return void
     */
    public function database($databases) {
        $this->addMessage('Checking if there is user SQL files for execution...', 1);
        foreach ($databases as $db => $params) {
            if (isset($params['data'])) {
                $params['advanced'] = true;
                $dbo = Database::getCreateNew($db, $params);

                $parts = explode(' ', trim($params['data']));
                $delim = isset($parts[1])? trim($parts[1]): null;

                $dir = rtrim(self::p($parts[0]), '/');

                if (!file_exists($dir)) {
                    $this->addMessage('Config > Database > ' . $db . ' > data > ' . $dir . ' not exists.', 9);
                    return;
                }

                $dirExecuted = $dir . '/executed/';
                $dir         = $dir . '/*';


                if (!file_exists($dirExecuted)) {
                    mkdir($dirExecuted);
                }

                foreach (glob($dir) as $file) {
                    if (!is_dir($file)) {
                        $i = 0;
                        $sqls = file_get_contents($file);
                        foreach ($delim? explode($delim, $sqls): array($sqls) as $sql) {
                            $i++; 
                            $dbo->query($sql);
                        }

                        $this->addMessage($file, 7);
                        $this->addMessage('Executed ' . $i, 8);
                        if (!@rename($file, $dirExecuted . basename($file))) {
                            $this->addMessage('Could not be moved to ' . basename($file) . ' requires write permissions to the dir.', 9);
                        }
                    }
                }
            }
        }
    }

    /**
     * Builds cache files needed for data modeling, synchronizes models with database.
     *
     * @param array  $databases=null  {database-object-name: {database-connection-configs},}
     * @param array  $models=null     {model-group-name: {model-configs},}
     * @param bool   $clear=false     Clear cache files before doing anything
     * @param string $sessionDbo=null Name of database object used by session, if session repo is database
     */
    public function dataModels($databases=null, $models=null, $clear=false, $sessionDbo=null) {
        Model::$__optimize__ = 2;
        if (!$databases && isset($this->buildMethodArgs['dataModels'])) {
            extract($this->buildMethodArgs['dataModels']);
        }
        $t2 = microtime(true);
        $this->addMessage('Synchronizing databases with models', 1);
        require_once Buildout::p(S_ARTA_DIR, 'T.php');
        require_once Buildout::p(S_ARTA_DIR, 'Database.php');
        require_once Buildout::p(S_ARTA_DIR, 'Models.php');
        /* clean cache params */
        $clearAll = $ignore = false;
        if (isset($_GET['models']) && $_GET['models'] === 'clearall') {
            $clearAll = $clear = true;
        }
        if (isset($_GET['models']) && $_GET['models'] === 'clear') {
            $clear = true;
        }
        if (isset($_GET['models']) && $_GET['models'] === 'ignore') {
            $ignore = true;
        }
        /* synch */
        foreach ($models as $group => $params) {
            $params['path']          = Buildout::p($params['path']);
            Model::$modelDbo[$group] = $params['database'];
            // if build is set to off in config.ini
            if (isset($params['build']) && !$params['build']) {
                continue;
            }
            Model::$__mdic__ = null;
            $databases[$params['database']]['advanced'] = true;
            $dbo = Database::getCreateNew($params['database'], $databases[$params['database']]);
            // db session
            if ($sessionDbo === $params['database']) {
                $this->__dbSession($dbo, $sessionDbo);
            }
            // models
            $synch = new Models($params['path'], $group, $dbo, true, S_TEMP_DIR);
            if (!$ignore) {
                if ($clear) {
                    $synch->clearCache($clearAll);
                }
                if (isset($params['dropsql']) && $params['dropsql']) {
                    $synch->createDropSQLFile();
                }
                $statistics = $synch->synch();
                /* interaction */
                $this->addMessage(sprintf('Model group \'%s\' synchronized using database object \'%s\'', $group, $params['database']), 7);

                if (isset($statistics['view-drop-junction'])) {
                    $this->addMessage(sprintf('%s junction table views dropped.', $statistics['view-drop-junction']), 8);
                }
                if (isset($statistics['view-drop'])) {
                    $this->addMessage(sprintf('%s views dropped.', $statistics['view-drop']), 8);
                }
                if (isset($statistics['table-create-alter'])) {
                    $this->addMessage(sprintf('%s tables created or updated.', $statistics['table-create-alter']), 8);
                }
                if (isset($statistics['views-create'])) {
                    $this->addMessage(sprintf('%s views created.', $statistics['views-create']), 8);
                }
                if (isset($statistics['views-create-junction'])) {
                    $this->addMessage(sprintf('%s junction table views created.', $statistics['views-create-junction']), 8);
                }

                if (isset($statistics['log-ok'])) {
                    $this->addMessage(sprintf('all executed queries are stored in log file: %s', $statistics['log-ok']), 8);
                } elseif (isset($statistics['log-ko'])) {
                    $this->addMessage(sprintf('log file: %s was not created, probably due lack of write permissions.', $statistics['log-ko']), 9);
                }

                if (isset($statistics['index-ok'])) {
                    $this->addMessage(sprintf('suggested index are stored in file: %s', $statistics['index-ok']), 8);
                } elseif (isset($statistics['index-ko'])) {
                    $this->addMessage(sprintf('suggested index file: %s was not created, probably due lack of write permissions.', $statistics['index-ko']), 9);
                }

                if (isset($statistics['translation-ok'])) {
                    $this->addMessage(sprintf(
                        'translation keys extacted from models are stored in file: %s '.
                        'make this file visible to poedit', $statistics['translation-ok']), 8);
                } elseif (isset($statistics['translation-ko'])) {
                    $this->addMessage(sprintf(
                        'keys extacted from models could not be stored in file: %s'.
                        ' probably due lack of write permissions.',
                        $statistics['translation-ko']), 9);
                }
            }
            // database used for users and permissions
            $models = $synch->getModelFiles();
            if (isset($models['User']) && isset($models['UserPermission']) && isset($models['UserGroup'])) {
                $this->authorizeDbo = "'$params[database]'";
                $this->addMessage(sprintf(
                    'User permission checkings will be done using database object \'%s\'',
                    $params['database']), 7);
            }
        }

        $this->addMessage(round(microtime(true)-$t2, 4).'s', 2);
    }

    /**
     * Create session table (if db session)
     */
    private function __dbSession($dbo, $sessionDboName) {
        require_once 'request/session/handler/SessionDb.php';
        SessionDb::createTable($dbo);
        $this->addMessage(sprintf('Session data will be stored via database object \'%s\' on table \'sessions\'', $sessionDboName), 7);
    }

    /**
     * Builds cache files required for URL/Factory mapping and permissions.
     *
     * @param array $factoryConfigs=null array (factory_path: factory-directory, factory_config: URL-mapping-file-path)
     */
    public function factory($factoryConfigs=null) {
        if (!$factoryConfigs) {
            $factoryConfigs = $this->buildMethodArgs['factory'];
        }
        $t2 = microtime(true);
        $this->addMessage('Creating URL mapping lookup table', 1);
        $factories = '';
        $specials  = array();

        foreach ($factoryConfigs as $config) {
            list($factories1, $specials1, $cperms) = $this->__factory($config);
            $factories .= $factories1;
            $specials  += $specials1;
        }
        /* repair string */
        if (count($factoryConfigs) > 1) {
            $parts = explode("\n    ),\n", $factories);
            array_pop($parts);
            $newParts = array();
            foreach ($parts as $part) {
                $part  = explode("\n", $part, 2);
                $index = $part[0];
                $newParts[$index] = isset($newParts[$index])? $newParts[$index]."\n".$part[1]: $part[1];
            }
            $factories = '';
            foreach ($newParts as $index => $part) {
                $factories = $index."\n".$part."\n    ),\n";
            }
        }
        /* Specials */
        if (!isset($specials['illegal'])) {
            $this->addMessage('No handler defined to handle illegal users', 5);
        }
        if (!isset($specials['unauthorized'])) {
            $this->addMessage('No handler defined to handle unauthorized users', 5);
        }
        if (!isset($specials['exception'])) {
            $this->addMessage('No handler defined to handle errors and exceptions', 5);
        }
        if (!isset($specials['nofactory'])) {
            $this->addMessage('No handler defined to hanlde undefined calls to missing factory files, classes or methods', 5);
        }
        if (!isset($specials['notfound'])) {
            $this->addMessage('No handler defined to handle requests to not defined URLs (error 404)', 5);
        }
        if (!isset($specials['notajax'])) {
            $this->addMessage('No handler defined to handle not ajax calls to paths defined as ajax only', 5);
        }
        /* Write to file */
        if (file_exists($this->file['factory'])) {
            unlink($this->file['factory']);
        }
        $gt = '';
        if (!$this->__gt) {
            $gt = 'if (!function_exists(\'_\')) { function _($string) { return $string; } }'."\n\n";
        }

        $ok = file_put_contents(
            $this->file['factory'],
            "<?php /* URL map to factory and permissions config file - created on buildout\n".
            "    by: Arta.Buildup\n    at: ".Arta::now('Y/m/d H:i:s')."\n".
            "    * Application will not work without this file */\n\n".$gt.'$routes = array('.
            "\n$factories\n);\n".implode("\n", $specials)."\n$cperms"
        );

        if ($ok === false) {
            $this->addMessage(sprintf(
                'Could not create URL map to factory and permissions config file \'%s\'. '.
                'Probably due lack of write permission.',
                $this->file['factory']), 6);
        }

        $this->addMessage(round(microtime(true)-$t2, 4).'s', 2);
    }


    private function __robots($urls) {
        if ($urls) {
            $txt = array();
            foreach ($urls as $url) {
                $txt[] = "User-agent: *\nDisallow: $url";
            }
            $file = S_APP_DIR.'robots.txt';
            $oldLines = array();
            if (file_exists($file)) {
                foreach ($lines=explode("\n", file_get_contents($file)) as $i => $line) {
                    if (!trim($line)) {
                        continue;
                    }
                    if (stripos($line, 'sitemap') !== false) {
                        $txt[] = $line;
                        continue;
                    }


                    if ((stripos($line, 'user-agent:') !== false || $i == count($lines)-1) && count($oldLines) > 1) {
                        if ($i == (count($lines)-1)) {
                            $oldLines[] = $line;
                        }
                        if (count($oldLines) == 2 && in_array(trim(str_ireplace('disallow:', '', $oldLines[1])), $urls)) {
                            $oldLines = array();
                        }
                        foreach ($oldLines as $oldLine) {
                            $txt[] = $oldLine[0] === '#'? $oldLine: "# $oldLine";
                        }
                        $oldLines = array();
                    }
                    $oldLines[] = $line;
                }
            }

            if (count($oldLines) == 2 && in_array(trim(str_ireplace('disallow:', '', $oldLines[1])), $urls)) {
                $oldLines = array();
            }

            foreach ($oldLines as $oldLine) {
                $txt[] = $oldLine[0]==='#'? $oldLine: "# $oldLine";
            }

            $ok = file_put_contents($file, implode("\n", $txt));
            $this->addMessage(sprintf('robots.txt was updated. %s URLs included. Old URLs become commented.', count($url)), 7);
        }
    }


    private function __factory($config) {
        list($filename, $factoriesPath, $tplObjectName) = $config;
        return strtolower(pathinfo($filename, PATHINFO_EXTENSION)) == 'php'?
            $this->__factoryPHP($filename, $factoriesPath, $tplObjectName):
            $this->__factoryXML($filename, $factoriesPath, $tplObjectName);
    }


    private function __factoryXML($filename, $factoriesPath, $tplObjectName) {
        $configure = array();
        $nodes = simplexml_load_file($filename);
        /* resources to array */
        foreach ($nodes->resource as $v) {
            if (   ($path     =isset($v['path'])?      trim($v['path']):      null)
                && ($directory=isset($v['directory'])? trim($v['directory']): null)) {

                $resources[] = array(
                    'path'      => $path,
                    'directory' => $directory,
                );
            }
        }

        if (isset($resources)) {
            $configure['resources'] = $resources;
        }
        /* special events to array */
        foreach ($this->__specials as $key => $event) {
            if ($factory=(string)$nodes->{$key}[0]['factory']) {
                $configure[$event] = $factory;
            }
        }
        /* class permissions */
        foreach ($nodes->permission as $cperm) {
            $permissions = trim((string)$cperm['permission']);
            if (strtolower($permissions) === 'true') {
                $permissions = true;
            }
            if (!$permissions) {
                continue;
            }
            if ($class=trim((string)$cperm['factory'])) {
                $configure['permissions'][] = array(
                    'factory'    => $class,
                    'permission' => $permissions,
                );
            } elseif ($class=trim((string)$cperm['class'])) {
                $configure['permissions'][] = array(
                    'class'      => $class,
                    'permission' => $permissions,
                );
            }
        }
        /* URL factories to array */
        foreach ($nodes->route as $route) {
            $atts = array();
            foreach ($route->attributes() as $key => $val) {
                $val = (string)$val;
                if (strtolower($val) === 'true') {
                    $val = true;
                }
                if (strtolower($val) === 'false') {
                    $val = false;
                }
                $atts[$key] = $val;
            }
            $configure[] = $atts; 
        }
        /* * */
        return $this->__factoryPHP($filename, $factoriesPath, $tplObjectName, $configure);
    }


    private function __factoryPHP($filename, $factoriesPath, $tplObjectName, $configure=null) {
        if (!$configure) {
            include $filename;
        }

        $specials = array();
        /* extract resources */
        if (isset($configure['resources'])) {
            foreach ($configure['resources'] as $v) {
                if (   ($path     =isset($v['path'])?      trim($v['path']):      null)
                    && ($directory=isset($v['directory'])? trim($v['directory']): null)) {
                    $resources[] = "    '$path' => '$directory',";
                }
            }
            if (isset($resources)) {
                $specials['resources'] = "\n".'$resources = array('."\n".implode("\n", $resources)."\n);";
            }
            unset($configure['resources']);
        }
        /* extract special events */
        foreach ($this->__specials as $key => $event) {
            if (   isset($configure[$key]) && ($factory=trim(is_array($configure[$key])
                && array_key_exists('factory', $configure[$key])? $configure[$key]['factory']: $configure[$key]))
                && ($factory=$this->__parseFactory($factory, $factoriesPath))) {

                $specials[$event] = 'Arta::$'.$event.'Handler = array('."'$factory[0]', '$tplObjectName', $factory[1], '$factory[2]');";
            }
            unset($configure[$key]);
        }
        /* db object - used for permission checkings */
        $specials['authorizeDbo'] = '$authorizeDbo = '.$this->authorizeDbo.';';
        /* class permissions */
        if (isset($configure['permissions']) && is_array($configure['permissions'])) {
            foreach ($configure['permissions'] as $cperm) {
                $factory     = isset($cperm['factory'])?    trim($cperm['factory']): null;
                $class       = isset($cperm['class'])?      trim($cperm['class']):   null;
                $permissions = isset($cperm['permission'])? $cperm['permission']:    null;

                if (!$permissions) {
                    continue;
                }
                if ($permissions === true) {
                    $perm = 'true';
                } else {
                    $perms = array('array(');
                    foreach (explode(',', $permissions) as $cpv) {
                        if ($cpv=trim($cpv)) {
                            $perms[] = "        '$cpv' => 1,";
                        }
                    }
                    $perms[] = '     )';
                    $perm    = implode("\n", $perms);
                }
                if ($class) {
                    $cpermsC[] = "    '$class' => $perm,";
                } elseif ($factory) {
                    $cpermsF[] = "    '$factory' => $perm,";
                }
            }
        }

        $perms = '';
        if (isset($cpermsF)) {
            $perms = 'Arta::$permissionsFactory = array('."\n".implode("\n", $cpermsF)."\n);\n";
        }
        if (isset($cpermsC)) {
            $perms .= 'Arta::$permissionsClass = array('."\n".implode("\n", $cpermsC)."\n);";
        }
        unset($configure['permissions']);
        /* URL to factory map > routes array */
        $routesA = array(
            'N' => array(), // 0,1
            'S' => array(), // 3
            'O' => array(), // 4
            'P' => array(), // 2
        );
        // extract URL factories
        $mappedCount = 0;
        $totalCount  = 0;
        $robots      = array();
        $sp1         = '        ';

        foreach ($configure as $v) {
            ++$totalCount;
            $paths   = isset($v['path'])?    trim($v['path']):     null;
            $factory = isset($v['factory'])? trim( $v['factory']): null;

            if (!$factory || !$paths) {
                $this->addMessage(sprintf("Unusable route: path='%s' factory='%s'", $paths, $factory), 5);
                continue;
            }
            // parse factory
            if (!($factoryA=$this->__parseFactory($factory, $factoriesPath))) {
                $this->addMessage(sprintf("Unusable route: path='%s' factory='%s'", $paths, $factory), 5);
                continue;
            }
            list($facPath, $facClass, $facMethod, $facType) = $factoryA;
            // extract path
            $paths = explode('/', $paths);
            if (isset($paths[0])) {
                unset($paths[0]);
            }
            $extraced = array($sp1.'array(', $sp1.'    array('); // extracted paths as PHP code
            $iRoutesA = 'N';
            $weight   = 0;
            $i        = 0;
            $robotUrl = array();
            // path atts
            foreach ($paths as $path) {
                ++$i;
                // 0 base URL
                if ($path === '') {
                    $string     = "'0'";
                    $robotUrl[] = '';
                }
                // 2 path=param
                elseif ($path[0] == ':') {
                    $weight    += -$i + 40;
                    $iRoutesA   = $iRoutesA == 'S'? $iRoutesA: 'P';
                    $string     = 2;
                    $robotUrl[] = '*';
                }
                // 3 path contains *
                elseif (strpos($path, '*') > -1) {
                    $weight    += -$i + 30;
                    $iRoutesA   = 'S';
                    $string     = '\'3/^'.str_replace('*', '(.*?)', $path).'$/i\'';
                    $robotUrl[] = $path;
                }
                // 4 path contains ?
                elseif (strpos($path, '?') > -1) {
                    $weight    += -$i + 10;
                    $iRoutesA   = $iRoutesA == 'N'? 'O': $iRoutesA;
                    $tmp        = array();
                    foreach (explode('?', $path) as $n) {
                        $tmp[] = "'$n' => 0";
                    }
                    $string     = 'array(4, '.implode(', ', $tmp).')';
                    $robotUrl[] = '*';
                }
                // 1 path=string
                else {
                    $string     = "'1$path'";
                    $robotUrl[] = $path;
                }
                $extraced[] = "$sp1        $string,";
            }
            // robots
            if (isset($v['robots']) && !$v['robots']) {
                $robots[] = '/'.implode('/', $robotUrl);
            }
            // factory
            $extraced[] = "$sp1    ),";
            $extraced[] = "$sp1    '$tplObjectName',";
            $extraced[] = "$sp1    '$facPath',";
            $extraced[] = "$sp1    $facClass,";
            if ((int)$facMethod > 0) {
                $extraced[] = "$sp1    $facMethod,";
            } else {
                $extraced[] = "$sp1    '$facMethod',";
            }
            // Dont load
            $objectNames = array(
                'database',
                'session',
                'i18n',
                'template',
                'model',
                'cache',
            );

            $objects = array();
            if (isset($v['ignore']) && $v['ignore']) {
                if (strtolower($v['ignore']) === 'all') {
                    foreach ($objectNames as $objectName) {
                        $objects[] = "$sp1        '$objectName' => 1,";
                    }
                } else {
                    foreach (is_array($v['ignore'])? $v['ignore']: explode(',', $v['ignore']) as $iv) {
                        if (trim($iv)) {
                            $objects[] = "$sp1        '".trim($iv)."' => 1,";
                        }
                    }
                }
            }

            foreach ($objectNames as $objectName) {
                if (isset($v[$objectName])) {
                    $vTemp = strtolower($v[$objectName]);
                    if ($v[$objectName] === false || $v[$objectName] === Null || $vTemp === 'off' || $vTemp === 'false') {
                        $objects[] = "$sp1        '$objectName' => 1,";
                    }
                }
            }

            $extraced[] = $objects? "$sp1    array(\n".implode("\n", $objects)."\n$sp1    ),": "$sp1    null,";
            // Permission
            $permissions = array();
            if (isset($v['permission']) && $v['permission']) {
                if ($v['permission'] === true) {
                    $permissions = 'true';
                } else {
                    foreach (is_array($v['permission'])? $v['permission']: explode(',', $v['permission']) as $pv) {
                        if (trim($pv)) {
                            $permissions[] = "$sp1        '$pv' => 1,";
                        }
                    }
                }
            }

            $extraced[] = $permissions && is_array($permissions)?
                "$sp1    array(\n".implode("\n", $permissions)."\n$sp1    ),":
                "$sp1    ".($permissions? 'true,': 'null,');
            // Ajax
            $extraced[] = "$sp1    ".(isset($v['ajax']) && $v['ajax']? 'true': 'false').",";
            // factory type (last - 8th index) 1=qs 0=path
            if ($facType) {
                $extraced[] = "$sp1    $facType,";
            }

            $extraced[]           = "$sp1),";
            $extraced             = array(implode("\n", $extraced), count($paths));
            $routesA[$iRoutesA][] = $iRoutesA == 'S' || $iRoutesA == 'P'? array($weight, $extraced): $extraced;
            ++$mappedCount;
        }

        $this->addMessage(sprintf('%s or %s maps where extracted from file: \'%s\'', $mappedCount, $totalCount, $filename), 7);
        /* sort routes by priority */
        sort($routesA['P']);
        foreach ($routesA['P'] as &$v) {
            $v = $v[1];
        }
        sort($routesA['S']);
        foreach ($routesA['S'] as &$v) {
            $v = $v[1];
        }
        $routesJ = array_merge($routesA['N'], $routesA['O'], $routesA['S'], $routesA['P']);
        /* group routes by number of path */
        $routesA = array();
        foreach ($routesJ as $route) {
            $routesA[array_pop($route)][] = $route[0];
        }
        $routeS = '';
        foreach ($routesA as $count => $routes) {
            $routeS .= "    $count => array(\n".implode("\n", $routes)."\n    ),\n";
        }
        /* * */
        $this->__robots($robots);
        return array($routeS, $specials, $perms);
    }


    private function __parseFactory($factory, $factoriesPath) {
        $type = 0; // 1=qs 0=normal
        if (strpos($factory, 'qs:') !== false) {
            $factory = str_replace('qs:', '', $factory);
            $type    = 1;
        }
        $f      = explode('.', $factory);
        $method = array_pop($f);
        if (strpos($method, ':') === false) {
            // class.method
            if (!($class=array_pop($f))) {
                return false;
            }
            $file = $f? array_pop($f): $class;
            if ((int)$class == 0) {
                $class = "'$class'";
            }

        } else {
            // file:function
            list($file, $method) = explode(':', $method);
            $class = 'null';
        }

        $path = $type==1?
            Buildout::p($factoriesPath.($f? '/': '').implode('/', $f)):
            Buildout::p($factoriesPath.($f? '/': '').implode('/', $f), (is_numeric($file)? '*': $file).'.php');

        return array($path, $class, $method, $type);
    }

    /**
     * Builds catche files required for browser dictionary (front-end resource dictionary).
     *
     * @param array $filenames=null [browser-dictionary-file-paths,]
     * @return void
     */
    public function browserDictionary($filenames=null) {
        if (!$filenames) {
            $filenames = $this->buildMethodArgs['browserDictionary'];
        }

        $t2 = microtime(true);
        $this->addMessage('Creating browser dictionary', 1);
        $js = $css = $tpl = array();

        foreach ($filenames as $filename) {
            if ($filename && file_exists($filename)) {
                $this->bc = array('js' => 0, 'css' => 0, 'tpl' => 0);
                $this->__useCompress = !APP_DEBUG_MODE;
                if (strtolower(pathinfo($filename, PATHINFO_EXTENSION)) == 'php') {
                    // PHP
                    include $filename;
                    if (!isset($configure)) {
                        $this->addMessage(sprintf(
                            'No $configure = array(...); defined inside \'%s\';', $filename), 5);
                        continue;
                    }
                    $configs     = isset($configure['configs'])?     $configure['configs']:     array();
                    $javascripts = isset($configure['javascripts'])? $configure['javascripts']: array();
                    $styles      = isset($configure['styles'])?      $configure['styles']:      array();
                    $templates   = isset($configure['templates'])?   $configure['templates']:   array();
                    if (isset($configs['compress']) && $configs['compress']) {
                        $this->__useCompress = true;
                    }
                    if (isset($configs['minify'])) {
                        $this->__minify = $configs['minify'];
                    }
                    $this->__browserDictionaryPHP($js, $javascripts, array('js',  'javascript'));
                    $this->__browserDictionaryPHP($css, $styles     , array('css', 'style'));
                    $this->__browserDictionaryPHP($tpl, $templates  , array('tpl', 'template'));
                    unset($templates, $styles, $javascripts, $configs);

                } else {
                    // XML
                    $nodes = simplexml_load_file($filename);

                    foreach ($nodes->configs as $x) {
                        foreach ($x as $name => $node) {
                            if ($name == 'config') {
                                if ($node['key'] == 'compress' && strtolower($node['value']) == 'true') {
                                    $this->__useCompress = true;
                                }
                                if ($node['key'] == 'minify') {
                                    $this->__minify = (string)$node['value'];
                                }
                            }
                        }
                    }

                    $this->__browserDictionaryXML($js, $nodes->javascripts, array('js',  'javascript'));
                    $this->__browserDictionaryXML($css, $nodes->styles,      array('css', 'style'));
                    $this->__browserDictionaryXML($tpl, $nodes->templates,   array('tpl', 'template'));
                }
                $this->addMessage(sprintf(
                    '%s .js keys, %s .css and %s template keys extracted from file: \'%s\'',
                    $this->bc['js'], $this->bc['css'], $this->bc['tpl'], $filename), 7);
            }
        }

        $js  = self::__implodeBrowserDictionary($js);
        $css = self::__implodeBrowserDictionary($css);
        $tpl = self::__implodeBrowserDictionary($tpl);

        /* build */
        $js  = str_replace(':build', $this->__build, $js);
        $css = str_replace(':build', $this->__build, $css);
        /* Write to file */
        if (file_exists($this->file['browser'])) {
            unlink($this->file['browser']);
        }

        $ok = file_put_contents(
            $this->file['browser'],
            "<?php /* Templates, JS and CSS file to key dictionary - created on buildout\n".
            "    by: Arta.Buildup\n    at: ".Arta::now('Y/m/d H:i:s')." */\n\n".
            '$T = array('."\n$tpl);\n".
            '$J = array('."\n$js);\n".
            '$S = array('."\n$css);"
        );

        if ($ok === false) {
            $this->addMessage(sprintf(
                'Could not create browser dictionary file \'%s\'. '.
                'Probably due lack of write permission.',
                $this->file['browser']), 6);
        }

        $this->addMessage(round(microtime(true)-$t2, 4).'s', 2);
    }

    private static function __implodeBrowserDictionary($array) {

        foreach ($array as $key => &$value) {
            if (count($value) > 1) {
                foreach ($value as $k => $v) {
                    $value[$k] = "        $v";
                }
                $value = "    $key => array(\n" . implode('', $value) . "    ),\n";
            } else {
                $value = "    $key => $value[0]";
            }
        }

        return implode('', $array);
    }

    /**
     * XML to array
     */
    private function __browserDictionaryXML(&$array, $nodes, $edge, $prefix=null, $first=true) {
        $prefix = $prefix? $prefix.'/': '';

        foreach ($nodes as $node) {
            $name = $node->getName();
            if ($name === $edge[0] || $name === $edge[1]) {
                /* compress */
                $nodePath = $this->__compressResource($node, $edge[0]);
                /* array item */
                $path = $nodePath;
                if ($path[0] == '/') {
                    $path = substr($path, 1, strlen($path) - 1);
                }

                $key = "'$prefix{$node['name']}'";
                if (!isset($array[$key])) {
                    $array[$key] = array();
                }
                $array[$key][] = "'$path',\n";

                $this->bc[$edge[0]] = $this->bc[$edge[0]]+1;
            } else {
                $this->__browserDictionaryXML($array, $node, $edge, $prefix.($first? '': $name), false);
            }
        }
    }

    /**
     * PHP to array
     */
    private function __browserDictionaryPHP(&$array, $nodes, $edge, $prefix=null) {
        $prefix = $prefix? $prefix.'/': '';

        foreach ($nodes as $name => $node) {
            if (is_int($name)) {
                /* compress */
                $nodePath = $this->__compressResource($node, $edge[0]);
                /* array item */
                $path = str_replace('BASE_URL/', $this->BASE_URL, $nodePath);
                if ($path[0] == '/') {
                    $path = substr($path, 1, strlen($path) - 1);
                }

                $key = "'$prefix{$node['name']}'";
                if (!isset($array[$key])) {
                    $array[$key] = array();
                }
                $array[$key] = "'$path',\n";

                $this->bc[$edge[0]] = $this->bc[$edge[0]]+1;
            } else {
                $this->__browserDictionaryPHP($array, $node, $edge, $prefix.$name);
            }
        }
    }

    /**
     * Apply yuicompressor to a js or css file
     */
    private function __compressResource($node, $type) {
        if (!APP_DEBUG_MODE && $this->__compress && isset($node['compress']) && $node['compress'] && ($type !== 'tpl')) {

            if ($type === 'js' && isset($this->__minify) && $this->__minify === 'closure') {
                if (file_exists($closureFile=ARTA_DIR.'../minify/closure-compiler/compiler.jar')) {

                    $path    = str_replace('APP_DIR/', S_APP_DIR, $node['compress']);
                    $file    = pathinfo($path, PATHINFO_FILENAME);
                    $ext     = pathinfo($path, PATHINFO_EXTENSION);
                    $minFile = "$file-min.$ext";
                    $pathMin = str_replace("$file.$ext", $minFile, $path);

                    if (!file_exists($path)) {
                        $this->addMessage(sprintf(_('\'%s\' not found to be compressed!'), $path), 5);
                    } else {
                        // --compilation_level ADVANCED_OPTIMIZATIONS
                        shell_exec($cmd="java -jar $closureFile --js $path --js_output_file $pathMin");

                        if (!file_exists($pathMin)) {
                            $this->addMessage(sprintf('\'%s\' not created! %s', $pathMin, $cmd), 5);
                        } elseif ($this->__useCompress) {
                            $this->addMessage(sprintf('\'%s\' was compressed using Closure Compiler!', $pathMin), 7);
                            return str_replace("$file.$ext", $minFile, (string)$node['path']);
                        }
                    }
                } else {
                    $this->addMessage(
                        sprintf(
                            'Could not compress js because Closure Compiler "compilerjar" was not found inside "%s"!',
                            $closureFile
                        ),
                        5
                    );
                }
            } else {
                $yuicMissing = true;

                if (file_exists($yuicDir=ARTA_DIR.'../minify/yuicompressor/build/')) {
                    if ($handle=opendir($yuicDir)) {
                        /* This is the correct way to loop over the directory. */
                        while (false !== ($entry = readdir($handle))) {
                            if (strpos(strtolower($entry), 'yuicompressor') === 0 &&
                               strpos(strtolower($entry), '.jar') !== false) {
                                $yuicMissing = false;
                                $path        = str_replace('APP_DIR/', S_APP_DIR, $node['compress']);
                                $file        = pathinfo($path, PATHINFO_FILENAME);
                                $ext         = pathinfo($path, PATHINFO_EXTENSION);
                                $minFile     = "$file-min.$ext";
                                $pathMin     = str_replace("$file.$ext", $minFile, $path);

                                if (!file_exists($path)) {
                                    $this->addMessage(sprintf(_('\'%s\' not found to be compressed!'), $path), 5);
                                } else {
                                    shell_exec($cmd="java -jar $yuicDir$entry $path -o $pathMin --charset utf-8");

                                    if (!file_exists($pathMin)) {
                                        $this->addMessage(sprintf('\'%s\' not created! %s', $pathMin, $cmd), 5);
                                    } elseif ($this->__useCompress) {
                                        $this->addMessage(sprintf('\'%s\' was compressed using YUI Compressor!', $pathMin), 7);
                                        closedir($handle);
                                        return str_replace("$file.$ext", $minFile, (string)$node['path']);
                                    }
                                }
                            }
                        }
                        closedir($handle);
                    }
                }

                if ($yuicMissing) {
                    $this->addMessage(
                        sprintf(
                            'Could not compress js/css because "yuicompressor-X.X.X.jar" was not found inside "%s"!',
                            $yuicDir
                        ),
                        5
                    );
                }
            }
        }

        return (string)$node['path'];
    }

    /**
     * SmartyGetText > Rips gettext strings from smarty template.
     *
     * @link      http://smarty-gettext.sf.net/
     * @author    Sagi Bashari <sagi@boom.org.il>
     * @copyright 2004 Sagi Bashari
     * @param string $tplDirectory=null Templates path
     */
    public function extractSmartyi18n($tplDirectory=null) {
        if (!$tplDirectory) {
            $tplDirectory = $this->buildMethodArgs['extractSmartyi18n'];
        }
        if (!$tplDirectory) {
            return;
        }

        $t2 = microtime(true);
        $this->addMessage('Extracting Smarty templates i18n keys', 1);

        if (file_exists($file=$this->file['smarty_i18n'])) {
            require_once $file;
            $lastModify = $oldKeys = array();
            if (isset($keys)) {
                foreach ($keys as $entry => $key) {
                    $lastModify[$entry] = $key[0];
                    $oldKeys[$entry] = $key[1];
                }
            }
            $this->__lastModify = $lastModify;
        } else {
            $this->__lastModify = $oldKeys = array();
        }

        $this->__keys = array();
        $this->__extractSmartyi18nBrowse($tplDirectory);

        foreach ($this->__keys as $entry => $keys) {
            $oldKeys[$entry] = $keys;
        }
        /* * */
        $translationKeys = array();
        $contents = $keyContents = '';
        /* cache */
        foreach ($oldKeys as $entry => $keys) {
            if (file_exists($entry)) {
                $translationKeys = array_merge($translationKeys, $keys); 
                $keyContents .= "    '$entry' => array(\n".
                                '        '.filemtime($entry).",\n".
                                "        array(\n";
                foreach (array_unique($keys) as $key) {
                    $keyContents .= "            " . addslashes($key) . ",\n";
                }
                $keyContents .= "        )\n    ),\n";
            }
        }
        /* translations */
        foreach ($au=array_unique($translationKeys) as $key) {
            $contents .= '    '.addslashes($key).",\n";
        }
        /* write to file */
        $ok = file_put_contents(
            $file,
            "<?php /* Smarty Translation keys - created on buildup\n".
            "    by: Arta.Buildup\n    at: ".Arta::now('Y/m/d H:i:s')." */\n\n".
            '$keys = array('."\n".$keyContents.");\n".
            '$translations = array('."\n".$contents.');'
        );

        if ($ok === false) {
            $this->addMessage(sprintf(
                'Could not create Smarty Translation keys file \'%s\'. '.
                'Probably due lack of write permission.', $file), 6);
        } else {
            $this->addMessage(sprintf('%s keys extracted and stored in \'%s\'. Make this file visible to poedit.', count($au), $file), 7);
        }

        $this->addMessage(round(microtime(true)-$t2, 4).'s', 2);
    }


    private function __extractSmartyi18nBrowse($tplDirectory) {
        $dir = dir($tplDirectory);

        while (false !== ($entry=$dir->read())) {
            if ($entry == '.' || $entry == '..') {
                continue;
            }
            if (is_dir($entry=Buildout::p($tplDirectory, $entry))) {
                $this->__extractSmartyi18nBrowse($entry);
            } else {
                $this->__extractSmartyi18n($entry);
            }
        }

        $dir->close();
    }


    private function __fixSlashes($str) {
        $str = stripslashes($str);
        $str = str_replace('"', '\"', $str);
        $str = str_replace("\n", '\n', $str);
        return $str;
    }


    private function __extractSmartyi18n($file) {
        if (isset($this->__lastModify[$file]) && $this->__lastModify[$file] === filemtime($file)) {
            return;
        }
        /* * */
        $keys = array();
        // extensions of smarty files, used when going through a directory
        $extensions = array('tpl' => true,);
        $pi         = pathinfo($file);
        if (!isset($pi['extension']) || !isset($extensions[$pi['extension']])) {
            return;
        }
        // smarty open tag
        $ldq = preg_quote('{');
        // smarty close tag
        $rdq = preg_quote('}');
        // smarty command
        $cmd = preg_quote('t');
        // do
        $content = @file_get_contents($file);
        if (empty($content)) {
            return;
        }

        preg_match_all("/{$ldq}\s*({$cmd})\s*([^{$rdq}]*){$rdq}([^{$ldq}]*){$ldq}\/\\1{$rdq}/", $content, $matches);

        for ($i=0; $i < count($matches[0]); $i++) {
            if (preg_match('/plural\s*=\s*["\']?\s*(.[^\"\']*)\s*["\']?/', $matches[2][$i], $match)) {
                $keys[] = 'ngettext("'.$this->__fixSlashes($matches[3][$i]).'","'.$this->__fixSlashes($match[1]).'",x);';
            } else {
                $keys[] = 'gettext("'.$this->__fixSlashes($matches[3][$i]).'");';
            }
        }

        $this->__keys[$file] = $keys;
    }

    /**
     * Parses all templates(phptal) and extracts translation keys.
     *
     * @param string $tplDirectory=null Templates path
     */
    public function extractTALi18n($tplDirectory=null) {
        if (!$tplDirectory) {
            $tplDirectory = $this->buildMethodArgs['extractTALi18n'];
        }
        if (!$tplDirectory) {
            return;
        }

        $t2 = microtime(true);
        $this->addMessage('Extracting PHPTal templates i18n keys', 1);

        if (file_exists($file=$this->file['phptal_i18n'])) {
            require_once $file;
            $lastModify = $oldKeys = array();
            if (isset($keys)) {
                foreach ($keys as $entry => $key) {
                    $lastModify[$entry] = $key[0];
                    $oldKeys[$entry] = $key[1];
                }
            }
            $this->__lastModify = $lastModify;
        } else {
            $this->__lastModify = $oldKeys = array();
        }

        $this->__keys = array();
        $this->__extractTALi18nBrowse($tplDirectory);
        foreach ($this->__keys as $entry => $keys) {
            $oldKeys[$entry] = $keys;
        }
        /* * */
        $translationKeys = array();
        $keyContents = '';
        /* cache */
        foreach ($oldKeys as $entry => $keys) {
            if (file_exists($entry)) {
                $translationKeys = array_merge($translationKeys, $keys); 
                $keyContents .= "    '$entry' => array(\n".
                                '        '.filemtime($entry).",\n".
                                "        array(\n";
                foreach (array_unique($keys) as $key) {
                    $keyContents .= "            '".addslashes($key)."',\n";
                }
                $keyContents .= "        )\n    ),\n";
            }
        }
        /* translations */
        $contentsGettext = '';
        $contentsPo = '';
        foreach ($au=array_unique($translationKeys) as $key) {
            $contentsGettext .= "    _('".addslashes($key)."'),\n";
            $contentsPo      .= "msgid \"$key\"\nmsgstr \"\"\n\n";
        }
        /* write to file */
        $ok = file_put_contents(
            $file,
            "<?php /* PHP TAL Translation keys - created on buildup\n".
            "    by: Arta.Buildup\n    at: ".Arta::now('Y/m/d H:i:s')." */\n\n".
            '$keys = array('."\n".$keyContents.");\n".
            '$translations = array('."\n".$contentsGettext.");\n\n".
            "/* in pot file format\n".$contentsPo."\n*/"
        );
        if ($ok === false) {
            $this->addMessage(sprintf(
                'Could not create Smarty Translation keys file \'%s\'. '.
                'Probably due lack of write permission.',
                $file), 6);
        } else {
            $this->addMessage(sprintf('%s keys extracted and stored in \'%s\'. Make this file visible to poedit.', count($au), $file), 7);
        }

        $this->addMessage(round(microtime(true)-$t2, 4).'s', 2);
    }


    private function __extractTALi18nBrowse($tplDirectory) {
        $dir = dir($tplDirectory);
        while (false !== ($entry=$dir->read())) {
            if ($entry == '.' || $entry == '..') {
                continue;
            }

            if (is_dir($entry=Buildout::p($tplDirectory, $entry))) {
                $this->__extractTALi18nBrowse($entry);
            } else {
                $this->__extractTALi18n($entry);
            }
        }
        $dir->close();
    }


    private function __extractTALi18n($file) {
        if (isset($this->__lastModify[$file]) && $this->__lastModify[$file] === filemtime($file)) {
            return;
        }

        $content = trim(file_get_contents($file));
        if (strpos($content, '<?xml') === false && strpos($content, '<!DOCTYPE') === false) {
            $content = "<xml>$content</xml>";
        }

        $parser = xml_parser_create('');
        xml_parser_set_option($parser, XML_OPTION_TARGET_ENCODING, 'UTF-8');
        xml_parser_set_option($parser, XML_OPTION_CASE_FOLDING, 0);
        xml_parser_set_option($parser, XML_OPTION_SKIP_WHITE, 1);
        xml_parse_into_struct($parser, str_replace('&', '&amp;', $content), $nodes);
        xml_parser_free($parser);
        /* * */
        $keys = array();
        for ($i=0; $i<count($nodes); $i++) {
            if (!isset($nodes[$i])) {
                continue;
            }

            $node = $nodes[$i];
            /* i18n:translate */
            if (isset($node['attributes']['i18n:translate'])) {
                if ($val=$node['attributes']['i18n:translate']) {
                    if (substr($val, 0, 7) === 'string:') {
                        $keys[] = trim(substr($val, 7));
                    } elseif (substr($val, 0, 11) === "structure '") {
                        $keys[] = trim(substr($val, 11, -1));
                    } else {
                        $keys[] = trim($val, "'");
                    }
                } else {
                    $key = '';
                    if (isset($node['value']) && $node['value']) {
                        $key .= $node['value'];
                    }
                    // > if has params
                    if (isset($nodes[$j=$i+1]) && $nodes[$j]['level'] > $node['level']) {
                        for (; $j<count($nodes); $j++) {
                            if (!isset($nodes[$j])) {
                                continue;
                            }

                            $nodeX = $nodes[$j];
                            // if has params
                            if (isset($nodeX['attributes']['i18n:name'])) {
                                $key .= '${'.$nodeX['attributes']['i18n:name'].'}';
                            } elseif (isset($nodeX['value']) && $nodeX['value']) {
                                $key .= $nodeX['value'];
                            }
                            // reached same level as started
                            if ($nodeX['level'] == $node['level'] && $node['type'] === 'close') {
                                break;
                            }
                        }
                    }

                    if ($key=preg_replace('/\s\s+/', ' ', $key)) {
                        $keys[] = trim($key);
                    }
                }
            }
            /* i18n:attributes */
            if (isset($node['attributes']['i18n:attributes'])) {
                foreach (explode(';', $node['attributes']['i18n:attributes']) as $attr) {
                    $attr = explode(' ', $attr, 2);
                    if (isset($attr[1])) {
                        $keys[] = trim($attr[1], "'");
                    }
                }
            }
            /* i18n:name */
            if (isset($node['attributes']['i18n:name'])) {
                if (isset($node['value']) && $node['value']) {
                    $keys[] = trim($node['value']);
                }
            }
        }

        $this->__keys[$file] = $keys;
    }

    /**
     * Buildout error handler - converts error to exception and calls the exception handler.
     *
     * @param int    $code        Error code
     * @param string $description Error description
     * @param string $file        File which raised the error
     * @param int    $line        Line where the error happened
     * @throws ErrorException Throws the error as exception
     */
    static public function errorHandler($code, $description, $file, $line) {
        Buildout::exceptionHandler(new ErrorException($description, $code, 0, $file, $line));
    }

    /**
     * Buildout exception handler.
     *
     * @param ErrorException $exception ErrorException Object to be handled
     * @return void
     */
    static public function exceptionHandler($exception) {
        require_once 'Inspect.php';
        Inspect::dumpException($exception, 'Artaengine buildout error');
    }

    /**
     * Buildout message.
     * 0  header text
     * 1  function - start text
     * 2  function - time
     * 3  end text
     * 4  end text - time
     * 5  warning
     * 6  fatal error
     * 7  action description
     * 8  action description indented
     * 9  action description indented
     * 10 end
     *
     * @param string $msg=null
     * @param int    $type=0
     * @return void
     */
    public function addMessage($msg=null, $type=0) {
        $html         = Arta::$htmlDebug;
        $this->msgs[] = array($msg, $type);

        if ($this->__manual) {
            return;
        }

        if (!$msg) {
            if ($html) {
                ob_end_clean();
                ini_set('zlib.output_compression', 0);
                ini_set('implicit_flush', 1);
                header('Content-type:text/html; charset=UTF-8');

                echo '<!doctype html><html><head><meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>'.
                '<style>p, h2, html {font-family:arial;color:'.
                '#eee;background-color:#222;padding:4px;margin:0} p {font-size:12px;}</style></head>'.
                '<body><h2>Artaengine buildout</h2>'.
                '<p>Artaengine version: '.Arta::VERSION.'</p>'.
                '<p style="padding-bottom:10px">Buildout version: '.Buildout::VERSION.'</p>';
            } else {
                echo
                "----------------------------------------------------------------------\n".
                "Artaengine buildout\n\n".
                "Artaengine version: ".Arta::VERSION."\n".
                "Buildout version: ".self::VERSION;
            }
        }

        $atts = '';
        $tag  = 'p';
        $t    = '';
        switch ($type) {
            case 0:
                $tag = 'h2';
                break;
            case 1:
                $t = "\n  ";
                $atts = 'style="padding-left:50px"';
                break;
            case 2:
                $t = "    ";
                $atts = 'style="padding-left:70px"';
                break;
            case 3: break;
            case 4: break;
            case 5:
                $t = "    ";
                $atts = 'style="padding-left:70px;color:#E89600"';
                break;
            case 6:
                $t = "    ";
                $atts = 'style="padding-left:70px;color:#D62929"';
                break;
            case 7:
                $t = "    ";
                $atts = 'style="padding-left:70px;color:#1E90FF"';
                break;
            case 8:
                $t = "      ";
                $atts = 'style="padding-left:90px;color:#1E90FF"';
                break;
            case 9:
                $t = "      ";
                $atts = 'style="padding-left:90px;color:#E89600"';
                break;
            case 10:
                return;
        }

        if ($html) {
            if (!ob_get_level()) {
                ob_start();
            }
            echo "<$tag $atts>$msg</$tag>";
            flush();
            ob_flush();
        } else {
            echo "$t$msg\n";
        }
    }
}
