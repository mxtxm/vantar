<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7700
 * Created  2009/02/18
 * Updated  2012/03/21
 */

//namespace arta;

/**
 * This class is for creating, deleting, editing and database synchronizing "IModel" classes.<br/>
 * This class is used by the application builder to synch database with models.
 * This class can be used for doing interesting things with the models for example
 * you may dynamically edit/add/remove data models in an application at runtime
 * and then synchronize the changes with the database.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.4
 * @since      1.0.0
 * @link       http://artaengine.com/api/Models
 * @example    http://artaengine.com/examples/models
 */
class Models extends ModelSynch {

    private $__file = array(
        'include'      => null, // modelgroupname-include.php      models include
        'dictionary'   => null, // modelgroupname-dictionary.php   models dictionary
        'modify'       => null, // modelgroupname-modify.php       last modify date cache
        'dependencies' => null, // modelgroupname-dependencies.php dependencies graph
        'cleandb'      => null, // modelgroupname-clear-db.sql     drop all
        'synch'        => null, // modelgroupname-clear-db-{date}.sql synchlog
        'i18n'         => null, // models-i18n.php                 translation keys
        'index'        => null, // modelgroupname-index.sql        create index SQLs
        'translation'  => null, // modelgroupname-sql-t.php        create SQL translation cache
    );

    /**
     * Constructor. Though the database object, temp-dir and model-directory are known to the framework, this class is
     * app undependable, so this params are required to be passed.
     *
     * @param string $modelsDir Path to the model's directory
     * @param string $group     Model group name
     * @param IDbAdvanced $dbo=null   Database access instance,
     *        null=database actions will not be available
     * @param bool $allowDrop=true true=allow dropping database objects
     * @param string  $tempDir=null   Project temp directory
     * @throws FileWriteError
     */
    public function __construct($modelsDir, $group, IDbAdvanced $dbo=null, $allowDrop=true, $tempDir=TEMP_DIR) {
        ini_set('max_execution_time', 3600);
        $this->__allowDrop = (bool)$allowDrop;
        $this->__group     = $group = strtolower($group);
        $this->__modelsDir = Arta::makePath($modelsDir, '');

        if (!$dbo instanceof IDbAbstract) {
            return;
        }
        if (!$dbo instanceof IDbAdvanced) {
            $dbo = Database::upgrade($dbo);
        }
        $this->__dbo = $dbo;

        /* Model file paths */
        $tempDir = Arta::makePath($tempDir, '');
        $artaTempDir = $tempDir.'arta'.DIRECTORY_SEPARATOR;
        $this->__file['include']      = $artaTempDir.$group.'-include.php';
        $this->__file['dictionary']   = $artaTempDir.$group.'-dictionary.php';
        $this->__file['modify']       = $artaTempDir.$group.'-modify.php';
        $this->__file['dependencies'] = $artaTempDir.$group.'-dependencies.php';
        $this->__file['cleandb']      = $artaTempDir.$group.'-clear-db.sql';
        $this->__file['synch']        = $artaTempDir.'synch-logs'.DIRECTORY_SEPARATOR.$group;
        $this->__file['i18n']         = $tempDir."models-i18n-$group.php";
        $this->__file['index']        = $artaTempDir.$group.'-index.sql';
        $this->__file['translation']  = $artaTempDir.$group.'-sql-t.php';

        /* update model caches */
        $this->loadModifyCache();
        $this->saveModelDictionary();
    }

    /**
     * Deletes all cache/meta files created by this class.
     *
     * @param  bool $deleteDictionary=false false=do not delete models dictionary, it is not recommended to remove this
     *         file on a live system because each model is mapped to a number and if this dictionary is deleted the new
     *         created dictionary may have different values
     * @return Models For method chaining
     * @throws FileWriteError
     */
    public function clearCache($deleteDictionary=false) {
        if ($deleteDictionary && file_exists($this->__file['dictionary'])) {
            unlink($this->__file['dictionary']);
        }

        if (file_exists($this->__file['modify'])) {
            unlink($this->__file['modify']);
        }

        if (file_exists($this->__file['dependencies'])) {
            unlink($this->__file['dependencies']);
        }

        if (file_exists($this->__file['cleandb'])) {
            unlink($this->__file['cleandb']);
        }

        if (file_exists($this->__file['synch'])) {
            unlink($this->__file['synch']);
        }

        if (file_exists($this->__file['i18n'])) {
            unlink($this->__file['i18n']);
        }

        $this->loadModifyCache();
        $this->saveModelDictionary();
        return $this;
    }

    /**
     * Get model class model class names and their file paths and also caches their last modified filetimes
     * (to avoid synchronizing unchanged models).
     *
     * @return array [IModel-class-name: path-to-the-file,]
     */
    public function getModelFiles() {
        $models = array();
        foreach (new RecursiveIteratorIterator(new RecursiveDirectoryIterator($this->__modelsDir), 2) as $path) {
            if (!$path->isDir() && $path && substr($path=(string)$path, -1) !== '.') {
                $path = (string)$path;
                $part = pathinfo($path);
                if (isset($part['extension']) && isset($part['filename']) && strtolower($part['extension']) === 'php') {
                    $models[$model=$part['filename']] = $path;
                    if (isset($this->__modify[$model])) {
                        $this->__modify[$model][1] = filemtime($path);
                    } else {
                        $this->__modify[$model] = array(-2, filemtime($path));
                    }
                }
            }
        }
        /* add models to autoload */
        Arta::$autoload[] = $this->__modelsDir;
        foreach ($models as $path) {
            Arta::$autoload[] = pathinfo($path, PATHINFO_DIRNAME);
        }
        Arta::$autoload = array_unique(Arta::$autoload);
        /* filter model classes */
        $realModels = array();
        foreach ($models as $name => $path) {
            if (!class_exists($name)) {
                continue;
            }

            $class = new ReflectionClass($name);
            if ($class->isAbstract()) {
                continue;
            }

            $mObj = new $name();
            if (!($mObj instanceof Model) || !isset($mObj->__instanceId__)) {
                continue;
            }

            $realModels[$name] = $path;
        }
        /* * */
        return $realModels;
    }

