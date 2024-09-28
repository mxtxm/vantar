package com.vantar.admin.database.dbms.elastic;

import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/database/elastic/mapping/get",
    "/admin/database/elastic/actions",
})
public class Controller extends RouteToMethod {

    public void databaseElasticMappingGet(Params params, HttpServletResponse response) throws FinishException {
        AdminElastic.getMappingElastic(params, response);
    }

    public void databaseElasticActions(Params params, HttpServletResponse response) throws FinishException {
        AdminElastic.actionsElastic(params, response);
    }
}