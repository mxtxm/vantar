package com.vantar.exception;


public class HttpException extends Exception {

    public HttpException(String message) {
        super(message);
    }

    public HttpException(Exception e) {
        super(e);
    }

    public HttpException(String message, Exception e) {
        super(message + " (" + e.getMessage() + ")");
        setStackTrace(e.getStackTrace());
    }
}
