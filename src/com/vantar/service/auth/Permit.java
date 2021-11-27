package com.vantar.service.auth;

import com.vantar.common.VantarParam;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.service.cache.ServiceDtoCache;
import com.vantar.util.string.StringUtil;
import com.vantar.web.Params;
import com.vantar.web.dto.Permission;
import org.elasticsearch.client.security.user.User;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Permit {

    protected static final int MAX_CONTROLLER_SIZE = 1000;

    protected final Map<String, TokenData> onlineUsers = new ConcurrentHashMap<>(ServiceAuth.MAX_SIGNED_USERS);
    private Map<String, Permission> controllerPermissionTable;

    public Integer tokenExpireMin;


    public boolean hasAccess(Params params, CommonUserRole... allowed) {
        try {
            return permitAccess(params, allowed) != null;
        } catch (AuthException ignore) {

        }
        return false;
    }

    public CommonUser permitAccess(Params params, CommonUserRole... allowed) throws AuthException {
        TokenData token = validateToken(params, onlineUsers);
        return permitAccess(token, allowed);
    }

    public CommonUser permitAccessString(Params params, String... allowed) throws AuthException {
        TokenData token = validateToken(params, onlineUsers);
        return permitAccess(token, allowed);
    }

    public synchronized CommonUser permitAccess(TokenData token, CommonUserRole... allowed) throws AuthException {
        CommonUserRole role = token.user.getRole();
        if (role == null) {
            return permitAccessMultiRole(token, allowed);
        }
        return permitAccessSingleRole(role, token, allowed);
    }

    private synchronized CommonUser permitAccessSingleRole(
        CommonUserRole role, TokenData token, CommonUserRole... allowed) throws AuthException {

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

        throw new AuthException(VantarKey.NO_ACCESS);
    }

    private synchronized CommonUser permitAccessMultiRole(TokenData token, CommonUserRole... allowed) throws AuthException {
        List<CommonUserRole> roles = token.user.getRoles();

        if (allowed.length == 0) {
            token.lastInteraction.setToNow();
            return token.user;
        }
        if (roles == null || roles.isEmpty()) {
            throw new AuthException(VantarKey.NO_ACCESS);
        }

        for (CommonUserRole role : roles) {
            // user is root or no roles requested
            if (role.isRoot()) {
                token.lastInteraction.setToNow();
                return token.user;
            }

            for (CommonUserRole access : allowed) {
                if (role.equals(access)) {
                    token.lastInteraction.setToNow();
                    return token.user;
                }
            }

        }

        throw new AuthException(VantarKey.NO_ACCESS);
    }

    private synchronized void limitAccess(CommonUserRole role, CommonUserRole... allowed) throws AuthException {
        if (role.equals(RootRole.rootRole)) {
            return;
        }

        for (CommonUserRole access : allowed) {
            if (role.equals(access)) {
                return;
            }
        }

        throw new AuthException(VantarKey.NO_ACCESS);
    }

    public synchronized CommonUser permitAccess(TokenData token, String... allowed) throws AuthException {
        CommonUserRole role = token.user.getRole();
        if (role == null) {
            return permitAccessMultiRoleString(token, allowed);
        }
        return permitAccessSingleRoleString(role, token, allowed);
    }

    private synchronized CommonUser permitAccessSingleRoleString(
        CommonUserRole role, TokenData token, String... allowed) throws AuthException {

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

        throw new AuthException(VantarKey.NO_ACCESS);
    }

    private synchronized CommonUser permitAccessMultiRoleString(TokenData tokenData, String... allowed) throws AuthException {
        List<CommonUserRole> roles = tokenData.user.getRoles();

        if (allowed.length == 0) {
            tokenData.lastInteraction.setToNow();
            return tokenData.user;
        }
        if (roles == null || roles.isEmpty()) {
            throw new AuthException(VantarKey.NO_ACCESS);
        }

        for (CommonUserRole role : roles) {
            // user is root or no roles requested
            if (role.isRoot()) {
                tokenData.lastInteraction.setToNow();
                return tokenData.user;
            }

            String roleTitle = role.getName();
            for (String access : allowed) {
                if (roleTitle.equals(access)) {
                    tokenData.lastInteraction.setToNow();
                    return tokenData.user;
                }
            }
        }

        throw new AuthException(VantarKey.NO_ACCESS);
    }

    private synchronized void limitAccessString(CommonUserRole role, String... allowed) throws AuthException {
        if (role.equals(RootRole.rootRole)) {
            return;
        }

        String roleTitle = role.getName();
        for (String access : allowed) {
            if (roleTitle.equals(access)) {
                return;
            }
        }

        throw new AuthException(VantarKey.NO_ACCESS);
    }

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

    public CommonUser permitFeature(Params params, String feature) throws AuthException {
        TokenData token = validateToken(params, onlineUsers);
        CommonUserRole role = token.user.getRole();
        if (role == null) {
            return permitFeatureMultiRole(token, feature);
        }
        return permitFeatureSingleRole(role, token, feature);
    }

    private synchronized CommonUser permitFeatureSingleRole(
        CommonUserRole role, TokenData token, String feature) throws AuthException {

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

        throw new AuthException(VantarKey.NO_ACCESS);
    }

    private synchronized CommonUser permitFeatureMultiRole(TokenData token, String feature) throws AuthException {
        List<CommonUserRole> roles = token.user.getRoles();

        if (StringUtil.isEmpty(feature)) {
            token.lastInteraction.setToNow();
            return token.user;
        }
        if (roles == null || roles.isEmpty()) {
            throw new AuthException(VantarKey.NO_ACCESS);
        }

        for (CommonUserRole role : roles) {
            // user is root or no roles requested
            if (role.isRoot()) {
                token.lastInteraction.setToNow();
                return token.user;
            }

            Set<String> features = role.getAllowedFeatures();
            if (features != null && features.contains(feature)) {
                token.lastInteraction.setToNow();
                return token.user;
            }
        }

        throw new AuthException(VantarKey.NO_ACCESS);
    }





    public void flushControllerCache() {
        controllerPermissionTable = null;
    }

    public void permitController(Params params, String methodName) throws AuthException, ServiceException {
        TokenData token = validateToken(params, onlineUsers);

        if (controllerPermissionTable == null) {
            controllerPermissionTable = new ConcurrentHashMap<>(MAX_CONTROLLER_SIZE);
            ServiceDtoCache cache = Services.get(ServiceDtoCache.class);
            if (cache == null) {
                throw new ServiceException(ServiceDtoCache.class);
            }
            for (Permission p : cache.getList(Permission.class)) {
                controllerPermissionTable.put(p.method, p);
            }
        }

        Permission item = controllerPermissionTable.get(methodName);
        if (item == null) {
            return;
        }

        if (item.allowedRoles != null && !item.allowedRoles.isEmpty()) {
            if (token.user.getRole() != null) {
                if (token.user.getRole().isRoot()) {
                    token.lastInteraction.setToNow();
                    return;
                }

                String roleTitle = token.user.getRole().getName();
                if (item.allowedRoles.contains(roleTitle)) {
                    return;
                }
            }

            if (token.user.getRoles() == null || token.user.getRoles().isEmpty()) {
                throw new AuthException(VantarKey.NO_ACCESS);
            }

            for (CommonUserRole r : token.user.getRoles()) {
                if (r.isRoot()) {
                    token.lastInteraction.setToNow();
                    return;
                }

                String roleTitle = r.getName();
                if (item.allowedRoles.contains(roleTitle)) {
                    return;
                }
            }
            throw new AuthException(VantarKey.NO_ACCESS);
        }

        if (StringUtil.isNotEmpty(item.allowedFeature)) {
            if (token.user.getRole() != null) {
                if (token.user.getRole().getAllowedFeatures().contains(item.allowedFeature)) {
                    token.lastInteraction.setToNow();
                    return;
                }
            }

        }

        throw new AuthException(VantarKey.NO_ACCESS);
    }

}