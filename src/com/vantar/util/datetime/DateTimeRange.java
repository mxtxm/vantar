package com.vantar.util.datetime;

import java.util.*;


public class DateTimeRange {

    public DateTime dateMin;
    public DateTime dateMax;

    public DateTimeRange(DateTime dateMin, DateTime dateMax) {
        this.dateMin = dateMin;
        this.dateMax = dateMax;
    }

    public DateTimeRange() {
        this.dateMin = new DateTime();
        this.dateMax = new DateTime().addDays(1);
    }

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

    public boolean isValid() {
        return dateMin != null && dateMax != null && dateMax.isAfter(dateMin);
    }

    public List<Integer> getYearsBetween() {
        List<Integer> values = new ArrayList<>();
        for (int i = dateMin.formatter().year; i <= dateMax.formatter().year; ++i) {
            values.add(i);
        }
        return values;
    }

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
        return "(" + (dateMin == null ? "null" : dateMin.toString()) + ", " + (dateMax == null ? "null" : dateMax.toString()) + ")";
    }
}
