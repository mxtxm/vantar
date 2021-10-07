<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7701
 * Created  2009/02/18
 * Updated  2013/03/05
 *
 * crawl: 0 dont crawl into this object | 1: crawl only one level | else: crawl
 */

//namespace arta;

/**
 * This class is for creating, deleting, editing and database synchronizing o
 * "IModel" classes and synchronizing.<br/>
 * This class is used by the application builder to synch database with models.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.3
 * @since      1.0.0
 * @link       http://artaengine.com/api/Models
 * @example    http://artaengine.com/examples/models
 */
abstract class ModelSynch {

    protected $__dbo;
    protected $__modelsDir;              // model files directory
    protected $__group;                  // directory which holds the models
    protected $__modify       = array(); // dict - date of last build [old, new]
    protected $__dictionary   = array(); // dict - model dictionary
    protected $__dependencies = array(); // models upper and lower dependencies
    protected $__drops;                  // drop all sql
    protected $__droped;                 // to avoid whats already dropped
    protected $__allowDrop;              // let drop db objects
    protected $__sqls;                   // temp sql collector
    protected $__keys;                   // temp translation key collector
    protected $__synchronized;           // after model is synched [model name: 1]
    protected $__index;                  // after model is synched [model name: 1]
    protected $__langWhere = array();
    protected $__throwErrors;

    protected function dropViews() {
        $this->__junctions = $this->__junctions = $this->__sqls = $this->__droped = array();
        foreach ($this->__dependencies as $modelName => $deps) {
            if (!isset($this->__sqls[$modelName])) {
                $this->createTableAndViewRemoveSqls($modelName, $deps, true);
            }
        }
        return $this->__sqls;
    }

    protected function createViews() {
        $this->__sqls = array('table' => array(), 'junction' => array());
        foreach ($this->__dependencies as $modelName => $deps) {
            if (!isset($this->__sqls[$modelName])) {
                $this->__createViews($deps);
            }
        }
        return $this->__sqls;
    }

    /**
     * VIEW >  SELECT cols for views - cares about exclusive col def depth
     *         maxDepth: null = crawl deep | 0 = no crawl | 1 = one level
     */
    private function getViewCols(&$cols, $mObj, $maxDepth=null, $params=array(
            'depth'    => 0,    // current depth
            'wasfirst' => false,// was last depth first depth?
            'start1'   => 'T.', // "start1 colname end1" AS "start2 colname end2"
            'start2'   => null, //
        )) {

        $depth    = 0;
        $wasfirst = false;
        $start1   = 'T.';
        $start2   = null;
        extract($params);
        $first    = $start1==='T.';
        $depth1   = $first? $maxDepth: $params['depth1'];
        /* self properties */
        foreach ($mObj->__def__[0] as $col => $def) {
            $cols[] = $start1.$col.' AS '.$start2.$col;
        }
        /* i18n */
        if ($mObj->__def__[3]) {
            $start1Lang = $first? 'L.': $start1;
            $cols[] = $start1Lang.'lang AS '.$start2.'lang';
            foreach ($mObj->__def__[3] as $col => $def) {
                $cols[] = $start1Lang.$col.' AS '.$start2.$col;
            }
        }
        /* objects */
        ++$depth;
        foreach ($mObj->__def__[1] as $col => $def) {
            $obj = $mObj->$col;
            /* self object pks */
            $cols[] = $start1.$def['fk'].' AS '.$start2.$def['fk'];
            /* crawl */
            if ($maxDepth === 0) {
                continue;
            }

            $maxDepthX = $maxDepth;
            $depthX    = $depth;
            if (isset($def['crawl'])) {
                if ($maxDepth === 1 && $depth1 === 1) {
                    continue;
                } elseif ($def['crawl'] === 0) {
                    continue;
                } elseif ($def['crawl'] === 1) {
                    $maxDepthX = $depthX = 0;
                }
            }
            if ($maxDepthX === null || $maxDepthX > $depthX-1) {
                $ci = $def['class_no'].($def['i']? 'i'.$def['i']: '').'_';
                $this->getViewCols(
                    $cols,
                    $obj,
                    $maxDepthX,
                    array(
                        'start1'   => $first? "v_$obj->__table__".
                                      ($maxDepthX===null? '_full': '')."$def[i].": $start1.($wasfirst? 'c': '').$ci,
                        'start2'   => ($first? 'c': $start2).$ci,
                        'depth'    => $depthX,
                        'wasfirst' => $first,
                        'depth1'   => $depth1, //function initial depth
                    )
                );
            }
        }
    }

