package com.vantar.exception;

import com.vantar.locale.*;


public class NoContentException extends VantarException {

    public NoContentException() {
        super(VantarKey.NO_CONTENT);
    }

    public NoContentException(Exception e) {
        super(e);
    }

    public NoContentException(String message) {
        super(message);
    }

    public NoContentException(VantarException e) {
        super(e);
    }

    public NoContentException(LangKey messageKey, Object... messageParams) {
        super(messageKey, messageParams);
    }
}
