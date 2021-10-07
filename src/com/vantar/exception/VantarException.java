package com.vantar.exception;

import com.vantar.locale.LangKey;
import com.vantar.locale.Locale;


public class VantarException extends Exception {

    private LangKey messageKey;
    private Object[] messageParams;


    public VantarException(Exception e) {
        super(e);
    }

    public VantarException(String message) {
        super(message);
    }

    public VantarException(VantarException e) {
        messageKey = e.messageKey;
        messageParams = e.messageParams;
    }

    public VantarException(LangKey messageKey, Object... messageParams) {
        this.messageKey = messageKey;
        this.messageParams = messageParams;
    }

    @Override
    public String getMessage() {
        return messageKey == null ? super.getMessage() : Locale.getString(messageKey, messageParams);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
