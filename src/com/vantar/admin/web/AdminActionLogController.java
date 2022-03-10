package com.vantar.admin.web;

import com.vantar.admin.model.*;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/action/log/user",
    "/admin/action/log/request",
})
public class AdminActionLogController extends RouteToMethod {

    public void actionLogUser(Params params, HttpServletResponse response) throws FinishException {
        AdminActionLog.user(params, response);
    }

    public void actionLogRequest(Params params, HttpServletResponse response) throws FinishException {
        AdminActionLog.request(params, response);
    }

}