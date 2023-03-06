package com.vantar.database.sql;

import org.aeonbits.owner.Config;


public interface SqlConfig {

    @Config.DefaultValue("10")
    @Config.Key("pool.initial.size")
    int getPoolInitialSize();

    @Config.DefaultValue("100")
    @Config.Key("pool.max.active")
    int getPoolMaxActive();

    @Config.DefaultValue("30")
    @Config.Key("pool.min.idle")
    int getPoolMinIdle();

    @Config.DefaultValue("80")
    @Config.Key("pool.max.idle")
    int getPoolMaxIdle();

    @Config.DefaultValue("3400")
    @Config.Key("pool.time.between.eviction.runs.millis")
    int getPoolTimeBetweenEvictionRunsMillis();

    @Config.DefaultValue("5400")
    @Config.Key("pool.min.evictable.idle.time.millis")
    int getPoolMinEvictableIdleTimeMillis();

    @Config.DefaultValue("3400")
    @Config.Key("pool.validation.interval")
    int getPoolValidationInterval();

    @Config.DefaultValue("SELECT 1")
    @Config.Key("pool.validation.query")
    String getPoolValidationQuery();

    @Config.DefaultValue("false")
    @Config.Key("pool.test.while.idle")
    boolean getPoolTestWhileIdle();

    @Config.DefaultValue("true")
    @Config.Key("pool.test.on.borrow")
    boolean getPoolTestOnBorrow();

    @Config.DefaultValue("false")
    @Config.Key("pool.test.on.return")
    boolean getPoolTestOnReturn();

    @Config.DefaultValue("true")
    @Config.Key("pool.remove.abandoned")
    boolean getPoolRemoveAbandoned();

    @Config.DefaultValue("55")
    @Config.Key("pool.remove.abandoned.timeout")
    int getPoolRemoveAbandonedTimeout();

    @Config.DefaultValue("5000")
    @Config.Key("pool.max.wait")
    int getPoolMaxWait();

    @Config.DefaultValue("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer")
    @Config.Key("pool.jdbc.interceptors")
    String getPoolJdbcInterceptors();

    @Config.DefaultValue("org.postgresql.Driver")
    @Config.Key("db.driver")
    String getDbDriverClass();

    @Config.DefaultValue("localhost:5432")
    @Config.Key("db.server")
    String getDbServer();

    @Config.DefaultValue("autoReconnect=true")
    @Config.Key("db.params")
    String getDbParams();


    @Config.Key("db.database")
    String getDbDatabase();

    @Config.Key("db.user")
    String getDbUser();

    @Config.Key("db.password")
    String getDbPassword();

    @Config.Key("db.arta.path")
    String getDbArtaPath();
}
