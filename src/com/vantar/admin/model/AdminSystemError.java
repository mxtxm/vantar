package com.vantar.admin.model;

import com.vantar.service.log.dto.Log;
import com.vantar.business.CommonRepoMongo;
import com.vantar.exception.DatabaseException;
import com.vantar.locale.*;
import com.vantar.service.log.LogEvent;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


public class AdminSystemError {

    private static final String PARAM_TAG = "tag";


    public static void systemErrors(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS), params, response);
        if (ui == null) {
            return;
        }
        ui.write();

        List<String> tags = LogEvent.getErrorTags();
        if (tags.isEmpty()) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_NO_ERROR));
        }

        for (String tag : tags) {
            ui  .beginBox(tag)
                .addBlockLink(Locale.getString(VantarKey.ADMIN_DELETE_DO), "/admin/system/errors/delete?" + PARAM_TAG + "=" + tag)
                .addEmptyLine();

            for (String item : LogEvent.get(tag)) {
                ui.addErrorMessage(item);
            }
            ui.containerEnd().write();
        }

        ui.finish();
    }

    public static void systemErrorsDelete(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_ERRORS_DELETE), params, response);
        if (ui == null) {
            return;
        }

        Log dto = new Log();
        dto.tag = params.getString(PARAM_TAG);

        ui.beginBox(dto.tag);

        try {
            long count = CommonRepoMongo.delete(dto);
            ui.addMessage(Locale.getString(VantarKey.DELETE_SUCCESS));
            ui.addMessage(count + Locale.getString(VantarKey.ADMIN_RECORDS));
        } catch (DatabaseException e) {
            ui.addErrorMessage(Locale.getString(VantarKey.DELETE_FAIL));
            ui.addErrorMessage(e);
        }

        ui  .addEmptyLine().addEmptyLine().addEmptyLine()
            .addBlockLink(Locale.getString(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS)), "/admin/system/errors")
            .finish();
    }
}