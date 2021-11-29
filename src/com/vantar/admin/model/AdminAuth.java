package com.vantar.admin.model;

import com.vantar.exception.AuthException;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminAuth {

    public static boolean hasAccess(Params params, CommonUserRole role) {
        ServiceAuth auth = Services.get(ServiceAuth.class);
        if (auth == null) {
            return false;
        }
        return auth.hasAccess(params, role);
    }

    public static void onlineUsers(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_ONLINE_USERS), params, response);
        if (ui == null) {
            return;
        }
        ServiceAuth auth = Services.get(ServiceAuth.class);
        if (auth == null) {
            ui.addErrorMessage(Locale.getString(VantarKey.ADMIN_AUTH_IS_DISABLED)).finish();
            return;
        }

        String tk = params.getString("tk");
        if (tk != null) {
            auth.removeToken(tk);
        }

        ui  .beginBox(Locale.getString(VantarKey.ADMIN_DELETE_TOKEN))
            .addText(Locale.getString(VantarKey.ADMIN_DELETE_TOKEN_DESCRIPTION))
            .beginFormPost()
            .addInput(Locale.getString(VantarKey.ADMIN_AUTH_TOKEN), "tk")
            .addSubmit(Locale.getString(VantarKey.ADMIN_DELETE_DO))
            .containerEnd()
            .containerEnd();

        ui.beginBox(Locale.getString(VantarKey.ADMIN_ONLINE_USERS));
        ui.addText("Token expire time: " + auth.tokenExpireMin + "mins");

            auth.getOnlineUsers().forEach((code, info) -> {
                long pending = -info.lastInteraction.secondsFromNow();
                long remaining = (auth.tokenExpireMin * 60) - pending;
                ui.addKeyValue(
                    info.user.getFullName() + " (" + info.user.getId() + ")\n" + info.user.getRole().toString(),
                    info.lastInteraction.toString() + " -> " + info.user.getToken() + " [pending=" + pending + "s, remaining=" + remaining + "s]"
                );
            });

        ui.containerEnd();

        ui.beginBox(Locale.getString(VantarKey.ADMIN_SIGNUP_TOKEN_TEMP));
        auth.getSignupVerifyTokens().forEach((code, info) -> ui.addKeyValue(
            info.user.getFullName() + " (" + info.user.getId() + ") - " + info.user.getRole().toString(),
            info.lastInteraction.toString() + " -> " + info.user.getToken()
        ));
        ui.containerEnd();

        ui.beginBox(Locale.getString(VantarKey.ADMIN_SIGNIN_TOKEN_TEMP));
        auth.getOneTimeTokens().forEach((code, info) -> ui.addKeyValue(
            info.user.getFullName() + " (" + info.user.getId() + ") - " + info.user.getRole().toString(),
            info.lastInteraction.toString() + " -> " + info.user.getToken()
        ));
        ui.containerEnd();

        ui.beginBox(Locale.getString(VantarKey.ADMIN_RECOVER_TOKEN_TEMP));
        auth.getVerifyTokens().forEach((code, info) -> ui.addKeyValue(
            info.user.getFullName() + " (" + info.user.getId() + ") - " + info.user.getRole().toString(),
            info.lastInteraction.toString() + " -> " + info.user.getToken()
        ));

        ui.finish();
    }
}
