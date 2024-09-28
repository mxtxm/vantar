package com.vantar.database.dto;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UniqueGroup {

    String[] value();
}
