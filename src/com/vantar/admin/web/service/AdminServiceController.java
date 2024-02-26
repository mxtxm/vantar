package com.vantar.admin.web.service;

import com.vantar.admin.model.service.*;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/services/index",
    "/admin/services/start",
    "/admin/services/stop",
    "/admin/factory/reset",
    "/admin/system/gc",
})
public class AdminServiceController extends RouteToMethod {

    public void servicesIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminServiceMonitoring.servicesIndex(params, response);
    }

    public void servicesStart(Params params, HttpServletResponse response) throws FinishException {
        AdminService.startServices(params, response);
    }

    public void servicesStop(Params params, HttpServletResponse response) throws FinishException {
        AdminService.stopServices(params, response);
    }

    public void factoryReset(Params params, HttpServletResponse response) throws FinishException {
        AdminService.factoryReset(params, response);
    }

    public void systemGc(Params params, HttpServletResponse response) throws FinishException {
        AdminService.gc(params, response);
    }
}