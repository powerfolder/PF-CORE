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

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

/**
 * Helper class for all formatting
 * 
 * @version $Revision: 1.6 $
 */
public class Format extends PFComponent {

    private static final CanonicalDateFormat CANONICAL_DATE_FORMAT = new CanonicalDateFormat();
    private static final ShortDateFormat SHORT_DATE_FORMAT = new ShortDateFormat();
    private static final ShortTimeFormat SHORT_TIME_FORMAT = new ShortTimeFormat();
    private static final LongTimeFormat LONG_TIME_FORMAT = new LongTimeFormat();
    private static final DoubleNumberFormat DOUBLE_NUMBER_FORMAT = new DoubleNumberFormat();
    private static final LongNumberFormat LONG_NUMBER_FORMAT = new LongNumberFormat();
    private static final PercentNumberFormat PERCENT_NUMBER_FORMAT = new PercentNumberFormat();

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
        if (number >= 1024) {
            number /= 1024;
            suffix = "TBytes";
        }
        String str = formatDecimal(number);
        return str + ' ' + suffix;
    }

    /**
     * @param bytes
     * @return a count of bytes in a string
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
        if (number >= 1024) {
            number /= 1024;
            suffix = "TB";
        }
        
        String str = formatDecimal(number);
        if (number < 0) {
            str = "??";
        }
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
        return LONG_TIME_FORMAT.get().format(date);
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
        return SHORT_TIME_FORMAT.get().format(date);
    }

    /**
     * Formats a date as universal canonical string in the format "dd MMM yyyy"
     * (English).
     * <p>
     * Examples: 10 JAN 2010, 30 DEC 2009.
     * 
     * @param date
     * @return a date as universal canonical string.
     */
    public static String formatDateCanonical(Date date) {
        return CANONICAL_DATE_FORMAT.get().format(date);
    }

    /**
     * Parses a date in universal canonical string format "dd MMM yyyy"
     * (English).
     * <p>
     * Examples: 10 JAN 2010, 30 DEC 2009.
     * 
     * @param str
     *            the string to parse
     * @return the date.
     * @throws ParseException
     */
    public static Date parseDateCanonical(String str) throws ParseException {
        return CANONICAL_DATE_FORMAT.get().parse(str);
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
     *            if not today, tomorrow or yesterday rendered as actual date
     *            string else as text "today", "tomorrow" and "yesterday" + time
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
                } else if (dayDiffer == 1) {
                    return Translation.getTranslation("general.tomorrow") + ' '
                        + formatTimeShort(date);
                }
            }
        }

        // Otherwise use default format
        return SHORT_DATE_FORMAT.get().format(date);
    }

    /**
     * Formats decimal numbers
     * 
     * @param n
     * @return
     */
    public static String formatDecimal(double n) {
        return DOUBLE_NUMBER_FORMAT.get().format(n);
    }

    /**
     * Formats long numbers
     * 
     * @param n
     * @return
     */
    public static String formatLong(long n) {
        return LONG_NUMBER_FORMAT.get().format(n);
    }

    /**
     * Formats numbers as percentage. 100.0 --> 100%
     * 
     * @param n
     * @return
     */
    public static String formatPercent(double n) {
        return PERCENT_NUMBER_FORMAT.get().format(n / 100.0);
    }

    /**
     * Formats boolean as 'Yes'/'No'
     *
     * @param n
     * @return
     */
    public static String formatBoolean(boolean n) {
        return n ? Translation.getTranslation("general.yes") : Translation.getTranslation("general.no");
    }

    /**
     * Translates a "how much time remaining" value into a string.
     * 
     * @param dt
     *            The time in milliseconds
     * @return the formatted string.
     * Examples "10 days", "5 hours", "17 minutes", "Less than one minute"
     */
    public static String formatDeltaTime(long dt) {
        Formatter f = new Formatter();
        long days = dt / 24 / 3600 / 1000;
        if (days >= 2) {
            return f.format(Translation.getTranslation("format.n.days",
                    String.valueOf(days))).out().toString();
        }
        long hours = dt / 3600 / 1000;
        if (hours >= 2) {
            return f.format(Translation.getTranslation("format.n.hours",
                    String.valueOf(hours))).out().toString();
        }
        long minutes = dt / 60 / 1000;
        if (minutes >= 1) {
            return f.format(Translation.getTranslation("format.n.minutes",
                    String.valueOf(minutes))).out().toString();
        } else if (minutes == 1) {
            return f.format(Translation.getTranslation(
                    "format.one_minute")).out().toString();
        } else {
            return f.format(
                    Translation.getTranslation("format.less_than_one_minute"))
                    .out().toString();
        }
    }

    private static class CanonicalDateFormat extends ThreadLocal<DateFormat> {
        protected DateFormat initialValue() {
            return new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        }
    }

    private static class ShortDateFormat extends ThreadLocal<DateFormat> {
        protected DateFormat initialValue() {
            return DateFormat.getDateTimeInstance(DateFormat.SHORT,
                DateFormat.SHORT);
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

    private static class DoubleNumberFormat extends ThreadLocal<NumberFormat> {
        protected NumberFormat initialValue() {
            NumberFormat format = NumberFormat.getInstance();
            format.setMaximumFractionDigits(2);
            return format;
        }
    }

    private static class LongNumberFormat extends ThreadLocal<NumberFormat> {
        protected NumberFormat initialValue() {
            return NumberFormat.getIntegerInstance();
        }
    }

    private static class PercentNumberFormat extends ThreadLocal<NumberFormat> {
        protected NumberFormat initialValue() {
            return NumberFormat.getPercentInstance();
        }
    }
}
