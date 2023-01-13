package com.vantar.web.dto;

import com.vantar.database.dto.*;
import com.vantar.exception.ServiceException;
import com.vantar.service.Services;
import com.vantar.service.auth.ServiceAuth;
import java.util.Set;

/**
 * if user.role(s) and allowedRoles have at least one match then permit
 * [OR] if user.role(s).features contains allowedFeature then permit
 */
@Cache
@Mongo
public class Permission extends DtoBase {

    public Long id;
    @Required
    public String method;
    // for info only
    public String path;
    public Set<String> allowedRoles;
    public String allowedFeature;
    public String extraData;


    @Override
    public void afterFetchData() {
        try {
            Services.get(ServiceAuth.class).flushControllerCache();
        } catch (ServiceException ignore) {

        }
    }
}
