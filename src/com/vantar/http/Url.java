package com.vantar.http;

import com.vantar.util.string.StringUtil;
import java.io.UnsupportedEncodingException;
import java.net.*;


public class Url {

    public static String removeUnsafeChars(String string) {
        return StringUtil.replace(
            string,
            new char[]   {'|',   '<',   '>',   ' ', ' ',         '"',   '{',   '}',   '[',   ']',   '\\',  '^',   '`',   '~'},
            new String[] {"%7C", "%3C", "%3E", "+", "%E2%80%8A", "%22", "%7B", "%7D", "%5B", "%5D", "%5C", "%5E", "%60", "%7E"}
        );
    }

    public static String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return string;
        }
    }

    public static boolean isValid(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        try {
            URL u = new URL(url);
            u.toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
        return true;
    }

    public static String getHost(String url) {
        try {
            URI u = new URI(url);
            return u.getHost();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static String fix(String url) {
        String urlLower = url.toLowerCase();
        if (!urlLower.startsWith("http://") && !urlLower.startsWith("https://")) {
            url = "http://" + url;
        }
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }

    /**
     * returns null for opaque urls such as mailto:something.com
     */
    public static String getRelative(String url, String baseUrl) {
        return getRelative(url, baseUrl, null, null);
    }

    public static String getRelative(String url, String baseUrl, String scheme, String currentUrl) {
        if (url == null || baseUrl == null) {
            return null;
        }

        url = StringUtil.replace(url.trim(), " ", "%20");
        if (url.isEmpty()) {
            return null;
        }

        URI uri;
        try {
            uri = new URI(url);
            // e.g. mailto:something.com
            if (uri.isOpaque()) {
                return null;
            }
            if (uri.isAbsolute()) {
                return url;
            }
        } catch (URISyntaxException e) {
            // pass
        }

        char first = url.charAt(0);
        if (first == '#') {
            return (currentUrl == null ? baseUrl : currentUrl) + url;
        }

        if (first == '.') {
            // relative to current path ./something means current-path/something
            url = url.substring(1);
            if (!url.isEmpty() && url.charAt(0) == '/') {
                url = url.substring(1);
            }
        } else if (first == '/') {
            if (url.length() > 1 && url.charAt(1) == '/') {
                // e.g. //something.com/path
                if (scheme == null) {
                    scheme = baseUrl.substring(0, baseUrl.indexOf(":"));
                }
                return scheme + ":" + url;
            } else {
                // e.g. /?something=value or /path
                return baseUrl + url.substring(1);
            }
        }

        // relative to current page URL e.g. ?something=something or path/path
        if (currentUrl == null) {
            currentUrl = baseUrl;
        }
        if (!currentUrl.isEmpty() && currentUrl.charAt(currentUrl.length() - 1) != '/') {
            currentUrl += '/';
        }
        return currentUrl + url;
    }


    public static class Domain {

        public final String domain;
        public final String strippedUrl;


        public Domain(String domain, String strippedUrl) {
            this.domain = domain;
            this.strippedUrl = strippedUrl;
        }

        public boolean equalDomains(String url) {
            return url.toLowerCase().contains(domain.toLowerCase());
        }

        public static Domain getDomain(String url) {
            String[] parts = StringUtil.split(StringUtil.remove(url, "http://", "https://", "www."), '/');
            if (parts.length == 0) {
                return new Domain(null, null);
            }
            return new Domain(parts[0], parts.length == 1 ? parts[0] : (parts[0] + '/' + parts[1]));
        }
    }
}