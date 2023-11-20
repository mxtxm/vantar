package com.vantar.admin.model;

import com.vantar.business.*;
import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.service.log.dto.UserLog;
import com.vantar.util.json.Json;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminActionLog {

    public static void loadLogPage(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_ACTION_LOG), params, response, true);

        String clazz = params.getString(VantarParam.DTO);
        Long id = params.getLong(VantarParam.ID);
        ui.addHeading(clazz + " (" + id + ")");

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
        String clazz = params.getString(VantarParam.DTO);
        Long id = params.getLong(VantarParam.ID);
        Integer page = params.getInteger("page", 1);

        ServiceDtoCache cache;
        try {
            cache = Services.get(ServiceDtoCache.class);
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
            CommonModelMongo.forEach(q, dto -> {
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
            String object = CommonModelMongo.getById(params, new UserLog()).object;
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

    public static void request(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_ACTION_LOG), params, response, true);
        int page = params.getInteger("page", 1);
        Long userId = params.getLong("userId");

        QueryBuilder q = new QueryBuilder(new UserLog());
        q.condition().equal("action", "REQUEST");
        q.page(page, 200);
        q.sort("id:desc");
        List<UserLog> requests;
        try {
            requests = CommonModelMongo.getData(q);
        } catch (NoContentException e) {
            ui.addErrorMessage(Locale.getString(VantarKey.NO_CONTENT));
            ui.finish();
            return;
        } catch (VantarException e) {
            ui.addErrorMessage(e);
            ui.finish();
            return;
        }

        q = new QueryBuilder(new UserLog());
        q.condition().equal("action", "RESPONSE");
        if (userId != null) {
            q.condition().equal("userId", userId);
        }
        q.page(page, 200);
        q.sort("id:desc");
        Map<String, UserLog> responses = new HashMap<>(200, 1);
        try {
            for (Dto dto : CommonModelMongo.getData(q)) {
                UserLog uLog = (UserLog) dto;
                responses.put(uLog.threadId + "-" + uLog.url, uLog);
            }
        } catch (NoContentException e) {
            ui.addErrorMessage(Locale.getString(VantarKey.NO_CONTENT));
            ui.finish();
            return;
        } catch (VantarException e) {
            ui.addErrorMessage(e);
            ui.finish();
            return;
        }

        for (UserLog req : requests) {
            UserLog res = responses.remove(req.threadId + "-" + req.url);

            ui.addPre(
                req.time.formatter().getDateTime() + ": " + req.url + " | " + req.action + " | " + req.requestType + "\n" +
                (res == null ? "NO RESPONSE" : (res.time.formatter().getDateTime() + ":" + res.headers.get("Status-Code"))) + "\n" +
                "REQUEST: " + req.object + "\n" +
                (res == null ? "" : "RESPONSE: " + res.object)
            ).write();
        }

        ui.finish();
    }

    public static void user(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_ACTION_LOG), params, response, true);
        int page = params.getInteger("page", 1);
        long userId = params.getLong("userId");

        QueryBuilder q = new QueryBuilder(new UserLog());
        q.condition().equal("userId", userId);
        q.page(page, 200);
        q.sort("id:desc");
        List<UserLog> requests;
        try {
            requests = CommonModelMongo.getData(q);
        } catch (NoContentException e) {
            ui.addErrorMessage(Locale.getString(VantarKey.NO_CONTENT));
            ui.finish();
            return;
        } catch (VantarException e) {
            ui.addErrorMessage(e);
            ui.finish();
            return;
        }

        for (UserLog req : requests) {
            ui.addPre(
                req.time.formatter().getDateTime() + ": " + req.url + " | " + req.action + " | " + req.requestType + "\n" +
                    "REQUEST: " + req.object + "\n"
            ).write();
        }

        ui.finish();
    }
}