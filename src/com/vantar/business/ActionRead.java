package com.vantar.business;

import com.vantar.exception.ServerException;
import com.vantar.exception.NoContentException;
import com.vantar.exception.InputException;


public interface ActionRead {

    Object get() throws ServerException, InputException, NoContentException;

    Object search() throws ServerException, InputException, NoContentException;

}
