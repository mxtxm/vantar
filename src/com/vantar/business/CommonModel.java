package com.vantar.business;

import com.vantar.common.VantarParam;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.*;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.util.json.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.slf4j.*;
import java.util.List;


public abstract class CommonModel {

    private static final Logger log = LoggerFactory.getLogger(CommonModel.class);


    public static void afterDataChange(Dto dto) {
        if (dto.hasAnnotation(Cache.class)) {
            ServiceDtoCache service;
            try {
                service = Services.get(ServiceDtoCache.class);
            } catch (ServiceException e) {
                return;
            }
            String dtoName = dto.getClass().getSimpleName();
            service.update(dtoName);
            Services.messaging.broadcast(VantarParam.MESSAGE_DATABASE_UPDATED, dtoName);
        }
    }

    protected static void importDataX(Import importCallback, String data, Dto dto, List<String> presentField, WebUi ui) {
        if (data.startsWith("[") && data.endsWith("[")) {
            importJsonList(importCallback, data, dto, presentField, ui);
        } else {
            String[] dataArray = StringUtil.split(data, '\n');
            if (dataArray.length < 1) {
                ui.write();
                return;
            }

            if (data.contains("----------")) {
                importHumanReadable(importCallback, data, dto, presentField, ui);
            } else {
                importCsv(importCallback, dataArray, dto, presentField, ui);
            }
        }

        afterDataChange(dto);

        ui.write();
    }

    private static void importHumanReadable(Import importCallback, String data, Dto dto, List<String> presentField, WebUi ui) {
        String[] dataArray = data.split("-{10,}");

        for (String row : dataArray) {
            row = row.trim();
            if (row.isEmpty() || row.contains("##$$")) {
                continue;
            }

            dto.reset();
            String key;
            String value;
            StringBuilder presentValue = new StringBuilder();
            String[] split = StringUtil.split(row, '\n');
            for (int i = 0; i < split.length; ++i) {
                String[] parts = StringUtil.split(split[i], '=', 2);
                key = parts[0].trim();
                value = parts[1].trim();

                if (value.equals(">>>>>")) {
                    StringBuilder valueSb = new StringBuilder();
                    for (++i; i < split.length; ++i) {
                        if (split[i].equals("<<<<<")) {
                            break;
                        }
                        valueSb.append(split[i]).append('\n');
                    }
                    value = valueSb.toString();
                }

                if (presentField.contains(key)) {
                    presentValue.append((presentValue.length() == 0) ? "" : ", ").append(value);
                }

                if (value.isEmpty()) {
                    value = null;
                }
                dto.setPropertyValue(key, value);
            }

            List<ValidationError> errors = dto.validate(Dto.Action.INSERT);
            if (errors.isEmpty()) {
                importCallback.execute(presentValue.toString());
                continue;
            }

            ui.addKeyValueFail(presentValue.toString(), ValidationError.toString(errors));
        }
    }

    private static void importCsv(Import importCallback, String[] dataArray, Dto dto, List<String> presentField, WebUi ui) {
        String presentValue = "";
        String[] fields = StringUtil.split(dataArray[0], ',');
        int fieldCount = fields.length;
        for (int i = 0; i < fieldCount; ++i) {
            fields[i] = fields[i].trim();
        }

        for (int i = 1, l = dataArray.length; i < l; ++i) {
            String row = dataArray[i]
                .replace("\r", "")
                .replace("\n", "")
                .replace("<U+200C>", "")
                .trim();

            String[] cols = StringUtil.split(row, ',');

            if (cols.length != fieldCount) {
                ui.addKeyValueFail(row, "invalid data. column/field mismatch (" + dataArray[0] + " : " + row + ")");
                log.error(" !! import failed column/field mismatch > ({} : {})", dataArray[0], row);
                continue;
            }

            for (int j = 0; j < fieldCount; j++) {
                cols[j] = cols[j].trim();
            }

            dto.reset();
            dto.setToDefaults();

            for (int j = 0; j < fieldCount; j++) {
                dto.setPropertyValue(fields[j], cols[j].equals("-") ? null : cols[j]);
                if (presentField.contains(fields[j])) {
                    presentValue = cols[j];
                }
            }

            importCallback.execute(presentValue);
        }
    }

    private static void importJsonList(Import importCallback, String data, Dto dto, List<String> presentField, WebUi ui) {
        List<? extends Dto> list = Json.d.listFromJson(data, dto.getClass());
        if (list == null) {
            return;
        }

        for (Dto dtoX: list) {
            dto = dtoX;

            List<ValidationError> errors = dto.validate(Dto.Action.INSERT);
            String presentValue = dto.getPresentationValue();
            if (errors.isEmpty()) {
                importCallback.execute(presentValue);
                continue;
            }

            ui.addKeyValueFail(presentValue, ValidationError.toString(errors));
        }
    }


    public interface Import {

        void execute(String presentValue);
    }


    public interface WriteEvent {

        void beforeSet(Dto dto) throws InputException, ServerException, NoContentException;
        void beforeWrite(Dto dto) throws InputException, ServerException, NoContentException;
        void afterWrite(Dto dto) throws InputException, ServerException, NoContentException;
    }


    public interface QueryEvent extends QueryResultBase.Event {

        void beforeQuery(QueryBuilder q) throws InputException, ServerException, NoContentException;

    }


    public interface BatchEvent {

        boolean beforeInsert(Dto dto);
        boolean beforeUpdate(Dto dto);
        boolean beforeDelete(Dto dto);
    }
}
