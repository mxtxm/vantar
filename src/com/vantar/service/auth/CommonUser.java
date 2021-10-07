package com.vantar.service.auth;


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

    void setSigningIn();

    Long getId();

    boolean passwordEquals(String password);
}
