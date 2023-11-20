package com.vantar.database.dto;

import java.lang.annotation.*;

/**
 * 30D      ---> each 30days
 * 1000000R ---> each 1000000Records
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Archive {

    String value();
}
