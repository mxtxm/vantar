package com.vantar.database.nosql.elasticsearch;

import com.vantar.common.VantarParam;
import com.vantar.util.string.StringUtil;


public class IndexUtil {

    public static class Normalize {

        public static String keywordToSearch(String string) {
            return textToSearch(string, true);
        }

        public static String keywordsToSearch(String string) {
            return textToSearch(string, false);
       }

        public static String textToSearch(String string) {
            return textToSearch(string, false);
        }

        private static String textToSearch(String string, boolean removeSpaces) {
            string = StringUtil.replace(
                string,
                new String[] {"&",       ",",       "+",       "،",       " و ",     "|",      " یا ", "-"},
                new String[] {"{}AND{}", "{}AND{}", "{}AND{}", "{}AND{}", "{}AND{}", "{}OR{}", "{}OR{}", "{}AND NOT{}"}
            );
            string = string.replaceAll("(\\{}AND\\{})+", "\\{}AND\\{}");
            string = string.replaceAll("(\\{}OR\\{})+",  "\\{}OR\\{}");
            string = string.replaceAll("(\\{}AND NOT\\{})+",  "\\{}AND NOT\\{}");
            return StringUtil.replace(
                string.replaceAll("\\s+", removeSpaces ? "" : " "),
                new String[] {"{}AND{}", "{}OR{}", "{}AND NOT{}",},
                new String[] {" AND ",   " OR ",   " AND NOT ",}
            );
        }
    }

    public static String escape(String string) {
        return StringUtil.replace(
            StringUtil.replace(string, new String[] {"||", "&&"}, " "),
            new char[] {'+', '-', '=', '!', '(', ')', '{', '}', '[', ']', '^', '"', '~', '*', '?', ':', '\\', '>', '<', '/',},
            new String[] {"\\+", "\\-", "\\=", "\\!", "\\(", "\\)", "\\{", "\\}", "\\[", "\\]", "\\^", "\\\"", "\\~", "\\*", "\\?", "\\:", "\\\\", "\\>", "\\<", "\\/",}
        );
    }

    public static String setOperatorToString(String string, String operator) {
        if (string == null || operator == null) {
            return string;
        }

        string = string.replaceAll("\\s{2,}", " ");
        switch (operator) {
            case VantarParam.OPERATOR_AND:
                return StringUtil.replace(string, ' ', " AND ");
            case VantarParam.OPERATOR_OR:
                return StringUtil.replace(string, ' ', " OR ");
            case VantarParam.OPERATOR_PHRASE:
                return '"' + StringUtil.remove(string, '"') + '"';
        }
        string = string.replaceAll("( AND )+", " AND ");
        string = string.replaceAll("( OR )+", " OR ");
        return string;
    }
}