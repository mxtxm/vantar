package com.vantar.locale;

import com.vantar.common.VantarParam;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.string.*;
import com.vantar.web.Params;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Locale {

    protected static String stopWordPath;
    private static String defaultLocale;
    private static Map<String, Translation> langTokens;
    private static Map<Long, String> threadLangs;


    public static void start(Map<String, Translation> langTokens, String defaultLocale, String stopWordPath) {
        Locale.langTokens = langTokens;
        Locale.defaultLocale = defaultLocale;
        threadLangs = new ConcurrentHashMap<>(30);
        if (stopWordPath != null) {
            StringUtil.rtrim(stopWordPath, '/');
        }
        Locale.stopWordPath = stopWordPath;
    }

    public static void stop() {
        langTokens = null;
        defaultLocale = null;
        threadLangs = null;
        stopWordPath = null;
    }

    public static void removeThreadLocale(long id) {
        if (threadLangs != null) {
            threadLangs.remove(id);
        }
    }

    public static String getDefaultLocale() {
        return defaultLocale;
    }

    public static String getSelectedLocale() {
        if (threadLangs == null) {
            return null;
        }
        return threadLangs.get(Thread.currentThread().getId() + Params.serverUpCount);
    }

    public static void setSelectedLocale(Params params) {
        if (threadLangs == null) {
            return;
        }

        String locale = params.getHeader(VantarParam.HEADER_LANG);
        if (locale != null) {
            threadLangs.put(Thread.currentThread().getId() + Params.serverUpCount, locale);
            return;
        }

        String contentType = params.getHeader("content-type");
        if (contentType == null || !StringUtil.contains(contentType, "json")) {
            threadLangs.put(Thread.currentThread().getId() + Params.serverUpCount, params.getString(VantarParam.LANG, defaultLocale));
            return;
        }

        try {
            String langJson = params.extractFromJson(VantarParam.LANG, String.class);
            threadLangs.put(Thread.currentThread().getId() + Params.serverUpCount, langJson == null ? defaultLocale : langJson);
            return;
        } catch (Exception ignore) {

        }

        threadLangs.put(Thread.currentThread().getId() + Params.serverUpCount, defaultLocale);
    }

    public static Set<String> getLangs() {
        if (threadLangs == null) {
            return null;
        }
        return langTokens.keySet();
    }

    public static String getString(LangKey key, Object... messageParams) {
        return getString(threadLangs == null ? null
            : threadLangs.get(Thread.currentThread().getId() + Params.serverUpCount), key, messageParams);
    }

    public static String getString(String lang, LangKey key, Object... messageParams) {
        if (threadLangs == null) {
            return null;
        }
        Translation translation = lang == null ? null : langTokens.get(lang);
        if (translation == null) {
            translation = langTokens.get(defaultLocale);
        }

        String value = translation.getString(key);
        if (value == null && !defaultLocale.equals(translation.getLangKey())) {
            value = langTokens.get(defaultLocale).getString(key);
        }
        if (value == null) {
            value = "fa".equals(lang) ? DefaultStringsFa.getString(key) : DefaultStringsEn.getString(key);
        }
        if (value == null) {
            return key.toString();
        }

        try {
            return MessageFormat.format(value, messageParams);
        } catch (Exception e) {
            ServiceLog.error(" ! can not replace params in text ('{}', {})\n", value, CollectionUtil.join(messageParams, ", "), e);
            return "!!!!";
        }
    }

    public static String getString(String key, Object... messageParams) {
        return getString(threadLangs.get(Thread.currentThread().getId() + Params.serverUpCount), key, messageParams);
    }

    public static String getString(String lang, String key, Object... messageParams) {
        if (threadLangs == null) {
            return null;
        }
        Translation translation = lang == null ? null : langTokens.get(lang);
        if (translation == null) {
            translation = langTokens.get(defaultLocale);
        }

        String value = translation.getString(key);
        if (value == null && !defaultLocale.equals(translation.getLangKey())) {
            value = langTokens.get(defaultLocale).getString(key);
        }
        if (value == null) {
            return key;
        }

        try {
            return MessageFormat.format(value, messageParams);
        } catch (Exception e) {
            ServiceLog.error(" ! can not replace params in text ('{}', {})\n", value, CollectionUtil.join(messageParams, ", "), e);
            return "!!!!";
        }
    }
}
