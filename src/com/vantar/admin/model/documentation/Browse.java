package com.vantar.admin.model.documentation;

import com.vantar.util.file.FileUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.WebUi;
import java.util.*;


public class Browse {

    private final WebUi ui;
    private final String search; // filtered in paths
    private final boolean isTag; // plus filter in file


    public Browse(WebUi ui, String search, boolean isTag) {
        this.ui = ui;
        this.search = search;
        this.isTag = isTag;
    }

    public void draw() {
        Map<String, Object> structure = FileUtil.getResourceStructure("/document");
        if (search != null) {
            filterByContent("/document", structure);
        }
        drawDirectoryStructure("document", "document", structure);
    }

    @SuppressWarnings({"unchecked"})
    private void drawDirectoryStructure(String path, String dir, Map<String, Object> structure) {
        ui.beginTree(path);

        if (dir.equals("document")) {
            ui  .beginTree("DTO manifest")
                .addHrefBlockNewPage("dto details", "/admin/documentation/show/dtos")
                .addHrefBlockNewPage("dto json samples", "/admin/documentation/show/dtos/json")
                .blockEnd();
        }

        List<String> files = (List<String>) structure.get("/");
        if (files != null) {
            for (String file : files) {
                ui.addHrefBlockNewPage(
                    file,
                    "/admin/documentation/show?"
                        + (isTag ? "tag=" + StringUtil.remove(search, '*').trim() + "&" : "")
                        + "document=" + dir + "--" + file
                );
            }
            structure.remove("/");
        }

        structure.forEach((innerPath, sub) ->
            drawDirectoryStructure(innerPath, dir + "--" + innerPath, (Map<String, Object>) sub)
        );

        ui.blockEnd();
    }

    /**
     * remove entries that do not contain search
     * true = remove entry
     */
    @SuppressWarnings("unchecked")
    private boolean filterByContent(String dir, Map<String, Object> sub) {
        List<String> files = (List<String>) sub.get("/");
        if (files != null) {
            files.removeIf(file -> !fileContentsContainsKeyword(dir + "/" + file));
            if (files.isEmpty()) {
                sub.remove("/");
            }
        }
        if (sub.isEmpty()) {
            return true;
        }

        sub.entrySet().removeIf(e -> {
            String path = e.getKey();
            if ("/".equals(path)) {
                return false;
            }
            Object value = e.getValue();
            return filterByContent(dir + "/" + path, (Map<String, Object>) value);
        });
        return sub.isEmpty();
    }

    private boolean fileContentsContainsKeyword(String documentPath) {
        String content = FileUtil.getFileContentFromClassPath(documentPath);
        if (StringUtil.isEmpty(content)) {
            content = FileUtil.getFileContent(documentPath);
        }
        if (StringUtil.isEmpty(content)) {
            return false;
        }
        return StringUtil.contains(content.toLowerCase(), search.toLowerCase());
    }
}
