package com.vantar.admin.web;

import com.vantar.admin.model.*;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/users/online",
})
public class AdminUsersController extends RouteToMethod {

    public void usersOnline(Params params, HttpServletResponse response) throws FinishException {
        AdminAuth.onlineUsers(params, response);
    }
}