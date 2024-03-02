package com.vantar.admin.modelw.monitoring;

import com.vantar.exception.*;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/monitoring",

    "/admin/system/errors/index",
    "/admin/system/errors/by/tag",
    "/admin/system/errors/delete",
})
public class Controller extends RouteToMethod {

    public void monitoring(Params params, HttpServletResponse response) throws FinishException {
        AdminMonitoring.index(params, response);
    }

    public void systemErrorsIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminSystemError.index(params, response);
    }

    public void systemErrorsByTag(Params params, HttpServletResponse response) throws FinishException, InputException {
        AdminSystemError.systemErrorsByTag(params, response);
    }

    public void systemErrorsDelete(Params params, HttpServletResponse response) throws FinishException, InputException {
        AdminSystemError.systemErrorsDelete(params, response);
    }
}