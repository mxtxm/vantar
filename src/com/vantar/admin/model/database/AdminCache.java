package com.vantar.admin.model.database;

import com.vantar.admin.model.index.Admin;
import com.vantar.database.dto.Dto;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.service.healthmonitor.ServiceHealthMonitor;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


@SuppressWarnings("unchecked")
public class AdminCache {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(Locale.getString(VantarKey.ADMIN_CACHE)), params, response, true);

        ServiceDtoCache serviceDtoCache;
        try {
            serviceDtoCache = Services.getService(ServiceDtoCache.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        Set<Class<?>> classes = serviceDtoCache.getCachedClasses();
        if (classes == null) {
            ui.addErrorMessage(Locale.getString(VantarKey.NO_CONTENT)).finish();
            return;
        }

        try {
            ServiceHealthMonitor monitor = Services.getService(ServiceHealthMonitor.class);
            ServiceHealthMonitor.MemoryStatus mStatus = monitor.getMemoryStatus();
            if (!mStatus.ok) {
                ui.addErrorMessage("WARNING! LOW MEMORY");
            }
            ui  .addKeyValue("Designated memory", NumberUtil.getReadableByteSize(mStatus.max))
                .addKeyValue("Allocated memory", NumberUtil.getReadableByteSize(mStatus.total))
                .addKeyValue("Free memory", NumberUtil.getReadableByteSize(mStatus.free))
                .addKeyValue("Used memory", NumberUtil.getReadableByteSize(mStatus.used))
                .addKeyValue(
                    "Physical memory",
                    NumberUtil.getReadableByteSize(mStatus.physicalFree)
                        + " / " + NumberUtil.getReadableByteSize(mStatus.physicalTotal)
                )
                .addKeyValue(
                    "Swap memory",
                    NumberUtil.getReadableByteSize(mStatus.swapFree)
                        + " / " + NumberUtil.getReadableByteSize(mStatus.swapTotal)
                );
        } catch (ServiceException ignore) {

        }

        long sum = 0L;
        for (Class<?> c : classes) {
            Map<Long, ? extends Dto> values = serviceDtoCache.getMap((Class<? extends Dto>) c);
            sum += ObjectUtil.sizeOf(values);
            ui.addHrefBlock(c.getName() + " (" + ObjectUtil.sizeOfReadable(values) + ")", "/admin/cache/view?c=" + c.getName());
        }

        ui.addEmptyLine().addEmptyLine().addMessage((sum / 1024) + "KB");
        ui.finish();
    }

    public static void view(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(Locale.getString(VantarKey.ADMIN_CACHE)), params, response, true);

        String className = params.getString("c");
        if (className == null) {
            ui.addErrorMessage(Locale.getString(VantarKey.NO_CONTENT)).finish();
            return;
        }

        ui.addHeading(3, className);

        Object object = ClassUtil.getInstance(className);
        ServiceDtoCache serviceDtoCache;
        try {
            serviceDtoCache = Services.getService(ServiceDtoCache.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }
        if (object == null || serviceDtoCache == null) {
            ui.addErrorMessage(Locale.getString(VantarKey.NO_CONTENT)).finish();
            return;
        }

        Map<Long, ? extends Dto> values = serviceDtoCache.getMap((Class<? extends Dto>) object.getClass());
        if (values == null) {
            ui.addErrorMessage(Locale.getString(VantarKey.NO_CONTENT)).finish();
            return;
        }

        ui.addEmptyLine().addBlock("pre", ObjectUtil.sizeOfReadable(values)).addEmptyLine();

        for (Map.Entry<Long, ? extends Dto> entry : values.entrySet()) {
            Dto dto = entry.getValue();
            ui.addBlock("pre", dto.toString()).write();
        }

        ui.finish();
    }

    public static void refresh(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(Locale.getString(VantarKey.ADMIN_CACHE)), params, response, true);
        ui.finish();
    }
}
