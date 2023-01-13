package com.vantar.business.importexport;

import com.vantar.business.*;
import com.vantar.database.dto.Dto;
import com.vantar.database.nosql.mongo.Mongo;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.CommonUser;
import com.vantar.service.log.ServiceUserActionLog;
import com.vantar.web.WebUi;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class ImportMongo extends CommonImport {

    public static void importDataAdmin(String data, Dto dto, List<String> presentField, boolean deleteAll, WebUi ui) {
        if (Services.isUp(ServiceUserActionLog.class)) {
            ServiceUserActionLog.add(Dto.Action.IMPORT, dto);
        }

        ui.beginBox2(dto.getClass().getSimpleName()).write();

        if (deleteAll) {
            try {
                CommonRepoMongo.purge(dto.getStorage());
            } catch (DatabaseException e) {
                ui.addErrorMessage(e);
                log.error(" !! {} : {} > {}\n", dto.getClass().getSimpleName(), dto, data, e);
                return;
            }
            ui.addPre(Locale.getString(VantarKey.DELETE_SUCCESS)).write();
        }

        AtomicInteger failed = new AtomicInteger();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();

        Import imp = (String presentValue, Map<String, Object> values) -> {
            try {
                if (dto.getId() == null ? CommonRepoMongo.existsByDto(dto) : CommonRepoMongo.existsById(dto)) {
                    duplicate.getAndIncrement();
                    return;
                }

                CommonRepoMongo.insert(dto);
                if (dto instanceof CommonUser) {
                    CommonModel.insertPassword(
                        dto,
                        (String) values.get("password")
                    );
                }

                success.getAndIncrement();
            } catch (DatabaseException | ServerException e) {
                ui.addErrorMessage(presentValue + " " + Locale.getString(VantarKey.IMPORT_FAIL));
                failed.getAndIncrement();
            }
        };

        importDataX(imp, data, dto, presentField, ui);
        long max;
        try {
            max = Mongo.Sequence.setToMax(dto);
        } catch (DatabaseException e) {
            ui.addErrorMessage(e);
            max = 0;
        }

        ui.addPre(
            Locale.getString(VantarKey.BUSINESS_WRITTEN_COUNT, success) + "\n" +
            Locale.getString(VantarKey.BUSINESS_ERROR_COUNT, failed) + "\n" +
            Locale.getString(VantarKey.BUSINESS_DUPLICATE_COUNT, duplicate) + "\n" +
            Locale.getString(VantarKey.BUSINESS_SERIAL_MAX, max)
        );
        ui.containerEnd().containerEnd().write();
    }
}