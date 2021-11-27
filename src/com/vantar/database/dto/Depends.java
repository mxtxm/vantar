package com.vantar.database.dto;

import java.lang.annotation.*;

/**
 * Can only used on Long fkId fields
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Depends {

    Class<? extends Dto> value();
}
