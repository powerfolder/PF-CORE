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
import de.dal33t.powerfolder.util.Profiling;
import de.dal33t.powerfolder.util.ProfilingEntry;

public class ProfilingTest extends TestCase {

    protected void tearDown() throws Exception {
        super.tearDown();
        Profiling.setEnabled(false);
    }

    public void testURISplit() {
        assertEquals("/webdav/NAS/",
                Profiling.shortenURI("/webdav/NAS/QNAP/CloudBackup/vm-store-daten/vm-store/Belege/5000/VM-Store/"));
        assertEquals("/webdav/NAS/",
                Profiling.shortenURI("/webdav/NAS/QNAP/CloudBackup/vm-store-daten/vm-store/Belege/5000/VM-Store"));
        assertEquals("/webdav/NAS/",
                Profiling.shortenURI("/webdav/NAS/QNAP/CloudBackup/vm-store-daten/vm-store/Belege/"));
        assertEquals("/webdav/NAS/", Profiling.shortenURI("/webdav/NAS/"));
        assertEquals("/webdav/NAS", Profiling.shortenURI("/webdav/NAS"));
        assertEquals("/webdav/", Profiling.shortenURI("/webdav/"));
        assertEquals("/webdav", Profiling.shortenURI("/webdav"));
        assertEquals("webdav", Profiling.shortenURI("webdav"));

        assertEquals("webdav/NAS/",
                Profiling.shortenURI("webdav/NAS/QNAP/CloudBackup/vm-store-daten/vm-store/Belege/5000/VM-Store/"));
        assertEquals("webdav/NAS/", Profiling.shortenURI("webdav/NAS/QNAP"));
        assertEquals("webdav/NAS/", Profiling.shortenURI("webdav/NAS/"));
        assertEquals("webdav/NAS", Profiling.shortenURI("webdav/NAS"));

        assertEquals("exe", Profiling.shortenURI("exe"));
        assertEquals("/newjoinfolder", Profiling.shortenURI("/newjoinfolder"));
        assertEquals("/wapi/files/",
                Profiling.shortenURI("/wapi/files/Mjk5ZGRTbkVXTVI5SGMzS3gydnE1/products%2FWB0R5L90S%20(2)/Static_Full_Version/css/plugins"));
        assertEquals("png/", Profiling.shortenURI("png/"));
    }

    /**
     * To take a look at the API.
     * <p>
     * Profiling takes 8 additional lines.
     */
    public void testAPI() {
        ProfilingEntry profilingEntry1 = null;
        if (Profiling.ENABLED) {
            profilingEntry1 = Profiling.start("Test profile 1");
        }
        try {

            // To be Test code
            System.out.println(Profiling.class.getSimpleName());

        } finally {
            Profiling.end(profilingEntry1);
        }
    }

    public void testTiming() {
        Profiling.setEnabled(true);
        ProfilingEntry profilingEntry1 = Profiling.start("Test profile 1");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue("elapsedMilliseconds >= 10000", profilingEntry1.elapsedMilliseconds() >= 10000);
        assertTrue("elapsedMilliseconds <= 10100", profilingEntry1.elapsedMilliseconds() <= 10100);
        assertTrue(profilingEntry1.getOperationName().equals("Test profile 1"));

        ProfilingEntry profilingEntry2 = Profiling.start("Test profile 2");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue("elapsedMilliseconds >= 2000", profilingEntry2.elapsedMilliseconds() >= 2000);
        assertTrue("elapsedMilliseconds <= 2100",profilingEntry2.elapsedMilliseconds() <= 2100);
        assertTrue(profilingEntry2.getOperationName().equals("Test profile 2"));
    }

    public void testDisabled() {
        Profiling.setEnabled(false);
        ProfilingEntry profilingEntry1 = Profiling.start("Test profile 1");
        assertNull(profilingEntry1);
    }

    public void testAutoOperationName() {
        Profiling.setEnabled(true);
        ProfilingEntry profilingEntry1 = Profiling.start();
        assertNotNull(profilingEntry1);
        Profiling.end(profilingEntry1);
        assertTrue(Profiling.dumpStats().contains("ProfilingTest"));
        assertTrue(Profiling.dumpStats().contains("testAutoOperationName"));
    }
}