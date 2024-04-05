package com.vantar.admin.documentation;

import com.vantar.admin.index.Admin;
import com.vantar.common.VantarParam;
import com.vantar.exception.FinishException;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.util.file.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminDocumentation {

    public static void index(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_MENU_DOCUMENTS, params, response, false);
        String lang = params.getLang();

        // > > > TAGS
        Set<String> tags = new HashSet<>(10, 1);
        DirUtil.browseByExtensionRecursive(
            FileUtil.getClassPathAbsolutePath("/document"),
            "md",
            file -> tags.addAll(extractTagsFromFile(FileUtil.getFileContent(file.getAbsolutePath())))
        );
        if (!tags.isEmpty()) {
            Object[] links = new String[tags.size() * 2 + 4];
            int i = -1;
            links[++i] = "ALL";
            links[++i] = "?lang=" + lang;
            for (String tag : tags) {
                links[++i] = tag;
                links[++i] = "?lang=" + lang + "&tag=" + tag;
            }
            links[++i] = Locale.getString(VantarKey.ADMIN_WEBSERVICE_INDEX_TITLE);
            links[++i] = "/admin/documentation/webservices/xlsx";
            ui.addHrefs(links);
        }

        // > > > SEARCH BOX
        String search = params.getString("search");
        ui  .beginFormGet()
            .addInput("Search keyword", "search", search)
            .addHidden("lang", lang)
            .addSubmit()
            .blockEnd();

        String tag = params.getString("tag");
        boolean isTag = StringUtil.isNotEmpty(tag);
        if (isTag) {
            search = "* " + tag;
        }

        new Browse(ui, search, isTag).draw();
        ui.finish();
    }

    public static void show(Params params, HttpServletResponse response) {
        show('/' + StringUtil.replace(params.getString("document"), "--", "/"), response, true, params.getString("tag"));
    }

    public static void showFromFile(String documentPath, HttpServletResponse response) {
        show(documentPath, response, false, null);
    }

    private static void show(String documentPath, HttpServletResponse response, boolean fromClasspath, String tag) {
        String md = fromClasspath ? FileUtil.getFileContentFromClassPath(documentPath) : FileUtil.getFileContent(documentPath);
        if (documentPath.contains(".p.")) {
            md = new WebServiceDocument().getViewableMd(md, tag);
        }
        showDocumentation(response, md, StringUtil.contains(documentPath, "-fa"));
    }

    public static void showDtoObjectDocument(HttpServletResponse response) {
        showDocumentation(response, DtoManifest.get(), false);
    }

    public static void showDtoObjectDocumentJson(HttpServletResponse response) {
        showDocumentation(response, DtoManifest.getJson(), false);
    }

    private static void showDocumentation(HttpServletResponse response, String mdText, boolean isFa) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(mdText);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        Response.writeString(
            response,
            "<html>" +
                "<head>" +
                "<link rel='stylesheet' type='text/css' href='/css/document" + (isFa ? "-fa" : "-en") +
                ".css?v=" + VantarParam.VERSION + "'>" +
                "</head>" +
                "<body>" + renderer.render(document) + "</body>" +
                "</html>"
        );
    }

    private static Set<String> extractTagsFromFile(String content) {
        Set<String> tags = new HashSet<>(5, 1);
        boolean inBlock = false;
        for (String line : StringUtil.split(content, '\n')) {
            line = line.trim();
            if (line.equals("### tags ###")) {
                inBlock = true;
                continue;
            }
            if (inBlock) {
                if (line.startsWith("#")) {
                    inBlock = false;
                    continue;
                }
                tags.add(StringUtil.remove(line, '*').trim());
            }
        }
        return tags;
    }
}
