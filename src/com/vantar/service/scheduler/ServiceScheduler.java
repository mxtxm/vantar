package com.vantar.service.scheduler;

import com.vantar.service.log.*;
import com.vantar.common.VantarParam;
import com.vantar.service.Services;
import com.vantar.util.collection.*;
import com.vantar.util.datetime.*;
import com.vantar.util.string.StringUtil;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;


public class ServiceScheduler implements Services.Service {

    private volatile boolean pause = false;
    private volatile boolean serviceUp = false;
    private volatile boolean lastSuccess = true;
    private List<String> logs;

    private ScheduledExecutorService[] schedules;

    // > > > service params injected from config
    // ClassName.Method,hh:mm;              once at hh:mm
    // ClassName.Method,x(s/m/h);           x seconds/minutes/hours
    // ClassName.Method,hh:mm,repeat;       every hh:mm
    // ClassName.Method,hh:mm,x(s/m/h);     starting from hh:mm, every x seconds/minutes/hours
    // ClassName.Method,x(s/m/h),y(s/m/h);  starting after x seconds/minutes/hours, repeat each x seconds/minutes/hours
    public String schedule;
    // < < <

    // > > > service methods

    @Override
    public void start() {
        if (StringUtil.isEmpty(schedule)) {
            ServiceLog.log.info(" >> schedule service not started because no schedules are available in the settings");
            return;
        }
        serviceUp = true;
        String[] parts = StringUtil.splitTrim(schedule, VantarParam.SEPARATOR_BLOCK);
        schedules = new ScheduledExecutorService[parts.length];
        try {
            setTasks(parts);
        } catch (Exception e) {
            lastSuccess = false;
            setLog(e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (schedules == null) {
            return;
        }
        for (ScheduledExecutorService s : schedules) {
            s.shutdown();
            try {
                s.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {

            }
        }
        serviceUp = false;
    }

    @Override
    public void pause() {
        pause = true;
    }

    @Override
    public void resume() {
        pause = false;
    }

    @Override
    public boolean isUp() {
        return serviceUp;
    }

    @Override
    public boolean isOk() {
        if (!serviceUp || !lastSuccess) {
            return false;
        }
        for (ScheduledExecutorService s : schedules) {
            if (s.isShutdown() || s.isTerminated()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isPaused() {
        return pause;
    }

    @Override
    public List<String> getLogs() {
        return logs;
    }

    private void setLog(String msg) {
        if (logs == null) {
            logs = new ArrayList<>(5);
        }
        logs.add(msg);
    }

    // service methods < < <

    private void setTasks(String[] parts) {
        int i = 0;
        for (String item : parts) {
            String[] classNameOptions = StringUtil.splitTrim(item, VantarParam.SEPARATOR_COMMON);
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

            Beat.set(this.getClass(), "set:" + classNameOptions[0]);

            if (classNameOptions.length == 2) {

                // ClassName.Method,hh:mm;              once at hh:mm
                if (StringUtil.contains(classNameOptions[1], VantarParam.SEPARATOR_KEY_VAL)) {
                    executor.schedule(
                        getRunnable(classNameOptions[0]),
                        getStartSeconds(classNameOptions[1], classNameOptions[0]),
                        TimeUnit.SECONDS
                    );

                } else {

                    // ClassName.Method,x(s/m/h);           x seconds/minutes/hours
                    int startAt = StringUtil.scrapeInteger(classNameOptions[1]);
                    if (StringUtil.contains(classNameOptions[1], 'm')) {
                        startAt *= 60;
                    } else if (StringUtil.contains(classNameOptions[1], 'h')) {
                        startAt *= 360;
                    }
                    executor.schedule(
                        getRunnable(classNameOptions[0]),
                        startAt,
                        TimeUnit.SECONDS
                    );
                    ServiceLog.log.info("  -> scheduled({}, s={}) ", getClassMethodName(classNameOptions[0]), startAt);
                }

            // ClassName.Method,hh:mm,repeat;       every hh:mm
            } else if ("repeat".equalsIgnoreCase(classNameOptions[2])) {
                executor.scheduleAtFixedRate(
                    getRunnable(classNameOptions[0]),
                    getStartSeconds(classNameOptions[1], classNameOptions[0]),
                    TimeUnit.DAYS.toSeconds(1),
                    TimeUnit.SECONDS
                );

            // ClassName.Method,hh:mm,x(s/m/h);     starting from hh:mm, every x seconds/minutes/hours
            } else if (StringUtil.contains(classNameOptions[1], VantarParam.SEPARATOR_KEY_VAL)) {
                int repeatAt = StringUtil.scrapeInteger(classNameOptions[2]);
                if (StringUtil.contains(classNameOptions[1], 'm')) {
                    repeatAt *= 60;
                } else if (StringUtil.contains(classNameOptions[1], 'h')) {
                    repeatAt *= 360;
                }
                executor.scheduleAtFixedRate(
                    getRunnable(classNameOptions[0]),
                    getStartSeconds(classNameOptions[1], classNameOptions[0]),
                    repeatAt,
                    TimeUnit.SECONDS
                );

            // ClassName.Method,x(s/m/h),y(s/m/h);  starting after x seconds/minutes/hours, repeat each x seconds/minutes/hours
            } else {
                int startAt = StringUtil.scrapeInteger(classNameOptions[1]);
                if (StringUtil.contains(classNameOptions[1], 'm')) {
                    startAt *= 60;
                } else if (StringUtil.contains(classNameOptions[1], 'h')) {
                    startAt *= 360;
                }
                int repeatAt = StringUtil.scrapeInteger(classNameOptions[2]);
                if (StringUtil.contains(classNameOptions[1], 'm')) {
                    repeatAt *= 60;
                } else if (StringUtil.contains(classNameOptions[1], 'h')) {
                    repeatAt *= 360;
                }
                executor.scheduleAtFixedRate(
                    getRunnable(classNameOptions[0]),
                    startAt,
                    repeatAt,
                    TimeUnit.SECONDS
                );
                ServiceLog.log.info("  -> scheduled({}, s={} r={}) ", getClassMethodName(classNameOptions[0]), startAt, repeatAt);
            }

            schedules[i++] = executor;
        }
    }

    private long getStartSeconds(String timeHms, String className) {
        String[] time = StringUtil.splitTrim(timeHms, VantarParam.SEPARATOR_KEY_VAL);
        DateTime now = new DateTime();
        DateTime next = new DateTime();
        int hn = now.formatter().hour;
        int mn = now.formatter().minute;
        int sn = now.formatter().second;
        int h = StringUtil.toInteger(time[0]);
        int m = time.length > 1 ? StringUtil.toInteger(time[1]) : 0;
        int s = time.length > 2 ? StringUtil.toInteger(time[2]) : 0;
        if (h > hn) {
            next.addHours(h - hn);
            next.addMinutes(m - mn);
        } else if (h == hn && m > mn) {
            next.addMinutes(m - mn);
        } else if (h == hn && m == mn && s >= sn) {
            next.addSeconds(s - sn + 1);
        } else {
            next.addDays(1);
            next.addHours(h - hn);
            next.addMinutes(m - mn);
            next.addSeconds(s - sn);
        }
        ServiceLog.log.info("  -> scheduled({}, n={}, s={}, r=daily)", getClassMethodName(className), now, next);
        return now.diffSeconds(next);
    }

    private String getClassMethodName(String cm) {
        String[] parts = StringUtil.split(cm, '.');
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    private Runnable getRunnable(String classNameMethodName) {
        return () -> {
            if (pause) {
                return;
            }
            String[] cm = StringUtil.split(classNameMethodName, '.');
            try {
                Class<?> tClass = Class.forName(ExtraUtils.join(cm, '.', cm.length-1));
                Method method = tClass.getMethod(cm[cm.length-1]);
                method.invoke(null);
                Beat.set(this.getClass(), "run:" + classNameMethodName);
            } catch (Exception e) {
                Beat.set(this.getClass(), "fail:" + classNameMethodName);
                ServiceLog.error(ServiceScheduler.class, "runnable failed ({})\n", classNameMethodName, e);
                setLog(classNameMethodName + ": " + e.getMessage());
            }
        };
    }

    public ScheduleInfo[] getToRuns() {
        String[] parts = StringUtil.splitTrim(schedule, VantarParam.SEPARATOR_BLOCK);
        ScheduleInfo[] cms = new ScheduleInfo[parts.length];

        int i = 0;
        for (String item : parts) {
            String[] classNameOptions = StringUtil.splitTrim(item, VantarParam.SEPARATOR_COMMON);
            String[] cm = StringUtil.split(classNameOptions[0], '.');

            ScheduleInfo c = new ScheduleInfo();
            c.className = ExtraUtils.join(cm, '.', cm.length-1);
            c.methodName = cm[cm.length-1];

            if (classNameOptions.length == 2) {
                // ClassName.Method,hh:mm;              once at hh:mm
                // ClassName.Method,hh:mm,repeat;       every hh:mm
                c.startAt = classNameOptions[1];
                c.repeatAt = "-";
                c.repeat = false;
            } else if ("repeat".equalsIgnoreCase(classNameOptions[2])) {
                c.startAt = classNameOptions[1];
                c.repeatAt = classNameOptions[1];
                c.repeat = true;
            // ClassName.Method,hh:mm,x(s/m/h);     starting from hh:mm, every x seconds/minutes/hours
            } else if (StringUtil.contains(classNameOptions[1], VantarParam.SEPARATOR_KEY_VAL)) {
                c.startAt = classNameOptions[1];
                c.repeatAt = classNameOptions[2];
                c.repeat = true;
            // ClassName.Method,x(s/m/h),y(s/m/h);  starting after x seconds/minutes/hours, repeat each x seconds/minutes/hours
            } else {
                c.startAt = classNameOptions[1];
                c.repeatAt = classNameOptions[2];
                c.repeat = true;
            }

            cms[i++] = c;
        }

        return cms;
    }


    public static class ScheduleInfo {

        public String className;
        public String methodName;
        public boolean repeat;
        public String startAt;
        public String repeatAt;

        public String getName() {
            return className + '.' + methodName;
        }
    }
}