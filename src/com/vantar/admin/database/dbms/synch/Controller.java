package com.vantar.admin.database.dbms.synch;

import com.vantar.database.common.Db;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/database/sql/synch",
    "/admin/database/elastic/synch",
})
public class Controller extends RouteToMethod {

    public void databaseSqlSynch(Params params, HttpServletResponse response) throws FinishException {
        AdminSynch.synch(params, response, Db.Dbms.SQL);
    }

    public void databaseElasticSynch(Params params, HttpServletResponse response) throws FinishException {
        AdminSynch.synch(params, response, Db.Dbms.ELASTIC);
    }
}