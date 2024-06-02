package com.vantar.service.healthmonitor;

import com.sun.management.OperatingSystemMXBean;
import com.vantar.database.common.Db;
import com.vantar.queue.common.Que;
import com.vantar.service.Services;
import com.vantar.service.log.*;
import com.vantar.util.datetime.DateTimeFormatter;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.ObjectUtil;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;


public class ServiceHealthMonitor implements Services.Service {

    private volatile boolean serviceUp = false;
    private ScheduledExecutorService schedule;
    private Event event;

    private volatile boolean pause = false;
    private List<String> lastMessages;
    private boolean lastWasOk = true;

    // warn only if repeated monitoring fails
    private int warnMemoryCount;
    private int warnProcessorCount;

    // > > > service params injected from config
    public Integer intervalMin;
    public Long warnFreeDiskBytes;
    public Long warnFreeMemoryBytes;
    public Long warnProcessorMaxPercent;
    // warn only if repeated monitoring fails
    public Integer warnMemoryThreshold;
    public Integer warnProcessorThreshold;
    // < < <

    // > > > service methods

    @Override
    public void start() {
        schedule = Executors.newSingleThreadScheduledExecutor();
        schedule.scheduleWithFixedDelay(this::monitor, intervalMin, intervalMin, TimeUnit.MINUTES);
        serviceUp = true;
        pause = false;
    }

    @Override
    public void stop() {
        schedule.shutdown();
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
        return serviceUp
            && schedule != null
            && !schedule.isShutdown()
            && !schedule.isTerminated();
    }

    @Override
    public boolean isPaused() {
        return pause;
    }

    @Override
    public List<String> getLogs() {
        return null;
    }

    public ServiceHealthMonitor setEvent(Event event) {
        this.event = event;
        return this;
    }

    // service methods < < <

    private void monitor() {
        if (pause) {
            return;
        }

        lastWasOk = true;
        lastMessages = new ArrayList<>(10);

        // > > > disk
        for (DiskStatus s : getDiskStatus()) {
            if (!s.ok) {
                String msg = "DISK SPACE LOW WARNING: disk=" + s.name + " "
                    + NumberUtil.getReadableByteSize(s.free) + " / " + NumberUtil.getReadableByteSize(s.total);
                event.warnDiskFreeSpace(msg);
                ServiceLog.error(ServiceHealthMonitor.class, msg);
                lastWasOk = false;
                lastMessages.add(msg);
            }
        }

        // > > > memory
        MemoryStatus memoryStatus = getMemoryStatus();
        if (!memoryStatus.ok) {
            ++warnMemoryCount;
        }
        if (warnMemoryCount >= warnMemoryThreshold) {
            warnMemoryCount = 0;
            String msg = "MEMORY LOW WARNING: " + NumberUtil.getReadableByteSize(memoryStatus.used)
                + " / " + NumberUtil.getReadableByteSize(memoryStatus.total);
            event.warnMemoryLow(msg);
            ServiceLog.error(ServiceHealthMonitor.class, msg);
            lastWasOk = false;
            lastMessages.add(msg);
        }

        // > > > processor
        ProcessorStatus processorStatus = getProcessorStatus();
        if (!processorStatus.ok) {
            ++warnProcessorMaxPercent;
        }
        if (warnProcessorCount >= warnProcessorThreshold) {
            warnProcessorCount = 0;
            String msg = "PROCESSOR HIGH LOAD WARNING: jvm=" + processorStatus.jvmLoadPercent
                + " system=" + processorStatus.systemLoadPercent;
            event.warnProcessorBusy(msg);
            ServiceLog.error(ServiceHealthMonitor.class, msg);
            lastWasOk = false;
            lastMessages.add(msg);
        }

        // > > > services
        if (Services.isEnabled(Que.Engine.RABBIT) && !Services.isUp(Que.Engine.RABBIT)) {
            String msg = "SERVICE OFF: Que";
            event.warnServiceFail(msg);
            ServiceLog.error(ServiceHealthMonitor.class, msg);
            lastWasOk = false;
            lastMessages.add(msg);
        }
        if (Services.isEnabled(Db.Dbms.MONGO) && !Services.isUp(Db.Dbms.MONGO)) {
            String msg = "SERVICE OFF: Mongo";
            event.warnServiceFail(msg);
            ServiceLog.error(ServiceHealthMonitor.class, msg);
            lastWasOk = false;
            lastMessages.add(msg);
        }
        if (Services.isEnabled(Db.Dbms.SQL) && !Services.isUp(Db.Dbms.SQL)) {
            String msg = "SERVICE OFF: SQL";
            event.warnServiceFail(msg);
            ServiceLog.error(ServiceHealthMonitor.class, msg);
            lastWasOk = false;
            lastMessages.add(msg);
        }
        if (Services.isEnabled(Db.Dbms.ELASTIC) && !Services.isUp(Db.Dbms.ELASTIC)) {
            String msg = "SERVICE OFF: Elastic";
            event.warnServiceFail(msg);
            ServiceLog.error(ServiceHealthMonitor.class, msg);
            lastWasOk = false;
            lastMessages.add(msg);
        }

        Collection<Services.Service> services = Services.getServices();
        if (services != null) {
            for (Services.Service service : services) {
                if (!service.isUp()) {
                    String msg = "SERVICE OFF: " + service.getClass().getSimpleName();
                    event.warnServiceFail(msg);
                    ServiceLog.error(ServiceHealthMonitor.class, msg);
                    lastWasOk = false;
                    lastMessages.add(msg);
                } else if (!service.isOk()) {
                    String msg = "SERVICE FAIL: " + service.getClass().getSimpleName();
                    event.warnServiceFail(msg);
                    ServiceLog.error(ServiceHealthMonitor.class, msg);
                    lastWasOk = false;
                    lastMessages.add(msg);
                }
            }
        }
    }

