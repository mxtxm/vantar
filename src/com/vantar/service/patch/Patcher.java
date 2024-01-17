package com.vantar.service.patch;

import com.vantar.business.ModelMongo;
import com.vantar.common.Settings;
import com.vantar.database.dto.DtoBase;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.NoContentException;
import com.vantar.service.Services;
import com.vantar.service.patch.dto.PatchHistory;
import com.vantar.util.object.*;
import com.vantar.web.WebUi;
import java.util.*;


public class Patcher extends DtoBase {

    public static void run() {
        Services.log.info(" > Patcher");
        String packageName = Settings.config.getProperty("patch.package");
        if (packageName == null) {
            Services.log.info(" < no patches");
            return;
        }

        for (Class<?> cls : ClassUtil.getClasses(packageName, Patch.class)) {
            execPatch(cls.getName());
        }

        for (Class<?> cls : ClassUtil.getClasses(packageName, PatchDelayed.class)) {
            Thread thread = new PatchThread(cls);
            thread.start();
        }

        Services.log.info(" < Patcher");
    }

    private static void execPatch(String className) {
        QueryBuilder q = new QueryBuilder(new PatchHistory());
        q.condition().equal("patchClass", className);
        try {
            ModelMongo.getFirst(q);
        } catch (NoContentException e) {
            execPatchManually(className, null);
        } catch (Exception e) {
            Services.log.error(" ! Patcher {}", className, e);
        }
    }

    public static PatchHistory execPatchManually(String className, WebUi ui) {
        PatchHistory history = new PatchHistory();
        try {
            Services.log.info(" >> running {}", className);

            Object object = ClassUtil.getInstance(className);
            if (!(object instanceof PatchInterface)) {
                Services.log.error(" ! invalid patch {}", className);
                return history;
            }

            PatchInterface patch = (PatchInterface) object;
            if (ui != null) {
                patch.setUi(ui);
            }
            Result result = patch.run();
            if (result == null) {
                return history;
            }

            QueryBuilder q = new QueryBuilder(history);
            q.condition().equal("patchClass", className);
            ModelMongo.deleteNoLog(q);

            history.patchClass = className;
            history.success = result.success;
            history.fail = result.fail;
            history.successCount = result.successCount;
            history.failCount = result.failCount;
            Services.log.info(" << finished {} --> fail={} success={}", className, result.failCount, result.successCount);
            ModelMongo.insertNoLog(history);
        } catch (Throwable t) {
            Services.log.error(" ! Patcher {}", className, t);
        }
        return history;
    }


    private static class PatchThread extends Thread {

        private final Class<?> cls;

        public PatchThread(Class<?> cls) {
            this.cls = cls;
        }

        public void run() {
            execPatch(cls.getName());
        }
    }


    public static class Result {

        private final List<String> success = new ArrayList<>(20);
        private final List<String> fail = new ArrayList<>(20);
        private int successCount;
        private int failCount;

        public Result countSuccess() {
            ++successCount;
            return this;
        }

        public Result addSuccess(String msg) {
            success.add(msg);
            return this;
        }

        public Result addFail(String msg) {
            fail.add(msg);
            return this;
        }

        public Result addFail(Throwable t) {
            fail.add(ObjectUtil.throwableToString(t));
            return this;
        }

        public Result countFail() {
            ++failCount;
            return this;
        }
    }

    public interface PatchInterface {

        void setUi(WebUi ui);

        Patcher.Result run();
    }
}
