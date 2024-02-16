package com.vantar.web;

import com.vantar.common.Settings;
import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.locale.Locale;
import com.vantar.service.Services;
import com.vantar.service.auth.ServiceAuth;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.datetime.DateTime;
import org.slf4j.*;
import javax.servlet.http.*;
import java.util.*;


public abstract class RouteBase extends HttpServlet {

    protected static final Logger log = LoggerFactory.getLogger(RouteBase.class);
    protected static boolean logRequest;
    protected static Map<String, DateTime> lastCall;


    static {
        if (Settings.web() != null) {
            logRequest = Settings.web().logRequest();
        }
    }


    public void callMethod(HttpServletRequest request, HttpServletResponse response, StringBuilder methodName, String[] dParams) {
        Params params = new Params(request);
        Params.setThreadParams(params);

        if (dParams != null) {
            for (int i = 0, l = dParams.length; i < l; ++i) {
                params.set("urlParam" + i, dParams[i]);
            }
        }

        Locale.setSelectedLocale(params);
        java.lang.reflect.Method method;
        String m = methodName.toString();
        try {
            method = this.getClass().getMethod(m, Params.class, HttpServletResponse.class);

            if (logRequest && !method.isAnnotationPresent(NoLog.class)) {
                ServiceLog.addRequest(params);
            }

            if (method.isAnnotationPresent(CallTimeLimit.class)) {
                if (lastCall == null) {
                    lastCall = new HashMap<>(16, 1);
                }
                DateTime lastCalled = lastCall.get(m);
                if (lastCalled == null) {
                    lastCall.put(m, new DateTime());
                } else {
                    int limitMinutes = method.getAnnotation(CallTimeLimit.class).value();
                    int diff = new DateTime(lastCalled).addMinutes(limitMinutes).diffMinutesRaw(new DateTime());
                    if (diff > 0) {
                        Response.clientError(response, Locale.getString(VantarKey.METHOD_CALL_TIME_LIMIT, diff));
                        return;
                    }
                    lastCall.put(m, new DateTime());
                }
            }

            if (method.isAnnotationPresent(VerifyPermission.class)) {
                Services.getService(ServiceAuth.class).permitController(params, m);
            } else if (method.isAnnotationPresent(Access.class)) {
                Services.getService(ServiceAuth.class).permitAccess(params, method.getAnnotation(Access.class).value());
            } else if (method.isAnnotationPresent(Feature.class)) {
                Services.getService(ServiceAuth.class).permitFeature(params, method.getAnnotation(Feature.class).value());
            }

            if (method.isAnnotationPresent(BackgroundTask.class)) {
                RouteBase t = this;
                new Thread((new Runnable() {
                    @Override
                    public void run() {
                        try {
                            method.invoke(t, new Params(params), response);
                        } catch (Throwable e) {
                            log.error(" !! unhandled error ({} > {})\n", request.getRequestURL(), methodName, e);
                            Response.serverError(response, e.getMessage());
                        }
                    }
                })).start();
                response.getWriter().flush();
                response.getWriter().close();
            } else {
                method.invoke(this, params, response);
            }

        } catch (NoSuchMethodException e) {
            log.error(" !! no-handler-method/404 ({} > {})\n", request.getRequestURL(), methodName, e);
            Response.serviceUnavailable(response, Locale.getString(VantarKey.METHOD_UNAVAILABLE, methodName.toString()));

        } catch (Throwable e) {
            Throwable x = e.getCause();
            if (x != null) {
                e = x;
            }

            if (e instanceof FinishException) {
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