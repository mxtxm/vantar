package com.vantar.business.importexport;

import com.vantar.business.ModelCommon;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.Dto;
import com.vantar.util.json.Json;
import com.vantar.util.string.StringUtil;
import com.vantar.web.WebUi;
import org.slf4j.*;
import java.util.*;


public abstract class ImportCommon {

    public static final Logger log = LoggerFactory.getLogger(ImportCommon.class);


    protected static void importDataX(Import importCallback, String data, Dto dto, List<String> presentField, WebUi ui) {
        try {
            if (data.startsWith("[") && data.endsWith("]")) {
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

            ModelCommon.afterDataChange(dto);

        } catch (Exception e) {
            log.error(" ! {}", dto, e);
        }

        ui.write();
    }

    private static void importHumanReadable(Import importCallback, String data, Dto dto, List<String> presentField, WebUi ui) {
        String[] dataArray = data.split("-{10,}");

        for (String row : dataArray) {
            row = row.trim();
            if (row.isEmpty() || row.contains("##$$")) {
                continue;
            }

            Map<String, Object> values = new HashMap<>(100, 1);
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
                values.put(key, value);
            }

            List<ValidationError> errors = dto.validate(Dto.Action.INSERT);
            if (errors.isEmpty()) {
                importCallback.execute(presentValue.toString(), values);
                continue;
            }

            ui.addKeyValue(presentValue.toString(), ValidationError.toString(errors), "error");
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
                ui.addKeyValue(row, "invalid data. column/field mismatch (" + dataArray[0] + " : " + row + ")", "error");
                log.error(" !! import failed column/field mismatch > ({} : {})", dataArray[0], row);
                continue;
            }

            for (int j = 0; j < fieldCount; j++) {
                cols[j] = cols[j].trim();
            }

            dto.reset();
            dto.setToDefaults();

            Map<String, Object> values = new HashMap<>(100, 1);
            for (int j = 0; j < fieldCount; j++) {
                dto.setPropertyValue(fields[j], cols[j].equals("-") ? null : cols[j]);
                values.put(fields[j], cols[j].equals("-") ? null : cols[j]);
                if (presentField.contains(fields[j])) {
                    presentValue = cols[j];
                }
            }

            importCallback.execute(presentValue, values);
        }
    }

    private static void importJsonList(Import importCallback, String data, Dto dto, List<String> presentField, WebUi ui) {
        List<? extends Dto> list = Json.d.listFromJson(data, dto.getClass());
        if (list == null) {
            return;
        }
        for (Dto dtoX: list) {
            dto.set(dtoX);

            List<ValidationError> errors = dto.validate(Dto.Action.INSERT);
            String presentValue = dto.getPresentationValue();
            if (errors.isEmpty()) {
                importCallback.execute(presentValue, new HashMap<>(1, 1));
                continue;
            }
            ui.addKeyValue(presentValue, ValidationError.toString(errors), "error");
        }
    }


    public interface Import {

        void execute(String presentValue, Map<String, Object> values);
    }
}
