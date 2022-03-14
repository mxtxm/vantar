package com.vantar.util.datetime;

import com.vantar.exception.DateTimeException;
import com.vantar.locale.VantarKey;
import com.vantar.util.string.Persian;
import com.vantar.util.string.StringUtil;
import java.util.Set;


public class DateTimeNormalizer {

    protected static final String[] DAY_NAMES_FA = {
        "",
        "اول",
        "دوم",
        "سوم",
        "چهارم",
        "پنجم",
        "ششم",
        "هفتم",
        "هشتم",
        "نهم",
        "دهم",
        "یازدم",
        "دوازدهم",
        "سیزدهم",
        "چهاردهم",
        "پانزدهم",
        "شانزدهم",
        "هفدهم",
        "هجدهم",
        "نوزدهم",
        "بیستم",
        "بیست و یکم",
        "بیست و دوم",
        "بیست و سوم",
        "بیست و چهارم",
        "بیست و پنجم",
        "بیست و ششم",
        "بیست و هفتم",
        "بیست و هشتم",
        "بیست و نهم",
        "سی ام",
        "سی و یکم",
    };
    protected static final String[] MONTH_NAMES_FA = {
        "",
        "فروردین",
        "اردیبهشت",
        "خرداد",
        "تیر",
        "مرداد",
        "شهریور",
        "مهر",
        "آبان",
        "آذر",
        "دی",
        "بهمن",
        "اسفند",
    };
    protected static final String[] MONTH_NAMES_EN = {
        "",
        "january",
        "february",
        "march",
        "april",
        "may",
        "june",
        "july",
        "august",
        "september",
        "october",
        "november",
        "december",
    };
    protected static final String[] MONTH_NAMES_EN_SHORT = {
        "",
        "jan",
        "feb",
        "mar",
        "apr",
        "may",
        "jun",
        "jul",
        "aug",
        "sep",
        "ocx",
        "nov",
        "dec",
    };
    private static final String[] REMOVE_TOKENS = {
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
        "mon", "tue", "wed", "thu", "fri", "sat", "sun",
        "شنبه", "یک", "دو", "سه", "چهار", "پنج", "جمعه",
        "ب.ظ", "ق.ظ", "am", "a.m.", "pm", "p.m.",
        "'", "\"", "\u200C", "تاریخ", "\t",
    };


    public static DateTimeFormatter fromString(String string, String format) throws DateTimeException {
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatter();
        if (string == null) {
            dateTimeFormatter.addError(VantarKey.INVALID_DATETIME);
            return dateTimeFormatter;
        }
        String dt = string.toLowerCase();

        if (StringUtil.countMatches(dt, '-') == 1 && !dt.contains(" - ")) {
            dt = StringUtil.replace(dt, '-', " - ");
        }

        boolean isPm = dt.contains("pm") || dt.contains("ب.ظ") || dt.contains("p.m.");
        boolean isAm = dt.contains("am") || dt.contains("ق.ظ") || dt.contains("a.m.");
        dt = StringUtil.replace(dt, new char[] {'ي', '،', ',', '/',' ',}, new String[] {"ی", " - ", " - ", "-"," ",});
        for (int k = DAY_NAMES_FA.length -1; k > 0; --k) {
            dt = StringUtil.replace(dt, DAY_NAMES_FA[k], Integer.toString(k));
        }
        dt = StringUtil.remove(dt, REMOVE_TOKENS);
        dt = Persian.Number.toLatin(dt.trim());
        dt = StringUtil.replace(dt, MONTH_NAMES_EN, MONTH_NAMES_EN_SHORT);
        dt = StringUtil.replace(dt, "oct", "ocx");
        dt = StringUtil.replace(dt, "gmt", "z");
        dt = StringUtil.trim(dt, ':', ',', ';', '-', '_', '.', '،');
        dt = dt.replaceAll("\\s{2,}", " - ");

        if (StringUtil.countMatches(dt, " - ") > 1) {
            dt = StringUtil.replace(dt, " - ", "-");
        }

        String[] dateTime;
        if (StringUtil.contains(dt, 't') && !StringUtil.contains(dt, "oct")) {
            dateTime = StringUtil.split(dt, 't');
        } else if (dt.contains(" - ")) {
            dateTime = StringUtil.split(dt, " - ");
        } else if (dt.contains("ساعت")) {
            dateTime = StringUtil.split(dt, "ساعت");
        } else if (StringUtil.countMatches(dt, '-') == 2 && StringUtil.countMatches(dt, ' ') > 1) {
            String[] allParts = StringUtil.split(dt.replaceAll(" +", " "), ' ', 2);
            dateTime = new String[] {allParts[0], allParts[1]};
        } else {
            int spaceCount = StringUtil.countMatches(dt, ' ');

            if (spaceCount == 1) {
                dateTime = StringUtil.split(dt, ' ');
            } else if (spaceCount >= 2) {
                String[] allParts = StringUtil.split(dt.replaceAll(" +", " "), ' ');
                switch (allParts.length) {
                    case 2:
                        dateTime = new String[] {allParts[0] + ' ' + allParts[1]};
                        break;
                    case 3:
                        dateTime = new String[] {allParts[0], allParts[1] + ' ' + allParts[2]};
                        break;
                    case 4:
                        dateTime = new String[] {allParts[0] + ' ' + allParts[1] + ' ' + allParts[2], allParts[3]};
                        break;
                    case 5:
                        dateTime = new String[] {allParts[0] + ' ' + allParts[1] + ' ' + allParts[2], allParts[3] + ' ' + allParts[4]};
                        break;
                    default:
                        dateTime = new String[] {dt};
                }
            } else {
                dateTime = new String[] {dt};
            }
        }

        String datePart;
        if (dateTime.length == 2) {
            datePart = setParsedTime(dateTimeFormatter, dateTime, isPm, isAm);
        } else if (dateTime.length == 1) {
            datePart = dateTime[0];
        } else {
            dateTimeFormatter.addError(VantarKey.INVALID_TIMEZONE);
            throw new DateTimeException(string, dateTimeFormatter.getErrors());
        }

        // date part

        if (datePart == null) {
            dateTimeFormatter.addError(VantarKey.INVALID_DATE);
        } else {
            setParsedDate(dateTimeFormatter, datePart, format);
        }

        Set<VantarKey> errors = dateTimeFormatter.getErrors();
        if (errors != null) {
            throw new DateTimeException(string, dateTimeFormatter.getErrors());
        }
        return dateTimeFormatter;
    }

