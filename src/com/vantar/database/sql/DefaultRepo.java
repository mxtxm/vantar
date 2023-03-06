package com.vantar.database.sql;


public class DefaultRepo extends SqlExecute {

    public DefaultRepo(SqlConnection connection) {
        this.connection = connection;
    }
}