package com.vantar.web;

import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.ServiceAuth;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * /p1/p2/p3/p4/p5
 *
 * methodP2p3...pn : camelCaseMethod
 * method(Params(request), response)
 */
public class RestfullToMethod extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(RestfullToMethod.class);
    public static RequestCallback requestCallback;

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

        Params params = new Params(request);
        if (requestCallback != null) {
            requestCallback.catchRequest(params);
        }

        Locale.setSelectedLocale(params);
        java.lang.reflect.Method method;
        String m = methodName.toString();
        try {
            method = this.getClass().getMethod(m, Params.class, HttpServletResponse.class);

            if (method.isAnnotationPresent(VerifyPermission.class)) {
                ServiceAuth auth = Services.get(ServiceAuth.class);
                if (auth == null) {
                    throw new ServiceException(ServiceAuth.class);
                }
                auth.permitController(params, m);
            } else if (method.isAnnotationPresent(Access.class)) {
                ServiceAuth auth = Services.get(ServiceAuth.class);
                if (auth == null) {
                    throw new ServiceException(ServiceAuth.class);
                }
                auth.permitAccessString(params, method.getAnnotation(Access.class).value());
            } else if (method.isAnnotationPresent(Feature.class)) {
                ServiceAuth auth = Services.get(ServiceAuth.class);
                if (auth == null) {
                    throw new ServiceException(ServiceAuth.class);
                }
                auth.permitFeature(params, method.getAnnotation(Feature.class).value());
            }

            method.invoke(this, params, response);

        } catch (NoSuchMethodException e) {
            Response.serviceUnavailable(response, Locale.getString(VantarKey.METHOD_UNAVAILABLE, methodName.toString()));

        } catch (Throwable e) {
            Throwable x = e.getCause();
            if (x != null) {
                e = x;
            }

            if (e instanceof InputException) {
                Response.clientError(response, e.getMessage());

            } else if (e instanceof AuthException) {
                Response.unauthorized(response, e.getMessage());

            } else if (e instanceof NoContentException) {
                Response.noContent(response);

            } else if (e instanceof ServerException) {
                Response.serverError(response, e.getMessage());

            } else {
                log.error("! unhandled error", e);
                Response.serverError(response, Locale.getString(VantarKey.UNEXPECTED_ERROR));
            }
        } finally {
            Locale.removeThreadLocale(Thread.currentThread().getId());
        }
    }
}