    private function loadModifyCache() {
        $modelsLastModify = array();
        if (file_exists($this->__file['modify'])) {

            include_once $this->__file['modify'];

            foreach ($modelsLastModify as $k => &$v) {
                $v = array($v, -1);
            }
        }
        $this->__modify = $modelsLastModify;
    }

    /**
     * Models dependency/object graph
     * @throws FileWriteError
     */
    private function makeDependencies() {
        $dependencies = array();

        foreach ($this->getModelFiles() as $model => $file) {
            $mObj = new $model();
            if ($mObj instanceof Model && isset($mObj->__instanceId__)) {
                $dependencies[$model]['object'] = $mObj;
                $dependencies[$model]['table']  = $mObj->__table__;

                if ($mObj->__def__[3]) {
                    $dependencies[$model]['lang'] = $mObj->__table__.'_lang';
                }

                foreach ($mObj->__def__[2] as $def) {
                    $class = $def['class'];
                    $dependencies[$model]['multi'][] = array(
                        'class'   => $class,
                        'table'   => $def['table'],
                        'view'    => $def['view'],
                        'fk_that' => $def['obj']['fk'],
                        'fk_this' => $mObj->__fk__,
                    );
                    $dependencies[$class]['_multi'][$model] = array(
                        'class'   => $model,
                        'table'   => $def['table'],
                        'view'    => $def['view'],
                        'fk_this' => $def['obj']['fk'],
                        'fk_that' => $mObj->__fk__,
                    );
                }

                foreach ($mObj->__def__[1] as $def) {
                    $class = $def['class'];
                    $dependencies[$model]['upper'][$class] = $class;
                    $dependencies[$class]['lower'][$model] = $model;
                }

                foreach ($mObj->__def__[0] as $def) {
                    if (isset($def['relation'])) {
                        $class = $def['relation'];
                        $dependencies[$model]['upper'][$class] = $class;
                        $dependencies[$class]['lower'][$model] = $model;
                    }
                }
            }
        }
        $this->__dependencies = $dependencies;
        /* save to file */
        $contents = '';

        $currentModel = null;
        try {
            foreach ($dependencies as $model => $deps) {
                $currentModel = $model;
                $contents .= "    '$model' => array(\n";
                $contents .= "        'table' => '$deps[table]',\n";

                if (isset($deps['lang'])) {
                    $contents .= "        'lang' => '$deps[lang]',\n";
                }

                $contents .= "        'multi' => array(\n";

                if (isset($deps['multi'])) {
                    foreach ($deps['multi'] as $moreA) {
                        $more = "                'class'   => '$moreA[class]',\n".
                                "                'table'   => '$moreA[table]',\n".
                                "                'view'    => '$moreA[view]',\n".
                                "                'fk_this' => '$moreA[fk_this]',\n".
                                "                'fk_that' => '$moreA[fk_that]',\n";
                        $contents .= "            array(\n$more            ),\n";
                    }
                }

                $contents .= "        ),\n";
                $contents .= "        '_multi' => array(\n";

                if (isset($deps['_multi'])) {
                    foreach ($deps['_multi'] as $moreA) {
                        $more = "                'class'   => '$moreA[class]',\n".
                                "                'table'   => '$moreA[table]',\n".
                                "                'view'    => '$moreA[view]',\n".
                                "                'fk_this' => '$moreA[fk_this]',\n".
                                "                'fk_that' => '$moreA[fk_that]',\n";
                        $contents .= "            array(\n$more            ),\n";
                    }
                }

                $contents .= "        ),\n";
                $contents .= "        'upper' => array(\n";

                if (isset($deps['upper'])) {
                    foreach ($deps['upper'] as $model) {
                        $contents .= "            '$model' => '$model',\n";
                    }
                }

                $contents .= "        ),\n";
                $contents .= "        'lower' => array(\n";

                if (isset($deps['lower'])) {
                    foreach ($deps['lower'] as $model) {
                        $contents .= "            '$model' => '$model',\n";
                    }
                }

                $contents .= "        ),\n";
                $contents .= "    ),\n";
            }
        } catch (Exception $x) {
            $x->message = $currentModel . " >> " . $x->message;
            throw $x;
        }

        /* write to file */
        return $this->__write(
            $this->__file['dependencies'],
            ModelChunks::system('dependencies', $contents)
        );
    }

    /**
     * Save models modify date temp - to avoid build for not modified models
     * @throws FileWriteError
     */
    private function saveModelModifyDate() {
        $contents = '';
        foreach ($this->__modify as $model => $dates) {
            $contents .= "    '$model' => {$dates[1]},\n";
        }
        /* write to file */
        return $this->__write(
            $this->__file['modify'],
            ModelChunks::system('modify', $contents)
        );
    }

