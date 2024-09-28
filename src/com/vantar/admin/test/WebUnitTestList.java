package com.vantar.admin.test;

import com.vantar.admin.index.Admin;
import com.vantar.common.VantarParam;
import com.vantar.database.common.Db;
import com.vantar.database.dto.Dto;
import com.vantar.database.query.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class WebUnitTestList {

    public static void list(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_MENU_TEST, params, response, true);

        QueryBuilder q = new QueryBuilder(new WebUnitTestCase());
        q.page(1, 1000);
        q.sort("order:ASC");
        PageData data;
        try {
            data = Db.mongo.getPage(q);
        } catch (VantarException e) {
            ui.addErrorMessage(e).finish();
            return;
        }

        WebUi.DtoListOptions options = new WebUi.DtoListOptions();
        options.archive = false;
        options.pagination = false;
        options.search = false;
        options.fields = new String[] {"id", "tag", "title", "order", "testCount", "createT",};
        options.colOptionCount = 2;
        options.event = new WebUi.DtoListOptions.Event() {
            @Override
            public void checkListFormContent(WebUi ui) {

            }

            @Override
            public List<WebUi.DtoListOptions.ColOption> getColOptions(Dto dtoX) {
                List<WebUi.DtoListOptions.ColOption> colOptions = new ArrayList<>(3);

                WebUi.DtoListOptions.ColOption selectCheckBox = new WebUi.DtoListOptions.ColOption();

                selectCheckBox.containerClass = "delete-option";
                selectCheckBox.content = ui.getCheckbox("delete-check", false, dtoX.getId(), "delete-check", false);
                colOptions.add(selectCheckBox);

                WebUi.DtoListOptions.ColOption view = new WebUi.DtoListOptions.ColOption();
                view.content = ui.getHref(
                    VantarKey.ADMIN_UPDATE,
                    "/admin/data/update?dto=WebUnitTestCase&id=" + dtoX.getId(), true, false, null
                );
                colOptions.add(view);

                return colOptions;
            }
        };

        ui  .addEmptyLine(2)
            .beginBlock("div", "test-list")
            .beginFormPost("/admin/test/web/unit/run")
            .addHidden(VantarParam.AUTH_TOKEN, ui.authToken)
            .addDtoList(data, options);

        ui.addEmptyLine();
        ui.direction = "ltr".equalsIgnoreCase(ui.direction) ? "rtl" : "ltr";
        ui.alignKey = "left".equalsIgnoreCase(ui.alignKey) ? "right" : "left";
        ui.addCheckbox(VantarKey.ADMIN_SELECT_ALL, "delete-select-all");
        ui.addSubmit(VantarKey.ADMIN_TEST_RUN, "run-button", "run-button");
        ui.direction = "ltr".equalsIgnoreCase(ui.direction) ? "rtl" : "ltr";
        ui.alignKey = "left".equalsIgnoreCase(ui.alignKey) ? "right" : "left";

        ui.finish();
    }
}
