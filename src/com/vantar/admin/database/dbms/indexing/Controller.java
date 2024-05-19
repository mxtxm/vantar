package com.vantar.admin.database.dbms.indexing;

import com.vantar.database.common.Db;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/database/mongo/index",
    "/admin/database/mongo/index/create",
    "/admin/database/mongo/index/get",
    "/admin/database/sql/index",
    "/admin/database/sql/index/create",
    "/admin/database/sql/index/get",
})
public class Controller extends RouteToMethod {

    public void databaseMongoIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabaseIndex.listIndexes(params, response, Db.Dbms.MONGO);
    }

    public void databaseMongoIndexCreate(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabaseIndex.createIndex(params, response, Db.Dbms.MONGO);
    }

    public void databaseMongoIndexGet(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabaseIndex.getIndexes(params, response, Db.Dbms.MONGO);
    }

    public void databaseSqlIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabaseIndex.listIndexes(params, response, Db.Dbms.SQL);
    }

    public void databaseSqlIndexCreate(Params params, HttpServletResponse response) throws FinishException {
        AdminDatabaseIndex.createIndex(params, response, Db.Dbms.SQL);
    }
}