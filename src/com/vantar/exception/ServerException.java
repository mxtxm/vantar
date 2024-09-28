package com.vantar.exception;

import com.vantar.locale.LangKey;


public class ServerException extends VantarException {

    public ServerException(Throwable e) {
        super(e);
    }

    public ServerException(String message) {
        super(message);
    }

    public ServerException(VantarException e) {
        super(e);
    }

    public ServerException(LangKey messageKey, Object... messageParams) {
        super(messageKey, messageParams);
    }
}
