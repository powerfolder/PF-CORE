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

import de.dal33t.powerfolder.PFComponent;

import java.text.DecimalFormat;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

/**
 * Helper class for all formatting
 * 
 * @version $Revision: 1.6 $
 */
public class Format extends PFComponent {

    private static final ShortDateFormat SHORT_DATE_FORAMT = new ShortDateFormat();
    private static final LongDateFormat LONG_DATE_FORAMT = new LongDateFormat();
    private static final ShortTimeFormat SHORT_TIME_FORAMT = new ShortTimeFormat();
    private static final LongTimeFormat LONG_TIME_FORAMT = new LongTimeFormat();

    private Format() {
        // No instance
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
     * Long time format. Something like 15:45:46 PM
     * 
     * @param date
     * @return
     */
    public static String formatTimeLong(Date date) {
        if (date == null) {
            return null;
        }
        return LONG_TIME_FORAMT.get().format(date);
    }

    /**
     * Short time format. Something like 15:45 PM
     * 
     * @param date
     * @return
     */
    public static String formatTimeShort(Date date) {
        if (date == null) {
            return null;
        }
        return SHORT_TIME_FORAMT.get().format(date);
    }

    /**
     * Long date format.
     * 
     * @param date
     * @return Something like 10 October 2009
     */
    public static String formatDateLong(Date date) {
        return formatDateLong(date, true);
    }

    /**
     * Long date format.
     * 
     * @param date
     * @param renderTodayYesterday
     *            if today and yesterday should be rendered as actual date
     *            string or as text "today" and "yesterday"
     * @return Something like 10 October 2009
     */
    public static String formatDateLong(Date date, boolean renderTodayYesterday)
    {
        if (date == null) {
            return null;
        }
        if (renderTodayYesterday) {
            Calendar calDate = Calendar.getInstance();
            calDate.setTime(date);
            Calendar calNow = Calendar.getInstance();
            if (calDate.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)) {
                int dayDiffer = calDate.get(Calendar.DAY_OF_YEAR)
                    - calNow.get(Calendar.DAY_OF_YEAR);
                if (dayDiffer == 0) {
                    return Translation.getTranslation("general.today") + ' '
                        + formatTimeLong(date);
                } else if (dayDiffer == -1) {
                    return Translation.getTranslation("general.yesterday")
                        + ' ' + formatTimeLong(date);
                }
            }
        }
        // Otherwise use default format
        return LONG_DATE_FORAMT.get().format(date);
    }

    /**
     * Short date format.
     * 
     * @param date
     * @return Something like 10/10/09 12:12
     */
    public static String formatDateShort(Date date) {
        return formatDateShort(date, true);
    }

    /**
     * Short date format.
     * 
     * @param date
     * @param renderTodayYesterday
     *            if today and yesterday should be rendered as actual date
     *            string or as text "today" and "yesterday"
     * @return Something like 10/10/09 12:12
     */
    public static String formatDateShort(Date date, boolean renderTodayYesterday)
    {
        if (date == null) {
            return null;
        }
        if (renderTodayYesterday) {
            Calendar calDate = Calendar.getInstance();
            calDate.setTime(date);
            Calendar calNow = Calendar.getInstance();
            if (calDate.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)) {
                int dayDiffer = calDate.get(Calendar.DAY_OF_YEAR)
                    - calNow.get(Calendar.DAY_OF_YEAR);
                if (dayDiffer == 0) {
                    return Translation.getTranslation("general.today") + ' '
                        + formatTimeShort(date);
                } else if (dayDiffer == -1) {
                    return Translation.getTranslation("general.yesterday")
                        + ' ' + formatTimeShort(date);
                }
            }
        }

        // Otherwise use default format
        return SHORT_DATE_FORAMT.get().format(date);
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
     * @param dt
     *            The time in milliseconds
     * @return the formatted string. Examples: "102 days", "10:20:23"
     */
    public static String formatDeltaTime(long dt) {
        // @TODO make this progressive as it nears zero: "102 days" or
        // "10 hours" or "5 minutes" or "Less than one minute". "10:20:23" looks
        // like a time (twenty past ten).
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

    public static DecimalFormat getLongFormat() {
        return createDecimalFormat("number_format.long", "#,###,###,###");
    }

    public static DecimalFormat getNumberFormat() {
        return createDecimalFormat("number_format.number", "#,###,###,###.##");
    }

    private static DecimalFormat createDecimalFormat(String preferred,
        String fallback)
    {
        try {
            return new DecimalFormat(Translation.getTranslation(preferred));
        } catch (Exception e) {
            return new DecimalFormat(fallback);
        }
    }

    private static class ShortDateFormat extends ThreadLocal<DateFormat> {
        protected DateFormat initialValue() {
            return DateFormat.getDateTimeInstance(DateFormat.SHORT,
                DateFormat.SHORT);
        }
    }

    private static class LongDateFormat extends ThreadLocal<DateFormat> {
        protected DateFormat initialValue() {
            return DateFormat.getDateTimeInstance(DateFormat.LONG,
                DateFormat.LONG);
        }
    }

    private static class ShortTimeFormat extends ThreadLocal<DateFormat> {
        protected DateFormat initialValue() {
            return DateFormat.getTimeInstance(DateFormat.SHORT);
        }
    }

    private static class LongTimeFormat extends ThreadLocal<DateFormat> {
        protected DateFormat initialValue() {
            return DateFormat.getTimeInstance(DateFormat.LONG);
        }
    }
}
