package de.dal33t.powerfolder.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.*;

public class FormatTest {

    Locale defaultLocale = Locale.getDefault();

    @Before
    public void setUp(){
        Locale.setDefault(Locale.US);
    }

    @After
    public void tearDown(){
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void formatBytesReturnBytesTest() {
        //Negative values return the value followed by Bytes, no matter how large the value provided was
        assertEquals("-20 Bytes", Format.formatBytes(-20));
        long almostOneKbyte = 1023L;
        assertEquals("1,023 Bytes", Format.formatBytes(almostOneKbyte));
    }

    @Test
    public void formatBytesReturnKbytesTest() {
        long oneKByte = 1024L;
        assertEquals("1 kBytes",Format.formatBytes(oneKByte));
        long twoKBytes = 2048L;
        assertEquals("2 kBytes", Format.formatBytes(twoKBytes));
        long almostOneMbyte = 1048575L;
        assertEquals("1,024 kBytes", Format.formatBytes(almostOneMbyte));
    }

    @Test
    public void formatBytesReturnMbytesTest() {
        long oneMByte = 1048576L;
        assertEquals("1 MBytes", Format.formatBytes(oneMByte));
        long twoMbytes = 2097152L;
        assertEquals("2 MBytes", Format.formatBytes(twoMbytes));
        long almostOneGbyte = 1073741823L;
        assertEquals("1,024 MBytes", Format.formatBytes(almostOneGbyte));
    }

    @Test
    public void formatBytesReturnGbytesTest() {
        long oneGbyte = 1073741824L;
        assertEquals("1 GBytes", Format.formatBytes(oneGbyte));
        long twoGbytes = 2147483648L;
        assertEquals("2 GBytes", Format.formatBytes(twoGbytes));
        long almostOneTbyte = 1099511627775L;
        assertEquals("1,024 GBytes", Format.formatBytes(almostOneTbyte));
    }

    @Test
    public void formatBytesReturnTbytesTest() {
        long oneTbyte = 1099511627776L;
        assertEquals("1 TBytes", Format.formatBytes(oneTbyte));
        long twoTbytes = 2199023255552L;
        assertEquals("2 TBytes", Format.formatBytes(twoTbytes));
    }



    @Test
    public void formatBytesShortReturnKbTest() {
        long oneKb = 1024L;
        long twoKb = 2048L;
        long almostOneMb = 1048575L;
        assertEquals("1 kB", Format.formatBytesShort(oneKb));
        assertEquals("2 kB", Format.formatBytesShort(twoKb));
        assertEquals("1,024 kB", Format.formatBytesShort(almostOneMb));
    }

    @Test
    public void formatBytesShortReturnMbTest() {
        long oneMb = 1048576L;
        long twoMb = 2097152L;
        long almostOneGb = 1073741823L;
        assertEquals("1 MB", Format.formatBytesShort(oneMb));
        assertEquals("2 MB", Format.formatBytesShort(twoMb));
        assertEquals("1,024 MB", Format.formatBytesShort(almostOneGb));
    }

    @Test
    public void formatBytesShortReturnGbTest() {
        long oneGb = 1073741824L;
        long twoGb = 2147483648L;
        long almostOneTb = 1099511627775L;
        assertEquals("1 GB", Format.formatBytesShort(oneGb));
        assertEquals("2 GB", Format.formatBytesShort(twoGb));
        assertEquals("1,024 GB", Format.formatBytesShort(almostOneTb));
    }

    @Test
    public void formatBytesShortReturnTbTest() {
        long oneTb = 1099511627776L;
        long twoTb = 2199023255552L;
        assertEquals("1 TB", Format.formatBytesShort(oneTb));
        assertEquals("2 TB", Format.formatBytesShort(twoTb));
    }


    @Test
    public void formatBytesShortReturnUnknownTest() {
        long unknownBytes = -250L;
        assertEquals("?? kB", Format.formatBytesShort(unknownBytes));

    }


    @Test
    public void formatTimeFrameReturnMillisecondsTest() {
        long milliseconds = 1000L;
        assertEquals(milliseconds + "ms", Format.formatTimeframe(milliseconds));
    }

    @Test
    public void formatTimeFrameReturnSecondsTest() {
        long twoSeconds = 2000L;
        assertEquals( "2s", Format.formatTimeframe(twoSeconds));
        long almostTwoMinutes = 1000L * 60 * 2  - 1;
        assertEquals("119s", Format.formatTimeframe(almostTwoMinutes));
    }

    @Test
    public void formatTimeFrameReturnsMinutesTest() {
        long twoMinutes = 1000L * 60 * 2;
        assertEquals("2m", Format.formatTimeframe(twoMinutes));
        long almostTwoHours = 1000L * 60 * 60 * 2 - 1;
        assertEquals("119m", Format.formatTimeframe(almostTwoHours));
    }

    @Test
    public void formatTimeframeReturnsHoursTest() {
        long twoHours = 1000L * 60 * 60 * 2;
        assertEquals("2h", Format.formatTimeframe(twoHours));
        long almostTwoDays = 1000L * 60 * 60 * 2 * 24  - 1;
        assertEquals("47h", Format.formatTimeframe(almostTwoDays));


    }

    @Test
    public void formatTimeframeReturnsDaysTest() {
        long twoDays = 1000L * 60 * 60 * 2 * 24;
        assertEquals("2d", Format.formatTimeframe(twoDays));
        long oneWeek = 1000L * 60 * 60 * 7 * 24;
        assertEquals("7d", Format.formatTimeframe(oneWeek));
    }

    @Test
    public void formatTimeLongTest() {
        Date date = null;
        assertNull(Format.formatTimeLong(date));

        date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("K:mm:ss aa zzz");

        assertEquals(simpleDateFormat.format(date), Format.formatTimeLong(date));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, 25);

        assertEquals(simpleDateFormat.format(calendar.getTime()), Format.formatTimeLong(calendar.getTime()));
    }