    public List<DiskStatus> getDiskStatus() {
        List<DiskStatus> statuses = new ArrayList<>(5);
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            DiskStatus status = new DiskStatus();
            status.name = root.toString();
            try {
                FileStore store = Files.getFileStore(root);
                status.free = store.getUsableSpace();
                status.total = store.getTotalSpace();
                status.used = status.total - status.free;
                status.ok = warnFreeDiskBytes == null || status.free > warnFreeDiskBytes;
                statuses.add(status);
            } catch (IOException ignore) {

            }
        }
        return statuses;
    }

    public MemoryStatus getMemoryStatus() {
        MemoryStatus status = new MemoryStatus();
        status.max = Runtime.getRuntime().maxMemory();
        status.total = Runtime.getRuntime().totalMemory();
        status.free = Runtime.getRuntime().freeMemory();
        status.used = status.total - status.free;
        status.ok = warnFreeMemoryBytes == null || status.free > warnFreeMemoryBytes;
        OperatingSystemMXBean os = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        status.physicalTotal = os.getTotalPhysicalMemorySize();
        status.physicalFree = os.getFreePhysicalMemorySize();
        status.swapTotal = os.getTotalSwapSpaceSize();
        status.swapFree = os.getFreeSwapSpaceSize();
        return status;
    }

    public ProcessorStatus getProcessorStatus() {
        ProcessorStatus status = new ProcessorStatus();
        OperatingSystemMXBean os = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        status.jvmLoadPercent = NumberUtil.round(os.getProcessCpuLoad() * 100D, 1);
        status.systemLoadPercent = NumberUtil.round(os.getSystemCpuLoad() * 100D, 1);
        status.ok = warnProcessorMaxPercent == null || status.jvmLoadPercent < warnProcessorMaxPercent;
        return status;
    }

    public boolean getOverallSystemHealth() {
        return lastWasOk;
    }

    public List<String> getLastErrors() {
        return lastMessages == null ? new ArrayList<>(1) : lastMessages;
    }

    public Map<String, Object> getSystemHealthReport() {
        Map<String, Object> report = new HashMap<>(10, 1);

        // memory
        Map<String, Object> memory = new HashMap<>(8, 1);
        MemoryStatus mStatus = getMemoryStatus();
        memory.put("Status", mStatus.ok);
        memory.put("Designated memory", NumberUtil.getReadableByteSize(mStatus.max));
        memory.put("Allocated memory", NumberUtil.getReadableByteSize(mStatus.total));
        memory.put("Free memory", NumberUtil.getReadableByteSize(mStatus.free));
        memory.put("Used memory", NumberUtil.getReadableByteSize(mStatus.used));
        memory.put(
            "Physical memory",
            NumberUtil.getReadableByteSize(mStatus.physicalFree) + " / " + NumberUtil.getReadableByteSize(mStatus.physicalTotal)
        );
        memory.put(
            "Swap memory",
            NumberUtil.getReadableByteSize(mStatus.swapFree) + " / " + NumberUtil.getReadableByteSize(mStatus.swapTotal)
        );
        report.put("Memory", memory);

        // processor
        Map<String, Object> processor = new HashMap<>(4, 1);
        ProcessorStatus pStatus = getProcessorStatus();
        processor.put("Status", pStatus.ok);
        processor.put("JVM load percent", pStatus.jvmLoadPercent);
        processor.put("System load percent", pStatus.systemLoadPercent);
        report.put("Processor", processor);

        // disk
        Map<String, Map<String, Object>> disks = new HashMap<>(5, 1);
        for (DiskStatus dStatus : getDiskStatus()) {
            Map<String, Object> disk = new HashMap<>(5, 1);
            disk.put("Status", dStatus.ok);
            disk.put("Free", NumberUtil.getReadableByteSize(dStatus.free));
            disk.put("Used", NumberUtil.getReadableByteSize(dStatus.used));
            disk.put("Total", NumberUtil.getReadableByteSize(dStatus.total));
            disks.put(dStatus.name, disk);
        }
        report.put("Disks", disks);

        // services data sources
        Map<String, Map<String, Boolean>> dataSources = new HashMap<>(5, 1);
        // >
        Map<String, Boolean> queue = new HashMap<>(2, 1);
        queue.put("Enabled", Services.isEnabled(Que.Engine.RABBIT));
        queue.put("Up", Services.isUp(Que.Engine.RABBIT));
        dataSources.put(Que.Engine.RABBIT.name(), queue);
        // >
        Map<String, Boolean> mongo = new HashMap<>(2, 1);
        mongo.put("Enabled", Services.isEnabled(Db.Dbms.MONGO));
        mongo.put("Up", Services.isUp(Db.Dbms.MONGO));
        dataSources.put(Db.Dbms.MONGO.name(), mongo);
        // >
        Map<String, Boolean> sql = new HashMap<>(2, 1);
        sql.put("Enabled", Services.isEnabled(Db.Dbms.SQL));
        sql.put("Up", Services.isUp(Db.Dbms.SQL));
        dataSources.put(Db.Dbms.SQL.name(), sql);
        // >
        Map<String, Boolean> elastic = new HashMap<>(2, 1);
        elastic.put("Enabled", Services.isEnabled(Db.Dbms.ELASTIC));
        elastic.put("Up", Services.isUp(Db.Dbms.ELASTIC));
        dataSources.put(Db.Dbms.ELASTIC.name(), elastic);
        // >
        report.put("Data sources (connections)", dataSources);

        // services
        Map<String, Map<String, Object>> services = new HashMap<>(14, 1);
        for (Services.Service service : Services.getServices()) {
            Map<String, Object> s = new HashMap<>(2, 1);
            s.put("Up", service.isOk());
            s.put("Ok", service.isOk());
            s.put("Logs", service.getLogs());
            services.put(service.getClass().getSimpleName(), s);
        }
        report.put("Services", services);

        // beats
        Map<String, Map<String, String>> beats = new HashMap<>(14, 1);
        Beat.getBeats().forEach((service, logs) -> {
            Map<String, String> actions = new HashMap<>(10, 1);
            logs.forEach((comment, time) ->
                actions.put(
                    comment,
                    time.toString() + " (" + DateTimeFormatter.secondsToDateTime(Math.abs(time.secondsFromNow())) + ")"
                )
            );
            beats.put(service.getSimpleName(), actions);
        });
        report.put("Last service action", beats);

        return report;
    }


    public static class DiskStatus {

        public String name;
        public long free;
        public long used;
        public long total;
        public boolean ok;

        public String toString() {
            return ObjectUtil.toString(this);
        }
    }


    public static class MemoryStatus {

        public long physicalTotal;
        public long physicalFree;
        public long swapFree;
        public long swapTotal;
        public long max;
        public long free;
        public long used;
        public long total;
        public boolean ok;

        public String toString() {
            return ObjectUtil.toString(this);
        }
    }


    public static class ProcessorStatus {

        public double jvmLoadPercent;
        public double systemLoadPercent;
        public boolean ok;

        public String toString() {
            return ObjectUtil.toString(this);
        }
    }


    public interface Event {

        void warnDiskFreeSpace(String msg);
        void warnMemoryLow(String msg);
        void warnProcessorBusy(String msg);
        void warnServiceFail(String msg);
    }
}