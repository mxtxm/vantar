package com.vantar.admin.model.service;

import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/service/index",
    "/admin/service/action",
    "/admin/factory/reset",
    "/admin/system/gc",
})
public class Controller extends RouteToMethod {

    public void serviceIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminServiceMonitoring.servicesIndex(params, response);
    }

    public void serviceAction(Params params, HttpServletResponse response) throws FinishException {
        AdminService.serviceAction(params, response);
    }

    public void factoryReset(Params params, HttpServletResponse response) throws FinishException {
        AdminService.factoryReset(params, response);
    }

    public void systemGc(Params params, HttpServletResponse response) throws FinishException {
        AdminService.gc(params, response);
    }
}