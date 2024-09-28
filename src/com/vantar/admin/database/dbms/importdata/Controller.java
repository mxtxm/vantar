package com.vantar.admin.database.dbms.importdata;

import com.vantar.database.common.Db;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/database/mongo/import",
    "/admin/database/sql/import",
    "/admin/database/elastic/import",
})
public class Controller extends RouteToMethod {

    public void databaseMongoImport(Params params, HttpServletResponse response) throws FinishException {
        AdminImportData.importData(params, response, Db.Dbms.MONGO);
    }

    public void databaseSqlImport(Params params, HttpServletResponse response) throws FinishException {
        AdminImportData.importData(params, response, Db.Dbms.SQL);
    }

    public void databaseElasticImport(Params params, HttpServletResponse response) throws FinishException {
        AdminImportData.importData(params, response, Db.Dbms.ELASTIC);
    }
}