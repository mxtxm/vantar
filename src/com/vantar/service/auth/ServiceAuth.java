package com.vantar.service.auth;

import com.vantar.common.VantarParam;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.util.file.FileUtil;
import com.vantar.util.json.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.*;


public class ServiceAuth extends Permit implements Services.Service {

    protected static final int MAX_VERIFY_TOKENS = 20;
    protected static final int DEFAULT_VERIFY_TOKEN_LENGTH = 5;
    protected static final boolean DEFAULT_VERIFY_TOKEN_NUMBER_ONLY = true;
    protected static final int MAX_SIGNED_USERS = 500;
    protected static String startupAuthToken;

    private static final Logger log = LoggerFactory.getLogger(ServiceAuth.class);
    private static final String AUTH_BACKUP_FILENAME = "auth.backup";
    private static Class<? extends CommonUser> userClass;

    private ScheduledExecutorService schedule;
    private Event event;
    private final Map<String, TokenData> signupVerifyTokens = new ConcurrentHashMap<>(MAX_VERIFY_TOKENS);
    private final Map<String, TokenData> oneTimeTokens = new ConcurrentHashMap<>(MAX_VERIFY_TOKENS);
    private final Map<String, TokenData> verifyTokens = new ConcurrentHashMap<>(MAX_VERIFY_TOKENS);

    public Boolean onEndSetNull;
    /* parent class: public Integer tokenExpireMin; */
    public Integer tokenCheckIntervalMin;
    public String backupPath;
    public int signUpVerifyTokenLength = DEFAULT_VERIFY_TOKEN_LENGTH;
    public int signInVerifyTokenLength = DEFAULT_VERIFY_TOKEN_LENGTH;
    public boolean signUpVerifyTokenNumberOnly = DEFAULT_VERIFY_TOKEN_NUMBER_ONLY;
    public boolean signInVerifyTokenNumberOnly = DEFAULT_VERIFY_TOKEN_NUMBER_ONLY;


    public void start() {
        schedule = Executors.newSingleThreadScheduledExecutor();
        schedule.scheduleWithFixedDelay(this::validateTokens, tokenCheckIntervalMin, tokenCheckIntervalMin, TimeUnit.MINUTES);
    }

    public void stop() {
        if (backupPath != null) {
            Jackson json = Json.getWithProtected();
            FileUtil.write(
                backupPath + AUTH_BACKUP_FILENAME,
                json.toJson(onlineUsers) + VantarParam.SEPARATOR_BLOCK_COMPLEX +
                json.toJson(signupVerifyTokens) + VantarParam.SEPARATOR_BLOCK_COMPLEX +
                json.toJson(oneTimeTokens) + VantarParam.SEPARATOR_BLOCK_COMPLEX +
                json.toJson(verifyTokens)
            );
            log.info("> > > auth-data backed-up");
        }
        schedule.shutdown();
    }

    public static void setUserClass(Class<? extends CommonUser> uc) {
        userClass = uc;
    }

    public static Class<? extends CommonUser> getUserClass() {
        return userClass;
    }

    /**
     * when users cache or db is updated > update users here
     */
    public ServiceAuth updateOnlineUsers(List<CommonUser> users) {
        for (CommonUser u : users) {
            updateOnlineUser(u);
        }
        return this;
    }

    public ServiceAuth updateOnlineUser(CommonUser user) {
        for (TokenData t : onlineUsers.values()) {
            if (t.user.getId().equals(user.getId())) {
                t.user.set(user);
            }
        }
        return this;
    }

