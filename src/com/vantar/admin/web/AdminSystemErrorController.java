package com.vantar.admin.web;

import com.vantar.admin.model.AdminSystemError;
import com.vantar.exception.*;
import com.vantar.service.log.LogEvent;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/system/errors",
    "/admin/system/errors/delete",

    "/admin/system/errors/get/tags",
    "/admin/system/errors/get",
})
public class AdminSystemErrorController extends RouteToMethod {

    public void systemErrors(Params params, HttpServletResponse response) throws FinishException {
        AdminSystemError.systemErrors(params, response);
    }

    public void systemErrorsDelete(Params params, HttpServletResponse response) throws FinishException {
        AdminSystemError.systemErrorsDelete(params, response);
    }

    public void systemErrorsGetTags(Params params, HttpServletResponse response) {
        Response.writeJson(response, LogEvent.getErrorTags());
    }

    public void systemErrorsGet(Params params, HttpServletResponse response) throws VantarException {
        Response.writeJson(response, AdminSystemError.query(params));
    }
}