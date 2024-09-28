package com.vantar.exception;

import com.vantar.locale.*;


public class ServiceException extends ServerException {

    public ServiceException(Class<?> service) {
        super(VantarKey.ADMIN_SERVICE_IS_OFF, service.getSimpleName());
    }

    public ServiceException(String service) {
        super(VantarKey.ADMIN_SERVICE_IS_OFF, service);
    }
}
