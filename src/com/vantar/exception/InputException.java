package com.vantar.exception;

import com.vantar.database.common.ValidationError;
import com.vantar.locale.LangKey;
import java.util.*;


public class InputException extends VantarException {

    private List<ValidationError> errors;


    public InputException(String message) {
        super(message);
    }

    public InputException(Exception e) {
        super(e);
    }

    public InputException(VantarException e) {
        super(e);
    }

    public InputException(LangKey messageKey, Object... messageParams) {
        super(messageKey, messageParams);
        errors = new ArrayList<>(1);
        errors.add(new ValidationError(messageKey, messageParams));
    }

    public InputException(List<ValidationError> errors) {
        super(ValidationError.toString(errors));
        this.errors = errors;
    }

    public InputException(ValidationError error) {
        super(error.toString());
        errors = new ArrayList<>(1);
        errors.add(error);
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}
