package com.vantar.admin.model.heath;

import com.vantar.admin.model.index.Admin;
import com.vantar.business.*;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.service.log.ServiceLog;
import com.vantar.service.log.dto.Log;
import com.vantar.locale.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminSystemError {

    public static void systemErrors(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS), params, response, true);
        ui.write();

        List<String> tags = ServiceLog.getLogTags();
        if (tags.isEmpty()) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_NO_ERROR));
        }

        for (String tag : tags) {
            ui  .beginBox(tag)
                .addHrefBlock(Locale.getString(VantarKey.ADMIN_DELETE_DO), "/admin/system/errors/delete?tag=" + tag)
                .addEmptyLine();

            for (String item : ServiceLog.getStoredLogs(tag)) {
                ui.addBlock("pre", item);
            }
            ui.blockEnd().write();
        }

        ui.finish();
    }

    public static void systemErrorsDelete(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_ERRORS_DELETE), params, response, true);

        String tag = params.getString("tag");

        ui.beginBox(tag);

        QueryBuilder q = new QueryBuilder(new Log());
        q.condition().equal("tag", tag);

        try {
            ModelMongo.delete(q);
            ui.addMessage(Locale.getString(VantarKey.DELETE_SUCCESS));
        } catch (VantarException e) {
            ui.addErrorMessage(Locale.getString(VantarKey.DELETE_FAIL));
            ui.addErrorMessage(e);
        }

        ui  .addEmptyLine().addEmptyLine().addEmptyLine()
            .addHrefBlock(Locale.getString(Locale.getString(VantarKey.ADMIN_SYSTEM_ERRORS)), "/admin/system/errors")
            .finish();
    }

    public static List<Dto> get(Params params) throws VantarException {
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