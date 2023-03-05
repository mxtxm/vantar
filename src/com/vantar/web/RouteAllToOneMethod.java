package com.vantar.web;

import com.vantar.exception.*;
import com.vantar.locale.*;
import com.vantar.service.log.ServiceUserActionLog;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * /p1/p2/p3/p4
 *
 * dispatcher(Params(request), response, url)
 */
public abstract class RouteAllToOneMethod extends RouteBase {

    public abstract void dispatcher(Params params, HttpServletResponse response, String url) throws VantarException;


    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");

        if (request.getMethod().equalsIgnoreCase("OPTIONS") || request.getMethod().equalsIgnoreCase("HEAD")) {
            Response.headersOnly(response);
        } else {
            Params params = new Params(request);
            Params.setThreadParams(params);

            if (logRequest) {
                ServiceUserActionLog.add("REQUEST", null);
                log.debug(" > {}", request.getRequestURI());
            }

            Locale.setSelectedLocale(params);
            try {
                dispatcher(params, response, request.getPathInfo());

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
                    log.error(" !! unhandled error ({} > dispatcher)\n", request.getRequestURL(), e);
                    Response.serverError(response, Locale.getString(VantarKey.UNEXPECTED_ERROR));
                }
            } finally {
                Locale.removeThreadLocale(Thread.currentThread().getId());
                Params.removeThreadParams();
            }
        }
    }
}