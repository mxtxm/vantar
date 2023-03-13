package com.vantar.service.healthmonitor;

import com.sun.management.OperatingSystemMXBean;
import com.vantar.common.Settings;
import com.vantar.service.Services;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.ObjectUtil;
import org.slf4j.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;


public class ServiceHealthMonitor implements Services.Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceHealthMonitor.class);

    private ScheduledExecutorService schedule;
    private Event event;

    private int warnMemoryCount;
    private int warnProcessorCount;

    public Boolean onEndSetNull;
    public Integer intervalMin;
    public Long warnFreeDiskBytes;
    public Long warnFreeMemoryBytes;
    public Long warnProcessorMaxPercent;
    public Integer warnMemoryThreshold;
    public Integer warnCpuThreshold;


    public void start() {
        schedule = Executors.newSingleThreadScheduledExecutor();
        schedule.scheduleWithFixedDelay(this::monitor, intervalMin, intervalMin, TimeUnit.MINUTES);
    }

    private void monitor() {
        for (DiskStatus diskStatus : getDiskStatus()) {
            if (!diskStatus.ok) {
                event.warnDiskFreeSpace("DISK SPACE LOW WARNING: " + diskStatus.name + " free-space " + diskStatus.free);
                log.warn(" ! DISK SPACE LOW {}", diskStatus);
            }
        }

        MemoryStatus memoryStatus = getMemoryStatus();
        if (!memoryStatus.ok) {
            ++warnMemoryCount;
        }
        if (warnMemoryCount >= warnMemoryThreshold) {
            warnMemoryCount = 0;
            event.warnMemoryLow("MEMORY LOW WARNING: " + memoryStatus.used + " / " + memoryStatus.total);
            log.warn(" ! MEMORY LOW {}", memoryStatus);
        }

        ProcessorStatus processorStatus = getProcessorStatus();
        if (!processorStatus.ok) {
            ++warnProcessorMaxPercent;
        }
        if (warnProcessorCount >= warnProcessorMaxPercent) {
            warnProcessorCount = 0;
            event.warnMemoryLow("PROCESSOR HIGH LOAD WARNING: jvm=" + processorStatus.jvmLoadPercent
                + " system=" + processorStatus.systemLoadPercent);
            log.warn(" ! PROCESSOR HIGH LOAD {}", processorStatus);
        }
    }

    public void stop() {
        schedule.shutdown();
    }

    public boolean onEndSetNull() {
        return onEndSetNull;
    }

    public ServiceHealthMonitor setEvent(Event event) {
        this.event = event;
        return this;
    }

    public boolean isOk() {
        return true;
    }

    public static List<DiskStatus> getDiskStatus() {
        List<DiskStatus> statuses = new ArrayList<>(5);
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            DiskStatus status = new DiskStatus();
            status.name = root.toString();
            try {
                FileStore store = Files.getFileStore(root);
                status.free = store.getUsableSpace();
                status.total = store.getTotalSpace();
                status.used = status.total - status.free;
                Long threshold = Settings.getValue("service.health.monitor.warn.free.disk.bytes", Long.class);
                status.ok = threshold == null || status.free > threshold;
                statuses.add(status);
            } catch (IOException ignore) {

            }
        }
        return statuses;
    }

    public static MemoryStatus getMemoryStatus() {
        MemoryStatus status = new MemoryStatus();
        status.max = Runtime.getRuntime().maxMemory();
        status.total = Runtime.getRuntime().totalMemory();
        status.free = Runtime.getRuntime().freeMemory();
        status.used = status.total - status.free;
        Long threshold = Settings.getValue("service.health.monitor.warn.free.memory.bytes", Long.class);
        status.ok = threshold == null || status.free > threshold;
        OperatingSystemMXBean os = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        status.physicalTotal = os.getTotalPhysicalMemorySize();
        status.physicalFree = os.getFreePhysicalMemorySize();
        status.swapTotal = os.getTotalSwapSpaceSize();
        status.swapFree = os.getFreeSwapSpaceSize();
        return status;
    }

    public static ProcessorStatus getProcessorStatus() {
        ProcessorStatus status = new ProcessorStatus();
        OperatingSystemMXBean os = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        status.jvmLoadPercent = NumberUtil.round(os.getProcessCpuLoad() * 100D, 1);
        status.systemLoadPercent = NumberUtil.round(os.getSystemCpuLoad() * 100D, 1);
        Long threshold = Settings.getValue("service.health.monitor.warn.processor.max.percent", Long.class);
        status.ok = threshold == null || status.jvmLoadPercent < threshold;
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
        void warnCpuFreeSpace(String msg);
    }
}