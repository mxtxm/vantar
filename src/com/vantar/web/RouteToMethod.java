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
 * p2p3...pn : camelCaseMethod
 * method(Params(request), response)
 */
public class RouteToMethod extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(RouteToMethod.class);
    public static RequestCallback requestCallback;

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

            Params params = new Params(request);
            if (requestCallback != null) {
                requestCallback.catchRequest(params);
            }

            Locale.setSelectedLocale(params);
            java.lang.reflect.Method method;
            try {
                method = this.getClass().getMethod(methodName.toString(), Params.class, HttpServletResponse.class);

                if (method.isAnnotationPresent(Access.class)) {
                    ServiceAuth auth = Services.get(ServiceAuth.class);
                    if (auth == null) {
                        throw new ServiceException(ServiceAuth.class);
                    }
                    auth.permitAccessStr(params, method.getAnnotation(Access.class).value());
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
}