    /**
     * set time from parts to ymdhms and return what is not time
     */
    private static String setParsedTime(DateTimeFormatter dateTimeFormatter, String[] parts, boolean isPm, boolean isAm) {
        String datePart;
        String timePart;

        boolean containsTimeDelim = StringUtil.contains(parts[0], ':');
        if (containsTimeDelim) {
            datePart = parts[1].trim();
            timePart = parts[0].trim();
        } else {
            datePart = parts[0].trim();
            timePart = parts[1].trim();
            containsTimeDelim = StringUtil.contains(timePart, ':');
        }

        // time zone
        timePart = StringUtil.replace(timePart, "+0000", "z");
        timePart = StringUtil.replace(timePart, "-0000", "z");
        timePart = StringUtil.replace(timePart, "+00:00", "z");
        timePart = StringUtil.replace(timePart, "-00:00", "z");
        timePart = StringUtil.replace(timePart, "+0:0", "z");
        timePart = StringUtil.replace(timePart, "-0:0", "z");
        if (StringUtil.contains(timePart, '-')) {
            String[] zoneTime = StringUtil.split(timePart, '-', 2);
            dateTimeFormatter.offset = -getParsedTimeZone(dateTimeFormatter, zoneTime[1]);
            timePart = zoneTime[0];

        } else if (timePart.contains("+")) {
            String[] zoneTime = StringUtil.split(timePart, '+', 2);
            dateTimeFormatter.offset = getParsedTimeZone(dateTimeFormatter, zoneTime[1]);
            timePart = zoneTime[0];

        } else if (StringUtil.contains(timePart, 'z')) {
            dateTimeFormatter.offset = 1;
            timePart = StringUtil.remove(timePart, 'z');
        } else {
            dateTimeFormatter.offset = 0;
        }

        timePart = StringUtil.split(timePart, '.')[0].trim();
        Integer hour;
        Integer minute;
        Integer seconds;

        if (containsTimeDelim) {
            String[] timeParts = StringUtil.split(timePart, ':');

            int len = timeParts.length;
            hour = StringUtil.toInteger(timeParts[0]);
            minute = len > 1 ? StringUtil.toInteger(timeParts[1]) : new Integer(0);
            seconds = len > 2 ? StringUtil.toInteger(timeParts[2]) : new Integer(0);
        } else {
            switch (timePart.length()) {
                case 1:
                case 2:
                    hour = StringUtil.toInteger(timePart);
                    minute = 0;
                    seconds = 0;
                    break;
                case 4:
                    String string = Character.toString(timePart.charAt(0)) + timePart.charAt(1);
                    hour = StringUtil.toInteger(string);
                    minute = StringUtil.toInteger(Character.toString(timePart.charAt(2)) + timePart.charAt(3));
                    seconds = 0;
                    break;
                case 6:
                    hour = StringUtil.toInteger(Character.toString(timePart.charAt(0)) + timePart.charAt(1));
                    minute = StringUtil.toInteger(Character.toString(timePart.charAt(2)) + timePart.charAt(3));
                    seconds = StringUtil.toInteger(Character.toString(timePart.charAt(4)) + timePart.charAt(5));
                    break;
                default:
                    hour = null;
                    minute = null;
                    seconds = null;
            }
        }

        boolean isValid = hour != null && minute != null && seconds != null;
        if (isValid) {
            dateTimeFormatter.hour = hour;
            dateTimeFormatter.minute = minute;
            dateTimeFormatter.second = seconds;

            if (dateTimeFormatter.hour == 12) {
                if (isAm) {
                    dateTimeFormatter.hour = 0;
                }
            } else if (isPm) {
                dateTimeFormatter.hour += 12;
            }
            if (dateTimeFormatter.hour >= 24) {
                dateTimeFormatter.hour = 0;
            }

            isValid = dateTimeFormatter.validateTime();
        }

        if (!isValid) {
            dateTimeFormatter.hour = 0;
            dateTimeFormatter.minute = 0;
            dateTimeFormatter.second = 0;
            dateTimeFormatter.addError(VantarKey.INVALID_TIME);
        }

        return datePart;
    }

