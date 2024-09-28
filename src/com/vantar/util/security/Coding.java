package com.vantar.util.security;

import com.vantar.util.string.StringUtil;
import org.apache.commons.net.util.Base64;
import org.slf4j.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;


public class Coding {

    private static final Logger log = LoggerFactory.getLogger(Coding.class);

    private static final int ITERATIONS = 1;
    private static final int saltLen = 32;
    private static final int desiredKeyLen = 256;


    public static String encode(String password) {
        if (StringUtil.isEmpty(password)) {
            return null;
        }

        try {
            byte[] salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(saltLen);
            return Base64.encodeBase64String(salt) + "$" + hash(password, salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error(" !! password({})\n", password, e);
            return null;
        }
    }

    public static boolean equals(String password, String stored) {
        if (StringUtil.isEmpty(password) || StringUtil.isEmpty(stored)) {
            return false;
        }

        String[] saltAndHash = stored.split("\\$");
        if (saltAndHash.length != 2) {
            return false;
        }
        try {
            return hash(password, Base64.decodeBase64(saltAndHash[0])).equals(saltAndHash[1]);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error(" !! equals({}, {})\n", password, stored, e);
            return false;
        }
    }

    public static String md5(String text) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(text.getBytes());
            BigInteger bigInt = new BigInteger(1, md5.digest());
            StringBuilder hashtext = new StringBuilder(bigInt.toString(16));
            while (hashtext.length() < 32) {
                hashtext.insert(0, "0");
            }
            return hashtext.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error(" !! md5 failed ({})\n", text, e);
            return "";
        }
    }

    private static String hash(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return Base64.encodeBase64String(
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, desiredKeyLen))
                .getEncoded()
        );
    }
}
