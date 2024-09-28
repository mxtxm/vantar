package com.vantar.http;

import com.vantar.exception.HttpException;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.json.Json;
import com.vantar.util.string.StringUtil;
import org.apache.http.*;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.*;
import java.io.*;
import java.net.URI;
import java.util.*;


public class HttpResponse {

    private static final Logger log = LoggerFactory.getLogger(HttpConnect.class);

    private final String url;
    private final HttpEntity entity;
    private final HttpRequest request;
    private CloseableHttpResponse response;
    private String content;


    public HttpResponse(URI uri, CloseableHttpClient client, HttpRequest request) throws HttpException {
        this.url = uri.toString();
        this.request = request;

        try {
            response = client.execute(URIUtils.extractHost(uri), request);

            boolean isGzipped = false;
            for (Header header : response.getHeaders("content-encoding")) {
                if (header.getValue().toLowerCase().contains("gzip")) {
                    isGzipped = true;
                    break;
                }
            }
            entity = isGzipped ? new GzipDecompressingEntity(response.getEntity()) : response.getEntity();

            if (entity != null) {
                content = EntityUtils.toString(entity, "UTF-8");
                if (content == null) {
                    content = "";
                }
            }
        } catch (Exception e) {
            throw new HttpException("response failed", e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                client.close();
            } catch (Exception e) {
                log.error(" !! response.close failed\n", e);
            }
        }
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return response.getStatusLine().getStatusCode();
    }

    public boolean isOk() {
        return getStatusCode() == HttpStatus.SC_OK;
    }

    public boolean isServerError() {
        return getStatusCode() / 100 == 5;
    }

    public boolean isClientError() {
        return getStatusCode() / 100 == 4;
    }

    public boolean isUnauthorized() {
        return getStatusCode() == HttpStatus.SC_UNAUTHORIZED;
    }

    public boolean isForbidden() {
        return getStatusCode() == HttpStatus.SC_FORBIDDEN;
    }

    @Override
    public String toString() {
        return content;
    }

    public String toStringDecoded() {
        return Json.d.fromJson(toString(), String.class);
    }

    public <T> T toObject(Class<T> typeClass) {
        return Json.d.fromJson(toString(), typeClass);
    }

    public <T> List<T> toList(Class<T> type) {
        return Json.d.listFromJson(toString(), type);
    }

    public <T> List<T> toList() {
        return Json.d.fromJson(toString(), List.class);
    }

    public <K, V> Map<K, V> toMap() {
        return Json.d.fromJson(toString(), Map.class);
    }

    public <K, V> Map<K, V> toMap(Class<K> typeClassKey, Class<V> typeClassValue) {
        return Json.d.mapFromJson(toString(), typeClassKey, typeClassValue);
    }

    public Integer toInteger() {
        return StringUtil.toInteger(StringUtil.remove(toString(), '[', ']'));
    }

    public Long toLong() {
        return StringUtil.toLong(StringUtil.remove(toString(), '[', ']'));
    }

    public Double toDouble() {
        return StringUtil.toDouble(StringUtil.remove(toString(), '[', ']'));
    }

    public Double toFloat() {
        return StringUtil.toDouble(StringUtil.remove(toString(), '[', ']'));
    }

    public Boolean toBoolean() {
        return StringUtil.toBoolean(StringUtil.remove(toString(), '[', ']'));
    }

    public InputStream toStream() {
        try {
            return entity.getContent();
        } catch (Exception e) {
            log.error(" ! failed to read stream ({})\n", url, e);
            return null;
        }
    }

    public Header[] getHeaders() {
        return response.getAllHeaders();
    }

    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder(1000);
        sb  .append(request.toString()).append("\n")
            .append("request headers: [\n");

        for (Header header : request.getAllHeaders()) {
            sb.append(header.getName()).append(": ").append(header.getValue()).append("\n");
        }

        try {
            sb.append("request content: ").append(EntityUtils.toString(((HttpEntityEnclosingRequest) request).getEntity())).append("\n\n;");
        } catch (IOException | IllegalArgumentException e) {
            sb.append("request content: IO ERROR");
        }

        sb  .append("]\n")
            .append("response status: ").append(getStatusCode()).append("\n")
            .append("response headers: [\n").append(CollectionUtil.join(getHeaders(), '\n')).append("\n]\n")
            .append("response content:\n").append(toString());

        return sb.toString();
    }
}
