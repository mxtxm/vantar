package com.vantar.admin.web;

import com.vantar.admin.model.Admin;
import com.vantar.common.VantarParam;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet({
    "/admin/signin",
    "/admin/signout",
})
public class AdminSigninController extends RouteToMethod {

    public void signin(Params params, HttpServletResponse response) throws ServerException {
        WebUi ui = new WebUi(params, response);
        ui  .addPageTitle(Locale.getString(VantarKey.ADMIN_SIGN_IN))
            .addEmptyLine()
            .addEmptyLine()
            .addEmptyLine()
            .addEmptyLine()
            .addEmptyLine();

        if (params.getString("username") == null) {
            ui  .beginFormPost()
                .addEmptyLine()
                .addInput(Locale.getString(VantarKey.USERNAME), VantarParam.USER_NAME)
                .addPassword(Locale.getString(VantarKey.PASSWORD), VantarParam.PASSWORD)
                .addSubmit(Locale.getString(VantarKey.SIGN_IN))
                .containerEnd();

        } else {
            ServiceAuth auth = Services.get(ServiceAuth.class);
            if (auth == null) {
                ui.addErrorMessage("AUTH SERVICE IS DISABLED").finish();
                return;
            }
            try {
                CommonUser user = auth.signin(params);
                ui.setAuthToken(user.getToken());
                Admin.log.info(" > admin dashboard signed in: {}'", user.getUsername());
                ui.redirect("/admin/index");
                return;
            } catch (AuthException e) {
                Admin.log.info(" ! admin dashboard rejected: {}", e.getMessage());
                ui.addErrorMessage(e);
            }
        }

        ui.finish();
    }

    public void signout(Params params, HttpServletResponse response) {
        try {
            Services.get(ServiceAuth.class).signout(params);
        } catch (ServiceException ignore) {

        }
        try {
            response.sendRedirect("/admin/signin");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}