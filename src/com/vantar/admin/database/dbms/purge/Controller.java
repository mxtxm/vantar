package com.vantar.admin.database.dbms.purge;

import com.vantar.database.dto.DtoDictionary;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/database/mongo/purge",
    "/admin/database/sql/purge",
    "/admin/database/elastic/purge",
})
public class Controller extends RouteToMethod {

    public void databaseMongoPurge(Params params, HttpServletResponse response) throws FinishException {
        AdminPurge.purge(params, response, DtoDictionary.Dbms.MONGO);
    }

    public void databaseSqlPurge(Params params, HttpServletResponse response) throws FinishException {
        AdminPurge.purge(params, response, DtoDictionary.Dbms.SQL);
    }

    public void databaseElasticPurge(Params params, HttpServletResponse response) throws FinishException {
        AdminPurge.purge(params, response, DtoDictionary.Dbms.ELASTIC);
    }
}