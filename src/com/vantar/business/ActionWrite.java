package com.vantar.business;

import com.vantar.exception.ServerException;
import com.vantar.exception.InputException;


public interface ActionWrite {

    int add() throws ServerException, InputException;

    void delete() throws ServerException, InputException;

    void update() throws ServerException, InputException;

}
