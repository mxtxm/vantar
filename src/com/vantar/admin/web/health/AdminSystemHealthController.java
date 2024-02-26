package com.vantar.admin.web.health;

import com.vantar.business.*;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.VantarException;
import com.vantar.service.Services;
import com.vantar.service.healthmonitor.ServiceHealthMonitor;
import com.vantar.service.log.ServiceLog;
import com.vantar.service.log.dto.Log;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@WebServlet({
    "/admin/system/health",
    "/admin/system/health/errors",
    "/admin/system/health/report",
    "/admin/system/database/report",
    "/admin/system/log/tags",
    "/admin/system/log/search",
})
public class AdminSystemHealthController extends RouteToMethod {

    public void systemHealth(Params params, HttpServletResponse response) {
        ServiceHealthMonitor monitor = Services.get(ServiceHealthMonitor.class);
        try {
            response.getWriter().write(monitor == null || monitor.getOverallSystemHealth() ? "OK" : "NOK");
        } catch (IOException ignore) {

        }
    }

    public void systemHealthErrors(Params params, HttpServletResponse response) {
        ServiceHealthMonitor monitor = Services.get(ServiceHealthMonitor.class);
        Response.writeJson(response, monitor == null ? new ArrayList<>(1) : monitor.getLastErrors());
    }

    public void systemHealthReport(Params params, HttpServletResponse response) {
        ServiceHealthMonitor monitor = Services.get(ServiceHealthMonitor.class);
        Response.writeJson(response, monitor == null ? null : monitor.getSystemHealthReport());
    }

    public static void systemLogTags(Params params, HttpServletResponse response) {
        Response.writeJson(response, ServiceLog.getLogTags());
    }

    public static void systemLogSearch(Params params, HttpServletResponse response) throws VantarException {
        String tag = params.extractFromJsonRequired("tag", String.class);
        Response.writeJson(
            response,
            ModelMongo.search(params, new Log(), new CommonModel.QueryEvent() {
                @Override
                public void beforeQuery(QueryBuilder q) {
                    q.condition()
                        .conditionRemove("tag")
                        .equal("tag", tag);
                }

                @Override
                public void afterSetData(Dto dto) {

                }

                @Override
                public void afterSetData(Dto dto, List<?> data) {

                }
            })
        );
    }
}