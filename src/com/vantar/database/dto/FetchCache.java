package com.vantar.database.dto;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FetchCache {

    String value() default "";
    Class<? extends Dto> dto() default Dto.class;
    String field() default "";
}