    /**
     *  VIEW > Creates 3 level views for model ('', '_full', '_1')
     */
    private function __createViews($deps) {
        if (isset($deps['upper'])) {
            foreach ($deps['upper'] as $upperModelName) {
                if (!isset($this->__sqls[$upperModelName])) {
                    $this->__createViews($this->__dependencies[$upperModelName]);
                }
            }
        }
        /* * */
        $mObj      = $deps['object'];
        $modelName = get_class($mObj);
        $table     = $mObj->__table__;
        $tableLang = $table.'_lang';
        /* Create table abstract view - includes langs */
        $from      = '';
        $where     = '';
        // deleted col
        if (isset($mObj->__delete__) && !$mObj->__delete__) {
            $where           = " WHERE T.deleted IS NULL";
            $this->__index[] = 'CREATE INDEX ix_'.strtolower($table)."_DELETED ON $table (deleted);";
        }
        // lang join and add lang to select
        if ($mObj->__def__[3]) {
            $from = "INNER JOIN $tableLang L ON L.$mObj->__fk__ = T.$mObj->__key__";
        }
        // select cols
        $sels = array();
        $this->getViewCols($sels, $mObj, 0);
        // VIEW >
        $sql[] = "CREATE VIEW v_$table AS SELECT ".implode(', ', $sels)." FROM $table T $from$where;";
        /* Create full view data and relations
                - this models FK cols are not included */
        $from1 = $from; // for level 1
        foreach ($mObj->__def__[1] as $col => $def) {
            $obj       = $mObj->$col;
            $i         = $def['i'];
            $t         = $obj->__table__;
            /* view name to ve joined: v_tablename_full */
            $viewAs    = "v_{$t}_full$i";                         // full: use as SELECT viewC.col ...
            $viewJoin  = "v_{$t}_full".($i? " AS $viewAs": null); // full: use as FROM viewF AS viewFxxx
            $viewAs1   = "v_{$t}$i";                              // 1: use as SELECT viewC.col ...
            $viewJoin1 = "v_{$t}".($i? " AS $viewAs1": null);     // 1: use as FROM viewF AS viewFxxx
            /* levels */
            if (isset($def['crawl'])) {
                if ($def['crawl'] == 0) {
                    continue;
                } elseif ($def['crawl'] == 1) {
                    $viewJoin = $viewJoin1;
                    $viewAs = $viewAs1;
                }
            }
            /* join type */
            $joinType = (isset($def['nullable'])? $def['nullable']: (isset($def['required'])?
                !$def['required']: true))? 'LEFT OUTER': 'INNER';
            /* from */
            $from  .= " $joinType JOIN $viewJoin ON $viewAs.$obj->__key__ = T.$obj->__fk__$i";
            $from1 .= " $joinType JOIN $viewJoin1 ON $viewAs1.$obj->__key__ = T.$obj->__fk__$i";
        }
        // select cols
        $sels = array();
        $this->getViewCols($sels, $mObj);
        // VIEW >
        $viewFull = "AS SELECT ".implode(', ', $sels)." FROM $table T $from";
        $sql[]    = "CREATE VIEW v_{$table}_full $viewFull$where;";
        if ($where) {
            $sql[] = "CREATE VIEW v_{$table}_deleted $viewFull".str_replace('NULL', 'NOT NULL', $where).';';
        }
        // select cols
        $sels = array();
        $this->getViewCols($sels, $mObj, 1);
        // VIEW >
        $sql[] = "CREATE VIEW v_{$table}_1 AS SELECT ".implode(', ', $sels)." FROM $table T $from1$where;";
        /* * */
        $this->__sqls['table'][$modelName] = $sql;
        /* junction views */
        $this->createJunctionViews($mObj);
    }

