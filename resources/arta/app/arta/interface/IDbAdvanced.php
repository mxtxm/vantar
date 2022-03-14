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
 * Interface for database engines. To make a database usable in Artaengine two
 * classes must be created, one must implement IDbAbstract and one IDbAdvanced. 
 *
 * @author Mehdi Torabi <mehdi @ artaengine.com>
 */
interface IDbAdvanced extends IDbAbstract{

    /**
     * Get info about the dbms and class.
     *
     * @return array Info array [@PostgreSql.advanced.info.return]
     * @since Artaengine 1.1.0
     */
    public function info();

    /**
     * Get a dictionary of database data types mapped to Artaengine data types.
     *
     * @return array {db-type-string: T::TYPE_CONST,}
     */
    public function dbType2ArtaType();

    /**
     * Get a dictionary of Artaengine data types mapped to database data types.
     *
     * @return array {T::TYPE_CONST: db-type-string,}
     */
    public function artaType2DbType($pos=false);

    /**
     * Get the database data type for virtual data types such as PASSWORD/EMAIL, etc.
     *
     * @param  const type T::TYPE_CONST (An Artaengine data type)
     * @return const|array Artaengine data type | [artaengine-data-type, len]
     */
    public function realType($type);

    /**
     * Returns the database type definition of a T::TYPE_CONST (Artaengine data type).
     *
     * @param  const      type            T::TYPE_CONST (Artaengine data type)
     * @param  int|array  length=null     Max length or [min-length, max-length]
     * @param  bool       unsigned=false  Signed or Unsigned (if supported by database)
     * @return string     Database column type definition
     */
    public function toType($type, $length=null, $unsigned=false);

    /**
     * Make a valid database constraint name.
     *
     * @param  string prefix Such as 'PK', 'FK', 'CK', etc.
     * @param  string table  Table name
     * @param  string name   Constraint main name
     * @return string Constraint name
     */
    public function constraintName($prefix, $table, $name);

    /**
     * Create table.
     *
     * @param  string       table            Table name
     * @param  array        cols=null        Column definitions [@DbAdvanced.createTable.cols]
     * @param  array        constraints=null Constraint definitions [@DbAdvanced.createTable.cons]
     * @param  string|array extra=null       Extra DBMS specific table parameters
     * @param  bool         exe=true         true=execute the CREATE TABLE on database
     * @return string CREATE TABLE SQL
     * @throws DatabaseError
     */
    public function createTable($table, array $cols=null, array $constraints=null, $extra=null, $exe=true);

    /**
     * Alter table.
     *
     * @param  string       table            Table name
     * @param  array        cols=null        Column definitions [@DbAdvanced.alterTable.cols]
     * @param  array        constraints=null Constraint definitions [@DbAdvanced.alterTable.cons]
     * @param  string|array extra=null       Extra DBMS specific table parameters
     * @param  bool         exe=true         true=execute the ALTER TABLE on database
     * @return string ALTER TABLE SQL
     * @throws DatabaseError
     */
    public function alterTable($table, array $cols=null, array $constraints=null, $extra=null, $exe=true);
    /**
     * Get table constraints meta data.
     *
     * @param  string table=null      Table name, null=get all constraints that exist in the database
     * @param  string constraint=null Constraint name to get info about, null=get info about all constraints
     * @return array  table=null then [{def},] or table!=null then {constraint-name: {def},} if constraint!=null then {def} def=
     *         [@DbAdvanced.getMetaConstraints]
     */
    public function getMetaConstraints($table=null, $constraint=null);

    /**
     * Get table primary keys.
     *
     * @param  string table Table name
     * @return array Array of primary key column names {pk-col-name: pk-col-name,}
     */
    public function getTablePrimaryKeys($table);
    /**
     * Get metadata of tables.
     *
     * @param  string table=null Table name, null=return a list of database tables
     * @param  int    shape=2 1=return column definitions only, 2=return column definitions
     *                and constrain statuses, 3=return column definitions and constraint definitions
     * @return array if table=null {table-name: table-name,} else {col-name: {meta},} where meta is: [DbAdvanced.getMetaTable]
     */
    public function getMetaTable($table=null, $shape=2);

    /**
     * Get database view list or view(s) meta data.
     *
     * @param  string view=null  View name, null=return for all views
     * @param  bool   thin=false true=return only a list of view names
     * @return array if thin=true return {view-name: view-name,} if view name is specified return
     *         {meta} if view name is not specified return {view-name: {meta},} meta= [@DbAdvanced.getMetaViews]
     */
    public function getMetaViews($view=null, $thin=false);

    /**
     * Get table indexes meta data (Experimental - subject to change).
     *
     * @param  string table table name, null=return all
     * @return array array {index-name: {meta},} meta= [@DbAdvanced.getMetaIndexes]
     */
    public function getMetaIndexes($table=null);

    /**
     * Get information about a table. The result is DBMS specific.
     *
     * @param  string table=null Table name
     * @return array [@PostgeSql.getMetaTableStatus]
     */
    public function getMetaTableStatus($table=null);

    /**
     * Get truncate/delete SQL string for all database tables.
     *
     * @param  bool delete=false bool false=TRUNCATE TABLE, true=DELETE FROM
     * @return string TRUNCATE/DELETE SQL
     */
    public function truncate($delete=false);
}
