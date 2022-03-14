package com.vantar.admin.model;

import com.vantar.admin.model.document.*;
import com.vantar.common.Settings;
import com.vantar.exception.FinishException;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.util.file.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminDocument {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(Locale.getString(VantarKey.ADMIN_MENU_DOCUMENTS), params, response, false);

        String lang = params.getLang();


        Set<String> tags = getTags();
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

    public static void showFromFile(String documentPath, HttpServletResponse response) {
        show(documentPath, response, false, null);
    }

    private static void show(String documentPath, HttpServletResponse response, boolean fromClasspath, String tag) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(
            WebServiceDocumentCreator.getParsedMd(
                fromClasspath ? FileUtil.getFileContentFromClassPath(documentPath) : FileUtil.getFileContent(documentPath),
                tag
            )
        );
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        Response.writeString(
            response,
            "<html>" +
                "<head>" +
                "<link rel='stylesheet' type='text/css' href='/css/document" +
                    (StringUtil.contains(documentPath, "-fa") ? "-fa" : "-en") + ".css'>" +
                "</head>" +
                "<body>" + renderer.render(document) + "</body>" +
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
            FileUtil.getClassPathAbsolutePath("/document"),
            "md",
            file -> tags.addAll(WebServiceDocumentCreator.getTags(FileUtil.getFileContent(file.getAbsolutePath())))
        );

        return tags;
    }

    public static void createAllDocuments() {
        Admin.log.info(" >> creating documents");
        AdminDocument.createDtoDocument();
        Admin.log.info(" created dto document <<");

        if (!FileUtil.exists(Settings.config.getProperty("documents.dir.release"))) {
            Admin.log.info(" >> not released all documents");
            Admin.log.info(" << finished creating documents");
            return;
        }

        try {
            DirUtil.removeDirectory(Settings.config.getProperty("documents.dir.release"));
            DirUtil.copy(
                FileUtil.getClassPathAbsolutePath("/document"),
                Settings.config.getProperty("documents.dir.release")
            );

            DirUtil.browseByExtensionRecursive(
                Settings.config.getProperty("documents.dir.release"),
                "md",
                file -> FileUtil.write(file.getAbsolutePath(), AdminDocument.getParsedDocument(file.getAbsolutePath(), false))
            );
        } catch (Exception e) {
            Admin.log.info(" !! failed to released all documents");
            return;
        }

        Admin.log.info(" >> released all documents");
        Admin.log.info(" << finished creating documents");
    }
}
