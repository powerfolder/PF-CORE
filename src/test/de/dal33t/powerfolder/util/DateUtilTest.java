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
* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.util;

import junit.framework.TestCase;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateUtilTest extends TestCase {

    public void testDefault() {
        assertTrue(DateUtil.equalsFileDateCrossPlattform(new Date(1000), new Date(3000)));
        assertTrue(DateUtil.equalsFileDateCrossPlattform(new Date(11111111), new Date(11111000)));
        assertFalse(DateUtil.equalsFileDateCrossPlattform(new Date(222222222), new Date(222220000)));

        // 2000 milliseconds we assume the same
        assertFalse(DateUtil.isNewerFileDateCrossPlattform(new Date(3000),new Date(1000)));
        // 2001 milliseconds we assume different
        assertTrue(DateUtil.isNewerFileDateCrossPlattform(new Date(3001),new Date(1000)));
        // other is newer
        assertFalse(DateUtil.isNewerFileDateCrossPlattform(new Date(1000),new Date(3001)));
    }

    public void testSpecial() {
        assertTrue(DateUtil.equalsFileDateCrossPlattform(new Date(1146605870000L), new Date(1146605868805L)));
    }

    public void testDateDaysInFuture() {
        Calendar cal = new GregorianCalendar();
        // Today is not more than 15 days ahead.
        assertFalse("Future date fault 0", DateUtil.isDateMoreThanNDaysInFuture(cal.getTime(), 15));
        cal.add(Calendar.DATE, 10);
        // Ten days in the future is not more than 15 days ahead.
        assertFalse("Future date fault 10", DateUtil.isDateMoreThanNDaysInFuture(cal.getTime(), 15));
        cal.add(Calendar.DATE, 10);
        // Twenty days in the future is more than 15 days ahead.
        assertTrue("Future date fault 20", DateUtil.isDateMoreThanNDaysInFuture(cal.getTime(), 15));
    }

    public void testDateHoursInFuture() {
        Calendar cal = new GregorianCalendar();
        // Today is not more than 15 hours ahead.
        assertFalse("Future date fault 0", DateUtil.isDateMoreThanNHoursInFuture(cal.getTime(), 15));
        cal.add(Calendar.HOUR, 10);
        // Ten hours in the future is not more than 15 hours ahead.
        assertFalse("Future date fault 10", DateUtil.isDateMoreThanNHoursInFuture(cal.getTime(), 15));
        cal.add(Calendar.HOUR, 10);
        // Twenty hours in the future is more than 15 hours ahead.
        assertTrue("Future date fault 20", DateUtil.isDateMoreThanNHoursInFuture(cal.getTime(), 15));
    }

    public void testDateMinutesInFuture() {
        Calendar cal = new GregorianCalendar();
        // Today is not more than 15 minutes ahead.
        assertFalse("Future date fault 0", DateUtil.isDateMoreThanNMinutesInFuture(cal.getTime(), 15));
        cal.add(Calendar.MINUTE, 10);
        // Ten minutes in the future is not more than 15 minuts ahead.
        assertFalse("Future date fault 10", DateUtil.isDateMoreThanNMinutesInFuture(cal.getTime(), 15));
        cal.add(Calendar.MINUTE, 10);
        // Twenty minuts in the future is more than 15 minuts ahead.
        assertTrue("Future date fault 20", DateUtil.isDateMoreThanNMinutesInFuture(cal.getTime(), 15));
    }

    public void testBeforeEndOfDate() {
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DATE, -1);
        assertTrue("Yesterday is before end of today", DateUtil.isBeforeEndOfDate(cal.getTime(), new Date()));
        cal.add(Calendar.DATE, 1);
        assertTrue("Today is before end of today", DateUtil.isBeforeEndOfDate(cal.getTime(), new Date()));
        cal.add(Calendar.DATE, 1);
        assertFalse("Tomorrow is not before end of today", DateUtil.isBeforeEndOfDate(cal.getTime(), new Date()));
    }

    public void testZeroTime() {
        Date date = new Date();
        Date result = DateUtil.zeroTime(date);
        assertFalse("Dates are the same", date.equals(result));

        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(result);

        assertEquals("Days are different", dateCal.get(Calendar.DAY_OF_YEAR),
                resultCal.get(Calendar.DAY_OF_YEAR));
        assertFalse("The rest are same",
                dateCal.get(Calendar.HOUR_OF_DAY) == resultCal.get(Calendar.HOUR_OF_DAY) &&
                        dateCal.get(Calendar.MINUTE) == resultCal.get(Calendar.MINUTE) &&
                        dateCal.get(Calendar.SECOND) == resultCal.get(Calendar.SECOND) &&
                        dateCal.get(Calendar.MILLISECOND) == resultCal.get(Calendar.MILLISECOND));
    }

    public void testTruncateToHour() {
        Date date = new Date();
        Date result = DateUtil.truncateToHour(date);
        assertFalse("Dates are the same", date.equals(result));

        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(result);

        assertEquals("Days are different", dateCal.get(Calendar.DAY_OF_YEAR),
                resultCal.get(Calendar.DAY_OF_YEAR));
        assertEquals("Hours are different", dateCal.get(Calendar.HOUR_OF_DAY),
                resultCal.get(Calendar.HOUR_OF_DAY));
        assertFalse("The rest are same",
                dateCal.get(Calendar.MINUTE) == resultCal.get(Calendar.MINUTE) &&
                        dateCal.get(Calendar.SECOND) == resultCal.get(Calendar.SECOND) &&
                        dateCal.get(Calendar.MILLISECOND) == resultCal.get(Calendar.MILLISECOND));
    }

    public void testConvertLdapToUnix() {
        // 24. 6. 2007 5:57:54.2968750
        assertEquals(1182664674296l, DateUtil.convertLdapToUnix(128271382742968750l));
        DateFormat df = new SimpleDateFormat("dd. MM. yyyy HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals("24. 06. 2007 05:57:54", df.format(new Date(1182664674296l)));
    }

    /**
     * @author krickl
     * @throws ParseException 
     */
    public void testIsMoreThanNDaysAfter() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM yyyy");
        Date d1 = sdf.parse("01.01 2010");
        Date d2 = sdf.parse("10.01 2010");

        assertTrue(DateUtil.isMoreThanNDaysAfter(d1, d2, 4));
        assertFalse(DateUtil.isMoreThanNDaysAfter(d1, d2, 15));
        assertFalse(DateUtil.isMoreThanNDaysAfter(d1, d2, 10));

        d1 = sdf.parse("27.01 2010");
        d2 = sdf.parse("10.02 2010");

        assertTrue(DateUtil.isMoreThanNDaysAfter(d1, d2, 4));
        assertFalse(DateUtil.isMoreThanNDaysAfter(d1, d2, 20));
        assertFalse(DateUtil.isMoreThanNDaysAfter(d1, d2, 14));

        d1 = sdf.parse("10.12 2010");
        d2 = sdf.parse("05.01 2011");

        assertTrue(DateUtil.isMoreThanNDaysAfter(d1, d2, 10));
        assertFalse(DateUtil.isMoreThanNDaysAfter(d1, d2, 30));
        assertFalse(DateUtil.isMoreThanNDaysAfter(d1, d2, 26));
    }

    @Test
    public void testParseDate() throws ParseException {
        assertNull(DateUtil.parseDate("9223372036854775807"));
        assertNull(DateUtil.parseDate("0"));

        // Mon Jan 01 00:00:00 UTC 1601
        assertEquals(1, 1, 1601, 0, 0, 0, DateUtil.parseDate("1"));

        // 2015-09-22T13:32:32.084Z
        assertEquals(22, 9, 2015, 13, 32, 32, DateUtil.parseDate("2015-09-22T13:32:32.084Z"));

        // 2013-04-03T17:04:39.9430000+03:00. +3 on UTC
        assertEquals(3, 4, 2013, 14, 4, 39, DateUtil.parseDate("2013-04-03T17:04:39.9430000+03:00"));

        // Souce: https://en.wikipedia.org/wiki/ISO_8601
        assertEquals(23, 4, 2020, 12, 50, 40, DateUtil.parseDate("2020-04-23T12:50:40+00:00"));
        assertEquals(23, 4, 2020, 12, 50, 40, DateUtil.parseDate("2020-04-23T12:50:40Z"));

        // Unsupported variants at the moment:
        // assertEquals(23, 4, 2020, 0, 0, 0, DateUtil.parseDate("2020-04-23"));
        // assertEquals(23, 4, 2020, 0, 0, 0, DateUtil.parseDate("2020-04-23"));
        // assertEquals(23, 4, 2020, 12, 50, 40, DateUtil.parseDate("20200423T125040Z"));
        // assertEquals(23, 4, 2020, 12, 50, 40, DateUtil.parseDate("--04-23[1]"));
        // assertEquals(23, 4, 2020, 12, 50, 40, DateUtil.parseDate("2020-W17"));
        // assertEquals(23, 4, 2020, 12, 50, 40, DateUtil.parseDate("2020-114"));
    }

    private void assertEquals(int day, int month, int year, int hour, int minutes, int seconds, Date actual) {
        Calendar c = Calendar.getInstance();
        c.setTime(actual);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));

        assertEquals(day, c.get(GregorianCalendar.DAY_OF_MONTH));
        assertEquals(month, c.get(GregorianCalendar.MONTH) + 1);
        assertEquals(year, c.get(GregorianCalendar.YEAR));

        assertEquals(hour, c.get(GregorianCalendar.HOUR_OF_DAY));
        assertEquals(minutes, c.get(GregorianCalendar.MINUTE));
        assertEquals(seconds, c.get(GregorianCalendar.SECOND));
    }
}
