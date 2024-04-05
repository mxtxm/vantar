package com.vantar.admin.database.data.panel;

import com.vantar.common.Settings;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.nosql.mongo.MongoConnection;
import com.vantar.database.sql.SqlConnection;
import com.vantar.locale.VantarKey;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.object.ClassUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;


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