    /**
     * Save models dictionary file - alias class to a number
     * @throws FileWriteError
     */
    private function saveModelDictionary() {
        $modelsDic = array();
        if (file_exists($this->__file['dictionary'])) {
            include $this->__file['dictionary'];
            $modelsDic = Model::$__mdic__[$this->__group];
        }
        /* if there is new models, add them to dic and assign a unique nom */
        $maxNom = $modelsDic && is_array($modelsDic)? max($modelsDic): 0;
        foreach (array_keys($this->getModelFiles()) as $model) {
            if (!isset($modelsDic[$model])) {
                $modelsDic[$model] = ++$maxNom;
                $this->__modify[$model][0] = -2;
            }
        }
        /* dictionary */
        Model::$__mdic__[$this->__group] = $this->__dictionary = $modelsDic;
        $dics = '';
        foreach ($modelsDic as $model => $nom) {
            $dics .= "    '$model' => $nom,\n";
        }
        /* model directories - used for autoload */
        $dirs = "    '".rtrim($this->__modelsDir, '/')."',\n";
        foreach (new RecursiveIteratorIterator(new RecursiveDirectoryIterator($this->__modelsDir), 2) as $path) {
            if ($path->isDir() && $path && substr($path, -1) !== '.') {
                $dirs .= "    '$path',\n";
            }
        }
        /* lang where */
        $this->makeDependencies();
        $this->__langWhere = array();
        foreach ($this->__dependencies as $model => $dep) {
            if ($dep['object']->__def__[3]) {
                $this->__langWhere[$model][$model] = 'lang = $_L_';
            }
            if (isset($dep['upper'])) {
                $this->createLangConditionPerModel($model, $dep['upper']);
            }
        }

        foreach ($this->__langWhere as $model => &$where) {
            $where = "    '$model' => '(".implode(' AND ', $where).")',";
        }

        $langWhere = '';
        if ($this->__langWhere) {
            $langWhere = 'Model::$__langwhere__ = array('."\n".implode("\n", $this->__langWhere)."\n);\n";
        }
        /* write to file */
        $ok = $this->__write(
            $this->__file['dictionary'],
            ModelChunks::system(
                'dictionary',
                '$path = \''.rtrim($this->__modelsDir, '/')."';\n".$langWhere.
                    'Model::$__mdic__[\''."$this->__group'] = array(\n$dics);"
            )
        );

        $optimize = isset(Arta::$configs['model:'.$this->__group]['optimize'])?
            Arta::$configs['model:'.$this->__group]['optimize']: true;

        $ok = $ok && $this->__write(
            $this->__file['include'],
            ModelChunks::system(
                'include',
                $optimize?
                    'if (DEBUG)'.
                    '    Arta::$autoload = array_merge(Arta::$autoload,  array('."\n".$dirs."));".
                    'else'.
                    '    Arta::$autoload[] = "'.TEMP_DIR.'arta/'.$this->__group.'";':
                    'Arta::$autoload = array_merge(Arta::$autoload,  array('."\n".$dirs."));"
            )
        );
        /* cache for class to DB SQL translations */
        if (!$ok) {
            return false;
        }

        $startC = ' c';
        $from1 = $from2 = $to1 = $to2 = array();
        foreach ($modelsDic as $class => $db) {
            $from1[] = "/(\s+){$class}[_.]/";
            $to1[] = $startC.$db.'_';
            $from2[] = "/[_.]{$class}[_.]/";
            $to2[] = '_'.$db.'_';
        }

        $startC = ' ';
        $fromX1 = $fromX2 = $toX1 = $toX2 = array();
        foreach ($modelsDic as $class => $db) {
            $db = strtolower($class);           
            $fromX1[] = "/(\s+){$class}[_.]/";
            $toX1[] = $startC.$db.'_';
            $fromX2[] = "/[_.]{$class}[_.]/";
            $toX2[] = '_'.$db.'_';
        }

        return $this->__write(
            $this->__file['translation'],
            ModelChunks::system(
                'translation',
                'function cp2db($str) {'."\n".
                '    return preg_replace('."\n".
                '        '.Utils::array2string($from1, array('i' => 2)).",\n".
                '        '.Utils::array2string($to1, array('i' => 2)).",\n".
                '        preg_replace('."\n".
                '            '.Utils::array2string($from2, array('i' => 3)).",\n".
                '            '.Utils::array2string($to2, array('i' => 3)).",\n".
                '            $str'."\n"."        )\n    );\n".
                '}'."\n".
                'function tc2db($str) {'.
                '    return preg_replace('."\n".
                '        '.Utils::array2string($fromX1, array('i' => 2)).",\n".
                '        '.Utils::array2string($toX1, array('i' => 2)).",\n".
                '        preg_replace('."\n".
                '            '.Utils::array2string($fromX2, array('i' => 3)).",\n".
                '            '.Utils::array2string($toX2, array('i' => 3)).",\n".
                '            $str'."\n"."        )\n    );\n".
                '}'
            )
        );
    }

    private function createLangConditionPerModel($mainModel, $deps) {
        foreach ($deps as $model) {
            if ($this->__dependencies[$model]['object']->__def__[3]) {
                $this->__langWhere[$mainModel][$model] = 'c'.Model::$__mdic__[$this->__group][$model].'_lang = $_L_';
            }
            if (isset($this->__dependencies[$model]['upper'])) {
                $this->createLangConditionPerModel($mainModel, $this->__dependencies[$model]['upper']);
            }
        }
    }

    /**
     * Create an SQL file of "delete all model relations" SQLs.
     *
     * @return bool State of success
     * @throws FileWriteError
     */
    public function createDropSQLFile() {
        $this->__junctions = $this->__sqls = $this->__droped = array();

        foreach ($this->__dependencies as $modelName => $deps) {
            if (!isset($this->__sqls[$modelName])) {
                $this->createTableAndViewRemoveSqls($modelName, $deps);
            }
        }
        /* prepare sqls */
        $sqls = '';
        foreach ($this->__sqls as $modelName => $drop) {
            $sqls .= "--- Model name: $modelName\n".implode("\n", $drop)."\n";
        }
        $junctions = "--- Junction views\n".implode("\n", $this->__junctions)."\n";
        /* write to sql file */
        return $this->__write(
            $this->__file['cleandb'],
            ModelChunks::system('cleandb', $junctions.$sqls)
        );
    }

