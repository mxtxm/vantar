package com.vantar.admin.web;

import com.vantar.admin.model.AdminMonitoring;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;


@WebServlet({
    "/admin/monitoring",
    "/admin/services/status",
    "/admin/services/run",
})
public class AdminMonitoringController extends RouteToMethod {

    public void monitoring(Params params, HttpServletResponse response) throws FinishException {
        AdminMonitoring.index(params, response);
    }

    public void servicesStatus(Params params, HttpServletResponse response) throws FinishException {
        AdminMonitoring.servicesLastRun(params, response);
    }

    public void servicesRun(Params params, HttpServletResponse response) throws FinishException {
        AdminMonitoring.servicesStatus(params, response);
    }
}