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
	private static final DateFormat TIME_ONLY_DATE_FORMAT =
		new SynchronizedDateFormat(new SimpleDateFormat(
				Translation.getTranslation("date_format.time_only_date")));
	private static final DateFormat DETAILED_TIME_FORMAT = 
		new SynchronizedDateFormat(new SimpleDateFormat(
				Translation.getTranslation("date_format.detailed_time")));
	private static final DateFormat FULL_DATE_FORMAT = 
		new SynchronizedDateFormat(new SimpleDateFormat(
				Translation.getTranslation("date_format.full_date")));
	// format of the added by date
	private static final DateFormat FILE_DATE_FORMAT = 
		new SynchronizedDateFormat(new SimpleDateFormat(
				Translation.getTranslation("date_format.file_date")));
	private static final DateFormat FILE_DATE_HOURS_FORMAT =
		new SynchronizedDateFormat(new SimpleDateFormat(
				Translation.getTranslation("date_format.file_date_hours")));

	// default number format for all numbers
	public static final DecimalFormat NUMBER_FORMATS = new DecimalFormat(
	Translation.getTranslation("number_format.number"));

	public static final DecimalFormat LONG_FORMATS = new DecimalFormat(
	Translation.getTranslation("number_format.long"));

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
				+ getFileDateFormatHours().format(date);
			} else if (dayDiffer == -1) {
				return Translation.getTranslation("general.yesterday") + " "
				+ getFileDateFormatHours().format(date);
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
		synchronized (NUMBER_FORMATS) {
			return NUMBER_FORMATS.format(n);
		}

	}

	public static String formatLong(long n) {
		synchronized (LONG_FORMATS) {
			return LONG_FORMATS.format(n);
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
		return TIME_ONLY_DATE_FORMAT;
	}

	/**
	 * See #692
	 * 
	 * @return the dETAILED_TIME_FOMRAT
	 */
	public static DateFormat getDetailedTimeFormat() {
		return DETAILED_TIME_FORMAT;
	}

	/**
	 * See #692
	 * 
	 * @return the fULL_DATE_FOMRAT
	 */
	public static DateFormat getFullDateFormat() {
		return FULL_DATE_FORMAT;
	}

	/**
	 * See #692
	 * 
	 * @return the fILE_DATE_FORMAT
	 */
	private static DateFormat getFileDateFormat() {
		return FILE_DATE_FORMAT;
	}

	/**
	 * See #692
	 * 
	 * @return the fILE_DATE_FORMAT_HOURS
	 */
	private static DateFormat getFileDateFormatHours() {
		return FILE_DATE_HOURS_FORMAT;
	}
}
