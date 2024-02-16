package com.vantar.admin.model.document;

import com.vantar.common.Settings;
import com.vantar.exception.VantarException;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import com.vantar.util.xlsx.*;
import com.vantar.web.Params;
import org.dhatim.fastexcel.Worksheet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminWebService {

    private static class WebServiceData {

        public String path;
        public String module;
        public String subModule;

        @Override
        public String toString() {
            return ObjectUtil.toString(this);
        }
    }

    public static void webServicesXlsx(Params params, HttpServletResponse response) throws VantarException {
        List<WebServiceData> services = new ArrayList<>(2000);
        for (Class<?> cls : ClassUtil.getClasses("documents.dto.package", WebServlet.class)) {
            for (String path : cls.getAnnotation(WebServlet.class).value()) {
                WebServiceData w = new WebServiceData();
                w.path = path;
                w.subModule = StringUtil.remove(cls.getSimpleName(), "Controller");
                w.module = StringUtil.remove(
                    cls.getPackage().getName(),
                    Settings.config.getProperty("documents.dto.package" + ".")
                );
                services.add(w);
            }
        }

        Xlsx.Config xlsx = new Xlsx.Config(response, "webservices.xlsx", "Webservices");
        xlsx.addWriteEvent("Webservices", (context) -> {
            Worksheet sheet = context.sheet;
            sheet.fitToWidth((short) 1);
            sheet.width(0, 100);
            sheet.width(1, 25);
            sheet.width(2, 25);
            sheet.width(3, 25);

            XlsxStyledBase.setHeader(context);
            Xlsx.setCell(context, "Path", "Module", "Sub module", "Usage");

            XlsxStyledBase.setNormalLeft(context);
            for (WebServiceData w : services) {
                context.nextRow();
                Xlsx.setCell(context, w.path, w.module, w.subModule, "");
            }
        });
        Xlsx.create(xlsx);
    }
}