    @Test
    public void formatTimeShortTest() {
        Date date = null;
        assertNull(Format.formatTimeShort(date));

        date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("K:mm aa");

        assertEquals(simpleDateFormat.format(date), Format.formatTimeShort(date));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, 2);

        assertEquals(simpleDateFormat.format(calendar.getTime()), Format.formatTimeShort(calendar.getTime()));
    }

    @Test
    public void formatDateCanonicalTest() {

        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy");

        assertEquals(simpleDateFormat.format(date), Format.formatDateCanonical(date));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, 48);

        assertEquals(simpleDateFormat.format(calendar.getTime()), Format.formatDateCanonical(calendar.getTime()));

    }

    @Test
    public void formatDateWithTimeCanonicalTest() {

        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy H:m");

        assertEquals(simpleDateFormat.format(date), Format.formatDateWithTimeCanonical(date));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, 48);

        assertEquals(simpleDateFormat.format(calendar.getTime()), Format.formatDateWithTimeCanonical(calendar.getTime()));

        calendar.add(Calendar.MINUTE, -25);
        assertEquals(simpleDateFormat.format(calendar.getTime()), Format.formatDateWithTimeCanonical(calendar.getTime()));


        calendar.add(Calendar.SECOND, 27);
        assertEquals(simpleDateFormat.format(calendar.getTime()), Format.formatDateWithTimeCanonical(calendar.getTime()));

    }

    @Test
    public void parseDateCanonicalTest() {

        String stringToParse = "25 MAY 2018";

        try {
            Date date = Format.parseDateCanonical(stringToParse);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            //May is the 5th month but calendar returns months starting from 0
            assertEquals(4, calendar.get(Calendar.MONTH));
            assertEquals(25, calendar.get(Calendar.DAY_OF_MONTH));
            assertEquals(2018, calendar.get(Calendar.YEAR));
        } catch (ParseException e) {
            e.printStackTrace();
            fail("Exception was thrown when parsing a string in the correct format");
        }
    }

    @Test(expected = ParseException.class)
    public void parseDateCanonicalExceptionTest() throws ParseException {

        String stringToParse = "This is a test string";
        Format.parseDateCanonical(stringToParse);

    }

    @Test
    public void formatDateShortRenderTodayYesterdayTrueTest() {
        Date date = null;
        assertNull(Format.formatDateShort(date, true));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("K:mm aa");
        Date today = new Date();

        assertEquals("Today " + simpleDateFormat.format(today), Format.formatDateShort(today, true));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.add(Calendar.HOUR, 24);
        Date tomorrow = calendar.getTime();

        assertEquals("Tomorrow " + simpleDateFormat.format(tomorrow), Format.formatDateShort(tomorrow, true));

        calendar.setTime(today);
        calendar.add(Calendar.HOUR, -24);
        Date yesterday = calendar.getTime();

        assertEquals("Yesterday " + simpleDateFormat.format(yesterday), Format.formatDateShort(yesterday, true));

        SimpleDateFormat format = new SimpleDateFormat("M/d/yy K:mm aa");

        calendar.setTime(today);
        calendar.add(Calendar.HOUR, 48);
        Date dayAfterTomorrow = calendar.getTime();

        assertEquals(format.format(dayAfterTomorrow), Format.formatDateShort(dayAfterTomorrow));
    }

    @Test
    public void formatDateShortRenderTodayYesterdayFalseTest() {
        Date date = null;
        assertNull(Format.formatDateShort(date, false));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("M/d/yy K:mm aa");
        Date today = new Date();
        assertEquals(simpleDateFormat.format(today), Format.formatDateShort(today, false));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.add(Calendar.HOUR, 24);
        Date tomorrow = calendar.getTime();

        assertEquals(simpleDateFormat.format(tomorrow), Format.formatDateShort(tomorrow, false));

        calendar.setTime(today);
        calendar.add(Calendar.HOUR, -24);
        Date yesterday = calendar.getTime();

        assertEquals(simpleDateFormat.format(yesterday), Format.formatDateShort(yesterday, false));

    }

    @Test
    public void formatDateShortTest() {
        Date date = null;
        assertNull(Format.formatDateShort(date, true));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("K:mm aa");
        Date today = new Date();
        assertEquals("Today " + simpleDateFormat.format(today), Format.formatDateShort(today));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.add(Calendar.HOUR, 24);
        Date tomorrow = calendar.getTime();

        assertEquals("Tomorrow " + simpleDateFormat.format(tomorrow), Format.formatDateShort(tomorrow));

        calendar.setTime(today);
        calendar.add(Calendar.HOUR, -24);
        Date yesterday = calendar.getTime();

        assertEquals("Yesterday " + simpleDateFormat.format(yesterday), Format.formatDateShort(yesterday));
    }


    @Test
    public void formatDecimalTest() {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);
        assertEquals(numberFormat.format(25000D), Format.formatDecimal(25000D));
        assertEquals(numberFormat.format(123.456D), Format.formatDecimal(123.456D));
        assertEquals(numberFormat.format(-2230.009D), Format.formatDecimal(-2230.009D));
    }

    @Test
    public void formatLong() {
        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        assertEquals(numberFormat.format(25000L), Format.formatLong(25000L));
        assertEquals(numberFormat.format(12345678L),Format.formatLong(12345678L));
        assertEquals(numberFormat.format(-8234000L), Format.formatLong(-8234000L));

    }

    @Test
    public void formatPercentTest() {
        double number = 200D;
        assertEquals("100%",Format.formatPercent(number));
        number = 25D;
        assertEquals("25%", Format.formatPercent(number));
        number = 63.999D;
        assertEquals("64%", Format.formatPercent(number));
        number = 99.9999999999999D;
        assertEquals("99%", Format.formatPercent(number));
        number = 100D;
        assertEquals("100%", Format.formatPercent(number));

    }

    @Test
    public void formatBoolean() {
        assertEquals(Translation.get("general.yes"), Format.formatBoolean(true));
        assertEquals(Translation.get("general.no"), Format.formatBoolean(false));
    }

    @Test
    public void formatDeltaTime() {
        long lessThanOneMinute = 59999L;
        assertEquals(Translation.get("format.less_than_one_minute"), Format.formatDeltaTime(lessThanOneMinute));


        long fifteenMinutes = 900000L;
        assertEquals(Translation.get("format.n.minutes", "15"), Format.formatDeltaTime(fifteenMinutes));

        long threeHours = 10800000L;
        assertEquals(Translation.get("format.n.hours", "3"), Format.formatDeltaTime(threeHours));

        long oneDay = 86400000L;
        assertEquals(Translation.get("format.n.hours", "24"), Format.formatDeltaTime(oneDay));

        long twoDays = 172800000L;
        assertEquals(Translation.get("format.n.days", "2"), Format.formatDeltaTime(twoDays));

        //DEFECT: In format delta time minutes >= 1 then minutes == 1 will never be executed
        long oneMinute = 60000L;
        assertEquals(Translation.get("format.one_minute"), Format.formatDeltaTime(oneMinute));

    }

}