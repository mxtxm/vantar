package com.vantar.admin.web;

import com.vantar.admin.model.*;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;


@WebServlet({
    "/admin/users/online",
})
public class AdminUsersController extends RouteToMethod {

    public void usersOnline(Params params, HttpServletResponse response) {
        AdminAuth.onlineUsers(params, response);
    }
}