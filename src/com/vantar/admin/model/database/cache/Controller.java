package com.vantar.admin.model.database.cache;

import com.vantar.exception.*;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/database/cache/index",
    "/admin/database/cache/view",
    "/admin/database/cache/refresh",
})
public class Controller extends RouteToMethod {

    public void databaseCacheIndex(Params params, HttpServletResponse response) throws FinishException {
        AdminDtoCache.index(params, response);
    }

    public void databaseCacheView(Params params, HttpServletResponse response) throws FinishException, InputException {
        AdminDtoCache.view(params, response);
    }

    public void databaseCacheRefresh(Params params, HttpServletResponse response) throws FinishException, InputException {
        AdminDtoCache.refresh(params, response);
    }
}