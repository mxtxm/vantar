package com.vantar.admin.database.cache;

import com.vantar.admin.index.Admin;
import com.vantar.admin.service.AdminService;
import com.vantar.database.dto.*;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@SuppressWarnings("unchecked")
public class AdminDtoCache {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_CACHE, params, response, true);

        ServiceDtoCache cache;
        try {
            cache = Services.getService(ServiceDtoCache.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        Set<Class<?>> classes = cache.getCachedClasses();
        if (classes == null) {
            ui.addErrorMessage(VantarKey.NO_CONTENT).finish();
            return;
        }

        ui.addHeading(2, VantarKey.ADMIN_CACHE);

        long sum = 0L;
        for (Class<?> c : classes) {
            Map<Long, ? extends Dto> values = cache.getMap((Class<? extends Dto>) c);
            sum += ObjectUtil.sizeOf(values);
            ui.addKeyValue(
                ui.getHref(c.getSimpleName(), "/admin/database/cache/view?dto=" + c.getSimpleName(), true, false, null),
                ui.getHref(VantarKey.ADMIN_REFRESH, "/admin/database/cache/refresh?dto=" + c.getSimpleName(), true, false, null) + " "
                    + ObjectUtil.sizeOfReadable(values),
                null,
                false
            );
        }
        ui.addKeyValue("", NumberUtil.getReadableByteSize(sum));

        ui.addHeading(2, VantarKey.ADMIN_MEMORY);
        AdminService.plotMemoryStatus(ui);

        ui.finish();
    }

    public static void view(Params params, HttpServletResponse response) throws FinishException, InputException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_CACHE, params, response, true);

        ServiceDtoCache cache;
        try {
            cache = Services.getService(ServiceDtoCache.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        String dtoClass = params.getStringRequired("dto");
        ui.addHeading(2, dtoClass);
        DtoDictionary.Info object = DtoDictionary.get(dtoClass);
        if (object == null) {
            ui.addErrorMessage(VantarKey.NO_CONTENT).finish();
            return;
        }

        Map<Long, ? extends Dto> values = cache.getMap(object.dtoClass);
        if (values == null) {
            ui.addErrorMessage(VantarKey.NO_CONTENT).finish();
            return;
        }

        ui.addEmptyLine().addBlock("pre", ObjectUtil.sizeOfReadable(values)).addEmptyLine();

        for (Map.Entry<Long, ? extends Dto> entry : values.entrySet()) {
            Dto dto = entry.getValue();
            ui.addBlock("pre", dto.toString()).write();
        }

        ui.finish();
    }

    public static void refresh(Params params, HttpServletResponse response) throws FinishException, InputException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_CACHE, params, response, true);

        ServiceDtoCache cache;
        try {
            cache = Services.getService(ServiceDtoCache.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        String dtoClass = params.getStringRequired("dto");
        ui.addHeading(2, dtoClass);
        DtoDictionary.Info object = DtoDictionary.get(dtoClass);
        if (object == null) {
            ui.addErrorMessage(VantarKey.NO_CONTENT).finish();
            return;
        }

        cache.update(dtoClass);

        ui.addMessage(VantarKey.SUCCESS_UPDATE).finish();
    }
}
