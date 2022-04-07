package com.vantar.util.datetime;

/**
 * Persian date utilities
 */
public class PersianDateUtil {

    public static final int PERSIAN_YEAR_UPPER_LIMIT = 1500;
    private static final int[] breaks = new int[] {-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060,
        2097, 2192, 2262, 2324, 2394, 2456, 3178};

    /**
     * Check if persian date is valid
     * @param year year
     * @param month month
     * @param day day
     * @return true if valid
     */
    public static boolean isValid(int year, int month, int day) {
        return  year >= -61 && year <= 3177 &&
            month >= 1 && month <= 12 &&
            day >= 1 && day <= getMonthDays(year, month);
    }

    /**
     * Get number months days
     * @param year year
     * @param month month
     * @return number of days
     */
    public static int getMonthDays(int year, int month) {
        if (month <= 6) {
            return 31;
        }
        if (month <= 11) {
            return 30;
        }
        // is year leap
        if (getYearsSinceLastLeap(year) == 0) {
            return 30;
        }
        return 29;
    }

    /**
     * Convert date
     * @param dateTimeFormatter date time formatter
     * @return gregorian date
     */
    public static DateTimeFormatter toGregorian(DateTimeFormatter dateTimeFormatter) {
        return toGregorian(dateTimeFormatter.year, dateTimeFormatter.month, dateTimeFormatter.day);
    }

    /**
     * Convert date
     * @param dateTimeFormatter date time formatter
     * @return persian date
     */
    public static DateTimeFormatter toPersian(DateTimeFormatter dateTimeFormatter) {
        return toPersian(dateTimeFormatter.year, dateTimeFormatter.month, dateTimeFormatter.day);
    }

    /**
     * Convert date
     * @param year year
     * @param month month
     * @param day day
     * @return persian date
     */
    public static DateTimeFormatter toPersian(int year, int month, int day) {
        int jdn = getJulianDayNumber(year, month, day);

        int gy = getGregorianDate(jdn).year; // Calculate Gregorian year (gy).
        int jy = gy - 621;
        JCal r = jalCal(jy, false);
        int jd;
        int jm;
        int k = jdn - getJulianDayNumber(gy, 3, r.march);

        // find number of days that passed since 1 Farvardin.
        if (k >= 0) {
            if (k <= 185) {
                // The first 6 months.
                jm = 1 + div(k, 31);
                jd = mod(k, 31) + 1;
                return new DateTimeFormatter(jy, jm, jd);
            } else {
                // The remaining months.
                k -= 186;
            }
        } else {
            // Previous Jalaali year.
            jy -= 1;
            k += 179;
            if (r.leap == 1) {
                k += 1;
            }
        }
        jm = 7 + div(k, 30);
        jd = mod(k, 30) + 1;

        return new DateTimeFormatter(jy, jm, jd);
    }

    /**
     * Convert date
     * @param year year
     * @param month month
     * @param day day
     * @return gregorian date
     */
    public static DateTimeFormatter toGregorian(int year, int month, int day) {
        JCal r = jalCal(year, true);
        return getGregorianDate(getJulianDayNumber(r.gy, 3, r.march) + (month - 1) * 31 - div(month, 7) * (month - 7) + day - 1);
    }

    private static int getYearsSinceLastLeap(int jy) {
        int bl = breaks.length;
        int jp = breaks[0];
        int jm;
        int jump = 0;
        int leap;
        int n;
        int i;

        for (i = 1; i < bl; i += 1) {
            jm = breaks[i];
            jump = jm - jp;
            if (jy < jm) {
                break;
            }
            jp = jm;
        }

        n = jy - jp;

        if (jump - n < 6) {
            n = n - jump + div(jump + 4, 33) * 33;
        }

        leap = mod(mod(n + 1, 33) - 1, 4);
        if (leap == -1) {
            leap = 4;
        }

        return leap;
    }

    private static JCal jalCal(int jy, boolean withoutLeap) {
        int bl = breaks.length;
        int gy = jy + 621;
        int leapJ = -14;
        int jp = breaks[0];
        int jm;
        int jump = 0;
        int leap = 0;
        int leapG;
        int march;
        int n;
        int i;

        // Find the limiting years for the Jalaali year jy.
        for (i = 1; i < bl; i += 1) {
            jm = breaks[i];
            jump = jm - jp;
            if (jy < jm) {
                break;
            }
            leapJ = leapJ + div(jump, 33) * 8 + div(mod(jump, 33), 4);
            jp = jm;
        }
        n = jy - jp;

        // Find the number of leap years from AD 621 to the beginning
        // of the current Jalaali year in the Persian calendar.
        leapJ = leapJ + div(n, 33) * 8 + div(mod(n, 33) + 3, 4);
        if (mod(jump, 33) == 4 && jump - n == 4) {
            leapJ += 1;
        }

        // And the same in the Gregorian calendar (until the year gy).
        leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150;

        // Determine the Gregorian date of Farvardin the 1st.
        march = 20 + leapJ - leapG;

        // Find how many years have passed since the last leap year.
        if (!withoutLeap) {
            if (jump - n < 6) {
                n = n - jump + div(jump + 4, 33) * 33;
            }
            leap = mod(mod(n + 1, 33) - 1, 4);
            if (leap == -1) {
                leap = 4;
            }
        }

        return new JCal(leap, gy, march);
    }

    private static int getJulianDayNumber(int gy, int gm, int gd) {
        int d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4)
            + div(153 * mod(gm + 9, 12) + 2, 5)
            + gd - 34840408;
        d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752;
        return d;
    }

    private static DateTimeFormatter getGregorianDate(int julianDayNumber) {
        int j;
        int i;
        int gd;
        int gm;
        int gy;

        j = 4 * julianDayNumber + 139361631;
        j = j + div(div(4 * julianDayNumber + 183187720, 146097) * 3, 4) * 4 - 3908;
        i = div(mod(j, 1461), 4) * 5 + 308;
        gd = div(mod(i, 153), 5) + 1;
        gm = mod(div(i, 153), 12) + 1;
        gy = div(j, 1461) - 100100 + div(8 - gm, 6);

        return new DateTimeFormatter(gy, gm, gd);
    }

    private static int div(int a, int  b) {
        return (int) Math.floor(a / b);
    }

    private static int mod(int a, int  b) {
        return (int) (a - Math.floor(a / b) * b);
    }

    private static class JCal {

        public final int leap;
        public final int gy;
        public final int march;

        private JCal(int leap, int gy, int march) {
            this.leap = leap;
            this.gy = gy;
            this.march = march;
        }
    }
}
