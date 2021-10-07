package com.vantar.admin.web;

import com.vantar.admin.model.AdminSystemError;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;


@WebServlet({
    "/admin/system/errors",
    "/admin/system/errors/delete",
})
public class AdminSystemErrorController extends RouteToMethod {

    public void systemErrors(Params params, HttpServletResponse response) {
        AdminSystemError.systemErrors(params, response);
    }

    public void systemErrorsDelete(Params params, HttpServletResponse response) {
        AdminSystemError.systemErrorsDelete(params, response);
    }
}