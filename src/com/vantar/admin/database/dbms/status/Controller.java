package com.vantar.admin.database.dbms.status;

import com.vantar.exception.*;
import com.vantar.web.*;
import javax.servlet.annotation.*;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/database/mongo/status",
    "/admin/database/sql/status",
    "/admin/database/elastic/status",
})
public class Controller extends RouteToMethod {

    public void databaseMongoStatus(Params params, HttpServletResponse response) throws FinishException {
        AdminStatus.statusMongo(params, response);
    }

    public void databaseSqlStatus(Params params, HttpServletResponse response) throws FinishException {
        AdminStatus.statusSql(params, response);
    }

    public void databaseElasticStatus(Params params, HttpServletResponse response) throws FinishException {
        AdminStatus.statusElastic(params, response);
    }
}