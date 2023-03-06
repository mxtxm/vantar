package com.vantar.database.dto;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FetchByFk {

    Class<? extends Dto> dto();
    String field();
    String fk();
}
