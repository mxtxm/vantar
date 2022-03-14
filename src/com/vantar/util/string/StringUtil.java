package com.vantar.util.string;

import com.vantar.common.VantarParam;
import com.vantar.database.datatype.Location;
import com.vantar.database.dto.*;
import com.vantar.exception.DateTimeException;
import com.vantar.locale.Locale;
import com.vantar.locale.StopWord;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.json.*;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import java.util.*;
import java.util.regex.Pattern;


public class StringUtil {

    private static final Pattern PATTERN_SNAKE_CASE = Pattern.compile("(.)(\\p{Upper})");
    private static final int INDEX_NOT_FOUND = -1;


    public static boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

    public static boolean isEmpty(String... values) {
        if (CollectionUtil.isEmpty(values)) {
            return true;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    public static Long scrapeLong(String string) {
        if (string == null) {
            return null;
        }
        string = Persian.Number.toLatin(string).trim();
        return toLong(Persian.Number.toLatin(string).trim().replaceAll("[^0-9]",""));
    }

    public static Integer scrapeInt(String string) {
        if (string == null) {
            return null;
        }
        return toInteger(Persian.Number.toLatin(string).trim().replaceAll("[^0-9]",""));
    }

    public static Double scrapeDouble(String string) {
        if (string == null) {
            return null;
        }
        return toDouble(Persian.Number.toLatin(string).trim().replaceAll("[^0-9]",""));
    }

    public static Integer toInteger(String string) {
        if (string == null) {
            return null;
        }
        try {
            return Integer.parseInt(string.trim());
        } catch (NumberFormatException e) {
            string = Persian.Number.toLatin(string).trim();
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException f) {
                Double d = toDouble(string);
                return d == null ? null : d.intValue();
            }
        }
    }

    public static Long toLong(String string) {
        if (string == null) {
            return null;
        }
        try {
            return Long.parseLong(string.trim());
        } catch (NumberFormatException e) {
            string = Persian.Number.toLatin(string).trim();
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException f) {
                Double d = toDouble(string);
                return d == null ? null : d.longValue();
            }
        }
    }

    public static Double toDouble(String string) {
        if (string == null) {
            return null;
        }
        try {
            return Double.parseDouble(string.trim());
        } catch (NumberFormatException e) {
            try {
                return Double.parseDouble(Persian.Number.toLatin(string).trim());
            } catch (NumberFormatException f) {
                return null;
            }
        }
    }

    public static Float toFloat(String string) {
        if (string == null) {
            return null;
        }
        try {
            return Float.parseFloat(string.trim());
        } catch (NumberFormatException e) {
            try {
                return Float.parseFloat(Persian.Number.toLatin(string).trim());
            } catch (NumberFormatException f) {
                return null;
            }
        }
    }

    public static Byte toByte(String string) {
        if (string == null) {
            return null;
        }
        try {
            return Byte.parseByte(string.trim());
        } catch (NumberFormatException e) {
            try {
                return Byte.parseByte(Persian.Number.toLatin(string).trim());
            } catch (NumberFormatException f) {
                return null;
            }
        }
    }

    public static Boolean toBoolean(String value) {
        if (isEmpty(value)) {
            return null;
        }
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }

