package com.vantar.admin.model.schedule;

import com.vantar.admin.model.index.Admin;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.log.*;
import com.vantar.service.scheduler.ServiceScheduler;
import com.vantar.util.collection.*;
import com.vantar.util.datetime.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.*;


public class AdminSchedule {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_MENU_SCHEDULE, params, response, true);
        ServiceScheduler service = Services.get(ServiceScheduler.class);
        if (service == null || !service.isUp()) {
            ui.addErrorMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, "ServiceScheduler")).finish();
            return;
        }

        for (ServiceScheduler.ScheduleInfo info : service.getToRuns()) {
            StringBuilder startAtComment = new StringBuilder();
            startAtComment.append(info.startAt).append(" ");
            int c = StringUtil.countMatches(info.startAt, ':');
            if (c == 1) {
                startAtComment.append("(H:M)");
            } else if (c == 2) {
                startAtComment.append("(H:M:S)");
            } else {
                startAtComment.append("(after service startup)");
            }

            StringBuilder repeatAtComment = new StringBuilder(20);
            repeatAtComment.append(info.repeatAt).append(" ");
            if (info.repeat) {
                c = StringUtil.countMatches(info.repeatAt, ':');
                if (c == 1) {
                    repeatAtComment.append("(H:M)");
                } else if (c == 2) {
                    repeatAtComment.append("(H:M:S)");
                } else {
                    repeatAtComment.append("(every)");
                }
            }

            DateTime time = Beat.getBeat(ServiceScheduler.class, "run:" + info.getName());

            ui  .beginBox(info.getName())
                .addHrefNewPage(VantarKey.ADMIN_SCHEDULE_RUN, "/admin/schedule/run?cm=" + info.getName())
                .addKeyValue(VantarKey.ADMIN_SCHEDULE_START_AT, startAtComment)
                .addKeyValue(VantarKey.ADMIN_SCHEDULE_REPEAT_AT, repeatAtComment)
                .addKeyValue(VantarKey.ADMIN_SERVICES_BEAT, time == null ? "-" : time.toString() + " ("
                    + DateTimeFormatter.secondsToDateTime(Math.abs(time.secondsFromNow())) + ")");

            ui.blockEnd();
        }

        ui.finish();
    }

    public static void run(Params params, HttpServletResponse response) throws FinishException, VantarException {
        String classNameMethodName = params.getStringRequired("cm");
        WebUi ui = Admin.getUi(VantarKey.ADMIN_MENU_SCHEDULE, params, response, true);
        ui.beginBox(classNameMethodName).write();

        String[] cm = StringUtil.split(classNameMethodName, '.');
        try {
            Class<?> tClass = Class.forName(ExtraUtils.join(cm, '.', cm.length-1));
            Method method = tClass.getMethod(cm[cm.length-1]);
            method.invoke(null);
            Beat.set(ServiceScheduler.class, "run:" + classNameMethodName);
            ui.addMessage(VantarKey.ADMIN_SCHEDULE_RUN_SUCCESS);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            ui.addErrorMessage(e);
        }

        ui.finish();
    }
}