    private function createJunctionViews($mObj) {
        $sqls = array();
        foreach ($mObj->__def__[2] as $col => $def) {
            if (class_exists($class=$def['class']) && ($rObj=new $class()) instanceof Model) {
                /* extra cols defined in junction class */
                $extraCols = array();
                if ($def['junction']) {
                    $jClass = new $def['junction'];
                    $extraCols = $jClass->properties();
                    foreach ($extraCols as &$eCol) {
                        $eCol = "J.$eCol AS j_$eCol";
                    }
                }
                /* view */
                $sqls[] = "CREATE VIEW $def[view] AS SELECT ".
                    implode(', ', array_merge(array($mObj->__fk__), $extraCols)).
                    ", R.* FROM $def[table] J INNER JOIN v_$rObj->__table__ R ".
                    "ON J.$rObj->__fk__ = R.$rObj->__key__";
            }
        }

        if ($sqls) {
            $this->__sqls['junction'][get_class($mObj)] = $sqls;
        }
    }

    private function extracti18n($mObj) {
        foreach ($mObj->__def__[0]+$mObj->__def__[3] as $col => $def) {
            $this->__keys[] = isset($def['label'])? $def['label']: $col;
            if (isset($def['options']) && is_array($def['options'])) {
                foreach ($def['options'] as $option) {
                    $this->__keys[] = is_array($option)? $option[0]: $option;
                }
            }
        }
        foreach ($mObj->__def__[1]+$mObj->__def__[2] as $col => $def) {
            $col = '__'.$col.'__';
            if (isset($mObj->$col)) {
                $def = $mObj->$col;
                $this->__keys[] = isset($def['label'])? $def['label']: get_class($mObj).'.'.str_replace('__', '', $col);
            }
        }
    }

