package com.vantar.admin.database.data.field;

import com.vantar.admin.index.Admin;
import com.vantar.database.dto.*;
import com.vantar.exception.FinishException;
import com.vantar.locale.VantarKey;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.*;


public class AdminDtoFields {

    public static void fields(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        WebUi ui = Admin.getUiDto(VantarKey.ADMIN_DATA_FIELDS, params, response, dtoInfo);
        if (dtoInfo == null) {
            return;
        }
        Dto dto = dtoInfo.getDtoInstance();

        ui.addHeading(2, dtoInfo.dtoClass.getName() + " (" + dtoInfo.title + ")");

        for (Field field : dto.getFields()) {
            StringBuilder t = new StringBuilder(1000);

            Class<?> type = field.getType();
            t.append(type.getSimpleName());

            if (type.equals(List.class) || type.equals(ArrayList.class) || type.equals(Set.class)) {
                t.append("<").append(dto.getPropertyGenericTypes(field.getName())[0].getSimpleName()).append(">");

            } else if (type.equals(Map.class)) {
                Class<?>[] genericTypes = dto.getPropertyGenericTypes(field.getName());
                t.append("<").append(genericTypes[0].getSimpleName()).append(", ")
                    .append(genericTypes[1].getSimpleName()).append(">");

            } else if (type.isEnum()) {
                t.append(" (enum)");
            }

            ui.addKeyValue(field.getName(), t.toString());
        }

        ui.finish();
    }
}