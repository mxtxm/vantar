package com.vantar.admin.model;

import com.vantar.common.*;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.queue.Queue;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminQueue {

    private static final String PARAM_EXCLUDE = "exclude";
    private static final String PARAM_DELAY = "delay";
    private static final String PARAM_TRIES = "tries";
    private static final int DB_DELETE_TRIES = 100;
    private static final int DELAY = 1000;


    public static void purge(Params params, HttpServletResponse response) {
        if (!Queue.isUp) {
            return;
        }

        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_DELETE_QUEUE), params, response);
        if (ui == null) {
            return;
        }

        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addInput(Locale.getString(VantarKey.ADMIN_DELAY), PARAM_DELAY, Integer.toString(DELAY), "ltr")
                .addInput(Locale.getString(VantarKey.ADMIN_TRIES), PARAM_TRIES, Integer.toString(DB_DELETE_TRIES), "ltr")
                .addInput(Locale.getString(VantarKey.ADMIN_QUEUE_DELETE_EXCLUDE), PARAM_EXCLUDE, "", "ltr")
                .addSubmit(Locale.getString(VantarKey.ADMIN_DELETE))
                .finish();
            return;
        }

        purge(ui, params.getInteger(PARAM_DELAY), params.getInteger(PARAM_TRIES), params.getStringSet(PARAM_EXCLUDE));
    }

    public static void purgeSelective(Params params, HttpServletResponse response) {
        if (!Queue.isUp) {
            return;
        }

        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_DELETE_QUEUE), params, response);
        if (ui == null) {
            return;
        }
        if (!params.isChecked("f")) {
            ui  .beginFormPost()
                .addInput(Locale.getString(VantarKey.ADMIN_DELAY), PARAM_DELAY, Integer.toString(DELAY), "ltr")
                .addInput(Locale.getString(VantarKey.ADMIN_TRIES), PARAM_TRIES, Integer.toString(DB_DELETE_TRIES), "ltr")
                .addHeading(Locale.getString(VantarKey.ADMIN_QUEUE_DELETE_INCLUDE));

            for (String s : StringUtil.split(Settings.queue().getRabbitMqQueues(), VantarParam.SEPARATOR_BLOCK)) {
                String queueName = StringUtil.split(s, VantarParam.SEPARATOR_COMMON)[0];
                ui.addCheckbox(queueName, queueName);
            }

            ui  .addSubmit(Locale.getString(VantarKey.ADMIN_DELETE))
                .finish();
            return;
        }

        Set<String> include = new HashSet<>();
        for (String s : StringUtil.split(Settings.queue().getRabbitMqQueues(), VantarParam.SEPARATOR_BLOCK)) {
            String queueName = StringUtil.split(s, VantarParam.SEPARATOR_COMMON)[0];
            if (params.isChecked(queueName)) {
                include.add(queueName);
            }
        }

        purgeSelective(ui, params.getInteger(PARAM_DELAY), params.getInteger(PARAM_TRIES), include);
    }

    public static void purge(WebUi ui, int delay, int maxTries, Set<String> exclude) {
        if (!Queue.isUp) {
            return;
        }

        ui.beginBox(Locale.getString(VantarKey.ADMIN_DELETE_QUEUE)).write();

        String[] queues = StringUtil.split(Settings.queue().getRabbitMqQueues(), VantarParam.SEPARATOR_BLOCK);
        if (queues == null) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_NO_QUEUE));
            return;
        }

        for (String q : queues) {
            String queueName = StringUtil.split(q, VantarParam.SEPARATOR_COMMON)[0];
            if (exclude != null && exclude.contains(queueName)) {
                ui.addKeyValue(queueName, Locale.getString(VantarKey.ADMIN_IGNORE)).write();
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

        ui.containerEnd().containerEnd().write();
    }

    public static void purgeSelective(WebUi ui, int delay, int maxTries, Set<String> include) {
        if (!Queue.isUp) {
            return;
        }

        ui.beginBox(Locale.getString(VantarKey.ADMIN_DELETE_QUEUE)).write();

        for (String queueName : include) {
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

        ui.containerEnd().containerEnd().write();
    }

    public static void status(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUiAdminAccess(Locale.getString(VantarKey.ADMIN_QUEUE_STATUS), params, response);
        if (ui == null) {
            return;
        }

        if (Queue.isUp) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_RABBIT_IS_ON));
        } else {
            ui.addErrorMessage(Locale.getString(VantarKey.ADMIN_RABBIT_IS_OFF));
        }

        ui.write();

        String[] queues = StringUtil.split(Settings.queue().getRabbitMqQueues(), VantarParam.SEPARATOR_BLOCK);
        if (queues == null) {
            ui.addMessage(Locale.getString(VantarKey.ADMIN_NO_QUEUE));
            return;
        }

        for (String q : queues) {
            String queueName = StringUtil.split(q, VantarParam.SEPARATOR_COMMON)[0];
            ui.addKeyValue(queueName, Queue.count(queueName) + " items");
        }

        ui.containerEnd().write();
    }
}
