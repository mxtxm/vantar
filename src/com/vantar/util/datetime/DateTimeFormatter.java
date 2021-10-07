package com.vantar.util.datetime;

import com.vantar.locale.VantarKey;
import com.vantar.util.string.Persian;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;


public class DateTimeFormatter {

    public int year;
    public int month;
    public int day;
    public int hour;
    public int minute;
    public int second;
    public int offset;
    public String dateFormat;
    public String pattern;

    private Set<VantarKey> errors;
    private final boolean hasTime;


    public DateTimeFormatter() {
        hasTime = true;
    }

    public DateTimeFormatter(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), TimeZone.getDefault().toZoneId());
        year = dateTime.getYear();
        month = dateTime.getMonthValue();
        day = dateTime.getDayOfMonth();
        hour = dateTime.getHour();
        minute = dateTime.getMinute();
        second = dateTime.getSecond();
        hasTime = true;
    }

    public DateTimeFormatter(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
        hasTime = false;
    }

    public void putDate(DateTimeFormatter dateTimeFormatter) {
        if (dateTimeFormatter == null) {
            addError(VantarKey.INVALID_DATE);
            return;
        }
        year = dateTimeFormatter.year;
        month = dateTimeFormatter.month;
        day = dateTimeFormatter.day;
    }

    public boolean validateTime() {
        return hour >= 0 && hour < 25 && minute >= 0 && minute < 61 && second >= 0 && second < 61;
    }

    public boolean validateDate() {
        return (day < 32 && day > 0) && (month > 0 && month < 13);
    }

    protected void addError(VantarKey error) {
        if (errors == null) {
            errors = new HashSet<>();
        }
        errors.add(error);
    }

    public boolean hasErrors() {
        return errors != null;
    }

    public Set<VantarKey> getErrors() {
        return errors;
    }

    protected String toString(String type) {
        StringBuilder dateTime = new StringBuilder(20);

        if (!type.equals(DateTime.TIME)) {
            dateTime.append(year);
            dateTime.append('-');
            if (month < 10) {
                dateTime.append('0');
            }
            dateTime.append(month);
            dateTime.append('-');
            if (day < 10) {
                dateTime.append('0');
            }
            dateTime.append(day);
        }

        if (!type.equals(DateTime.DATE)) {
            if (type.equals(DateTime.TIME)) {
                pattern = "HH:mm:ss";
            } else {
                dateTime.append(' ');
                pattern = "yyyy-MM-dd HH:mm:ss";
            }
            if (hour < 10) {
                dateTime.append('0');
            }
            dateTime.append(hour);

            dateTime.append(':');
            if (minute < 10) {
                dateTime.append('0');
            }
            dateTime.append(minute);

            dateTime.append(':');
            if (second < 10) {
                dateTime.append('0');
            }
            dateTime.append(second);
        } else {
            pattern = "yyyy-MM-dd";
        }

        return dateTime.toString();
    }

    public String toString() {
        return toString(DateTime.TIMESTAMP);
    }

    public String getDateTime() {
        return toString();
    }

    public String getDateTimePersianAsString() {
        String v = PersianDateUtil.toPersian(year, month, day).getDate() + " " + getTimeHms();
        pattern = "yyyy-MM-dd HH:mm:ss";
        return v;
    }

    public DateTimeFormatter getDateTimePersian() {
        DateTimeFormatter x = PersianDateUtil.toPersian(year, month, day);
        x.year = year;
        x.month = month;
        x.day = day;
        return x;
    }

    public String getDate() {
        return toString(DateTime.DATE);
    }

    public String getDatePersian() {
        pattern = "yyyy-MM-dd";
        return PersianDateUtil.toPersian(year, month, day).getDate();
    }

    public String getDatePersianYm() {
        pattern = "yyyy-MM";
        DateTimeFormatter x = PersianDateUtil.toPersian(year, month, day);
        return x.year + "/" + x.month;
    }

    public String getDatePersianYmonth() {
        pattern = "yyyy-MM";
        DateTimeFormatter x = PersianDateUtil.toPersian(year, month, day);
        return x.year + " " + DateTimeNormalizer.MONTH_NAMES_FA[x.month];
    }

    public String getDateHm() {
        String v = toString(DateTime.DATE) + " " + getTimeHm();
        pattern = "yyyy-MM-dd HH:mm";
        return v;
    }

    public String getDateTimePersianHm() {
        String v = PersianDateUtil.toPersian(year, month, day).getDate() + " " + getTimeHm();
        pattern = "yyyy-MM-dd HH:mm";
        return v;
    }

    public String getTimeHms() {
        return toString(DateTime.TIME);
    }

    public String getTimeHm() {
        StringBuilder sb = new StringBuilder(8);
        if (hour < 10) {
            sb.append('0');
        }
        sb.append(hour).append(':');
        if (minute < 10) {
            sb.append('0');
        }
        sb.append(minute);
        pattern = "HH:m";
        return sb.toString();
    }

    public String getDateStyled() {
        pattern = "d MMM yyyy";
        return day + " " + DateTimeNormalizer.MONTH_NAMES_EN[month] + " " + year;
    }

    /**
     *۷ اردیبهشت ۱۳۵۸
     */
    public String getDatePersianStyled() {
        pattern = "d MMM yyyy";
        DateTimeFormatter date = PersianDateUtil.toPersian(year, month, day);
        return
            Persian.Number.toPersian(Integer.toString(date.day)) + " " +
            DateTimeNormalizer.MONTH_NAMES_FA[date.month] + " " +
            Persian.Number.toPersian(Integer.toString(date.year));
    }

    public String getDateTimeStyled() {
        String v = getDateStyled() + (hasTime ? (" - " + getTimeHms()) : "");
        pattern = "d MMM yyyy - H:m:s";
        return v;
    }

    /**
     *۱۲:۱:۳ - ۷ اردیبهشت ۱۳۵۸
     */
    public String getDateTimePersianStyled() {
        String v = getDatePersianStyled() + (hasTime ? (" - " + Persian.Number.toPersian(getTimeHms())) : "");
        pattern = "d MMM yyyy - H:m:s";
        return v;
    }

    public String getDateTimeStyledHm() {
        String v = getDateStyled() + (hasTime ? (" - " + getTimeHm()) : "");
        pattern = "d MMM yyyy - H:m";
        return v;
    }

    /**
     *۱۲:۱ - ۷ اردیبهشت ۱۳۵۸
     */
    public String getDateTimePersianStyledHm() {
        String v = getDatePersianStyled() + (hasTime ? (" - " + Persian.Number.toPersian(getTimeHm())) : "");
        pattern = "d MMM yyyy - H:m";
        return v;
    }

    public String getAsStringIso() {
        pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        return java.time.format.DateTimeFormatter.ofPattern(pattern)
            .withZone(ZoneOffset.systemDefault())
            .format(
                LocalDateTime
                    .of(year, month, day, hour, minute, second)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            );
    }

    public String getDateTimeSimple() {
        StringBuilder sb = new StringBuilder(20);
        sb.append(year).append('-');
        if (month < 10) {
            sb.append('0');
        }
        sb.append(month).append('-');
        if (day < 10) {
            sb.append('0');
        }
        sb.append(day);

        if (hasTime) {
            sb.append('-');
            if (hour < 10) {
                sb.append('0');
            }
            sb.append(hour).append('-');
            if (minute < 10) {
                sb.append('0');
            }
            sb.append(minute).append('-');
            if (second < 10) {
                sb.append('0');
            }
            sb.append(second);
        }
        pattern = "yyyy-MM-dd-HH-mm-ss";
        return sb.toString();
    }

    public String getDateTimeCompact() {
        StringBuilder sb = new StringBuilder(20);
        sb.append(year);
        if (month < 10) {
            sb.append('0');
        }
        sb.append(month);
        if (day < 10) {
            sb.append('0');
        }
        sb.append(day);

        if (hour < 10) {
            sb.append('0');
        }
        sb.append(hour);
        if (minute < 10) {
            sb.append('0');
        }
        sb.append(minute);
        if (second < 10) {
            sb.append('0');
        }
        sb.append(second);
        pattern = "yyyyMMddHHmmss";
        return sb.toString();
    }

    public long getAsTimestamp() {
        try {
            long ts = LocalDateTime.of(year, month, day, hour, minute, second).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (offset != 0) {
                ts = ts + ((- offset + ZoneId.systemDefault().getRules().getOffset(Instant.now()).getTotalSeconds()) * 1000);
            }
            return ts;
        } catch (java.time.DateTimeException e) {
            addError(VantarKey.INVALID_DATETIME);
        }
        return 0;
    }

    /**
     * how many days, hours, minutes, seconds is
     */
    public static String secondsToDateTime(long seconds) {
        long fragmentSeconds = seconds % 60;
        long minutes = (seconds / 60) % 60;
        long hours = (seconds / (60 * 60)) % 24;
        long days = seconds / (24 * 60 * 60);

        StringBuilder time = new StringBuilder();
        if (days != 0) {
            time.append(days).append("d");
        }
        if (hours != 0) {
            if (time.length() > 0) {
                time.append(", ");
            }
            time.append(hours).append("h");
        }
        if (minutes != 0) {
            if (time.length() > 0) {
                time.append(", ");
            }
            time.append(minutes).append("m");
        }
        if (fragmentSeconds != 0) {
            if (time.length() > 0) {
                time.append(", ");
            }
            time.append(fragmentSeconds).append("s");
        }

        return time.toString();
    }
}
