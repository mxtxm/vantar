package com.vantar.database.sql;

import org.aeonbits.owner.Config;


public interface SqlConfig {

    @Config.DefaultValue("10")
    @Config.Key("sql.pool.initial.size")
    int getPoolInitialSize();

    @Config.DefaultValue("100")
    @Config.Key("sql.pool.max.active")
    int getPoolMaxActive();

    @Config.DefaultValue("30")
    @Config.Key("sql.pool.min.idle")
    int getPoolMinIdle();

    @Config.DefaultValue("80")
    @Config.Key("sql.pool.max.idle")
    int getPoolMaxIdle();

    @Config.DefaultValue("3400")
    @Config.Key("sql.pool.time.between.eviction.runs.millis")
    int getPoolTimeBetweenEvictionRunsMillis();

    @Config.DefaultValue("5400")
    @Config.Key("sql.pool.min.evictable.idle.time.millis")
    int getPoolMinEvictableIdleTimeMillis();

    @Config.DefaultValue("3400")
    @Config.Key("sql.pool.validation.interval")
    int getPoolValidationInterval();

    @Config.DefaultValue("SELECT 1")
    @Config.Key("sql.pool.validation.query")
    String getPoolValidationQuery();

    @Config.DefaultValue("false")
    @Config.Key("sql.pool.test.while.idle")
    boolean getPoolTestWhileIdle();

    @Config.DefaultValue("true")
    @Config.Key("sql.pool.test.on.borrow")
    boolean getPoolTestOnBorrow();

    @Config.DefaultValue("false")
    @Config.Key("sql.pool.test.on.return")
    boolean getPoolTestOnReturn();

    @Config.DefaultValue("true")
    @Config.Key("sql.pool.remove.abandoned")
    boolean getPoolRemoveAbandoned();

    @Config.DefaultValue("55")
    @Config.Key("sql.pool.remove.abandoned.timeout")
    int getPoolRemoveAbandonedTimeout();

    @Config.DefaultValue("5000")
    @Config.Key("sql.pool.max.wait")
    int getPoolMaxWait();

    @Config.DefaultValue("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer")
    @Config.Key("sql.pool.jdbc.interceptors")
    String getPoolJdbcInterceptors();

    @Config.DefaultValue("org.postgresql.Driver")
    @Config.Key("sql.db.driver")
    String getDbDriverClass();

    @Config.DefaultValue("localhost:5432")
    @Config.Key("sql.db.server")
    String getDbServer();

    @Config.DefaultValue("autoReconnect=true")
    @Config.Key("sql.db.params")
    String getDbParams();


    @Config.Key("sql.database")
    String getDbDatabase();

    @Config.Key("sql.user")
    String getDbUser();

    @Config.Key("sql.password")
    String getDbPassword();

    @Config.Key("sql.arta.path")
    String getDbArtaPath();
}
