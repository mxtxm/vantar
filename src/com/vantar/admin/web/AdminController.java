package com.vantar.admin.web;

import com.vantar.admin.model.Admin;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;


@WebServlet({
    "/admin",
    "/admin/",
    "/admin/index",
})
public class AdminController extends RouteToMethod {

    public void index(Params params, HttpServletResponse response) {
        Admin.index(params, response);
    }
}