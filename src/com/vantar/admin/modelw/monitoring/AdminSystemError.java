package com.vantar.admin.modelw.monitoring;

import com.vantar.admin.model.index.Admin;
import com.vantar.business.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.service.log.ServiceLog;
import com.vantar.service.log.dto.Log;
import com.vantar.locale.*;
import com.vantar.util.object.ObjectUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminSystemError {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_SYSTEM_ERRORS, params, response, true);
        List<String> tags = ServiceLog.getLogTags();
        if (ObjectUtil.isEmpty(tags)) {
            ui.addMessage(VantarKey.NO_CONTENT);
        }
        for (String tag : tags) {
            ui.addHrefBlock(tag, "/admin/system/errors/by/tag?t=" + tag);
        }
        ui.finish();
    }

    public static void systemErrorsByTag(Params params, HttpServletResponse response) throws FinishException, InputException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_SYSTEM_ERRORS, params, response, true).write();
        String tag = params.getStringRequired("t");

        ui  .addHeading(2, tag)
            .addHrefBlock(Locale.getString(VantarKey.ADMIN_DELETE_DO), "/admin/system/errors/delete?tag=" + tag)
            .addEmptyLine();

        for (String item : ServiceLog.getStoredLogs(tag)) {
            ui.addBlock("pre", item).write();
        }
        ui.finish();
    }

    public static void systemErrorsDelete(Params params, HttpServletResponse response) throws FinishException, InputException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_ERRORS_DELETE, params, response, true);
        String tag = params.getStringRequired("tag");

        ui.addHeading(2, tag);

        QueryBuilder q = new QueryBuilder(new Log());
        q.condition().equal("tag", tag);
        try {
            ModelMongo.delete(q);
            ui.addMessage(VantarKey.DELETE_SUCCESS);
        } catch (VantarException e) {
            ui.addErrorMessage(VantarKey.DELETE_FAIL);
            ui.addErrorMessage(e);
        }

        ui  .addEmptyLine().addEmptyLine()
            .addHrefBlock(VantarKey.ADMIN_SYSTEM_ERRORS, "/admin/system/errors/index")
            .finish();
    }
}