package com.vantar.admin.web;

import com.vantar.admin.model.AdminCache;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;


@WebServlet({
    "/admin/cache/index",
    "/admin/cache/view",
})
public class AdminCacheController extends RouteToMethod {

    public void cacheIndex(Params params, HttpServletResponse response) {
        AdminCache.index(params, response);
    }

    public void cacheView(Params params, HttpServletResponse response) {
        AdminCache.view(params, response);
    }
}