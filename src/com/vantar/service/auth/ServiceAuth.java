package com.vantar.service.auth;

import com.vantar.common.VantarParam;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.file.FileUtil;
import com.vantar.util.json.Json;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.Params;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.*;
import java.util.Map;
import java.util.concurrent.*;


public class ServiceAuth implements Services.Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceAuth.class);

    private static final int MAX_SIGNED_USERS = 200;
    private static final int MAX_VERIFY_TOKENS = 20;
    private static final int DEFAULT_VERIFY_TOKEN_LENGTH = 4;
    private static final boolean DEFAULT_VERIFY_TOKEN_NUMBER_ONLY = true;

    private ScheduledExecutorService schedule;
    private final Map<String, TokenData> onlineUsers = new ConcurrentHashMap<>(MAX_SIGNED_USERS);
    private final Map<String, TokenData> signupVerifyTokens = new ConcurrentHashMap<>(MAX_VERIFY_TOKENS);
    private final Map<String, TokenData> oneTimeTokens = new ConcurrentHashMap<>(MAX_VERIFY_TOKENS);
    private final Map<String, TokenData> verifyTokens = new ConcurrentHashMap<>(MAX_VERIFY_TOKENS);
    private static String startupAuthToken;
    private Event event;

    public Integer tokenExpireMin;
    public Integer tokenCheckIntervalMin;
    public Boolean onEndSetNull;
    public Boolean debug;
    public String tokenStorePath;
    public int signUpVerifyTokenLength = DEFAULT_VERIFY_TOKEN_LENGTH;
    public int signInVerifyTokenLength = DEFAULT_VERIFY_TOKEN_LENGTH;
    public boolean signUpVerifyTokenNumberOnly = DEFAULT_VERIFY_TOKEN_NUMBER_ONLY;
    public boolean signInVerifyTokenNumberOnly = DEFAULT_VERIFY_TOKEN_NUMBER_ONLY;
    private CommonUser dummyUser;


    public void start() {
        schedule = Executors.newSingleThreadScheduledExecutor();
        schedule.scheduleWithFixedDelay(this::validateTokens, tokenCheckIntervalMin, tokenCheckIntervalMin, TimeUnit.MINUTES);
    }

    public void stop() {
        if (tokenStorePath != null) {
            Json.addInterface(CommonUser.class);
            FileUtil.write(
                tokenStorePath,
                Json.toJson(onlineUsers) + VantarParam.SEPARATOR_BLOCK_COMPLEX +
                Json.toJson(signupVerifyTokens) + VantarParam.SEPARATOR_BLOCK_COMPLEX +
                Json.toJson(oneTimeTokens) + VantarParam.SEPARATOR_BLOCK_COMPLEX +
                Json.toJson(verifyTokens)
            );
            log.info("Auth tokens are backed up.");
        }
        schedule.shutdown();
    }

    public ServiceAuth restoreTokens() {
        if (tokenStorePath == null || !FileUtil.exists(tokenStorePath)) {
            return this;
        }

        String contents = FileUtil.getFileContent(tokenStorePath);
        if (StringUtil.isEmpty(contents)) {
            return this;
        }

        String[] parts = StringUtil.split(contents, VantarParam.SEPARATOR_BLOCK_COMPLEX);
        if (parts.length == 4) {
            Json.addInterface(CommonUser.class);
            if (StringUtil.isNotEmpty(parts[0])) {
                Map<String, TokenData> x = Json.mapFromJson(parts[0], String.class, TokenData.class);
                if (x != null) {
                    onlineUsers.putAll(x);
                }
            }
            if (StringUtil.isNotEmpty(parts[1])) {
                Map<String, TokenData> x = Json.mapFromJson(parts[1], String.class, TokenData.class);
                if (x != null) {
                    signupVerifyTokens.putAll(x);
                }
            }
            if (StringUtil.isNotEmpty(parts[2])) {
                Map<String, TokenData> x = Json.mapFromJson(parts[2], String.class, TokenData.class);
                if (x != null) {
                    oneTimeTokens.putAll(x);
                }
            }
            if (StringUtil.isNotEmpty(parts[3])) {
                Map<String, TokenData> x = Json.mapFromJson(parts[3], String.class, TokenData.class);
                if (x != null) {
                    verifyTokens.putAll(x);
                }
            }
            log.info("Auth tokens are loaded.");
        }
        return this;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public boolean onEndSetNull() {
        return onEndSetNull;
    }

    // > > > signup

    public synchronized String getSignupVerifyToken(CommonUser user) {
        user.setToken(
            signUpVerifyTokenNumberOnly ?
                StringUtil.getRandomStringOnlyNumbers(signUpVerifyTokenLength) :
                StringUtil.getRandomString(signUpVerifyTokenLength)
        );
        removeItem(user, signupVerifyTokens);
        signupVerifyTokens.put(user.getToken(), new TokenData(user));
        return user.getToken();
    }

    public synchronized CommonUser checkSignupVerifyToken(Params params) throws AuthException {
        TokenData tokenData = validateToken(params, signupVerifyTokens);
        signupVerifyTokens.remove(tokenData.user.getToken());
        makeUserOnline(new TokenData(tokenData.user));
        return tokenData.user;
    }

    // > > > signin

    public synchronized String getOneTimeSigninToken(CommonUser user) throws AuthException {
        user.setToken(
            signInVerifyTokenNumberOnly ?
                StringUtil.getRandomStringOnlyNumbers(signInVerifyTokenLength) :
                StringUtil.getRandomString(signInVerifyTokenLength)
        );
        removeItem(user, oneTimeTokens);

        if (user.getAccessStatus().equals(AccessStatus.DISABLED)) {
            throw new AuthException(VantarKey.USER_DISABLED);
        }
        if (user.getAccessStatus().equals(AccessStatus.UNSUBSCRIBED)) {
            throw new AuthException(VantarKey.USER_NOT_EXISTS);
        }

        oneTimeTokens.put(user.getToken(), new TokenData(user));
        return user.getToken();
    }

    public synchronized CommonUser signin(Params params, CommonUserRole... allowed) throws ServerException, AuthException {
        String username = params.getString(VantarParam.USER_NAME);
        String password = params.getString(VantarParam.PASSWORD);
        if (StringUtil.isEmpty(username) || StringUtil.isEmpty(password)) {
            throw new AuthException(VantarKey.USER_PASSWORD_EMPTY);
        }

        TokenData tokenData = oneTimeTokens.get(password);
        if (tokenData != null) {
            tokenData = new TokenData(tokenData.user);
            oneTimeTokens.remove(password);
            if (allowed.length > 0) {
                permitAccess(tokenData, allowed);
            }
            makeUserOnline(tokenData);
            return tokenData.user;
        }

        if (event == null) {
            throw new AuthException(VantarKey.USER_REPO_NOT_SET);
        }
        CommonUser user;
        try {
            user = event.getUser(username);
        } catch (NoContentException e) {
            throw new AuthException(VantarKey.USER_NOT_EXISTS);
        } catch (DatabaseException e) {
            throw new ServerException(VantarKey.FETCH_FAIL);
        }

        if (user.getAccessStatus().equals(AccessStatus.DISABLED)) {
            throw new AuthException(VantarKey.USER_DISABLED);
        }
        if (user.getAccessStatus().equals(AccessStatus.UNSUBSCRIBED)) {
            throw new AuthException(VantarKey.USER_NOT_EXISTS);
        }

        if (!user.passwordEquals(password)) {
            throw new AuthException(VantarKey.WRONG_PASSWORD);
        }

        tokenData = new TokenData(user);
        makeUserOnline(tokenData);
        permitAccess(tokenData, allowed);

        CommonUser u = Json.fromJson(Json.toJson(user), user.getClass());
        u.nullPassword();
        return u;
    }

    // > > > signout

    public synchronized void signout(Params params) {
        String token = params.getHeader(VantarParam.HEADER_AUTH_TOKEN);
        if (token != null) {
            onlineUsers.remove(token);
        }
    }

    // > > > checking

    public boolean hasAccess(Params params, CommonUserRole... allowed) {
        try {
            return permitAccess(params, allowed) != null;
        } catch (AuthException e) {

            if (debug && dummyUser != null) {
                try {
                    limitAccess(dummyUser.getRole(), allowed);
                    return true;
                } catch (AuthException ignore) {

                }
            }

            return false;
        }
    }

    public CommonUser permitAccess(Params params, CommonUserRole... allowed) throws AuthException {
        TokenData token;
        try {
            token = validateToken(params, onlineUsers);
        } catch (AuthException e) {
            if (debug && dummyUser != null) {
                limitAccess(dummyUser.getRole(), allowed);
                return dummyUser;
            }
            throw e;
        }

        return permitAccess(token, allowed);
    }

    public CommonUser permitAccessStr(Params params, String... allowed) throws AuthException {
        TokenData token;
        try {
            token = validateToken(params, onlineUsers);
        } catch (AuthException e) {
            if (debug && dummyUser != null) {
                limitAccess(dummyUser.getRole(), allowed);
                return dummyUser;
            }
            throw e;
        }

        return permitAccess(token, allowed);
    }

    public synchronized CommonUser permitAccess(TokenData tokenData, CommonUserRole... allowed) throws AuthException {
        CommonUserRole role = tokenData.user.getRole();
        String roleTitle = role.toString();

        if (allowed.length == 0 || roleTitle.equals(AdminUserRole.ROOT.toString())) {
            tokenData.lastInteraction.setToNow();
            return tokenData.user;
        }

        if (roleTitle.equals(AdminUserRole.ADMIN.toString())) {
            if (allowed[0].toString().equals(AdminUserRole.ROOT.toString())) {
                throw new AuthException(VantarKey.NO_ACCESS);
            }
            tokenData.lastInteraction.setToNow();
            return tokenData.user;
        }

        for (CommonUserRole access : allowed) {
            if (tokenData.user.getRole().equals(access)) {
                tokenData.lastInteraction.setToNow();
                return tokenData.user;
            }
        }

        if (debug && dummyUser != null) {
            limitAccess(dummyUser.getRole(), allowed);
            return dummyUser;
        }

        throw new AuthException(VantarKey.NO_ACCESS);
    }

    public synchronized CommonUser permitAccess(TokenData tokenData, String... allowed) throws AuthException {
        CommonUserRole role = tokenData.user.getRole();
        String roleTitle = role.toString();

        if (allowed.length == 0 || roleTitle.equals(AdminUserRole.ROOT.toString())) {
            tokenData.lastInteraction.setToNow();
            return tokenData.user;
        }

        if (roleTitle.equals(AdminUserRole.ADMIN.toString())) {
            if (allowed[0].toString().equals(AdminUserRole.ROOT.toString())) {
                throw new AuthException(VantarKey.NO_ACCESS);
            }
            tokenData.lastInteraction.setToNow();
            return tokenData.user;
        }

        String roleStr = tokenData.user.getRole().toString();
        for (String access : allowed) {
            if (roleStr.equals(access)) {
                tokenData.lastInteraction.setToNow();
                return tokenData.user;
            }
        }

        if (debug && dummyUser != null) {
            limitAccess(dummyUser.getRole(), allowed);
            return dummyUser;
        }

        throw new AuthException(VantarKey.NO_ACCESS);
    }

    private synchronized void limitAccess(CommonUserRole role, CommonUserRole... allowed) throws AuthException {
        if (role.equals(AdminUserRole.ROOT) || role.equals(AdminUserRole.ADMIN)) {
            return;
        }

        for (CommonUserRole access : allowed) {
            if (role.equals(access)) {
                return;
            }
        }

        throw new AuthException(VantarKey.NO_ACCESS);
    }

    private synchronized void limitAccess(CommonUserRole role, String... allowed) throws AuthException {
        if (role.equals(AdminUserRole.ROOT) || role.equals(AdminUserRole.ADMIN)) {
            return;
        }

        String roleStr = role.toString();
        for (String access : allowed) {
            if (roleStr.equals(access)) {
                return;
            }
        }

        throw new AuthException(VantarKey.NO_ACCESS);
    }

    // > > > verify email/mobile

    public String getVerifyToken(CommonUser user, TokenDataType type) {
        user.setToken(StringUtil.getRandomString(DEFAULT_VERIFY_TOKEN_LENGTH));
        verifyTokens.put(user.getToken(), new TokenData(user, type));
        return user.getToken();
    }

    public boolean verifyTokenExists(CommonUser user, TokenDataType type) {
        TokenData tokenData = verifyTokens.get(user.getToken());

        if (tokenData == null) {
            return false;
        }

        if (-tokenData.lastInteraction.secondsFromNow() > (tokenExpireMin * 60)) {
            oneTimeTokens.remove(user.getToken());
            return false;
        }

        if (!type.equals(tokenData.type)) {
            return false;
        }

        oneTimeTokens.remove(user.getToken());
        return true;
    }

    public ServiceAuth startupSignin(CommonUser temporaryRoot) {
        startupAuthToken = makeUserOnline(new TokenData(temporaryRoot));
        return this;
    }

    private TokenData validateToken(Params params, Map<String, TokenData> map) throws AuthException {
        String token = params.getHeader(VantarParam.HEADER_AUTH_TOKEN);
        if (StringUtil.isEmpty(token)) {
            token = params.getString(VantarParam.AUTH_TOKEN);
        }
        if (StringUtil.isEmpty(token)) {
            token = startupAuthToken;
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

    private String makeUserOnline(TokenData info) {
        while (true) {
            String token = DigestUtils.sha1Hex(info.user.getPassword() + info.user.getFullName() + Math.random());
            if (onlineUsers.containsKey(token)) {
                continue;
            }

            onlineUsers.forEach((t, u) -> {
                if (u.user.getId().equals(info.user.getId())) {
                    onlineUsers.remove(u.user.getToken());
                }
            });
            onlineUsers.put(token, info);
            info.user.setToken(token);
            return token;
        }
    }

    private synchronized void removeItem(CommonUser user, Map<String, TokenData> map) {
        map.forEach((token, u) -> {
            if ((user.getMobile() != null && user.getMobile().equals(u.user.getMobile()))
                || (user.getUsername() != null && user.getUsername().equals(u.user.getUsername()))
                || (user.getEmail() != null && user.getEmail().equals(u.user.getEmail()))) {

                oneTimeTokens.remove(token);
            }
        });
    }

    private void validateTokens() {
        startupAuthToken = null;
        onlineUsers.forEach((key, value) -> {
            if (-value.lastInteraction.secondsFromNow() > (tokenExpireMin * 60)) {
                onlineUsers.remove(key);
            }
        });
        signupVerifyTokens.forEach((key, value) -> {
            if (-value.lastInteraction.secondsFromNow() > (tokenExpireMin * 60)) {
                onlineUsers.remove(key);
            }
        });
        oneTimeTokens.forEach((key, value) -> {
            if (-value.lastInteraction.secondsFromNow() > (tokenExpireMin * 60)) {
                oneTimeTokens.remove(key);
            }
        });
    }

    public static CommonUser getCurrentSignedInUser(Params params) throws NoContentException, ServiceException {
        ServiceAuth auth = Services.get(ServiceAuth.class);
        if (auth == null) {
            throw new ServiceException(ServiceAuth.class);
        }
        CommonUser user = auth.getCurrentUser(params);
        if (user == null) {
            throw new NoContentException();
        }
        return user;
    }

    public CommonUser getCurrentUser(Params params) {
        if (debug && dummyUser != null) {
            return dummyUser;
        }

        TokenData tokenData;
        try {
            tokenData = validateToken(params, onlineUsers);
        } catch (AuthException e) {
            return null;
        }
        return tokenData.user;
    }

    public Map<String, TokenData> getOnlineUsers() {
        return onlineUsers;
    }

    public Map<String, TokenData> getSignupVerifyTokens() {
        return signupVerifyTokens;
    }

    public Map<String, TokenData> getOneTimeTokens() {
        return oneTimeTokens;
    }

    public Map<String, TokenData> getVerifyTokens() {
        return verifyTokens;
    }

    public synchronized void removeToken(String token) {
        onlineUsers.remove(token);
        signupVerifyTokens.remove(token);
        oneTimeTokens.remove(token);
        verifyTokens.remove(token);
    }

    public ServiceAuth setDummyUser(CommonUser dummyUser) {
        this.dummyUser = dummyUser;
        return this;
    }

    public CommonUser getDummyUser() {
        return dummyUser;
    }


    public static class TokenData {

        public CommonUser user;
        public DateTime lastInteraction;
        public TokenDataType type;

        public TokenData(CommonUser user) {
            this.user = user;
            lastInteraction = new DateTime();
        }

        public TokenData(CommonUser user, TokenDataType type) {
            this.user = user;
            lastInteraction = new DateTime();
            this.type = type;
        }

        public String toString() {
            return ObjectUtil.toString(this);
        }
    }


    public interface Event {

        CommonUser getUser(String username) throws NoContentException, DatabaseException;
    }


    public enum  TokenDataType {
        VERIFY_EMAIL,
        VERIFY_MOBILE,
    }
}