package com.vantar.web;

import com.vantar.common.VantarParam;
import com.vantar.database.dto.DtoDictionary;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


abstract class WebUiBasics <T extends WebUiBasics<T>> {

    protected abstract T getThis();

    public static final String PARAM_CONFIRM = "confirm";

    public Params params;
    protected HttpServletResponse response;
    protected boolean written;

    protected StringBuilder html = new StringBuilder(100000);
    protected final Stack<String> openTags = new Stack<>();
    protected List<String> js;

    protected String authToken;
    protected String lang;
    public String direction;
    public String boxFloat;
    public String align;
    public String alignKey;
    public String alignValue;
    protected StringBuilder widgetComment;
    protected final HtmlEscape escape = new HtmlEscape();


    public T setAuthToken(String token) {
        authToken = token;
        return getThis();
    }

    public T reverseDirection() {
        direction = "ltr".equalsIgnoreCase(direction) ? "rtl" : "ltr";
        boxFloat = "float-right".equalsIgnoreCase(boxFloat) ? "float-left" : "float-right";
        align = "right".equalsIgnoreCase(align) ? "left" : "right";
        alignKey = "left".equalsIgnoreCase(alignKey) ? "right" : "left";
        alignValue = "right".equalsIgnoreCase(alignValue) ? "left" : "right";
        return getThis();
    }

    // > > > MENU

    public T addMenu(Map<String, String> menu, String... text) {
        html.append("<ul id='menu' class='").append(direction).append("'>\n");

        boolean isFirst = true;
        for (Map.Entry<String, String> entry : menu.entrySet()) {
            html.append("<li");
            if (isFirst && direction.equals("ltr")) {
                html.append(" style='border: none!important'");
            }
            html.append("><a href='").append(getLink(entry.getValue())).append("'>").append(entry.getKey())
                .append("</a></li>\n");
            isFirst = false;
        }

        if (text.length > 0) {
            html.append("<li class='signout'><a href='").append("/admin/signout").append("'>")
                .append(Locale.getString(VantarKey.SIGN_OUT)).append("</a></li>\n");
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
                html.append("<li class='menu-lang'><a href='").append(getLink(path)).append("'>").append(lang)
                    .append("</a></li>\n");
            }
        }
        lang = l;

