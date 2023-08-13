package com.vantar.admin.model;

import com.vantar.business.CommonModelMongo;
import com.vantar.common.Settings;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.patch.*;
import com.vantar.service.patch.dto.PatchHistory;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


public class AdminPatch {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_MENU_PATCH), params, response, true);

        String packageName = Settings.config.getProperty("patch.package");
        if (packageName == null) {
            ui.addMessage("No patches!").finish();
            return;
        }

        try {
            List<PatchHistory> patches = CommonModelMongo.getAll(new PatchHistory());
            for (PatchHistory patch : patches) {
                ui  .beginBox(patch.patchClass)
                    .addLinkNewPage(
                        Locale.getString(VantarKey.ADMIN_SCHEDULE_RUN),
                        "patch/run?c=" + patch.patchClass
                    )
                    .addKeyValue(Locale.getString(VantarKey.ADMIN_RUN_TIME), patch.executedTime)
                    .addKeyValue(Locale.getString(VantarKey.ADMIN_FAIL), patch.failCount);
                    if (ObjectUtil.isNotEmpty(patch.fail)) {
                        ui.addKeyValue(Locale.getString(VantarKey.ADMIN_FAIL_MSG), patch.fail);
                    }
                    ui.addKeyValue(Locale.getString(VantarKey.ADMIN_SUCCESS), patch.successCount);
                    if (ObjectUtil.isNotEmpty(patch.success)) {
                        ui.addKeyValue(Locale.getString(VantarKey.ADMIN_SUCCESS_MSG), patch.success);
                    }
                    ui.containerEnd();
            }
        } catch (VantarException ignore) {

        }

        for (Class<?> cls : ClassUtil.getClasses(packageName, PatchManual.class)) {
            ui  .beginBox(cls.getName())
                .addLinkNewPage(
                    Locale.getString(VantarKey.ADMIN_SCHEDULE_RUN),
                    "patch/run?c=" + cls.getName()
                )
                .containerEnd();
        }

        ui.finish();
    }

    public static void run(Params params, HttpServletResponse response) throws FinishException {
        String classNameMethodName = params.getString("c");
        if (classNameMethodName == null) {
            Response.notFound(response, "c missing");
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_MENU_SCHEDULE), params, response, true);
        ui  .beginBox(classNameMethodName).write()
            .addMessage("running...").write();
        PatchHistory patch = Patcher.execPatchManually(classNameMethodName, ui);
        ui.addMessage("finished!").write();

        ui  .addKeyValue(Locale.getString(VantarKey.ADMIN_RUN_TIME), new DateTime())
            .addKeyValue(Locale.getString(VantarKey.ADMIN_FAIL), patch.failCount);
        if (ObjectUtil.isNotEmpty(patch.fail)) {
            ui.addKeyValue(Locale.getString(VantarKey.ADMIN_FAIL_MSG), patch.fail);
        }
        ui.addKeyValue(Locale.getString(VantarKey.ADMIN_SUCCESS), patch.successCount);
        if (ObjectUtil.isNotEmpty(patch.success)) {
            ui.addKeyValue(Locale.getString(VantarKey.ADMIN_SUCCESS_MSG), patch.success);
        }

        ui.finish();
    }
}
