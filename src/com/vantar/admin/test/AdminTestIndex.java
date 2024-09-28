package com.vantar.admin.test;

import com.vantar.admin.index.Admin;
import com.vantar.exception.FinishException;
import com.vantar.locale.VantarKey;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;


public class AdminTestIndex {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_MENU_TEST, params, response, true);

        ui  .beginBox("WEB/API test")
            .addHrefBlockNewPage("Run test", "/admin/test/web")
            .blockEnd()
            .beginBox("WEB/API unit test")
            .addHrefBlockNewPage("Run/List tests", "/admin/test/web/unit/list")
            .addHrefBlockNewPage("Create tests", "/admin/test/web/unit/create")
            .finish();
    }
}
