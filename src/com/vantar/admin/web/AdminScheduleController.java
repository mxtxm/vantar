package com.vantar.admin.web;

import com.vantar.admin.model.AdminSchedule;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;


@WebServlet({
    "/admin/schedule",
    "/admin/schedule/run",
})
public class AdminScheduleController extends RouteToMethod {

    public void schedule(Params params, HttpServletResponse response) {
        AdminSchedule.index(params, response);
    }

    public void scheduleRun(Params params, HttpServletResponse response) {
        AdminSchedule.run(params, response);
    }
}