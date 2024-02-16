package com.vantar.util.string;

/**
 * Persian language utilities
 */
public class Persian {

    /**
     * Check if a string contains latin chars
     * @param word string to check
     * @return true -> contains
     */
    public static boolean containsLatin(String word) {
        for (char c : word.toCharArray()) {
            if (c >= 65 && c <= 90 || c >= 97 && c <= 122) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a string contains persian chars
     * @param word string to check
     * @return true -> contains
     */
    public static boolean containsPersian(String word) {
        for (int i = 0; i < Character.codePointCount(word, 0, word.length()); ++i) {
            int c = word.codePointAt(i);
            if ((c >= 0x0600 && c <= 0x06FF) || (c >= 0xFB50 && c <= 0xFDFF) || (c >= 0xFE70 && c <= 0xFEFF)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Persian numbers
     */
    public static class Number {

        /**
         * Replace persian number chars to latin number chars
         * @param string string to check
         * @return updated string ("" if string == null)
         */
        public static String toLatin(String string) {
            if (StringUtil.isEmpty(string)) {
                return "";
            }
            StringBuilder sb = new StringBuilder(20);
            char[] charArr = string.toCharArray();
            for (char c : charArr) {
                switch (c) {
                    case '۰':
                        sb.append('0');
                        break;
                    case '۱':
                        sb.append('1');
                        break;
                    case '۲':
                        sb.append('2');
                        break;
                    case '۳':
                        sb.append('3');
                        break;
                    case '۴':
                        sb.append('4');
                        break;
                    case '۵':
                        sb.append('5');
                        break;
                    case '۶':
                        sb.append('6');
                        break;
                    case '۷':
                        sb.append('7');
                        break;
                    case '۸':
                        sb.append('8');
                        break;
                    case '۹':
                        sb.append('9');
                        break;
                    default:
                        sb.append(c);
                }
            }
            return sb.toString();
        }

        /**
         * Replace number chars to persian number chars
         * @param string string to check
         * @return updated string ("" if string == null)
         */
        public static String toPersian(String string) {
            if (StringUtil.isEmpty(string)) {
                return "";
            }
            StringBuilder sb = new StringBuilder(20);
            char[] charArr = string.toCharArray();
            for (char c : charArr) {
                switch (c) {
                    case '0':
                        sb.append('۰');
                        break;
                    case '1':
                        sb.append('۱');
                        break;
                    case '2':
                        sb.append('۲');
                        break;
                    case '3':
                        sb.append('۳');
                        break;
                    case '4':
                        sb.append('۴');
                        break;
                    case '5':
                        sb.append('۵');
                        break;
                    case '6':
                        sb.append('۶');
                        break;
                    case '7':
                        sb.append('۷');
                        break;
                    case '8':
                        sb.append('۸');
                        break;
                    case '9':
                        sb.append('۹');
                        break;
                    default:
                        sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    /**
     * Persian numbers to latin numbers, non persian chars to respecting persian chars
     * @param string string to normalize
     * @return updated string ("" if string == null)
     */
    public static String normalize(String string) {
        if (StringUtil.isEmpty(string)) {
            return "";
        }
        return StringUtil.replace(
            string,
            new char[] {
                '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹', '۰',
                '•', '·', '●', '·', '・', '∙', '｡', 'ⴰ', ',', '٬', '٫', '‚', '，', '¬', 'ۀ', 'ە', 'ة', 'ہ', 'ވ', 'ﯙ', 'ۈ',
                'ۋ', 'ۊ', 'ۇ', 'ۏ', 'ۅ', 'ۉ', 'ؤ', 'ﮚ', 'ڤ', 'ۼ', 'ڠ', 'ﻋ', 'ݭ', 'ݜ', 'ښ', 'ڙ', 'ڗ', 'ڒ', 'ڑ', 'ڏ', 'ډ',
                'ٲ', 'ٱ', 'إ', 'ﺍ', 'أ', '٦', '٥', '٤', 'ء', 'ڭ', 'ګ', 'ڪ', 'ك', 'ں', 'ێ', 'ے', 'ي',
            },
            new char[] {
                '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
                '.', '.', '.', '.', '.', '.', '.', '.', '،', '،', '،', '،', '،', ' ', 'ه', 'ه', 'ه', 'ه', 'و', 'و', 'و',
                'و', 'و', 'و', 'و', 'و', 'و', 'و', 'گ', 'ق', 'غ', 'ع', 'ع', 'س', 'س', 'س', 'ر', 'ر', 'ر', 'ر', 'د', 'د',
                'ا', 'ا', 'ا', 'ا', 'ا', '۶', '۵', '۴', 'ئ', 'ک', 'ک', 'ک', 'ک', 'ی', 'ی', 'ی', 'ی',
            }
        );
    }

    public static String numberToMonth(int m) {
        if (m == 1) {
            return " فروردین ";
        } else if (m == 2) {
            return " اردیبهشت ";
        } else if (m == 3) {
            return " خرداد ";
        } else if (m == 4) {
            return " تیر ";
        } else if (m == 5) {
            return " مرداد ";
        } else if (m == 6) {
            return " شهریور ";
        } else if (m == 7) {
            return " مهر ";
        } else if (m == 8) {
            return " آبان ";
        } else if (m == 9) {
            return " آذز ";
        } else if (m == 10) {
            return " دی ";
        } else if (m == 11) {
            return " بهمن ";
        } else if (m == 12) {
            return " اسفند ";
        }
        return "-";
    }

}