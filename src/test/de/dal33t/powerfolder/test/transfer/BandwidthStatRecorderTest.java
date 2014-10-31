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
import de.dal33t.powerfolder.transfer.CoalescedBandwidthStat;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

import java.util.*;

/**
 * Set of tests to validate the BandwidthStatRecorder functionality.
 */
public class BandwidthStatRecorderTest extends ControllerTestCase {

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
                BandwidthLimiterInfo.LAN_INPUT, 1000L, 101L));
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1001L, 107L));
        Set<CoalescedBandwidthStat> set = recorder.getBandwidthStats();

        // Check that it coalesces stats.
        assertEquals("Wrong size", 1, set.size());

        // Check that the values sum.
        CoalescedBandwidthStat stat = set.iterator().next();
        assertEquals("Wrong initial", 2001L, stat.getInitialBandwidth());
        assertEquals("Wrong residual", 208L, stat.getResidualBandwidth());
        assertEquals("Wrong peak", 899L, stat.getPeakBandwidth());
    }

    /**
     * Test that stats for different time get summed in different entries.
     */
    public void testStatsByDate() {
        Calendar cal = Calendar.getInstance();
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1000L, 101L));
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1001L, 107L));
        cal.add(Calendar.HOUR, 1);
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 999L, 99L));
        Set<CoalescedBandwidthStat> set = recorder.getBandwidthStats();

        // Check that it coalesces stats.
        assertEquals("Wrong size", 2, set.size());

        // Check that the values sum.
        Iterator<CoalescedBandwidthStat> iterator = set.iterator();
        CoalescedBandwidthStat stat1 = iterator.next();
        assertEquals("Wrong initial 1", 2001L, stat1.getInitialBandwidth());
        assertEquals("Wrong residual 1", 208L, stat1.getResidualBandwidth());
        assertEquals("Wrong peak 1", 899L, stat1.getPeakBandwidth());
        CoalescedBandwidthStat stat2 = iterator.next();
        assertEquals("Wrong initial 2", 999L, stat2.getInitialBandwidth());
        assertEquals("Wrong residual 2", 99L, stat2.getResidualBandwidth());
        assertEquals("Wrong peak 2", 900L, stat2.getPeakBandwidth());
    }

    /**
     * Test that stats for different infos get summed in different entries.
     */
    public void testStatsByInfo() {
        Calendar cal = Calendar.getInstance();
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1000L, 101L));
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_OUTPUT, 1001L, 107L));
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 999L, 99L));
        Set<CoalescedBandwidthStat> set = recorder.getBandwidthStats();

        // Check that it coalesces stats.
        assertEquals("Wrong size", 2, set.size());

        // Check that the values sum.
        Iterator<CoalescedBandwidthStat> iterator = set.iterator();
        CoalescedBandwidthStat stat1 = iterator.next();
        assertEquals("Wrong initial 1", 1001L, stat1.getInitialBandwidth());
        assertEquals("Wrong residual 1", 107L, stat1.getResidualBandwidth());
        assertEquals("Wrong peak 1", 894L, stat1.getPeakBandwidth());
        CoalescedBandwidthStat stat2 = iterator.next();
        assertEquals("Wrong initial 2", 1999L, stat2.getInitialBandwidth());
        assertEquals("Wrong residual 2", 200L, stat2.getResidualBandwidth());
        assertEquals("Wrong peak 2", 900L, stat2.getPeakBandwidth());
    }

    /**
     * Test that we can prune stats by date.
     */
    public void testPrune() {
        Calendar cal = Calendar.getInstance();
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1000L, 101L));
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1001L, 107L));
        cal.add(Calendar.HOUR, 2);
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 999L, 99L));

        // Check that it coalesces stats.
        Set<CoalescedBandwidthStat> set = recorder.getBandwidthStats();
        assertEquals("Wrong size", 2, set.size());

        cal.add(Calendar.HOUR, -1);

        // Remove the older record.
        recorder.pruneStats(cal.getTime());

        // Check the older one is gone.
        set = recorder.getBandwidthStats();
        assertEquals("Wrong size", 1, set.size());
        Iterator<CoalescedBandwidthStat> iterator = set.iterator();
        CoalescedBandwidthStat stat = iterator.next();
        assertEquals("Wrong initial", 999L, stat.getInitialBandwidth());
        assertEquals("Wrong residual", 99L, stat.getResidualBandwidth());
        assertEquals("Wrong Peak", 900L, stat.getPeakBandwidth());
    }

    public void testSyntheticFields() {
        CoalescedBandwidthStat stat = new CoalescedBandwidthStat(new Date(),
                BandwidthLimiterInfo.LAN_INPUT, 5432L, 3453, 40, 7);
        assertEquals("Bad used bandwidth", 1979, stat.getUsedBandwidth());
        assertEquals("Bad percent used bandwidth", 36.43225331369661,
                stat.getPercentageUsedBandwidth());
        assertEquals("Bad Average used bandwidth", 282.7142857142857,
                stat.getAverageUsedBandwidth());
    }

}
