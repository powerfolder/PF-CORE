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
 * $Id: DateUtil.java 9297 2009-09-03 19:17:22Z tot $
 */
package de.dal33t.powerfolder.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * General date and time utilities.
 * See Format class for formatting dates and times as String.
 */
public class DateUtil {

    /**
     * Compares with a marge of 2000 milliseconds to solve the rounding problems
     * or some filesystems.
     *
     * @true if dates are the same within a marge of 2000 milliseconds
     */
    public static boolean equalsFileDateCrossPlattform(Date date1, Date date2)
    {
        return equalsFileDateCrossPlattform(date1.getTime(), date2.getTime());
    }

    /**
     * Compares with a marge of 2000 milliseconds to solve the rounding problems
     * on some filesystems.
     *
     * @true if times are the same within a marge of 2000 milliseconds
     * @see Convert#convertToGlobalPrecision(long)
     */
    public static boolean equalsFileDateCrossPlattform(long time1, long time2)
    {
        if (time1 == time2) {
            return true;
        }
        long difference;
        if (time1 > time2) {
            difference = time1 - time2;
        } else {
            difference = time2 - time1;
        }
        return difference <= 2000;
    }

    /**
     * Compares with a marge of 2000 milliseconds to solve the rounding problems
     * or some filesystems.
     *
     * @return true if date1 is a newer date than date2
     */
    public static boolean isNewerFileDateCrossPlattform(Date date1, Date date2)
    {
        return isNewerFileDateCrossPlattform(date1.getTime(), date2.getTime());
    }

    /**
     * Compares with a marge of 2000 milliseconds to solve the rounding problems
     * or some filesystems.
     *
     * @return true if time1 is a newer time than time2
     */
    public static boolean isNewerFileDateCrossPlattform(long time1, long time2)
    {
        if (time1 == time2) {
            return false;
        }
        long difference = time1 - time2;
        return difference > 2000;
    }

    /**
     * Returns true is a date is more than n days in the future.
     *
     * @param date
     * @param n
     * @return
     */
    public static boolean isDateMoreThanNDaysInFuture(Date date, int n) {
        Reject.ifNull(date, "No date");
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DATE, n);
        return date.after(cal.getTime());
    }

    /**
     * Is {@code t1} more than {@code n} days after {@code t0}?
     * 
     * @param t0 the earlier date
     * @param t1 the later date
     * @param n the days between them
     * @return
     */
    public static boolean isMoreThanNDaysAfter(Date t0, Date t1, int n) {
        Reject.ifNull(t0, "No Date t0");
        Reject.ifNull(t1, "No Date t1");

        Calendar cal = new GregorianCalendar();
        cal.setTime(t0);
        cal.add(Calendar.DATE, n);
        return t1.after(cal.getTime());
    }

    /**
     * Returns true is a date is more than n minutes in the future.
     *
     * @param date
     * @param n
     * @return
     */
    public static boolean isDateMoreThanNHoursInFuture(Date date, int n) {
        Reject.ifNull(date, "No date");
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.HOUR, n);
        return date.after(cal.getTime());
    }

    /**
     * Returns true is a date is more than n minutes in the future.
     *
     * @param date
     * @param n
     * @return
     */
    public static boolean isDateMoreThanNMinutesInFuture(Date date, int n) {
        Reject.ifNull(date, "No date");
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.MINUTE, n);
        return date.after(cal.getTime());
    }

    public static int getDaysInFuture(Date date) {
        Reject.ifNull(date, "No date");
        Calendar cal = new GregorianCalendar();
        Date now = cal.getTime();
        long diff = date.getTime() - now.getTime();
        return (int) (diff / 1000 / 3660 / 24);
    }

    public static int getHoursInFuture(Date date) {
        Reject.ifNull(date, "No date");
        Calendar cal = new GregorianCalendar();
        Date now = cal.getTime();
        long diff = date.getTime() - now.getTime();
        return (int) (diff / 1000 / 3660);
    }

    public static int getMinutesInFuture(Date date) {
        Reject.ifNull(date, "No date");
        Calendar cal = new GregorianCalendar();
        Date now = cal.getTime();
        long diff = date.getTime() - now.getTime();
        return (int) (diff / 1000 / 60);
    }

    /**
     * Is the subject date before the end of the predicate date?
     * '5 December 2009 21:15:45' is before end of '5 December 2009'.
     * Actually tests that subject is before day-after-predicate.
     *
     * @param subject
     * @param predicate
     * @return
     */
    public static boolean isBeforeEndOfDate(Date subject,Date predicate) {

        Calendar cal = new GregorianCalendar();
        cal.setTime(zeroTime(predicate));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, 1);
        return subject.before(cal.getTime());
    }

    /**
     * Returns a date that is the same day as the arg with all time parts == 0.
     *
     * @param date
     * @return
     */
    public static Date zeroTime(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * Returns a date that is the same day and hour as the arg
     * with all other time parts == 0.
     *
     * @param date
     * @return
     */
    public static Date truncateToHour(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * The LDAP timestamp is defined as the 100-nanosecond-intervals since
     * January 1, 1601. Unix timestamps are defined as the seconds since January
     * 1, 1970.
     * 
     * @param ldapTimestamp the time in LDAP format
     * @return the time in unix format
     */
    public static long convertLdapToUnix(long ldapTimestamp) {
        long nsToSec = 10000;
        long diff1601Till1970 = 11644473600000l;

        return (ldapTimestamp / nsToSec) - diff1601Till1970;
    }

    /**
     * Try parsing the String {@code expiration} as date as one of the following formats:
     * <ol>
     * <li>ISO-8601 as yyyy-MM-ddTHH:mm:ss.fffZ</li>
     * <li>LDAP encoded Unix Timestamp</li>
     * <li>ISO-8601 as yyyyMMddHHmmss</li>
     * </ol>
     * 
     * @param stringDate 
     * @return The parsed date
     * @throws ParseException
     */
    public static Date parseDate(String stringDate) throws ParseException {
        Date newDate = null;
        try {
            // ISO-8601 timeformat: 2015-09-22T13:32:32.084Z
            Calendar cal = javax.xml.bind.DatatypeConverter
                .parseDateTime(stringDate);
            newDate = cal.getTime();
        } catch (RuntimeException rte) {
            try {
                long expDate = Long.parseLong(stringDate);
                if (expDate != 0 && expDate != 9223372036854775807l) {
                    expDate = DateUtil.convertLdapToUnix(expDate);
                    newDate = new Date(expDate);
                }
            } catch (NumberFormatException nfe) {
                if (!stringDate.startsWith("9999")
                    && !stringDate.equals("0"))
                {
                    SimpleDateFormat sdf = new SimpleDateFormat(
                        "yyyyMMddHHmmss");
                    newDate = sdf.parse(stringDate);
                }
            }
        }
        return newDate;
    }
}
