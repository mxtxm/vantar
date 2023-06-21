package com.vantar.util.datetime;

import com.vantar.admin.model.Admin;
import com.vantar.exception.DateTimeException;
import com.vantar.util.json.Json;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import java.util.*;

/**
 * Date-time range data structure
 */
public class DateTimeRange {

    /**
     * lower bound
     */
    public DateTime dateMin;
    /**
     * upper bound
     */
    public DateTime dateMax;

    /**
     * Set data
     * @param dateMin lower bound
     * @param dateMax upper bound
     */
    public DateTimeRange(DateTime dateMin, DateTime dateMax) {
        this.dateMin = dateMin;
        this.dateMax = dateMax;
    }

    /**
     * Set data from String (timestamp or date value)
     * @param dateMin lower bound
     * @param dateMax upper bound
     */
    public DateTimeRange(String dateMin, String dateMax) throws DateTimeException {
        this.dateMin = new DateTime(dateMin);
        this.dateMax = new DateTime(dateMax);
    }

    /**
     * Set data from String (timestamp or date value)
     * @param range lower-bound,upper-bound e.g: "2021-01-01,2021-02-02"
     */
    public DateTimeRange(String range) throws DateTimeException {
        if (range == null) {
            return;
        }
        if (range.startsWith("{") && range.endsWith("}")) {
            DateTimeRange temp = Json.d.fromJson(range, DateTimeRange.class);
            dateMin = temp.dateMin;
            dateMax = temp.dateMax;
            return;
        }
        String[] parts = StringUtil.splits(range, ',', '|', '،');
        if (parts.length != 2) {
            return;
        }
        dateMin = new DateTime(parts[0]);
        dateMax = new DateTime(parts[1]);
    }

    /**
     * Set data, lower bound=not upper bound=tomorrow
     */
    public DateTimeRange() {

    }

    @SuppressWarnings("unchecked")
    public static DateTimeRange toDateTimeRange(Object value) throws DateTimeException {
        if (value == null || value instanceof DateTimeRange) {
            return (DateTimeRange) value;
        }

        if (value instanceof Map) {
            Map<String, ?> map = (Map<String, ?>) value;
            Object dateMin = map.get("dateMin");
            Object dateMax = map.get("dateMax");
            if (dateMin == null || dateMax == null) {
                return null;
            }
            return new DateTimeRange(dateMin.toString(), dateMax.toString());
        }


        String stringValue = value.toString();
        if (StringUtil.isEmpty(stringValue)) {
            return null;
        }
        return new DateTimeRange(stringValue);
    }

    /**
     * Set data, lower-bound=now upper-bound=tomorrow
     */
    public DateTimeRange setDefaultRange() {
        this.dateMin = new DateTime();
        this.dateMax = new DateTime().addDays(1);
        return this;
    }


    /**
     * Truncate time and normalize the range
     */
    public DateTimeRange adjustDateRange() {
        if (dateMin == null || dateMax == null) {
            return this;
        }
        dateMax.addDays(1);
        dateMin.truncateTime();
        dateMax.truncateTime();
        if (dateMax.equals(dateMin)) {
            dateMax.addDays(1);
            dateMax.truncateTime();
            if (dateMax.equals(dateMin)) {
                dateMax.addDays(1);
                dateMax.truncateTime();
            }
        }
        dateMax.decreaseSeconds(1);
        return this;
    }

    /**
     * Normalize the range
     */
    public DateTimeRange adjustDateTimeRange() {
        if (dateMin == null || dateMax == null) {
            return this;
        }
        if (dateMax.equals(dateMin)) {
            dateMax.addDays(1);
            if (dateMax.equals(dateMin)) {
                dateMax.addDays(1);
            }
        }
        dateMin.addSeconds(1);
        dateMax.decreaseSeconds(1);
        return this;
    }

    /**
     * Check if the range is valid (values are set and lower bound is before upper bound)
     * @return true if range is valid
     */
    public boolean isValid() {
        return dateMin != null && dateMax != null && dateMax.isAfterOrEqual(dateMin);
    }

    /**
     * Check if at least one of the range bounds is null
     * @return true if empty
     */
    public boolean isEmpty() {
        return dateMin == null || dateMax == null;
    }

    /**
     * Check both range bounds are not null
     * @return true if not empty
     */
    public boolean isNotEmpty() {
        return dateMin != null && dateMax != null;
    }

    /**
     * Get a list of years between the bounds
     * @return list of years
     */
    public List<Integer> getYearsBetween() {
        List<Integer> values = new ArrayList<>(30);
        for (int i = dateMin.formatter().year; i <= dateMax.formatter().year; ++i) {
            values.add(i);
        }
        return values;
    }

    /**
     * Get a list of months between the bounds
     * @return list of months
     */
    public List<Integer> getMonthsBetween() {
        List<Integer> values = new ArrayList<>(100);
        if (dateMin.formatter().year == dateMax.formatter().year) {
            for (int i = dateMin.formatter().month, l = dateMax.formatter().month ; i <= l ; ++i) {
                values.add(i);
            }
            return values;
        }

        for (int i = dateMin.formatter().month ; i <= 12 ; ++i) {
            values.add(i);
        }
        for (int i = dateMin.formatter().year + 1, yMax = dateMax.formatter().year ; i < yMax ; ++i) {
            for (int j = 1; j <= 12; ++j) {
                values.add(j);
            }
        }
        for (int i = 1, mMax = dateMax.formatter().month ; i <= mMax ; ++i) {
            values.add(i);
        }
        return values;
    }


