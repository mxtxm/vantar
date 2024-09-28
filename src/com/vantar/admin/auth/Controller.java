package com.vantar.admin.auth;

import com.vantar.exception.*;
import com.vantar.service.Services;
import com.vantar.service.auth.ServiceAuth;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/signin",
    "/admin/signout",
    "/admin/users/online",
    "/admin/users/signin/fails/reset",
})
public class Controller extends RouteToMethod {

    public void signin(Params params, HttpServletResponse response) throws ServerException {
        AdminAuth.signin(params, response);
    }

    public void signout(Params params, HttpServletResponse response) {
        AdminAuth.signout(params, response);
    }

    public void usersOnline(Params params, HttpServletResponse response) throws FinishException {
        AdminAuth.onlineUsers(params, response);
    }

    /**
     * reset locked users caused by failed signin attempts
     */
    public void usersSigninFailsReset(Params params, HttpServletResponse response) throws ServiceException {
        Services.getService(ServiceAuth.class).resetSigninFails();
    }
}