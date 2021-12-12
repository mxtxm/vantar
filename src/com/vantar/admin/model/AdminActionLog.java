package com.vantar.admin.model;

import com.vantar.business.CommonRepoMongo;
import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.service.Services;
import com.vantar.service.auth.CommonUser;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.service.log.dto.UserLog;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminActionLog {

    public static Class<? extends Dto> userClass = null;


    public static void show(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_ACTION_LOG), params, response);
        if (ui == null) {
            return;
        }

        String clazz = params.getString(VantarParam.DTO);
        Long id = params.getLong("id");
        ui.addHeading(clazz + " (" + id + ")");

        QueryBuilder q = new QueryBuilder(new UserLog());
        q.condition().equal("className", clazz);
        q.condition().equal("objectId", id);

        List<UserLog> data;
        try {
            data = CommonRepoMongo.getData(q);
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
            ui.finish();
            return;
        } catch (NoContentException e) {
            ui.addErrorMessage(Locale.getString(VantarKey.NO_CONTENT));
            ui.finish();
            return;
        }

        ServiceDtoCache cache;
        try {
            cache = Services.get(ServiceDtoCache.class);
        } catch (ServiceException e) {
            ui.addErrorMessage(e).write();
            return;
        }

        for (UserLog dto : data) {
            CommonUser user;
            if (cache == null || dto.userId == null) {
                user = null;
            } else {
                Dto u = cache.getDto(userClass, dto.userId);
               user = (CommonUser) u;
            }
            ui.addLogRow(dto, user);
        }

        ui.finish();
    }
}