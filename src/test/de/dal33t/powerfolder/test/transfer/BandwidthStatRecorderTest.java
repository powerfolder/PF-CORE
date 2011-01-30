/*
* Copyright 2004 - 2011 Christian Sprajc. All rights reserved.
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
* $Id: BandwidthStatRecorderTest.java 4282 2011-01-28 03:25:09Z harry $
*/
package de.dal33t.powerfolder.test.transfer;

import de.dal33t.powerfolder.transfer.BandwidthStatsRecorder;
import de.dal33t.powerfolder.transfer.BandwidthStat;
import de.dal33t.powerfolder.transfer.BandwidthLimiterInfo;
import de.dal33t.powerfolder.test.ControllerTest;

import java.util.*;

/**
 * Set of tests to validate the BandwidthStatRecorder functionality.
 */
public class BandwidthStatRecorderTest extends ControllerTest {

    private BandwidthStatsRecorder recorder;

    /**
     * Create a BandwidthStatsRecorder to play with.
     *
     * @throws Exception
     */
    public void setUp() throws Exception {
        super.setUp();
        recorder = new BandwidthStatsRecorder(getController());
    }

    /**
     * Test that stats coalesce.
     */
    public void testBasicStats() {
        Calendar cal = Calendar.getInstance();
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1000L, 101L, 1));
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1001L, 107L, 1));
        Set<BandwidthStat> set = recorder.getStats();

        // Check that it coalesces stats.
        assertEquals("Wrong size", 1, set.size());

        // Check that the values sum.
        BandwidthStat stat = set.iterator().next();
        assertEquals("Wrong initial", 2001L, stat.getInitialBandwidth());
        assertEquals("Wrong residual", 208L, stat.getResidualBandwidth());
    }

    /**
     * Test that stats for different time get summed in different entries.
     */
    public void testStatsByDate() {
        Calendar cal = Calendar.getInstance();
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1000L, 101L, 1));
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1001L, 107L, 1));
        cal.add(Calendar.HOUR, 1);
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 999L, 99L, 1));
        Set<BandwidthStat> set = recorder.getStats();

        // Check that it coalesces stats.
        assertEquals("Wrong size", 2, set.size());

        // Check that the values sum.
        Iterator<BandwidthStat> iterator = set.iterator();
        BandwidthStat stat1 = iterator.next();
        assertEquals("Wrong initial 1", 2001L, stat1.getInitialBandwidth());
        assertEquals("Wrong residual 1", 208L, stat1.getResidualBandwidth());
        BandwidthStat stat2 = iterator.next();
        assertEquals("Wrong initial 2", 999L, stat2.getInitialBandwidth());
        assertEquals("Wrong residual 2", 99L, stat2.getResidualBandwidth());
    }

    /**
     * Test that stats for different infos get summed in different entries.
     */
    public void testStatsByInfo() {
        Calendar cal = Calendar.getInstance();
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1000L, 101L, 1));
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_OUTPUT, 1001L, 107L, 1));
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 999L, 99L, 1));
        Set<BandwidthStat> set = recorder.getStats();

        // Check that it coalesces stats.
        assertEquals("Wrong size", 2, set.size());

        // Check that the values sum.
        Iterator<BandwidthStat> iterator = set.iterator();
        BandwidthStat stat1 = iterator.next();
        assertEquals("Wrong initial 1", 1001L, stat1.getInitialBandwidth());
        assertEquals("Wrong residual 1", 107L, stat1.getResidualBandwidth());
        BandwidthStat stat2 = iterator.next();
        assertEquals("Wrong initial 2", 1999L, stat2.getInitialBandwidth());
        assertEquals("Wrong residual 2", 200L, stat2.getResidualBandwidth());
    }

    /**
     * Test that we can prune stats by date.
     */
    public void testPrune() {
        Calendar cal = Calendar.getInstance();
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1000L, 101L, 1));
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1001L, 107L, 1));
        cal.add(Calendar.HOUR, 2);
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 999L, 99L, 1));

        // Check that it coalesces stats.
        Set<BandwidthStat> set = recorder.getStats();
        assertEquals("Wrong size", 2, set.size());

        cal.add(Calendar.HOUR, -1);

        // Remove the older record.
        recorder.pruneStats(cal.getTime());

        // Check the older one is gone.
        set = recorder.getStats();
        assertEquals("Wrong size", 1, set.size());
        Iterator<BandwidthStat> iterator = set.iterator();
        BandwidthStat stat = iterator.next();
        assertEquals("Wrong initial", 999L, stat.getInitialBandwidth());
        assertEquals("Wrong residual", 99L, stat.getResidualBandwidth());
    }

}
