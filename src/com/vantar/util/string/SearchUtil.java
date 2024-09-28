package com.vantar.util.string;

import com.vantar.common.VantarParam;
import com.vantar.locale.*;

/**
 * String search utilities
 */
public class SearchUtil {

    public static String normalizeKeywords(String string) {
        if (StringUtil.isEmpty(string)) {
            return "";
        }

        String separator = null;
        for (String item : new String[] {",", "،", "+", "|",}) {
            if (StringUtil.contains(string, item)) {
                separator = item;
                break;
            }
        }

        if (separator == null) {
            return string.trim().replaceAll("\\s{2,}", " ");
        }

        return StringUtil.replace(string.replaceAll("\\s+", ""), separator, " ").trim();
    }

    public static String normalizeFullText(String string, String lang) {
        if (lang == null) {
            lang = Locale.getDefaultLocale();
        }
        String[] stopWords = StopWord.get(lang);

        StringBuilder sb = new StringBuilder(string.length());
        for (String word : StringUtil.split(string.replaceAll("\\s{2,}", " "), ' ')) {
            int length = word.length();
            if (length == 1) {
                continue;
            }

            boolean accept = true;

            for (String stopWord : stopWords) {
                if (stopWord.startsWith("{excluderange")) {
                    String[] parts = StringUtil.splitTrim(StringUtil.remove(stopWord, "{excluderange", "}", "(", ")"), VantarParam.SEPARATOR_COMMON);
                    if (parts.length != 2) {
                        continue;
                    }
                    int low = StringUtil.toInteger(parts[0]);
                    int high = StringUtil.toInteger(parts[1]);
                    for (int i = 0; i < length; ++i) {
                        int c = word.charAt(i);
                        if (c >= low && c <= high) {
                            accept = false;
                            break;
                        }
                    }

                } else if (stopWord.startsWith("{includerange")) {
                    String[] items = StringUtil.splitTrim(StringUtil.remove(stopWord, "{includerange", "}"), VantarParam.SEPARATOR_BLOCK);
                    MinMax[] minMaxes = new MinMax[items.length];
                    int i = 0;
                    for (String item : items) {
                        String[] parts = StringUtil.splitTrim(StringUtil.remove(item, ')', '('), VantarParam.SEPARATOR_COMMON);
                        if (parts.length != 2) {
                            continue;
                        }
                        minMaxes[i++] = new MinMax(StringUtil.toInteger(parts[0]), StringUtil.toInteger(parts[1]));
                    }
                    for (i = 0; i < length; ++i) {
                        int c = word.charAt(i);
                        for (MinMax minMax : minMaxes) {
                            accept = accept || minMax.inRange(c);
                        }
                        if (!accept) {
                            break;
                        }
                    }

                } else if (stopWord.equalsIgnoreCase(word)) {
                    accept = false;
                    break;
                }
            }

            if (accept) {
                sb.append(word).append(" ");
            }
        }

        return sb.toString();
    }

    public static String getSearchableEn(String string) {
        string = StringUtil.replace(string.toLowerCase(), new String[] {"gh", "kh"}, new String[] {"q", "x"});
        int l = string.length();
        StringBuilder sb = new StringBuilder(l);
        for (int i=0; i<l; ++i) {
            char c = string.charAt(i);
            if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y' || c == '\n' || c == ' ' || c == '\t') {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }


    private static class MinMax {

        public int min;
        public int max;

        public MinMax() {

        }

        public MinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public boolean inRange(int value) {
            return value >= min && value <= max;
        }
    }
}