    protected function createTableAndViewRemoveSqls($modelName, $deps, $viewOnly=false) {
        if (isset($deps['lower'])) {
            foreach ($deps['lower'] as $lowerModelName) {
                if (!isset($this->__sqls[$lowerModelName])) {
                    $this->createTableAndViewRemoveSqls(
                        $lowerModelName,
                        $this->__dependencies[$lowerModelName],
                        $viewOnly
                    );
                }
            }
        }

        $table = $deps['table'];
        $sqls  = array();

        if (isset($deps['multi'])) {
            foreach ($deps['multi'] as $more) {
                $junction = $more['table'];
                if (isset($more['view'])) {
                    $view = $more['view'];
                    $this->__junctions[$view] = "DROP VIEW IF EXISTS $view;";
                }
                if (!$viewOnly && !isset($this->__droped[$junction])) {
                    $sqls[$junction] = "DROP TABLE IF EXISTS $junction;";
                }
                $this->__droped[$junction] = true;
            }
        }

        $view = "v_{$table}_deleted";
        $sqls[$view] = "DROP VIEW IF EXISTS $view;";
        $view = "v_{$table}_full";
        $sqls[$view] = "DROP VIEW IF EXISTS $view;";
        $view = "v_{$table}_1";
        $sqls[$view] = "DROP VIEW IF EXISTS $view;";
        $view = "v_$table";
        $sqls[$view] = "DROP VIEW IF EXISTS $view;";

        if (!$viewOnly && isset($deps['lang'])) {
            $view = $deps['lang'];
            $sqls[$t=$deps['lang']] = "DROP TABLE IF EXISTS $view;";
        }
        if (!$viewOnly) {
            $sqls[$table] = "DROP TABLE IF EXISTS $table;";
        }

        $this->__sqls[$modelName] = $sqls;
    }

    /**
     * Searches inside a model class PHP file for an array and returns the array value.
     *
     * @param string $class         Model class name
     * @param string $name          Target array variable name
     * @param string $filepath=null Model file path, null=use models group path
     * @return array  The found array
     */
    public function getArray($class, $name, $filepath=null) {
        $contents = file_get_contents($filepath? $filepath: $this->__modelsDir."$class.php");
        $val      = array();
        if (preg_match('/\$'.$name.'\s*\=\s*array\([^;]+/', $contents, $m) && isset($m[0])) {
            eval(str_replace('$'.$name, '$val', $m[0]).';');
        }
        return $val;
    }

    /**
     * Edit a model's PHP file and replace value of the first found array with newVal.
     *
     * @param string $class         Model class name
     * @param string $name          Target array variable name
     * @param array  $newVal        Value to be put in-place of the older one
     * @param int    $indent=2      Number of indents. Each indent is four spaces
     * @param string $filepath=null Model file path, null=use models group path
     * @return bool   State of success
     * @throws FileWriteError
     */
    public function replaceArray($class, $name, $newVal, $indent=2, $filepath=null) {
        if (!$filepath) {
            $filepath = $this->__modelsDir . "$class.php";
        }
        $contents = file_get_contents($filepath);
        if (preg_match('/\$'.$name.'\s*\=\s*array\([^;]+/', $contents, $m) && isset($m[0])) {
            $new = "$$name = ".Utils::array2string($newVal, array(
                'i'     => $indent,
                'const' => ModelChunks::$constants,
            ));
            return $this->__write(
                $filepath,
                implode($new, explode($m[0], $contents, 2))
            );
        }

        return false;
    }

    private function propertyValueToString($name, $val, $type='public') {
        if ($val === null) {
            return "    $type $$name = null;";
        } elseif (is_array($val) && !$val) {
            return "    $type $$name = array();";
        } elseif (is_array($val)) {
            if (count($val) == 1 && isset($val[0])) {
                return "    $type $$name = array('$val[0]');";
            }

            return "    $type $$name = ".
                Utils::array2string($val, array(
                    'model' => true,
                    'const' => ModelChunks::$constants,
                )).';';

        } else {
            if (is_numeric($val)) {

            } elseif (is_bool($val)) {
                $val = $val? 'true': 'false';
            } else {
                $val = "'$val'";
            }
            return "    $type $$name = $val;";
        }
    }

    private function propertyDefToString($defs) {
        $data = array();
        foreach ($defs as $name => $val) {
            $data[$name] = $this->propertyValueToString($name, $val).(isset($defs["__{$name}__"])? '': "\n");
        }
        return $data;
    }

