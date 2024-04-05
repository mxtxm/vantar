package com.vantar.admin.database.data.panel;

import com.vantar.admin.index.Admin;
import com.vantar.business.*;
import com.vantar.business.importexport.ImportMongo;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.elasticsearch.ElasticConnection;
import com.vantar.database.nosql.mongo.MongoConnection;
import com.vantar.database.sql.SqlConnection;
import com.vantar.exception.*;
import com.vantar.locale.*;
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

        if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
            if (MongoConnection.isUp()) {
                List<Dto> data = ModelMongo.getAll(dto);
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
        } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
            if (SqlConnection.isUp()) {
                // todo
            }
        } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
            if (ElasticConnection.isUp()) {

            }
        }
    }

    public static void importData(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        WebUi ui = Admin.getUiDto(VantarKey.ADMIN_IMPORT, params, response, dtoInfo);
        if (dtoInfo == null) {
            return;
        }
        if (!DataUtil.isUp(dtoInfo.dbms, ui)) {
            ui.finish();
            return;
        }
        Dto dto = dtoInfo.getDtoInstance();
        DataUtil.Event event = DataUtil.getEvent();
        if (event != null) {
            dto = event.dtoExchange(dto, "import");
        }

        if (!params.isChecked("f")) {
            ui.beginFormPost()
                .addEmptyLine(2)
                .addTextArea(VantarKey.ADMIN_MENU_DATA, "imd", dtoInfo.getImportData(), "large ltr")
                .addCheckbox(VantarKey.ADMIN_DATA_PURGE, "da")
                .addSubmit(VantarKey.ADMIN_SUBMIT)
                .blockEnd()
                .finish();
            return;
        }

        String data = params.getString("imd");
        boolean deleteAll = params.isChecked("da");

        if (dtoInfo.dbms.equals(DtoDictionary.Dbms.MONGO)) {
            ImportMongo.importDtoData(data, dto, dto.getPresentationPropertyNames(), deleteAll, ui);
        } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.SQL)) {
            CommonModelSql.importDataAdmin(data, dto, dto.getPresentationPropertyNames(), deleteAll, ui);
        } else if (dtoInfo.dbms.equals(DtoDictionary.Dbms.ELASTIC)) {
            CommonModelElastic.importDataAdmin(data, dto, dto.getPresentationPropertyNames(), deleteAll, ui);
        }

        if (dtoInfo.broadcastMessage != null) {
            Services.messaging.broadcast(dtoInfo.broadcastMessage);
        }

        ui.finish();
    }
}