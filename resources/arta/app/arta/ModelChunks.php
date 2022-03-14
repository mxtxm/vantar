<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2012::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7715
 * Created  2012/03/21
 * Updated  2012/03/21
 */

//namespace arta;

/**
 * Chunks of code used by the Models class used whrn creating IModel classes.
 *
 * @copyright  ::COPYRIGHT2012::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.0
 * @since      1.1.0
 * @link       http://artaengine.com/api/Models
 * @example    http://artaengine.com/examples/models
 */
class ModelChunks {

    /**
     * Constants used in IModel property definitions.
     */
    static public $constants = array(
        'Widget::DROPDOWN',
        'Widget::MULTI_CHECKBOX',
        'Widget::MULTI_OPTION',
        'Widget::MULTI_SELECT',
        'Widget::DATE_PICKER',
        'Widget::DROPDOWN_TIME',
        'Widget::DATE_TIME',
        'Widget::AUTO_COMPLETE',
        'Widget::CK',
        'Widget::COMBO',
        'Form::INPUT',
        'Form::TEXT',
        'Form::FILE',
        'Form::CHECKBOX',
        'Form::RADIO',
        'Form::IMG',
        'Form::BUTTON',
        'Form::SUBMIT',
        'Form::RESET',
        'Form::IMAGE',
        'Form::LABEL',
        'Form::PASSWORD',
        'Form::HIDDEN',
        'Form::DIV',
        'Form::ANCHOR',
        'Form::IBUTTON',
    );

    /**
     * Method body, used for creating/updating models. Constructor.
     *
     * @return string Method definition code
     */
    static public function construct($group, $title=null) {
        return
            "    /**\n".
            "     * IModel constructor\n".
            "     */\n".
            '    public function __construct($params=null)'."\n".
            "    {\n".
            '        parent::__construct(\''.$group.'\', $params);'."\n".($title?
            '        $this->__title__'." = _('$title');\n": '').
            "    }";
    }

    /**
     * Method body, used for creating/updating models. Before add event.
     *
     * @return string Method definition code
     */
    static public function beforeAdd() {
        return
            "    /**\n".
            "     * Event: before adding model data\n".
            "     *\n".
            "     * @return bool false=do not proceed with adding the data into the database\n".
            "     */\n".
            "    public function beforeAdd()\n".
            "    {\n\n".
            "        return true;\n".
            "    }";
    }

    /**
     * Method body, used for creating/updating models. After add event.
     *
     * @return string Method definition code
     */
    static public function afterAdd() {
        return
            "    /**\n".
            "     * Event: after adding model data\n".
            "     *\n".
            "     * @param boolean ok   state of success\n".
            "     * @param array   data an array of the data been added {property-name: value,}\n".
            "     * @return void\n".
            "     */\n".
            '    public function afterAdd($ok, $data)'."\n".
            "    {\n\n".
            "    }";
    }

    /**
     * Method body, used for creating/updating models. Before update event.
     *
     * @return string Method definition code
     */
    static public function beforeUpdate() {
        return
            "    /**\n".
            "     * Event: before updating model data\n".
            "     *\n".
            "     * @return bool false=do not proceed with updating the data in the database\n".
            "     */\n".
            "    public function beforeUpdate()\n".
            "    {\n".
            "        /* if this method returns boolean false model data will not be updated */\n".
            "        return true;\n".
            "    }";
    }

    /**
     * Method body, used for creating/updating models. After update event.
     *
     * @return string Method definition code
     */
    static public function afterUpdate() {
        return
            "    /**\n".
            "     * Event: after updating model data\n".
            "     *\n".
            "     * @param boolean ok   state of success\n".
            "     * @param array   data an array of the data been updated {property-name: value,}\n".
            "     * @return void\n".
            "     */\n".
            '    public function afterUpdate($ok, $data)'."\n".
            "    {\n\n".
            "    }";
    }

