package com.vantar.service.auth;

import com.vantar.database.dto.Dto;
import java.util.List;


public interface CommonUser extends Dto {

    void setToken(String token);

    String getToken();

    AccessStatus getAccessStatus();

    void nullPassword();

    String getPassword();

    String getFullName();

    String getMobile();

    String getEmail();

    String getUsername();

    CommonUserRole getRole();
    List<? extends CommonUserRole> getRoles();

    void setSigningIn();

    Long getId();

    boolean passwordEquals(String password);
}
