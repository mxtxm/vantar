package com.vantar.admin.documentation.get;

import com.vantar.exception.InputException;
import com.vantar.util.file.*;
import com.vantar.util.json.Json;
import com.vantar.util.string.StringUtil;
import com.vantar.web.Params;


public class WebServiceGet {

    public static WebServiceData.Data getJson(Params params) throws InputException {
        WebServiceData.Data data = get(params);
        if (data == null) {
            return null;
        }
        data.inputSampleJson = Json.d.toJsonPretty(data.inputSample);
        return data;
    }

    public static WebServiceData.Data get(Params params) throws InputException {
        String url = params.getStringRequired("url");
        String[] content = new String[] {null, null};

        DirUtil.browseFile(FileUtil.getClassPathAbsolutePath("/document"), file -> {
            content[0] = FileUtil.getFileContent(file.getAbsolutePath());
            if (content[0].contains(url)) {
                content[1] = file.getAbsolutePath();
                return true;
            }
            content[0] = null;
            return false;
        });


        if (content[0] == null) {
            return null;
        }
        WebServiceData serviceData = new WebServiceData();
        WebServiceData.Data data = serviceData.get(content[0], url);
        if (data != null) {
            String[] parts = StringUtil.split(content[1], "/document/");
            if (parts.length == 2) {
                data.documentUrl = "/admin/documentation/show?lang=en&document="
                    + StringUtil.replace("/document/" + parts[1], "/", "--");
            }
        }
        return data;
    }
}
