package com.vantar.admin.setting;

import com.vantar.common.Settings;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/system/settings/reload",
    "/admin/system/settings/edit/config",
    "/admin/system/settings/edit/tune",

    "/admin/system/settings/get",
})
public class Controller extends RouteToMethod {

    public void systemSettingsReload(Params params, HttpServletResponse response) throws FinishException {
        AdminSettings.settingsReload(params, response);
    }

    public void systemSettingsEditConfig(Params params, HttpServletResponse response) throws FinishException {
        AdminSettings.settingsEdit(params, response, Settings.configClass);
    }

    public void systemSettingsEditTune(Params params, HttpServletResponse response) throws FinishException {
        AdminSettings.settingsEdit(params, response, Settings.tuneClass);
    }

    public void systemSettingsGet(Params params, HttpServletResponse response) throws FinishException {
        Response.writeJson(response, AdminSettings.getSettings());
    }
}