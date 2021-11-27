package com.vantar.service.auth;

import java.util.*;


public interface CommonUserRole {

    String getName();
    boolean isRoot();
    Set<String> getAllowedFeatures();

}
