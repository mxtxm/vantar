package com.vantar.admin.model;

import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminAuth {

    public static boolean hasAccess(Params params, CommonUserRole role) {
        try {
            return Services.get(ServiceAuth.class).hasAccess(params, role);
        } catch (ServiceException e) {
            return false;
        }
    }

    public static void onlineUsers(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_ONLINE_USERS), params, response);
        if (ui == null) {
            return;
        }
        ServiceAuth auth;
        try {
            auth = Services.get(ServiceAuth.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
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
                    info.user.getFullName() + " (" + info.user.getId() + ")\n" + getRoleDescription(info.user),
                    info.lastInteraction.toString() + " -> " + info.user.getToken()
                        + " [pending=" + pending + "s, remaining=" + remaining + "s]"
                );
            });

        ui.containerEnd();

        ui.beginBox(Locale.getString(VantarKey.ADMIN_SIGNUP_TOKEN_TEMP));
        auth.getSignupVerifyTokens().forEach((code, info) -> ui.addKeyValue(
            info.user.getFullName() + " (" + info.user.getId() + ") - " + getRoleDescription(info.user),
            info.lastInteraction.toString() + " -> " + info.user.getToken()
        ));
        ui.containerEnd();

        ui.beginBox(Locale.getString(VantarKey.ADMIN_SIGNIN_TOKEN_TEMP));
        auth.getOneTimeTokens().forEach((code, info) -> ui.addKeyValue(
            info.user.getFullName() + " (" + info.user.getId() + ") - " + getRoleDescription(info.user),
            info.lastInteraction.toString() + " -> " + info.user.getToken()
        ));
        ui.containerEnd();

        ui.beginBox(Locale.getString(VantarKey.ADMIN_RECOVER_TOKEN_TEMP));
        auth.getVerifyTokens().forEach((code, info) -> ui.addKeyValue(
            info.user.getFullName() + " (" + info.user.getId() + ") - " + getRoleDescription(info.user),
            info.lastInteraction.toString() + " -> " + info.user.getToken()
        ));

        ui.finish();
    }

    private static String getRoleDescription(CommonUser user) {
        if (user.getRole() != null) {
            return user.getRole().getName() + (user.getRole().isRoot() ? " *root*" : "");
        }
        if (user.getRoles() == null) {
            return "!!NO ROLE!!";
        }
        StringBuilder sb = new StringBuilder();
        for (CommonUserRole role : user.getRoles()) {
            sb.append(role.getName()).append(role.isRoot() ? " *root*" : "").append(", ");
        }
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }
}
