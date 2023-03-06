package com.vantar.exception;

import com.vantar.locale.LangKey;


public class ServerException extends VantarException {

    public ServerException(String message) {
        super(message);
    }

    public ServerException(Exception e) {
        super(e);
    }

    public ServerException(VantarException e) {
        super(e);
    }

    public ServerException(LangKey messageKey, Object... messageParams) {
        super(messageKey, messageParams);
    }
}
