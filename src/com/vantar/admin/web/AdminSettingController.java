package com.vantar.admin.web;

import com.vantar.admin.model.*;
import com.vantar.common.Settings;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;


@WebServlet({
    "/admin/system/settings/reload",
    "/admin/system/settings/edit/config",
    "/admin/system/settings/edit/tune",
})
public class AdminSettingController extends RouteToMethod {

    public void systemSettingsReload(Params params, HttpServletResponse response) throws FinishException {
        AdminSettings.settingsReload(params, response);
    }

    public void systemSettingsEditConfig(Params params, HttpServletResponse response) throws FinishException {
        AdminSettings.settingsEdit(params, response, Settings.configClass);
    }

    public void systemSettingsEditTune(Params params, HttpServletResponse response) throws FinishException {
        AdminSettings.settingsEdit(params, response, Settings.tuneClass);
    }
}