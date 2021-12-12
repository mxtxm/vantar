package com.vantar.admin.model;

import com.vantar.admin.model.document.*;
import com.vantar.common.Settings;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.util.file.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.slf4j.*;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;


public class AdminDocument {

    public static final Logger log = LoggerFactory.getLogger(AdminDocument.class);
    private static Set<String> tags;


    public static void index(Params params, HttpServletResponse response) {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_MENU_DOCUMENTS), params, response);
        if (ui == null) {
            return;
        }

        String lang = params.getLang();

        if (tags == null) {
            tags = getTags();
        }
        if (!tags.isEmpty()) {
            String[] links = new String[tags.size() * 2 + 2];
            int i = -1;
            links[++i] = "ALL";
            links[++i] = "?lang=" + lang;
            for (String tag : tags) {
                links[++i] = tag;
                links[++i] = "?lang=" + lang + "&tag=" + tag;
            }
            ui.addLinks(links);
        }

        String search = params.getString("search");
        ui  .beginFormGet()
            .addInput("Search", "search", search)
            .addHidden("lang", lang)
            .addSubmit()
            .containerEnd();

        String tag = params.getString("tag");
        boolean isTag = StringUtil.isNotEmpty(tag);
        if (isTag) {
            search = "* " + tag;
        }

        drawDirectoryStructure(ui, "document", "document", FileUtil.getResourceStructure("/document"), search, isTag);
        ui.finish();
    }

    @SuppressWarnings({"unchecked"})
    private static void drawDirectoryStructure(WebUi ui, String path, String dir, Map<String, Object> paths, String search, boolean isTag) {
        ui.beginTree(path);

        List<String> files = (List<String>) paths.get("/");
        if (files != null) {
            for (String file : files) {
                if (!StringUtil.isEmpty(search) && !containsSearch("/" + StringUtil.replace(dir, "--", "/") + "/" + file, search)) {
                    continue;
                }

                ui.addBlockLink(
                    file,
                    "/admin/document/show?"
                        + (isTag ? "tag=" + StringUtil.remove(search, '*').trim() + "&" : "")
                        + "document=" + dir + "--" + file
                );
            }
            paths.remove("/");
        }

        paths.forEach((innerPath, structure) ->
            drawDirectoryStructure(ui, innerPath, dir + "--" + innerPath, (Map<String, Object>) structure, search, isTag)
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
        show('/' + StringUtil.replace(params.getString("document"), "--", "/"), response, true, params.getString("tag"));
    }

    public static void show(String documentPath, HttpServletResponse response, boolean fromClasspath) {
        show(documentPath, response, fromClasspath, null);
    }

    public static void show(String documentPath, HttpServletResponse response, boolean fromClasspath, String tag) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        Node document = parser.parse(
            WebServiceDocumentCreator.getParsedMd(
                fromClasspath ? FileUtil.getFileContentFromClassPath(documentPath) : FileUtil.getFileContent(documentPath),
                tag
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
            fromClasspath ? FileUtil.getFileContentFromClassPath(documentPath) : FileUtil.getFileContent(documentPath),
            null
        );
    }

    public static void createDtoDocument() {
        DtoDocumentCreator.create();
    }

    public static Set<String> getTags() {
        Set<String> tags = new HashSet<>();
        DirUtil.browseByExtensionRecursive(
            Settings.config.getProperty("documents.dir.raw"),
            "md",
            new DirUtil.Callback() {
                @Override
                public void found(File file) {
                    tags.addAll(WebServiceDocumentCreator.getTags(FileUtil.getFileContent(file.getAbsolutePath())));
                }
            }
        );
        return tags;
    }

    public static void createAllDocuments() {
        log.info("> creating documents");
        AdminDocument.createDtoDocument();
        log.info("created dto document");

        try {
            DirUtil.copy(
                Settings.config.getProperty("documents.dir.raw"),
                Settings.config.getProperty("documents.dir.release")
            );
        } catch (IOException e) {
            return;
        }

        DirUtil.browseByExtensionRecursive(
            Settings.config.getProperty("documents.dir.release"),
            "md",
            file -> FileUtil.write(file.getAbsolutePath(), AdminDocument.getParsedDocument(file.getAbsolutePath(), false))
        );
        log.info("released all documents");
        log.info("< finished creating documents");
    }
}
