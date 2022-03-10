package com.vantar.service.auth;

import com.vantar.database.dto.Dto;


public interface CommonUserPassword extends Dto {

    String getPassword();

    void setPassword(String password);

    String getUsername();

    boolean passwordEquals(String password);
}
