package com.vantar.service.auth;

import com.vantar.common.VantarParam;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.Params;
import com.vantar.web.dto.Permission;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Permit {

    protected static final int MAX_CONTROLLER_SIZE = 1000;
    protected final Map<String, TokenData> onlineUsers = new ConcurrentHashMap<>(ServiceAuth.MAX_SIGNED_USERS);
    private Map<String, Permission> controllerPermissionTable;
    public Integer tokenExpireMin;


    // PERMIT ROLE > > >


    public boolean hasAccess(Params params, CommonUserRole... allowed) {
        try {
            return permitAccess(params, allowed) != null;
        } catch (AuthException e) {
            return false;
        }
    }

    public CommonUser permitAccess(Params params, CommonUserRole... allowed) throws AuthException {
        TokenData token = validateToken(params, onlineUsers);

        // single role
        CommonUserRole role = token.user.getRole();
        if (role != null) {
            CommonUser user = getUserByMatchedRoles(role, token, allowed);
            if (user != null) {
                return user;
            }
            throw new AuthException(VantarKey.NO_ACCESS);
        }

        // multi role
        List<? extends CommonUserRole> roles = token.user.getRoles();
        if (CollectionUtil.isEmpty(roles)) {
            throw new AuthException(VantarKey.NO_ACCESS);
        }
        for (CommonUserRole r : roles) {
            CommonUser user = getUserByMatchedRoles(r, token, allowed);
            if (user != null) {
                return user;
            }
        }
        throw new AuthException(VantarKey.NO_ACCESS);
    }

    public boolean hasAccess(Params params, String... allowed) {
        try {
            return permitAccess(params, allowed) != null;
        } catch (AuthException e) {
            return false;
        }
    }

    public CommonUser permitAccess(Params params, String... allowed) throws AuthException {
        TokenData token = validateToken(params, onlineUsers);
        CommonUserRole role = token.user.getRole();

        // single role
        if (role != null) {
            CommonUser user = getUserByMatchedRolesString(role, token, allowed);
            if (user != null) {
                return user;
            }
            throw new AuthException(VantarKey.NO_ACCESS);
        }

        // multi role
        List<? extends CommonUserRole> roles = token.user.getRoles();
        if (CollectionUtil.isEmpty(roles)) {
            throw new AuthException(VantarKey.NO_ACCESS);
        }
        for (CommonUserRole r : roles) {
            CommonUser user = getUserByMatchedRolesString(r, token, allowed);
            if (user != null) {
                return user;
            }
        }
        throw new AuthException(VantarKey.NO_ACCESS);
    }

    private CommonUser getUserByMatchedRoles(CommonUserRole role, TokenData token, CommonUserRole... allowed) {
        // user is root or no roles requested
        if (allowed.length == 0 || role.isRoot()) {
            token.lastInteraction.setToNow();
            return token.user;
        }
        for (CommonUserRole access : allowed) {
            if (role.equals(access)) {
                token.lastInteraction.setToNow();
                return token.user;
            }
        }
        return null;
    }

    private CommonUser getUserByMatchedRolesString(CommonUserRole role, TokenData token, String... allowed) {
        // user is root or no roles requested
        if (allowed.length == 0 || role.isRoot()) {
            token.lastInteraction.setToNow();
            return token.user;
        }

        String roleTitle = role.getName();
        for (String access : allowed) {
            if (roleTitle.equals(access)) {
                token.lastInteraction.setToNow();
                return token.user;
            }
        }
        return null;
    }


    // < < < PERMIT ROLE


    // PERMIT FEATURE > > >


    public CommonUser permitFeature(Params params, String feature) throws AuthException {
        TokenData token = validateToken(params, onlineUsers);
        CommonUserRole role = token.user.getRole();

        // single role
        if (role != null) {
            CommonUser user = getUserByMatchedFeature(role, token, feature);
            if (user != null) {
                return user;
            }
            throw new AuthException(VantarKey.NO_ACCESS);
        }

        // multi role
        List<? extends CommonUserRole> roles = token.user.getRoles();
        if (CollectionUtil.isEmpty(roles)) {
            throw new AuthException(VantarKey.NO_ACCESS);
        }
        for (CommonUserRole r : roles) {
            CommonUser user = getUserByMatchedFeature(r, token, feature);
            if (user != null) {
                return user;
            }
        }
        throw new AuthException(VantarKey.NO_ACCESS);
    }

    private CommonUser getUserByMatchedFeature(CommonUserRole role, TokenData token, String feature) {
        // user is root or no feature requested
        if (StringUtil.isEmpty(feature) || role.isRoot()) {
            token.lastInteraction.setToNow();
            return token.user;
        }

        Set<String> features = role.getAllowedFeatures();
        if (features != null && features.contains(feature)) {
            token.lastInteraction.setToNow();
            return token.user;
        }
        return null;
    }

    //  < < < PERMIT FEATURE


    // PERMIT CONTROLLER > > >


    public void flushControllerCache() {
        controllerPermissionTable = null;
    }

    /**
     * Throws AuthException when not permitted
     */
    public void permitController(Params params, String methodName) throws AuthException, ServiceException {
        if (controllerPermissionTable == null) {
            controllerPermissionTable = new ConcurrentHashMap<>(MAX_CONTROLLER_SIZE);
            for (Permission p : Services.get(ServiceDtoCache.class).getList(Permission.class)) {
                controllerPermissionTable.put(p.method, p);
            }
        }

        TokenData token = validateToken(params, onlineUsers);
        CommonUserRole role = token.user.getRole();

        Permission methodPermissionRules = controllerPermissionTable.get(methodName);
        if (methodPermissionRules == null) {
            return;
        }

        if (CollectionUtil.isNotEmpty(methodPermissionRules.allowedRoles)) {
            // single role
            if (role != null) {
                if (getUserByMatchedRolesSet(role, token, methodPermissionRules.allowedRoles)) {
                    return;
                }
                throw new AuthException(VantarKey.NO_ACCESS);
            }

            // multi role
            List<? extends CommonUserRole> roles = token.user.getRoles();
            if (CollectionUtil.isEmpty(roles)) {
                throw new AuthException(VantarKey.NO_ACCESS);
            }
            for (CommonUserRole r : roles) {
                if (getUserByMatchedRolesSet(r, token, methodPermissionRules.allowedRoles)) {
                    return;
                }
            }
            throw new AuthException(VantarKey.NO_ACCESS);
        }

        if (StringUtil.isNotEmpty(methodPermissionRules.allowedFeature)) {
            // single feature
            if (role != null) {
                if (getUserByMatchedFeatureSet(role, token, methodPermissionRules.allowedFeature)) {
                    return;
                }
                throw new AuthException(VantarKey.NO_ACCESS);
            }

            // multi feature
            List<? extends CommonUserRole> roles = token.user.getRoles();
            if (CollectionUtil.isEmpty(roles)) {
                throw new AuthException(VantarKey.NO_ACCESS);
            }
            for (CommonUserRole r : roles) {
                if (getUserByMatchedFeatureSet(r, token, methodPermissionRules.allowedFeature)) {
                    return;
                }
            }
            throw new AuthException(VantarKey.NO_ACCESS);
        }
    }

    private boolean getUserByMatchedRolesSet(CommonUserRole role, TokenData token, Set<String> allowed) {
        // user is root or no roles requested
        if (allowed.size() == 0 || role.isRoot()) {
            token.lastInteraction.setToNow();
            return true;
        }

        if (allowed.contains(token.user.getRole().getName())) {
            token.lastInteraction.setToNow();
            return true;
        }
        return false;
    }

    private boolean getUserByMatchedFeatureSet(CommonUserRole role, TokenData token, String feature) {
        // user is root or no feature requested
        if (StringUtil.isEmpty(feature) || role.isRoot()) {
            token.lastInteraction.setToNow();
            return true;
        }

        Set<String> features = role.getAllowedFeatures();
        if (features != null && features.contains(feature)) {
            token.lastInteraction.setToNow();
            return true;
        }
        return false;
    }

    // < < < PERMIT CONTROLLER


    protected TokenData validateToken(Params params, Map<String, TokenData> map) throws AuthException {
        String token = params.getHeader(VantarParam.HEADER_AUTH_TOKEN);
        if (StringUtil.isEmpty(token)) {
            token = params.getString(VantarParam.AUTH_TOKEN);
        }
        if (StringUtil.isEmpty(token)) {
            token = ServiceAuth.startupAuthToken;
        }
        if (StringUtil.isEmpty(token)) {
            throw new AuthException(VantarKey.MISSING_AUTH_TOKEN);
        }

        TokenData tokenData = map.get(token);
        if (tokenData == null) {
            throw new AuthException(VantarKey.INVALID_AUTH_TOKEN);
        }

        if (-tokenData.lastInteraction.secondsFromNow() > (tokenExpireMin * 60)) {
            map.remove(token);
            throw new AuthException(VantarKey.EXPIRED_AUTH_TOKEN);
        }

        return tokenData;
    }

    public boolean isRoot(Params params) throws ServiceException, AuthException {
        CommonUserRole role = validateToken(params, onlineUsers).user.getRole();
        if (role != null) {
            return role.isRoot();
        }
        List<? extends CommonUserRole> roles = Services.get(ServiceAuth.class).getCurrentUser(params).getRoles();
        if (roles != null) {
            for (CommonUserRole roleC : roles) {
                if (roleC.isRoot()) {
                    return true;
                }
            }
        }
        return false;
    }
}