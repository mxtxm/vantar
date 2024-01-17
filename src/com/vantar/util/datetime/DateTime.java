package com.vantar.util.datetime;

import com.vantar.exception.DateTimeException;
import com.vantar.locale.Locale;
import com.vantar.util.string.StringUtil;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;


public class DateTime {

    public static final String TIMESTAMP = "timestamp";
    public static final String DATE = "date";
    public static final String TIME = "time";
    public static final short BIGGER = 1;
    public static final short SMALLER = -1;
    public static final short EQUAL = 0;
    public static final String DEFAULT_DATE_FORMAT = "ymd";

    private Long timestamp;
    private String type = TIMESTAMP;
    private String dateFormat = DEFAULT_DATE_FORMAT;
    private DateTimeFormatter formatter;

    // construct

    public DateTime() {
        setToNow();
    }

    public DateTime(DateTime dateTime) {
        timestamp = dateTime.timestamp;
        type = dateTime.type;
        dateFormat = dateTime.dateFormat;
    }

    public DateTime(long timestamp) {
        setTimeStamp(timestamp);
    }

    public DateTime(Long timestamp) {
        setTimeStamp(timestamp);
    }

    public DateTime(java.util.Date date) {
        setTimeStamp(date.getTime());
    }

    public DateTime(java.sql.Date date) {
        setTimeStamp(date.getTime());
    }

    public DateTime(java.sql.Timestamp timestamp) {
        setTimeStamp(timestamp.getTime());
    }

    public DateTime(java.sql.Time time) {
        setTimeStamp(time.getTime());
    }

    public DateTime(DateTimeFormatter dateTimeFormatter) {
        formatter = dateTimeFormatter;
        if (formatter.year < PersianDateUtil.PERSIAN_YEAR_UPPER_LIMIT) {
            formatter = PersianDateUtil.toGregorian(formatter);
        }
        updateTimeStamp(
            LocalDateTime
                .of(
                    formatter.year, formatter.month, formatter.day,
                    formatter.hour, formatter.minute, formatter.second)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        );
    }

    public DateTime(int year, int month, int day) {
        this(year, month, day, 0, 0, 0);
    }

    public DateTime(int year, int month, int day, int hour, int minute, int second) {
        if (year < PersianDateUtil.PERSIAN_YEAR_UPPER_LIMIT) {
            formatter = PersianDateUtil.toGregorian(year, month, day);
            year = formatter.year;
            month = formatter.month;
            day = formatter.day;
        } else {
            formatter = null;
        }
        updateTimeStamp(
            LocalDateTime
                .of(year, month, day, hour, minute, second)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        );
    }

