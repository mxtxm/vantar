package com.vantar.web.dto;

import com.vantar.database.dto.*;
import com.vantar.service.Services;
import com.vantar.service.auth.ServiceAuth;
import java.util.Set;


@Cache
@Mongo
public class Permission extends DtoBase {

    public Long id;
    @Required
    public String method;
    public Set<String> allowedRoles;
    public String allowedFeature;


    @Override
    public void afterFetchData() {
        ServiceAuth auth = Services.get(ServiceAuth.class);
        if (auth != null) {
            auth.flushControllerCache();
        }
    }
}
