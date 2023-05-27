package com.vantar.service.auth;

import com.vantar.database.dto.Dto;
import java.util.*;


public interface CommonUser extends Dto {

    void setToken(String token);

    String getToken();

    AccessStatus getAccessStatus();

    String getFullName();

    String getMobile();

    String getEmail();

    String getUsername();

    CommonUserRole getRole();
    Collection<? extends CommonUserRole> getRoles();

    Long getId();

    Map<String, Object> getExtraData();
}
