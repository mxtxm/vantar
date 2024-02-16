package com.vantar.admin.web;

import com.vantar.admin.model.database.AdminCache;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/cache/index",
    "/admin/cache/view",
})
public class AdminCacheController extends RouteToMethod {

    public void cacheIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminCache.index(params, response);
    }

    public void cacheView(Params params, HttpServletResponse response) throws FinishException {
        AdminCache.view(params, response);
    }
}