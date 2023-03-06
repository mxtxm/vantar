package com.vantar.service.auth;

import java.util.*;


public class RootRole implements CommonUserRole {

    public static final String NAME = "ROOT";
    public static final RootRole rootRole = new RootRole();


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    public Set<String> getAllowedFeatures() {
        return null;
    }
}
