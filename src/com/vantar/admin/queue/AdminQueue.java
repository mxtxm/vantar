package com.vantar.admin.queue;

import com.vantar.admin.index.Admin;
import com.vantar.common.*;
import com.vantar.exception.FinishException;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.queue.common.Que;
import com.vantar.service.Services;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminQueue {

    public static void status(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_QUEUE_STATUS, params, response, true);
        if (!Services.isUp(Que.Engine.RABBIT)) {
            if (Services.isEnabled(Que.Engine.RABBIT)) {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ENABLED, Que.Engine.RABBIT.name()));
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_DISABLED, Que.Engine.RABBIT.name()));
            }
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, Que.Engine.RABBIT.name()));
            ui.finish();
            return;
        }
        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ON, Que.Engine.RABBIT.name())).write();

        String[] queues = Que.rabbit.getQueues();
        if (queues == null) {
            ui.addMessage(VantarKey.ADMIN_QUEUE_NO_QUEUE);
        } else {
            for (String q : queues) {
                String queueName = StringUtil.splitTrim(q, VantarParam.SEPARATOR_COMMON)[0];
                ui.addKeyValue(queueName, Que.rabbit.count(queueName) + " items");
            }
        }

        ui  .addEmptyLine(3)
            .addHrefBlock(VantarKey.ADMIN_QUEUE_SELECTIVE_DELETE, "/admin/queue/purge/selective")
            .addHrefBlock(VantarKey.ADMIN_DATA_PURGE, "/admin/queue/purge");

        ui.finish();
    }

    public static void purge(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_QUEUE_DELETE, params, response, true);
        if (!Services.isUp(Que.Engine.RABBIT)) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "RabbitMQ")).finish();
            return;
        }

        if (!params.contains("f")) {
            ui  .beginFormPost()
                .addInput(VantarKey.ADMIN_DELAY, "delay", 1, null, "ltr")
                .addInput(VantarKey.ADMIN_ATTEMPT_COUNT, "tries", 100, null, "ltr")
                .addInput(VantarKey.ADMIN_EXCLUDE, "exclude", null, null, "ltr")
                .addSubmit(VantarKey.ADMIN_DELETE)
                .finish();
            return;
        }

        purge(ui, params.getInteger("delay"), params.getInteger("tries"), params.getStringSet("exclude"));
    }

    public static void purgeSelective(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_QUEUE_DELETE, params, response, true);
        if (!Services.isUp(Que.Engine.RABBIT)) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "RabbitMQ")).finish();
            return;
        }

        if (!params.contains("f")) {
            ui  .beginFormPost()
                .addInput(VantarKey.ADMIN_DELAY, "delay", 1, null, "ltr")
                .addInput(VantarKey.ADMIN_ATTEMPT_COUNT, "tries", 100, null, "ltr")
                .addHeading(2, VantarKey.ADMIN_INCLUDE);

            for (String queueName : Que.rabbit.getQueues()) {
                ui.addCheckbox(queueName, queueName);
            }

            ui  .addSubmit(VantarKey.ADMIN_DELETE)
                .finish();
            return;
        }

        Set<String> include = new HashSet<>(10, 1);
        for (String queueName : Que.rabbit.getQueues()) {
            if (params.isChecked(queueName)) {
                include.add(queueName);
            }
        }

        purgeSelective(ui, params.getInteger("delay"), params.getInteger("tries"), include);
    }

    public static void purge(WebUi ui, int delay, int maxTries, Set<String> exclude) {
        if (!Services.isUp(Que.Engine.RABBIT)) {
            return;
        }

        if (ui != null) {
            ui.beginBox(Que.Engine.RABBIT.name()).write();
        }

        String[] queues = Que.rabbit.getQueues();
        if (queues == null) {
            if (ui != null) {
                ui.addMessage("  > " + VantarKey.ADMIN_QUEUE_NO_QUEUE).blockEnd().write();
            }
            return;
        }

        for (String q : queues) {
            String queueName = StringUtil.splitTrim(q, VantarParam.SEPARATOR_COMMON)[0];
            if (exclude != null && exclude.contains(queueName)) {
                continue;
            }

            long count = Que.rabbit.count(queueName);
            long total = count;
            int tryCount = 1;
            String msg = "";
            while (count > 0 && tryCount++ <= maxTries) {
                msg = Que.rabbit.delete(queueName) ? Locale.getString(VantarKey.SUCCESS_DELETE)
                    : Locale.getString(VantarKey.FAIL_DELETE);
                if (ui != null) {
                    ui.sleepMs(delay * 1000);
                }
                count = Que.rabbit.count(queueName);
            }
            if (ui != null) {
                ui.addKeyValue(queueName, msg + ": " + total + " > " + count).write();
            }
        }
        if (ui != null) {
            ui.blockEnd();
        }
    }

    private static void purgeSelective(WebUi ui, int delay, int maxTries, Set<String> include) {
        if (!Services.isUp(Que.Engine.RABBIT)) {
            return;
        }

        ui.beginBox(VantarKey.ADMIN_QUEUE_DELETE).write();

        for (String queueName : include) {
            long count = Que.rabbit.count(queueName);
            long total = count;
            int tryCount = 1;
            String msg = "";
            while (count > 0 && tryCount++ <= maxTries) {
                msg = Que.rabbit.delete(queueName) ?
                    Locale.getString(VantarKey.SUCCESS_DELETE) : Locale.getString(VantarKey.FAIL_DELETE);
                ui.sleepMs(delay * 1000);
                count = Que.rabbit.count(queueName);
            }
            ui.addKeyValue(queueName, msg + " : " + total + " > " + count).write();
        }

        ui.blockEnd().blockEnd().write();
    }
}
