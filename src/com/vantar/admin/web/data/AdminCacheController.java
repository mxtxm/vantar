package com.vantar.admin.web.data;

import com.vantar.admin.model.database.AdminCache;
import com.vantar.exception.*;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/cache/index",
    "/admin/cache/view",
    "/admin/cache/refresh",
})
public class AdminCacheController extends RouteToMethod {

    public void cacheIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminCache.index(params, response);
    }

    public void cacheView(Params params, HttpServletResponse response) throws FinishException, InputException {
        AdminCache.view(params, response);
    }

    public void cacheRefresh(Params params, HttpServletResponse response) throws FinishException, InputException {
        AdminCache.refresh(params, response);
    }
}