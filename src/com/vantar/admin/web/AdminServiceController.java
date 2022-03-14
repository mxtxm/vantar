package com.vantar.admin.web;

import com.vantar.admin.model.*;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/system/services/start",
    "/admin/system/services/stop",
    "/admin/system/factory/reset",
    "/admin/system/gc",
})
public class AdminServiceController extends RouteToMethod {

    public void systemServicesStart(Params params, HttpServletResponse response) throws FinishException {
        AdminService.startServices(params, response);
    }

    public void systemServicesStop(Params params, HttpServletResponse response) throws FinishException {
        AdminService.stopServices(params, response);
    }

    public void systemFactoryReset(Params params, HttpServletResponse response) throws FinishException {
        AdminService.factoryReset(params, response);
    }

    public void systemGc(Params params, HttpServletResponse response) throws FinishException {
        AdminService.gc(params, response);
    }
}