package com.vantar.admin.model.database.data.panel;

import com.vantar.admin.model.index.Admin;
import com.vantar.database.dto.*;
import com.vantar.exception.FinishException;
import com.vantar.locale.*;
import com.vantar.service.dbarchive.ServiceDbArchive;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminDataArchive {

    public static void archiveSwitch(Params params, HttpServletResponse response, DtoDictionary.Info dtoInfo) throws FinishException {
        WebUi ui = Admin.getUiDto(VantarKey.ADMIN_UPDATE, params, response, dtoInfo);
        if (dtoInfo == null) {
            return;
        }
        if (!DataUtil.isUp(dtoInfo.dbms, ui)) {
            ui.finish();
            return;
        }
        Dto dto = dtoInfo.getDtoInstance();

        String a = params.getString("a");
        if (StringUtil.isEmpty(a)) {
            ui.addMessage("wrong params");
        } else {
            ServiceDbArchive.switchCollection(dto.getClass(), a);
            ui.addMessage(dto.getClass().getSimpleName() + " storage switched to " + a + " refresh data page.");
        }

        ui.finish();
    }
}