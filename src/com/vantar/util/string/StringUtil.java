package com.vantar.util.string;

import com.vantar.database.datatype.Location;
import com.vantar.database.dto.Dto;
import com.vantar.exception.DateTimeException;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.json.Json;
import com.vantar.util.number.NumberUtil;
import com.vantar.util.object.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * String value utilities
 */
public class StringUtil {

    private static final Pattern PATTERN_SNAKE_CASE = Pattern.compile("(.)(\\p{Upper})");
    private static final int INDEX_NOT_FOUND = -1;

    /**
     * Check if a string is empty
     *
     * @param value string to check
     * @return null, "", "      " are empty
     */
    public static boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

    /**
     * Check if all strings are empty
     *
     * @param values strings to check
     * @return values == null or values == [] or all items are (null or "" or "      ")
     */
    public static boolean areEmpty(String... values) {
        if (ObjectUtil.isEmpty(values)) {
            return true;
        }
        for (String value : values) {
            if (!isEmpty(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a string is not empty
     *
     * @param value string to check
     * @return null, "", "      " are empty
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    /**
     * Check if all strings are not empty
     *
     * @param values strings to check
     * @return values != null and values != [] and all items are not (null or "" or "      ")
     */
    public static boolean areNotEmpty(String... values) {
        if (ObjectUtil.isEmpty(values)) {
            return false;
        }
        for (String value : values) {
            if (isEmpty(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Replace persian chars, remove none number chars (including ".") then convert to number
     *
     * @param string string to convert
     * @return number or (null if string == null)
     */
    public static Long scrapeLong(String string) {
        if (string == null) {
            return null;
        }
        return toLong(Persian.Number.toLatin(string).trim().replaceAll("[^0-9]", ""));
    }

    /**
     * Replace persian chars, remove none number chars (including ".") then convert to number
     *
     * @param string string to convert
     * @return number or (null if string == null)
     */
    public static Integer scrapeInteger(String string) {
        if (string == null) {
            return null;
        }
        return toInteger(Persian.Number.toLatin(string).trim().replaceAll("[^0-9]", ""));
    }

    /**
     * Replace persian chars, remove none number chars then convert to number
     *
     * @param string string to convert
     * @return number or (null if string == null)
     */
    public static Double scrapeDouble(String string) {
        if (string == null) {
            return null;
        }
        return toDouble(Persian.Number.toLatin(string).trim().replaceAll("[^0-9.]", ""));
    }

    /**
     * Replace persian chars, remove none number chars then convert to number
     *
     * @param string string to convert
     * @return number or (null if string == null)
     */
    public static Float scrapeFloat(String string) {
        if (string == null) {
            return null;
        }
        return toFloat(Persian.Number.toLatin(string).trim().replaceAll("[^0-9.]", ""));
    }

    /**
     * Replace persian chars then convert to number
     *
     * @param string string to convert
     * @return number or (null if string == null)
     */
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

    /**
     * Replace persian chars then convert to number
     *
     * @param string string to convert
     * @return number or (null if string == null)
     */
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

    /**
     * Replace persian chars then convert to number
     *
     * @param string string to convert
     * @return number or (null if string == null)
     */
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

    /**
     * Replace persian chars then convert to number
     *
     * @param string string to convert
     * @return number or (null if string == null)
     */
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

    /**
     * Replace persian chars then convert to byte
     *
     * @param string string to convert
     * @return byte or (null if string == null)
     */
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

    /**
     * Convert to boolean
     *
     * @param string string to convert
     * @return (true if string - ci == " 1 ", " true ", " yes ", " on ", " بله ", " بلی ") or
     * (false if string-ci == "0", "false", "no", "off", "نه", "خیر") or
     * (null if string == null or "" or "     " or not in above values)
     */
    public static Boolean toBoolean(String string) {
        if (isEmpty(string)) {
            return null;
        }
        string = string.trim();
        if (string.isEmpty()) {
            return null;
        }

        if (string.equals("1") || string.equalsIgnoreCase("true") || string.equalsIgnoreCase("yes")
            || string.equalsIgnoreCase("بله") || string.equalsIgnoreCase("بلی") || string.equalsIgnoreCase("on")) {
            return true;
        }
        if (string.equals("0") || string.equalsIgnoreCase("false") || string.equalsIgnoreCase("no")
            || string.equalsIgnoreCase("نه") || string.equalsIgnoreCase("خیر") || string.equalsIgnoreCase("off")) {
            return false;
        }
        return null;
    }

    /**
     * Convert to char
     *
     * @param string string to convert
     * @return first char or (null if string == null or "" or "     ")
     */
    public static Character toCharacter(String string) {
        if (string == null) {
            return null;
        }
        string = string.trim();
        if (string.isEmpty()) {
            return null;
        }
        return string.charAt(0);
    }

    /**
     * Convert to timestamp
     *
     * @param string string to convert
     * @return (timestamp if string is a valid timestamp or date - time) or
     * (null if string == null or "" or "     " or invalid date-time string)
     */
    public static Long toTimestamp(String string) {
        try {
            return new DateTime(string).getAsTimestamp();
        } catch (DateTimeException e) {
            return null;
        }
    }

    /**
     * Convert to object
     *
     * @param string    string to convert
     * @param typeClass class to convert to (number classes, Byte, Boolean, Character, Location, DateTime, Enum, Dto)
     * @return instance of typeClass or (null if string == null or "" or "     " or unsupported typeClass)
     */
    public static Object toObject(String string, Class<?> typeClass) {
        if (typeClass.equals(String.class)) {
            return string;
        }
        if (ClassUtil.isInstantiable(typeClass, Number.class)) {
            return NumberUtil.toNumber(string, typeClass);
        }
        if (typeClass.equals(Boolean.class)) {
            return toBoolean(string);
        }
        if (typeClass.equals(Character.class)) {
            return toCharacter(string);
        }
        if (typeClass.equals(Location.class)) {
            return new Location(string);
        }
        if (typeClass.equals(DateTime.class)) {
            try {
                return new DateTime(string);
            } catch (DateTimeException ignore) {
                return null;
            }
        }
        if (typeClass.isEnum()) {
            return EnumUtil.getEnumValue(string, typeClass);
        }
        if (ClassUtil.isInstantiable(typeClass, Dto.class)) {
            return Json.d.fromJson(string, typeClass);
        }
        return null;
    }

    /**
     * Change to camelCase
     *
     * @param string string to convert
     * @return (null if string == null)
     */
    public static String toCamelCase(String string) {
        return toCamelCase(string, false);
    }

    /**
     * Change to CamelCase
     *
     * @param string string to convert
     * @return (null if string == null)
     */
    public static String toStudlyCase(String string) {
        return toCamelCase(string, true);
    }

    private static String toCamelCase(String string, boolean capitalizeFirst) {
        if (string == null) {
            return null;
        }
        string = StringUtil.trim(string, '/', '_', '.', '_', '/');

        StringBuilder stringBuilder = new StringBuilder(20);
        char value;

        if (string.contains("_")) {
            string = string.toLowerCase();
        }

        for (int i = 0; i < string.length(); ++i) {
            value = string.charAt(i);
            if (value == '_' || value == '.' || value == '-' || value == '/') {
                capitalizeFirst = true;
            } else if (capitalizeFirst) {
                stringBuilder.append(Character.toUpperCase(value));
                capitalizeFirst = false;
            } else {
                stringBuilder.append(i == 0 ? Character.toLowerCase(value) : value);
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Change to snake_case
     *
     * @param string string to convert
     * @return (null if string == null)
     */
    public static String toSnakeCase(String string) {
        return string == null ? null : PATTERN_SNAKE_CASE.matcher(string).replaceAll("$1_$2").toLowerCase();
    }

    /**
     * Change to snake-case
     *
     * @param string string to convert
     * @return (null if string == null)
     */
    public static String toKababCase(String string) {
        return string == null ? null : PATTERN_SNAKE_CASE.matcher(string).replaceAll("$1-$2").toLowerCase();
    }

    /**
     * Get a random string
     *
     * @param length number of chars
     * @return piece of UUID
     */
    public static String getRandomString(int length) {
        return remove(UUID.randomUUID().toString(), '-').substring(0, length);
    }

    /**
     * Get random strings
     *
     * @param count number of random strings to get
     * @return a set of random strings
     */
    public static Set<String> getRandomStrings(int count) {
        Set<String> codes = new HashSet<>(count, 1);
        while (codes.size() < count) {
            codes.add(split(UUID.randomUUID().toString(), '-')[0]);
        }
        return codes;
    }

    /**
     * Get random strings
     *
     * @param count  number of random strings to get
     * @param length number of chars
     * @return a set of random strings
     */
    public static Set<String> getRandomStrings(int count, int length) {
        Set<String> codes = new HashSet<>(count, 1);
        while (codes.size() < count) {
            codes.add(getRandomString(length));
        }
        return codes;
    }

    /**
     * Get a random string with only nummeric chars
     *
     * @param length number of chars
     * @return "321543213"
     */
    public static String getRandomStringOnlyNumbers(int length) {
        char[] chars = "0123456789".toCharArray();
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars[rnd.nextInt(chars.length)]);
        }
        return sb.toString();
    }

    /**
     * Remove chars from a string
     *
     * @param string      base
     * @param removeChars to remove
     * @return updated string (null if string == null)
     */
    public static String remove(String string, char... removeChars) {
        if (isEmpty(string)) {
            return string;
        }

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

    /**
     * Remove string from a string
     *
     * @param string       base
     * @param removeString to remove
     * @return updated string (null if string == null)
     */
    public static String remove(String string, String removeString) {
        if (isEmpty(string)) {
            return string;
        }

        int start = 0;
        int end = string.indexOf(removeString, start);
        if (end == INDEX_NOT_FOUND) {
            return string;
        }
        int max = 0;
        int replLength = removeString.length();
        StringBuilder buf = new StringBuilder(string.length() + replLength);
        while (end != INDEX_NOT_FOUND) {
            buf.append(string, start, end);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = string.indexOf(removeString, start);
        }
        buf.append(string, start, string.length());
        return buf.toString();
    }

    /**
     * Remove strings from a string
     *
     * @param string        base
     * @param removeStrings to remove
     * @return updated string (null if string == null)
     */
    public static String remove(String string, String... removeStrings) {
        if (isEmpty(string)) {
            return string;
        }
        for (String s : removeStrings) {
            string = remove(string, s);
        }
        return string;
    }

    /**
     * Remove all chars from a strings except the given chars
     *
     * @param string base
     * @param keep   chars to keep
     * @return updated string (null if string == null)
     */
    public static String removeExcept(String string, char... keep) {
        if (isEmpty(string)) {
            return string;
        }
        StringBuilder sb = new StringBuilder(string.length());
        boolean found;
        for (int i = 0, l = string.length(); i < l; ++i) {
            char c = string.charAt(i);
            for (char value : keep) {
                if (c == value) {
                    sb.append(c);
                    break;
                }
            }
        }
        return sb.toString();
    }

    /**
     * Trim(remove) chars and whitespace from left and right side of a string
     *
     * @param string    to trim
     * @param trimChars chars to trim
     * @return updated string (null if string == null)
     */
    public static String trim(String string, char... trimChars) {
        return string == null ? null : ltrim(rtrim(string, trimChars), trimChars);
    }

    /**
     * Trim(remove) chars and whitespace from left
     *
     * @param string    to trim
     * @param trimChars chars to trim
     * @return updated string (null if string == null)
     */
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

    /**
     * Trim(remove) chars and whitespace from right side of a string
     *
     * @param string    to trim
     * @param trimChars chars to trim
     * @return updated string (null if string == null)
     */
    public static String rtrim(String string, char... trimChars) {
        if (string == null || string.length() == 0) {
            return string;
        }
        int i = string.length() - 1;
        for (; i >= 0; --i) {
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
        return i <= 0 ? "" : string.substring(0, i);
    }

    /**
     * Split string (items are not trimmed)
     *
     * @param string    to split
     * @param separator separator
     * @return splatted parts (null if string == null, [string] if separator == null or "" or string == "")
     */
    public static String[] split(String string, String separator) {
        return split(string, separator, 0, false);
    }

    /**
     * Split string (items are trimmed)
     *
     * @param string    to split
     * @param separator separator
     * @return splatted parts (null if string == null, [string] if separator == null or "" or string == "")
     */
    public static String[] splitTrim(String string, String separator) {
        return split(string, separator, 0, true);
    }

    /**
     * Split string (items are not trimmed)
     *
     * @param string    to split
     * @param separator separator
     * @param max       max number of splatted parts (splits from left to right) 0 = no limit
     * @return splatted parts (null if string == null, [string] if separator == null or "" or string == "")
     */
    public static String[] split(String string, String separator, int max) {
        return split(string, separator, max, false);
    }

    /**
     * Split string (items are trimmed)
     *
     * @param string    to split
     * @param separator separator
     * @param max       max number of splatted parts (splits from left to right) 0 = no limit
     * @return splatted parts (null if string == null, [string] if separator == null or "" or string == "")
     */
    public static String[] splitTrim(String string, String separator, int max) {
        return split(string, separator, max, true);
    }

    /**
     * Split string
     *
     * @param string    to split
     * @param separator separator
     * @param max       max number of splatted parts (splits from left to right) 0 = no limit
     * @param trim      trim items
     * @return splatted parts (null if string == null, [string] if separator == null or "" or string == "")
     */
    public static String[] split(String string, String separator, int max, boolean trim) {
        if (isEmpty(string)) {
            return null;
        }
        int len = string.length();
        if (len == 0 || separator == null || separator.length() == 0) {
            return new String[]{string};
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
                substrings.add(pos >= i ? "" : (trim ? string.substring(pos, i).trim() : string.substring(pos, i)));
                i += separatorLen;
                pos = i;
                i--;
                ++count;

                if (max != 0 && max == count) {
                    break;
                }
            }
        }
        substrings.add(trim ? string.substring(pos, len).trim() : string.substring(pos, len));

        return substrings.toArray(new String[0]);
    }

    /**
     * Split string (items are not trimmed)
     *
     * @param string    to split
     * @param separator separator
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static String[] split(String string, char separator) {
        return split(string, separator, 0, false);
    }

    /**
     * Split string (items are trimmed)
     *
     * @param string    to split
     * @param separator separator
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static String[] splitTrim(String string, char separator) {
        return split(string, separator, 0, true);
    }

    /**
     * Split string (items are not trimmed)
     *
     * @param string    to split
     * @param separator separator
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static Set<String> splitToSet(String string, char separator) {
        String[] a = split(string, separator);
        return a == null ? null : new HashSet<>(Arrays.asList(a));
    }

    /**
     * Split string (items are trimmed)
     *
     * @param string    to split
     * @param separator separator
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static Set<String> splitToSetTrim(String string, char separator) {
        String[] a = splitTrim(string, separator);
        return a == null ? null : new HashSet<>(Arrays.asList(a));
    }

    /**
     * Split string (items are not trimmed)
     *
     * @param string    to split
     * @param separator separator
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static List<String> splitToList(String string, char separator) {
        String[] a = split(string, separator);
        return a == null ? null : new ArrayList<>(Arrays.asList(a));
    }

    /**
     * Split string (items are trimmed)
     *
     * @param string    to split
     * @param separator separator
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static List<String> splitToListTrim(String string, char separator) {
        String[] a = splitTrim(string, separator);
        return a == null ? null : new ArrayList<>(Arrays.asList(a));
    }

    /**
     * Split string (items are not trimmed)
     * @param string to split
     * @param separator separator
     * @param max max number of splatted parts (splits from left to right) 0 = no limit
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static String[] split(String string, char separator, int max) {
        return split(string, separator, max, false);
    }

    /**
     * Split string (items are trimmed)
     * @param string to split
     * @param separator separator
     * @param max max number of splatted parts (splits from left to right) 0 = no limit
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static String[] splitTrim(String string, char separator, int max) {
        return split(string, separator, max, true);
    }

    /**
     * Split string
     * @param string to split
     * @param separator separator
     * @param max max number of splatted parts (splits from left to right) 0 = no limit
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static String[] split(String string, char separator, int max, boolean trim) {
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
                substrings.add(pos >= i ? "" : (trim ? string.substring(pos, i).trim() : string.substring(pos, i)));
                pos = i + 1;
                ++count;

                if (max != 0 && max == count) {
                    break;
                }
            }
        }
        substrings.add(trim ? string.substring(pos, len).trim() : string.substring(pos, len));

        return substrings.toArray(new String[0]);
    }

    /**
     * Split string (items are not trimmed)
     * @param string to split
     * @param separator separator
     * @return splatted parts (null if string == null, [string] if separator == null or "" or string == "")
     */
    public static Set<String> splitToSet(String string, String separator) {
        String[] a = split(string, separator);
        return a == null ? null : new HashSet<>(Arrays.asList(a));
    }

    /**
     * Split string (items are trimmed)
     * @param string to split
     * @param separator separator
     * @return splatted parts (null if string == null, [string] if separator == null or "" or string == "")
     */
    public static Set<String> splitToSetTrim(String string, String separator) {
        return new HashSet<>(Arrays.asList(splitTrim(string, separator)));
    }

    /**
     * Split string to objects of the given type
     * @param string to split
     * @param separator separator
     * @param type type of splatted objects
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static <T> List<T> splitToType(String string, String separator, Class<T> type) {
        return splitToType(string, separator, 0, type);
    }

    /**
     * Split string to objects of the given type
     * @param string to split
     * @param separator separator
     * @param max max number of splatted parts (splits from left to right) 0 = no limit
     * @param type type of splatted objects
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static <T> List<T> splitToType(String string, String separator, int max, Class<T> type) {
        if (string == null) {
            return null;
        }
        int len = string.length();
        if (len == 0 || separator == null || separator.length() == 0) {
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
                items.add((T) toObject(pos >= i ? "" : string.substring(pos, i), type));
                i += separatorLen;
                pos = i;
                i--;
                ++count;

                if (max != 0 && max == count) {
                    break;
                }
            }
        }
        items.add((T) toObject(string.substring(pos, len), type));

        return items;
    }

    /**
     * Split string to objects of the given type
     * @param string to split
     * @param separator separator
     * @param type type of splatted objects
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static <T> List<T> splitToType(String string, char separator, Class<T> type) {
        return splitToType(string, separator, 0, type);
    }

    /**
     * Split string to objects of the given type
     * @param string to split
     * @param separator separator
     * @param max max number of splatted parts (splits from left to right) 0 = no limit
     * @param type type of splatted objects
     * @return splatted parts (null if string == null, [string] if string == "")
     */
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
                items.add((T) toObject(pos >= i ? "" : string.substring(pos, i), type));
                pos = i + 1;
                ++count;

                if (max != 0 && max == count) {
                    break;
                }
            }
        }

        items.add((T) toObject(string.substring(pos, len), type));

        return items;
    }

    /**
     * Split strings (items are not trimmed)
     * @param string to split
     * @param separator separators
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static String[] splits(String string, char... separator) {
        return splits(string, separator, 0, false);
    }

    /**
     * Split strings (items are trimmed)
     * @param string to split
     * @param separator separators
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static String[] splitsTrim(String string, char... separator) {
        return splits(string, separator, 0, true);
    }

    /**
     * Split strings (items are not trimmed)
     * @param string to split
     * @param separator separators
     * @param max max number of splatted parts (splits from left to right) 0 = no limit
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static String[] splits(String string, char[] separator, int max) {
        return splits(string, separator, max, false);
    }

    /**
     * Split strings (items are trimmed)
     * @param string to split
     * @param separator separators
     * @param max max number of splatted parts (splits from left to right) 0 = no limit
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static String[] splitsTrim(String string, char[] separator, int max) {
        return splits(string, separator, max, true);
    }

    /**
     * Split strings
     * @param string to split
     * @param separator separators
     * @param max max number of splatted parts (splits from left to right) 0 = no limit
     * @param trim trim items
     * @return splatted parts (null if string == null, [string] if string == "")
     */
    public static String[] splits(String string, char[] separator, int max, boolean trim) {
        if (string == null) {
            return null;
        }
        int len = string.length();
        if (len == 0) {
            return new String[] {string};
        }

        List<String> substrings = new ArrayList<>(20);
        int count = 1;
        int pos = 0;
        for (int i = 0; i < len; ++i) {
            char current = string.charAt(i);
            for (char c : separator) {
                if (current == c) {
                    substrings.add(pos >= i ? "" : (trim ? string.substring(pos, i).trim() : string.substring(pos, i)));
                    pos = i + 1;
                    ++count;

                    if (max != 0 && max == count) {
                        break;
                    }
                }
            }
        }
        substrings.add(trim ? string.substring(pos, len).trim() : string.substring(pos, len));

        return substrings.toArray(new String[0]);
    }

    /**
     * Check if string contains a char or one of the chars
     * @param string to check
     * @param searchChar char(s) to check
     * @return true if contains at least one of the chars
     */
    public static boolean contains(String string, char... searchChar) {
        if (isEmpty(string)) {
            return false;
        }
        for (char c : searchChar) {
            if (string.indexOf(c) > -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if string contains all char(s)
     * @param string to check
     * @param searchChar char(s) to check
     * @return true if contains all of the chars
     */
    public static boolean containsAll(String string, char... searchChar) {
        if (isEmpty(string)) {
            return false;
        }
        for (char c : searchChar) {
            if (string.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if string contains a string or one of the strings
     * @param string to check
     * @param search string(s) to check
     * @return true if contains at least one of the strings
     */
    public static boolean contains(String string, String... search) {
        if (isEmpty(string)) {
            return false;
        }
        for (String s : search) {
            if (isEmpty(s)) {
                continue;
            }
            if (string.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if string contains all of the strings
     * @param string to check
     * @param search string(s) to check
     * @return true if contains all of the strings
     */
    public static boolean containsAll(String string, String... search) {
        if (isEmpty(string)) {
            return false;
        }
        for (String s : search) {
            if (isEmpty(s)) {
                continue;
            }
            if (!string.contains(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if string contains a string or one of the strings (case insensitive)
     * @param string to check
     * @param search string(s) to check
     * @return true if contains at least one of the strings
     */
    public static boolean containsCi(String string, String... search) {
        if (isEmpty(string)) {
            return false;
        }
        string = string.toLowerCase();
        for (String s : search) {
            if (isEmpty(s)) {
                continue;
            }
            s = s.toLowerCase();
            if (string.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if string contains all of the strings (case insensitive)
     * @param string to check
     * @param search string(s) to check
     * @return true if contains all of the strings
     */
    public static boolean containsCiAll(String string, String... search) {
        if (isEmpty(string)) {
            return false;
        }
        string = string.toLowerCase();
        for (String s : search) {
            if (isEmpty(s)) {
                continue;
            }
            s = s.toLowerCase();
            if (!string.contains(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Count number of a char occurrences in a string
     * @param string string to check
     * @param search to count
     * @return count
     */
    public static int countMatches(String string, char search) {
        if (isEmpty(string)) {
            return 0;
        }
        int count = 0;
        for (int i = 0, l = string.length(); i < l; ++i) {
            if (string.charAt(i) == search) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Count number of a string occurrences in a string
     * @param string string to check
     * @param search to count
     * @return count
     */
    public static int countMatches(String string, String search) {
        if (isEmpty(string) || isEmpty(search)) {
            return 0;
        }
        int count = 0;
        char c = search.charAt(0);
        int k, j;
        boolean matched;
        for (int i = 0, len = string.length(), subStrLen = search.length(); i < len; ++i) {
            if (string.charAt(i) == c) {
                if (subStrLen > len - i) {
                    return count;
                }
                k = i + 1;
                matched = true;
                for (j = 1; j < subStrLen; ++j, ++k) {
                    if (search.charAt(j) != string.charAt(k)) {
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

    /**
     * Replace something with something in a string
     * @param string to check
     * @param search to be replaced
     * @param replace to replace with
     * @return updated string (null if string == null)
     */
    public static String replace(String string, char search, String replace) {
        if (isEmpty(string)) {
            return string;
        }
        StringBuilder sb = new StringBuilder(string.length());
        for (int i = 0, l = string.length(); i < l; ++i) {
            char c = string.charAt(i);
            sb.append(c == search ? replace : c);
        }
        return sb.toString();
    }

    /**
     * Replace something with something in a string
     * @param string to check
     * @param search to be replaced
     * @param replace to replace with
     * @return updated string (null if string == null)
     */
    public static String replace(String string, char search, char replace) {
        if (isEmpty(string)) {
            return string;
        }
        StringBuilder sb = new StringBuilder(string.length());
        for (int i = 0, l = string.length(); i < l; ++i) {
            char c = string.charAt(i);
            sb.append(c == search ? replace : c);
        }
        return sb.toString();
    }

    /**
     * Replace somethings with something in a string
     * @param string to check
     * @param search to be replaced
     * @param replace to replace with
     * @return updated string (null if string == null)
     */
    public static String replace(String string, char[] search, String replace) {
        if (isEmpty(string) || search.length == 0) {
            return string;
        }
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

    /**
     * Replace somethings with somethings in a string (matched by array indexes)
     * @param string to check
     * @param search to be replaced
     * @param replace to replace with
     * @return updated string (null if string == null)
     */
    public static String replace(String string, char[] search, char[] replace) {
        if (isEmpty(string) || search.length == 0 || search.length != replace.length) {
            return string;
        }
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

    /**
     * Replace somethings with somethings in a string (matched by array indexes)
     * @param string to check
     * @param search to be replaced
     * @param replace to replace with
     * @return updated string (null if string == null)
     */
    public static String replace(String string, char[] search, String[] replace) {
        if (isEmpty(string) || search.length == 0 || search.length != replace.length) {
            return string;
        }
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

    /**
     * Replace somethings with somethings in a string (matched by array indexes) (case sensitive)
     * @param string to check
     * @param search to be replaced
     * @param replace to replace with
     * @return updated string (null if string == null)
     */
    public static String replace(String string, String[] search, String[] replace) {
        if (isEmpty(string) || search.length == 0 || search.length != replace.length) {
            return string;
        }
        for (int i = 0, l = search.length; i < l; ++i) {
            string = replace(string, search[i], replace[i]);
        }
        return string;
    }

    /**
     * Replace somethings with something in a string (matched by array indexes) (case sensitive)
     * @param string to check
     * @param search to be replaced
     * @param replace to replace with
     * @return updated string (null if string == null)
     */
    public static String replace(String string, String[] search, String replace) {
        if (isEmpty(string) || search.length == 0) {
            return string;
        }
        for (String s : search) {
            string = replace(string, s, replace);
        }
        return string;
    }

    /**
     * Replace something with something in a string (case sensitive)
     * @param string to check
     * @param search to be replaced
     * @param replace to replace with
     * @return updated string (null if string == null)
     */
    public static String replace(String string, String search, String replace) {
        return replace(string, search, replace, -1, false, false);
    }

    /**
     * Replace something with something in a string (matched by array indexes) (case sensitive)
     * @param string to check
     * @param search to be replaced
     * @param replace to replace with
     * @param max only replace the first max occurrences
     * @return updated string (null if string == null)
     */
    public static String replace(String string, String search, String replace, int max) {
        return replace(string, search, replace, max, false, false);
    }

    /**
     * Replace something with something in a string (case insensitive)
     * @param string to check
     * @param search to be replaced
     * @param replace to replace with
     * @return updated string (null if string == null)
     */
    public static String replaceCi(String string, String search, String replace) {
        return replace(string, search, replace, -1, true, false);
    }

    /**
     * Replace something with something in a string (case insensitive)
     * @param string to check
     * @param search to be replaced
     * @param replace to replace with
     * @param max only replace the first max occurrences
     * @return updated string (null if string == null)
     */
    public static String replaceCi(String string, String search, String replace, int max) {
        return replace(string, search, replace, max, true, false);
    }

    /**
     * Replace a word (words are separated by spaces inside a string) with something in a string (case sensitive)
     * @param string to check
     * @param search to be replaced
     * @param replace to replace with
     * @return updated string (null if string == null)
     */
    public static String replaceWord(String string, String search, String replace) {
        return replace(string, search, replace, -1, false, true);
    }

    /**
     * Replace a word (words are separated by spaces inside a string) with something in a string (case insensitive)
     * @param string to check
     * @param search to be replaced
     * @param replace to replace with
     * @return updated string (null if string == null)
     */
    public static String replaceWordIgnoreCase(String string, String search, String replace) {
        return replace(string, search, replace, -1, true, true);
    }

    public static String replace(String string, String searchString, String replace, int max, boolean ignoreCase, boolean isWord) {
        if (isEmpty(string) || searchString == null || searchString.length() == 0 || replace == null || max == 0) {
             return string;
        }

        if (isWord) {
            if (string.startsWith(searchString)) {
                string = replace + string.substring(searchString.length());
            }
            searchString = " " + searchString;
        }
        String searchText = string;

        if (ignoreCase) {
            searchText = searchText.toLowerCase();
            searchString = searchString.toLowerCase();
        }
        int start = 0;
        int end = searchText.indexOf(searchString, start);
        if (end == INDEX_NOT_FOUND) {
            return string;
        }
        int replLength = searchString.length();
        int increase = replace.length() - replLength;
        increase = Math.max(increase, 0);
        increase *= max < 0 ? 16 : Math.min(max, 64);
        StringBuilder buf = new StringBuilder(string.length() + increase);
        while (end != INDEX_NOT_FOUND) {
            buf.append(string, start, end).append(replace);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = searchText.indexOf(searchString, start);
        }
        buf.append(string, start, string.length());
        return buf.toString();
    }

    /**
     * Change the first char of a string to lower case
     * @param string base
     * @return updated string (null if string == null)
     */
    public static String firstCharToLowerCase(String string) {
        if (isEmpty(string)) {
            return string;
        }
        return string.length() == 1 ? string.toLowerCase() : Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }

    /**
     * Change the first char of a string to upper case
     * @param string base
     * @return updated string (null if string == null)
     */
    public static String firstCharToUpperCase(String string) {
        if (isEmpty(string)) {
            return string;
        }
        return string.length() == 1 ? string.toUpperCase() : Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    /**
     * Change the first char of a string to lower case
     * @param string base
     * @return false if string == null or "" or "  " or contains at least a char that is not a number or "+", "-", "/", "."
     */
    public static boolean isNumeric(String string) {
        if (isEmpty(string)) {
            return false;
        }
        string = StringUtil.remove(string, '+', '-', '/');
        try {
            Double.parseDouble(string);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }

    public static boolean isBool(String string) {
        if (isEmpty(string)) {
            return false;
        }
        if (string.equals("1") || string.equalsIgnoreCase("true") || string.equalsIgnoreCase("yes")
            || string.equalsIgnoreCase("بله") || string.equalsIgnoreCase("بلی") || string.equalsIgnoreCase("on")) {
            return true;
        }
        if (string.equals("0") || string.equalsIgnoreCase("false") || string.equalsIgnoreCase("no")
            || string.equalsIgnoreCase("نه") || string.equalsIgnoreCase("خیر") || string.equalsIgnoreCase("off")) {
            return true;
        }
        return false;
    }

    /**
     * Get a part of string that is between two strings
     * @param string base
     * @param start start poring
     * @param end end point
     * @return the part of the string between start and end, not including start and end (null if string == null)
     */
    public static String getBetween(String string, String start, String end) {
        if (isEmpty(string) || start == null || end == null) {
            return string;
        }
        String result = string.substring(string.indexOf(start) + start.length());
        result = result.substring(0, result.indexOf(end));
        return result;
    }

    /**
     * Compare two strings (case sensitive)
     * @param sA first string
     * @param sB string to be compared against sB
     * @return
     * (0 if sA and sB == null)
     * (1 if sB == null)
     * (-1 if sA == null)
     * (0 "abcd", "abcd")
     * (1 "abcddd", "abcd")
     * (-1 "abcd", "abcddd")
     * (1 "bcd", "abc")
     * (1 "bcd", "abcdef")
     * (1 "abcde", "abcd")
     * (-1 "abc", "bcd")
     * (-1 "abcdef", "bcd")
     * (-1 "abcd", "abcde")
     */
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
        for (int i = 0, l = Math.min(lenA, lenB); i < l; ++i) {
            if (sA.charAt(i) < sB.charAt(i)) {
                return -1;
            } else if (sA.charAt(i) > sB.charAt(i)) {
                return 1;
            }
        }
        return Integer.compare(lenA, lenB);
    }

    /**
     * Compare two strings (case insensitive)
     * @param sA first string
     * @param sB string to be compared against sB
     * @return
     * (0 if sA and sB == null)
     * (1 if sB == null)
     * (-1 if sA == null)
     * (0 "abcd", "abcd")
     * (1 "abcddd", "abcd")
     * (-1 "abcd", "abcddd")
     * (1 "bcd", "abc")
     * (1 "bcd", "abcdef")
     * (1 "abcde", "abcd")
     * (-1 "abc", "bcd")
     * (-1 "abcdef", "bcd")
     * (-1 "abcd", "abcde")
     */
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
        for (int i = 0, l = Math.min(lenA, lenB); i < l; ++i) {
            if (sA.charAt(i) < sB.charAt(i)) {
                return -1;
            } else if (sA.charAt(i) > sB.charAt(i)) {
                return 1;
            }
        }
        return Integer.compare(lenA, lenB);
    }

    /**
     * Convert object to char
     * @param obj string or number or char or boolean to convert
     * @return null if obj == null or object is not convertible
     */
    public static Character toCharacter(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Character) {
            return (Character) obj;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj ? '1' : '0';
        }
        return toCharacter(obj.toString());
    }

    /**
     * Get value or default value if null
     * @param value
     * @param defaultValue
     * @return value or default value if null
     */
    public static String value(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }
}