    public DateTime(LocalDateTime dateTime) {
        setTimeStamp(dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    public DateTime(String dateTime) throws DateTimeException {
        if (dateTime.equalsIgnoreCase("now")) {
            setToNow();
        }
        fromString(dateTime, DEFAULT_DATE_FORMAT);
    }

    public DateTime(String dateTime, String format) throws DateTimeException {
        fromString(dateTime, format);
    }

    // set time

    public DateTime setToNow() {
        setTimeStamp(Instant.now().toEpochMilli());
        return this;
    }

    public DateTime setTimeStamp(long timestamp) {
        updateTimeStamp(timestamp);
        formatter = null;
        return this;
    }

    private DateTime updateTimeStamp(long timestamp) {
        if (timestamp <= Integer.MAX_VALUE) {
            timestamp *= 1000;
        }
        this.timestamp = timestamp;
        return this;
    }

    public DateTime fromString(String string, String format) throws DateTimeException {
        timestamp = StringUtil.toLong(string);
        if (timestamp != null) {
            setTimeStamp(timestamp);
            return this;
        }
        if (StringUtil.isEmpty(format)) {
            format = DEFAULT_DATE_FORMAT;
        }
        formatter = DateTimeNormalizer.fromString(string, format);

        if (formatter.hasErrors()) {
            throw new DateTimeException(string, formatter.getErrors());
        }
        dateFormat = formatter.dateFormat;
        setTimeStamp(formatter.getAsTimestamp());
        return this;
    }

    public DateTime setTimeZoneOffset(String offset) {
        int sign = StringUtil.contains(offset, '-') ? -1 : 1;
        offset = StringUtil.remove(offset, '-', '+');

        String[] parts = StringUtil.split(offset, ':');
        if (parts.length > 3 || parts.length == 0) {
            return this;
        }

        Integer h = StringUtil.toInteger(parts[0]);
        if (h == null) {
            return this;
        }

        Integer m;
        if (parts.length > 1) {
            m = StringUtil.toInteger(parts[1]);
            if (m == null) {
                return this;
            }
        } else {
            m = 0;
        }

        Integer s;
        if (parts.length > 2) {
            s = StringUtil.toInteger(parts[2]);
            if (s == null) {
                return this;
            }
        } else {
            s = 0;
        }

        addHours(sign * h);
        addMinutes(sign * m);
        addSeconds(sign * s);
        return this;
    }

    public DateTime addDays(long v) {
        timestamp = Timestamp.valueOf(new Timestamp(timestamp).toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDateTime().plusDays(v)).getTime();
        formatter = null;
        return this;
    }

    public DateTime addHours(long v) {
        return addSeconds(v * 60 * 60);
    }

    public DateTime addMinutes(long v) {
        return addSeconds(v * 60);
    }

    public DateTime addSeconds(long v) {
        timestamp += v * 1000;
        formatter = null;
        return this;
    }

    public DateTime decreaseDays(long v) {
        return addDays(-v);
    }

    public DateTime decreaseHours(long v) {
        return addHours(-v);
    }

    public DateTime decreaseMinutes(long v) {
        return addMinutes(-v);
    }

    public DateTime decreaseSeconds(long v) {
        return addSeconds(-v);
    }

    public void setMax(DateTime time) {
        if (time.timestamp > timestamp) {
            setTimeStamp(time.timestamp);
        }
    }

    public void setMin(DateTime time) {
        if (time.timestamp < timestamp) {
            setTimeStamp(time.timestamp);
        }
    }

    public DateTime truncateTime() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        setTimeStamp(cal.getTimeInMillis());
        return this;
    }

    public void setType(String type) {
        this.type = type;
        if (type.equals(DATE)) {
            try {
                fromString(toString(), null);
            } catch (DateTimeException dateTimeException) {
                // pass
            }
            formatter = null;
        }
    }

    public String getType() {
        return type;
    }

    // get as
    public long getAsTimestampMilli() {
        return timestamp;
    }

    public long getDateAsTimestampMilli() {
        long oldTimestamp = timestamp;
        truncateTime();
        long timestampDate = timestamp;
        timestamp = oldTimestamp;
        return timestampDate;
    }

    public Long getAsTimestamp() {
        return timestamp == null ? null : timestamp / 1000;
    }

    public long getDateAsTimestamp() {
        return getDateAsTimestampMilli() / 1000;
    }

    public java.util.Date getAsDate() {
        return new java.util.Date(timestamp);
    }

    public java.sql.Date getAsSqlDate() {
        return new java.sql.Date(timestamp);
    }

    public java.sql.Timestamp getAsSqlTimestamp() {
        return new java.sql.Timestamp(timestamp);
    }

    public java.sql.Time getAsSqlTime() {
        return new java.sql.Time(timestamp);
    }

    public LocalDateTime getAsLocalDateTime() {
        return LocalDateTime
                   .ofInstant(
                       Instant.ofEpochMilli(timestamp),
                       TimeZone.getDefault().toZoneId()
                   );
    }

    public DateTimeFormatter formatter() {
        if (formatter == null) {
            formatter = timestamp == null ? new DateTimeFormatter() : new DateTimeFormatter(timestamp);
        }
        return formatter;
    }

    public String toString() {
        String v = formatter().toString(type);
        if ("fa".equals(Locale.getSelectedLocale())) {
            if (type.equals(TIMESTAMP)) {
                return formatter().getDateTimePersianAsString();
            }
            if (type.equals(DATE)) {
                return formatter().getDatePersian();
            }
        }
        return v;
    }

    public long secondsFromNow() {
        return (timestamp - Instant.now().toEpochMilli()) / 1000;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    // compare

    public long diffSecondsRaw(DateTime dateTime) {
        if (dateTime.timestamp == null) {
            return 0;
        }
        return (timestamp - dateTime.timestamp) / 1000;
    }

    public int diffMinutesRaw(DateTime dateTime) {
        return (int) (diffSecondsRaw(dateTime) / 60);
    }

    public int diffHoursRaw(DateTime dateTime) {
        return diffMinutesRaw(dateTime) / 60;
    }

    public int diffDaysRaw(DateTime dateTime) {
        return diffHoursRaw(dateTime) / 24;
    }

    public long diffSeconds(DateTime dateTime) {
        if (dateTime.timestamp == null) {
            return 0;
        }
        return Math.abs(timestamp - dateTime.timestamp) / 1000;
    }

    public int diffMinutes(DateTime dateTime) {
        return (int) (diffSeconds(dateTime) / 60);
    }

    public int diffHours(DateTime dateTime) {
        return diffMinutes(dateTime) / 60;
    }

    public int diffDays(DateTime dateTime) {
        return diffHours(dateTime) / 24;
    }

    public long decreaseSeconds(DateTime dateTime) {
        if (dateTime.timestamp == null) {
            return 0;
        }
        return (timestamp - dateTime.timestamp) / 1000;
    }

    public int decreaseMinutes(DateTime dateTime) {
        return (int) (decreaseSeconds(dateTime) / 60);
    }

    public int decreaseHours(DateTime dateTime) {
        return decreaseMinutes(dateTime) / 60;
    }

    public int decreaseDays(DateTime dateTime) {
        return decreaseHours(dateTime) / 24;
    }



    public int than(DateTime dateTime) {
        if (dateTime.timestamp == null || timestamp > dateTime.timestamp) {
            return BIGGER;
        } else if (timestamp < dateTime.timestamp) {
            return SMALLER;
        }
        return EQUAL;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }
        return timestamp.equals(((DateTime) obj).timestamp);
    }

    @Override
    public int hashCode() {
        return timestamp.hashCode();
    }

    public boolean isAfter(DateTime dateTime) {
        return dateTime.timestamp != null && timestamp > dateTime.timestamp;
    }

    public boolean isAfterOrEqual(DateTime dateTime) {
        return dateTime.timestamp != null && timestamp >= dateTime.timestamp;
    }

    public boolean isBefore(DateTime dateTime) {
        return dateTime.timestamp != null && timestamp < dateTime.timestamp;
    }

    public boolean isBeforeOrEqual(DateTime dateTime) {
        return dateTime.timestamp != null && timestamp <= dateTime.timestamp;
    }

    public boolean isBetween(DateTime dateTimeA, DateTime dateTimeB) {
        return dateTimeA.timestamp != null && timestamp > dateTimeA.timestamp
            && dateTimeB.timestamp != null && timestamp < dateTimeB.timestamp;
    }

    public boolean isBetweenOrEqual(DateTime dateTimeA, DateTime dateTimeB) {
        return dateTimeA.timestamp != null && timestamp >= dateTimeA.timestamp
            && dateTimeB.timestamp != null && timestamp <= dateTimeB.timestamp;
    }

    public boolean hasSecondsElapsed(long elapse) {
        return (Math.abs(Instant.now().toEpochMilli() - this.timestamp) >= (elapse * 1000));
    }

    public boolean hasSecondsElapsed(DateTime dateTime, long elapse) {
        return dateTime.timestamp != null && (Math.abs(this.timestamp - dateTime.timestamp) >= (elapse * 1000));
    }

    public boolean hasMinutesElapsed(int elapse) {
        return (Math.abs(Instant.now().toEpochMilli() - this.timestamp) >= (elapse * 1000 * 60));
    }

    public boolean hasMinutesElapsed(DateTime dateTime, int elapse) {
        return dateTime.timestamp != null && (Math.abs(this.timestamp - dateTime.timestamp) >= (elapse * 1000 * 60));
    }

    public boolean hasHoursElapsed(int elapse) {
        return (Math.abs(Instant.now().toEpochMilli() - this.timestamp) >= (elapse * 1000 * 60 * 60));
    }

    public boolean hasHoursElapsed(DateTime dateTime, int elapse) {
        return dateTime.timestamp != null && (Math.abs(this.timestamp - dateTime.timestamp) >= (elapse * 1000 * 60 * 60));
    }

    public boolean isToday() {
        DateTime current = new DateTime();
        DateTimeFormatter thisDate = formatter();
        return thisDate.year == current.formatter().year
                   && thisDate.month == current.formatter().month
                   && thisDate.day == current.formatter().day;
    }

    // static

    public static long getTimestamp() {
        return Instant.now().toEpochMilli() / 1000;
    }

    public static long getTimestampMs() {
        return Instant.now().toEpochMilli();
    }

    public static long getTodayTimestamp() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 1);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis() / 1000;
    }

    public static DateTime getToday() {
        return new DateTime(getTodayTimestamp());
    }

    public static long getTomorrowTimestamp() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 1);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, 1);
        return cal.getTimeInMillis() / 1000;
    }

    public static DateTime getTomorrow() {
        return new DateTime(getTomorrowTimestamp());
    }

    public static long getYesterdayTimestamp() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, -1);
        return cal.getTimeInMillis() / 1000;
    }

    public static DateTime getYesterday() {
        return new DateTime(getYesterdayTimestamp());
    }

    public static DateTime toDateTime(Object value) throws DateTimeException {
        if (value == null || value instanceof DateTime) {
            return (DateTime) value;
        }
        String stringValue = value.toString();
        if (StringUtil.isEmpty(stringValue)) {
            return null;
        }
        return new DateTime(stringValue);
    }
}
