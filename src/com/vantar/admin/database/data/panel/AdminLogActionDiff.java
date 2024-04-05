package com.vantar.admin.database.data.panel;

import com.vantar.admin.index.Admin;
import com.vantar.business.*;
import com.vantar.database.differences.DtoChanges;
import com.vantar.database.dto.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.log.dto.*;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


public class AdminLogActionDiff {

    public static void view(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        WebUi ui = Admin.getUiDto(Locale.getString(VantarKey.ADMIN_LOG_DIFFERENCES), params, response, dtoInfo);
        if (dtoInfo == null) {
            return;
        }
        if (!DataUtil.isUp(dtoInfo.dbms, ui)) {
            ui.finish();
            return;
        }
        List<Long> ids = params.getLongList("delete-check");
        if (ObjectUtil.isEmpty(ids)) {
            ui.finish();
            return;
        }

        QueryBuilder q = new QueryBuilder(new UserLog());
        q.sort("id:desc");
        q.condition()
            .equal("classNameSimple", dtoInfo.dtoClass.getSimpleName())
            .inNumber("id", ids);

        List<UserLog> data;
        try {
            data = ModelMongo.getData(q);
        } catch (VantarException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        UserLog userLogBefore = null;
        for (UserLog userLogAfter : data) {
            if (userLogBefore != null) {
                DtoChanges diff = new DtoChanges();
                List<DtoChanges.Change> changes = diff.get(
                    Json.d.fromJson(Json.d.toJson(userLogAfter.objectX), userLogAfter.className),
                    Json.d.fromJson(Json.d.toJson(userLogBefore.objectX), userLogBefore.className)
                );
                plotLogDiff(ui, changes, userLogBefore, userLogAfter);
            }
            userLogBefore = userLogAfter;
        }

        ui.finish();
    }

    private static void plotLogDiff(WebUi ui, List<DtoChanges.Change> changes, UserLog userLogBefore, UserLog userLogAfter) {
        ui  .addEmptyLine(3)
            .addHeading(2, userLogAfter.action + " : " + userLogBefore.time);

        ui  .addKeyValue(userLogBefore.action, userLogAfter.action)
            .addKeyValue(userLogBefore.time, userLogAfter.time)
            .addKeyValue(userLogBefore.userName, userLogAfter.userName)
            .addKeyValue(userLogBefore.url, userLogAfter.url)
            .addKeyValue("user.id " + userLogBefore.userId, userLogAfter.userId)
            .addKeyValue("thread.id " + userLogBefore.threadId, userLogAfter.threadId)
            .addKeyValue("log.id " + userLogBefore.id, userLogAfter.id)
            .addKeyValue("", "more...", "diff-more", null, null, "getLogWebInfo(" + userLogAfter.id + ")")
            .write();

        for (DtoChanges.Change change : changes) {
            ui.addBlock("pre", change.column, "log-field").write();
            ui.addBlock("pre", change.before, "log-before").write();
            ui.addBlock("pre", change.after, "log-after").write();
        }
    }
}