package com.vantar.web;

import com.vantar.admin.model.*;
import com.vantar.common.VantarParam;
import com.vantar.database.dto.*;
import com.vantar.database.query.PageData;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.service.log.ServiceUserActionLog;
import com.vantar.service.log.dto.UserLog;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.json.*;
import com.vantar.util.object.*;
import com.vantar.util.string.*;
import org.slf4j.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;


public class WebUi {

    private static final Logger log = LoggerFactory.getLogger(WebUi.class);

    public static final String PARAM_CONFIRM = "confirm";
    public static final String COLSPAN_SEPARATOR = ":::";
    public static final String LINK_SEPARATOR = ">>>";

    public Params params;
    private final HttpServletResponse response;
    private boolean written;

    private StringBuilder html = new StringBuilder(100000);
    private final Stack<String> openTags = new Stack<>();
    private Set<String> js;

    private String authToken;
    private String lang;
    private final String direction;
    private final String boxFloat;
    private final String align;
    private final String alignKey;
    private final String alignValue;
    private StringBuilder additive;
    private final HtmlEscape escape = new HtmlEscape();


    public WebUi(Params params, HttpServletResponse response) {
        authToken = params.getString(VantarParam.AUTH_TOKEN);
        lang = params.getLang();
        if (lang.equals("fa")) {
            direction = "rtl";
            boxFloat = "float-right";
            align = "right";
            alignKey = "left";
            alignValue = "right";
        } else {
            lang = "en";
            direction = "ltr";
            boxFloat = "float-left";
            align = "left";
            alignKey = "right";
            alignValue = "left";
        }
        this.params = params;
        this.response = response;
    }

    public WebUi setAuthToken(String token) {
        authToken = token;
        return this;
    }

    // > > > TEXT AND SPACE AND LINE

    public WebUi addText(String text) {
        html.append(escapeWithNtoBr(text)).append('\n');
        return this;
    }

    public WebUi addTextLine(String text) {
        html.append(escapeWithNtoBr(text)).append("<br/>\n");
        return this;
    }

    public WebUi addEmptyLine() {
        html.append("<br/>");
        return this;
    }

    public WebUi addEmptyLine(int count) {
        for (int i = 0 ; i < count ; ++i) {
            html.append("<br/>");
        }
        return this;
    }

    public WebUi addLine() {
        html.append("<div class='line'></div>\n");
        return this;
    }

    // < < < TEXT AND SPACE AND LINE

    // > > > HEADERS AND TITLE

    public WebUi addPageTitle(String text) {
        html.append("<h1>").append(escapeWithNtoBr(text)).append("</h1>\n");
        return this;
    }

    public WebUi addHeading(int h, String text) {
        html.append("<h").append(h).append(">").append(escapeWithNtoBr(text)).append("</h").append(h).append(">\n");
        return this;
    }

    public WebUi addHeading(String text) {
        html.append("<h2>").append(escapeWithNtoBr(text)).append("</h2>\n");
        return this;
    }

    // < < < HEADERS AND TITLE

    // > > > LINKS AND URLS

    public WebUi addBlockLink(String text, String url) {
        return addLink(text, url, false, true);
    }

    public WebUi addBlockLinkNewPage(String text, String url) {
        return addLink(text, url, true, true);
    }

    public WebUi addLink(String msg, String url) {
        return addLink(msg, url, false, false);
    }

    public WebUi addLinkNewPage(String msg, String url) {
        return addLink(msg, url, true, false);
    }

    public WebUi addLinks(String... values) {
        html.append("<div class='actions'>\n");
        for (int i = 0, l = values.length; i < l; ++i) {
            if (i > 0 && i < l-1) {
                html.append(" | ");
            }
            addLink(values[i], values[++i], false, false);
        }
        html.append("</div>\n");
        return this;
    }

    private WebUi addLink(String text, String url, boolean newWindow, boolean block) {
        html.append(getLink(text, url, newWindow, block));
        return this;
    }

    public String getLink(String text, String url, boolean newWindow, boolean block) {
        StringBuilder s = new StringBuilder();
        if (block) {
            s.append("<p class='link'>\n");
        }
        s.append("<a");
        if (newWindow) {
            s.append(" target='_blank'");
        }
        s.append(" href='").append(getCompleteLink(url)).append("'>").append(escapeWithNtoBr(text)).append("</a>\n");
        if (block) {
            s.append("</p>\n");
        }
        return s.toString();
    }

    public void redirect(String url) {
        Response.redirect(response, getCompleteLink(url));
    }

