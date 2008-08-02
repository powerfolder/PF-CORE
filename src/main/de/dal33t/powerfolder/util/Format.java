/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

/**
 * Helper class for all formatting
 *
 * @version $Revision: 1.6 $
 */
public class Format {

    /**
     * Returns a count of bytes in a string
     *
     * @param bytes
     * @return
     */
    public static String formatBytes(long bytes) {
        double number = bytes;
        String suffix = "Bytes";

        if (number > 800) {
            number /= 1024;
            suffix = "KBytes";
        }
        if (number > 800) {
            number /= 1024;
            suffix = "MBytes";
        }
        if (number > 800) {
            number /= 1024;
            suffix = "GBytes";
        }
        String str = getNumberFormat().format(number);
        return str + ' ' + suffix;
    }

    /**
     * Returns a count of bytes in a string
     *
     * @param bytes
     * @return
     */
    public static String formatBytesShort(long bytes) {
        double number = bytes;

        number /= 1024;
        String suffix = "KB";
        if (number > 800) {
            number /= 1024;
            suffix = "MB";
        }
        if (number > 800) {
            number /= 1024;
            suffix = "GB";
        }
        String str = getNumberFormat().format(number);
        return str + ' ' + suffix;
    }

    /**
     * Formats a date
     *
     * @param date
     * @return
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calDate = Calendar.getInstance();
        calDate.setTime(date);
        Calendar calNow = Calendar.getInstance();
        if (calDate.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)) {
            int dayDiffer = calDate.get(Calendar.DAY_OF_YEAR)
                    - calNow.get(Calendar.DAY_OF_YEAR);
            if (dayDiffer == 0) {
                return Translation.getTranslation("general.today") + ' '
                        + getFileDateHoursFormat().format(date);
            } else if (dayDiffer == -1) {
                return Translation.getTranslation("general.yesterday") + ' '
                        + getFileDateHoursFormat().format(date);
            }

        }
        // otherwise use default format
        return getFileDateFormat().format(date);
    }

    /**
     * Formats numbers
     *
     * @param n
     * @return
     */
    public static String formatNumber(double n) {
        return getNumberFormat().format(n);
    }

    public static String formatLong(long n) {
        return getLongFormat().format(n);
    }

    /**
     * Translates a "how much time remaining" value into a string.
     *
     * @param dt The time in milliseconds
     * @return the formatted string. Examples: "102 days", "10:30:23"
     */
    public static String formatDeltaTime(long dt) {
        Formatter f = new Formatter();
        long days = dt / 1000 / 60 / 60 / 24;
        long hours = dt / 1000 / 60 / 60;
        if (days > 1) { // Two days or more
            f.format(Translation.getTranslation("general.days", days) + ", ");
            hours %= 24;
        }
        long minutes = (dt / 1000 / 60) % 60;
        long seconds = (dt / 1000) % 60;
        return f.format(Translation.getTranslation("general.time"), hours,
                minutes, seconds).out().toString();
    }

    /*
      * The reason for the following methods: The javadoc of DateFormat states,
      * that that class (and subclasses mention the same) is not thread safe.
      * Actually they recommend to create an instance per thread. (But since this
      * is a general purpose class, we won't do that).
      */

    /**
     * See #692
     *
     * @return the TIME_ONLY_DATE_FOMRAT
     */
    public static DateFormat getTimeOnlyDateFormat() {
        return createSimpleDateFormat("date_format.time_only_date", "[HH:mm:ss]");
    }

    /**
     * See #692
     *
     * @return the DETAILED_TIME_FOMRAT
     */
    public static DateFormat getDetailedTimeFormat() {
        return createSimpleDateFormat("date_format.detailed_time", "[HH:mm:ss:SSS]");
    }

    /**
     * See #692
     *
     * @return the FULL_DATE_FOMRAT
     */
    public static DateFormat getFullDateFormat() {
        return createSimpleDateFormat("date_format.full_date", "MM/dd/yyyy HH:mm:ss");
    }

    /**
     * See #692
     *
     * @return the FILE_DATE_FORMAT
     */
    public static DateFormat getFileDateFormat() {
        return createSimpleDateFormat("date_format.file_date", "MM/dd/yyyy HH:mm");
    }

    /**
     * See #692
     *
     * @return the FILE_DATE_FORMAT_HOURS
     */
    private static DateFormat getFileDateHoursFormat() {
        return createSimpleDateFormat("date_format.file_date_hours", "HH:mm");
    }

    public static DecimalFormat getLongFormat() {
        return createDecimalFormat("number_format.long", "#,###,###,###");
    }

    public static DecimalFormat getNumberFormat() {
        return createDecimalFormat("number_format.number", "#,###,###,###.##");
    }

    private static DecimalFormat createDecimalFormat(String preferred, String fallback) {
        try {
            return new DecimalFormat(Translation.getTranslation(preferred));
        } catch (Exception e) {
            return new DecimalFormat(fallback);
        }
    }

    private static SimpleDateFormat createSimpleDateFormat(String preferred, String fallback) {
        try {
            return new SimpleDateFormat(Translation.getTranslation(preferred));
        } catch (Exception e) {
            return new SimpleDateFormat(fallback);
        }
    }
}
