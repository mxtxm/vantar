package com.vantar.admin.model;

import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.log.LogEvent;
import com.vantar.service.scheduler.ServiceScheduler;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.*;


public class AdminSchedule {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_MENU_SCHEDULE), params, response, true);

        ServiceScheduler service;
        try {
            service = Services.get(ServiceScheduler.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).write();
            return;
        }

        if (!Services.isUp(ServiceScheduler.class) || service == null) {
            ui.addErrorMessage(Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF)).write();
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

            StringBuilder repeatAtComment = new StringBuilder();
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

            DateTime time = LogEvent.getBeat(ServiceScheduler.class, "run:" + info.getName());

            ui  .beginBox(info.getName())
                .addLinkNewPage(
                    Locale.getString(VantarKey.ADMIN_SCHEDULE_RUN),
                    "schedule/run?cm=" + info.getName()
                )
                .addKeyValue(Locale.getString(VantarKey.ADMIN_SCHEDULE_START_AT), startAtComment)
                .addKeyValue(Locale.getString(VantarKey.ADMIN_SCHEDULE_REPEAT_AT), repeatAtComment)
                .addKeyValue(
                    Locale.getString(VantarKey.ADMIN_SERVICES_LAST_RUN),
                    time == null ? "-" : time.toString() + " ("
                        + DateTimeFormatter.secondsToDateTime(Math.abs(time.secondsFromNow())) + ")"
                );

            ui.containerEnd();
        }

        ui.finish();
    }

    public static void run(Params params, HttpServletResponse response) throws FinishException {
        String classNameMethodName = params.getString("cm");
        if (classNameMethodName == null) {
            Response.notFound(response, "cm missing");
            return;
        }

        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_MENU_SCHEDULE), params, response, true);

        ui.beginBox(classNameMethodName);

        String[] cm = StringUtil.split(classNameMethodName, '.');
        try {
            Class<?> tClass = Class.forName(CollectionUtil.join(cm, '.', cm.length-1));
            Method method = tClass.getMethod(cm[cm.length-1]);
            method.invoke(null);

            LogEvent.beat(ServiceScheduler.class, "run:" + classNameMethodName);
            ui.addMessage(Locale.getString(VantarKey.ADMIN_SCHEDULE_RUN_SUCCESS));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            ui.addErrorMessage(Locale.getString(VantarKey.ADMIN_SCHEDULE_RUN_FAIL));
            ui.addErrorMessage(e);
        }

        ui.write();
    }
}