        if (value.equals("1") || value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")
            || value.equalsIgnoreCase("بله") || value.equalsIgnoreCase("بلی") || value.equalsIgnoreCase("on")) {
            return true;
        }
        if (value.equals("0") || value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no")
            || value.equalsIgnoreCase("نه") || value.equalsIgnoreCase("خیر") || value.equalsIgnoreCase("off")) {
            return false;
        }
        return null;
    }

    public static Character toCharacter(String value) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        return value.charAt(0);
    }

    public static Long toTimestamp(String value) {
        try {
            return new DateTime(value).getAsTimestamp();
        } catch (DateTimeException e) {
            return null;
        }
    }

    public static Object toObject(String value, Class<?> typeClass) {
        if (typeClass.equals(String.class)) {
            return value;
        }
        if (ClassUtil.extendsClass(typeClass, Number.class)) {
            return NumberUtil.toNumber(value, typeClass);
        }
        if (typeClass.equals(Boolean.class)) {
            return StringUtil.toBoolean(value);
        }
        if (typeClass.equals(Character.class)) {
            return StringUtil.toCharacter(value);
        }
        if (typeClass.equals(Location.class)) {
            return new Location(value);
        }
        if (typeClass.equals(DateTime.class)) {
            try {
                return new DateTime(value);
            } catch (DateTimeException ignore) {
                return null;
            }
        }
        if (typeClass.isEnum()) {
            return EnumUtil.getEnumValue(value, typeClass);
        }
        if (ClassUtil.extendsClass(typeClass, Dto.class)) {
            return Json.d.fromJson(value, typeClass);
        }
        return null;
    }

    public static String toCamelCase(String string) {
        return toCamelCase(string, false);
    }

    public static String toStudlyCase(String string) {
        return toCamelCase(string, true);
    }

    private static String toCamelCase(String string, boolean capitalize) {
        if (string == null) {
            return null;
        }
        string = StringUtil.trim(string, '/', '_', '.', '_', '/');

        StringBuilder stringBuilder = new StringBuilder();
        char value;
        String input = string.toLowerCase();

        for (int i = 0; i < input.length(); ++i) {
            value = input.charAt(i);
            if (value == '_' || value == '.' || value == '-' || value == '/') {
                capitalize = true;
            } else if (capitalize) {
                stringBuilder.append(Character.toUpperCase(value));
                capitalize = false;
            } else {
                stringBuilder.append(value);
            }
        }

        return stringBuilder.toString();
    }

    public static String toSnakeCase(String string) {
        return string == null ? null : PATTERN_SNAKE_CASE.matcher(string).replaceAll("$1_$2").toLowerCase();
    }

    public static String toKababCase(String string) {
        return string == null ? null : PATTERN_SNAKE_CASE.matcher(string).replaceAll("$1-$2").toLowerCase();
    }

    public static String getSearchableEn(String string) {
        string = replace(string.toLowerCase(), new String[] {"gh", "kh"}, new String[] {"q", "x"});
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

    public static Set<String> getRandomStrings(int count) {
        Set<String> codes = new HashSet<>();
        while (codes.size() < count) {
            codes.add(split(UUID.randomUUID().toString(), '-')[0]);
        }
        return codes;
    }

    public static String getRandomStringOnlyNumbers (int length) {
        char[] chars = "0123456789".toCharArray();
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars[rnd.nextInt(chars.length)]);
        }
        return sb.toString();
    }

    public static String getRandomString(int length) {
        return remove(UUID.randomUUID().toString(), '-').substring(0, length);
    }

    public static String remove(String string, char... removeChars) {
        StringBuilder sb = new StringBuilder(string.length());
        for (int i = 0, l = string.length(); i < l; ++i) {
            char c = string.charAt(i);
            boolean contains = false;
            for (char x : removeChars) {
                if (x == c) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String remove(String text, String search) {
        if (isEmpty(text) || isEmpty(search)) {
            return text;
        }

        int start = 0;
        int end = text.indexOf(search, start);
        if (end == INDEX_NOT_FOUND) {
            return text;
        }
        int max = 0;
        int replLength = search.length();
        StringBuilder buf = new StringBuilder(text.length() + replLength);
        while (end != INDEX_NOT_FOUND) {
            buf.append(text, start, end);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(search, start);
        }
        buf.append(text, start, text.length());
        return buf.toString();
    }

    public static String remove(String string, String... removeStrings) {
        for (String s : removeStrings) {
            string = remove(string, s);
        }
        return string;
    }

    public static String trim(String string, char... trimChars) {
        return string == null ? null : ltrim(rtrim(string, trimChars), trimChars);
    }

    public static String ltrim(String string, char... trimChars) {
        if (string == null || string.length() == 0) {
            return string;
        }
        int i = 0;
        for (int l = string.length(); i < l; ++i) {
            char c = string.charAt(i);
            boolean contains = Character.isWhitespace(c);
            if (!contains) {
                for (char x : trimChars) {
                    if (x == c) {
                        contains = true;
                        break;
                    }
                }
            }
            if (!contains) {
                break;
            }
        }
        return i == 0 ? string : string.substring(i);
    }

    public static String rtrim(String string, char... trimChars) {
        if (string == null || string.length() == 0) {
            return string;
        }
        int i = string.length() - 1;
        for (; i > 0; --i) {
            char c = string.charAt(i);
            boolean contains = Character.isWhitespace(c);
            if (!contains) {
                for (char x : trimChars) {
                    if (x == c) {
                        contains = true;
                        break;
                    }
                }
            }
            if (!contains) {
                ++i;
                break;
            }
        }
        return string.substring(0, i);
    }

    public static Set<String> splitToSet(String string, char separator) {
        return new HashSet<>(Arrays.asList(split(string, separator)));
    }

    public static Set<String> splitToSet(String string, String separator) {
        return new HashSet<>(Arrays.asList(split(string, separator)));
    }

    public static String[] split(String string, String separator) {
        return split(string, separator, 0);
    }

    public static String[] split(String string, String separator, int max) {
        if (string == null) {
            return null;
        }
        int len = string.length();
        if (len == 0 || isEmpty(separator)) {
            return new String[] {string};
        }

        int separatorLen = separator.length();
        List<String> substrings = new ArrayList<>();
        int count = 1;
        int pos = 0;
        for (int i = 0; i < len; ++i) {
            boolean match = true;
            for (int j = 0; j < separatorLen; ++j) {
                int k = i + j;
                if (k >= len || string.charAt(k) != separator.charAt(j)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                substrings.add(pos >= i ? "" : string.substring(pos, i));
                i += separatorLen;
                pos = i;
                i--;
                ++count;

                if (max != 0 && max == count) {
                    break;
                }
            }
        }
        substrings.add(string.substring(pos, len));

        return substrings.toArray(new String[0]);
    }

    public static String[] split(String string, char separator) {
        return split(string, separator, 0);
    }

    public static String[] split(String string, char separator, int max) {
        if (string == null) {
            return null;
        }
        int len = string.length();
        if (len == 0) {
            return new String[] {string};
        }

        List<String> substrings = new ArrayList<>();
        int count = 1;
        int pos = 0;
        for (int i = 0; i < len; ++i) {
            if (string.charAt(i) == separator) {
                substrings.add(pos >= i ? "" : string.substring(pos, i));
                pos = i + 1;
                ++count;

                if (max != 0 && max == count) {
                    break;
                }
            }
        }
        substrings.add(string.substring(pos, len));

        return substrings.toArray(new String[0]);
    }

    public static <T> List<T> splitToType(String string, String separator, Class<T> type) {
        return splitToType(string, separator, 0, type);
    }

    public static <T> List<T> splitToType(String string, String separator, int max, Class<T> type) {
        if (string == null) {
            return null;
        }
        int len = string.length();
        if (len == 0 || isEmpty(separator)) {
            return new ArrayList<>();
        }

        int separatorLen = separator.length();
        List<T> items = new ArrayList<>();
        int count = 1;
        int pos = 0;
        for (int i = 0; i < len; ++i) {
            boolean match = true;
            for (int j = 0; j < separatorLen; ++j) {
                int k = i + j;
                if (k >= len || string.charAt(k) != separator.charAt(j)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                items.add((T) convertValue(pos >= i ? "" : string.substring(pos, i), type));
                i += separatorLen;
                pos = i;
                i--;
                ++count;

                if (max != 0 && max == count) {
                    break;
                }
            }
        }
        items.add((T) convertValue(string.substring(pos, len), type));

        return items;
    }

    public static <T> List<T> splitToType(String string, char separator, Class<T> type) {
        return splitToType(string, separator, 0, type);
    }

    public static <T> List<T> splitToType(String string, char separator, int max, Class<T> type) {
        if (string == null) {
            return null;
        }
        int len = string.length();
        if (len == 0) {
            return new ArrayList<>();
        }

        List<T> items = new ArrayList<>();
        int count = 1;
        int pos = 0;
        for (int i = 0; i < len; ++i) {
            if (string.charAt(i) == separator) {
                items.add((T) convertValue(pos >= i ? "" : string.substring(pos, i), type));
                pos = i + 1;
                ++count;

                if (max != 0 && max == count) {
                    break;
                }
            }
        }

        items.add((T) convertValue(string.substring(pos, len), type));

        return items;
    }

    private static Object convertValue(String string, Class type) {
        if (type.equals(Integer.class)) {
            return toInteger(string);
        } else if (type.equals(Long.class)) {
            return toLong(string);
        } else if (type.equals(Double.class)) {
            return toDouble(string);
        } else if (type.equals(DateTime.class)) {
            try {
                return new DateTime(string);
            } catch (DateTimeException e) {
                return null;
            }
        } else if (type.equals(Character.class)) {
            return toCharacter(string);
        } else if (type.equals(Boolean.class)) {
            return toCharacter(string);
        } else {
            return string;
        }
    }

    public static String[] splits(String string, char... separator) {
        return splits(string, separator, 0);
    }

    public static String[] splits(String string, char[] separator, int max) {
        if (string == null) {
            return null;
        }
        int len = string.length();
        if (len == 0) {
            return new String[] {string};
        }

        List<String> substrings = new ArrayList<>();
        int count = 1;
        int pos = 0;
        for (int i = 0; i < len; ++i) {
            char current = string.charAt(i);
            for (char c : separator) {
                if (current == c) {
                    substrings.add(pos >= i ? "" : string.substring(pos, i));
                    pos = i + 1;
                    ++count;

                    if (max != 0 && max == count) {
                        break;
                    }
                }
            }
        }
        substrings.add(string.substring(pos, len));

        return substrings.toArray(new String[0]);
    }

    public static boolean contains(String string, char searchChar) {
        if (isEmpty(string)) {
            return false;
        }
        return string.indexOf(searchChar) >= 0;
    }

    public static boolean contains(String string, String search) {
        return string.contains(search);
    }

    public static boolean containsIgnoreCase(String string, String search) {
        return string != null && search != null && string.toLowerCase().contains(search.toLowerCase());
    }

    public static int countMatches(String string, char c) {
        if (isEmpty(string)) {
            return 0;
        }
        int count = 0;
        for (int i = 0, l = string.length(); i < l; ++i) {
            if (string.charAt(i) == c) {
                ++count;
            }
        }
        return count;
    }

    public static int countMatches(String string, String subString) {
        if (isEmpty(string) || isEmpty(subString)) {
            return 0;
        }
        int count = 0;
        char c = subString.charAt(0);
        int k, j;
        boolean matched;
        for (int i = 0, len = string.length(), subStrLen = subString.length(); i < len; ++i) {
            if (string.charAt(i) == c) {
                if (subStrLen > len - i) {
                    return count;
                }
                k = i + 1;
                matched = true;
                for (j = 1; j < subStrLen; ++j, ++k) {
                    if (subString.charAt(j) != string.charAt(k)) {
                        matched = false;
                        break;
                    }
                }
                if (matched) {
                    ++count;
                    i = k;
                }
            }
        }
        return count;
    }

    public static String replace(String string, char search, String replace) {
        StringBuilder sb = new StringBuilder(string.length());
        for (int i = 0, l = string.length(); i < l; ++i) {
            char c = string.charAt(i);
            sb.append(c == search ? replace : c);
        }
        return sb.toString();
    }

    public static String replace(String string, char search, char replace) {
        StringBuilder sb = new StringBuilder(string.length());
        for (int i = 0, l = string.length(); i < l; ++i) {
            char c = string.charAt(i);
            sb.append(c == search ? replace : c);
        }
        return sb.toString();
    }

    public static String replace(String string, char[] search, String replace) {
        StringBuilder sb = new StringBuilder(string.length());
        boolean found;
        for (int i = 0, l = string.length(); i < l; ++i) {
            char c = string.charAt(i);
            found = false;
            for (char d : search) {
                if (d == c) {
                    sb.append(replace);
                    found = true;
                }
            }
            if (!found) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String replace(String string, char[] search, char[] replace) {
        StringBuilder sb = new StringBuilder(string.length());
        int searchCharsLen = search.length;
        boolean found;
        for (int i = 0, l = string.length(); i < l; ++i) {
            char c = string.charAt(i);
            found = false;
            for (int j = 0; j < searchCharsLen; ++j) {
                if (c == search[j]) {
                    sb.append(replace[j]);
                    found = true;
                    break;
                }
            }
            if (!found) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String replace(String string, char[] search, String[] replace) {
        StringBuilder sb = new StringBuilder(string.length());
        int searchCharsLen = search.length;
        boolean found;
        for (int i = 0, l = string.length(); i < l; ++i) {
            char c = string.charAt(i);
            found = false;
            for (int j = 0; j < searchCharsLen; ++j) {
                if (c == search[j]) {
                    sb.append(replace[j]);
                    found = true;
                    break;
                }
            }
            if (!found) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String replace(String string, String[] search, String[] replace) {
        for (int i = 0, l = search.length; i < l; ++i) {
            string = replace(string, search[i], replace[i]);
        }
        return string;
    }

    public static String replace(String string, String[] search, String replace) {
        for (String s : search) {
            string = replace(string, s, replace);
        }
        return string;
    }

    public static String replace(String text, String search, String replace) {
        return replace(text, search, replace, -1, false, false);
    }

    public static String replace(String text, String search, String replace, int max) {
        return replace(text, search, replace, max, false, false);
    }

    public static String replaceIgnoreCase(String text, String search, String replace) {
        return replace(text, search, replace, -1, true, false);
    }

    public static String replaceIgnoreCase(String text, String search, String replace, int max) {
        return replace(text, search, replace, max, true, false);
    }

    public static String replaceWord(String text, String search, String replace) {
        return replace(text, search, replace, -1, false, true);
    }

    public static String replaceWordIgnoreCase(String text, String search, String replace) {
        return replace(text, search, replace, -1, true, true);
    }

    private static String replace(String text, String searchString, String replace, int max, boolean ignoreCase, boolean isWord) {
        if (isEmpty(text) || searchString == null || searchString.length() == 0 || replace == null || max == 0) {
             return text;
        }

        if (isWord) {
            if (text.startsWith(searchString)) {
                text = replace + text.substring(searchString.length());
            }
            searchString = " " + searchString;
        }
        String searchText = text;

        if (ignoreCase) {
            searchText = searchText.toLowerCase();
            searchString = searchString.toLowerCase();
        }
        int start = 0;
        int end = searchText.indexOf(searchString, start);
        if (end == INDEX_NOT_FOUND) {
            return text;
        }
        int replLength = searchString.length();
        int increase = replace.length() - replLength;
        increase = Math.max(increase, 0);
        increase *= max < 0 ? 16 : Math.min(max, 64);
        StringBuilder buf = new StringBuilder(text.length() + increase);
        while (end != INDEX_NOT_FOUND) {
            buf.append(text, start, end).append(replace);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = searchText.indexOf(searchString, start);
        }
        buf.append(text, start, text.length());
        return buf.toString();
    }

    public static String removeExcept(String string, char... keep) {
        StringBuilder sb = new StringBuilder(string.length());
        int keepCharsLen = keep.length;
        boolean found;
        for (int i = 0, l = string.length(); i < l; ++i) {
            char c = string.charAt(i);
            found = false;
            for (char value : keep) {
                if (c == value) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String normalizeKeywords(String string) {
        if (isEmpty(string)) {
            return "";
        }

        String separator = null;
        for (String item : new String[] {",", "،", "+", "|",}) {
            if (contains(string, item)) {
                separator = item;
                break;
            }
        }

        if (separator == null) {
            return string.trim().replaceAll("\\s{2,}", " ");
        }

        return replace(string.replaceAll("\\s+", ""), separator, " ").trim();
    }

    public static String normalizeFullText(String string, String lang) {
        if (lang == null) {
            lang = Locale.getDefaultLocale();
        }
        String[] stopWords = StopWord.get(lang);

        StringBuilder sb = new StringBuilder(string.length());
        for (String word : split(string.replaceAll("\\s{2,}", " "), ' ')) {
            int length = word.length();
            if (length == 1) {
                continue;
            }

            boolean accept = true;

            for (String stopWord : stopWords) {
                if (stopWord.startsWith("{excluderange")) {
                    String[] parts = split(remove(stopWord, "{excluderange", "}", "(", ")"), VantarParam.SEPARATOR_COMMON);
                    if (parts.length != 2) {
                        continue;
                    }
                    int low = toInteger(parts[0]);
                    int high = toInteger(parts[1]);
                    for (int i = 0; i < length; ++i) {
                        int c = word.charAt(i);
                        if (c >= low && c <= high) {
                            accept = false;
                            break;
                        }
                    }

                } else if (stopWord.startsWith("{includerange")) {
                    String[] items = split(remove(stopWord, "{includerange", "}"), VantarParam.SEPARATOR_BLOCK);
                    MinMax[] minMaxes = new MinMax[items.length];
                    int i = 0;
                    for (String item : items) {
                        String[] parts = split(remove(item, ')', '('), VantarParam.SEPARATOR_COMMON);
                        if (parts.length != 2) {
                            continue;
                        }
                        minMaxes[i++] = new MinMax(toInteger(parts[0]), toInteger(parts[1]));
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

    public static String firstCharToLowerCase(String string) {
        if (string == null || string.length() == 0) {
            return "";
        }
        return string.length() == 1 ? string.toLowerCase() : Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }

    public static String firstCharToUpperCase(String string) {
        if (string == null || string.length() == 0) {
            return "";
        }
        return string.length() == 1 ? string.toUpperCase() : Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    public static boolean isNumeric(String string) {
        if (string == null) {
            return false;
        }
        string = StringUtil.remove(string, "+", "-", "/");
        try {
            Double.parseDouble(string);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }

    public static String getBetween(String text, String textFrom, String textTo) {
        String result = text.substring(text.indexOf(textFrom) + textFrom.length());
        result = result.substring(0, result.indexOf(textTo));
        return result;
    }

    public static int compare(String sA, String sB) {
        if (sA == null) {
            if (sB == null) {
                return 0;
            }
            return -1;
        }
        if (sB == null) {
            return 1;
        }

        int lenA = sA.length();
        int lenB = sB.length();
        for (int i = 0, l = Math.min(lenA, lenB) ; i < l ; ++i) {
            if (sA.charAt(i) < sB.charAt(i)) {
                return -1;
            } else if (sA.charAt(i) > sB.charAt(i)) {
                return 1;
            }
        }
        return Integer.compare(lenA, lenB);
    }

    public static int compareCi(String sA, String sB) {
        if (sA == null) {
            if (sB == null) {
                return 0;
            }
            return -1;
        }
        if (sB == null) {
            return 1;
        }

        int lenA = sA.length();
        int lenB = sB.length();
        sA = sA.toLowerCase();
        sB = sB.toLowerCase();
        for (int i = 0, l = Math.min(lenA, lenB) ; i < l ; ++i) {
            if (sA.charAt(i) < sB.charAt(i)) {
                return -1;
            } else if (sA.charAt(i) > sB.charAt(i)) {
                return 1;
            }
        }
        return Integer.compare(lenA, lenB);
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
