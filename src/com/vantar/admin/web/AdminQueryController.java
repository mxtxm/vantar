package com.vantar.admin.web;

import com.vantar.admin.model.AdminQuery;
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

    public void queryIndex(Params params, HttpServletResponse response) {
        AdminQuery.index(params, response);
    }

    public void queryWrite(Params params, HttpServletResponse response) {
        AdminQuery.write(params, response);
    }

    public void queryDelete(Params params, HttpServletResponse response) {
        AdminQuery.delete(params, response);
    }

    public void queryGet(Params params, HttpServletResponse response) {
        AdminQuery.get(params, response);
    }

}