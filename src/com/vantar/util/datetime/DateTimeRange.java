package com.vantar.util.datetime;

import com.vantar.util.object.ObjectUtil;
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
     * Set data, lower bound=not upper bound=tomorrow
     */
    public DateTimeRange() {
        this.dateMin = new DateTime();
        this.dateMax = new DateTime().addDays(1);
    }

    /**
     * Truncate time and normalize the range
     */
    public void adjustDateRange() {
        if (dateMin == null || dateMax == null) {
            return;
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
        dateMin.addSeconds(1);
        dateMax.decreaseSeconds(1);
    }

    /**
     * Normalize the range
     */
    public void adjustDateTimeRange() {
        if (dateMin == null || dateMax == null) {
            return;
        }
        if (dateMax.equals(dateMin)) {
            dateMax.addDays(1);
            if (dateMax.equals(dateMin)) {
                dateMax.addDays(1);
            }
        }
        dateMin.addSeconds(1);
        dateMax.decreaseSeconds(1);
    }

    /**
     * Check if the range is valid (values are set and lower bound is before upper bound)
     * @return true if range is valid
     */
    public boolean isValid() {
        return dateMin != null && dateMax != null && dateMax.isAfterOrEqual(dateMin);
    }

    /**
     * Get a list of years between the bounds
     * @return list of years
     */
    public List<Integer> getYearsBetween() {
        List<Integer> values = new ArrayList<>();
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
        List<Integer> values = new ArrayList<>();
        if (dateMin.formatter().year == dateMax.formatter().year) {
            for (int i = dateMin.formatter().month; i <= dateMax.formatter().month; ++i) {
                values.add(i);
            }
            return values;
        }

        for (int i = dateMin.formatter().month; i <= 12; ++i) {
            values.add(i);
        }
        for (int i = dateMin.formatter().year + 1; i < dateMax.formatter().year; ++i) {
            for (int j = 1; j <= 12; ++j) {
                values.add(j);
            }
        }
        for (int i = 1; i <= dateMax.formatter().month; ++i) {
            values.add(i);
        }
        return values;
    }

    public String toString() {
        return ObjectUtil.toString(this);
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
