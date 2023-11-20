package com.vantar.service.scheduler;

import com.vantar.service.log.LogEvent;
import com.vantar.common.VantarParam;
import com.vantar.service.Services;
import com.vantar.util.collection.*;
import com.vantar.util.datetime.*;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.lang.reflect.*;
import java.util.concurrent.*;


public class ServiceScheduler implements Services.Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceScheduler.class);
    private ScheduledExecutorService[] schedules;

    public Boolean onEndSetNull;

    // ClassName.Method,hh:mm;              once at hh:mm
    // ClassName.Method,x(s/m/h);           x seconds/minutes/hours
    // ClassName.Method,hh:mm,repeat;       every hh:mm
    // ClassName.Method,hh:mm,x(s/m/h);     starting from hh:mm, every x seconds/minutes/hours
    // ClassName.Method,x(s/m/h),y(s/m/h);  starting after x seconds/minutes/hours, repeat each x seconds/minutes/hours
    public String schedule;


    public void start() {
        String[] parts = StringUtil.splitTrim(schedule, VantarParam.SEPARATOR_BLOCK);
        schedules = new ScheduledExecutorService[parts.length];

        int i = 0;
        for (String item : parts) {
            String[] classNameOptions = StringUtil.splitTrim(item, VantarParam.SEPARATOR_COMMON);
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

            LogEvent.beat(this.getClass(), "set:" + classNameOptions[0]);

            if (classNameOptions.length == 2) {
                // ClassName.Method,hh:mm;              once at hh:mm
                if (StringUtil.contains(classNameOptions[1], ':')) {
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
                    log.info("    >> scheduled ({}, startat={}) ", classNameOptions[0], startAt);
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
            } else if (StringUtil.contains(classNameOptions[1], ':')) {
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
                log.info("    >> scheduled ({}, startat={} repeatat={}) ", classNameOptions[0], startAt, repeatAt);
            }

            schedules[i++] = executor;
        }
    }

    private long getStartSeconds(String timeHms, String className) {
        String[] time = StringUtil.split(timeHms, ':');
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
        log.info("    >> scheduled ({}, now={}, start={}, repeat every day)", className, now, next);
        return now.diffSeconds(next);
    }

    public void stop() {
        for (ScheduledExecutorService s : schedules) {
            s.shutdown();
            try {
                s.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {

            }
        }
    }

    public boolean onEndSetNull() {
        return onEndSetNull;
    }

    private Runnable getRunnable(String classNameMethodName) {
        return () -> {
            String[] cm = StringUtil.split(classNameMethodName, '.');
            try {
                Class<?> tClass = Class.forName(ExtraUtils.join(cm, '.', cm.length-1));
                Method method = tClass.getMethod(cm[cm.length-1]);
                method.invoke(null);
                LogEvent.beat(this.getClass(), "run:" + classNameMethodName);
            } catch (Exception e) {
                log.error(" !! runnable failed ({})\n", classNameMethodName, e);
            }
        };
    }

    public ScheduleInfo[] getToRuns() {
        String[] parts = StringUtil.split(schedule, VantarParam.SEPARATOR_BLOCK);
        ScheduleInfo[] cms = new ScheduleInfo[parts.length];

        int i = 0;
        for (String item : parts) {
            String[] classNameOptions = StringUtil.split(item, VantarParam.SEPARATOR_COMMON);
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
            } else if (StringUtil.contains(classNameOptions[1], ':')) {
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

    public boolean isOk() {
        return true;
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