<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2012::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7717
 * Created  2012/08/10
 * Updated  2012/08/10
 */

//namespace arta;

/**
 * Model extra methods.
 *
 * @copyright  ::COPYRIGHT2012::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.2.1
 * @link       http://artaengine.com/api/ModelX
 * @example    http://artaengine.com/examples/models
 */
class ModelX {

    /**
     * Commits all open model SQL queues at once. This is the only way to execute more than one model object actions in
     * one single SQL transaction.
     *
     * @param string $modelGroup=null  Model group name (all open SQL queues related to this group will be committed)
     * @param bool   $transaction=true true=execute SQLs in a database transaction, false=execute all the SQLs but not in a transaction
     * @return bool State of success
     */
    static public function commit($modelGroup=null, $transaction=true) {
        $dbo = Database::get($modelGroup? Model::$modelDbo[$modelGroup]: true);
        $que = array();

        foreach ($dbo->__que__ as $key => $transactions) {
            if (substr($key, 0, 2) === 'mi') {
                $que = array_merge($que, $transactions);
                unset($dbo->__que__[$key]);
            }
        }

        $dbo->__transaction__ = 'miall';
        $dbo->__que__['miall'] = $que;

        return $dbo->commit($transaction);
    }

    /**
     * Cancels (rolls back) all open model SQL queues at once.
     *
     * @param string $modelGroup=null Model group name (all open SQL queues related to this group will be committed)
     */
    static public function cancel($modelGroup=null) {
        $dbo = Database::get($modelGroup? Model::$modelDbo[$modelGroup]: true);

        foreach ($dbo->__que__ as $key => $transactions) {
            if (substr($key, 0, 2) === 'mi') {
                $dbo->__transaction__ = $key;
                $dbo->rollback();
            }
        }
    }

    /**
     * Inspect model database object SQLs. Displays info.
     *
     * @param string $modelGroup Model group name
     */
    static public function inspect($modelGroup=null) {
        if (is_object($modelGroup)) {
            $modelGroup = $modelGroup->__group__;
        }
        $dbo = Database::get($modelGroup? Model::$modelDbo[$modelGroup]: true);
        $dbo->inspect();
    }

    /**
     * Backup model data as an SQL file with inserts. Currently does not backup locale data.
     *
     * @since Artaengine 2.1.0
     * @param IModel $model   the model which has been queries, to be dupmed
     * @param string $path    path to the file to write into, null = TEMP_DIR/models/backup/ModelClassName/yy-mm-dd.sql
     * @param array  $exclude an array of model properties to be excluded
     */
    static public function backupSql(IModel $model, $path=null, $exclude=array()) {
        $c = $model->count();
        if (!$c) {
            return;
        }

        if (!$path) {
            $path = TEMP_DIR . 'models/backup';
            @mkdir($path, 0777, true);
            $path .= '/' . get_class($model) . '-' . Arta::now('Y-m-d') . '.sql';
        }
        @unlink($path);
        $fp = fopen($path, 'w');

        $cols = array();
        foreach ($model->__def__[0] as $col => $def) {
            if (!in_array($col, $exclude)) {
                $cols[] = $col;
            }
        }
        foreach ($model->__def__[1] as $col => $def) {
            $cols[] = $def['fk'];
        }
        fwrite($fp, 'INSERT INTO ' . $model->__table__ . ' (' . implode(', ', $cols) . ") VALUES\n");

        $i = 1;
        $v = ',';
        $multi = array();
        while ($model->next()) {
            if ($i++ >= $c) {
                $v = ';';
            }
            $data = array();

            foreach ($model->__def__[0] as $col => $def) {
                if (!in_array($col, $exclude)) {
                    $data[] = $model->$col? ("'" . $model->__dbo__->esc($model->$col) . "'"): 'NULL';
                }
            }

            foreach ($model->__def__[1] as $col => $def) {
                if (!in_array($col, $exclude)) {
                    $pk = $model->$col->__key__;
                    $data[] = $model->$col->$pk? ("'" . $model->__dbo__->esc($model->$col->$pk) . "'"): 'NULL';
                }
            }

            foreach ($model->__def__[2] as $col => $def) {
                foreach ($model->$col as $obj) {
                    if (!in_array($col, $exclude)) {
                        $multi[] = 'INSERT INTO ' . $def['table'] .
                            ' (' . $def['obj']['fk'] . ', ' . $model->__fk__ . ") VALUES ('" .
                            $model->__dbo__->esc($obj->{$def['obj']['pk']}) . "', '" . $model->__dbo__->esc($model->{$model->__key__}) . "');";
                    }
                }
            }

            fwrite($fp, '(' . implode(', ', $data) . ")$v\n");
        }

        fwrite($fp, "\n" . implode("\n", $multi));
        fclose($fp);
    }

    public function __toString() {
        return '[arta\ModelX instance: extra model methods]';
    }
}
