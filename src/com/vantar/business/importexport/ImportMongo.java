package com.vantar.business.importexport;

import com.vantar.business.*;
import com.vantar.database.dto.Dto;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.CommonUser;
import com.vantar.service.log.ServiceLog;
import com.vantar.web.WebUi;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class ImportMongo extends ImportCommon {

    public static void importDtoData(String data, Dto dto, List<String> presentField, boolean deleteAll, WebUi ui) {
        if (Services.isUp(ServiceLog.class)) {
            ServiceLog.addAction(Dto.Action.IMPORT, dto);
        }

        if (ui != null) {
            ui.addHeading(3, dto.getClass().getSimpleName()).write();
        }
        if (deleteAll) {
            String collection = dto.getStorage();
            try {
                Mongo.deleteAll(collection);
                Mongo.Sequence.remove(collection);
            } catch (Exception e) {
                if (ui != null) {
                    ui.addErrorMessage(e).write();
                }
                return;
            }
            if (ui != null) {
                ui.addMessage(VantarKey.DELETE_SUCCESS).write();
            }
        }

        AtomicInteger failed = new AtomicInteger();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();
        Import imp = (String presentValue, Map<String, Object> values) -> {
            try {
                if (dto.getId() == null ? ModelMongo.existsByDto(dto) : ModelMongo.existsById(dto)) {
                    duplicate.getAndIncrement();
                    return;
                }
                ModelMongo.insert(new ModelCommon.Settings(dto).logEvent(false).mutex(false));

                if (dto instanceof CommonUser) {
                    ModelCommon.insertPassword(dto, (String) values.get("password"));
                }

                success.getAndIncrement();
            } catch (VantarException e) {
                if (ui != null) {
                    ui.addErrorMessage(presentValue + " " + Locale.getString(VantarKey.IMPORT_FAIL));
                }
                failed.getAndIncrement();
            }
        };

        importDataX(imp, data.trim(), dto, presentField, ui);
        long max;
        try {
            max = Mongo.Sequence.setToMax(dto);
        } catch (DatabaseException e) {
            if (ui != null) {
                ui.addErrorMessage(e);
            }
            max = 0;
        }

        if (ui != null) {
            ui  .addKeyValue(VantarKey.BUSINESS_WRITTEN_COUNT, success)
                .addKeyValue(VantarKey.BUSINESS_ERROR_COUNT, failed)
                .addKeyValue(VantarKey.BUSINESS_DUPLICATE_COUNT, duplicate)
                .addKeyValue(VantarKey.BUSINESS_SERIAL_MAX, max)
                .write();
        }
    }
}