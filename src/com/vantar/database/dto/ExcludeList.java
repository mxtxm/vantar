package com.vantar.database.dto;

import java.lang.annotation.*;

/**
 * exclude from list in admin panel
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExcludeList {

}
