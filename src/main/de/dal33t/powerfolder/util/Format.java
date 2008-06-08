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
    
    private static DateFormat timeOnlyDateFormat;
	private static DateFormat detailedTimeFormat;
	private static DateFormat fullDateFormat;
	private static DateFormat fileDateFormat;
	private static DateFormat fileDateHoursFormat;

    private static DecimalFormat numberFormat;
	private static DecimalFormat longFormat;

    static {
        try {
            timeOnlyDateFormat =
                    new SynchronizedDateFormat(new SimpleDateFormat(
                            Translation.getTranslation("date_format.time_only_date")));
            detailedTimeFormat =
                    new SynchronizedDateFormat(new SimpleDateFormat(
                            Translation.getTranslation("date_format.detailed_time")));
            fullDateFormat =
                    new SynchronizedDateFormat(new SimpleDateFormat(
                            Translation.getTranslation("date_format.full_date")));
            fileDateFormat =
                    new SynchronizedDateFormat(new SimpleDateFormat(
                            Translation.getTranslation("date_format.file_date")));
            fileDateHoursFormat =
                    new SynchronizedDateFormat(new SimpleDateFormat(
                            Translation.getTranslation("date_format.file_date_hours")));
            numberFormat = new DecimalFormat(
                    Translation.getTranslation("number_format.number"));

            longFormat = new DecimalFormat(
                    Translation.getTranslation("number_format.long"));
        } catch (Exception e) {
            System.err.println("Could not initializ formats");

            // Use defaults. Testcases need this.
            timeOnlyDateFormat = new SynchronizedDateFormat(new SimpleDateFormat(
                    "[HH:mm:ss]"));
            detailedTimeFormat = new SynchronizedDateFormat(new SimpleDateFormat(
                    "[HH:mm:ss:SSS]"));
            fullDateFormat = new SynchronizedDateFormat(new SimpleDateFormat(
                    "MM/dd/yyyy HH:mm:ss"));
            fileDateFormat = new SynchronizedDateFormat(new SimpleDateFormat(
                    "MM/dd/yyyy HH:mm"));
            fileDateHoursFormat = new SynchronizedDateFormat(new SimpleDateFormat(
                    "HH:mm"));
            numberFormat = new DecimalFormat("#,###,###,###.##");
            longFormat = new DecimalFormat("#,###,###,###");
        }
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
		String str = numberFormat.format(number);
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
		String str = numberFormat.format(number);
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
				+ fileDateHoursFormat.format(date);
			} else if (dayDiffer == -1) {
				return Translation.getTranslation("general.yesterday") + ' '
				+ fileDateHoursFormat.format(date);
			}

		}
		// otherwise use default format
		return fileDateFormat.format(date);
	}

	/**
	 * Formats numbers
	 * 
	 * @param n
	 * @return
	 */
	public static String formatNumber(double n) {
		synchronized (numberFormat) {
			return numberFormat.format(n);
		}
	}

	public static String formatLong(long n) {
		synchronized (longFormat) {
			return longFormat.format(n);
		}
	}

	/**
	 * Translates a "how much time remaining" value into a string.
	 * 
	 * @param dt
	 *            The time in milliseconds
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
	 * @return the tIME_ONLY_DATE_FOMRAT
	 */
	public static DateFormat getTimeOnlyDateFormat() {
		return timeOnlyDateFormat;
	}

	/**
	 * See #692
	 * 
	 * @return the dETAILED_TIME_FOMRAT
	 */
	public static DateFormat getDetailedTimeFormat() {
		return detailedTimeFormat;
	}

	/**
	 * See #692
	 * 
	 * @return the fULL_DATE_FOMRAT
	 */
	public static DateFormat getFullDateFormat() {
		return fullDateFormat;
	}

	/**
	 * See #692
	 * 
	 * @return the fILE_DATE_FORMAT
	 */
	private static DateFormat getFileDateFormat() {
		return fileDateFormat;
	}

	/**
	 * See #692
	 * 
	 * @return the fILE_DATE_FORMAT_HOURS
	 */
	private static DateFormat getFileDateHoursFormat() {
		return fileDateHoursFormat;
	}

    public static DecimalFormat getLongFormat() {
        return longFormat;
    }

    public static DecimalFormat getNumberFormat() {
        return numberFormat;
    }
}
