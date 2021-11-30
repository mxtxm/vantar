package com.vantar.admin.model;

import com.vantar.admin.model.document.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.util.file.FileUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.slf4j.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminDocument {

    public static final Logger log = LoggerFactory.getLogger(AdminDocument.class);


    public static void index(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_MENU_DOCUMENTS), params, response);
        if (ui == null) {
            return;
        }

        String search = params.getString("search");

        ui  .beginFormGet()
            .addInput("Search", "search", search)
            .addSubmit()
            .containerEnd();

        drawDirectoryStructure(ui, "document", "document", FileUtil.getResourceStructure("/document"), search);
        ui.finish();
    }

    @SuppressWarnings({"unchecked"})
    private static void drawDirectoryStructure(WebUi ui, String path, String dir, Map<String, Object> paths, String search) {
        ui.beginTree(path);

        List<String> files = (List<String>) paths.get("/");
        if (files != null) {
            for (String file : files) {
                if (!StringUtil.isEmpty(search) && !containsSearch("/" + StringUtil.replace(dir, "--", "/") + "/" + file, search)) {
                    continue;
                }

                ui.addBlockLink(file, "/admin/document/show?document=" + dir + "--" + file);
            }
            paths.remove("/");
        }

        paths.forEach((innerPath, structure) ->
            drawDirectoryStructure(ui, innerPath, dir + "--" + innerPath, (Map<String, Object>) structure, search)
        );

        if (dir.equals("document")) {
            ui.beginTree("dto objects");
            ui.addBlockLink("objects.md", "/admin/document/show/dtos");
            ui.containerEnd();
        }

        ui.containerEnd();
    }

    private static boolean containsSearch(String documentPath, String search) {
        String content = FileUtil.getFileContentFromClassPath(documentPath);
        if (StringUtil.isEmpty(content)) {
            content = FileUtil.getFileContent(documentPath);
        }
        if (StringUtil.isEmpty(content)) {
            return false;
        }

        return StringUtil.contains(content.toLowerCase(), search.toLowerCase());
    }

    public static void show(Params params, HttpServletResponse response) {
        show('/' + StringUtil.replace(params.getString("document"), "--", "/"), response, true);
    }

    public static void show(String documentPath, HttpServletResponse response, boolean fromClasspath) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        Node document = parser.parse(
            WebServiceDocumentCreator.getParsedMd(
                fromClasspath ? FileUtil.getFileContentFromClassPath(documentPath) : FileUtil.getFileContent(documentPath)
            )
        );
        String html = renderer.render(document);

        Response.writeString(
            response,
            "<html>" +
                "<head>" +
                "<link rel='stylesheet' type='text/css' href='/css/document" +
                    (StringUtil.contains(documentPath, "-fa") ? "-fa" : "-en") + ".css'>" +
                "</head>" +
                "<body>" + html + "</body>" +
                "</html>"
        );
    }

    public static String getParsedDocument(String documentPath, boolean fromClasspath) {
        return WebServiceDocumentCreator.getParsedMd(
            fromClasspath ? FileUtil.getFileContentFromClassPath(documentPath) : FileUtil.getFileContent(documentPath)
        );
    }

    public static void createDtoDocument() {
        DtoDocumentCreator.create();
    }
}
