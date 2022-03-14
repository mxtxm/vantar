package com.vantar.web;

import com.vantar.database.dto.Dto;
import com.vantar.locale.LangKey;
import com.vantar.locale.Locale;


public class ResponseMessage {

    public int code;
    public String message;
    public Object value;
    public Dto dto;
    public boolean successful;


    public ResponseMessage() {

    }

    public static ResponseMessage success(LangKey message) {
        return get(200, Locale.getString(message, 200), null, null);
    }
    public static ResponseMessage success(String message) {
        return get(200, message, null, null);
    }
    public static ResponseMessage success(String message, Object value) {
        return get(200, Locale.getString(message, 200), value, null);
    }
    public static ResponseMessage success(LangKey message, Object value) {
        return get(200, Locale.getString(message, 200), value, value instanceof Dto ? (Dto) value : null);
    }
    public static ResponseMessage success(LangKey message, Object value, Dto dto) {
        return get(200, Locale.getString(message, 200), value, dto);
    }
    public static ResponseMessage success(String message, Object value, Dto dto) {
        return get(200, message, value, dto);
    }

    public static ResponseMessage get(int code, LangKey message) {
        return get(code, Locale.getString(message, 200), null, null);
    }
    public static ResponseMessage get(int code, String message) {
        return get(code, message, null, null);
    }
    public static ResponseMessage get(int code, String message, Object value) {
        return get(code, Locale.getString(message, 200), value, null);
    }
    public static ResponseMessage get(int code, LangKey message, Object value) {
        return get(code, Locale.getString(message, 200), value, value instanceof Dto ? (Dto) value : null);
    }
    public static ResponseMessage get(int code, LangKey message, Object value, Dto dto) {
        return get(code, Locale.getString(message, 200), value, dto);
    }
    public static ResponseMessage get(int code, String message, Object value, Dto dto) {
        ResponseMessage r = new ResponseMessage();
        r.code = code;
        r.successful = code >= 200 && code < 300;
        r.message = message;
        r.value = value;
        r.dto = dto;
        return r;
    }
}