    public ServiceAuth restoreFromBackup() {
        if (backupPath == null || !FileUtil.exists(backupPath + AUTH_BACKUP_FILENAME)) {
            return this;
        }

        String contents = FileUtil.getFileContent(backupPath + AUTH_BACKUP_FILENAME);
        if (StringUtil.isEmpty(contents)) {
            log.warn(" ! auth-data NOT restored (no backup)");
            return this;
        }

        String[] parts = StringUtil.split(contents, VantarParam.SEPARATOR_BLOCK_COMPLEX);
        if (parts.length != 4) {
            log.warn(" ! auth-users NOT restored corrupted data)");
            return this;
        }

        Jackson json = Json.getWithProtected();
        json.addPolymorphism(CommonUser.class, userClass);

        if (StringUtil.isNotEmpty(parts[0])) {
            Map<String, TokenData> x = json.mapFromJson(parts[0], String.class, TokenData.class);
            if (x != null) {
                onlineUsers.putAll(x);
            }
        }
        if (StringUtil.isNotEmpty(parts[1])) {
            Map<String, TokenData> x = json.mapFromJson(parts[1], String.class, TokenData.class);
            if (x != null) {
                signupVerifyTokens.putAll(x);
            }
        }
        if (StringUtil.isNotEmpty(parts[2])) {
            Map<String, TokenData> x = json.mapFromJson(parts[2], String.class, TokenData.class);
            if (x != null) {
                oneTimeTokens.putAll(x);
            }
        }
        if (StringUtil.isNotEmpty(parts[3])) {
            Map<String, TokenData> x = json.mapFromJson(parts[3], String.class, TokenData.class);
            if (x != null) {
                verifyTokens.putAll(x);
            }
        }
        log.info("> > > auth-data restored");
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

    public synchronized CommonUser signin(Params params) throws ServerException, AuthException {
        String username = params.getString(VantarParam.USER_NAME);
        String password = params.getString(VantarParam.PASSWORD);
        if (StringUtil.isEmpty(username) || StringUtil.isEmpty(password)) {
            throw new AuthException(VantarKey.USER_PASSWORD_EMPTY);
        }

        TokenData tokenData = oneTimeTokens.get(password);
        if (tokenData != null) {
            tokenData = new TokenData(tokenData.user);
            oneTimeTokens.remove(password);
            makeUserOnline(tokenData);
            return tokenData.user;
        }

        if (event == null) {
            throw new AuthException(VantarKey.USER_REPO_NOT_SET);
        }
        SigninBundle signinBundle;
        try {
            signinBundle = event.getUserPassword(username);
        } catch (NoContentException e) {
            throw new AuthException(VantarKey.USER_NOT_EXISTS);
        } catch (Exception e) {
            log.error("! failed to get signinBundle", e);
            throw new ServerException(VantarKey.FETCH_FAIL);
        }

        if (signinBundle.commonUser.getAccessStatus().equals(AccessStatus.DISABLED)) {
            throw new AuthException(VantarKey.USER_DISABLED);
        }
        if (signinBundle.commonUser.getAccessStatus().equals(AccessStatus.UNSUBSCRIBED)) {
            throw new AuthException(VantarKey.USER_NOT_EXISTS);
        }
        if (!signinBundle.commonUserPassword.passwordEquals(password)) {
            throw new AuthException(VantarKey.WRONG_PASSWORD);
        }

        tokenData = new TokenData(signinBundle.commonUser);
        makeUserOnline(tokenData);

        return signinBundle.commonUser;
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

    private synchronized String makeUserOnline(TokenData info) {
        while (true) {
            String token = DigestUtils.sha1Hex(info.user.getId() + info.user.getFullName() + Math.random());
            if (onlineUsers.containsKey(token)) {
                continue;
            }

            try {
                List<String> tokensToDelete = new ArrayList<>(5);
                for (Map.Entry<String, TokenData> entry : onlineUsers.entrySet()) {
                    if (entry.getValue().user.getId().equals(info.user.getId())) {
                        tokensToDelete.add(entry.getKey());
                    }
                }
                for (String tokenToDelete : tokensToDelete) {
                    onlineUsers.remove(tokenToDelete);
                }
            } catch (Exception e) {
                log.error("!  token={} online={}\n", info, onlineUsers);
            }

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

    public static CommonUser getCurrentSignedInUser(Params params) throws ServiceException, AuthException {
        return Services.get(ServiceAuth.class).getCurrentUser(params);
    }

    public CommonUser getCurrentUser(Params params) throws AuthException {
        return validateToken(params, onlineUsers).user;
    }

    public static void assumeSignedIn(Params params) throws ServiceException, AuthException {
        Services.get(ServiceAuth.class).getCurrentUser(params);
    }

    public void isSignedIn(Params params) throws AuthException {
        validateToken(params, onlineUsers);
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

        SigninBundle getUserPassword(String username) throws NoContentException, DatabaseException;
    }


    public static class SigninBundle {

        public CommonUser commonUser;
        public CommonUserPassword commonUserPassword;
    }
}