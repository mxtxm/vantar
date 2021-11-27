package com.vantar.service.auth;


import java.util.List;


public interface CommonUser {

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
    List<CommonUserRole> getRoles();

    void setSigningIn();

    Long getId();

    boolean passwordEquals(String password);
}
