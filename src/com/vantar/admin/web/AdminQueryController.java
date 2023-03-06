package com.vantar.admin.web;

import com.vantar.admin.model.AdminQuery;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/query/index",
    "/admin/query/write",
    "/admin/query/delete",
    "/admin/query/get",
})
public class AdminQueryController extends RouteToMethod {

    public void queryIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminQuery.index(params, response);
    }

    public void queryWrite(Params params, HttpServletResponse response) throws FinishException {
        AdminQuery.write(params, response);
    }

    public void queryDelete(Params params, HttpServletResponse response) throws FinishException {
        AdminQuery.delete(params, response);
    }

    public void queryGet(Params params, HttpServletResponse response) throws FinishException {
        AdminQuery.get(params, response);
    }

}