/*
* Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
* Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
*/
package de.dal33t.powerfolder.transfer;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import de.dal33t.powerfolder.util.test.ControllerTestCase;
import static org.junit.jupiter.api.Assertions.*;

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
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        recorder = new BandwidthStatsRecorder(getController());
    }

    /**
     * Test that stats coalesce.
     */
    @Test
    public void testBasicStats() {
        Calendar cal = Calendar.getInstance();
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1000L, 101L));
        recorder.handleBandwidthStat(new BandwidthStat(cal.getTime(),
                BandwidthLimiterInfo.LAN_INPUT, 1001L, 107L));
        Set<CoalescedBandwidthStat> set = recorder.getBandwidthStats();

        // Check that it coalesces stats.
        assertEquals( 1, set.size(),"Wrong size");

        // Check that the values sum.
        CoalescedBandwidthStat stat = set.iterator().next();
        assertEquals( 2001L, stat.getInitialBandwidth(),"Wrong initial");
        assertEquals( 208L, stat.getResidualBandwidth(),"Wrong residual");
        assertEquals( 899L, stat.getPeakBandwidth(),"Wrong peak");
    }

    /**
     * Test that stats for different time get summed in different entries.
     */
    @Test
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
        assertEquals( 2, set.size(),"Wrong size");

        // Check that the values sum.
        Iterator<CoalescedBandwidthStat> iterator = set.iterator();
        CoalescedBandwidthStat stat1 = iterator.next();
        assertEquals( 2001L, stat1.getInitialBandwidth(),"Wrong initial 1");
        assertEquals( 208L, stat1.getResidualBandwidth(),"Wrong residual 1");
        assertEquals( 899L, stat1.getPeakBandwidth(),"Wrong peak 1");
        CoalescedBandwidthStat stat2 = iterator.next();
        assertEquals( 999L, stat2.getInitialBandwidth(),"Wrong initial 2");
        assertEquals( 99L, stat2.getResidualBandwidth(),"Wrong residual 2");
        assertEquals( 900L, stat2.getPeakBandwidth(),"Wrong peak 2");
    }

    /**
     * Test that stats for different infos get summed in different entries.
     */
    @Test
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
        assertEquals( 2, set.size(),"Wrong size");

        // Check that the values sum.
        Iterator<CoalescedBandwidthStat> iterator = set.iterator();
        CoalescedBandwidthStat stat1 = iterator.next();
        assertEquals( 1001L, stat1.getInitialBandwidth(),"Wrong initial 1");
        assertEquals( 107L, stat1.getResidualBandwidth(),"Wrong residual 1");
        assertEquals( 894L, stat1.getPeakBandwidth(),"Wrong peak 1");
        CoalescedBandwidthStat stat2 = iterator.next();
        assertEquals( 1999L, stat2.getInitialBandwidth(),"Wrong initial 2");
        assertEquals( 200L, stat2.getResidualBandwidth(),"Wrong residual 2");
        assertEquals( 900L, stat2.getPeakBandwidth(),"Wrong peak 2");
    }

    /**
     * Test that we can prune stats by date.
     */
    @Test
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
        assertEquals( 2, set.size(),"Wrong size");

        cal.add(Calendar.HOUR, -1);

        // Remove the older record.
        recorder.pruneStats(cal.getTime());

        // Check the older one is gone.
        set = recorder.getBandwidthStats();
        assertEquals( 1, set.size(),"Wrong size");
        Iterator<CoalescedBandwidthStat> iterator = set.iterator();
        CoalescedBandwidthStat stat = iterator.next();
        assertEquals( 999L, stat.getInitialBandwidth(),"Wrong initial");
        assertEquals( 99L, stat.getResidualBandwidth(),"Wrong residual");
        assertEquals( 900L, stat.getPeakBandwidth(),"Wrong Peak");
    }

    @Test
    public void testSyntheticFields() {
        CoalescedBandwidthStat stat = new CoalescedBandwidthStat(new Date(),
                BandwidthLimiterInfo.LAN_INPUT, 5432L, 3453, 40, 7);
        assertEquals( 1979, stat.getUsedBandwidth(),"Bad used bandwidth");
        assertEquals( 36.43225331369661,
                stat.getPercentageUsedBandwidth(),"Bad percent used bandwidth");
        assertEquals( 282.7142857142857,
                stat.getAverageUsedBandwidth(),"Bad Average used bandwidth");
    }

}