    /**
     * Method body, used for creating/updating models. Before delete event.
     *
     * @return string Method definition code
     */
    static public function beforeDelete() {
        return
            "    /**\n".
            "     * Event: before removing model data\n".
            "     *\n".
            "     * @return bool|array false=do not proceed with deleting database data, array={property-name: value,} update this cols if delete is logical\n".
            "     */\n".
            "    public function beforeDelete()\n".
            "    {\n".
            "        /* if this method returns boolean false model data will not be added */\n".
            "        /* to change some property values on logical delete return {propery-name: value,} */\n".
            "        return true;\n".
            "    }";
    }

    /**
     * Method body, used for creating/updating models. After delete event
     *
     * @return string Method definition code
     */
    static public function afterDelete() {
        return
            "    /**\n".
            "     * Event: after removing model data\n".
            "     *\n".
            '     * @param boolean ok   state of success'."\n".
            "     * @param array   data an array of the data been deleted {property-name: value,}\n".
            "     * @return void\n".
            "     */\n".
            '    public function afterDelete($ok, $data)'."\n".
            "    {\n\n".
            "    }";
    }

    /**
     * Method body, used for creating/updating models. Before query event.
     *
     * @return string Method definition code
     */
    static public function beforeQuery() {
        return
            "    /**\n".
            "     * Event: before querying model data\n".
            "     *\n".
            "     * @return bool false=do not proceed querying data\n".
            "     */\n".
            "    public function beforeQuery()\n".
            "    {\n".
            "        /* if this method returns boolean false querying will be canceled */\n".
            "        return true;\n".
            "    }";
    }

    /**
     * Method body, used for creating/updating models. After query event.
     *
     * @return string Method definition code
     */
    static public function afterQuery() {
        return
            "    /**\n".
            "     * Event: after querying model data\n".
            "     *\n".
            "     * @return void\n".
            "     */\n".
            "    public function afterQuery()\n".
            "    {\n\n".
            "    }";
    }

    /**
     * Method body, used for creating/updating models. After next event.
     *
     * @return string Method definition code
     */
    static public function afterNext() {
        return
            "    /**\n".
            "     * Event: after filling model properties with a row od data\n".
            "     *\n".
            "     * @return void\n".
            "     */\n".
            "    public function afterNext()\n".
            "    {\n\n".
            "    }";
    }

    /**
     * Code templates used by Artaengine builder to create cache files.
     *
     * @return string Code
     */
    static public function system($name, $contents) {
        $time   = Arta::now('Y/m/d H:i');
        $create = "Created by    Arta.Models  $time";

        switch ($name) {
            case 'index':
                return
                    "--- Indexes on logical delete column\n".
                    "--- If you think this indexes can optimize your database performance\n".
                    "--- execute this SQLs\n".
                    "--- $create\n\n".$contents;

            case 'synch':
                return
                    "--- SQLs executed on database to synch database with models\n".
                    "--- $create\n\n".$contents;

            case 'cleandb':
                return
                    "--- SQLs to drop all database relations created by Artaengine\n".
                    "--- $create\n\n".$contents;

            case 'i18n':
                return
                    "<?php\n".
                    "// Models translation keys - created on buildup or Model synch\n".
                    "// The keys in this file can be detected by poedit\n".
                    "// $create\n\n".
                    '$translations = array('."\n$contents);";

            case 'dependencies':
                return
                    "<?php\n".
                    "// Models dependency graph - created on buildup or model synch\n".
                    "// $create\n\n".
                    '$modelsDependencies = array('."\n$contents);";

            case 'modify':
                return
                    "<?php\n".
                    "// Models last modify time cache - created on buildup or model synch\n".
                    "// $create\n\n".
                    '$modelsLastModify = array('."\n$contents);";

            case 'dictionary':
                return
                    "<?php\n".
                    "// Models dictionary - created on buildup or model synch\n".
                    "// $create\n".
                    "// * deleting this file can result model ID's to change\n\n".
                    $contents;

            case 'include':
                return
                    "<?php\n".
                    "// Models include - created on buildup or model synch\n".
                    "// $create\n\n".
                    $contents;

            case 'translation':
                return
                    "<?php\n".
                    "// Models Class to SQL translation cache - created on buildup or model synch\n".
                    "// $create\n\n".
                    $contents;
        }
        return null;
    }

    public function __toString() {
        return '[arta\ModelChunks instance]';
    }
}
