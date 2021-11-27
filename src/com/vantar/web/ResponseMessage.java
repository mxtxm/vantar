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


    public ResponseMessage(String message, int code) {
        this.code = code;
        successful = false;
        this.message = message;
    }

    public ResponseMessage(String message, Object value) {
        code = 200;
        successful = true;
        this.message = message;
        this.value = value;
    }

    public ResponseMessage(String message) {
        code = 200;
        successful = true;
        this.message = message;
    }

    public ResponseMessage(LangKey message, int code) {
        this.code = code;
        successful = false;
        this.message = Locale.getString(message, code);
    }

    public ResponseMessage(LangKey message, Object value) {
        code = 200;
        successful = true;
        this.message = Locale.getString(message);
        this.value = value;
        if (value instanceof Dto) {
            this.dto = (Dto) value;
        }
    }

    public ResponseMessage(LangKey message) {
        code = 200;
        successful = true;
        this.message = Locale.getString(message);
    }

    public ResponseMessage(LangKey message, Object value, Dto dto) {
        code = 200;
        successful = true;
        this.message = Locale.getString(message, code);
        this.value = value;
        this.dto = dto;
    }
}
