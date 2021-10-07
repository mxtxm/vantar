package com.vantar.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class HttpConfig {

    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:31.0) Gecko/20100101 Firefox/31.0 Iceweasel/31.4.0";
    public static final boolean DEFAULT_IGNORE_SSL_CERT = true;
    public static final int DEFAULT_TIMEOUT_SEC = 60;
    public static final int DEFAULT_MAX_REDIRECTS = 10;

    public Map<String, String> headers = new HashMap<>();
    public List<String> userAgents;
    public int timeoutSec = DEFAULT_TIMEOUT_SEC;
    public boolean ignoreSsl = DEFAULT_IGNORE_SSL_CERT;
    public int maxRedirects = DEFAULT_MAX_REDIRECTS;

    private final Random randomGenerator = new Random();


    public HttpConfig() {
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Accept-Encoding", "gzip");
    }

    public String getUseragent() {
        if (userAgents == null) {
            return DEFAULT_USER_AGENT;
        }
        int index = randomGenerator.nextInt(userAgents.size());
        return userAgents.get(index);
    }
}
