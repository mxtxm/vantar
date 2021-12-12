package com.vantar.database.common;

import com.vantar.locale.LangKey;
import com.vantar.locale.Locale;
import java.util.List;


public class ValidationError {

    public final LangKey error;
    public final Object[] params;


    public ValidationError(String name, LangKey error) {
        this.error = error;
        params = new String[] { name == null ? "" : name };
    }

    public ValidationError(String name, LangKey error, Object... params) {
        this.error = error;
        this.params = new String[params.length + 1];
        this.params[0] = name == null ? "" : name;
        for (int i = 0, paramsLength = params.length; i < paramsLength; i++) {
            this.params[i+1] = params[i];
        }
    }

    public ValidationError(LangKey error, Object... params) {
        this.error = error;
        this.params = params;
    }

    public String toString() {
        return Locale.getString(error, params);
    }

    public static String toString(List<ValidationError> errors) {
        return toString(errors, '\n');
    }

    public static String toString(List<ValidationError> errors, char separator) {
        StringBuilder msg = new StringBuilder();
        for (ValidationError error : errors) {
            msg.append(error.toString()).append(separator);
        }
        if (msg.length() > 1) {
            msg.setLength(msg.length() - 1);
        }
        return msg.toString();
    }

    public static String toString(List<ValidationError> errors, String separator) {
        StringBuilder msg = new StringBuilder();
        for (ValidationError error : errors) {
            msg.append(error.toString()).append(separator);
        }
        if (msg.length() > 1) {
            msg.setLength(msg.length() - separator.length());
        }
        return msg.toString();
    }
}
