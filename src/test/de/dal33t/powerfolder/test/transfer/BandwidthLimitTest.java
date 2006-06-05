package de.dal33t.powerfolder.test.transfer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;

import de.dal33t.powerfolder.transfer.BandwidthLimiter;
import de.dal33t.powerfolder.transfer.BandwidthProvider;
import de.dal33t.powerfolder.transfer.LimitedInputStream;
import de.dal33t.powerfolder.transfer.LimitedOutputStream;
import junit.framework.TestCase;

public class BandwidthLimitTest extends TestCase {
    BandwidthProvider provider = new BandwidthProvider();

    public void testLimiter() {
        System.out.println("BandwidthLimitTest.testLimiter");
        BandwidthLimiter bl = new BandwidthLimiter();
        try {
            assertEquals(bl.requestBandwidth(Long.MAX_VALUE), Long.MAX_VALUE);
        } catch (InterruptedException e) {
            fail(e.toString());
        }
        bl.setAvailable(2500);
        long amount = 700;
        while (amount > 0) {
            long rem = 0;
            try {
                rem = bl.requestBandwidth(amount);
            } catch (InterruptedException e) {
                fail(e.toString());
            }
            amount -= rem;
            assertTrue("Short on amount", rem > 0);
        }
        assertTrue("Exceeded amount", amount == 0);
    }
    
    public void testLimiteds() {
        BandwidthLimiter bl = new BandwidthLimiter();
        bl.setAvailable(10000);
        byte b[] = new byte[1000];
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        LimitedOutputStream out = new LimitedOutputStream(bl, bout);
        try {
            out.write(b);
        } catch (IOException e) {
            fail(e.toString());
        }
        assertTrue("Wrong amount left/write", bl.getAvailable() == 10000 - b.length);
        assertTrue("Wrong amount written", bout.size() == b.length);
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        bl.setAvailable(10000);
        LimitedInputStream in = new LimitedInputStream(bl, bin);
        int read = 0;
        try {
            read = in.read(b);
        } catch (IOException e) {
            fail(e.toString());
        }
        assertTrue("Wrong amount left/read", bl.getAvailable() == 10000 - read);
        assertTrue("Wrong amount read", bin.available() == b.length - read);
    }
    
    public void testProvider() {
        BandwidthLimiter bl = new BandwidthLimiter();
        bl.setAvailable(0);
        provider.setLimitBPS(bl, 1000);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail(e.toString());
        }
        provider.removeLimiter(bl);
        assertTrue(bl.getAvailable() == 1000 / BandwidthProvider.PERIOD * BandwidthProvider.PERIOD);
    }
    
    public static class ReaderThread implements Runnable {
        public LimitedInputStream in;
        public long amount;
        
        public ReaderThread(LimitedInputStream in, long amount) {
            this.in = in;
            this.amount = amount;
        }


        public void run() {
            byte b[] = new byte[1024];
            while (amount > 0) {
                try {
                    amount -= in.read(b, 0, Math.min((int) amount, b.length));
                } catch (IOException e) {
                    fail(e.toString());
                }
            }
        }
    }
    
    public void testHeavyLoad() {
        BandwidthLimiter bl = new BandwidthLimiter();
        bl.setAvailable(0);
        provider.setLimitBPS(bl, 1024 * 100);
        LimitedInputStream in = new LimitedInputStream(bl, 
            new InputStream() {
                @Override
                public int read() throws IOException {
                    return 0;
                }
            });
        Thread pool[] = new Thread[400];
        for (int i = 0; i < pool.length; i++) 
            pool[i] = new Thread(new ReaderThread(in, 1000));
        
        for (int i = 0; i < pool.length; i++)
            pool[i].start();
        
        
        try {
            Thread.sleep(6000);
            pool[0].join(1);
        } catch (InterruptedException e) {
            fail(e.toString());
        }
        assertEquals(Thread.State.TERMINATED, pool[0].getState());
        for (int i = 0; i < pool.length; i++) {
            try {
                pool[i].join(1);
            } catch (InterruptedException e) {
                fail(e.toString());
            }
            assertEquals(Thread.State.TERMINATED, pool[i].getState());
        }
    }
}