    /**
     * Create a new model class file.
     *
     * @param array $def              Model definiition [@Models.create.def]
     * @param array $dataPropertyDefs Model data property def  [@Models.create.dataPropertyDefs]
     * @param array $events=null      Events to be added [@Models.create.events]
     * @return bool State of success
     * @throws FileWriteError
     */
    public function create($def, $dataPropertyDefs, $events=null) {
        if (isset($def['constants'])) {
            array_merge(ModelChunks::$constants, $def['constants']);
        }

        $class = isset($def['class'])? $def['class']: $def['name'];
        if (isset($def['file'])) {
            $filepath = $def['file'];
            $dir      = pathinfo($filepath, PATHINFO_DIRNAME);
            if ($dir && !file_exists($dir)) {
                mkdir($dir, 0777, true);
            }
        } elseif (isset($def['path'])) {
            $filepath = str_replace('\\', '/',
                rtrim(rtrim($this->__modelsDir, '/'),
                '\\').'/'.
                trim(trim($def['path'], '\\'), '/')
            );

            if (!file_exists($filepath)) {
                mkdir($filepath, 0777, true);
            }
            $filepath .= "/$class.php";
        } else {
            $filepath = $this->__modelsDir."$class.php";
        }

        $extends = isset($def['parent'])?  $def['parent']:  'Model';
        $creator = isset($def['creator'])? $def['creator']: 'Arta.Models';
        $updater = isset($def['updater'])? $def['updater']: 'Arta.Models';
        /* properties */
        $propertiesPHP = '';
        if (isset($def['delete']) && !$def['delete']) {
            $propertiesPHP .= '    public $__delete__ = false;'."\n";
        }
        if (isset($def['table']) && $def['table']) {
            $propertiesPHP .= '    public $__table__ = '."'$def[table]';\n";
        }
        if ($propertiesPHP) {
            $propertiesPHP .= "\n";
        }
        if (isset($def['public'])) {
            foreach ($def['public'] as $name => $val) {
                $propertiesPHP .= $this->propertyValueToString("__{$name}__", $val, 'public')."\n";
            }
            $propertiesPHP .= "\n";
        }
        if (isset($def['private'])) {
            foreach ($def['private'] as $name => $val) {
                $propertiesPHP .= $this->propertyValueToString("__{$name}__", $val, 'private')."\n";
            }
            $propertiesPHP .= "\n";
        }
        if (isset($def['protected'])) {
            foreach ($def['protected'] as $name => $val) {
                $propertiesPHP .= $this->propertyValueToString("__{$name}__", $val, 'protected')."\n";
            }
            $propertiesPHP .= "\n";
        }
        /* events, methods */
        $methodsPHP = $extends=='Model'? ModelChunks::construct($this->__group, isset($def['title'])? $def['title']: null)."\n": '';
        if ($events) {
            if (isset($events['add']['before'])    && $events['add']['before']) {
                $methodsPHP .= ModelChunks::beforeAdd() . "\n";
            }

            if (isset($events['add']['after'])     && $events['add']['after']) {
                $methodsPHP .= ModelChunks::afterAdd() . "\n";
            }

            if (isset($events['update']['before']) && $events['update']['after']) {
                $methodsPHP .= ModelChunks::beforeUpdate() . "\n";
            }

            if (isset($events['update']['after'])  && $events['update']['after']) {
                $methodsPHP .= ModelChunks::afterUpdate() . "\n";
            }

            if (isset($events['delete']['before']) && $events['delete']['before']) {
                $methodsPHP .= ModelChunks::beforeDelete() . "\n";
            }

            if (isset($events['delete']['after'])  && $events['delete']['after']) {
                $methodsPHP .= ModelChunks::afterDelete() . "\n";
            }

            if (isset($events['query']['before'])  && $events['query']['before']) {
                $methodsPHP .= ModelChunks::beforeQuery() . "\n";
            }

            if (isset($events['query']['after'])   && $events['query']['after']) {
                $methodsPHP .= ModelChunks::afterQuery() . "\n";
            }

            if (isset($events['next']['after'])    && $events['next']['after']) {
                $methodsPHP .= ModelChunks::afterNext() . "\n";
            }
        }

        if (isset($def['methods'])) {
            foreach ($def['methods'] as $name => $def) {
                $methodsPHP .= "\n".
                    (isset($def['comment'])? "$def[comment]\n": '').'    '.
                    (isset($def['private'])? 'private':
                        (isset($def['protected'])? 'protected': 'public')).
                    " function $name(".(isset($def['args'])? $def['args']: '').
                    ")\n    {\n$def[source]\n    }\n";
            }
        }
        /* * */
        return $this->__write(
            $filepath,
            "<?php\n".
            "// Copyright (C) ".Arta::now('Y')." by $creator\n".
            "//\n".
            "// $class data model\n".
            "//\n".
            "// Created: ".($dt=Arta::now('Y/m/d H:i'))." - $creator\n".
            "// Updated: $dt - $updater \n\n\n".
            "class $class extends $extends\n{\n".$propertiesPHP.
            implode("\n", $this->propertyDefToString($dataPropertyDefs))."\n$methodsPHP}"
        )? $filepath: false;
    }

