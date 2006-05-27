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
    public static final DateFormat TIME_ONLY_DATE_FOMRAT = new SimpleDateFormat(
        "[HH:mm:ss]");
    public static final DateFormat DETAILED_TIME_FOMRAT = new SimpleDateFormat(
        "[HH:mm:ss:SS]");
    public static final DateFormat FULL_DATE_FOMRAT = new SimpleDateFormat(
        "dd.MM.yyyy HH:mm:ss");
    // format of the added by date
    private static final DateFormat FILE_DATE_FORMAT = new SimpleDateFormat(
        "dd.MM.yyyy HH:mm");
    private static final DateFormat FILE_DATE_FORMAT_HOURS = new SimpleDateFormat(
        "HH:mm");
    
    // default number format for all numbers
    public static final DecimalFormat NUMBER_FORMATS = new DecimalFormat(
        "#,###,###,###.##");

    public static final DecimalFormat LONG_FORMATS = new DecimalFormat(
        "#,###,###,###");

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
            number = number / 1024;
            suffix = "KBytes";
        }
        if (number > 800) {
            number = number / 1024;
            suffix = "MBytes";
        }
        if (number > 800) {
            number = number / 1024;
            suffix = "GBytes";
        }
        String str = NUMBER_FORMATS.format(number);
        return str + " " + suffix;
    }

    /**
     * Returns a count of bytes in a string
     * 
     * @param bytes
     * @return
     */
    public static String formatBytesShort(long bytes) {
        double number = bytes;

        number = number / 1024;
        String suffix = "KB";
        if (number > 800) {
            number = number / 1024;
            suffix = "MB";
        }
        if (number > 800) {
            number = number / 1024;
            suffix = "GB";
        }
        String str = NUMBER_FORMATS.format(number);
        return str + " " + suffix;
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
                return Translation.getTranslation("general.today") + " "
                    + FILE_DATE_FORMAT_HOURS.format(date);
            } else if (dayDiffer == -1) {
                return Translation.getTranslation("general.yesterday") + " "
                    + FILE_DATE_FORMAT_HOURS.format(date);
            }

        }
        // otherwise use default format
        return FILE_DATE_FORMAT.format(date);
    }

    /**
     * Formats numbers
     * 
     * @param n
     * @return
     */
    public static String formatNumber(double n) {
        return NUMBER_FORMATS.format(n);
    }

    public static String formatLong(long n) {
        return LONG_FORMATS.format(n);
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
        return f.format(Translation.getTranslation("general.time"), hours, minutes, seconds).out().toString();
    }
}
