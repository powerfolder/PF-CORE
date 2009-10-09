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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.PFComponent;

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
public class Format extends PFComponent {

    private static Format instance;

    public static Format getInstance(Controller controller) {
        if (instance == null) {
            instance = new Format(controller);
        }
        return instance;
    }

    private Format(Controller controller) {
        super(controller);
    }

    /**
     * Returns a count of bytes in a string
     *
     * @param bytes
     * @return
     */
    public static String formatBytes(long bytes) {
        double number = bytes;
        String suffix = "Bytes";

        if (number >= 1024) {
            number /= 1024;
            suffix = "KBytes";
        }
        if (number >= 1024) {
            number /= 1024;
            suffix = "MBytes";
        }
        if (number >= 1024) {
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
        if (number >= 1024) {
            number /= 1024;
            suffix = "MB";
        }
        if (number >= 1024) {
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
    public String formatDate(Date date) {
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

    public static String formatDateLegacy(Date date) {
        return DateFormat.getDateInstance(DateFormat.SHORT).format(date) + ' '
            + DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
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
            f.format(Translation.getTranslation("general.days", String
                    .valueOf(days))
                    + ", ");
            hours %= 24;
        }
        long minutes = dt / 1000 / 60 % 60;
        long seconds = dt / 1000 % 60;
        return f.format(Translation.getTranslation("general.time"), hours,
                minutes, seconds).out().toString();
    }

    /**
     * @param syncPercentage
     * @return the rendered sync percentage.
     */
    public static String formatSyncPercentage(double syncPercentage) {
        if (syncPercentage >= 0) {
            return Translation.getTranslation("percent.place.holder",
                    getNumberFormat().format(syncPercentage));
        }
        return Translation.getTranslation("percent.place.holder", "?");
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
    public DateFormat getTimeOnlyDateFormat() {
        return createSimpleDateFormat("date_format.time_only_date",
                "[HH:mm:ss]");
    }

    /**
     * See #692
     *
     * @return the DETAILED_TIME_FOMRAT
     */
    public DateFormat getDetailedTimeFormat() {
        return createSimpleDateFormat("date_format.detailed_time",
                "[HH:mm:ss:SSS]");
    }

    /**
     * See #692
     *
     * @return the FULL_DATE_FOMRAT
     */
    public DateFormat getFullDateFormat() {
        return createSimpleDateFormat("date_format.full_date",
                "MM/dd/yyyy HH:mm:ss");
    }

    /**
     * See #692
     *
     * @return the FILE_DATE_FORMAT
     */
    public DateFormat getFileDateFormat() {
        return createSimpleDateFormat("date_format.file_date",
                "MM/dd/yyyy HH:mm");
    }

    /**
     * See #692
     *
     * @return the FILE_DATE_FORMAT_HOURS
     */
    private DateFormat getFileDateHoursFormat() {
        return createSimpleDateFormat("date_format.file_date_hours", "HH:mm");
    }

    public static DecimalFormat getLongFormat() {
        return createDecimalFormat("number_format.long", "#,###,###,###");
    }

    public static DecimalFormat getNumberFormat() {
        return createDecimalFormat("number_format.number", "#,###,###,###.##");
    }

    private static DecimalFormat createDecimalFormat(String preferred,
                                                     String fallback) {
        try {
            return new DecimalFormat(Translation.getTranslation(preferred));
        } catch (Exception e) {
            return new DecimalFormat(fallback);
        }
    }

    private SimpleDateFormat createSimpleDateFormat(String preferred,
                                                    String fallback) {
        try {
            return new SimpleDateFormat(hour24(Translation.getTranslation(preferred), getController()));
        } catch (Exception e) {
            return new SimpleDateFormat(hour24(fallback, getController()));
        }
    }

    /**
     * Replace HH in time formats with h, and add a.
     * So 23:59:59 becomes 11:59:59 PM, and [23:59:59] becomes [11:59:59 PM].
     * 
     * @param format
     * @param controller
     * @return
     */
    private static String hour24(String format, Controller controller) {
        if (!PreferencesEntry.TIME_24_HOUR.getValueBoolean(controller)) {
            int hh = format.indexOf("HH");
            if (hh >= 0) {
                StringBuilder sb = new StringBuilder(format);
                sb.replace(hh, hh + 2, "h");

                // Some date/time formats are surrounded by [...]
                int j = sb.toString().lastIndexOf(']');
                if (j > 0) {
                    sb.replace(j, j + 1, " a]");
                } else {
                    sb.append(" a");
                }
                return sb.toString();
            }
        }
        return format;
    }
}
