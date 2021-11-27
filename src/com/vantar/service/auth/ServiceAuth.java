package com.vantar.service.auth;

import com.vantar.common.VantarParam;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.util.file.FileUtil;
import com.vantar.util.json.Json;
import com.vantar.util.string.StringUtil;
import com.vantar.web.Params;
import com.vantar.web.dto.Permission;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.*;


public class ServiceAuth extends Permit implements Services.Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceAuth.class);

    protected static final int MAX_VERIFY_TOKENS = 20;
    protected static final int DEFAULT_VERIFY_TOKEN_LENGTH = 5;
    protected static final boolean DEFAULT_VERIFY_TOKEN_NUMBER_ONLY = true;
    protected static final int MAX_SIGNED_USERS = 500;
    protected static String startupAuthToken;

    private ScheduledExecutorService schedule;
    private Event event;
    private final Map<String, TokenData> signupVerifyTokens = new ConcurrentHashMap<>(MAX_VERIFY_TOKENS);
    private final Map<String, TokenData> oneTimeTokens = new ConcurrentHashMap<>(MAX_VERIFY_TOKENS);
    private final Map<String, TokenData> verifyTokens = new ConcurrentHashMap<>(MAX_VERIFY_TOKENS);

    public Boolean onEndSetNull;
    /* parent class: public Integer tokenExpireMin; */
    public Integer tokenCheckIntervalMin;
    public String tokenStorePath;
    public int signUpVerifyTokenLength = DEFAULT_VERIFY_TOKEN_LENGTH;
    public int signInVerifyTokenLength = DEFAULT_VERIFY_TOKEN_LENGTH;
    public boolean signUpVerifyTokenNumberOnly = DEFAULT_VERIFY_TOKEN_NUMBER_ONLY;
    public boolean signInVerifyTokenNumberOnly = DEFAULT_VERIFY_TOKEN_NUMBER_ONLY;

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
            log.info("> > > Auth-tokens are backed-up.");
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
            log.info("> > > Backed-up auth-tokens are loaded.");
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
        if (u != null) {
            u.nullPassword();
        }
        return u;
    }

    // > > > signout

    public synchronized void signout(Params params) {
        String token = params.getHeader(VantarParam.HEADER_AUTH_TOKEN);
        if (token != null) {
            onlineUsers.remove(token);
        }
    }


    // > > > verify email/mobile

    public String getVerifyToken(CommonUser user, TokenData.Type type) {
        user.setToken(StringUtil.getRandomString(DEFAULT_VERIFY_TOKEN_LENGTH));
        verifyTokens.put(user.getToken(), new TokenData(user, type));
        return user.getToken();
    }

    public boolean verifyTokenExists(CommonUser user, TokenData.Type type) {
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
        try {
            return validateToken(params, onlineUsers).user;
        } catch (AuthException e) {
            return null;
        }
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


    public interface Event {

        CommonUser getUser(String username) throws NoContentException, DatabaseException;
    }
}