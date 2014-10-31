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
package de.dal33t.powerfolder.test.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.SimpleTimeEstimator;
import de.dal33t.powerfolder.util.ui.TimeEstimator;

/**
 * Test for the Util class.
 * 
 * @see Util
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class UtilTest extends TestCase {

    public void testCopyResourceTo() throws IOException {
        File testFile1 = File.createTempFile("xxxxx", "yy");
        assertEquals(testFile1, Util.copyResourceTo("Translation.properties",
            null, testFile1, false, false));
        assertTrue(testFile1.length() > 100);
        assertEquals(testFile1, Util.copyResourceTo("Translation.properties",
            null, testFile1, false, false));
        assertTrue(testFile1.length() > 100);

        File testFile2 = File.createTempFile("xxxxx", "yy");
        assertNull(Util.copyResourceTo("NOTEXISTING", null, testFile2, false,
            false));
        assertTrue(testFile2.length() == 0);
    }

    public void testSplitArray() throws UnsupportedEncodingException {
        byte[] testArray = new byte[94];
        List<byte[]> output = Util.splitArray(testArray, 100);
        assertEquals(1, output.size());
        assertEquals(testArray.length, output.get(0).length);

        output = Util.splitArray(testArray, 10);
        assertEquals(10, output.size());
        assertEquals(4, output.get(9).length);

        testArray = "HALLO_DIESE_TESTX_MACHT_NIXEX".getBytes("UTF-8");
        output = Util.splitArray(testArray, 6);
        assertEquals(5, output.size());
        assertEquals("HALLO_", new String(output.get(0), "UTF-8"));
        assertEquals("DIESE_", new String(output.get(1), "UTF-8"));
        assertEquals("TESTX_", new String(output.get(2), "UTF-8"));
        assertEquals("MACHT_", new String(output.get(3), "UTF-8"));
        assertEquals("NIXEX", new String(output.get(4), "UTF-8"));
    }

    public void testMergeArray() throws UnsupportedEncodingException {
        List<byte[]> arrayList = new ArrayList<byte[]>();
        arrayList.add("TEST".getBytes("UTF-8"));
        arrayList.add("1".getBytes("UTF-8"));
        arrayList.add("|||HEYHO".getBytes("UTF-8"));
        arrayList.add("XXX".getBytes("UTF-8"));

        byte[] output = Util.mergeArrayList(arrayList);
        String outputStr = new String(output, "UTF-8");
        assertEquals("TEST1|||HEYHOXXX", outputStr);
    }

    public void testSimpleTimeEstimation() throws InterruptedException {
        SimpleTimeEstimator estimator = new SimpleTimeEstimator();
        long now = new Date().getTime();
        long target = now + 600000;
        int nullCount = 0;
        int actualCount = 0;
        for (int i = 1; i <= 10; i++) {
            System.out.println("testSimpleTimeEstimation " + i + " of 10...");
            Date value = estimator.updateEstimate(i);
            if (value != null) {
                // Allow for minor time calculation rounding varaitions.
                double var = Math.abs(target / 1000 - value.getTime() / 1000
                    - 6);
                assertTrue("Target: " + target + ", Value: " + value.getTime()
                    + ", Var: " + var, var <= 2);
            }
            if (value == null) {
                // First attempt cannot calculate a date.
                nullCount++;
            } else {
                actualCount++;
            }
            Thread.sleep(6000);
        }
        assertEquals(9, actualCount);
        assertEquals(1, nullCount);
    }

    /**
     * Estimating backwards is not possible.
     * 
     * @throws InterruptedException
     */
    public void testSimpleTimeEstimationBack() throws InterruptedException {
        SimpleTimeEstimator estimator = new SimpleTimeEstimator();
        int nullCount = 0;
        int actualCount = 0;
        for (int i = 99; i >= 0; i--) {
            Date value = estimator.updateEstimate(i);
            if (value == null) {
                // First attempt cannot calculate a date.
                nullCount++;
            } else {
                actualCount++;
            }
            Thread.sleep(100);
        }
        assertEquals(0, actualCount);
        assertEquals(100, nullCount);
    }

    public void testTimeEstimation() throws InterruptedException {
        TimeEstimator t = new TimeEstimator();
        int warmup = Constants.ESTIMATION_MINVALUES;
        for (long value = 0; value < 100; value += 10) {
            Thread.sleep(100);
            t.addValue(value);
            long est = t.estimatedMillis(100);
            if (est < 0) {
                assertTrue(warmup-- > 0);
            } else {
                long exp = (100 - value) * 10;
                assertTrue("expected " + exp * 1.2 + " > " + est,
                    est < exp * 1.2);
                assertTrue("expected " + exp * 0.8 + " < " + est,
                    est > exp * 0.8);
            }
        }

        t = new TimeEstimator(20);
        warmup = Constants.ESTIMATION_MINVALUES;
        long time = System.currentTimeMillis();
        for (long value = 0; value < 100; value += 1) {
            Thread.sleep(50);
            t.addValue(value);
            long est = t.estimatedMillis(100);
            if (est < 0) {
                assertTrue(warmup-- > 0);
            } else {
                long exp = (System.currentTimeMillis() - time) * (100 - value)
                    / (value + 1);
                assertTrue("expected " + exp * 1.2 + " > " + est,
                    est < exp * 1.2);
                assertTrue("expected " + exp * 0.8 + " < " + est,
                    est > exp * 0.2);
            }
        }
    }

    public void testCompare() {
        // null because it's a static method
        assertTrue(Util.compareVersions("1", "0.9.3"));
        assertTrue(Util.compareVersions("1 devel", "0.9.3"));
        assertFalse(Util.compareVersions("0.3 devel", "0.9.3"));
        assertFalse(Util.compareVersions("0.3.0", "0.9.3"));
        assertFalse(Util.compareVersions("0.3", "0.9.3"));
        assertFalse(Util.compareVersions("0", "0.9.3"));
        assertFalse(Util.compareVersions("0.9.3", "0.9.3"));
        assertTrue(Util.compareVersions("1.0.0", "0.9.3"));
        assertFalse(Util.compareVersions("0.9.3", "1.0.0"));
        assertTrue(Util.compareVersions("0.9.3", "0.9.3 devel"));
        assertTrue(Util.compareVersions("0.9.4", "0.9.3 devel"));
        assertFalse(Util.compareVersions("1.0.1 devel", "1.0.1"));
        assertTrue(Util.compareVersions("1.0.1", "1.0.0"));
        assertTrue(Util.compareVersions("1.0.2", "1.0.1"));
        assertTrue(Util.compareVersions("1.0.2", "1.0.2 devel"));
        assertFalse(Util.compareVersions("1.0.2 devel", "1.1"));
        assertTrue(Util.compareVersions("1.1", "1.0.2 devel"));
        assertTrue(Util.compareVersions("1.1.0", "1.1.0 devel"));
        assertTrue(Util.compareVersions("1.1.1", "1.1 devel"));
        assertTrue(Util.compareVersions("1.1.1", "1.1.1 devel"));
        assertTrue(Util.compareVersions("1.1.2", "1.1.1"));
        assertTrue(Util.compareVersions("2", "1.1.1"));
        assertTrue(Util.compareVersions("2.0.1", "2.0.0"));
        assertFalse(Util.compareVersions("2.0.0", "2.0.1"));
        assertTrue(Util.compareVersions("3.9.9", "3.1.8.16"));
        assertTrue(Util.compareVersions("3.9.9", "3.1.8"));
        assertFalse(Util.compareVersions("3.9.9", "4.0.0"));
        assertFalse(Util.compareVersions("3.9.9", "4.0.1"));
        assertFalse(Util.compareVersions("3.9.9", "4.0.0 - 1.0.1"));
    }

    public void testEqual() {
        assertFalse(Util.equals(null, "bob"));
        assertFalse(Util.equals("bob", null));
        assertFalse(Util.equals("bob", "jim"));
        assertTrue(Util.equals("bob", "bob"));
        assertTrue(Util.equals(null, null));
    }
}
