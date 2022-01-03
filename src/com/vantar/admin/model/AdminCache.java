package com.vantar.admin.model;

import com.vantar.database.dto.Dto;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.util.object.ObjectUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminCache {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(Locale.getString(VantarKey.ADMIN_CACHE)), params, response, true);

        ServiceDtoCache serviceDtoCache;
        try {
            serviceDtoCache = Services.get(ServiceDtoCache.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        Set<Class<?>> classes = serviceDtoCache.getCachedClasses();
        if (classes == null) {
            ui.addErrorMessage(Locale.getString(VantarKey.NO_CONTENT)).finish();
            return;
        }

        long sum = 0L;
        for (Class<?> c : classes) {
            Map<Long, Dto> values = serviceDtoCache.getMap((Class<? extends Dto>) c);
            sum += ObjectUtil.sizeOf(values);
            ui.addBlockLink(c.getName() + " (" + ObjectUtil.sizeOfReadable(values) + ")", "/admin/cache/view?c=" + c.getName());
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

        Object object = ObjectUtil.getInstance(className);
        ServiceDtoCache serviceDtoCache;
        try {
            serviceDtoCache = Services.get(ServiceDtoCache.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }
        if (object == null || serviceDtoCache == null) {
            ui.addErrorMessage(Locale.getString(VantarKey.NO_CONTENT)).finish();
            return;
        }

        Map<Long, Dto> values = serviceDtoCache.getMap((Class<? extends Dto>) object.getClass());
        if (values == null) {
            ui.addErrorMessage(Locale.getString(VantarKey.NO_CONTENT)).finish();
            return;
        }

        ui.addEmptyLine().addPre(ObjectUtil.sizeOfReadable(values)).addEmptyLine();

        for (Map.Entry<Long, Dto> entry : values.entrySet()) {
            Dto dto = entry.getValue();
            ui.addPre(dto.toString()).write();
        }

        ui.finish();
    }

    public static void refresh(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(Locale.getString(VantarKey.ADMIN_CACHE)), params, response, true);
        ui.finish();
    }
}
