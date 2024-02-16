package com.vantar.admin.web;

import com.vantar.admin.model.*;
import com.vantar.exception.*;
import com.vantar.service.Services;
import com.vantar.service.auth.ServiceAuth;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/users/online",
    "/admin/users/signin/fails/reset",
})
public class AdminUsersController extends RouteToMethod {

    public void usersOnline(Params params, HttpServletResponse response) throws FinishException {
        AdminAuth.onlineUsers(params, response);
    }

    public void usersSigninFailsReset(Params params, HttpServletResponse response) throws ServiceException {
        Services.getService(ServiceAuth.class).resetSigninFails();
    }
}