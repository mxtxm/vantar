package com.vantar.web;

import com.vantar.util.string.StringUtil;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * /p1/p2/p3/p4/p5
 *
 * methodP2p3...pn : camelCaseMethod
 * method(Params(request), response)
 */
public class RestfullToMethod extends RouteBase {

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");

        String path = request.getServletPath();
        String[] parts = StringUtil.split(path, '/');

        StringBuilder methodName = new StringBuilder(path.length())
            .append(request.getMethod().toLowerCase());

        for (int i = 2, l = parts.length; i < l; ++i) {
            String part = parts[i];
            methodName.append(Character.toUpperCase(part.charAt(0)));
            methodName.append(part.substring(1).toLowerCase());
        }

        callMethod(request, response, methodName, null);
    }
}