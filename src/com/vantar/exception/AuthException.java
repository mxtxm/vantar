package com.vantar.exception;

import com.vantar.locale.LangKey;


public class AuthException extends VantarException {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(VantarException e) {
        super(e);
    }

    public AuthException(LangKey messageKey, Object... messageParams) {
        super(messageKey, messageParams);
    }
}
