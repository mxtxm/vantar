package com.vantar.admin.model.heath;

import com.vantar.admin.model.index.Admin;
import com.vantar.business.ModelMongo;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.ServiceAuth;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.service.log.dto.UserLog;
import com.vantar.util.json.Json;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminActionLog {

    public static void loadLogPage(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_ACTION_LOG), params, response, true);

        String clazz = params.getString("dto");
        Long id = params.getLong("id");
        ui.addHeading(2, clazz + " (" + id + ")");

//        QueryBuilder q = new QueryBuilder(new UserLog.View());
//        q.limit(10);
//        q.condition().equal("className", clazz);
//        q.condition().equal("objectId", id);
//        q.sort("id:desc");

//        List<UserLog.View> data;
//        try {
//            data = CommonRepoMongo.getData(q);
//        } catch (DatabaseException e) {
//            ui.addErrorMessage(e);
//            ui.finish();
//            return;
//        } catch (NoContentException e) {
//            ui.addErrorMessage(Locale.getString(VantarKey.NO_CONTENT));
//            ui.finish();
//            return;
//        }

//        ServiceDtoCache cache;
//        try {
//            cache = Services.get(ServiceDtoCache.class);
//        } catch (ServiceException e) {
//            ui.addErrorMessage(e).write();
//            return;
//        }

//        for (UserLog.View dto : data) {
//            ui.addLogRow(
//                dto,
//                cache == null || dto.userId == null ? null : cache.getDto(ServiceAuth.getUserClass(), dto.userId)
//            );
  //      }

        ui.addLogPage(clazz, id);

        ui.finish();
    }

    public static String getRows(Params params) {
        String clazz = params.getString("dto");
        Long id = params.getLong("id");
        Integer page = params.getInteger("page", 1);

        ServiceDtoCache cache;
        try {
            cache = Services.getService(ServiceDtoCache.class);
        } catch (ServiceException e) {
            return "ERROR";
        }

        QueryBuilder q = new QueryBuilder(new UserLog.View());
        q.page(page, 5);
        q.condition().equal("className", clazz);
        q.condition().equal("objectId", id);
        q.sort("id:desc");

        WebUi ui = new WebUi(params);
        try {
            ModelMongo.forEach(q, dto -> {
                UserLog.View log = (UserLog.View) dto;
                ui.addLogRows(
                    log,
                    cache == null || log.userId == null ? null : cache.getDto(ServiceAuth.getUserClass(), log.userId)
                );

            });
        } catch (NoContentException e) {
            return "FINISHED";
        } catch (VantarException e) {
            return "ERROR";
        }

        return ui.getString();
    }

    public static String getObject(Params params) {
        try {
            String object = ModelMongo.getById(params, new UserLog()).object;
            if (object == null) {
                return "";
            } else if (Json.isJsonShallow(object)) {
                return Json.d.toJsonPretty(object);
            } else {
                return object;
            }
        } catch (VantarException e) {
            return "";
        }
    }
}