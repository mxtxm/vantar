package com.vantar.web;

import com.vantar.util.string.StringUtil;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Enumeration;


/**
 * /p1/p2/p3/p4/p5
 *
 * p2p3...pn : camelCaseMethod
 * method(Params(request), response)
 */
public class RouteToMethod extends RouteBase {

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");

        if (request.getMethod().equalsIgnoreCase("OPTIONS") || request.getMethod().equalsIgnoreCase("HEAD")) {
            Response.headersOnly(response);
        } else {
            String path = request.getServletPath();
            String[] parts = StringUtil.split(path, '/');
            StringBuilder methodName = new StringBuilder(path.length()).append(parts.length < 3 ? "index" : parts[2]);

            for (int i = 3, l = parts.length; i < l; ++i) {
                String part = parts[i];
                methodName.append(Character.toUpperCase(part.charAt(0)));
                methodName.append(part.substring(1).toLowerCase());
            }

            callMethod(request, response, methodName, null);
        }
    }
}