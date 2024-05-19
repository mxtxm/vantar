package com.vantar.admin.patch;

import com.vantar.admin.index.Admin;
import com.vantar.common.Settings;
import com.vantar.database.common.Db;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.patch.*;
import com.vantar.service.patch.dto.PatchHistory;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


public class AdminPatch {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_MENU_PATCH, params, response, true);
        String packageName = Settings.config.getProperty("package.patch");
        if (packageName == null) {
            ui.addMessage("No patches!").finish();
            return;
        }

        try {
            List<PatchHistory> patches = Db.modelMongo.getAll(new PatchHistory());
            for (PatchHistory patch : patches) {
                ui  .beginBox(patch.patchClass)
                    .addHrefNewPage(VantarKey.ADMIN_SCHEDULE_RUN, "/admin/patch/run?c=" + patch.patchClass)
                    .addKeyValue(VantarKey.ADMIN_RUN_TIME, patch.executedTime)
                    .addKeyValue(VantarKey.ADMIN_FAIL, patch.failCount);
                if (ObjectUtil.isNotEmpty(patch.fail)) {
                    ui.addKeyValue(VantarKey.ADMIN_FAIL_MSG, patch.fail);
                }
                ui.addKeyValue(VantarKey.ADMIN_SUCCESS, patch.successCount);
                if (ObjectUtil.isNotEmpty(patch.success)) {
                    ui.addKeyValue(VantarKey.ADMIN_SUCCESS_MSG, patch.success);
                }
                ui.blockEnd();
            }
        } catch (VantarException ignore) {

        }

        for (Class<?> cls : ClassUtil.getClasses(packageName, PatchManual.class)) {
            ui  .beginBox(cls.getName())
                .addHrefNewPage(VantarKey.ADMIN_SCHEDULE_RUN, "/admin/patch/run?c=" + cls.getName())
                .blockEnd();
        }

        ui.finish();
    }

    public static void run(Params params, HttpServletResponse response) throws FinishException, VantarException {
        String classNameMethodName = params.getStringRequired("c");
        WebUi ui = Admin.getUi(VantarKey.ADMIN_MENU_SCHEDULE, params, response, true);
        ui  .beginBox(classNameMethodName).write()
            .addMessage("running...").write();

        PatchHistory patch = Patcher.execPatchManually(ui, classNameMethodName);
        ui.addMessage("finished!").write();

        ui  .addKeyValue(VantarKey.ADMIN_RUN_TIME, new DateTime())
            .addKeyValue(VantarKey.ADMIN_FAIL, patch.failCount);
        if (ObjectUtil.isNotEmpty(patch.fail)) {
            ui.addKeyValue(VantarKey.ADMIN_FAIL_MSG, patch.fail);
        }
        ui.addKeyValue(VantarKey.ADMIN_SUCCESS, patch.successCount);
        if (ObjectUtil.isNotEmpty(patch.success)) {
            ui.addKeyValue(VantarKey.ADMIN_SUCCESS_MSG, patch.success);
        }

        ui.finish();
    }
}
