package com.vantar.admin.database.data.panel;

import com.vantar.business.*;
import com.vantar.business.importexport.MongoImport;
import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.file.FileUtil;
import com.vantar.util.json.Json;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


public class AdminDataImportExport {

    /**
     * Dump collection/table
     */
    public static void exportData(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws VantarException {
        Dto dto = dtoInfo.getDtoInstance();

        if (dtoInfo.dbms.equals(Db.Dbms.MONGO)) {
            if (Services.isUp(Db.Dbms.MONGO)) {
                List<Dto> data = Db.modelMongo.getAll(dto);
                String json = Json.d.toJson(data);
                String filepath = FileUtil.getTempFilename();
                FileUtil.write(filepath, json);
                Response.download(
                    response,
                    filepath,
                    StringUtil.toKababCase(dto.getClass().getSimpleName()) + "-"
                        + new DateTime().formatter().getDateTimeAsFilename() + ".json"
                );
            }
        } else if (dtoInfo.dbms.equals(Db.Dbms.SQL)) {
            if (Services.isUp(Db.Dbms.SQL)) {
                // todo
            }
        } else if (dtoInfo.dbms.equals(Db.Dbms.ELASTIC)) {
            if (Services.isUp(Db.Dbms.ELASTIC)) {

            }
        }
    }

    public static void importData(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDto(VantarKey.ADMIN_IMPORT, "import", params, response, info);

        DataUtil.Event event = DataUtil.getEvent();
        if (event != null) {
            u.dto = event.dtoExchange(u.dto, "import");
        }

        if (!params.contains("f")) {
            u.ui.beginFormPost()
                .addEmptyLine(2)
                .addTextArea(VantarKey.ADMIN_MENU_DATA, "imd", info.getImportData(), "large ltr")
                .addCheckbox(VantarKey.ADMIN_DATA_PURGE, "da")
                .addSubmit(VantarKey.ADMIN_SUBMIT)
                .blockEnd()
                .finish();
            return;
        }

        String data = params.getString("imd");
        boolean deleteAll = params.isChecked("da");

        if (info.dbms.equals(Db.Dbms.MONGO)) {
            MongoImport.importDtoData(u.ui, data, u.dto, u.dto.getPresentationPropertyNames(), deleteAll, Db.mongo);
        } else if (info.dbms.equals(Db.Dbms.SQL)) {
            CommonModelSql.importDataAdmin(u.ui, data, u.dto, u.dto.getPresentationPropertyNames(), deleteAll);
        } else if (info.dbms.equals(Db.Dbms.ELASTIC)) {
            CommonModelElastic.importDataAdmin(u.ui, data, u.dto, u.dto.getPresentationPropertyNames(), deleteAll);
        }

        if (info.broadcastMessage != null) {
            Services.messaging.broadcast(info.broadcastMessage);
        }

        u.ui.finish();
    }
}