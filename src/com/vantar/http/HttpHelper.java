package com.vantar.http;

import com.vantar.database.dto.Dto;
import com.vantar.exception.HttpException;
import org.apache.http.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;


public class HttpHelper {

    protected static URI getUri(String url) throws HttpException {
        try {
            URI uri = new URI(Url.removeUnsafeChars(url));
            if (!uri.isAbsolute() || uri.isOpaque()) {
                throw new HttpException("URI error: (" + url + ") isAbsolute or isOpaque");
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new HttpException("URI error", e);
        }
    }

    /**
     * Dto > url for get
     */
    protected static URI getUri(String url, Dto dto, String[] includeFields) throws HttpException {
        return getUri(url, dto.getPropertyValues(includeFields));
    }

    protected static URI getUri(String url, Map<String, Object> data) throws HttpException {
        try {
            URIBuilder builder = new URIBuilder(url);

            for (Map.Entry<String, Object> item : data.entrySet()) {
                Object value = item.getValue();
                if (value != null) {
                    builder.setParameter(item.getKey(), value.toString());
                }
            }

            URI uri = builder.build();
            if (!uri.isAbsolute() || uri.isOpaque()) {
                throw new HttpException("URI error: (" + url + ") isAbsolute or isOpaque");
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new HttpException("URI error", e);
        }
    }

    /**
     * Dto > entity for post
     */
    protected static HttpEntity dtoToEntity(Dto dto, String[] fields) {
        return dto == null ? null : mapToEntity(dto.getPropertyValues(fields));
    }

    /**
     * Map > entity for post
     */
    protected static HttpEntity mapToEntity(Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        List<NameValuePair> params = new ArrayList<>(data.size());
        for (Map.Entry<String, Object> item : data.entrySet()) {
            Object value = item.getValue();
            if (value != null) {
                params.add(new BasicNameValuePair(item.getKey(), value.toString()));
            }
        }

        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            entity.setContentEncoding("UTF-8");
            return entity;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
