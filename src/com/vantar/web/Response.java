package com.vantar.web;

import com.vantar.common.VantarParam;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.json.Json;
import org.slf4j.*;
import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.*;


public class Response {

    private static final Logger log = LoggerFactory.getLogger(Response.class);

    public static final boolean DEFAULT_ERROR_FORMAT = true;
    public static final String DEFAULT_ALLOWED_ORIGIN = "*";
    public static String DEFAULT_ALLOWED_METHODS = "GET, POST, DELETE, PUT, HEAD, OPTIONS";
    public static final String[] DEFAULT_ALLOWED_HEADERS = new String[]
        {"*", "content-type", "x-lang", "x-auth-token", "authorization", "access-control-allow-origin"};

    private static String allowMethods = DEFAULT_ALLOWED_METHODS;
    private static boolean jsonError = DEFAULT_ERROR_FORMAT;
    private static String allowOrigin = DEFAULT_ALLOWED_ORIGIN;
    private static String[] allowHeaders = DEFAULT_ALLOWED_HEADERS;


    public static void setAllowOrigin(String allowOrigin) {
        Response.allowOrigin = allowOrigin;
    }

    public static void setAllowMethods(String allowMethods) {
        Response.allowMethods = allowMethods;
    }

    public static void setAllowHeaders(String... headers) {
        allowHeaders = headers;
    }

    public static void setJsonError() {
        jsonError = true;
    }

    public static void setTextError() {
        jsonError = false;
    }

    public static void redirect(HttpServletResponse response, String url) {
        try {
            response.sendRedirect(url);
        } catch (IOException e) {
            log.error("! redirect failed ({})", url, e);
        }
    }

    public static void headersOnly(HttpServletResponse response) {
        setOriginHeaders(response);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK); // 200
    }

    public static void download(HttpServletResponse response, String filepath, String filename) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/octet-stream");
        response.setHeader("Expires", "0");
        response.setHeader("Pragma", "no-Cache");
        response.setHeader("Cache-Control", "no-Cache");
        response.setHeader("Content-Description", "File Transfer");
        //todo escape: you surround the string with double-quotes, and escape any quotes or backslashes within by preceding them with a single backslash.
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("Content-Transfer-Encoding", "binary");
        setOriginHeaders(response);

        OutputStream out = null;
        FileInputStream in = null;
        try {
            out = response.getOutputStream();
            in = new FileInputStream(filepath);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0){
                out.write(buffer, 0, length);
            }
        } catch (IOException e) {
            log.error("! download failed ({}, {})", filepath, filename, e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.flush();
                }
            } catch (IOException e) {
                log.error("! close failed ({}, {})", filepath, filename, e);
            }
        }
    }

    public static void writeJsonString(HttpServletResponse response, String data) {
        setJsonHeaders(response);
        try {
            response.getWriter().append(data);
        } catch (IOException e) {
            log.error("! write json failed ({})", data, e);
        }
    }

    public static void writeJson(HttpServletResponse response, Object data) {
        setJsonHeaders(response);
        writeGson(response, data);
    }

    public static void writeGson(HttpServletResponse response, Object data) {
        try {
            response.getWriter().append(Json.toJson(data));
        } catch (IOException e) {
            log.error("! write json failed ({})", data, e);
        }
    }

    public static void writeSuccess(HttpServletResponse response) {
        writeString(response, VantarParam.SUCCESS);
    }

    public static void writeFail(HttpServletResponse response) {
        writeString(response, VantarParam.FAIL);
    }

    public static void writeString(HttpServletResponse response, long data) {
        writeString(response, Long.toString(data));
    }

    public static void writeString(HttpServletResponse response, String data) {
        setStringHeaders(response);
        try {
            response.getWriter().append(data);
        } catch (IOException e) {
            log.error("! write string failed ({})", data, e);
        }
    }

    public static void setError(HttpServletResponse response, int status, String msg) {
        if (msg == null) {
            msg = "";
        }
        response.reset();
        response.setCharacterEncoding("UTF-8");
        setOriginHeaders(response);
        if (jsonError) {
            response.setContentType("application/json");
            msg = Json.toJson(new ResponseMessage(msg, status));
        } else {
            response.setContentType("text/html;charset=UTF-8");
        }

        response.setStatus(status);
        try {
            PrintWriter writer = response.getWriter();
            writer.write(msg);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.error("! set error failed ({}, {})", status, msg, e);
        }
    }

    public static void serverError(HttpServletResponse response, String msg) {
        setError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg); // 500
    }

    public static void serviceUnavailable(HttpServletResponse response, String msg) {
        setError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, msg); // 503
    }

    public static void headerExpectationFailed(HttpServletResponse response, String msg) {
        setError(response, HttpServletResponse.SC_EXPECTATION_FAILED, msg); // 417 expectation in header
    }

    public static void methodNotAllowed(HttpServletResponse response, String msg) {
        setError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg); // 405
    }

    public static void notAcceptable(HttpServletResponse response, String msg) {
       setError(response, HttpServletResponse.SC_NOT_ACCEPTABLE, msg); // 406
    }

    public static void unsupportedMediaType(HttpServletResponse response, String msg) {
        setError(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, msg); // 415
    }

    public static void clientError(HttpServletResponse response, String msg) {
        setError(response, HttpServletResponse.SC_BAD_REQUEST, msg); // 400
    }

    public static void unauthorized(HttpServletResponse response, String msg) {
        setError(response, HttpServletResponse.SC_UNAUTHORIZED, msg); // 401
    }

    public static void forbidden(HttpServletResponse response, String msg) {
        setError(response, HttpServletResponse.SC_FORBIDDEN, msg); // 403
    }

    public static void notFound(HttpServletResponse response, String msg) {
        setError(response, HttpServletResponse.SC_NOT_FOUND, msg); // 404
    }

    public static void noContent(HttpServletResponse response) {
        response.reset();
        setOriginHeaders(response);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
    }

    private static void setOriginHeaders(HttpServletResponse response) {
        if (allowOrigin != null) {
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Origin", allowOrigin);
            response.setHeader("Access-Control-Allow-Methods", allowMethods);
            response.setHeader("Access-Control-Allow-Headers", CollectionUtil.join(allowHeaders, VantarParam.SEPARATOR_KEY_PHRASE));
        }
    }

    private static void setJsonHeaders(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        setOriginHeaders(response);
    }

    private static void setStringHeaders(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        setOriginHeaders(response);
    }

    public static void showTemplate(Params params, HttpServletResponse response, String template) {
        RequestDispatcher rd = params.request.getRequestDispatcher(template);
        try {
            rd.forward(params.request, response);
        } catch (ServletException | IOException e) {
            log.error("! show template failed ({})", template, e);
        }
    }
}