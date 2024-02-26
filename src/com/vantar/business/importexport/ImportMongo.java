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


public class ImportMongo extends CommonImport {

    public static void importDataAdmin(String data, Dto dto, List<String> presentField, boolean deleteAll, WebUi ui) {
        if (Services.isUp(ServiceLog.class)) {
            ServiceLog.addAction(Dto.Action.IMPORT, dto);
        }

        ui.beginBox(dto.getClass().getSimpleName(), null, "box-title2").write();

        if (deleteAll) {
            String collection = dto.getStorage();
            try {
                Mongo.deleteAll(collection);
                Mongo.Index.remove(collection);
                Mongo.Sequence.remove(collection);
            } catch (DatabaseException e) {
                ui.addErrorMessage(e);
                log.error(" !! {} : {} > {}\n", dto.getClass().getSimpleName(), dto, data, e);
                return;
            }
            ui.addBlock("pre", Locale.getString(VantarKey.DELETE_SUCCESS)).write();
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
                ModelMongo.insert(dto);
                if (dto instanceof CommonUser) {
                    CommonModel.insertPassword(
                        dto,
                        (String) values.get("password")
                    );
                }

                success.getAndIncrement();
            } catch (VantarException e) {
                ui.addErrorMessage(presentValue + " " + Locale.getString(VantarKey.IMPORT_FAIL));
                failed.getAndIncrement();
            }
        };

        importDataX(imp, data.trim(), dto, presentField, ui);
        long max;
        try {
            max = Mongo.Sequence.setToMax(dto);
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
            max = 0;
        }

        ui.addBlock("pre",
            Locale.getString(VantarKey.BUSINESS_WRITTEN_COUNT, success) + "\n" +
            Locale.getString(VantarKey.BUSINESS_ERROR_COUNT, failed) + "\n" +
            Locale.getString(VantarKey.BUSINESS_DUPLICATE_COUNT, duplicate) + "\n" +
            Locale.getString(VantarKey.BUSINESS_SERIAL_MAX, max)
        );
        ui.blockEnd().blockEnd().write();
    }
}