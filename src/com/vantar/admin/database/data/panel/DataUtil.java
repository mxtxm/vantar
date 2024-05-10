package com.vantar.admin.database.data.panel;

import com.vantar.admin.index.Admin;
import com.vantar.common.Settings;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.nosql.mongo.MongoConnection;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.VantarKey;
import com.vantar.service.auth.CommonUser;
import com.vantar.service.log.ServiceLog;
import com.vantar.service.log.dto.*;
import com.vantar.util.object.ClassUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class DataUtil {

    public static final int N_PER_PAGE = 100;


    protected static Event getEvent() {
        String adminApp = Settings.getAdminApp();
        if (StringUtil.isNotEmpty(adminApp)) {
            try {
                return (Event) ClassUtil.callStaticMethod(adminApp + ".getAdminDataEvent");
            } catch (Throwable e) {
                ServiceLog.log.error("! AdminDataImportExport '{}.getAdminDataEvent()'", adminApp, e);
            }
        }
        return null;
    }

    protected static boolean isUp(DtoDictionary.Dbms dbms, WebUi ui) {
        if (dbms.equals(DtoDictionary.Dbms.MONGO) && MongoConnection.isUp()) {
            return true;
        } else if (dbms.equals(DtoDictionary.Dbms.SQL) && SqlConnection.isUp()) {
            return true;
        } else if (dbms.equals(DtoDictionary.Dbms.ELASTIC) && ElasticConnection.isUp()) {
            return true;
        }
        ui.addMessage(com.vantar.locale.Locale.getString(VantarKey.ADMIN_SERVICE_IS_OFF, dbms));
        return false;
    }

    public static class Ui {
        public WebUi ui;
        public Dto dto;
    }
    
    protected static Ui initDto(VantarKey title, String exclude, Params params, HttpServletResponse response
        , DtoDictionary.Info info) throws FinishException {

        if (info == null) {
            throw new FinishException();
        }
        Ui u = new Ui();
        u.ui = Admin.getUiDto(title, params, response, info);
        if (!DataUtil.isUp(info.dbms, u.ui)) {
            u.ui.finish();
            throw new FinishException();
        }
        u.dto = info.getDtoInstance();

        boolean isLog = isDtoLog(u.dto);

        List<String> menuItems = new ArrayList<>(10);
        menuItems.add("list");
        if (!isLog) {
            menuItems.add("insert");
        }
        menuItems.add("import");
        menuItems.add("export");
        menuItems.add("purge");
        if (!isLog) {
            menuItems.add("undelete");
            menuItems.add("log");
            menuItems.add("index");
            menuItems.add("dependencies");
            menuItems.add("cache");
        }

        u.ui.addDtoLinks(info, u.dto, menuItems, exclude)
            .addEmptyLine();

        return u;
    }

    protected static Ui initDtoItem(VantarKey title, String exclude, Params params, HttpServletResponse response
        , DtoDictionary.Info info) throws FinishException {

        if (info == null) {
            throw new FinishException();
        }
        Ui u = new Ui();
        String userName = params.getString("un");
        String userFullName = params.getString("ufn");
        u.ui = Admin.getUiDto(
            Locale.getString(title) + (userName == null ? "" : (" " + userFullName + " (" + userName + ") ")),
            params,
            response,
            info
        );
        if (!DataUtil.isUp(info.dbms, u.ui)) {
            u.ui.finish();
            throw new FinishException();
        }
        u.dto = info.getDtoInstance();
        try {
            u.dto.setId(u.ui.params.getLongRequired("id"));
        } catch (InputException e) {
            u.ui.addErrorMessage(e).finish();
            throw new FinishException();
        }

        boolean isLog = isDtoLog(u.dto);

        List<String> menuItems = new ArrayList<>(11);
        if (userName == null) {
            menuItems.add("view");
            menuItems.add("update");
            menuItems.add("delete");
            if (!isLog) {
                menuItems.add("dependencies");
                menuItems.add("log-action");
            }
        } else {
            menuItems.add("ufn:" + userFullName);
            menuItems.add("un:" + userName);
            menuItems.add("view");
            menuItems.add("update");
            menuItems.add("delete");
            if (!isLog) {
                menuItems.add("dependencies");
                menuItems.add("log-action");
                if (u.dto instanceof CommonUser) {
                    menuItems.add("log-activity");
                    menuItems.add("log-web");
                }
            }
        }
        menuItems.add("update-property");

        u.ui.addDtoItemLinks(u.dto.getClass().getSimpleName(), u.dto.getId(), menuItems, exclude)
            .addEmptyLine();

        return u;
    }

    public static boolean isDtoLog(Dto dto) {
        return
            Log.class.equals(dto.getClass())
                || Log.Mini.class.equals(dto.getClass())
                || UserWebLog.class.equals(dto.getClass())
                || UserWebLog.Mini.class.equals(dto.getClass())
                || UserLog.class.equals(dto.getClass())
                || UserLog.Mini.class.equals(dto.getClass());
    }

    public interface Event {

        // change dto before action
        Dto dtoExchange(Dto dto, String action);

        void beforeInsert(Dto dto);
        void afterInsert(Dto dto);

        void beforeUpdate(Dto dto);
        void afterUpdate(Dto dto);

        void beforeDelete(Dto dto);
        void afterDelete(Dto dto);
    }
}
