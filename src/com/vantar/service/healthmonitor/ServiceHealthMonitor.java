package com.vantar.service.healthmonitor;

import com.sun.management.OperatingSystemMXBean;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.nosql.mongo.MongoConnection;
import com.vantar.database.sql.SqlConnection;
import com.vantar.queue.Queue;
import com.vantar.service.Services;
import com.vantar.service.log.ServiceLog;
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
        serviceUp = true;
        schedule = Executors.newSingleThreadScheduledExecutor();
        schedule.scheduleWithFixedDelay(this::monitor, intervalMin, intervalMin, TimeUnit.MINUTES);
    }

    @Override
    public void stop() {
        schedule.shutdown();
        serviceUp = false;
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
    public List<String> getLogs() {
        return null;
    }

    public ServiceHealthMonitor setEvent(Event event) {
        this.event = event;
        return this;
    }

    // service methods < < <

    private void monitor() {

        // > > > disk
        for (DiskStatus s : getDiskStatus()) {
            if (!s.ok) {
                String msg = "DISK SPACE LOW WARNING: disk=" + s.name + " "
                    + NumberUtil.getReadableByteSize(s.free) + " / " + NumberUtil.getReadableByteSize(s.total);
                event.warnDiskFreeSpace(msg);
                ServiceLog.error(ServiceHealthMonitor.class, msg);
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
        }

        // > > > services
        if (Services.isDependencyEnabled(MongoConnection.class) && !Services.isUp(MongoConnection.class)) {
            String msg = "SERVICE OFF: Mongo";
            event.warnServiceFail(msg);
            ServiceLog.error(ServiceHealthMonitor.class, msg);
        }
        if (Services.isDependencyEnabled(ElasticConnection.class) && !Services.isUp(ElasticConnection.class)) {
            String msg = "SERVICE OFF: Elastic";
            event.warnServiceFail(msg);
            ServiceLog.error(ServiceHealthMonitor.class, msg);
        }
        if (Services.isDependencyEnabled(SqlConnection.class) && !Services.isUp(SqlConnection.class)) {
            String msg = "SERVICE OFF: SQL";
            event.warnServiceFail(msg);
            ServiceLog.error(ServiceHealthMonitor.class, msg);
        }
        if (Services.isDependencyEnabled(Queue.class) && !Services.isUp(Queue.class)) {
            String msg = "SERVICE OFF: Queue";
            event.warnServiceFail(msg);
            ServiceLog.error(ServiceHealthMonitor.class, msg);
        }

        for (Services.Service service : Services.getServices()) {
            if (!service.isUp()) {
                String msg = "SERVICE OFF: " + service;
                event.warnServiceFail(msg);
                ServiceLog.error(ServiceHealthMonitor.class, msg);
            } else if (!service.isOk()) {
                String msg = "SERVICE FAIL: " + service;
                event.warnServiceFail(msg);
                ServiceLog.error(ServiceHealthMonitor.class, msg);
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