    /**
     * Update a model class file.
     *
     * @param array $def              Model definiition [@Models.update.def]
     * @param array $dataPropertyDefs Model data property def [@Models.update.dataPropertyDefs]
     * @param array $events=null      Events to be added or removed [@Models.update.events]
     * @return bool State of success
     * @throws FileWriteError
     */
    public function update($def, $dataPropertyDefs, $events=null) {
        if (isset($def['constants'])) {
            array_merge(ModelChunks::$constants, $def['constants']);
        }

        $class = isset($def['class'])? $def['class']: $def['name'];
        if (isset($def['file'])) {
            $filepath = $def['file'];
        } elseif (isset($def['path'])) {
            $filepath = str_replace(
                '\\',
                '/',
                trim(rtrim($this->__modelsDir, '/'), '\\') . '/'. trim(trim($def['path'], '\\'), '/') . "/$class.php"
            );
        } else {
            $filepath = $this->__modelsDir."$class.php";
        }

        $updater = isset($def['updater'])? $def['updater']: 'Arta.Models';
        if (!file_exists($filepath)) {
            return $this->create($def, $dataPropertyDefs, $events);
        }
        /* add/remove properties */
        $this->propertyAdd($class, $dataPropertyDefs, $filepath);
        $toRemove      = isset($def['remove'])? $def['remove']: (isset($def['delete-properties'])? $def['delete-properties']: array());
        $toRemove[]    = '__delete__';
        $toRemove[]    = '__table__';
        $propertiesPHP = '';

        if (isset($def['delete']) && !$def['delete']) {
            $propertiesPHP .= '    public $__delete__ = false;'."\n";
        }
        if (isset($def['table']) && $def['table']) {
            $propertiesPHP .= '    public $__table__ = '."'$def[table]';\n";
        }
        if (isset($def['public'])) {
            if ($propertiesPHP) {
                $propertiesPHP .= "\n";
            }
            foreach ($def['public'] as $name => $val) {
                $propertiesPHP .= $this->propertyValueToString($toRemove[]="__{$name}__", $val, 'public')."\n";
            }
        }
        if (isset($def['private'])) {
            if ($propertiesPHP) {
                $propertiesPHP .= "\n";
            }
            foreach ($def['private'] as $name => $val) {
                $propertiesPHP .= $this->propertyValueToString($toRemove[]="__{$name}__", $val, 'private')."\n";
            }
        }
        if (isset($def['protected'])) {
            if ($propertiesPHP) {
                $propertiesPHP .= "\n";
            }
            foreach ($def['protected'] as $name => $val) {
                $propertiesPHP .= $this->propertyValueToString($toRemove[]="__{$name}__", $val, 'protected')."\n";
            }
        }

        $this->propertyRemove($class, $toRemove, $filepath);
        $this->propertyAdd($class, $propertiesPHP, $filepath);
        /* events, methods */
        $methodsRemove = isset($def['methods-remove'])? $def['methods-remove']: array();
        $methodsAdd['__construct'] =
            preg_match('/class\s+'.$class.'\s+extends\s+Model.*/i', file_get_contents($filepath))?
            ModelChunks::construct($this->__group, isset($def['title'])? $def['title']: null): '';

        if ($events) {
            if (isset($events['add']['before'])) {
                if ($events['add']['before']) {
                    $methodsAdd['beforeAdd'] = ModelChunks::beforeAdd();
                } else {
                    $methodsRemove[]='beforeAdd';
                }
            }

            if (isset($events['add']['after'])) {
                if ($events['add']['after']) {
                    $methodsAdd['afterAdd']=ModelChunks::afterAdd();
                } else {
                    $methodsRemove[]='afterAdd';
                }
            }

            if (isset($events['update']['before'])) {
                if ($events['update']['before']) {
                    $methodsAdd['beforeUpdate']=ModelChunks::beforeUpdate();
                } else {
                    $methodsRemove[]='beforeUpdate';
                }
            }

            if (isset($events['update']['after'])) {
                if ($events['update']['after']) {
                    $methodsAdd['afterUpdate']=ModelChunks::afterUpdate();
                } else {
                    $methodsRemove[]='afterUpdate';
                }
            }

            if (isset($events['delete']['before'])) {
                if ($events['delete']['before']) {
                    $methodsAdd['beforeDelete']=ModelChunks::beforeDelete();
                } else {
                    $methodsRemove[]='beforeDelete';
                }
            }

            if (isset($events['delete']['after'])) {
                if ($events['delete']['after']) {
                    $methodsAdd['afterDelete']=ModelChunks::afterDelete();
                } else {
                    $methodsRemove[]='afterDelete';
                }
            }

            if (isset($events['query']['before'])) {
                if ($events['query']['before']) {
                    $methodsAdd['beforeQuery']=ModelChunks::beforeQuery();
                } else {
                    $methodsRemove[]='beforeQuery';
                }
            }

            if (isset($events['query']['after'])) {
                if ($events['query']['after']) {
                    $methodsAdd['afterQuery']=ModelChunks::afterQuery();
                } else {
                    $methodsRemove[]='afterQuery';
                }
            }

            if (isset($events['next']['after'])) {
                if ($events['next']['after']) {
                    $methodsAdd['afterNext'] = ModelChunks::afterNext();
                } else {
                    $methodsRemove[] = 'afterNext';
                }
            }
        }

        if (isset($def['methods'])) {
            foreach ($def['methods'] as $name => $def) {
                $methodsAdd[$name] = "\n".
                    (isset($def['comment'])? "$def[comment]\n": '').'    '.
                    (isset($def['static'])? 'static ': '').
                    (isset($def['private'])? 'private':
                        (isset($def['protected'])? 'protected': 'public')).
                    " function $name(".(isset($def['args'])? $def['args']: '').
                    ")\n    {\n$def[source]\n    }\n";
            }
        }

        $this->methodRemove($class, $methodsRemove, $filepath);
        $this->methodAdd($class, $methodsAdd, $filepath);
        /* * */
        $contents = file_get_contents($filepath);
        $contents = preg_replace('/\n\s\s\s\sUpdated\:\s.*\s\*\//', "\n    Updated: ".Arta::now('Y/m/d H:i')." - $updater */", $contents);

        return $this->__write($filepath, $contents)? $filepath: false;
    }

    /**
     * Remove a model file.
     *
     * @param string $class Model class name
     */
    public function remove($class) {
        if (file_exists($this->__modelsDir.$class.'.php')) {
            unlink($this->__modelsDir.$class.'.php');
        }
    }

    /**
     * Add/Update data property in a model file (if property exists it will be updated).
     *
     * @param string $class         Model class name
     * @param array  $defs          Property definition
     * @param string $filepath=null Model file path, null=use models group path
     * @return bool State of success
     * @throws FileWriteError
     */
    public function propertyAdd($class, $defs, $filepath=null) {
        if (!$defs)
            return true;

        if (!$filepath)
            $filepath = $this->__modelsDir."$class.php";

        /* clear old property defs */
        if (is_array($defs)) {
            $this->propertyRemove(
                $class,
                array_keys($defs=$this->propertyDefToString($defs)),
                $filepath
            );
            $defs = implode("\n", $defs);
        }
        /* add new property defs */
        $lines     = file($filepath, FILE_IGNORE_NEW_LINES);
        $classLine = null;

        foreach ($lines as $i => $line) {
            if (preg_match('/class\s+'.$class.'/i', $line)) {
                $classLine = $i;
            }
            /* insert new defs immediately after class is seen */
            if ($classLine && strpos($line, '{') !== false) {
                $lines[$i] = trim($lines[$i], "\n")."\n$defs";
                return $this->__write($filepath, implode("\n", $lines))? true: false;
            }
        }

        return false;
    }

