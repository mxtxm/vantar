package com.vantar.admin.schedule;

import com.vantar.exception.*;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/schedule",
    "/admin/schedule/run",
})
public class Controller extends RouteToMethod {

    public void schedule(Params params, HttpServletResponse response) throws FinishException {
        AdminSchedule.index(params, response);
    }

    public void scheduleRun(Params params, HttpServletResponse response) throws FinishException, VantarException {
        AdminSchedule.run(params, response);
    }
}