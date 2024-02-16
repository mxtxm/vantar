package com.vantar.database.sql;

import com.vantar.exception.DatabaseException;
import org.apache.tomcat.jdbc.pool.*;
import org.slf4j.*;
import java.sql.*;
import java.util.*;


public class SqlConnection implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SqlConnection.class);
    private static DataSource dataSource;
    private java.sql.Connection connection;
    private boolean isCommitted;
    private static SqlConfig config;
    private static SqlDbms dbms;
    private static boolean isUp;


    public static boolean isEnabled() {
        return config != null;
    }

    public static boolean isUp() {
        return isUp;
    }

    public static SqlConfig getConfig() {
        return config;
    }

    public static SqlDbms getDbms() {
        return dbms;
    }

    public static void start(SqlConfig config) {
        if (config == null) {
            return;
        }
        SqlConnection.config = config;
        if (config.getDbDriverClass().contains("post")) {
            dbms = SqlDbms.POSTGRESQL;
        } else if (config.getDbDriverClass().contains("my")) {
            dbms = SqlDbms.MYSQL;
        }

        PoolProperties properties = new PoolProperties();
        properties.setUrl(getJdbcUrl(config));
        properties.setDriverClassName(config.getDbDriverClass());
        properties.setUsername(config.getDbUser());
        properties.setPassword(config.getDbPassword());

        properties.setInitialSize(config.getPoolInitialSize());
        properties.setMaxActive(config.getPoolMaxActive());
        properties.setMinIdle(config.getPoolMinIdle());
        properties.setMaxIdle(config.getPoolMaxIdle());

        properties.setTimeBetweenEvictionRunsMillis(config.getPoolTimeBetweenEvictionRunsMillis());
        properties.setMinEvictableIdleTimeMillis(config.getPoolMinEvictableIdleTimeMillis());
        properties.setValidationInterval(config.getPoolValidationInterval());

        properties.setValidationQuery(config.getPoolValidationQuery());
        properties.setTestWhileIdle(config.getPoolTestWhileIdle());
        properties.setTestOnBorrow(config.getPoolTestOnBorrow());
        properties.setTestOnReturn(config.getPoolTestOnReturn());
        properties.setRemoveAbandoned(config.getPoolRemoveAbandoned());
        properties.setRemoveAbandonedTimeout(config.getPoolRemoveAbandonedTimeout());
        properties.setMaxWait(config.getPoolMaxWait());

        properties.setJdbcInterceptors(config.getPoolJdbcInterceptors());

        dataSource = new DataSource();
        dataSource.setPoolProperties(properties);

        isUp = true;
    }

    public static String getDbEngine(SqlConfig config) {
        if (config.getDbDriverClass().contains("postgre")) {
            return "postgresql";
        }
        if (config.getDbDriverClass().contains("mysql")) {
            return "mysql";
        }
        return null;
    }

    public static void shutdown() {
        dataSource.close(true);
        dataSource = null;
        isUp = false;

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                log.info("deregistering jdbc driver: " + driver);
            } catch (SQLException e) {
                log.warn("! error deregistering driver " + driver, e);
            }
        }
    }

    public static String getJdbcUrl(SqlConfig config) {
        return "jdbc:" + getDbEngine(config) + "://" + config.getDbServer() + "/" + config.getDbDatabase() + "?" + config.getDbParams();
    }

    public java.sql.Connection getDatabase() throws DatabaseException {
        if (connection == null) {
            if (dataSource == null) {
                throw new DatabaseException("! dataSource is null, no handle to database");
            }
            try {
                connection = dataSource.getConnection();
            } catch (SQLException e) {
                log.error("! failed to get database connection", e);
                throw new DatabaseException(e);
            }
        }
        return connection;
    }

    @Override
    public void close() {
        try {
            if (!getDatabase().getAutoCommit() && !isCommitted) {
                rollback();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (DatabaseException | SQLException e) {
            log.error("! failed to close connection", e);
        }
        connection = null;
    }

    public void startTransaction() throws DatabaseException {
        try {
            getDatabase().setAutoCommit(false);
            isCommitted = false;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public void commit() throws DatabaseException {
        try {
            getDatabase().commit();
            isCommitted = true;
            getDatabase().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public void rollback() {
        try {
            getDatabase().rollback();
            getDatabase().setAutoCommit(true);
        } catch (DatabaseException | SQLException e) {
            log.error("! failed to rollback transaction", e);
        }
    }

    public boolean isTransactionOpen() {
        try {
            return !connection.getAutoCommit();
        } catch (SQLException e) {
            return false;
        }
    }
}
