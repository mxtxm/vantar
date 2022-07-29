package com.vantar.web;

import com.vantar.common.Settings;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.ServiceAuth;
import com.vantar.service.log.ServiceUserActionLog;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * /p1/p2/p3/p4/param
 *
 * p2p3...pn : camelCaseMethod
 * method(Params(request), response)
 */
public class RouteToMethodParam extends HttpServlet {

    protected static final Logger log = LoggerFactory.getLogger(RouteToMethodParam.class);
    private static boolean logRequest;

    static {
        if (Settings.web() != null) {
            logRequest = Settings.web().logRequest();
        }
    }


    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");

        if (request.getMethod().equalsIgnoreCase("OPTIONS") || request.getMethod().equalsIgnoreCase("HEAD")) {
            Response.headersOnly(response);
        } else {
            String path = request.getServletPath();
            String[] parts = StringUtil.split(path, '/');

            StringBuilder methodName = new StringBuilder(path.length()).append(StringUtil.isEmpty(parts[1]) ? "index" : parts[1]);

            for (int i = 2, l = parts.length; i < l; ++i) {
                String part = parts[i];
                methodName.append(Character.toUpperCase(part.charAt(0)));
                methodName.append(part.substring(1).toLowerCase());
            }

            Params params = new Params(request);
            Params.setThreadParams(params);

            String[] dParams = StringUtil.split(request.getPathInfo(), '/');
            if (dParams != null) {
                for (int i = 0, l = dParams.length; i < l; ++i) {
                    params.set("urlParam" + i, dParams[i]);
                }
            }

            if (logRequest) {
                ServiceUserActionLog.add("REQUEST", null);
                log.debug(" > {}", request.getRequestURI());
            }

            Locale.setSelectedLocale(params);
            java.lang.reflect.Method method;
            String m = methodName.toString();
            try {
                method = this.getClass().getMethod(m, Params.class, HttpServletResponse.class);

                if (method.isAnnotationPresent(VerifyPermission.class)) {
                    Services.get(ServiceAuth.class).permitController(params, m);
                } else if (method.isAnnotationPresent(Access.class)) {
                    Services.get(ServiceAuth.class).permitAccess(params, method.getAnnotation(Access.class).value());
                } else if (method.isAnnotationPresent(Feature.class)) {
                    Services.get(ServiceAuth.class).permitFeature(params, method.getAnnotation(Feature.class).value());
                }

                method.invoke(this, params, response);

            } catch (NoSuchMethodException e) {
                log.error(" !! no-handler-method/404 ({} > {})\n", request.getRequestURL(), methodName, e);
                Response.notFound(response, request.getRequestURL().toString());

            } catch (Throwable e) {
                Throwable x = e.getCause();
                if (x != null) {
                    e = x;
                }

                if (e instanceof FinishException){
                    // do nothing
                } else if (e instanceof InputException) {
                    Response.clientError(response, e.getMessage());

                } else if (e instanceof AuthException) {
                    Response.unauthorized(response, e.getMessage());

                } else if (e instanceof NoContentException) {
                    Response.noContent(response);

                } else if (e instanceof ServerException) {
                    Response.serverError(response, e.getMessage());

                } else {
                    log.error(" !! unhandled error ({} > {})\n", request.getRequestURL(), methodName, e);
                    Response.serverError(response, Locale.getString(VantarKey.UNEXPECTED_ERROR));
                }
            } finally {
                Locale.removeThreadLocale(Thread.currentThread().getId());
                Params.removeThreadParams();
            }
        }
    }
}