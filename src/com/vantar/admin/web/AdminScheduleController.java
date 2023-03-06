package com.vantar.admin.web;

import com.vantar.admin.model.AdminSchedule;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/schedule",
    "/admin/schedule/run",
})
public class AdminScheduleController extends RouteToMethod {

    public void schedule(Params params, HttpServletResponse response) throws FinishException {
        AdminSchedule.index(params, response);
    }

    public void scheduleRun(Params params, HttpServletResponse response) throws FinishException {
        AdminSchedule.run(params, response);
    }
}