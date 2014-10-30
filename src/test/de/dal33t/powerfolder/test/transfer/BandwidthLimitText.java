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
package de.dal33t.powerfolder.test.transfer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.apache.commons.io.output.ByteArrayOutputStream;

import de.dal33t.powerfolder.transfer.BandwidthLimiter;
import de.dal33t.powerfolder.transfer.BandwidthProvider;
import de.dal33t.powerfolder.transfer.BandwidthStat;
import de.dal33t.powerfolder.transfer.BandwidthStatsListener;
import de.dal33t.powerfolder.transfer.LimitedInputStream;
import de.dal33t.powerfolder.transfer.LimitedOutputStream;

public class BandwidthLimitText extends TestCase {
    BandwidthProvider provider = new BandwidthProvider();

    public void testUnlimited() {
        BandwidthLimiter bl = BandwidthLimiter.LAN_INPUT_BANDWIDTH_LIMITER;
        try {
            assertEquals(bl.requestBandwidth(Long.MAX_VALUE), Long.MAX_VALUE);
        } catch (InterruptedException e) {
            fail(e.toString());
        }
        provider.setLimitBPS(bl, 0);
        long amount = 70000000;
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

    public void testLimiter() {
        System.out.println("BandwidthLimitTest.testLimiter");
        BandwidthLimiter bl = BandwidthLimiter.LAN_INPUT_BANDWIDTH_LIMITER;
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
        BandwidthLimiter bl = BandwidthLimiter.LAN_INPUT_BANDWIDTH_LIMITER;
        bl.setAvailable(10000);
        byte b[] = new byte[1000];

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (LimitedOutputStream out = new LimitedOutputStream(bl, bout)) {
            out.write(b);
        } catch (IOException e) {
            fail(e.toString());
        }
        assertTrue("Wrong amount left/write",
            bl.getAvailable() == 10000 - b.length);
        assertTrue("Wrong amount written", bout.size() == b.length);
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        bl.setAvailable(10000);
        int read = 0;
        try (LimitedInputStream in = new LimitedInputStream(bl, bin)) {
            read = in.read(b);
        } catch (IOException e) {
            fail(e.toString());
        }
        assertTrue("Wrong amount left/read", bl.getAvailable() == 10000 - read);
        assertTrue("Wrong amount read", bin.available() == b.length - read);
    }

    public void testProvider() {
        BandwidthLimiter bl = BandwidthLimiter.LAN_INPUT_BANDWIDTH_LIMITER;
        bl.setAvailable(0);
        provider.start();
        provider.setLimitBPS(bl, 1000);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail(e.toString());
        }
        provider.removeLimiter(bl);
        provider.shutdown();
        assertEquals(1000, bl.getAvailable());
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
        BandwidthLimiter bl = BandwidthLimiter.LAN_INPUT_BANDWIDTH_LIMITER;
        bl.setAvailable(0);
        provider.start();
        provider.setLimitBPS(bl, 1024 * 100);
        LimitedInputStream in = new LimitedInputStream(bl, new InputStream() {
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

        provider.shutdown();
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

    public void testBandwidthStats() {
        BandwidthLimiter bl = BandwidthLimiter.LAN_INPUT_BANDWIDTH_LIMITER;
        bl.setAvailable(0);
        provider.start();
        provider.setLimitBPS(bl, 1000);
        final AtomicBoolean gotStat = new AtomicBoolean();
        BandwidthStatsListener listener = new BandwidthStatsListener() {
            public void handleBandwidthStat(BandwidthStat stat) {
                System.out.println("Got a stat...");
                gotStat.set(true);
            }

            public boolean fireInEventDispatchThread() {
                return false;
            }
        };
        provider.addBandwidthStatListener(listener);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail(e.toString());
        }
        provider.removeLimiter(bl);
        provider.shutdown();
        assertTrue("Failed to get any stats?", gotStat.get());
    }
}
