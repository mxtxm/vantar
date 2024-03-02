package com.vantar.admin.modelw.queue;

import com.vantar.admin.model.index.Admin;
import com.vantar.common.*;
import com.vantar.exception.FinishException;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.queue.Queue;
import com.vantar.service.Services;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminQueue {

    public static void status(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_QUEUE_STATUS, params, response, true);
        if (!Services.isUp(Queue.class)) {
            if (Services.isDependencyEnabled(Queue.class)) {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ENABLED, "RabbitMQ"));
            } else {
                ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_DISABLED, "RabbitMQ"));
            }
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "RabbitMQ"));
            ui.finish();
            return;
        }
        ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_ON, "RabbitMQ")).write();

        String[] queues = Queue.connection.getQueues();
        if (queues == null) {
            ui.addMessage(VantarKey.ADMIN_NO_QUEUE);
        } else {
            for (String q : queues) {
                String queueName = StringUtil.split(q, VantarParam.SEPARATOR_COMMON)[0];
                ui.addKeyValue(queueName, Queue.count(queueName) + " items");
            }
        }

        ui  .addEmptyLine(3)
            .addHrefBlock(VantarKey.ADMIN_DELETE_OPTIONAL, "/admin/queue/purge/selective")
            .addHrefBlock(VantarKey.ADMIN_DELETE_ALL, "/admin/queue/purge");

        ui.finish();
    }

    public static void purge(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_DELETE_QUEUE, params, response, true);
        if (!Services.isUp(Queue.class)) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "RabbitMQ")).finish();
            return;
        }

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addInput(VantarKey.ADMIN_DELAY, "delay", 1, null, "ltr")
                .addInput(VantarKey.ADMIN_TRIES, "tries", 100, null, "ltr")
                .addInput(VantarKey.ADMIN_QUEUE_DELETE_EXCLUDE, "exclude", null, null, "ltr")
                .addSubmit(VantarKey.ADMIN_DELETE)
                .finish();
            return;
        }

        purge(ui, params.getInteger("delay"), params.getInteger("tries"), params.getStringSet("exclude"));
    }

    public static void purgeSelective(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_DELETE_QUEUE, params, response, true);
        if (!Services.isUp(Queue.class)) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "RabbitMQ")).finish();
            return;
        }

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addInput(VantarKey.ADMIN_DELAY, "delay", 1, null, "ltr")
                .addInput(VantarKey.ADMIN_TRIES, "tries", 100, null, "ltr")
                .addHeading(2, VantarKey.ADMIN_QUEUE_DELETE_INCLUDE);

            for (String queueName : Queue.connection.getQueues()) {
                ui.addCheckbox(queueName, queueName);
            }

            ui  .addSubmit(VantarKey.ADMIN_DELETE)
                .finish();
            return;
        }

        Set<String> include = new HashSet<>(10, 1);
        for (String queueName : Queue.connection.getQueues()) {
            if (params.isChecked(queueName)) {
                include.add(queueName);
            }
        }

        purgeSelective(ui, params.getInteger("delay"), params.getInteger("tries"), include);
    }

    public static void purge(WebUi ui, int delay, int maxTries, Set<String> exclude) {
        if (!Services.isUp(Queue.class)) {
            return;
        }

        ui.beginBox(VantarKey.ADMIN_DELETE_QUEUE).write();

        String[] queues = Queue.connection.getQueues();
        if (queues == null) {
            ui.addMessage(VantarKey.ADMIN_NO_QUEUE);
            ui.blockEnd().blockEnd().write();
            return;
        }

        for (String q : queues) {
            String queueName = StringUtil.split(q, VantarParam.SEPARATOR_COMMON)[0];
            if (exclude != null && exclude.contains(queueName)) {
                ui.addKeyValue(queueName, VantarKey.ADMIN_IGNORE).write();
                continue;
            }

            long count = Queue.count(queueName);
            long total = count;
            int tryCount = 1;
            String msg = "";
            while (count > 0 && tryCount++ <= maxTries) {
                msg = Queue.delete(queueName) ? Locale.getString(VantarKey.DELETE_SUCCESS) : Locale.getString(VantarKey.DELETE_FAIL);
                ui.sleep(delay * 1000);
                count = Queue.count(queueName);
            }
            ui.addKeyValue(queueName, msg + " : " + total + " > " + count).write();
        }

        ui.blockEnd().blockEnd().write();
    }

    private static void purgeSelective(WebUi ui, int delay, int maxTries, Set<String> include) {
        if (!Services.isUp(Queue.class)) {
            return;
        }

        ui.beginBox(VantarKey.ADMIN_DELETE_QUEUE).write();

        for (String queueName : include) {
            long count = Queue.count(queueName);
            long total = count;
            int tryCount = 1;
            String msg = "";
            while (count > 0 && tryCount++ <= maxTries) {
                msg = Queue.delete(queueName) ?
                    Locale.getString(VantarKey.DELETE_SUCCESS) : Locale.getString(VantarKey.DELETE_FAIL);
                ui.sleep(delay * 1000);
                count = Queue.count(queueName);
            }
            ui.addKeyValue(queueName, msg + " : " + total + " > " + count).write();
        }

        ui.blockEnd().blockEnd().write();
    }
}