    /**
     * Get a list of year-months between the bounds. e.i: 202201, 202202, ...
     * @return list of months
     */
    public List<Integer> getYearMonthsBetween() {
        return getYearMonthsBetween(false);
    }

    public List<Integer> getYearMonthsBetweenPersian() {
        return getYearMonthsBetween(true);
    }

    private List<Integer> getYearMonthsBetween(boolean persian) {
        DateTimeFormatter minF;
        DateTimeFormatter maxF;
        if (persian) {
            minF = dateMin.formatter().getDateTimePersian();
            maxF = dateMax.formatter().getDateTimePersian();
        } else {
            minF = dateMin.formatter();
            maxF = dateMax.formatter();
        }
        int minY = minF.year;
        int maxY = maxF.year;
        int minM = minF.month;
        int maxM = maxF.month;

        List<Integer> values = new ArrayList<>(100);
        if (minY == maxY) {
            for (int i = minM; i <= maxM; ++i) {
                values.add(minY * 100 + i);
            }
            return values;
        }

        for (int i = minM ; i <= 12 ; ++i) {
            values.add(minY * 100 + i);
        }
        for (int i = minY + 1 ; i < maxY ; ++i) {
            for (int j = 1; j <= 12; ++j) {
                values.add(i * 100 + j);
            }
        }
        for (int i = 1; i <= maxM; ++i) {
            values.add(maxY * 100 + i);
        }
        return values;
    }

    /**
     * Get a list of year-month-days between the bounds. e.i: 20220101, 20220230, ...
     * @return list of days
     */
    public List<Integer> getYearMonthDaysBetween() {
        return getYearMonthDaysBetweenX(false);
    }

    public List<Integer> getYearMonthDaysBetweenPersian() {
        return getYearMonthDaysBetweenX(true);
    }

    private List<Integer> getYearMonthDaysBetweenX(boolean persian) {
        DateTimeFormatter minF;
        DateTimeFormatter maxF;
        if (persian) {
            minF = dateMin.formatter().getDateTimePersian();
            maxF = dateMax.formatter().getDateTimePersian();
        } else {
            minF = dateMin.formatter();
            maxF = dateMax.formatter();
        }
        int minY = minF.year;
        int maxY = maxF.year;
        int minM = minF.month;
        int maxM = maxF.month;
        int minD = minF.day;
        int maxD = maxF.day;

        List<Integer> values = new ArrayList<>((maxY - minY) * 365);

        for (int y = minY; y <= maxY; ++y) {
            int firstMonth = -1;
            int lastMonth = -1;
            if (y == minY) {
                firstMonth = minM;
            }
            if (y == maxY) {
                lastMonth = maxM;
            }
            if (firstMonth == -1) {
                firstMonth = 1;
            }
            if (lastMonth == -1) {
                lastMonth = 12;
            }

            for (int m = firstMonth; m <= lastMonth; ++m) {
                int firstDay = -1;
                int lastDay = -1;
                if (y == minY && m == minM) {
                    firstDay = minD;
                }
                if (y == maxY && m == maxM) {
                    lastDay = maxD;
                }
                if (firstDay == -1) {
                    firstDay = 1;
                }
                if (lastDay == -1) {
                    lastDay = persian ?
                        DateTimeNormalizer.getMonthDayCountPersian(y, m) :
                        DateTimeNormalizer.getMonthDayCount(y, m);
                }

                for (int d = firstDay; d <= lastDay; ++d) {
                    values.add(y * 10000 + m * 100 + d);
                }
            }
        }
        return values;
    }

    /**
     * Get a list of year-months-day between the bounds. e.i: 20220101, 20220230, ...
     * @return list of days
     */
    public List<Integer> getYearWeeksBetween() {
        return getYearWeeksBetween(false);
    }

    public List<Integer> getYearWeeksBetweenPersian() {
        return getYearWeeksBetween(true);
    }

    public List<Integer> getYearWeeksBetween(boolean persian) {
        int minY = dateMin.formatter().year;
        int maxY = dateMax.formatter().year;
        int minM = dateMin.formatter().month;
        int maxM = dateMax.formatter().month;
        int minD = dateMin.formatter().day;
        int maxD = dateMax.formatter().day;

        List<Integer> values = new ArrayList<>((maxY - minY) * 365);

        for (int y = minY; y <= maxY; ++y) {
            int w = 1;
            int yd = 1;

            for (int m = 1; m <= 12; ++m) {
                int lastDay = persian ?
                    DateTimeNormalizer.getMonthDayCountPersian(y, m) :
                    DateTimeNormalizer.getMonthDayCount(y, m);
                for (int d = 1; d <= lastDay; ++d) {
                    if (yd % 7 == 0) {
                        ++w;
                    }
                    ++yd;
                    if (y == minY && m < minM) {
                        continue;
                    }
                    if (y == minY && m == minM && d < minD) {
                        continue;
                    }
                    if (y == maxY && m > maxM) {
                        return values;
                    }
                    if (y == maxY && m == maxM && d > maxD) {
                        return values;
                    }
                    int ww = y * 100 + w;
                    if (!values.contains(ww)) {
                        values.add(ww);
                    }
                }
            }
        }

        return values;
    }

    public String toString() {
        return ObjectUtil.toStringViewable(this);
    }

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
        DateTimeRange range = (DateTimeRange) obj;
        if (dateMin == null || dateMax == null || range.dateMax == null || range.dateMin == null) {
            return false;
        }
        return dateMin.equals(range.dateMin) && dateMax.equals(range.dateMax);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (dateMin == null ? 0 : dateMin.hashCode());
        hash = 31 * hash + (dateMax == null ? 0 : dateMax.hashCode());
        return hash;
    }
}
