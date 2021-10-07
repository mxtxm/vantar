package com.vantar.admin.web;

import com.vantar.admin.model.AdminQueue;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;


@WebServlet({
    "/admin/queue/status",
    "/admin/queue/purge",
    "/admin/queue/purge/selective",
})
public class AdminQueueController extends RouteToMethod {

    public void queueStatus(Params params, HttpServletResponse response) {
        AdminQueue.status(params, response);
    }

    public void queuePurge(Params params, HttpServletResponse response) {
        AdminQueue.purge(params, response);
    }

    public void queuePurgeSelective(Params params, HttpServletResponse response) {
        AdminQueue.purgeSelective(params, response);
    }
}