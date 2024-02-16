package com.vantar.admin.web;

import com.vantar.admin.model.index.AdminMonitoring;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/monitoring",
})
public class AdminMonitoringController extends RouteToMethod {

    public void monitoring(Params params, HttpServletResponse response) throws FinishException {
        AdminMonitoring.index(params, response);
    }
}