    protected function synchDbWithModels($modelName) {
        /* if already build */
        if (isset($this->__synchronized[$modelName])) {
            return null;
        }
        /* this class or a parent of it has changed or not */
        $synchTable  = true;
        $modelModify = $this->__modify;

        if ($modelModify[$modelName][0] == $modelModify[$modelName][1]) {
            $synchTable = false;
            /* but if has a change model parent needs synch */
            $parent = $modelName;
            while ($parent=get_parent_class($parent)) {
                if (isset($modelModify[$parent]) && $modelModify[$parent][0] != $modelModify[$parent][1]) {
                    $synchTable = true;
                }
            }
        }
        /* make model object instance */
        //include_once "$this->__modelsDir$modelName.php";
        if (!class_exists($modelName)) {
            return false;
        }

        $mObj = new $modelName();

        /* dont synch if not a model or model constructor not called */
        if (!($mObj instanceof Model) || !isset($mObj->__instanceId__)) {
            return false;
        }

        $dbo = $mObj->__dbo__;

        /* extract translatable labels */
        $this->extracti18n($mObj);

        if (!is_object($dbo)) {
            throw new ModelSynchError("Data model class $modelName can not connect to database." .
                'Either the model is not created as conventions or model configs are wrong.');
        }

        if (!$dbo->exists($mObj->__table__)) {
            $synchTable = true;
        }
        /* * */
        if (!$synchTable) {
            return false;
        }
        /* init synch */
        $table  = $mObj->__table__;
        $engine = $dbo::DBMS === 'mysql'? (isset($mObj->__engine__)? $mObj->__engine__: 'InnoDB'): null;
        $cols   = $mObj->__def__[0];
        $i18n   = $mObj->__def__[3];
        $cons   = isset($mObj->__constraints__) && is_array($mObj->__constraints__)? $mObj->__constraints__: array();

        if (isset($mObj->__delete__) && !$mObj->__delete__) {
            $cols['deleted'] = array(
                'type'     => TIMESTAMP,
                'nullable' => true,
                'default'  => null,
            );
        }
        /* OBJECTS */
        foreach ($mObj->__def__[1] as $col => $def) {
            if (is_object($rObj=$mObj->$col) && $rObj instanceof Model) {
                $rObj->__fk__ = $def['fk'];
                $this->synchDbWithModels($def['class']);
                if ($refs=$this->getSynchedRefs($cols, $rObj, $def)) {
                    $cons[] = array(
                        'type'      => 'FOREIGN KEY',
                        'reference' => array(
                            'table' => $rObj->__table__,
                            'cols'  => $refs,
                        ),
                        'delete' => isset($def['delete']) ? $def['delete'] : 'CASCADE',
                    );
                }
            }
        }
        /* OBJECTS - multi */
        $junctionDef = array();
        foreach ($mObj->__def__[2] as $col => $def) {
            if (!isset($this->__synchronized[$keyDone="multi_$def[table]"]) && class_exists($class=$def['class'])) {
                $rObj = new $class();
                if ($rObj instanceof Model) {
                    /* multi object can be many to many (defined in both classes)
                       its enough to be synched only once (first class to be seen) */
                    $this->__synchronized[$keyDone] = 1;
                    /* findout refs and cols for junction table */
                    $mCols = array();
                    $mRefs = $this->getSynchedRefs($mCols, $mObj);
                    $this->synchDbWithModels($class);
                    $rCols = array();
                    $rRefs = $this->getSynchedRefs($rCols, $rObj);

                    if (!$rRefs || !$mRefs) {
                        continue;
                    }             

                    $rmCols = array_merge($rCols, $mCols);
                    /* extra cols defined in junction class */
                    $extraCols = array();
                    if ($def['junction']) {
                        $jClass = new $def['junction'];
                        $extraCols = $jClass->cols();
                    }
                    /* junction table def */
                    $junctionDef[$def['table']] = array(
                        'cols'   => array_merge($rmCols, $extraCols),
                        'cons'   => array(
                            array(
                                'type' => 'PRIMARY KEY',
                                'cols' => array_keys($rmCols),
                            ),
                            array(
                                'type'      => 'FOREIGN KEY',
                                'reference' => array(
                                    'table' => $table,
                                    'cols'  => $mRefs,
                                ),
                                'delete' => 'CASCADE',
                            ),
                            array(
                                'type'      => 'FOREIGN KEY',
                                'reference' => array(
                                    'table' => $rObj->__table__,
                                    'cols'  => $rRefs,
                                ),
                                'delete' => 'CASCADE',
                            ),
                        ),
                    );
                }
            }
        }
        /* force relation */
        foreach ($cols as $col => $def) {
            if (isset($def['relation'])) {
                $rObj = new $def['relation']();
                if ($rObj instanceof Model) {
                    $rObj->__fk__ = $col;
                    $this->synchDbWithModels($def['relation']);

                    if ($refs=$this->getSynchedRefs($cols, $rObj, $def)) {
                        $cons[] = array(
                            'type'      => 'FOREIGN KEY',
                            'reference' => array(
                                'table' => $rObj->__table__,
                                'cols'  => $refs,
                            ),
                            'delete' => isset($def['delete']) ? $def['delete'] : 'CASCADE',
                        );
                    }
                }
            }
        }
        /* synch model with database */
        $this->synchWithDb($table, $cols, $cons, $engine);
        $this->__synchronized[$modelName] = $modelName;
        /* i18n */
        if ($i18n) {
            $i18nKeys['lang'] = array(
                'type'     => STRING,
                'len'      => 5,
                'required' => true,
            );

            $mRefs = $this->getSynchedRefs(
                $i18nKeys,
                $mObj,
                array('nullable' => false)
            );

            $this->synchWithDb(
                $table.'_lang',
                array_merge($i18n, $i18nKeys),
                array(
                    array(
                        'type' => 'PRIMARY KEY',
                        'cols' => array_keys($i18nKeys),
                    ),
                    array(
                        'type'      => 'FOREIGN KEY',
                        'reference' => array(
                            'table' => $table,
                            'cols'  => $mRefs,
                        ),
                        'delete' => 'CASCADE',
                    ),
                ),
                $engine
            );
        }
        /* synch junctions with database */
        foreach ($junctionDef as $jTable => $def) {
            $this->synchWithDb(
                $jTable,
                $def['cols'],
                $def['cons'],
                $engine
           );
        }

        return null;
    }

