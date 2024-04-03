package com.vantar.admin.model.query;

import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/query/index",
})
public class Controller extends RouteToMethod {

    public void queryIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminQuery.index(params, response);
    }
}