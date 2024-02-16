package com.vantar.exception;

import com.vantar.locale.DefaultStringsEn;
import com.vantar.locale.VantarKey;
import java.util.Set;


public class DateTimeException extends Exception {

    private String date;
    private Set<VantarKey> errors;


    public DateTimeException(String msg) {
        super(msg);
    }

    public DateTimeException(String date, Set<VantarKey> errors) {
        this.date = date;
        this.errors = errors;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(20);
        sb.append('(').append(date == null ? "NULL" : date).append(')');
        if (errors != null) {
            for (VantarKey error : errors) {
                sb.append(" ").append(DefaultStringsEn.getString(error));
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