    private function synchWithDb($table, $cols, $cons, $engine=null) {
        $dbo = $this->__dbo;

        if ($dbo->exists($table)) {
            $changes['add']    = array();
            $changes['drop']   = array();
            $changes['alter']  = array();
            $changes['rename'] = array();
            /* engine - mysql */
            if ($engine && $dbo::DBMS === 'mysql') {
                $st = $this->__dbo->getMetaTableStatus($table);
                if (strtolower($st['engine']) !== strtolower($engine)) {
                    $changes['engine'] = " ENGINE=$engine";
                }
            }
            /* fetch col constraints */
            foreach ($cons as $s => $t) {
                if ($t['type'] == 'PRIMARY KEY') {
                    foreach (is_array($t['cols'])? $t['cols']: array($t['cols']) as $v) {
                        if (isset($cols[$v])) {
                            $cols[$v]['nullable'] = false;
                            $cols[$v]['key'] = true;
                        }
                    }
                    unset($cons[$s]);
                }
            }

            $cons['pk'] = array(
                'type' => 'PRIMARY KEY',
                'cols' => array(),
            );

            $modelColsPKs = array();
            foreach ($cols as $k => $v) {
                if (isset($v['key']) && $v['key']) {
                    $cons['pk']['cols'][] = $k;
                    $modelColsPKs[$k] = $v;
                }

                if (isset($v['unique']) && $v['unique']) {
                    $cons[] = array(
                        'type' => 'UNIQUE',
                        'cols' => array($k),
                    );
                }

                if (isset($v['check'])) {
                    $cons[] = array(
                        'type'     => 'CHECK',
                        'exp'      => $v['check'],
                        'col_name' => $k,
                    );
                }

                unset(
                    $cols[$k]['check'],
                    $cols[$k]['unique'],
                    $cols[$k]['validate'],
                    $cols[$k]['widget']
                );
            }

            $meta     = $dbo->getMetaTable($table, 3);
            $metaCons = $meta['_constraints'];
            unset($meta['_constraints']);
            /* c o l u m n s */
            $this->synchedColAgainstMeta($changes, $cols, $meta);
            /* c o n s t r a i n t s */
            foreach ($metaCons as $metaK => $metaV) {
                switch ($metaV['type']) {

                    case 'PRIMARY KEY':
                        $pkChanged  = $colChanged = false;
                        $modelColsT = $modelColsPKs;

                        foreach ($metaV['cols'] as $mK => $mV) {
                            unset($modelColsT[$mK]);
                            $modelType  = null;
                            if (isset($modelColsPKs[$mK])) {
                                /* pk col exists in db and model - check for column specs
                                   no need to trigger constraint alter */
                                if (!isset($modelColsPKs[$mK]['type'])) {
                                    if (!$meta[$mK]['S']) {
                                        $colChanged = true;
                                        $modelType  = SERIAL;
                                    }
                                } else {
                                    $modelType     = $modelColsPKs[$mK]['type'];
                                    $modelUnsigned = $dbo::DBMS!='postgresql' && isset($modelColsPKs[$mK]['unsigned'])?
                                        $modelColsPKs[$mK]['unsigned']: false;
                                    // if model def is different thatn meta
                                    if ($modelType == SERIAL || $modelType == SERIAL_LONG) {

                                        if (  !$meta[$mK]['S'] 
                                           || ($modelType == SERIAL   && $meta[$mK]['type'] != INTEGER)
                                           || ($modelType == SERIAL_LONG && $meta[$mK]['type'] != LONG)
                                           || (isset($modelColsPKs[$mK]['len']) && $modelColsPKs[$mK]['len'] != $meta[$mK]['len'])
                                           || $modelUnsigned != $meta[$mK]['unsigned']) {

                                            $colChanged = true;
                                        }

                                    } elseif (   $meta[$mK]['type'] != $modelType
                                              || $modelUnsigned != $meta[$mK]['unsigned']
                                              || (   isset($modelColsPKs[$mK]['len'])
                                                  && $modelColsPKs[$mK]['len'] != $meta[$mK]['len'])) {

                                        $colChanged = true;
                                    }
                                }

                                if ($colChanged) {
                                    $cA = array('type' => $modelType);
                                    if (isset($modelColsPKs[$mK]['len'])) {
                                        $cA = $modelColsPKs[$mK]['len'];
                                    }
                                    if (isset($modelColsPKs[$mK]['unsigned'])) {
                                        $cA = $modelColsPKs[$mK]['unsigned'];
                                    }
                                    $changes['alter'][$mK] = $cA;
                                }
                            } else {
                                /* pk col exists in db but not in model
                                   - constraint must be deleted */
                                $pkChanged = true;
                                if ($this->__allowDrop) {
                                    $changes['drop'][$mK] = $mK;
                                }
                            }
                        }
                        /* in model not in db - constraint must be altered */
                        $this->synchedColAgainstMeta(
                            $changes,
                            $modelColsT,
                            $meta,
                            false
                        );
                        /* alter pk constraint */
                        if ($pkChanged || count($modelColsT) > 0) {
                            if ($this->__allowDrop) {
                                $changes['drop'][] = array(
                                    'subject' => 'CONSTRAINT',
                                    'name'    => $metaV['name'],
                                );
                            }
                            $changes['add'][] = array(
                                'subject' => 'CONSTRAINT',
                                'type'    => 'PRIMARY KEY',
                                'cols'    => array_keys($modelColsPKs),
                            );
                        }

                        unset($metaCons[$metaK]);
                        $modelColsPKs = null;
                        break;

                    case 'UNIQUE':
                        foreach ($cons as $conK => $conV) {
                            if ($conV['type'] == 'UNIQUE') {
                                $cC = (array)$conV['cols'];
                                $cM = $metaV['cols'];
                                if (count($cC) === count($cM) && count(array_diff($cC, $cM)) == 0) {
                                    unset($cons[$conK], $metaCons[$metaK]);
                                }
                            }
                        }
                        break;

                    case 'CHECK':
                        if ($dbo::DBMS == 'mysql') {
                            unset($metaCons[$metaK]);
                        }
                        break;

                    case 'FOREIGN KEY':
                        foreach ($cons as $conK => $conV) {
                            if ($conV['type'] == 'FOREIGN KEY') {
                                $cC = $conV['reference']['cols'];
                                $cM = $metaV['reference']['cols'];
                                $deleteC = isset($conV['delete'])?  strtoupper($conV['delete']):  'CASCADE';
                                $deleteM = isset($metaV['delete'])? strtoupper($metaV['delete']): '';
                                if (   count($cC) == count($cM)
                                    && $conV['reference']['table'] == $metaV['reference']['table']
                                    && count(array_diff_assoc($cC, $cM)) == 0
                                    && $deleteC == $deleteM) {

                                    unset($cons[$conK], $metaCons[$metaK]);
                                }
                            }
                        }
                }
            }

            /* in model not in db - new constraints */

            /* PK */
            if ($modelColsPKs !== null) {
                $this->synchedColAgainstMeta(
                    $changes,
                    $modelColsPKs,
                    $meta,
                    false
                );
                $changes['add'][] = array(
                    'subject' => 'CONSTRAINT',
                    'type'    => 'PRIMARY KEY',
                    'cols'    => $cons['pk']['cols'],
                );
            }

            foreach ($cons as $conK => $conV) {
                /* UN */
                if ($conV['type'] == 'UNIQUE') {
                    $changes['add'][] = array(
                        'subject' => 'CONSTRAINT',
                        'type'    => 'UNIQUE',
                        'cols'    => (array)$conV['cols'],
                    );
                }
                /* FK */
                elseif ($conV['type'] == 'FOREIGN KEY') {
                    $changes['add'][] = array(
                        'subject'   => 'CONSTRAINT',
                        'type'      => 'FOREIGN KEY',
                        'reference' => $conV['reference'],
                        'delete'    => isset($conV['delete'])? $conV['delete']: 'CASCADE',
                    );
                }
                /* CK */
                elseif ($conV['type'] === 'CHECK' && $dbo::DBMS != 'mysql') {
                    $conV['exp'] = isset($conV['exp'])? $conV['exp']: (isset($conV['definition'])? $conV['definition']: $conV);

                    if ($checkCon=$dbo->checkConstraint($conV)) {
                        list($name, $exp) = $checkCon;
                        $name = $dbo->constraintName('ck', $table, $name);

                        if (isset($metaCons[$name])) {
                            unset($metaCons[$name]);
                        } elseif ($exp) {
                            $changes['add'][] = array(
                                'subject'  => 'CONSTRAINT',
                                'type'     => 'CHECK',
                                'exp'      => $exp,
                                'name'     => $name,
                                'col_name' => $conV['col_name'],
                            );
                        }
                    }
                }
            }

            /* in db not in model - old constraints */
            foreach ($metaCons as $metaK => $metaV) {
                if ($this->__allowDrop && $metaV['cols']) {
                    $changes['drop'][] = array(
                        'subject' => 'CONSTRAINT',
                        'name'    => $metaV['name'],
                    );
                }
            }

            if ($changes['add'] || $changes['drop'] || $changes['alter'] || $changes['rename'] || isset($changes['engine'])) {
                $this->__sqls[$table] = $dbo->alterTable(
                    $table,
                    $changes,
                    null,
                    null,
                    false
                );
            }
        } else {
            $this->__sqls[$table]['c'] = $dbo->createTable(
                $table,
                $cols,
                $cons,
                $engine? " ENGINE=$engine": null,
                false
            );
        }
    }