        for (String t: text) {
            html.append("<li class='menu-text'>").append(t).append("</li>\n");
        }
        html.append("</ul>\n");
        return getThis();
    }

    public T setBreadcrumb(String title, DtoDictionary.Info dtoInfo) {
        html.append("<h1>")
            .append(dtoInfo.group)
            .append(" &gt; ")
            .append("<a href='").append(getLink("/admin/data/list?dto=" + dtoInfo.getDtoClassName()))
            .append("'>").append(dtoInfo.title).append("</a>")
            .append(" &gt; ")
            .append(title)
            .append("</h1>\n");
        return getThis();
    }

    // MENU < < <

    // > > > MESSAGE

    public T addMessage(Object msg) {
        html.append("<p class=\"msg\">")
            .append(escapeWithNtoBr(msg instanceof LangKey ? Locale.getString((LangKey) msg) : msg.toString()))
            .append("</p>\n");
        return getThis();
    }

    public T addErrorMessage(Object msg) {
        return addErrorMessage(
            msg instanceof Throwable ? ((Throwable) msg).getMessage() : msg,
            msg instanceof Throwable ? ObjectUtil.toString(msg) : null
        );
    }

    public T addErrorMessage(Object msg, String comment) {
        String id = "i" + StringUtil.getRandomString(10);
        html.append("<p class=\"error\" onclick='$(\"#").append(id).append("\").toggle()'>\n")
            .append(escapeWithNtoBr(msg instanceof LangKey ? Locale.getString((LangKey) msg) : ObjectUtil.toString(msg)))
            .append("</p>\n<pre id=\"").append(id).append("\" class=\"error\" style=\"display:none\">\n")
            .append(escapeWithNtoBr(comment)).append("</pre>\n");
        return getThis();
    }

    // MESSAGE < < <

    // > > > TEXT AND SPACE AND LINE

    public T addText(Object text) {
        html.append(escapeWithNtoBr(text instanceof LangKey ? Locale.getString((LangKey) text) : text.toString()))
            .append('\n');
        return getThis();
    }

    public T addTextLine(Object text) {
        html.append(escapeWithNtoBr(text instanceof LangKey ? Locale.getString((LangKey) text) : text.toString()))
            .append("<br/>\n");
        return getThis();
    }

    public T addEmptyLine() {
        html.append("<br/>");
        return getThis();
    }

    public T addEmptyLine(int count) {
        for (int i = 0; i < count; ++i) {
            html.append("<br/>");
        }
        return getThis();
    }

    public String getLines(Object... values) {
        StringBuilder sb = new StringBuilder(500);
        for (int i = 0, l = values.length; i < l; ++i) {
            Object v = values[i];
            sb.append("<p class=\"b-line b-line").append(i).append("\">").append(v).append("</p>");
        }
        return sb.toString();
    }

    // TEXT AND SPACE AND LINE < < <

    // > > > HEAD title, class

    public T addHeading(int h, Object... options) {
        html.append("<h").append(h);
        if (options.length > 1 && options[1] != null) {
            html.append(" class=\"").append(options[1]).append("\"");
        }
        html.append(">")
            .append(escapeWithNtoBr(options[0] instanceof LangKey ? Locale.getString((LangKey) options[0]) : options[0].toString()))
            .append("</h").append(h).append(">\n");
        return getThis();
    }

    // HEAD < < <

    // > > > LINKS AND URLS

    public T addHref(Object text, String url) {
        return addHref(text, url, false, false, "");
    }

    public T addHref(Object text, String url, boolean newWindow, boolean block, String className) {
        html.append(getHref(text, url, newWindow, block, className));
        return getThis();
    }

    public T addHrefNewPage(Object text, String url) {
        return addHref(text, url, true, false, "");
    }

    public T addHrefBlock(Object text, String url) {
        return addHref(text, url, false, true, "");
    }

    public T addHrefBlockNewPage(Object text, String url) {
        return addHref(text, url, true, true, "");
    }

    public T addHrefs(Object... values) {
        html.append("<div class='actions'>\n");
        for (int i = 0, l = values.length; i < l; ++i) {
            if (i > 0 && i < l-1) {
                html.append(" | ");
            }
            addHref(values[i], values[++i].toString(), false, false, "");
        }
        html.append("</div>\n");
        return getThis();
    }

    public String getHref(Object text, String url, boolean newWindow, boolean block, String className) {
        StringBuilder s = new StringBuilder(200);
        if (block) {
            s.append("<p class='link'>");
        }
        s.append("<a");
        if (className != null) {
            s.append(" class='").append(className).append("'");
        }
        if (newWindow) {
            s.append(" target='_blank'");
        }
        s.append(" href='").append(getLink(url)).append("'>")
            .append(escapeWithNtoBr(text instanceof LangKey ? Locale.getString((LangKey) text) : text.toString()))
            .append("</a>");
        if (block) {
            s.append("</p>");
        }
        return s.toString();
    }

    public String getLink(String url) {
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

    public void redirect(String url) {
        Response.redirect(response, getLink(url));
    }

    // LINKS AND URLS < < <

    // > > > BLOCK

    /**
     *  options : tag, class/id ---> if begins with "id-"
     */
    public T beginBlock(String... options) {
        String tag = options.length > 0 && options[0] != null ? options[0] : "div";
        openTags.push("</" + tag + ">\n");
        html.append("<").append(tag);
        if (options.length > 1 && options[1] != null ) {
            html.append(" ").append(options[1].startsWith("id-") ? "id" : "class").append("=\"").append(options[1]).append("\"");
        }
        html.append(">\n");
        return getThis();
    }

    /**
     *  options : tag, text --> no escape if starts with "~~~", class/id ---> if begins with "id-"
     */
    public T addBlockNoEscape(String... options) {
        if (options.length > 1 && options[1] != null ) {
            options[1] = "~~~" + options[1];
        }
        return addBlock(options);
    }
    public T addBlock(String... options) {
        String tag = options.length > 0 ? options[0] : "div";
        html.append("<").append(tag);
        if (options.length > 2) {
            html.append(" ").append(options[2].startsWith("id-") ? "id" : "class").append("=\"").append(options[2]).append("\"");
        }
        html.append(">\n");
        if (options.length > 1) {
            if (!"pre".equals(tag) && !options[1].startsWith("~~~")) {
                options[1] = escapeWithNtoBr(StringUtil.remove(options[1], "~~~"));
            }
            html.append(StringUtil.remove(options[1], "~~~"));
        }
        html.append("</").append(tag).append(">\n");
        return getThis();
    }

    /**
     *  end block/box last blocks will be ended automatically
     */
    public T blockEnd() {
        if (openTags.size() > 0) {
            html.append(openTags.pop());
        }
        return getThis();
    }

    public String getBlock(String type, String value, boolean escape) {
        return "<" + type + ">" + (escape ? escapeWithNtoBr(value) : value) + "</" + type + ">";
    }

    // BLOCK < < <

    // > > > FORM

    public T beginFormGet() {
        return beginForm("get", null, false);
    }

    public T beginFormGet(String action) {
        return beginForm("get", action, false);
    }

    public T beginFormPost() {
        return beginForm("post", null, false);
    }

    public T beginFormPost(String action) {
        return beginForm("post", action, false);
    }

    public T beginUploadForm() {
        return beginForm("post", null, true);
    }

    public T beginUploadForm(String action) {
        return beginForm("post", action, true);
    }

    private T beginForm(String method, String action, boolean multiPart) {
        openTags.push("</form>\n");
        html.append("<form ");
        if (multiPart) {
            html.append("enctype=\"multipart/form-data\" ");
        }
        if (action != null) {
            html.append("action='").append(action).append("' ");
        }
        html.append("method=\"").append(method).append("\">\n<input type=\"hidden\" name=\"f\" value=\"1\"/>\n");
        String q = params.request.getQueryString();
        if (StringUtil.isNotEmpty(q)) {
            if (StringUtil.contains(q, VantarParam.AUTH_TOKEN + "=") && authToken != null) {
                html.append("<input type=\"hidden\" name=\"" + VantarParam.AUTH_TOKEN + "\" value=\"")
                    .append(authToken).append("\"/>\n");
            }
            if (StringUtil.contains(q, VantarParam.LANG + "=") && lang != null){
                html.append("<input type=\"hidden\" name=\"" + VantarParam.LANG + "\" value=\"")
                    .append(lang).append("\"/>\n");
            }
        }
        return getThis();
    }

    // FORMS < < <

    // > > > INPUTS

    public T addHidden(String name, Object value) {
        html.append("<input type=\"hidden\" name=\"").append(name).append("\" value=\"").append(value).append("\"/>");
        return getThis();
    }

    public T addInput(Object label, String id, Object... options) {
        return addWidgetRow(label, id, getInput("text", id, options));
    }

    public T addInputSelectable(Object label, String id, Collection<Object> selectable, Object... options) {
        return addWidgetRow(label, id, getInputSelectable("text", id, selectable, options));
    }

    public T addPassword(Object label, String id, Object... options) {
        return addWidgetRow(label, id, getInput("password", id, options));
    }

    public T addFile(Object label, String id, Object... options) {
        return addWidgetRow(label, id, getInput("file", id, options));
    }

    /**
     * options --> value, class, direction
     */
    public String getInput(String type, String id, Object... options) {
        StringBuilder sb = new StringBuilder(250);
        sb  .append("<input type=\"").append(type).append("\" id=\"").append(id).append("\" name=\"").append(id).append("\"")
            .append(" autocomplete=\"off\"")
            .append(" style=\"direction:").append(options.length > 2 ? options[2] : alignValue).append("\"");
        if (options.length > 0 && options[0] != null) {
            sb.append(" value=\"").append(escape(options[0].toString())).append("\"");
        }
        if (options.length > 1 && options[1] != null) {
            sb.append(" class=\"").append(options[1]).append("\"");
        }
        sb.append("/>\n");
        return sb.toString();
    }

    /**
     * options --> value, class, direction
     */
    public String getInputSelectable(String type, String id, Collection<Object> selectable, Object... options) {
        return
            getInput(type, id, options)
            + getSelect(
                id + "-select",
                selectable,
                false,
                null,
                "input-selectable",
                "$('#" + id + "').val($('#" + id + "').val() + ',' + $(this).val())"
            );
    }

    public T addSubmit() {
        return addSubmit(VantarKey.ADMIN_SUBMIT);
    }

    /**
     * options --> class, id
     */
    public T addSubmit(Object label, String... options) {
        return addWidgetRow("", "", getSubmit(label, options));
    }

    public String getSubmit(Object label, String... options) {
        String value = label instanceof LangKey ? Locale.getString((LangKey) label) : label.toString();
        String id = options.length > 1 && options[1] != null ? options[1] : null;
        return  "<button type=\"submit\" value=\"" + value + "\""
            + (id != null ? " name=\"" + id + "\" id=\"" + id + "\"" : "")
            + (options.length > 0 && options[0] != null ? " class=\"" + options[0] + "\"" : "")
            + " style=\"direction:" + alignValue + "\">" + value
            + "</button>\n";
    }

    public T addButton(Object label, String id, String... options) {
        return addWidgetRow(
            "",
            "",
            "<button" + (options.length > 0 && options[0] != null ? " class=\"" + options[0] + "\"" : "")
                + " style=\"direction:" + alignValue + "\" class= type=\"button\" id=\"" + id + "\">"
                + (label instanceof LangKey ? Locale.getString((LangKey) label) : label)
                + "</button>\n"
        );
    }

    public T addTextArea(Object label, String id, Object... options) {
        return addWidgetRow(label, id, getTextArea(id, options));
    }

    /**
     * options --> value, class, direction
     */
    public String getTextArea(String id, Object... options) {
        StringBuilder sb = new StringBuilder(250);
        sb  .append("<textarea ").append(" id=\"").append(id).append("\" name=\"").append(id).append("\"")
            .append(" autocomplete=\"off\"")
            .append(" style=\"direction:").append(options.length > 2 ? options[2] : alignValue).append("\"")
            .append(" class=\"vtx vtx-").append(id);
        if (options.length > 1 && options[1] != null) {
            sb.append(" ").append(options[1]);
        }
        sb.append("\">");
        if (options.length > 0 && options[0] != null) {
            sb.append(escape(options[0].toString()));
        }
        sb.append("</textarea>\n");
        return sb.toString();
    }

    public T addCheckbox(Object label, String id, Object... options) {
        return addWidgetRow(label, id, getCheckbox(id, options));
    }

    /**
     * options --> checked, value, class, adjust
     */
    public String getCheckbox(String id, Object... options) {
        StringBuilder sb = new StringBuilder(250);
        sb  .append("<input type=\"checkbox\" id=\"").append(id).append("\" name=\"").append(id).append("\"")
            .append(" value=\"").append(options.length > 1 && options[1] != null ? options[1] : "1").append("\"");
        if (options.length <= 3 || options[3] == null || (boolean) options[3]) {
            sb.append(" style=\"margin-top:5px;direction:").append(alignValue).append("\"");
        }
        if (options.length > 0 && options[0] != null && (boolean) options[0]) {
            sb.append(" checked=\"checked\"");
        }
        if (options.length > 2 && options[2] != null) {
            sb.append(" class=\"").append(options[2]).append("\"");
        }
        sb.append("/>\n");
        return sb.toString();
    }

    public T addSelect(Object label, String id, Object items, Object... options) {
        return addWidgetRow(label, id, getSelect(id, items, options));
    }

    /**
     * options --> isMultiSelect, value, class, direction, onchange
     */
    public String getSelect(String id, Object items, Object... options) {
        boolean isMulti = (options.length > 1) && (boolean) options[0];
        StringBuilder sb = new StringBuilder(500);
        sb  .append("<select id=\"").append(id).append("\" name=\"").append(id).append("\" autocomplete=\"off\"")
            .append(" style=\"direction:").append(options.length > 3 ? options[3] : alignValue).append("\"");
        if (isMulti) {
            sb.append(" multiple=\"multiple\"");
        }
        if (isMulti || options.length > 2) {
            sb.append(" class=\"");
            if (isMulti) {
                sb.append("multi-select");
            }
            if (options.length > 2 && options[2] != null) {
                sb.append(" ").append(options[2]);
            }
            sb.append("\"");
        }
        if (options.length > 3 && options[3] != null) {
            sb.append(" onchange=\"").append(options[3]).append("\"");
        }
        sb.append(">\n");
        if (!isMulti) {
            sb.append("<option></option>\n");
        }
        Object value = options.length > 1 ? options[1] : null;

        if (items instanceof Map) {
            for (Map.Entry<?, ?> item : ((Map<?, ?>) items).entrySet()) {
                sb.append("<option value=\"").append(item.getKey()).append("\"");
                if (value != null) {
                    if (isMulti) {
                        for (Object v : (Collection<?>) value) {
                            if (item.getKey().toString().equals(v.toString())) {
                                sb.append(" selected=\"selected\"");
                            }
                        }
                    } else if (item.getKey().toString().equals(value.toString())) {
                        sb.append(" selected=\"selected\"");
                    }
                }
                sb.append(" title=\"").append(item.getKey()).append("\">").append(item.getValue()).append("</option>\n");
            }
        } else if (items instanceof Collection<?>) {
            for (Object item : (Collection<?>) items) {
                sb.append("<option value=\"").append(item).append("\"");
                if (value != null) {
                    if (isMulti) {
                        for (Object v : (Collection<?>) value) {
                            if (item.toString().equals(v.toString())) {
                                sb.append(" selected=\"selected\"");
                            }
                        }
                    } else if (item.toString().equals(value.toString())) {
                        sb.append(" selected=\"selected\"");
                    }
                }
                sb.append(">").append(item).append("</option>\n");
            }
        } else {
            for (String item : (String[]) items) {
                sb.append("<option value=\"").append(item).append("\"");
                if (value != null) {
                    if (isMulti) {
                        for (Object v : (Collection<?>) value) {
                            if (item.equals(v.toString())) {
                                sb.append(" selected=\"selected\"");
                            }
                        }
                    } else if (item.equals(value.toString())) {
                        sb.append(" selected=\"selected\"");
                    }
                }
                sb.append(">").append(item).append("</option>\n");
            }
        }
        sb.append("</select>\n");
        return sb.toString();
    }

    public T addWidgetRow(Object label, String name, String widget) {
        html.append("<div class=\"flex-container\" style=\"direction:").append(direction)
            .append("; justify-content:").append(align).append("; align-items:").append(align).append("\">\n")
            .append("<label style=\"text-align:").append(alignKey).append("\" for=\"").append(name).append("\">\n")
            .append(label instanceof LangKey ? Locale.getString((LangKey) label) : label).append("</label>\n")
            .append(widget);
        if (widgetComment != null) {
            html.append("<button type=\"button\" class=\"data-hint-b\" onclick=\"dataHintB(this)\">...</button>")
                .append("<span class=\"field-hint\">").append(widgetComment).append("</span>");
            widgetComment = null;
        }
        html.append("</div>\n");
        return getThis();
    }

    // INPUTS < < <

    // > > > BOX

    /**
     * options ---> title, container-class, title-class, box-class
     */
    public T beginBox(Object... options) {
        String title = options.length > 0 ?
            (options[0] instanceof LangKey ? Locale.getString((LangKey) options[0]) : options[0].toString()) : null;
        String containerClass = options.length > 1 && options[1] != null ? options[1].toString() : "solid-box-empty";
        String titleClass = options.length > 2 && options[2] != null ? options[2].toString() : "box-title";
        openTags.push("</div>\n</div>\n");
        html.append("<div class=\"").append(containerClass).append(" clearfix\">\n");
        if (title != null) {
            html.append("<h2 class=\"").append(titleClass).append("\">\n").append(title).append("</h2>");
        }
        html.append("<div class=\"solid-box-clear ");
        if (options.length > 3) {
            html.append(options[3]).append(" ");
        }
        html.append("clearfix\">\n");
        return getThis();
    }

    public T beginTree(Object text) {
        openTags.push("</div>\n");
        html.append("<div class=\"tree\">\n<h2 class=\"tree-title\">")
            .append(escapeWithNtoBr(text instanceof LangKey ? Locale.getString((LangKey) text) : text.toString()))
            .append("</h2>\n");
        return getThis();
    }

    /**
     * TITLE0
     * title1
     * title2
     * ------
     * data
     */
    public T beginFloatBox(String clazz, Object... titles) {
        openTags.push("</div>\n</div>\n");
        html.append("<div class=\"float-box-empty clearfix ").append(boxFloat).append(" ").append(clazz).append("\">\n")
            .append("<h2 class='box-title'>");
        // title
        for (int i = 0, l = titles.length; i < l; ++i) {
            Object t = titles[i];
            if (i > 0) {
                html.append("<p>");
            }
            html.append(escapeWithNtoBr(t instanceof LangKey ? Locale.getString((LangKey) t) : t.toString()));
            if (i > 0) {
                html.append("</p>\n");
            }
        }
        html.append("</h2>\n<div class=\"solid-box-clear clearfix\">\n");
        return getThis();
    }

    /**
     * TITLE0(link)     tag
     * title1
     * title2
     * --------------------
     * data
     */
    public T beginFloatBoxLink(String clazz, String tag, String link, Object... titles) {
        openTags.push("</div>\n</div>\n");
        html.append("<a href=\"").append(getLink(link)).append("\" class=\"title-link\" target=\"_blank\">")
            .append("<div class=\"link-box float-box-empty clearfix ").append(boxFloat).append(" ").append(clazz).append("\">\n")
            .append("<h2 class=\"box-title\">");
        if (tag != null) {
            html.append("<span class=\"tag\">").append(tag).append("</span>");
        }
        // title
        for (int i = 0, l = titles.length; i < l; ++i) {
            Object t = titles[i];
            String[] txt = StringUtil.split(
                escapeWithNtoBr(t instanceof LangKey ? Locale.getString((LangKey) t) : t.toString()),
                "|"
            );
            html.append(i == 0 ? "<p class=\"title\">" : "<p class=\"comment\">");
            html.append(txt[0]);
            if (txt.length > 1) {
                html.append("<label class=\"title-tag\">").append(txt[1]).append("</label>");
            }
            html.append("</p>\n");
        }
        html.append("</h2></a>\n<div class=\"solid-box-clear clearfix\">\n");
       return getThis();
    }

    // BOX < < <

    // > > > KEY VALUE

    /**
     * options ---> class, escape, valuePre
     */
    public T addKeyValue(Object key, Object value, Object... options) {
        boolean escape = options.length <= 1 || options[1] == null || (boolean) options[1];
        key = StringUtil.replace(
            key == null ? "" :
                (key instanceof LangKey ? Locale.getString((LangKey) key) :
                    (escape ? escape(key.toString()) : key.toString())),
            "\n",
            "<br/>"
        );
        value = StringUtil.replace(
            value == null ? "NULL" :
                (value instanceof LangKey ? Locale.getString((LangKey) value) :
                    (escape ? escape(value.toString()) : value.toString())),
            "\n",
            "<br/><br/>"
        );

        String valuePre = options.length > 2 ? (String) options[2] : null;

        String clazz = options.length > 0 ? (String) options[0] : null;
        String classK = "kv-key" + (clazz == null ? "" : " key-" + clazz);
        String classV = "kv-value" + (clazz == null ? "" : " value-" + clazz);
            // container >
        html.append("<div class=\"kv-flex-container\" style=\"direction:").append(direction).append(";justify-content:")
            .append(alignValue).append(";align-items:").append(alignValue).append("\">\n")
            // k
            .append("<label class=\"").append(classK).append("\" style=\"overflow-wrap:break-word;text-align:")
            .append(alignKey).append("\">").append(key).append("</label>")
            // v
            .append("<label class=\"").append(classV).append("\" style=\"text-align:").append(alignValue).append("\"");
            if (valuePre == null) {
                if (options.length > 3) {
                    html.append(" onclick=\"").append(options[3]).append("\"");
                }
                html.append(">").append(value).append("</label>");
            } else  {
                html.append(" onclick='$(this).find(\"pre\").toggle()'>").append(value)
                    .append("<pre style='display:none'>").append(valuePre).append("</pre></label>");
            }
            // container >
            html.append("</div>");
        return getThis();
    }

    // KEY VALUE < < <

    // > > > HTML

    public T setJs(String path) {
        if (js == null) {
            js = new ArrayList<>(7);
        }
        js.add(path);
        return getThis();
    }

    public T sleepMs(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignore) {

        }
        return getThis();
    }

    public T write() {
        String content = written ?
            html.toString() :
            "<!DOCTYPE html>\n<head>\n    <meta charset='utf-8'/>\n    <title>"
                + Locale.getString(VantarKey.ADMIN_ADMIN_PANEL) + "</title>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"description\" content=\"Vantar admin dashboard\">\n" +
                "    <meta name=\"author\" content=\"Mehdi Torabi\">\n" +
                "    <link rel='stylesheet' type='text/css' href='/css/index.css?v=" +
                VantarParam.VERSION + "'>\n</head>\n<body class="
                + direction + ">\n\n"
                + html.toString();

        written = true;
        Response.writeHtml(response, content);

        try {
            response.flushBuffer();
        } catch (IOException ignore) {
        }

        html = new StringBuilder(100000);
        return getThis();
    }

    public void finish() {
        while (openTags.size() > 0) {
            html.append(openTags.pop());
        }
        setJs("/js/jquery.min.js");
        setJs("/js/vantar.js");
        setJs("/js/webservice.js");
        if (js != null) {
            for (String j : js.stream().distinct().collect(Collectors.toList())) {
                html.append("<script src=\"").append(j).append("?v=" + VantarParam.VERSION + "\"></script>\n");
            }
        }
        html.append("</body>\n</html>");
        write();
    }

    protected String escape(String text) {
        return escape.encode(text);
    }

    protected String escapeWithNtoBr(String text) {
        return StringUtil.replace(escape.encode(text), "\n", "<br/>");
    }

    public String getString() {
        return html.toString();
    }

    // HTML < < <
}