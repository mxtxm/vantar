<?php
/**
 * Artaengine project http://artaengine.com/
 * ::COPYRIGHT2009::
 * 
 * ::LICENCE::
 * ::LICENCE-URL::
 *
 * ClassID  7200
 * Created  2007/09/18
 * Updated  2013/02/05
 */

//namespace arta;

require_once 'interface/IObjectFactory.php';

/**
 * Database access object factory. Normally the database configs and object names
 * are defined in the app config array or ini file, "$object-name = Database::get('object-name');"
 * will return the database access object on demand. A database connection and
 * object creation is done on the first demand, the object and connection will
 * remain alive for the next demands.
 *
 * @copyright  ::COPYRIGHT2009::
 * @author     Mehdi Torabi <mehdi @ artaengine.com>
 * @version    1.0.2
 * @since      1.0.0
 * @link       http://artaengine.com/api/Database
 * @example    http://artaengine.com/examples/database
 */
class Database implements IObjectFactory {

    private static $dbos = array();


    /**
     * Set configs for a database access object. The configs are stored in "Arta::$dbConfigs" which also contains the
     * app database object configs (from the app config ini file or array).
     *
     * @param string $objectName=null The object name to set configs for, null=return all object configs
     * @param array  $configs=null    To add a new database connection/object configs [@Database.configs]
     * @return array|string {configs} or {object-name: {configs},} or the value of a config key
     */
    static public function setConfig($objectName, array $configs) {
        Arta::$dbConfigs[$objectName] = $configs;
    }

    /**
     * Get configs for a database access object. The configs are stored in "Arta::$dbConfigs" which also contains the
     * app database object configs (from the app config ini file or array).
     *
     * @param string $objectName=null The object name to get configs for, null=return all object configs
     * @param string $index=null      Config key, to get the value of a specific setting
     * @return array|string {configs} or {object-name: {configs},} or the value of a config key
     */
    static public function getConfig($objectName=null, $index=null) {
        return
            $objectName?
                (isset(Arta::$dbConfigs[$objectName])?
                    ($index?
                        (isset(Arta::$dbConfigs[$objectName][$index])?
                            Arta::$dbConfigs[$objectName][$index]:
                            null):
                        Arta::$dbConfigs[$objectName]):
                    null):
                Arta::$dbConfigs;
    }

    /**
     * Creates and returns or returns an already created object. When creating a new
     * object the connection/object configs are expected to exists in "Arta::$dbConfigs",
     * note that the database connection/objects defined in the app config array or ini
     * file are put in "Arta::$dbConfigs" by Artaengine.<br/>
     * Get a database access object by passing the object name.<br/>
     *
     * @param string $objectName=null Object name to create or to get handle of, null=return the last object in the list
     * @return IDbAbstract|IDbAdvanced Reference to a database access object
     * @throws DbmsNotSupported
     * @throws FactoryObjectCreation
     * @see http://artaengine.com/tutorials/database#connecting
     */
    static public function get($objectName=null) {
        if (!$objectName) {
            reset(Arta::$dbConfigs);
            $objectName = key(Arta::$dbConfigs);
        }

        if (isset(self::$dbos[$objectName])) {
            return self::$dbos[$objectName];
        }

        if (isset(Arta::$dbConfigs[$objectName])) {
            $configs = Arta::$dbConfigs[$objectName];
            switch (strtolower($configs['engine'])) {

                case 'postgres':
                case 'postgresql':
                    $engine = 'PostgreSql';
                    break;

                case 'mysql':
                case 'mysqli':
                    $engine = 'MySqli';
                    break;

                //case 'mssql':
                //case 'sqlserver':
                //case 'mssqlserver':
                //    $engine = 'MsSql';
                //    break;

                default:
                    throw new DbmsNotSupported($configs['engine']);
            }

            $class = $engine . (isset($configs['advanced']) && $configs['advanced']? 'Advanced': 'Abstract');

            require_once ARTA_DIR . "/db/engine/$class.php";
            Arta::$dbConfigs[$objectName]['name'] = $objectName;
            self::$dbos[$objectName] = new $class($configs);

            return self::$dbos[$objectName];
        }

        throw new FactoryObjectCreation("Could not create $objectName because connection settings for this object name does not exist.");
    }

    /**
     * Get a dictionary of all objects created till now.
     *
     * @return array The database object pool {object-name: IDbAbstract/IDbAdvanced}
     */
    static public function getAll() {
        return self::$dbos;
    }

    /**
     * Creates and returns an object. Use this method to create a new object by passing the connection/object configs versus
     * "get()" that expects the configs to already exist in "Arta::$dbConfigs".
     *
     * @param string $objectName The object name to be created
     * @param array  $configs    Database connection/object configs [@Database.configs]
     * @return IDbAbstract|IDbAdvanced Reference to the newly created database access object
     * @throws DbmsNotSupported
     */
    static public function getCreateNew($objectName, $configs) {
        Arta::$dbConfigs[$objectName] = $configs;
        return self::get($objectName);
    }

    /**
     * Remove an object if already been created.
     *
     * @param string $objectName Object name to remove
     */
    static public function remove($objectName) {
        unset(self::$dbos[$objectName]);
    }

    /**
     * Check if it is possible to create an object with the given name (if configs have been set).
     *
     * @param string $objectName Object name to remove
     * @return bool true=it is creatable
     */
    static public function creatable($objectName) {
        return isset(Arta::$dbConfigs[$objectName]);
    }

    /**
     * Converts an abstract database access object into an advanced database access object and returns it.
     * An advanced (IDbAdvanced) database access object is an extend to an abstract database access object
     * (IDbAbstract) which adds methods for extra actions such as create and alter table.
     * 
     * @param IDbAbstract|string $objectName='dbo' Name of the database access object or the object itself
     * @return IDbAdvanced Database advanced object
     * @see http://artaengine.com/tutorials/database#advanced
     * @throws FactoryObjectCreation
     */
    static public function upgrade($objectName='dbo') {
        if ($objectName instanceof IDbAbstract) {
            $obj = $objectName;
            $objectName = isset($configs['name'])? $configs['name']: 'dbo';
        } else {
            $obj = self::get($objectName);
        }

        $configs = $obj->connection();

        self::remove($objectName);
        $configs['advanced'] = true;

        return self::getCreateNew($objectName, $configs);
    }

    public function __toString() {
        $var = array(
            'database-objects' => self::$dbos,
            'configs' =>  Arta::$dbConfigs,
        );

        return '' . Inspect::dumpText(array($var), false, false);
    }
}