    /**
     * Remove property or properties from a model file. Alias of "propertyRemove()".
     *
     * @param string       $class         Model class name
     * @param string|array $property      Property name or [property-names,] to be removed
     * @param string       $filepath=null Model file path, null=use models group path
     * @return bool State of success
     * @throws FileWriteError
     */
    public function propertyDelete($class, $property, $filepath=null) {
        $this->propertyRemove($class, $property, $filepath);
    }

    /**
     * Remove property or properties from a model file.
     *
     * @param string       $class         Model class name
     * @param string|array $property      Property name or [property-names,] to be removed
     * @param string       $filepath=null Model file path, null=use models group path
     * @return bool State of success
     * @throws FileWriteError
     */
    public function propertyRemove($class, $property, $filepath=null) {
        if (!$property) {
            return true;
        }

        if (!$filepath) {
            $filepath = $this->__modelsDir . "$class.php";
        }

        $unset = false;
        $lines = file($filepath, FILE_IGNORE_NEW_LINES);
        foreach ($lines as $i => $line) {
            if (!$unset) {
                foreach ((array)$property as $name) {
                    if (preg_match('/(public|private|protected|static public|static '.
                        'private|static protected)\s*\$'.$name.'\s*=/i', $line)) {
                        $unset = true;
                        break;
                    }
                }
            }
            if ($unset) {
                if (strpos($line, ';') !== false) {
                    $unset = false;
                    if (isset($lines[$i+1]) && !trim($lines[$i+1])) {
                        unset($lines[$i+1]);
                    }
                }
                unset($lines[$i]);
            }
        }

        return $this->__write($filepath, implode("\n", $lines))? true: false;
    }

    /**
     * Remove method or methods from a model file. Alias of "methodRemove()".
     *
     * @param string       $class         Model class name
     * @param string|array $method        Method name or [method-names,] to be removed
     * @param string       $filepath=null Model file path, null=use models group path
     * @return bool State of success
     * @throws FileWriteError
     */
    public function methodDelete($class, $method, $filepath=null) {
        $this->methodRemove($class, $method, $filepath);
    }

