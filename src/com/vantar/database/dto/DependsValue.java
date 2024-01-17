package com.vantar.database.dto;

import java.lang.annotation.*;

/**
 * Field value must exists in another collection
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DependsValue {

    Class<? extends Dto> dto();
    String field();
}
