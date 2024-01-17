package com.vantar.admin.model;

import com.vantar.business.*;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.service.log.dto.Log;
import com.vantar.locale.*;
import com.vantar.service.log.LogEvent;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminSystemError {

    public static void systemErrors(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS), params, response, true);
        ui.write();

        List<String> tags = LogEvent.getErrorTags();
        if (tags.isEmpty()) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_NO_ERROR));
        }

        for (String tag : tags) {
            ui  .beginBox(tag)
                .addBlockLink(Locale.getString(VantarKey.ADMIN_DELETE_DO), "/admin/system/errors/delete?tag=" + tag)
                .addEmptyLine();

            for (String item : LogEvent.get(tag)) {
                ui.addErrorMessage(item);
            }
            ui.containerEnd().write();
        }

        ui.finish();
    }

    public static void systemErrorsDelete(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_ERRORS_DELETE), params, response, true);

        Log dto = new Log();
        dto.tag = params.getString("tag");

        ui.beginBox(dto.tag);

        try {
            long count = (long) ModelMongo.deleteById(dto).value;
            ui.addMessage(Locale.getString(VantarKey.DELETE_SUCCESS));
            ui.addMessage(count + Locale.getString(VantarKey.ADMIN_RECORDS));
        } catch (VantarException e) {
            ui.addErrorMessage(Locale.getString(VantarKey.DELETE_FAIL));
            ui.addErrorMessage(e);
        }

        ui  .addEmptyLine().addEmptyLine().addEmptyLine()
            .addBlockLink(Locale.getString(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS)), "/admin/system/errors")
            .finish();
    }

    public static List<Dto> query(Params params) throws VantarException {
        QueryBuilder q = new QueryBuilder(new Log())
            .sort("id:desc")
            .page(params.getInteger("page", 1), params.getInteger("count", 200));

        String tag = params.getString("tag");
        if (tag != null) {
            q.condition().equal("tag", tag);
        }
        return ModelMongo.getData(q);
    }

}