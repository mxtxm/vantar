package com.vantar.business.importexport;

import com.vantar.business.*;
import com.vantar.database.common.Db;
import com.vantar.database.dto.*;
import com.vantar.database.nosql.mongo.DbMongo;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.CommonUser;
import com.vantar.service.log.ServiceLog;
import com.vantar.web.WebUi;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class MongoImport extends ImportCommon {

    public static void importDtoData(WebUi ui, String data, Dto dto, List<String> presentField, boolean deleteAll, DbMongo db) {
        if (Services.isUp(ServiceLog.class)) {
            ServiceLog.addAction(Dto.Action.IMPORT, dto);
        }

        if (ui != null) {
            ui.addHeading(3, dto.getClass().getSimpleName()).write();
        }
        if (deleteAll) {
            String collection = dto.getStorage();
            try {
                db.deleteAll(collection);
                db.autoIncrementRemove(collection);
            } catch (Exception e) {
                if (ui != null) {
                    ui.addErrorMessage(e).write();
                }
                return;
            }
            if (ui != null) {
                ui.addMessage(VantarKey.SUCCESS_DELETE).write();
            }
        }

        AtomicInteger failed = new AtomicInteger();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();
        Import imp = (String presentValue, Map<String, Object> values) -> {
            try {
                if (dto.getId() == null ? Db.modelMongo.existsByDto(dto) : Db.modelMongo.existsById(dto)) {
                    duplicate.getAndIncrement();
                    return;
                }
                Db.modelMongo.insert(new ModelCommon.Settings(dto).logEvent(false).mutex(false));

                if (dto instanceof CommonUser) {
                    Db.modelMongo.insertPassword(dto, (String) values.get("password"));
                }

                success.getAndIncrement();
            } catch (VantarException e) {
                if (ui != null) {
                    ui.addErrorMessage(presentValue + " " + Locale.getString(VantarKey.FAIL_IMPORT));
                }
                failed.getAndIncrement();
            }
        };

        importDataX(ui, imp, data.trim(), dto, presentField);
        long max;
        try {
            max = db.autoIncrementSetToMax(dto);
        } catch (VantarException e) {
            if (ui != null) {
                ui.addErrorMessage(e);
            }
            max = 0;
        }

        if (ui != null) {
            ui  .addKeyValue(VantarKey.SUCCESS_COUNT, success)
                .addKeyValue(VantarKey.FAIL_COUNT, failed)
                .addKeyValue(VantarKey.DUPLICATE_COUNT, duplicate)
                .addKeyValue(VantarKey.AUTO_INCREMENT_MAX, max)
                .write();
        }
    }
}