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
        assertTrue(System.currentTimeMillis() - millis < 1000);
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
                .getInstance("MD5"));
        Random r = new Random();
        byte[] data = new byte[1024 * 1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) r.nextInt(256);
        }
        FilePartsRecord pi = pim.buildFilePartsRecord(new ByteArrayInputStream(
            data), 128);
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

        PartInfoMatcher matcher = new PartInfoMatcher(ra, sha256);
        MatchInfo[] infos = matcher.matchParts(new ByteArrayInputStream(data),
            pi.getInfos()).toArray(new MatchInfo[0]);

        ra.update(data, 0, 127);
        int matches = 0;
        for (int i = 127; i < data.length; i++) {
            ra.update(data[i]);
            long sum = ra.getValue();
            List<PartInfo> plist = m.get(sum);

            if (plist != null) {
                for (PartInfo p : plist) {
                    // Potential match
                    sha256.update(data, i - 127, 128);
                    if (Arrays.equals(sha256.digest(), p.getDigest())) {
                        // This SHOULD be a match
                        for (int j = 0; j < 128; j++) {
                            assertEquals(data[i - 127 + j], data[(int) (128 * p
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
        assertEquals(pim.getProcessedBytesCount().getValue(), matcher
            .getProcessedBytes().getValue());

        FilePartsRecordBuilder rolpim = new FilePartsRecordBuilder(
            new RollingAdler32(16384), d2 = MessageDigest
                .getInstance("SHA-256"), MessageDigest.getInstance("MD5"));
        assertEquals(d1.getProvider(), d2.getProvider());
        assertEquals(d1.getProvider(), sha256.getProvider());
        for (int i = 128; i <= 4096; i <<= 1) {
            FilePartsRecord fpr = pim.buildFilePartsRecord(
                new ByteArrayInputStream(data), i);
            FilePartsRecord fpr2 = rolpim.buildFilePartsRecord(
                new ByteArrayInputStream(data), i);
            assertEquals(fpr.getInfos().length, fpr2.getInfos().length);
            assertTrue(Arrays.equals(fpr.getFileDigest(), fpr2.getFileDigest()));
            for (int j = 0; j < fpr.getInfos().length; j++) {
                assertEquals(fpr.getInfos()[j], fpr2.getInfos()[j]);
            }
            PartInfoMatcher mymatcher = new PartInfoMatcher(new RollingAdler32(
                i), sha256);
            infos = mymatcher.matchParts(new ByteArrayInputStream(data),
                fpr.getInfos()).toArray(new MatchInfo[0]);
            if (data.length / i != infos.length) {
                fail("Matching error at blocksize " + i + ", expected "
                    + data.length / i + " but found " + infos.length);
            }
        }

        for (int i = 0; i < data.length / 128; i++) {
            int j = r.nextInt(data.length / 128);
            for (int k = 0; k < 128; k++) {
                byte tmp = data[j * 128 + k];
                data[j * 128 + k] = data[i * 128 + k];
                data[i * 128 + k] = tmp;
            }
        }

        infos = matcher.matchParts(new ByteArrayInputStream(data),
            pi.getInfos()).toArray(new MatchInfo[0]);
        assertEquals(data.length / 128, infos.length);
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
    public void testDigests() throws NoSuchAlgorithmException {
        testDigest("MD5");
        testDigest("SHA-256");
        // We don't actually use SHA-1 and this causes strange errors. disabled
        // RE: Yes because the bamboo server had a hardware/software problem. This HAS to work perfectly fine.
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
    }

    public void testPartInfosMultipleTimes() throws Exception {
        // Decreased from 1000 to 10. But if we're going to use that buggy bamboo machine again this goes right
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
