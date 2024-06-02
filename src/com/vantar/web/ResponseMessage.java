package com.vantar.web;

import com.vantar.database.dto.Dto;
import com.vantar.locale.*;
import com.vantar.util.object.ObjectUtil;
import java.util.Objects;


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

    @Override
    public String toString() {
        return ObjectUtil.toString(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }
        ResponseMessage rObj = (ResponseMessage) obj;

        if (this.code != rObj.code) {
            return false;
        }
        if (this.successful != rObj.successful) {
            return false;
        }
        if (!Objects.equals(this.message, rObj.message)) {
            return false;
        }
        if (!Objects.equals(this.value, rObj.value)) {
            return false;
        }
        if (!Objects.equals(this.dto, rObj.dto)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = getClass().hashCode();
        hash = 31 * hash + code;
        hash = 31 * hash + (successful ? 1 : 0);
        hash = 31 * hash + (value == null ? 0 : value.hashCode());
        hash = 31 * hash + (dto == null ? 0 : dto.hashCode());
        return hash;
    }
}
