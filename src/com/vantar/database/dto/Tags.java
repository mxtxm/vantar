package com.vantar.database.dto;

import java.lang.annotation.*;

/**
 * for WebUi.
 * "none" : no form
 * "password" : input type
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Tags {

    String[] value();
}