    private String getCompleteLink(String url) {
        if (StringUtil.contains(url, "://")) {
            return url;
        }

        StringBuilder sb = new StringBuilder(100);

        String[] urlQuery = StringUtil.splitTrim(url, '?');
        sb.append(urlQuery[0]).append('?');
        if (urlQuery.length > 1) {
            for (String q : StringUtil.splitTrim(urlQuery[1], '&')) {
                if (q.startsWith(VantarParam.AUTH_TOKEN + "=") || q.startsWith(VantarParam.LANG + "=")) {
                    continue;
                }
                sb.append(q).append('&');
            }
        }

        if (lang != null) {
            sb.append(VantarParam.LANG).append('=').append(lang).append('&');
        }
        if (authToken != null) {
            sb.append(VantarParam.AUTH_TOKEN).append('=').append(authToken).append('&');
        }

        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    // > > > LINKS AND URLS

    // > > > BOX

    public WebUi addTag(String tag) {
        html.append("<div class='tag'>").append(escapeWithNtoBr(tag)).append("</div>\n");
        return this;
    }

    public WebUi beginMain() {
        openTags.push("</main>\n");
        html.append("<main>\n");
        return this;
    }

    public WebUi addPre(String msg) {
        html.append("<pre>\n").append(msg).append("</pre>\n");
        return this;
    }

    public WebUi beginPre() {
        openTags.push("</pre>\n");
        html.append("<pre>\n");
        return this;
    }

    public WebUi beginBox() {
        openTags.push("</div>\n");
        html.append("<div class='solid-box clearfix'>\n");
        return this;
    }

    public WebUi beginBox(String title) {
        openTags.push("    </div>\n</div>\n");
        html.append("<div class='solid-box-empty clearfix'>\n    <h2 class='box-title'>")
            .append(escapeWithNtoBr(title))
            .append("</h2>\n    <div class='solid-box-clear clearfix'>\n");
        return this;
    }

    public WebUi addBoxWithNoEscape(String content) {
        html.append("<div class='box-content'>").append(content).append("</div>");
        return this;
    }

    public WebUi beginBox2(String title) {
        openTags.push("    </div>\n</div>\n");
        html.append("<div class='solid-box-empty clearfix'>\n    <h4 class='box-title2'>")
            .append(escapeWithNtoBr(title))
            .append("</h4>\n    <div class='solid-box-clear clearfix'>\n");
        return this;
    }

    public WebUi beginTree(String title) {
        openTags.push("</div>\n");
        html.append("<div class='tree'>\n    <h2 class='tree-title'>").append(escapeWithNtoBr(title)).append("</h2>\n");
        return this;
    }

    public WebUi beginFloatBox() {
        openTags.push("</div>\n");
        html.append("<div class='float-box ").append(boxFloat).append("'>\n");
        return this;
    }

    public WebUi beginFloatBox(String tClass, String... title) {
        openTags.push("    </div>\n</div>\n");
        StringBuilder t = new StringBuilder(escapeWithNtoBr(title[0]));
        for (int i = 1; i < title.length; ++i) {
            t.append("    <p>").append(escapeWithNtoBr(escapeWithNtoBr(title[i]))).append("</p>\n");
        }
        html.append("<div class='float-box-empty clearfix ").append(boxFloat).append(" ").append(tClass)
            .append("'>\n    <h2 class='box-title'>").append(t).append("</h2>\n    <div class='solid-box-clear clearfix'>\n");
        return this;
    }

    public WebUi beginAlignRight() {
        openTags.push("</div>\n");
        html.append("<div style='direction:rtl!important'>\n");
        return this;
    }

    public WebUi beginAlignLeft() {
        openTags.push("</div>\n");
        html.append("<div style='direction:left!important'>\n");
        return this;
    }

    public WebUi addKeyValueFail(Object key, Object value) {
        return addKeyValue(key, value, "error");
    }

    public WebUi addKeyValue(Object key, Object value) {
        return addKeyValue(key, value, "");
    }

    public WebUi addKeyValue(Object key, Object value, String classs) {
        key = StringUtil.replace(escape(key == null ? "" : key.toString()), "\n", "<br/>");
        value = StringUtil.replace(escape(value == null ? "NULL" : value.toString()), "\n", "<br/><br/>");
        String kc = StringUtil.isEmpty(classs) ? "" : " key-" + classs;
        String vc = StringUtil.isEmpty(classs) ? "" : " value-" + classs;

        html.append("<div class='kv-flex-container' style='direction:").append(direction).append("; justify-content:")
            .append(alignValue).append("; align-items:").append(alignValue).append("'>\n")
            .append("<label class='kv-key").append(kc).append("' style='overflow-wrap:break-word;text-align:")
            .append(alignKey).append("'>")
            .append(key).append("</label>").append("<label class='kv-value").append(vc).append("' style='text-align:")
            .append(alignValue).append("'>").append(value).append("</label>").append("</div>");
        return this;
    }

    public WebUi addKeyValueExtendable(Object key, Object value, String hValue) {
        key = StringUtil.replace(escape(key == null ? "" : key.toString()), "\n", "<br/>");
        value = StringUtil.replace(escape(value == null ? "NULL" : value.toString()), "\n", "<br/><br/>");

        html.append("<div class='kv-flex-container' style='direction:").append(direction).append("; justify-content:")
            .append(alignValue).append("; align-items:").append(alignValue).append("'>\n")
            .append("<label class='kv-key' style='overflow-wrap:break-word;text-align:").append(alignKey).append("'>")
            .append(key).append("</label>").append("<label class='kv-value' style='text-align:")
            .append(alignValue).append("' onclick='$(this).find(\"pre\").toggle()'>")
            .append(value).append("<br/><pre style='display:none'>")
            .append(hValue).append("</pre></label>").append("</div>");

        setJs("/js/jquery.min.js");
        return this;
    }

    public WebUi addLogRow(UserLog userLog, CommonUser user) {
        html.append("<div class='log-row clearfix'>");

        html.append("<div class='log-col-user'>")
            .append("<p class='action'>").append(userLog.action).append("</p>")
            .append("<p class='url'>").append(userLog.url).append("</p>")
            .append("<p class='request-type'>").append(userLog.requestType).append("</p>")
            .append("<p class='thread-id'>").append("thread: ").append(userLog.threadId).append("</p>")
            .append("<p class='time'>").append(userLog.time.formatter().getDateTimePersianAsString()).append("</p>");
        if (user != null) {
            html.append("<p class='user'>(").append(user.getId()).append(") ")
                .append(user.getUsername()).append(" - ").append(user.getFullName()).append("</p>");
        }
        html.append("</div>");

        html.append("<div class='log-col-request'>")
            .append("<p class='ip'>").append(userLog.ip).append("</p><pre class='headers'>");
        if (userLog.headers != null) {
            userLog.headers.forEach((k, v) -> {
                html.append("<strong>").append(k).append("</strong>: ").append(escape(v)).append("\n");
            });
        }
        html.append("</pre></div>");

        html.append("<div class='log-col-data'><pre class='object'>");
        if (userLog.className != null) {
            html.append("<strong>").append(escape(userLog.className));
            if (userLog.objectId != null) {
                html.append(" (").append(userLog.objectId).append(")");
            }
            html.append("</strong>\n");
        }
        if (userLog.object == null) {
            // nothing
        } else if (Json.isJsonShallow(userLog.object)) {
            html.append(escape(Json.d.toJsonPretty(userLog.object)));
        } else {
            html.append(escape(userLog.object));
        }
        if (userLog.uploadedFiles != null) {
            html.append("\n\n<strong>files:</strong>");
            for (String file : userLog.uploadedFiles) {
                html.append(escape(file)).append("\n");
            }
        }
        html.append("</pre></div></div>");

        return this;
    }

    public WebUi beginContainer(String id, String className) {
        openTags.push("</div>");
        html.append("<div");
        if (id != null) {
            html.append(" id='").append(id).append("'");
        }
        if (className != null) {
            html.append(" class='").append(className).append("'>\n");
        }
        return this;
    }

    public WebUi containerEnd() {
        if (openTags.size() > 0) {
            html.append(openTags.pop());
        }
        return this;
    }

    // < < < BOX

    // > > > TABLE

    public WebUi beginTable() {
        openTags.push("</table>");
        html.append("<table class='table'>");
        return this;
    }

    public WebUi addTableHeader(String... cols) {
        html.append("<tr>");
        for (String col : cols) {
            if (StringUtil.contains(col, COLSPAN_SEPARATOR)) {
                String[] parts = StringUtil.splitTrim(col, COLSPAN_SEPARATOR);
                html.append("<th colspan='").append(parts[1]).append("'>").append(parts[0]).append("</th>");
                continue;
            }
            html.append("<th>").append(col).append("</th>");
        }
        html.append("</tr>");
        return this;
    }

    public WebUi addTableRow(Object... cols) {
        html.append("<tr>");
        for (Object col : cols) {
            if (col instanceof String) {
                if (StringUtil.contains((String) col, COLSPAN_SEPARATOR)) {
                    String[] parts = StringUtil.splitTrim((String) col, COLSPAN_SEPARATOR);
                    html.append("<td colspan='").append(parts[1]).append("'><div>").append(parts[0]).append("</div></td>");
                    continue;
                }
                if (StringUtil.contains((String) col, LINK_SEPARATOR)) {
                    String[] parts = StringUtil.splitTrim((String) col, LINK_SEPARATOR);
                    html.append("<td><div>");
                    addLink(parts[0], parts[1], true, false);
                    html.append("<div></td>");
                    continue;
                }
            }
            html.append("<td><div>").append(col).append("<div></td>");
        }
        html.append("</tr>");
        return this;
    }

    public WebUi addTableRowComment(Object col, int colspan) {
        html.append("<tr><td colspan='").append(colspan).append("'><pre class='red'>").append(col).append("</pre></td></tr>");
        return this;
    }

    // < < < TABLE

    // > > > MENU

    public WebUi addMenu(Map<String, String> menu, String... text) {
        html.append("<ul id='menu' class='").append(direction).append("'>\n");

        boolean isFirst = true;
        for (Map.Entry<String, String> entry : menu.entrySet()) {
            html.append("    <li");
            if (isFirst && direction.equals("ltr")) {
                html.append(" style='border: none!important'");
            }
            html.append("><a href='").append(getCompleteLink(entry.getValue())).append("'>").append(entry.getKey())
                .append("</a></li>\n");
            isFirst = false;
        }

        if (text.length > 0) {
            html.append("    <li class='signout'><a href='").append("/admin/signout").append("'>")
                .append(Locale.getString(VantarKey.ADMIN_MENU_SIGN_OUT)).append("</a></li>\n");
        }

        String path = params.request.getRequestURI();
        String q = params.request.getQueryString();
        if (StringUtil.isNotEmpty(q)) {
            path += "?" + q;
        }

        String l = lang;
        if (Locale.getLangs() != null) {
            for (String lang : Locale.getLangs()) {
                this.lang = lang;
                html.append("    <li class='menu-lang'><a href='").append(getCompleteLink(path)).append("'>").append(lang)
                    .append("</a></li>\n");
            }
        }
        lang = l;

        for (String t: text) {
            html.append("    <li class='menu-text'>").append(t).append("</li>\n");
        }
        html.append("</ul>\n");
        return this;
    }

    public WebUi setBreadcrumb(String title, DtoDictionary.Info dtoInfo) {
        html.append("<h1>")
            .append(dtoInfo.category)
            .append(" &gt; ")
            .append("<a href='").append(getCompleteLink("/admin/data/list?dto=" + dtoInfo.getDtoClassName()))
            .append("'>").append(dtoInfo.title).append("</a>")
            .append(" &gt; ")
            .append(title)
            .append("</h1>\n");
        return this;
    }

    // < < < MENU

    // > > > MESSAGE

    public WebUi addMessage(String msg) {
        html.append("<p class='msg'>\n").append(escapeWithNtoBr(msg)).append("</p>\n");
        return this;
    }

    public WebUi addErrorMessage(String msg) {
        html.append("<p class='error'>\n").append(escapeWithNtoBr(msg)).append("</p>\n");
        return this;
    }

    public WebUi addErrorMessage(Throwable e) {
        return addErrorMessage(e.getMessage(), ObjectUtil.throwableToString(e));
    }

    public WebUi addErrorMessage(String msg, String comment) {
        String id = "i" + StringUtil.getRandomString(10);
        html.append("<p class='error' onclick='$(\"#").append(id).append("\").toggle()'>\n")
            .append(escapeWithNtoBr(msg)).append("</p>\n").append("<pre id='").append(id)
            .append("' class='error' style='display:none'>\n").append(escapeWithNtoBr(comment)).append("</pre>\n");

        setJs("/js/jquery.min.js");
        return this;
    }

    // < < < MESSAGE

    // > > > FORM

    public WebUi beginFormGet() {
        return beginForm("get", null, false);
    }

    public WebUi beginFormGet(String action) {
        return beginForm("get", action, false);
    }

    public WebUi beginFormPost() {
        return beginForm("post", null, false);
    }

    public WebUi beginFormPost(String action) {
        return beginForm("post", action, false);
    }

    public WebUi beginUploadForm() {
        return beginForm("post", null, true);
    }

    public WebUi beginUploadForm(String action) {
        return beginForm("post", action, true);
    }

    private WebUi beginForm(String method, String action, boolean multiPart) {
        openTags.push("</form>\n");
        html.append("<form ");
        if (multiPart) {
            html.append("enctype='multipart/form-data' ");
        }
        if (action != null) {
            html.append("action='").append(action).append("' ");
        }
        html.append("method='").append(method).append("'>\n    <input type='hidden' name='f' value='1'/>\n");
        String q = params.request.getQueryString();
        if (StringUtil.isNotEmpty(q)) {
            if (StringUtil.contains(q, VantarParam.AUTH_TOKEN + "=") && authToken != null){
                html.append("    <input type='hidden' name='" + VantarParam.AUTH_TOKEN + "' value='")
                    .append(authToken).append("'/>\n");
            }
            if (StringUtil.contains(q, VantarParam.LANG + "=") && lang != null){
                html.append("    <input type='hidden' name='" + VantarParam.LANG + "' value='")
                    .append(lang).append("'/>\n");
            }
        }
        return this;
    }

    // < < < FORMS

    // > > > INPUTS

    public WebUi addHidden(String key, String value) {
        html.append("<input type='hidden' id='").append(key).append("' name='").append(key).append("' value='")
            .append(value == null ? "" : value).append("'/>\n");
        return this;
    }

    public WebUi addCheckbox(String label, String name) {
        return addCheckbox(label, name, null, "1");
    }

    public WebUi addCheckbox(String label, String name, String value) {
        return addCheckbox(label, name, null, value);
    }

    public WebUi addCheckbox(String label, String name, Boolean checked) {
        return addCheckbox(label, name, checked, "1");
    }

    public WebUi addCheckbox(String label, String name, Boolean checked, String value) {
        return addWidgetRow(label, name,
            "<input style='margin-top:5px; direction:" + alignValue + "' type='checkbox' value='" + value
            + "' name='" + name+ "' id='" + name + "' "
            + (checked != null && checked ? " checked='checked'" : "") + " />\n");
    }

    public String getCheckbox(String name, String value) {
        return "<span class='check-box-container'>" + "<input type='checkbox' value='" + value + "' name='" + name+ "' "
            + "style='direction:" + alignValue + "' /></span>";
    }

    public WebUi addInput(String label, String name) {
        return addInput(label, name, "");
    }

    public WebUi addInput(String label, String name, Object defaultValue) {
        return addInput(label, name, defaultValue == null ? null : defaultValue.toString(), "text");
    }

    public WebUi addPassword(String label, String name) {
        return addInput(label, name, null, "password");
    }

    public WebUi addFile(String label, String name) {
        return addInput(label, name, null, "file");
    }
    //autocomplete="off"
    public WebUi addInput(String label, String name, String defaultValue, String type) {
        return addInput(label, name, defaultValue, type, null);
    }

    public WebUi addInput(String label, String name, String defaultValue, String type, String classs) {
        String autoComplete = StringUtil.contains(name, "password") ? "new-password'" : "nope'";
        return addWidgetRow(label, name,
            "<input autocomplete='" + autoComplete + " type='" + type + "' name='" + name+ "' id='" + name + "' "
                + (classs == null ? " " : "class='" + classs + "' ")
                + (defaultValue == null ? "" : ("value='" + defaultValue) + "' ") + "style='direction:" + alignValue + "' />\n");
    }

    public WebUi addTextArea(String label, String name) {
        return addTextArea(label, name, null, null);
    }

    public WebUi addTextArea(String label, String name, String defaultValue) {
        return addTextArea(label, name, defaultValue, null);
    }

    public WebUi addTextArea(String label, String name, Object defaultValue, String tClass) {
        return addWidgetRow(label, name,
            "    <textarea autocomplete='off' class='vtx " + (tClass == null ? "" : tClass) + "' name='" + name
            + "' id='" + name + "' style='direction:" + alignValue + "'>\n"
            + (defaultValue == null ? "" : defaultValue) + "</textarea>\n");
    }

    public WebUi addSubmit() {
        return addSubmit(Locale.getString(VantarKey.ADMIN_SUBMIT));
    }

    public WebUi addSubmit(String label) {
        return addWidgetRow("", "",
            "    <button style='direction:" + alignValue + "' type='submit'>" + label + "</button>\n");
    }

    public WebUi addButton(String label, String id) {
        return addWidgetRow("", "",
            "    <button style='direction:" + alignValue + "' class= type='button' id='" + id + "'>" + label + "</button>\n");
    }

    public WebUi addSelect(String label, String name, List<? extends Dto> dtos) {
        return addSelect(label, name, dtos, null);
    }

    public WebUi addSelect(String label, String name, List<? extends Dto> dtos, Long defaultValue) {
        Map<String, String> items = new HashMap<>();
        for (Dto dto : dtos) {
            items.put(Long.toString(dto.getId()), dto.getPresentationValue());
        }
        return addSelectX(label, name, items, defaultValue, false, null);
    }

    public WebUi addSelect(String label, String name, Map<String, String> items) {
        return addSelectX(label, name, items, null, false, null);
    }

    public WebUi addSelect(String label, String name, Map<String, String> items, String defaultValue) {
        return addSelectX(label, name, items, defaultValue, false, null);
    }

        public WebUi addSelect(String label, String name, Map<String, String> items, String defaultValue, boolean multi) {
        return addSelectX(label, name, items, defaultValue, multi, null);
    }

    public WebUi addSelect(String label, String name, String[] items) {
        return addSelectX(label, name, items, null, false, null);
    }

    public WebUi addSelect(String label, String name, String[] items, Object defaultValue) {
        return addSelectX(label, name, items, defaultValue, false, null);
    }

    public WebUi addSelect(String label, String name, String[] items, Object defaultValue, boolean multi) {
        return addSelectX(label, name, items, defaultValue, multi, null);
    }

    public WebUi addSelect(String label, String name, String[] items, Object defaultValue, boolean multi, String classs) {
        return addSelectX(label, name, items, defaultValue, multi, classs);
    }

    private WebUi addSelectX(String label, String name, Object items, Object defaultValue, boolean multi, String classs) {
        StringBuilder select = new StringBuilder();
        select.append("    <select name='").append(name).append("' id='").append(name).append("' style='direction:")
            .append(alignValue).append("'");
        if (multi) {
            select.append(" class='multi-select ").append(classs == null ? "" : classs)
                .append("' autocomplete='off' multiple='multiple'>\n");
        } else {
            select.append(">\n    <option></option>\n");
        }

        if (items instanceof Map) {
            for (Map.Entry<String, String> item : ((Map<String, String>) items).entrySet()) {
                select.append("        <option value='").append(item.getKey()).append("'");
                if (item.getKey().equals(defaultValue)) {
                    select.append(" selected='selected'");
                }
                select.append(">").append(item.getValue()).append("</option>\n");
            }
        } else {
            for (String item : (String[]) items) {
                select.append("        <option value='").append(item).append("'");
                if (defaultValue != null) {
                    if (multi) {
                        for (String o : (List<String>) defaultValue) {
                            if (item.equals(o)) {
                                select.append(" selected='selected'");
                            }
                        }
                    } else if (item.equals(defaultValue)) {
                        select.append(" selected='selected'");
                    }
                }
                select.append(">").append(item).append("</option>\n");
            }
        }

        select.append("    </select>\n");
        return addWidgetRow(label, name, select.toString());
    }

    private WebUi addWidgetRow(String label, String name, String widget) {
        html.append("<div class='flex-container' style='direction:").append(direction)
            .append("; justify-content:").append(align).append("; align-items:").append(align).append("'>\n")
            .append("<label style='text-align:").append(alignKey).append("' for='").append(name).append("'>\n")
            .append(label).append("</label>\n")
            .append(widget);
        if (additive != null) {
            html.append(additive);
            additive = null;
        }
        html.append("</div>\n");
        return this;
    }

    // < < < INPUTS

    // > > > TOOLS

    public WebUi addImportForm(String data) {
        beginFormPost();
        addTextArea(Locale.getString(VantarKey.ADMIN_DATA), "import", escape(data), "large ltr");
        addCheckbox(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL), "deleteall");
        addSubmit(Locale.getString(VantarKey.ADMIN_DATA_ENTRY));
        return containerEnd();
    }

