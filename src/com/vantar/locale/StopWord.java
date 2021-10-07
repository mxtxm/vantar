package com.vantar.locale;

import com.vantar.util.file.FileUtil;
import com.vantar.util.string.StringUtil;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class StopWord {

    private static final Logger log = LoggerFactory.getLogger(StopWord.class);
    private static final String COMMENT_INDICATOR = "###";
    private static Map<String, String[]> stopWords;


    public static String[] get(String lang) {
        if (stopWords == null) {
            stopWords = new ConcurrentHashMap<>();
        }

        String[] words;
        try {
            words = stopWords.get(lang);
        } catch (Exception e) {
            log.warn("! StopWord.get {}", lang, e);
            return new String[] {};
        }

        if (words == null) {
            words = loadFromFile(lang);
            stopWords.put(lang, words);
        }
        return words;
    }

    private static synchronized String[] loadFromFile(String lang) {
        Set<String> set;
        if (Locale.stopWordPath == null) {
            String path = "/data/stopword/" + lang;
            if (!FileUtil.resourceExists(path)) {
                log.warn("-----> stopword file not exists ({})", path);
                return new String[] {};
            }
            set = StringUtil.splitToSet(FileUtil.getFileContentFromClassPath(path), '\n');
        } else {
            set = StringUtil.splitToSet(FileUtil.getFileContent(Locale.stopWordPath + lang), '\n');
        }

        set.removeIf(element -> element.startsWith(COMMENT_INDICATOR) && element.endsWith(COMMENT_INDICATOR));

        log.debug("-----> stopword file loaded {}", lang);
        return set.toArray(new String[0]);
    }

    public static void remove(String lang) {
        if (stopWords != null) {
            stopWords.remove(lang);
        }
    }
}
