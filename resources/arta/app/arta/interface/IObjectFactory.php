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
 * Artaengine object factory interface. In an Artaengine app the most efficient
 * and appropriate way to create/get-handle to objects which access a resource
 * such as a database connection, cache, template engine, etc. is to use the
 * object factory provided for it, this factories must implement this interface.
 * Each object is identified by a name(object-name) and can have configs. The
 * object name and configs can be set in the app config array or ini file.
 *
 * @author Mehdi Torabi <mehdi @ artaengine.com>
 */
interface IObjectFactory {

    /**
     * Set configs for a an object.
     *
     * @param string $objectName The object name to set configs for
     * @param array  $configs    Configs to be sets
     */
    static public function setConfig($objectName, array $configs);

    /**
     * Get configs for a an object. If no param is passed an array of all configs is returned.
     *
     * @param string $objectName=null The object name to get configs for, null=return all object configs
     * @param string $index=null      Config key, to get the value of a specific setting
     * @return array|string {configs} or {object-name: {configs},} or the value of a config key
     */
    static public function getConfig($objectName=null, $index=null);

    /**
     * Creates and returns or returns an already created object.
     *
     * @param string $objectName=null Object name to create or to get handle of, null=return the last object in the list,
     * @return object Reference to the object
     * @throws FactoryObjectCreation
     */
    static public function get($objectName=null);

    /**
     * Creates and returns an object.
     *
     * @param string $objectName The object name to be created
     * @param array  $configs    Database connection/object configs [@Database.configs]
     * @return object Reference to the newly created object
     */
    static public function getCreateNew($objectName, $configs);

    /**
     * Remove an object if already been created.
     *
     * @param string $objectName Object name to remove
     */
    static public function remove($objectName);
}
