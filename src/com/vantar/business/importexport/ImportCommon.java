package com.vantar.business.importexport;

import com.vantar.business.ModelCommon;
import com.vantar.database.common.ValidationError;
import com.vantar.database.dto.Dto;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.json.Json;
import com.vantar.util.string.StringUtil;
import com.vantar.web.WebUi;
import java.util.*;


public abstract class ImportCommon {

    protected static void importDataX(WebUi ui, Import importCallback, String data, Dto dto, List<String> presentField) {
        try {
            if (data.startsWith("[") && data.endsWith("]")) {
                importJsonList(ui, importCallback, data, dto, presentField);
            } else {
                String[] dataArray = StringUtil.splitTrim(data, '\n');
                if (dataArray.length < 1) {
                    if (ui != null) {
                        ui.write();
                    }
                    return;
                }

                if (data.contains("----------")) {
                    importHumanReadable(ui, importCallback, data, dto, presentField);
                } else {
                    importCsv(ui, importCallback, dataArray, dto, presentField);
                }
            }

            ModelCommon.updateDtoCache(dto);

        } catch (Exception e) {
            if (ui == null) {
                ServiceLog.log.error("! import({})", dto.getClass().getSimpleName(), e);
            } else {
                ui.addErrorMessage(e).write();
            }
        }
    }

    private static void importHumanReadable(WebUi ui, Import importCallback, String data, Dto dto, List<String> presentField) {
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
            String[] split = StringUtil.splitTrim(row, '\n');
            for (int i = 0; i < split.length; ++i) {
                String[] parts = StringUtil.splitTrim(split[i], '=', 2);
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
            if (ui != null) {
                ui.addKeyValue(presentValue.toString(), ValidationError.toString(errors), "error").write();
            }
        }
    }

    private static void importCsv(WebUi ui, Import importCallback, String[] dataArray, Dto dto, List<String> presentField) {
        String presentValue = "";
        String[] fields = StringUtil.splitTrim(dataArray[0], ',');
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

            String[] cols = StringUtil.splitTrim(row, ',');

            if (cols.length != fieldCount) {
                if (ui == null) {
                    ServiceLog.log.error(" !! import failed column/field mismatch > ({} : {})", dataArray[0], row);
                } else {
                    ui.addKeyValue(row, "invalid data. column/field mismatch (" + dataArray[0] + " : " + row + ")", "error");
                }
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

    private static void importJsonList(WebUi ui, Import importCallback, String data, Dto dto, List<String> presentField) {
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
            if (ui != null) {
                ui.addKeyValue(presentValue, ValidationError.toString(errors), "error");
            }
        }
    }


    public interface Import {

        void execute(String presentValue, Map<String, Object> values);
    }
}
