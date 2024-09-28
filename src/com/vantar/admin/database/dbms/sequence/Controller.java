package com.vantar.admin.database.dbms.sequence;

import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/database/mongo/sequence",
})
public class Controller extends RouteToMethod {

    public void databaseMongoSequence(Params params, HttpServletResponse response) throws FinishException {
        AdminSequence.listMongoSequences(params, response);
    }
}