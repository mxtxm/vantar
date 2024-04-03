package com.vantar.admin.model.database.dbms.importdata;

import com.vantar.database.dto.DtoDictionary;
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
        AdminImportData.importData(params, response, DtoDictionary.Dbms.MONGO);
    }

    public void databaseSqlImport(Params params, HttpServletResponse response) throws FinishException {
        AdminImportData.importData(params, response, DtoDictionary.Dbms.SQL);
    }

    public void databaseElasticImport(Params params, HttpServletResponse response) throws FinishException {
        AdminImportData.importData(params, response, DtoDictionary.Dbms.ELASTIC);
    }
}