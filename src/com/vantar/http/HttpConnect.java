package com.vantar.http;

import com.vantar.database.dto.Dto;
import com.vantar.exception.HttpException;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.util.json.Json;
import org.apache.http.*;
import org.apache.http.client.config.*;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.ssl.SSLContextBuilder;
import javax.ws.rs.HttpMethod;
import java.net.URI;
import java.security.*;
import java.util.*;


public class HttpConnect {

    private final HttpConfig config;
    private final RequestConfig requestConfig;
    private Map<String, String> headers;


    public HttpConnect() {
        this(new HttpConfig());
    }

    public HttpConnect(HttpConfig config) {
        this.config = config;
        int timeout = config.timeoutSec * 1000;
        requestConfig = RequestConfig.custom()
            .setSocketTimeout(timeout)
            .setConnectTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .setMaxRedirects(config.maxRedirects)
            .setRedirectsEnabled(true)
            .setCircularRedirectsAllowed(true)
            .setRelativeRedirectsAllowed(true)
            .setContentCompressionEnabled(false)
            .setCookieSpec(CookieSpecs.STANDARD)
            .build();
    }

    public HttpConnect addHeader(String name, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(name, value);
        return this;
    }

    public HttpConnect addHeader(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    // > > > HTTP METHOD HEADERS

    public Header[] headers(String url) throws HttpException {
        return get(url).getHeaders();
    }

    public Header[] headers(String url, Dto dto, String... fields) throws HttpException {
        return get(url, dto, fields).getHeaders();
    }

    public Header[] headers(String url, Map<String, Object> data) throws HttpException {
        return get(url, data).getHeaders();
    }

    // > > > HTTP METHOD GET

    public HttpResponse get(String url) throws HttpException {
        return get(HttpHelper.getUri(url));
    }

    public HttpResponse get(String url, Dto dto, String... includeFields) throws HttpException {
        return get (dto == null ? HttpHelper.getUri(url) : HttpHelper.getUri(url, dto, includeFields));
    }

    private HttpResponse get(String url, Map<String, Object> data) throws HttpException {
        return get(data == null ? HttpHelper.getUri(url) : HttpHelper.getUri(url, data));
    }

    private HttpResponse get(URI uri) throws HttpException {
        try {
            HttpGet request = new HttpGet(uri);
            request.setConfig(requestConfig);
            setHeaders(request);
            return getResponse(uri, request);
        } catch (IllegalArgumentException e) {
            throw new HttpException(e);
        }
    }

    // > > > HTTP METHOD DELETE

    public HttpResponse delete(String url) throws HttpException {
        return delete(HttpHelper.getUri(url));
    }

    public HttpResponse delete(String url, Dto dto, String... includeFields) throws HttpException {
        return delete (dto == null ? HttpHelper.getUri(url) : HttpHelper.getUri(url, dto, includeFields));
    }

    private HttpResponse delete(String url, Map<String, Object> data) throws HttpException {
        return delete(data == null ? HttpHelper.getUri(url) : HttpHelper.getUri(url, data));
    }

    private HttpResponse delete(URI uri) throws HttpException {
        try {
            HttpDelete request = new HttpDelete(uri);
            request.setConfig(requestConfig);
            setHeaders(request);
            return getResponse(uri, request);
        } catch (IllegalArgumentException e) {
            throw new HttpException(e);
        }
    }

    // > > > HTTP METHOD OPTIONS

    public HttpResponse options(String url) throws HttpException {
        return options(HttpHelper.getUri(url));
    }

    public HttpResponse options(String url, Dto dto, String... includeFields) throws HttpException {
        return options (dto == null ? HttpHelper.getUri(url) : HttpHelper.getUri(url, dto, includeFields));
    }

    private HttpResponse options(String url, Map<String, Object> data) throws HttpException {
        return options(data == null ? HttpHelper.getUri(url) : HttpHelper.getUri(url, data));
    }

    private HttpResponse options(URI uri) throws HttpException {
        try {
            HttpOptions request = new HttpOptions(uri);
            request.setConfig(requestConfig);
            setHeaders(request);
            return getResponse(uri, request);
        } catch (IllegalArgumentException e) {
            throw new HttpException(e);
        }
    }

    // > > > HTTP METHOD POST

    public HttpResponse post(String url) throws HttpException {
        return executeRequest(url, HttpMethod.POST, null);
    }

    public HttpResponse post(String url, Dto dto, String... includeFields) throws HttpException {
        return executeRequest(url, HttpMethod.POST, dto.getPropertyValues(includeFields));
    }

    public HttpResponse post(String url, Map<String, Object> data) throws HttpException {
        return executeRequest(url, HttpMethod.POST, data);
    }

    public HttpResponse postJson(String url, Object object) throws HttpException {
        return requestJson(url, HttpMethod.POST, object);
    }

    // > > > HTTP METHOD PUT

    public HttpResponse put(String url) throws HttpException {
        return executeRequest(url, HttpMethod.PUT, null);
    }

    public HttpResponse put(String url, Dto dto, String... includeFields) throws HttpException {
        return executeRequest(url, HttpMethod.PUT, dto.getPropertyValues(includeFields));
    }

    public HttpResponse put(String url, Map<String, Object> data) throws HttpException {
        return executeRequest(url, HttpMethod.PUT, data);
    }

    public HttpResponse putJson(String url, Object object) throws HttpException {
        return requestJson(url, HttpMethod.PUT, object);
    }

    // > > > HTTP METHOD PATCH

    public HttpResponse patch(String url) throws HttpException {
        return executeRequest(url, HttpMethod.PATCH, null);
    }

    public HttpResponse patch(String url, Dto dto, String... includeFields) throws HttpException {
        return executeRequest(url, HttpMethod.PATCH, dto.getPropertyValues(includeFields));
    }

    public HttpResponse patch(String url, Map<String, Object> data) throws HttpException {
        return executeRequest(url, HttpMethod.PATCH, data);
    }

    public HttpResponse patchJson(String url, Object object) throws HttpException {
        return requestJson(url, HttpMethod.PATCH, object);
    }

    // < < < HTTP METHOD

    public HttpResponse executeRequest(String url, String method, Map<String, Object> data) throws HttpException {
        HttpRequest request;
        try {
            URI uri = HttpHelper.getUri(url);
            HttpEntity requestEntity = HttpHelper.mapToEntity(data);

            if (HttpMethod.POST.equals(method)) {
                HttpPost r = new HttpPost(uri);
                r.setConfig(requestConfig);
                if (requestEntity != null) {
                   r.setEntity(requestEntity);
                }
                request = r;
            } else if (HttpMethod.PUT.equals(method)) {
                HttpPut r = new HttpPut(uri);
                r.setConfig(requestConfig);
                if (requestEntity != null) {
                    r.setEntity(requestEntity);
                }
                request = r;
            } else if (HttpMethod.PATCH.equals(method)) {
                HttpPatch r = new HttpPatch(uri);
                r.setConfig(requestConfig);
                if (requestEntity != null) {
                    r.setEntity(requestEntity);
                }
                request = r;
            } else {
                throw new HttpException(Locale.getString(VantarKey.INVALID_METHOD));
            }

            setHeaders(request);
            return getResponse(uri, request);
        } catch (IllegalArgumentException e) {
            throw new HttpException(e);
        }
    }

    private HttpResponse requestJson(String url, String method, Object object) throws HttpException {
        String json;
        if (object instanceof Dto) {
            json = Json.toJson(((Dto) object).getPropertyValues());
        } else if (object instanceof String) {
            json = (String) object;
        } else {
            json = Json.toJson(object);
        }

        HttpRequest request;
        try {
            StringEntity entity;
            if (json != null) {
                entity = new StringEntity(json, "UTF-8");
                entity.setContentEncoding("UTF-8");
                entity.setContentType("application/json");
            } else {
                entity = null;
            }

            URI uri = HttpHelper.getUri(url);
            if (HttpMethod.POST.equals(method)) {
                HttpPost r = new HttpPost(uri);
                r.setConfig(requestConfig);
                r.setEntity(entity);
                request = r;
            } else if (HttpMethod.PUT.equals(method)) {
                HttpPut r = new HttpPut(uri);
                r.setConfig(requestConfig);
                r.setEntity(entity);
                request = r;
            } else if (HttpMethod.PATCH.equals(method)) {
                HttpPatch r = new HttpPatch(uri);
                r.setConfig(requestConfig);
                r.setEntity(entity);
                request = r;
            } else {
                throw new HttpException(Locale.getString(VantarKey.INVALID_METHOD));
            }

            setHeaders(request);
            request.addHeader("content-type", "application/json; charset=UTF-8");
            return getResponse(uri, request);
        } catch (IllegalArgumentException e) {
            throw new HttpException(e);
        }
    }

    private void setHeaders(HttpRequest request) {
        if (headers != null) {
            headers.forEach(request::addHeader);
        }
    }

    private HttpResponse getResponse(URI uri, HttpRequest request) throws HttpException {
        request.addHeader("User-Agent", config.getUseragent());
        for (Map.Entry<String, String> entry : config.headers.entrySet()) {
            request.addHeader(entry.getKey(), entry.getValue());
        }

        CloseableHttpClient client = null;
        if (uri.getScheme().contains("https") && config.ignoreSsl) {
            SSLContextBuilder builder = new SSLContextBuilder();
            try {
                builder.loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true);
                client = HttpClients.custom().setSSLSocketFactory(new SSLConnectionSocketFactory(builder.build())).build();
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                throw new HttpException("could not skip SSL certs", e);
            }
        }

        if (client == null) {
            client = HttpClients.createDefault();
        }

        return new HttpResponse(uri, client, request);
    }
}