    /**
     * SYNCH > compare model and db and fetch changes (not FK, PK, UN, CK constraints)
     */
    private function synchedColAgainstMeta(&$changes, $cols, $meta, $normal=true) {
        $dbo     = $this->__dbo;
        $changed = false;

        foreach ($meta as $col => $defMeta) {
        /* in model and in database */
            if (isset($cols[$col])) {
                $defModel = $cols[$col];
                if (isset($defModel['len']) && is_array($defModel['len'])) {
                    $defModel['len'] = $defModel['len'][1];
                }
                $alter = array();
                // PK
                if (isset($defModel['key']) && $defModel['key']) {
                    if ($normal) {
                        continue;
                    }
                    $defModel['nullable'] = false;
                    if (!isset($defModel['type'])) {
                        $defModel['type'] = SERIAL;
                        unset($defModel['len']);
                    }
                }
                // rename col 
                if (isset($defModel['rename'])) {
                    $changes['rename'][$col] = $defModel['rename'];
                    $changed = true;
                }
                // nullable/required
                $nullable = isset($defModel['nullable'])?
                    $defModel['nullable']:
                    (isset($defModel['required'])? !$defModel['required']: true);

                if ($defMeta['nullable'] !== $nullable) {
                    $alter['nullable'] = $nullable;
                }
                // default
                if (isset($defModel['default'])) {
                    // now workaround >
                    $dMeta   = $defMeta['default'];
                    $dMetaL  = strtolower($dMeta);
                    $dModel  = $defModel['default'];
                    $dModelL = strtolower($dModel);

                    if ($dMetaL === 'current_timestamp' || $dMetaL === 'now' || $dMetaL === 'now()') {
                        $dMeta = 'NOW()';
                    }
                    if ($dModelL === 'current_timestamp' || $dModelL === 'now' || $dMetaL  === 'now()') {
                        $dModel = 'NOW()';
                    }
                    // now workaround <
                    if ((string)$dModel !== (string)$dMeta) {
                        $alter['default'] = $dModel;
                    }

                } elseif ($defMeta['has_default']) {
                    $alter['default'] = 'drop';
                }
                // type prepare
                if (isset($defModel['type'])) {
                    $type = $dbo->realType($defModel['type']);
                    if (is_array($type)) {
                        $defModel['type'] = $type[0];
                        if (!isset($defModel['len'])) {
                            $defModel['len'] = (int)$type[1];
                        }
                    } else {
                        $defModel['type'] = $type;
                    }

                } else {
                    $defModel['type'] = STRING;
                    if (!isset($defModel['len'])) {
                        $defModel['len'] = 255;
                    }
                }
                // type > type
                if (isset($defModel['type']) && $defModel['type'] !== $defMeta['type']) {
                    $alter['type'] = $defModel['type'];
                    //$alter['len']      = $defMeta['len'];
                    //$alter['unsigned'] = $defMeta['unsigned'];
                }
                // type > len
                if (isset($defModel['len']) && (int)$defModel['len'] !== (int)$defMeta['len']) {
                    if (!isset($alter['type'])) {
                        $alter['type'] = $defMeta['type'];
                    }
                    $alter['len']      = (int)$defModel['len'];
                    $alter['unsigned'] = $defMeta['unsigned'];
                }
                // type > unsigned
                if (!isset($defModel['unsigned'])) {
                    $defModel['unsigned'] = false;
                }

                if ($dbo::DBMS !== 'postgresql' && $defModel['unsigned'] !== $defMeta['unsigned']) {
                    $alter['unsigned'] = $defModel['unsigned'];

                    if (!isset($alter['type'])) {
                        $alter['type'] = $defMeta['type'];
                    }
                    if (!isset($defModel['len'])) {
                        $alter['len']  = (int)$defMeta['len'];
                    }
                }
                // changes
                if ($alter) {
                    $changed = true;
                    $changes['alter'][$col] = $alter;
                }
            }
        /* in db not in model- deleted from db */
            elseif ($normal && $this->__allowDrop) {
                $changed = true;
                $changes['drop'][$col] = $col;
            }
        }

        /* in model and not in database */
        foreach ($cols as $col => $def) {
            if (!isset($meta[strtolower($col)]) && (!$normal || !isset($def['key']) || !$def['key'])) {
                $changed              = true;
                $changes['add'][$col] = $def;
            }
        }

        return $changed;
    }

    /**
     * SYNCH > prepare fk col and ref to model objects db table
     * model base model object
     * override col def 
     */
    private function getSynchedRefs(&$cols, $model, $override=null) {
        $output = array();
        if ($pkCol=$model->__key__) {
            /* start col def with refed pk def */
            $def = $model->__def__[0][$pkCol];
            unset($def['key'], $def['unique'], $def['required'], $def['nullable']);
            /* type */
            $type = isset($def['type'])? $def['type']: false;

            $def['unsigned'] = isset($def['unsigned'])? $def['unsigned']: !$type || $type==SERIAL || $type==SERIAL_LONG;

            $def['type'] = $type? ($type == SERIAL? INTEGER: ($type == SERIAL_LONG? LONG: $type)): INTEGER;
            /* override - extra options */
            if ($override) {
                $def = array_merge($def, $override);
            }
            // unify required and nullable
            $def['nullable'] = isset($def['nullable'])? $def['nullable']: (isset($def['required'])? !$def['required']: true);
            unset($def['required']);
            /* col def and references */
            $cols  [$model->__fk__] = $def;
            $output[$model->__fk__] = $pkCol;
        }
        return $output;
    }
}