    public WebUi addPurgeForm() {
        beginFormPost();
        addErrorMessage(Locale.getString(VantarKey.ADMIN_DELETE));
        addCheckbox(Locale.getString(VantarKey.ADMIN_DELETE_ALL_CONFIRM), PARAM_CONFIRM);
        addSubmit(Locale.getString(VantarKey.ADMIN_DELETE));
        return containerEnd();
    }

    public WebUi addDeleteForm(List<Dto> dtos, List<String> fields) {
        beginFormPost();
        addErrorMessage(Locale.getString(VantarKey.ADMIN_DELETE)).addEmptyLine();

        boolean isLogical = dtos.size() > 0 && dtos.get(0).isDeleteLogicalEnabled();
        if (isLogical) {
            html.append("<div>");
            addCheckbox(Locale.getString(VantarKey.LOGICAL_DELETED), VantarParam.LOGICAL_DELETED);
            html.append("<div>");
            addCheckbox(Locale.getString(VantarKey.LOGICAL_DELETED_UNDO), VantarParam.LOGICAL_DELETED_UNDO);
            html.append("</div><br/><br/>");
        }

        for (Dto dto : dtos) {
            html.append("<div class='row delete-item'><input name='ids' type='checkbox' value='")
                .append(dto.getId()).append("'/> ");
            for (String name : fields) {
                Object value = dto.getPropertyValue(name);
                html.append(value).append(" - ");
            }
            html.setLength(html.length() - 3);
            html.append("</div>");
        }
        addSubmit(isLogical ? Locale.getString(VantarKey.ADMIN_DO) : Locale.getString(VantarKey.ADMIN_DELETE));
        return containerEnd();
    }

