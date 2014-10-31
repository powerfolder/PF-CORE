/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.Adler32;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.RingBuffer;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;
import de.dal33t.powerfolder.util.delta.FilePartsRecordBuilder;
import de.dal33t.powerfolder.util.delta.FilePartsState;
import de.dal33t.powerfolder.util.delta.MatchInfo;
import de.dal33t.powerfolder.util.delta.PartInfo;
import de.dal33t.powerfolder.util.delta.PartInfoMatcher;
import de.dal33t.powerfolder.util.delta.RollingAdler32;
import de.dal33t.powerfolder.util.delta.RollingChecksum;
import de.dal33t.powerfolder.util.delta.FilePartsState.PartState;

/**
 * Testcase for "delta encoding".
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $
 */
public class DeltaTest extends TestCase {
    private final static int ADLER_RS = 10;

    /*
     * This test was used to check if the old and new implementation do the same
     * public void testNewBuilder() throws NoSuchAlgorithmException, IOException
     * { final int PSIZE = 4096, VFSIZE = 1024 1024 10 + 1111, UPDATE_SIZE =
     * 1337; Random prng = new Random(); FilePartsRecordBuilder b1 = new
     * FilePartsRecordBuilder(new Adler32(),
     * MessageDigest.getInstance("SHA-256"), MessageDigest .getInstance("MD5"));
     * FilePartsRecordBuilder b2 = new FilePartsRecordBuilder(new Adler32(),
     * MessageDigest.getInstance("SHA-256"), MessageDigest .getInstance("MD5"),
     * PSIZE); FilePartsRecordBuilder b3 = new FilePartsRecordBuilder(new
     * Adler32(), MessageDigest.getInstance("SHA-256"), MessageDigest
     * .getInstance("MD5"), PSIZE); byte data[] = new byte[VFSIZE]; for (int i =
     * 0; i < data.length; i++) { data[i] = (byte) prng.nextInt(255); }
     * FilePartsRecord r1 = b1.buildFilePartsRecord(new
     * ByteArrayInputStream(data), PSIZE); b3.update(data); int i = 0; while (i
     * < VFSIZE) { if (i < 1024) { b2.update(data[i++]); } else { int us =
     * Math.min(VFSIZE - i, UPDATE_SIZE); b2.update(data, i, us); i += us; } }
     * FilePartsRecord r2 = b2.getRecord(); FilePartsRecord r3 = b3.getRecord();
     * assertEquals(r1, r2); assertEquals(r1, r3); }
     */
    public void xtestAdlerMultiple() throws Exception {
        for (int i = 0; i < 40; i++) {
            testAdler();
            tearDown();
            setUp();
        }
    }

