package com.vantar.admin.web.health;

import com.vantar.admin.model.heath.AdminSystemError;
import com.vantar.exception.*;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/system/errors/index",
    "/admin/system/errors/by/tag",
    "/admin/system/errors/delete",
})
public class AdminSystemErrorController extends RouteToMethod {

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