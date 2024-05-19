package com.vantar.admin.database.data.panel;

import com.vantar.database.common.Db;
import com.vantar.database.differences.DtoChanges;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.log.dto.UserLog;
import com.vantar.util.json.Json;
import com.vantar.util.object.ObjectUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


public class AdminLogActionDiff {

    public static void view(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDto(VantarKey.ADMIN_LOG_DIFFERENCES, "delete-check", params, response, info);

        List<Long> ids = params.getLongList("delete-check");
        if (ObjectUtil.isEmpty(ids)) {
            u.ui.finish();
            return;
        }

        QueryBuilder q = new QueryBuilder(new UserLog());
        q.sort("id:desc");
        q.condition()
            .equal("classNameSimple", info.dtoClass.getSimpleName())
            .inNumber("id", ids);

        List<UserLog> data;
        try {
            data = Db.modelMongo.getData(q);
        } catch (VantarException e) {
            u.ui.addErrorMessage(e).finish();
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
                plotLogDiff(u.ui, changes, userLogBefore, userLogAfter);
            }
            userLogBefore = userLogAfter;
        }

        u.ui.finish();
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