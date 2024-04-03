package com.vantar.admin.model.queue;

import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/queue/index",
    "/admin/queue/purge",
    "/admin/queue/purge/selective",
})
public class Controller extends RouteToMethod {

    public void queueIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminQueue.status(params, response);
    }

    public void queuePurge(Params params, HttpServletResponse response) throws FinishException {
        AdminQueue.purge(params, response);
    }

    public void queuePurgeSelective(Params params, HttpServletResponse response) throws FinishException {
        AdminQueue.purgeSelective(params, response);
    }
}