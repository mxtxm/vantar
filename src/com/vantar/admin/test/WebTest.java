package com.vantar.admin.test;

import com.vantar.common.*;
import com.vantar.util.object.ClassUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class WebTest {

    public static void show(Params params, HttpServletResponse response) {
        String username = Settings.tune.getProperty("test.user");
        String password = Settings.tune.getProperty("test.password");
        String[] urls = StringUtil.splitTrim(Settings.tune.getProperty("test.base.urls"), ';');

        String x = params.getString("x");

        StringBuilder html = new StringBuilder(5000);
        html.append("<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset='utf-8'/>\n" +
            "    <meta name=\"description\" content=\"Vantar system administration: WEB/RESTAPI TEST\">\n" +
            "    <meta name=\"author\" content=\"Mehdi Torabi\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>WEB/RESTAPI TEST</title>\n" +
            "    <link rel=\"stylesheet\" type=\"text/css\" href=\"/css/webtest.css?v=" + VantarParam.VERSION + "\"/>\n" +
            "</head>\n"
        );
        html.append(
            "<body class=\"clearfix\">\n" +
            "    <section id=\"left-pane\">\n" +
            "        <p><input id=\"title\" placeholder=\"title\" value=\"Test case X\"/></p>\n" +
            "        <p>\n" +
            "            <select id=\"base-url\">\n"
        );

        for (String url : urls) {
            html.append("<option value=\"").append(url).append("\">").append(url).append("</option>");
        }

        html.append(
            "            </select>\n" +
            "        </p>\n" +
            "        <p id=\"auth-form\">\n" +
            "<input id=\"username\" placeholder=\"username\" value=\"" + username + "\"/>" +
            "<input id=\"password\" placeholder=\"password\" value=\"" + password + "\"/>" +
            "<button type=\"button\" id=\"get-token\">signin</button>\n" +
            "        </p>\n" +
            "\n" +
            "        <p id=\"auth-container\">\n" +
            "<input id=\"auth\" placeholder=\"auth-token\" value=\"" + x + "\"/>" +
            "<input id=\"lang\" placeholder=\"lang\" value=\"en\"/>" +
            "        </p>\n" +
            "        <p id=\"pick-webservice-container\">\n" +
            "            <select id=\"pick-webservice\">\n"
        );

        List<Class<?>> classList = ClassUtil.getClasses(Settings.config.getProperty("package.web"), WebServlet.class);
        classList.sort(Comparator.comparing(Class::getSimpleName));
        for (Class<?> cls : classList) {
            html.append("    <optgroup label=\"").append(cls.getSimpleName()).append("\">");
            List<String> paths = Arrays.asList(cls.getAnnotation(WebServlet.class).value());
            paths.sort(String::compareTo);
            for (String path : paths) {
                html.append("<option value=\"").append(path).append("\">").append(path).append("</option>");
            }
            html.append("    </optgroup>");
        }

        html.append("</select></p>");
        html.append("<p id=\"web-service-path-container\"><input id=\"web-service-path\" placeholder=\"web-service path\"/></p>");

        html.append(
            "        <p>\n" +
            "            <select id=\"method\">\n" +
            "                <option value=\"POST\" selected>POST</option>\n" +
            "                <option value=\"POST JSON\">POST JSON</option>\n" +
            "                <option value=\"GET\">GET</option>\n" +
            "                <option value=\"POST UPLOAD FILE\">POST UPLOAD FILE</option>\n" +
            "                <option value=\"DOWNLOAD (GET DIRECT)\">DOWNLOAD (GET DIRECT)</option>\n" +
            "                <option value=\"PUT\">PUT</option>\n" +
            "                <option value=\"DELETE\">DELETE</option>\n" +
            "                <option value=\"PATCH\">PATCH</option>\n" +
            "                <option value=\"HEAD\">HEAD</option>\n" +
            "                <option value=\"CONNECT\">CONNECT</option>\n" +
            "                <option value=\"TRACE\">TRACE</option>\n" +
            "                <option value=\"OPTIONS\">OPTIONS</option>\n" +
            "            </select>" +
            "<button type=\"button\" id=\"add-pre-test\">pre</button>" +
            "<button type=\"button\" id=\"exec\">RUN</button>" +
            "        </p>\n" +

            "        <div id=\"file-container\">\n" +
            "            <div>" +
            "                <input id=\"file-key\" placeholder=\"FILE KEY\"/>" +
            "                <button type=\"button\" id=\"file-add-row\">add</button>\n" +
            "            </div>" +
            "            <div id=\"files\"></div>\n" +
            "        </div>\n" +

            "        <div id=\"added-tests\"></div>\n" +
            "        <p><textarea id=\"data\" placeholder=\"JSON\"></textarea></p>\n" +
            "        <p id=\"doc-link\"></p>\n" +
            "    </section>\n" +
            "    <section id=\"right-pane\"></section>\n" +
            "</body>\n"
        );

        html.append(
            "<script src=\"/js/jquery.min.js\"></script>\n" +
            "<script src=\"/js/beautify-json.js\"></script>\n" +
            "<script src=\"/js/select2.min.js\"></script>" +
            "<script src=\"/js/webservice.js?v=" + VantarParam.VERSION + "\"></script>\n" +
            "<script src=\"/js/webtest.js?v=" + VantarParam.VERSION + "\"></script>\n"
        );
        html.append("</html>");

        Response.writeString(response, html.toString());
    }
}