    public void testAdler() throws UnsupportedEncodingException {
        // Reference implementation from SUN, too bad it doesn't support rolling
        Adler32 ref = new Adler32();
        Random r = new Random();

        RollingChecksum ch = new RollingAdler32(ADLER_RS);
        ch.update("Wikipedia".getBytes("ASCII"));
        // Taken straight from the wikipedia site
        assertEquals(ch.getValue(), 0x11E60398);
        ch.reset();
        // Test timing on large input
        long millis = System.currentTimeMillis();
        for (int i = 0; i < 5 * 1024 * 1024; i++) {
            ch.update((byte) i);
        }
        // On my system the time difference is ~80ms so this should work for
        // slower machines too
        // Of course this test is kind of stupid, maybe someone else has a good
        // idea for
        // performance testing ?

        // Uncomment for performance check
        // assertTrue(System.currentTimeMillis() - millis < 1000);
        ch.reset();
        byte[] data = new byte[2048];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (r.nextInt(256));
        }
        ch.update(data, 0, 2048);
        ref.update(data, data.length - ADLER_RS, ADLER_RS);
        long cs1 = ch.getValue();
        ch.reset();
        ch.update(data, 1024, 1024);
        assertEquals(ref.getValue(), cs1);
        assertEquals(cs1, ch.getValue());
        data = new byte[10000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (r.nextInt(256));
        }
        ch = new RollingAdler32(data.length);
        ref.reset();
        for (int i = 4096; i < 8192; i++) {
            ch.reset();
            ch = new RollingAdler32(i);
            ch.update(data, 0, i);
            for (int j = 0; j <= 100; j++) {
                ref.reset();
                ref.update(data, j, i);
                assertTrue("At " + i + ", " + j + ": checksum mismatch", ref
                    .getValue() == ch.getValue());
                if (j + i + 3 < data.length) {
                    ch.update(data[j + i]);
                }
            }
        }
    }

    public void testPartInfos() throws NoSuchAlgorithmException, IOException {
        MessageDigest d1, d2;
        FilePartsRecordBuilder pim = new FilePartsRecordBuilder(new Adler32(),
            d1 = MessageDigest.getInstance("SHA-256"), MessageDigest
                .getInstance("MD5"), 128);
        Random r = new Random();
        byte[] data = new byte[1024 * 1024 + 7];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) r.nextInt(256);
        }
        pim.update(data);
        FilePartsRecord pi = pim.getRecord();
        Adler32 ref = new Adler32();
        for (int i = 0; i < data.length / 128; i++) {
            ref.reset();
            ref.update(data, i * 128, 128);
            assertTrue("Failed at index " + i + ", expected " + ref.getValue()
                + " but got " + pi.getInfos()[i].getChecksum(),
                ref.getValue() == pi.getInfos()[i].getChecksum());
        }

        // Block searching test
        Map<Long, List<PartInfo>> m = new HashMap<Long, List<PartInfo>>();
        for (PartInfo p : pi.getInfos()) {
            List<PartInfo> inf;
            inf = m.get(p.getChecksum());
            if (inf == null) {
                m.put(p.getChecksum(), inf = new LinkedList<PartInfo>());
            }
            inf.add(p);
        }
        RollingAdler32 ra = new RollingAdler32(128);
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        PartInfoMatcher matcher = new PartInfoMatcher(new ByteArrayInputStream(
            data), ra, sha256, pi.getInfos());
        MatchInfo[] infos = performMatch(matcher);

        ra.update(data, 0, 127);
        int matches = 0;
        byte[] tmp = new byte[data.length + 127];
        System.arraycopy(data, 0, tmp, 0, data.length);

        for (int i = 127; i < tmp.length; i++) {
            ra.update(tmp[i]);
            long sum = ra.getValue();
            List<PartInfo> plist = m.get(sum);

            if (plist != null) {
                for (PartInfo p : plist) {
                    // Potential match
                    sha256.update(tmp, i - 127, 128);
                    if (Arrays.equals(sha256.digest(), p.getDigest())) {
                        // This SHOULD be a match
                        for (int j = 0; j < 128; j++) {
                            assertEquals(tmp[i - 127 + j], tmp[(int) (128 * p
                                .getIndex() + j)]);
                        }
                        assertEquals(i - 127, infos[matches]
                            .getMatchedPosition());
                        matches++;
                    }
                }
            } else {
                if (i % 128 == 127) {
                    fail("Expected match at block: " + i / 128);
                }
            }
        }
        // Make sure we found all frames (Maybe even more due to randomness)
        assertTrue("Found " + matches + ", but expected at least "
            + pi.getInfos().length, matches >= pi.getInfos().length);
        assertTrue("Found " + infos.length + ", but expected " + matches
            + " matches!", infos.length == matches);
        // assertEquals(pim.getProcessedBytesCount().getValue(), matcher
        // .getProcessedBytes().getValue());

        for (int i = 128; i <= 4096; i <<= 1) {
            FilePartsRecordBuilder rolpim = new FilePartsRecordBuilder(
                new RollingAdler32(16384), d2 = MessageDigest
                    .getInstance("SHA-256"), MessageDigest.getInstance("MD5"),
                i);
            pim = new FilePartsRecordBuilder(new Adler32(), d1 = MessageDigest
                .getInstance("SHA-256"), MessageDigest.getInstance("MD5"), i);
            assertEquals(d1.getProvider(), d2.getProvider());
            assertEquals(d1.getProvider(), sha256.getProvider());

            pim.update(data);
            rolpim.update(data);
            FilePartsRecord fpr = pim.getRecord();
            FilePartsRecord fpr2 = rolpim.getRecord();
            assertEquals(fpr, fpr2);
            // assertEquals(fpr.getInfos().length, fpr2.getInfos().length);
            // assertTrue(Arrays.equals(fpr.getFileDigest(),
            // fpr2.getFileDigest()));
            // for (int j = 0; j < fpr.getInfos().length; j++) {
            // assertEquals(fpr.getInfos()[j], fpr2.getInfos()[j]);
            // }
            infos = performMatch(new PartInfoMatcher(new ByteArrayInputStream(
                data), new RollingAdler32(i), sha256, fpr.getInfos()));
            // Allow distance of 1 since we're not using a buffer which is
            // dividable by any power of 2
            if (Math.abs(data.length / i - infos.length) > 1) {
                fail("Matching error at blocksize " + i + ", expected "
                    + data.length / i + " but found " + infos.length);
            }
        }

        for (int i = 0; i < data.length / 128; i++) {
            int j = r.nextInt(data.length / 128);
            for (int k = 0; k < 128; k++) {
                byte tmp2 = data[j * 128 + k];
                data[j * 128 + k] = data[i * 128 + k];
                data[i * 128 + k] = tmp2;
            }
        }

        infos = performMatch(new PartInfoMatcher(
            new ByteArrayInputStream(data), ra, sha256, pi.getInfos()));
        assertEquals((data.length + 127) / 128, infos.length);
    }

    private MatchInfo[] performMatch(PartInfoMatcher partInfoMatcher)
        throws IOException
    {
        List<MatchInfo> mil = new LinkedList<MatchInfo>();
        MatchInfo inf = null;
        try {
            while ((inf = partInfoMatcher.nextMatch()) != null) {
                mil.add(inf);
            }
        } catch (InterruptedException e) {
            fail(e.toString());
        }
        return mil.toArray(new MatchInfo[0]);
    }

    public void testMultipleLens() throws NoSuchAlgorithmException, IOException
    {
        Random rng = new Random();
        byte tmp[] = new byte[1000000];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = (byte) rng.nextInt(256);
        }
        for (int i = 100000; i < 1000000; i += rng.nextInt(5000) + 5000) {
            int j = rng.nextInt(1000) + 1000;

            FilePartsRecordBuilder rolpim = new FilePartsRecordBuilder(
                new RollingAdler32(j), MessageDigest.getInstance("SHA-256"),
                MessageDigest.getInstance("MD5"), j);
            rolpim.update(tmp, 0, i);
            FilePartsRecord rec = rolpim.getRecord();
            MatchInfo mi[] = performMatch(new PartInfoMatcher(
                new ByteArrayInputStream(tmp, 0, i), new RollingAdler32(j),
                MessageDigest.getInstance("SHA-256"), rec.getInfos()));
            assertEquals("Expected " + rec.getInfos().length + " but was "
                + mi.length + ", i=" + i + ", j=" + j, rec.getInfos().length,
                mi.length);
        }
    }

    private void testDigest(String alg) throws NoSuchAlgorithmException {
        MessageDigest d1 = MessageDigest.getInstance(alg);
        MessageDigest d2 = MessageDigest.getInstance(alg);
        assertEquals(d1.getProvider(), d2.getProvider());
        Random r = new Random();
        for (int i = 0; i < 1024 * 1024; i++) {
            for (int j = 0; j < 5; j++) {
                byte b = (byte) r.nextInt(256);
                d1.update(b);
                d2.update(b);
                // Use up some memory
                d1.digest(new byte[]{1});
                d2.digest(new byte[]{1});
            }
        }
        byte[] _m1 = d1.digest(new byte[]{1});
        byte[] _m2 = d2.digest(new byte[]{1});
        assertTrue(MessageDigest.isEqual(_m1, _m2));
        assertTrue(Arrays.equals(_m1, _m2));
        // FIXME in JAVA: MessageDigest.digest() does not perfom RESET!
        // RE: Read the API doc please - it does perform a reset.
        for (int i = 0; i < 1024 * 1024; i++) {
            for (int j = 0; j < 200; j++) {
                byte b = (byte) r.nextInt(256);
                d1.update(b);
                d2.update(b);
            }
            byte[] m1 = d1.digest(new byte[]{1});
            byte[] m2 = d2.digest(new byte[]{1});
            assertTrue("Digest not equal on alg '" + alg + "'. Digest 1 len: "
                + m1.length + ", Digest 2 len: " + m2.length + " after " + i
                + " runs", MessageDigest.isEqual(m1, m2));
            assertTrue("Digest not equal on alg '" + alg + "'. Digest 1 len: "
                + m1.length + ", Digest 2 len: " + m2.length + " after " + i
                + " runs", Arrays.equals(m1, m2));
        }
    }

    /**
     * This test CANNOT fail (unless see later) - it's just there to punish your
     * CPU. If this test fails there's a huge problem: Either your JVM is buggy
     * or your machine has a problem
     */
    public void xtestDigests() throws NoSuchAlgorithmException {
        testDigest("MD5");
        testDigest("SHA-256");
        // We don't actually use SHA-1 and this causes strange errors. disabled
        // RE: Yes because the bamboo server had a hardware/software problem.
        // This HAS to work perfectly fine.
        testDigest("SHA-1");
    }

    public void testRingBuffer() {
        for (int i = 1; i < 8192; i += 7) {
            for (int k = 0; k < 3; k++) {
                RingBuffer rb = new RingBuffer(i);
                for (int j = 0; j < i; j++) {
                    assertEquals(i - j, rb.remaining());
                    assertEquals(j, rb.available());
                    rb.write(j & 0xff);
                }
                assertEquals(0, rb.remaining());
                for (int j = 0; j < i; j++) {
                    assertEquals(j, rb.remaining());
                    assertEquals(i - j, rb.available());
                    assertEquals(j & 0xff, rb.read());
                }
            }
        }
        Random rng = new Random();
        RingBuffer rb = new RingBuffer(100);
        byte buf[] = new byte[100];
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                for (int k = 0; k < j; k++) {
                    rb.write(rng.nextInt(256));
                }
                rb.peek(buf, 0, j);
                for (int k = 0; k < j; k++) {
                    assertEquals("At i = " + i + ", j = " + j + ", k = " + k,
                        rb.read(), buf[k] & 0xff);
                }
            }
        }
    }

    public void testPartInfosMultipleTimes() throws Exception {
        // Decreased from 1000 to 10. But if we're going to use that buggy
        // bamboo machine again this goes right
        // back to 1000.
        for (int i = 0; i < 10; i++) {
            System.out.println(i);
            testPartInfos();
            tearDown();
            setUp();
        }
    }

    /**
     * Note: This test will always pass, it's only there to note some
     * performance values
     */
    public void testRollingAdlerPerformance() {
        /*
         * RollingAdler32 ra = new RollingAdler32(8192); long time =
         * System.currentTimeMillis(); for (int i = 0; i < 100000000; i++) {
         * ra.update(255); ra.getValue(); } System.out.println("RollingAdler
         * measured time: " + (System.currentTimeMillis() - time) + " ms");
         */
    }

    public void testDataSplitter() {
        List<MatchInfo> mis = new LinkedList<MatchInfo>();
        long matchLen = 1000, maxData = 10000;
        // If you change the array make sure to fix the checks below (including
        // the fndRanges one)
        int mip[] = new int[]{100, 1100, 4000, 5000};
        // Create cluttered data
        for (int i = 0; i < mip.length; i++) {
            mis.add(new MatchInfo(null, mip[i]));
        }

        FilePartsState ds = new FilePartsState(maxData);
        for (MatchInfo m : mis) {
            ds.setPartState(Range.getRangeByLength(m.getMatchedPosition(),
                matchLen), PartState.AVAILABLE);
        }
        Range r = ds.findPart(Range.getRangeByNumbers(0, 9999),
            PartState.NEEDED);
        assertEquals(0, r.getStart());
        assertEquals(99, r.getEnd());

        r = ds.findPart(Range.getRangeByNumbers(100, 1001), PartState.NEEDED);
        assertNull(r);

        r = ds.findPart(Range.getRangeByNumbers(1100, 2100), PartState.NEEDED);
        assertEquals(2100, r.getStart());
        assertEquals(2100, r.getEnd());

        r = ds.findPart(Range.getRangeByNumbers(5000, 10000), PartState.NEEDED);
        assertEquals(6000, r.getStart());
        assertEquals(9999, r.getEnd());

        r = ds.findPart(Range.getRangeByNumbers(0, 9999), PartState.NEEDED);
        assertEquals(0, r.getStart());
        assertEquals(99, r.getEnd());

        r = ds.findPart(Range.getRangeByLength(9999, 1), PartState.NEEDED);
        assertEquals(9999, r.getStart());
        assertEquals(9999, r.getEnd());

        // Use maxdata instead of maxdata + 1 as upper bound so the loop exits
        Range todo = Range.getRangeByNumbers(0, maxData);
        int fndRanges = 0;
        while ((r = ds.findPart(todo, PartState.NEEDED)) != null) {
            // Make "sure" not to enter a infinite loop
            assertTrue(r.getStart() >= todo.getStart());

            fndRanges++;

            todo = Range.getRangeByNumbers(r.getEnd() + 1, maxData);
        }

        // Adjust this when changing mip above
        assertEquals(3, fndRanges);
    }

    public void testRange() {
        Range a = Range.getRangeByNumbers(0, 1000);
        Range b = Range.getRangeByNumbers(500, 1500);
        assertTrue(a.intersects(b));
        assertTrue(b.intersects(a));
        assertTrue(!a.contains(b));
        assertTrue(!b.contains(a));
        assertTrue(a.contains(a));
        assertTrue(b.contains(b));

        b = Range.getRangeByNumbers(500, 1000);
        assertTrue(a.intersects(b));
        assertTrue(b.intersects(a));
        assertTrue(a.contains(b));
    }
}