    /**
     * Remove method or methods from a model file.
     *
     * @param string       $class         Model class name
     * @param string|array $method        Method name or [method-names,] to be removed
     * @param string       $filepath=null Model file path, null=use models group path
     * @return bool State of success
     * @throws FileWriteError
     */
    public function methodRemove($class, $method, $filepath=null) {
        if (!$method) {
            return true;
        }

        if (!$filepath) {
            $filepath = $this->__modelsDir . "$class.php";
        }

        $unset = $bracsInc = false;
        $bracs = 0;
        $lines = file($filepath, FILE_IGNORE_NEW_LINES);

        foreach ($lines as $i => $line) {
            if (!$unset) {
                foreach ((array)$method as $name) {
                    if (preg_match('/(public|private|protected|public\s+static|'.
                       'private\s+static|protected\s+static)\s+function\s+'.$name.
                       '\s*\(/i', $line)) {
                        $unset = true;
                        if (isset($lines[$j=$i-1]) && strpos($lines[$j], '*/') !== false) {
                            for (; $j>0; --$j) {
                                $tmp = $lines[$j];
                                unset($lines[$j]);
                                if (strpos($tmp, '/*') !== false) {
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }
            }

            if ($unset) {
                if ($b=substr_count($line, '{')) {
                    $bracsInc = true;
                }
                $bracs += $b;
                $bracs -= substr_count($line, '}');
                if ($bracsInc && $bracs < 1) {
                    $bracs = 0;
                    $unset = $bracsInc = false;
                    if (isset($lines[$i+1]) && !trim($lines[$i+1])) {
                        unset($lines[$i+1]);
                    }
                }
                if (isset($lines[$i-1]) && !trim($lines[$i-1])) {
                    unset($lines[$i-1]);
                }
                unset($lines[$i]);
            }
        }
        return $this->__write($filepath, implode("\n", $lines))? true: false;
    }

    /**
     * Add method(s) to a model file.
     *
     * @param string $class         Model class name
     * @param array  $defs          Method definition [@Models.methodAdd.defs]
     * @param string $filepath=null Model file path, null=use models group path
     * @return bool State of success
     * @throws FileWriteError
     */
    public function methodAdd($class, array $defs, $filepath=null) {
        if (!$filepath) {
            $filepath = $this->__modelsDir . "$class.php";
        }

        /* existing methods */
        foreach ($defs as $method => $def) {
            if (method_exists($class, $method)) {
                unset($defs[$method]);
            }
        }

        if (!$defs) {
            return true;
        }
        /* add new property defs */
        $lines     = file($filepath, FILE_IGNORE_NEW_LINES);
        $classLine = null;
        $bracs     = 0;
        $check     = false;
        foreach ($lines as $i => $line) {
            if (!$classLine && preg_match('/class\s+'.$class.'/i', $line)) {
                $classLine = $i;
            }
            if ($classLine) {
                $bracs += substr_count($line, '{');
                if ($bracs) {
                    $check = true;
                }
                $bracs -= substr_count($line, '}');
                // last line of class
                if ($check && !$bracs) {
                    $lastLine = explode('}', $line);
                    array_pop($lastLine);
                    $lines[$i] = implode('}', $lastLine).implode("\n", $defs)."\n}";
                    for ($j=$i-1; $j>0; --$j) {
                        if ($lines[$j]) {
                            break;
                        } else {
                            unset($lines[$j]);
                        }
                    }

                    return $this->__write($filepath, implode("\n", $lines))? true: false;
                }
            }
        }
        return false;
    }

    /**
     * Synchronize all or one model with the database, update all cache/meta files.
     *
     * @param string|array $model=null null=synch all models, string=model-name to be synched array=list of model-names to be synched
     * @return array|bool false=not successful or array of statistics
     * @throws FileWriteError
     */
    public function synch($model=null) {
        $statistics = array();
        $this->__synchronized     = array();
        $this->__junctionViewDefs = array();
        $this->__sqls = array();
        $this->__keys = array();

        if (!($models=$this->getModelFiles())) {
            return false;
        }

        /* if model has changed - sign  */
        $tochange     = array();
        $modelModify  = $this->__modify;
        $dependencies = $this->__dependencies;

        foreach ($models as $modelName => $file) {
            $this->__keys[] = $modelName;
            if ($modelModify[$modelName][0] != $modelModify[$modelName][1] && isset($dependencies[$modelName]['lower'])) {
                $tochange += $dependencies[$modelName]['lower'];
            }
        }
        foreach ($tochange as $modelName) {
            $modelModify[$modelName][0] = 0;
            $modelModify[$modelName][1] = 1;
        }
        /* * */
        include_once 'Model.php';
        if ($model) {
            foreach ((array)$model as $modelName) {
                if (isset($models[$modelName])) {
                    $this->synchDbWithModels($modelName);
                }
            }
        } else {
            foreach ($models as $modelName => $v) {
                $this->synchDbWithModels($modelName);
            }
        }

        // result
        $tables = $this->__sqls;
        /* Perform changes - Save log */
        $dbo  = $this->__dbo;
        $dump = '';
        $dbo->query('BEGIN;');
        // VIEWS > DROP
        $existing = $dbo->getMetaViews(null, true);
        $count    = 0;
        if (isset($this->__junctions) && $this->__junctions) {
            $dump .= "--- DROP VIEWS - junctions\n";
            foreach ($this->__junctions as $vname => $sql) {
                if (isset($existing[$vname])) {
                    $dump .= "$sql\n";
                    $dbo->query($sql);
                    ++$count;
                }
            }
        }

        $statistics['view-drop-junction'] = $count;
        $count = 0;

        foreach ($this->dropViews() as $className => $sqls) {
            $dump .= "--- DROP VIEWS - Model name: $className\n";
            foreach ($sqls as $vname => $sql) {
                if (isset($existing[$vname])) {
                    $dump .= "$sql\n";
                    $dbo->query($sql);
                    ++$count;
                }
            }
        }

        $statistics['view-drop'] = $count;
        // TABLE > CREATE/ALTER
        $count = 0;
        foreach ($tables as $className => $sqls) {
            $dump .= "--- TABLE - Model name: $className\n";
            foreach ($sqls as $sql) {
                if ($sql) {
                    $dump .= "$sql\n";
                    $dbo->query($sql);
                    ++$count;
                }
            }
        }
        $statistics['table-create-alter'] = $count;
        // VIEWS > CREATE
        $count    = 0;
        $viewSqls = $this->createViews();
        foreach ($viewSqls['table'] as $className => $views) {
            $dump .= "--- CREATE VIEWS - Model name: $className\n";
            foreach ($views as $sql) {
                $dump .= "$sql\n";
                //$dbo->query($sql);
                //++$count;
            }
        }
        $statistics['views-create'] = $count;
        // VIEWS JUNCTION > CREATE
        $count = 0;
        foreach ($viewSqls['junction'] as $className => $views) {
            $dump .= "--- CREATE JUNCTION VIEWS - Model name: $className\n";
            foreach ($views as $sql) {
                $dump .= "$sql\n";
                //$dbo->query($sql);
                //++$count;
            }
        }
        $statistics['views-create-junction'] = $count;
        $dbo->query('COMMIT;');
        /* write to log */
        $time = Arta::now('Y/m/d H:i');
        $ok   = $this->__write(
            $this->__file['synch'].'.'.str_replace('/', '-', $time).'.sql',
            ModelChunks::system('synch', $dump)
        );
        $statistics['log-'.($ok? 'ok': 'ko')] = $this->__file['synch'].$time.'.sql';
        /* indexes for deleted */
        if ($this->__index) {
            $ok = $this->__write(
                $this->__file['index'],
                ModelChunks::system('index', implode("\n", $this->__index))
            );
            $statistics['index-'.($ok? 'ok': 'ko')] = $this->__file['index'];
        }
        /* save models modify dates */
        $this->saveModelModifyDate();
        /* label translations */
        $contentsGettext = '';
        $contentsPo = '';
        foreach (array_unique($this->__keys) as $key) {
            $contentsGettext .= "    _('".addslashes($key)."'),\n";
            $contentsPo      .= "msgid \"$key\"\nmsgstr \"\"\n\n";
        }
        /* write to file */
        $ok = $this->__write(
            $this->__file['i18n'],
            ModelChunks::system('i18n', $contentsGettext)."\n\n# in pot file format\n".$contentsPo
        );
        $statistics['translate-'.($ok? 'ok': 'ko')] = $this->__file['i18n'];

        return $statistics;
    }

    private function __write($filepath, $contents) {
        if (@file_put_contents($filepath, $contents)) {
            return true;
        }
        throw new FileWriteError($filepath);
    }

    public function __toString() {
        $var = array(
            'group'  => $this->__group,
            'dir'    =>  $this->__modelsDir,
            'models' => $this->__modify,
        );

        return '' . Inspect::dumpText(array($var), false, false);
    }
}
