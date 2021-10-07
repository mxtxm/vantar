package com.vantar.exception;

import com.vantar.locale.*;


public class DatabaseException extends Exception {

    public DatabaseException(Throwable e) {
        super(e);
    }

    public DatabaseException(Throwable e, String message) {
        super(e);
    }

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(LangKey key, Object... params) {
        super(Locale.getString(key, params));
    }
}