    private static int getParsedTimeZone(DateTimeFormatter dateTimeFormatter, String zone) {
        if (StringUtil.contains(zone, ':')) {
            String[] hm = StringUtil.split(zone, ':');
            if (hm.length == 2) {
                Integer h = StringUtil.toInteger(hm[0]);
                Integer m = StringUtil.toInteger(hm[1]);
                if (h != null && m != null) {
                    return (h * 60 * 60) + (m * 60);
                }
            }
        } else {
            int len = zone.length();
            if (len == 4) {
                Integer hm = StringUtil.toInteger(zone);
                if (hm != null) {
                    return ((hm / 100) * 60 * 60) + ((hm % 100) * 60);
                }
            } else if (len == 2) {
                Integer h = StringUtil.toInteger(zone);
                if (h != null) {
                    return h * 60 * 60;
                }
            }
        }
        dateTimeFormatter.addError(VantarKey.INVALID_TIMEZONE);
        return 0;
    }

    /**
     * allows (yy)yyy-(m)m-(d)d or yyyyymmdd
     */
    private static void setParsedDate(DateTimeFormatter dateTimeFormatter, String datePart, String format) {
        datePart = datePart.trim();
        int yPos = getFormatPosition(format, 'y');
        int mPos = getFormatPosition(format, 'm');
        int dPos = getFormatPosition(format, 'd');

        if (yPos == -1 || mPos == -1 || dPos == -1) {
            yPos = 0;
            mPos = 1;
            dPos = 2;
        }

        // (yy)yy-(m)m-(d)d in any order
        if (datePart.contains("-")) {
            String[] parts = StringUtil.split(datePart, '-');
            if (parts.length != 3) {
                dateTimeFormatter.addError(VantarKey.INVALID_DATE);
                return;
            }

            Integer year = StringUtil.toInteger(parts[yPos].trim());
            Integer month = getMonthValue(parts[mPos].trim());
            Integer day = StringUtil.toInteger(parts[dPos].trim());

            if (year == null || month == null || day == null) {
                dateTimeFormatter.addError(VantarKey.INVALID_DATE);
                return;
            }

            if (day > 100) {
                year += day;
                day = year - day;
                year = year - day;

                yPos += dPos;
                dPos = yPos - dPos;
                yPos = yPos - dPos;
            }
            if (month > 100) {
                year += month;
                month = year - month;
                year = year - month;

                yPos += mPos;
                mPos = yPos - mPos;
                yPos = yPos - mPos;
            }
            if (month > 12) {
                month += day;
                day = month - day;
                month = month - day;

                mPos += dPos;
                dPos = mPos - dPos;
                mPos = mPos - dPos;
            }

            dateTimeFormatter.dateFormat =
                String.valueOf(yPos == 0 ? 'y' : (dPos == 0 ? 'd' : 'm'))
                + (mPos == 1 ? 'm' : 'd')
                + (dPos == 2 ? 'd' : (mPos == 2 ? 'm' : 'y'));

            dateTimeFormatter.day = day;
            dateTimeFormatter.month = month;
            dateTimeFormatter.year = year;
            if (!dateTimeFormatter.validateDate()) {
                dateTimeFormatter.addError(VantarKey.INVALID_DATE);
                return;
            }

            if (year < PersianDateUtil.PERSIAN_YEAR_UPPER_LIMIT) {
                dateTimeFormatter.putDate(PersianDateUtil.toGregorian(year, month, day));
            }
            return;
        }

        // yyyymmdd
        Integer extended = StringUtil.toInteger(datePart);
        if (extended != null) {
            if (yPos == 0 && mPos == 1) {
                dateTimeFormatter.year = extended / 10000;
                dateTimeFormatter.month = (extended % 10000) / 100;
                dateTimeFormatter.day = extended % 100;
            } else if (yPos == 2 && mPos == 1) {
                dateTimeFormatter.day = extended / 1000000;
                dateTimeFormatter.month = (extended % 1000000) / 10000;
                dateTimeFormatter.year = extended % 10000;
            } else {
                dateTimeFormatter.month = extended / 1000000;
                dateTimeFormatter.day = (extended % 1000000) / 10000;
                dateTimeFormatter.year = extended % 10000;
            }

            dateTimeFormatter.dateFormat =
                String.valueOf(yPos == 0 ? 'y' : (dPos == 0 ? 'd' : 'm'))
                + (mPos == 1 ? 'm' : 'd')
                + (dPos == 2 ? 'd' : (mPos == 2 ? 'm' : 'y'));

            if (!dateTimeFormatter.validateDate()) {
                dateTimeFormatter.addError(VantarKey.INVALID_DATE);
                return;
            }

            if (dateTimeFormatter.year < PersianDateUtil.PERSIAN_YEAR_UPPER_LIMIT) {
                dateTimeFormatter.putDate(PersianDateUtil.toGregorian(dateTimeFormatter));
            }

            return;
        }

        // 7 بهمن 1397
        // 1 MONTH-NAME yyyyy
        // day, 1 MONTH-NAME yyyyy (day is removed earlier)
        // MONTH-NAME 1 yyyyy (day is removed earlier)
        String[] parts = StringUtil.split(datePart.replaceAll(" +", " "), ' ');

        if (parts.length != 3) {
            dateTimeFormatter.addError(VantarKey.INVALID_DATE);
            return;
        }

        Integer month = StringUtil.toInteger(parts[1].trim());
        Integer day = StringUtil.toInteger(parts[0].trim());

        if (month == null && day != null) {
            month = getMonthValue(parts[1]);
        } else if (month != null && day == null) {
            day = month;
            month = getMonthValue(parts[0]);
        }

        if (month == null) {
            dateTimeFormatter.addError(VantarKey.INVALID_DATE);
            return;
        }

        Integer year = StringUtil.toInteger(parts[2].trim());
        if (year == null) {
            dateTimeFormatter.addError(VantarKey.INVALID_DATE);
            return;
        }

        if (day > 100) {
            year += day;
            day = year - day;
            year = year - day;
        }

        dateTimeFormatter.year = year;
        dateTimeFormatter.month = month;
        dateTimeFormatter.day = day;

        if (!dateTimeFormatter.validateDate()) {
            dateTimeFormatter.addError(VantarKey.INVALID_DATE);
            return;
        }

        if (year < PersianDateUtil.PERSIAN_YEAR_UPPER_LIMIT) {
            dateTimeFormatter.putDate(PersianDateUtil.toGregorian(dateTimeFormatter));
        }
    }

    private static Integer getMonthValue(String monthStr) {
        if (monthStr == null) {
            return null;
        }
        Integer month = StringUtil.toInteger(monthStr.toLowerCase());
        if (month == null) {
            for (int i = 0, l = MONTH_NAMES_FA.length; i < l; ++i) {
                if (MONTH_NAMES_FA[i].equals(monthStr) || MONTH_NAMES_EN_SHORT[i].equals(monthStr)) {
                    return i;
                }
            }
        }
        return month;
    }

    /**
     * get where to find y m and d in the date ymd dmy
     */
    private static int getFormatPosition(String format, char element) {
        if (format.length() != 3) {
            return -1;
        }
        if (format.charAt(0) == element) {
            return 0;
        }
        if (format.charAt(1) == element) {
            return 1;
        }
        return 2;
    }
}