    public WebUi addDtoAddForm(Dto dto, String... fields) {
        return addDtoForm(dto, "insert", fields);
    }

    public WebUi addDtoUpdateForm(Dto dto, String... fields) {
        return addDtoForm(dto, "update", fields);
    }

    private void setAdditive(Dto dto, String name, Class<? extends Annotation> annotation, String msg) {
        if (dto.hasAnnotation(name, annotation)) {
            if (additive == null) {
                additive = new StringBuilder();
            }
            additive.append(msg);
        }
    }

    private void setAdditive(String msg) {
        if (additive == null) {
            additive = new StringBuilder();
        }
        additive.append(msg);
    }

    /**
     * Tags: not exists: include, none: exclude, insert/update: include for action
     */
    private WebUi addDtoForm(Dto dto, String action, String... include) {
        html.append("<form method='post' id='dto-form' autocomplete='off' ><input name='f' value='1' type='hidden'/>\n");
        addSubmit(Locale.getString(VantarKey.ADMIN_DO));

        for (String name : include) {
            Tags tags = dto.getField(name).getAnnotation(Tags.class);
            if (tags != null
                && (!CollectionUtil.contains(tags.value(), action) || CollectionUtil.contains(tags.value(), "none"))) {
                continue;
            }

            setAdditive(dto, name, Required.class, "<pre class='required'>*</pre>");
            setAdditive(dto, name, Unique.class, "<pre class='unique'>unique</pre>");
            Default a = dto.getAnnotation(name, Default.class);
            if (a != null) {
                setAdditive(
                    dto,
                    name, Default.class, "<pre class='default' title=\"default value\">"
                        + dto.getAnnotation(name, Default.class).value() + "</pre>"
                );
            }
            setAdditive(dto, name, Localized.class, "<pre class='format'>{\"en\":\"\", \"fa\":\"\"}</pre>");

            Field field = dto.getField(name);
            Class<?> type = dto.getPropertyType(name);
            Object value = dto.getPropertyValue(name);
            if (value == null || (type == String.class && StringUtil.isEmpty((String) value))) {
                value = dto.getDefaultValue(name);
            }

            if (ClassUtil.isInstantiable(type, Number.class)) {
                addInput(name, name, value == null ? null : value.toString());

            } else if (type == Boolean.class) {
                addSelect(name, name, new String[] {"Yes", "No"}, value == null ? null : ((Boolean) value ? "Yes" : "No"));

            } else if (type.isEnum()) {
                final Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) type;
                String[] values = new String[enumType.getEnumConstants().length];
                int i = 0;
                for (Enum<?> x : enumType.getEnumConstants()) {
                    values[i++] = x.toString();
                }
                addSelect(name, name, values, value == null ? null : value.toString());

            } else if (ClassUtil.isInstantiable(type, CollectionUtil.class) && dto.getPropertyGenericTypes(name)[0].isEnum()) {
                final Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) dto.getPropertyGenericTypes(name)[0];
                String[] values = new String[enumType.getEnumConstants().length];
                int i = 0;
                for (Enum<?> x : enumType.getEnumConstants()) {
                    values[i++] = x.toString();
                }

                List<String> selectedValues;
                if (value == null) {
                    selectedValues = null;
                } else {
                    selectedValues = new ArrayList<>();
                    for (Object x : (List<?>) value) {
                        selectedValues.add(x.toString());
                    }
                }
                addSelect(name, name, values, value == null ? null : selectedValues, true, "json");

            } else if (CollectionUtil.isCollection(type)) {
                Class<?>[] g = ClassUtil.getGenericTypes(field);
                if (ObjectUtil.isNotEmpty(g)) {
                    if (ClassUtil.isInstantiable(g[0], Dto.class)) {
                        Object obj = ClassUtil.getInstance(g[0]);
                        if (obj != null) {
                            setAdditive("<pre class='format'>[" + Json.getWithNulls().toJsonPretty(obj) + "]</pre>");
                        }
                    } else {
                        setAdditive("<pre class='format'>[" + g[0].getSimpleName() + "]</pre>");
                    }
                }
                addTextArea(
                    name,
                    name,
                    value == null ? null : Json.getWithNulls().toJsonPretty(value),
                    "small json"
                );

            } else if (CollectionUtil.isMap(type)) {
                Class<?>[] g = ClassUtil.getGenericTypes(field);
                if (ObjectUtil.isNotEmpty(g) && g.length == 2) {
                    setAdditive("<pre class='format'>{" + g[0].getSimpleName() + ": " + g[1].getSimpleName() + "}</pre>");
                }
                addTextArea(
                    name,
                    name,
                    value == null ? null : Json.getWithNulls().toJsonPretty(value),
                    "small json"
                );

            } else if (ClassUtil.isInstantiable(type, Dto.class)) {
                DtoDictionary.Info info = DtoDictionary.get(type);
                if (info == null) {
                    log.error("! dto not in dictionary? ({})", type);
                } else {
                    Dto dtoX = info.getDtoInstance();
                    if (dtoX == null) {
                        log.error("! dto not in dictionary? ({})", type);
                    } else {
                        setAdditive("<pre class='format'>" + Json.getWithNulls().toJsonPretty(dtoX) + "</pre>");
                    }
                }
                addTextArea(
                    name,
                    name,
                    value == null ? null : Json.getWithNulls().toJsonPretty(value),
                    "small json"
                );

            } else {
                if (Json.isJsonShallow(value)) {
                    addTextArea(
                        name,
                        name,
                        Json.getWithNulls().toJsonPretty(value),
                        "small json"
                    );
                    continue;
                }

                String iType;
                if (name.contains("password") || (tags != null && CollectionUtil.contains(tags.value(), "password"))) {
                    iType = "password";
                    value = "";
                } else {
                    iType = null;
                }
                addInput(name, name, value == null ? null : value.toString(), iType);
            }
        }

        if (dto instanceof CommonUser && !(dto instanceof CommonUserPassword)) {
            addInput("password", "password", null, "password");
        }

        addSubmit(Locale.getString(VantarKey.ADMIN_DO));
        html.append("<input type='hidden' name='asjson' id='asjson'/>").append("</form>");
        setJs("/js/jquery.min.js");
        setJs("/js/vantar.js");
        return this;
    }

    public WebUi addDtoView(Dto dto) {
        for (Map.Entry<String, Object> item : dto.getPropertyValues().entrySet()) {
            String value;
            if (item.getValue() == null) {
                value = "NULL";
            } else if (item.getValue() instanceof DateTime) {
                value = lang != null && lang.equals("fa") ?
                    ((DateTime) item.getValue()).formatter().getDateTimePersianStyled() :
                    ((DateTime) item.getValue()).formatter().getDateTimeStyled();
            } else {
                value = ObjectUtil.toStringViewable(dto.getPropertyValue(item.getKey()));
            }

            if (value.toLowerCase().contains("jpg") || value.toLowerCase().contains("png")) {
                StringBuilder images = new StringBuilder();
                for (String v : StringUtil.splitTrim(value, '|')) {
                    if (StringUtil.isNotEmpty(v)) {
                        images
                            .append("<img src='").append(v).append("' alt='").append(v)
                            .append("' title='").append(v).append("'/> ");
                    }
                }
                value = images.toString();
            } else {
                value = escapeWithNtoBr(value);
            }

            addKeyValue(item.getKey(), value);
        }
        return this;
    }

    public WebUi addDtoListWithHeader(PageData data, DtoDictionary.Info info, String... fields) {
        Dto dto = info.getDtoInstance();
        String className = dto.getClass().getSimpleName();

        html.append("<div id='actions'>")
            .append(" <a href='").append(getCompleteLink("/admin/data/insert?dto=" + className))
            .append("'>").append(Locale.getString(VantarKey.ADMIN_NEW_RECORD)).append("</a>")
            .append(" | <a href='").append(getCompleteLink("/admin/data/import?dto=" + className))
            .append("'>").append(Locale.getString(VantarKey.ADMIN_IMPORT)).append("</a>")
            .append(" | <a href='").append(getCompleteLink("/admin/data/export?dto=" + className))
            .append("'>").append(Locale.getString(VantarKey.ADMIN_EXPORT)).append("</a>");

        if (data != null) {
            html.append(" | <a href='").append(getCompleteLink("/admin/data/update?dto=" + className))
                .append("'>").append(Locale.getString(VantarKey.ADMIN_BATCH_EDIT)).append("</a>")
                .append(" | <a href='").append(getCompleteLink("/admin/data/delete?dto=" + className))
                .append("'>").append(Locale.getString(VantarKey.ADMIN_BATCH_DELETE)).append("</a>")
                .append(" | <a href='").append(getCompleteLink("/admin/data/purge?dto=" + className))
                .append("'>").append(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL)).append("</a>");
        }
        html.append("</div>");

        html.append("<script>\n//<![CDATA[\n")
            .append("function toggleSw() {\n" +
                "var x = document.getElementById('container-search-options');\n" +
                "var y = document.getElementById('container-search-json');\n" +
                "if (x.style.display === \"none\") {\n" +
                "x.style.display = \"block\";\n" +
                "y.style.display = \"none\";\n" +
                "} else {\n" +
                "x.style.display = \"none\";\n" +
                "y.style.display = \"block\";\n" +
                "}\n" +
                "}\n")
            .append("\n//]]>\n</script>\n")
            .append("<div id='toggle' onclick='toggleSw()'>").append(Locale.getString(VantarKey.ADMIN_JSON_OPTION))
            .append("</div>")
            .append("<form id='dto-list-form' method='post'>");

        String jsonSearch = params.getString(VantarParam.JSON_SEARCH, "");

        html.append("<div id='container-search-json' style='display:")
            .append(jsonSearch.isEmpty() ? "none" : "block").append("'>");

        addTextArea("JSON", VantarParam.JSON_SEARCH, jsonSearch);
        html.append(" &nbsp;");
        addLinkNewPage(
            Locale.getString(VantarKey.ADMIN_HELP),
            "/admin/document/show?document=document--webservice--common--search.md"
        );
        addSubmit("&gt;&gt;");

        html.append("</div>");

        Params.QueryParams qp = new Params.QueryParams();
        // add all used params here
        qp.exclude = new String[] {
            VantarParam.PAGE,
            VantarParam.SORT_FIELD,
            VantarParam.SORT_POS,
            VantarParam.SEARCH_FIELD,
            VantarParam.SEARCH_VALUE,
            VantarParam.LOGICAL_DELETED,
        };
        Map<String, Object> q = params.queryParams(qp);
        q.forEach((key, value) ->
            html.append("<input type='hidden' name='").append(key).append("' value='").append(value).append("'/>"));

        // PAGE
        // SORT
        // PAGE
        // PAGE
        html.append("<table id='container-search-options' style='display:")
            .append(jsonSearch.isEmpty() ? "block" : "none").append("'>")

            // N PER PAGE
            .append("<tr>")
            .append("<td class='dto-list-form-title'>").append(Locale.getString(VantarKey.ADMIN_N_PER_PAGE))
            .append("</td>")
            .append("<td class='dto-list-form-option'><input id='dto-list-form-page' type='text' name='"
                + VantarParam.COUNT + "' value='")
            .append(params.getString(VantarParam.COUNT, Integer.toString(AdminData.N_PER_PAGE))).append("'/>")
            .append("</td>")
            .append("</tr>")

            // PAGE
            .append("<tr>")
            .append("<td class='dto-list-form-title'>").append(Locale.getString(VantarKey.ADMIN_PAGE)).append("</td>")
            .append("<td class='dto-list-form-option'><input id='dto-list-form-page' type='text' name='"
                + VantarParam.PAGE + "' value='")
            .append(params.getString(VantarParam.PAGE, "1")).append("'/>").append("<input id='total-pages' value='")
            .append(Locale.getString(VantarKey.ADMIN_FROM)).append(" ")
            .append(data == null ? "" : (long) Math.ceil((double) data.total / (double) data.length))
            .append("'/>")
            .append("</td>")
            .append("</tr>")

            // SORT
            .append("<tr>").append("<td class='dto-list-form-title'>")
            .append(Locale.getString(VantarKey.ADMIN_SORT)).append("</td>")
            .append("<td class='dto-list-form-option'>");

        String sortField = params.getString(VantarParam.SORT_FIELD, "id");
        String sortPos = params.getString(VantarParam.SORT_POS, "desc");


        html.append("<select name='" + VantarParam.SORT_FIELD + "'>");
        for (String name : dto.getProperties()) {
            html.append("<option ")
                .append(sortField.equals(name) ? "selected='selected' " : "")
                .append("value='").append(name).append("'>").append(name).append("</option>");
        }
        html
            .append("</select>")
            .append("<select name='" + VantarParam.SORT_POS + "'><option ")
            .append(sortPos.equals("asc") ? "selected='selected' " : "")
            .append("value='asc'>asc</option><option ")
            .append(sortPos.equals("desc") ? "selected='selected' " : "")
            .append("value='desc'>desc</option></select>")
            .append("</td></tr>");

        // search

        String searchCol = params.getString(VantarParam.SEARCH_FIELD);
        String searchValue = params.getString(VantarParam.SEARCH_VALUE);
        html.append("<tr>")
            .append("<td class='dto-list-form-title'>").append(Locale.getString(VantarKey.ADMIN_SEARCH)).append("</td>")
            .append("<td class='dto-list-form-option'>")
            .append("<select name='" + VantarParam.SEARCH_FIELD + "'><option value='all'>all</option>");
        for (String name : dto.getProperties()) {
            if (ClassUtil.isInstantiable(dto.getPropertyType(name), Dto.class)) {
                DtoDictionary.Info obj = DtoDictionary.get(name);
                if (obj == null) {
                    continue;
                }
                for (String nameInner : obj.getDtoInstance().getProperties()) {
                    nameInner = name + '.' + nameInner;
                    html.append("<option ")
                        .append(nameInner.equals(searchCol) ? "selected='selected' " : "")
                        .append("value='").append(nameInner).append("'>").append(nameInner).append("</option>");
                }
            } else {
                html.append("<option ")
                    .append(name.equals(searchCol) ? "selected='selected' " : "")
                    .append("value='").append(name).append("'>").append(name).append("</option>");
            }
        }
        html.append("</select>")
            .append("<input id='dto-list-form-search' type='text' name='" + VantarParam.SEARCH_VALUE + "' value='")
            .append(searchValue == null ? "" : searchValue).append("'/>")
            .append("</td>")
            .append("</tr>");

        boolean showDeleted = params.isChecked(VantarParam.LOGICAL_DELETED);
        if (dto.isDeleteLogicalEnabled()) {
            html.append("<tr><td></td><td><input value='1' type='checkbox' id='" + VantarParam.LOGICAL_DELETED + "' name='"
                + VantarParam.LOGICAL_DELETED + "' ").append(showDeleted ? "checked='checked'" : "").append("/> <label for='")
                .append(VantarParam.LOGICAL_DELETED).append("'>").append(Locale.getString(VantarKey.SHOW_DELETED))
                .append("</label></td></tr>");
        }

        // button
        html.append("<tr><td></td><td><button type='submit'>&gt;&gt;</button></td></tr>")
            .append("<tr><td></td><td>")
            .append("</td></tr></table></form>")
            .append("<form method='post' action='/admin/data/delete/many'>")
            .append("    <input type='hidden' name='" + VantarParam.AUTH_TOKEN + "' value='")
            .append(authToken).append("'/>\n");

        if (data == null) {
            addMessage(Locale.getString(VantarKey.NO_CONTENT));
        } else {
            addDtoList(data, true, fields);
        }

        html.append("<div id='delete-container'>")
            .append("<input name='" + VantarParam.DTO + "' value='").append(dto.getClass().getSimpleName())
            .append("' type='hidden'/>");

        if (dto.isDeleteLogicalEnabled()) {
            html.append("<div class='delete-type-container'>")
                .append("<input type='checkbox' name='do-logical-delete' id='do-logical-delete' checked='checked'/> ")
                .append("<label for='do-logical-delete'>").append(Locale.getString(VantarKey.LOGICAL_DELETED)).append("</label></div>");
        }

        html.append("<div> <input type='checkbox' name='confirm-delete'/> <label for='confirm-delete'>")
            .append(Locale.getString(VantarKey.ADMIN_CONFIRM)).append("</label></div>")
            .append("<div class='delete-button-container'><button type='submit'>")
            .append(Locale.getString(VantarKey.ADMIN_DELETE)).append("</button></div>")
            .append("</div>");

        setJs("/js/jquery.min.js");
        setJs("/js/vantar.js");

        return this;
    }

    public WebUi addDtoList(PageData data, boolean options, String... fields) {
        html.append("<div class='scroll'><table class='list'>");
        boolean isLogServiceUp = Services.isUp(ServiceUserActionLog.class);
        boolean isFirst = true;
        for (Dto dto : data.data) {
            if (isFirst) {
                html.append("<tr>");
                if (options) {
                    html.append("<th id='total' colspan='").append(isLogServiceUp ? 3 : 2).append("'>")
                        .append(data.total).append("</th>");
                }
                for (String name : fields) {
                    Class<?> type = dto.getPropertyType(name);
                    if ((!name.equals("name") && !name.equals("title")) &&
                        (CollectionUtil.isCollectionAndMap(type) || ClassUtil.isInstantiable(type, Dto.class))) {
                        continue;
                    }

                    Tags actions = dto.getField(name).getAnnotation(Tags.class);
                    if (actions != null
                        && (!CollectionUtil.contains(actions.value(), "list")
                        || CollectionUtil.contains(actions.value(), "none"))) {
                        continue;
                    }

                    html.append("<th>").append(name).append("</th>");
                }
                html.append("</tr>");
                isFirst = false;
            }

            html.append("<tr>");
            if (options) {
                String x = authToken == null ? "" : ("&" + VantarParam.AUTH_TOKEN + "=" + authToken);
                if (lang != null) {
                    x += "&" + VantarParam.LANG + "=" + lang;
                }

                String className = dto.getClass().getSimpleName();
                html
//                    .append("<td class='option'><div class='option'><a href='" + "/admin/data/delete?" + VantarParam.DTO + "=")
                    .append("<td class='option delete-option'><input class='delete-check' name='delete-check' value='")
                    .append(dto.getId()).append("' type='checkbox'/></td>")
                    .append("<td class='option'><div class='option'><a href='" + "/admin/data/update?" + VantarParam.DTO + "=")
                    .append(className).append("&id=").append(dto.getId()).append(x).append("'>")
                    .append(Locale.getString(VantarKey.ADMIN_EDIT)).append("</a></div></td>");


                if (isLogServiceUp) {
                    html.append("<td class='option'><div class='option'><a href='" + "/admin/data/log?" + VantarParam.DTO + "=")
                        .append(dto.getClass().getName()).append("&id=").append(dto.getId()).append(x).append("'>")
                        .append(Locale.getString(VantarKey.ADMIN_ACTION_LOG)).append("</a></div></td>");
                }
            }
            for (String name : fields) {
                Class<?> type = dto.getPropertyType(name);

                if ((!name.equals("name") && !name.equals("title")) &&
                    (CollectionUtil.isCollectionAndMap(type) || ClassUtil.isInstantiable(type, Dto.class))) {
                    continue;
                }

                Tags actions = dto.getField(name).getAnnotation(Tags.class);
                if (actions != null
                    && (!CollectionUtil.contains(actions.value(), "list") || CollectionUtil.contains(actions.value(), "none"))) {
                    continue;
                }

                Object realValue = dto.getPropertyValue(name);
                if (!(realValue instanceof Long) && !(realValue instanceof DateTime)) {
                    realValue = "";
                }

                Object value = ObjectUtil.toStringViewable(dto.getPropertyValue(name));
                if (value == null) {
                    value = "-";
                }

                html.append("<td><div onclick='cellClick(this)'>");
                if (realValue instanceof DateTime) {
                    html.append(((DateTime) realValue).formatter().getDateTimePersianStyled()).append("<br/>");
                }
                html.append(value).append("<input type='hidden' value='").append(realValue).append("'/>").append("</div></td>");
            }
            html.append("</tr>");
        }
        html.append("</table></div>");
        return this;
    }

    public WebUi addPagination(long total, int pageNumber, int nPerPage) {
        return addPagination(total, pageNumber, nPerPage, null);
    }

    public WebUi addPagination(long total, int pageNumber, int nPerPage, String newParams) {
        html.append("<div class='pagination'>").append("<select class='page-select' onchange='location=this.value;'>");

        for (long i = 1, l = (long) Math.ceil((double) total / (double) nPerPage); i <= l; ++i) {
            html.append("<option ").append(pageNumber == i ? "selected='selected' " : "").append("value='?");

            Map<String, Object> q = params.getAll();
            q.put(VantarParam.PAGE, Long.toString(i));
            if (authToken != null) {
                q.put(VantarParam.AUTH_TOKEN, authToken);
            }
            if (lang != null) {
                q.put(VantarParam.LANG, lang);
            }
            boolean first = true;
            for (Map.Entry<String, Object> entry : q.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue().toString();
                if (!first) {
                    html.append("&");
                }
                first = false;
                html.append(k).append("=").append(v);
            }

            if (StringUtil.isNotEmpty(newParams)) {
                html.append('&').append(newParams);
            }

            html.append("'>").append(i).append("</option>");
        }
        html.append("</select></div>");
        return this;
    }


    // > > > WRITE FINISH SLEEP

    public WebUi sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // pass
        }
        return this;
    }

    public WebUi write() {
        String content = written ?
            html.toString() :
            "<!DOCTYPE html>\n<head>\n    <meta charset='utf-8'/>\n    <title>"
                + Locale.getString(VantarKey.ADMIN_ADMIN_PANEL) + "</title>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"description\" content=\"Vantar admin dashboard\">\n" +
                "    <meta name=\"author\" content=\"Mehdi Torabi\">\n" +
                "    <link rel='stylesheet' type='text/css' href='/css/index.css'>\n</head>\n<body class="
                + direction + ">\n\n"
                + html.toString();

        written = true;
        Response.writeHtml(response, content);

        try {
            response.flushBuffer();
        } catch (IOException e) {
            // pass
        }

        html = new StringBuilder(10000);
        return this;
    }

    public String getHtml() {
        String content = html.toString();
        html = new StringBuilder(10000);
        return content;
    }

    public WebUi setHtml(String html) {
        this.html.append(html);
        return this;
    }

    public WebUi setJs(String path) {
        if (js == null) {
            js = new HashSet<>(10);
        }
        js.add(path);
        return this;
    }

    public void finish() {
        while (openTags.size() > 0) {
            html.append(openTags.pop());
        }
        if (js != null) {
            for (String j : js) {
                html.append("    <script src=\"").append(j).append("\"></script>\n");
            }
        }
        html.append("</body>\n</html>\n");
        write();
    }

    private String escape(String text) {
        return escape.encode(text);
    }

    private String escapeWithNtoBr(String text) {
        return StringUtil.replace(escape.encode(text), "\n", "<br/>");
    }
}