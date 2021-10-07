<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 */

//namespace arta;

/**
 * Interface for the Artaengine utilities class. This interface is to insures the
 * class will always be backward compatible.
 *
 * @author Mehdi Torabi <mehdi @ artaengine.com>
 */
interface IUtils {

    /**
     * Pluralize an English word.
     *
     * @param  string word Word to be pluralized
     * @return string Pluralized word
     */
    static public function pluralize($word);

    /**
     * Is an array a list or a dictionary, list means keys start from 0 and are
     * numeric and sequential.
     *
     * @since Artaengine 1.1.0
     * @param  array array Array to be checked
     * @return bool true=is list
     */
    static public function isList($array);

    /**
     * Flatten an array into a string for display purposes.
     *
     * @param  array array        Array to render
     * @param  array configs=null Settings [@Utils.arraytostring]
     * @param  bool  first=true   For internal use
     * @return string The flattened array
     */
    static public function array2string($array, $configs=null, $first=true);

    /**
     * Convert "T::TYPE_CONST" (Artaengine data type) string to const. This ambiguous
     * is used for editing PHP model files.
     *
     * @param  string type=null "T::TYPE_CONST" (Artaengine data type) string
     * @return const "T::TYPE_CONST" (Artaengine data type)
     */
    static public function str2typ($type=null);

    /**
     * Convert "T::TYPE_CONST" (Artaengine data type) const to string. This ambiguous
     * is used for editing PHP model files.
     *
     * @param  string type=null "T::TYPE_CONST" (Artaengine data type) string
     * @return string "T::TYPE_CONST" (Artaengine data type) as string
     */
    static public function typ2str($type=null, $direction=true);

    /**
     * If a model data property has options, this function will replace the option
     * key which is set to the property with the option's value. This is to make
     * all property values of a model human readable (but not actionable).
     *
     * @param  array|string &data Reference to model property data to render
     * @param  array        def   Model property definition
     * @return void
     */
    static public function mapModelPropertyOptions(&$data, $def);

    /**
     * Traverse/crawl a path of objects in the model and returns the last node.
     *
     * @since Artaengine 1.2.2 
     * @param  IModel model Model object to start the traverse from
     * @param  string path  Path showing model objects, last node may be a method
     * @return IModel Last model object found
     */
    static public function getModelPath($model, $path);

    /**
     * Traverse/crawl a path of objects in the model and return the data found on the last node.
     *
     * @param  IModel model Model object to start the traverse from
     * @param  string path  Path showing model objects, last node may be a method last node may be a method
     * @param  bool   render=true true=apply "Render::list2Html()" on result
     * @param  array  &def=array  Will be set to last Model object definition
     * @return mixed Whatever is found on the last node
     */
    static public function getModelPathData($model, $path, $render=true, &$def=array());

    /**
     * gettext an array.
     *
     * @param  &aReference to the array
     * @return array Translated
     */
    static public function translateArray(&$a);

    /**
     * Get class static method or static property value by string.
     *
     * @since Artaengine 1.2.2
     * @param string source "Class::method" or "Class::property"
     * @return mixed The returned value of "Class::method()" or "Class::property"
     */
    static public function getCallStaticMethod($source);

    /**
     * Get browser dictionary (front-end resource dictionary).
     *
     * @since Artaengine 1.2.2
     * @return array [{js dic}, {css dic}, {template dic}]
     */
    static public function getBrowserDictionary();
}
