package com.vantar.admin.documentation;

import com.vantar.common.Settings;
import com.vantar.exception.VantarException;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import com.vantar.util.xlsx.*;
import org.dhatim.fastexcel.Worksheet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Webservice manifest as xlsx
 */
public class WebServiceManifest {

    public static void downloadXlsx(HttpServletResponse response) throws VantarException {
        String webPackage = Settings.config.getProperty("package.web");
        List<WebServiceData> services = new ArrayList<>(2000);
        for (Class<?> cls : ClassUtil.getClasses(webPackage, WebServlet.class)) {
            for (String path : cls.getAnnotation(WebServlet.class).value()) {
                WebServiceData w = new WebServiceData();
                w.path = path;
                w.subModule = StringUtil.remove(cls.getSimpleName(), "Controller");
                w.module = StringUtil.remove(
                    cls.getPackage().getName(),
                    webPackage + "."
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


    private static class WebServiceData {

        public String path;
        public String module;
        public String subModule;

        @Override
        public String toString() {
            return ObjectUtil.toString(this);
        }
    }
}
