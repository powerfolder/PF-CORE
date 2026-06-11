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
package de.dal33t.powerfolder.folder;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.SyncProfileConfiguration;

/**
 * Class to run some basic tests on sync profiles.
 */
public class SyncProfileTest {

    /**
     * Checks that profiles cannot have the same name or config as each other.
     */
    @Test
    public void testDuplicates() {

        SyncProfile tweedledee = SyncProfile
                .getSyncProfileByFieldList("true,false,true,false,333");
        SyncProfile tweedledum = SyncProfile
                .getSyncProfileByFieldList("false,true,false,true,334");

        // Test setting custom name with another custom name
        String tweedledumName = tweedledum.getName();
        boolean passed = false;
        try {
            tweedledee.setName(tweedledumName);
        } catch (RuntimeException e) {
            e.printStackTrace();
            passed = true;
        }
        assertTrue(
                passed,"Should not have been able to set two profiles with the same name");

        // Test setting custom name with preset name
        String presetProfileName = SyncProfile.getSyncProfilesCopy().get(0)
                .getName();
        passed = false;
        try {
            tweedledee.setName(presetProfileName);
        } catch (RuntimeException e) {
            e.printStackTrace();
            passed = true;
        }
        assertTrue(
                passed,"Should not have been able to set a profile name with a preset name");

        // Test setting custom config with another custom config
        SyncProfileConfiguration tweedledumConfiguration = tweedledum
                .getConfiguration();
        passed = false;
        try {
            tweedledee.setConfiguration(tweedledumConfiguration);
        } catch (RuntimeException e) {
            e.printStackTrace();
            passed = true;
        }
        assertTrue(
                passed,"Should not have been able to set two profiles with the same config");

        // Test setting custom config with a preset config
        SyncProfileConfiguration presetConfiguration = SyncProfile
                .getSyncProfilesCopy().get(0).getConfiguration();
        passed = false;
        try {
            tweedledee.setConfiguration(presetConfiguration);
        } catch (RuntimeException e) {
            e.printStackTrace();
            passed = true;
        }
        assertTrue(
                passed,"Should not have been able to set a profile configuration with a preset configuration");
    }

    /**
     * Checks that preset profiles cannot have name or config set.
     */
    @Test
    public void testPresets() {

        SyncProfile presetProfile = SyncProfile.getSyncProfilesCopy().get(0);

        // Check cannot set name.
        boolean passed = false;
        try {
            presetProfile.setName("Snowball");
        } catch (RuntimeException e) {
            e.printStackTrace();
            passed = true;
        }
        assertTrue(
                passed,"Should not have been able to set a preset profile name");

        // Check cannot set config.
        passed = false;
        try {
            presetProfile.setConfiguration(new SyncProfileConfiguration(
                    false, false, true, false, 1024));
        } catch (RuntimeException e) {
            e.printStackTrace();
            passed = true;
        }
        assertTrue(
                passed,"Should not have been able to set a preset profile configuration");
    }

    /**
     * Checks that two profiles with the same config but different name,
     * load as one profile.
     */
    @Test
    public void testSameConfig() {

        int initialSize = SyncProfile.getSyncProfilesCopy().size();
        SyncProfile patty = SyncProfile
                .getSyncProfileByFieldList("true,true,true,true,555,false,12,0,m,patty");
        SyncProfile marge = SyncProfile
                .getSyncProfileByFieldList("true,true,true,true,444,false,12,0,m,marge");
        SyncProfile selma = SyncProfile
                .getSyncProfileByFieldList("true,true,true,true,555,false,12,0,m,selma");
        int finalSize = SyncProfile.getSyncProfilesCopy().size();

        assertSame(
                finalSize - initialSize, 2,"Loading identical config profiles should only create one new profile (1)");
        assertEquals(
                patty.getName(), selma.getName(),"Loading identical config profiles should only create one new profile (2)");
        assertFalse(
                patty.getName().equals(marge.getName()),"Loading different config profiles should create two new profile");
    }

    /**
     * Thorough check of the SyncProfileConfig equals method
     */
    @Test
    public void testConfigEquals() {

        SyncProfileConfiguration base = new SyncProfileConfiguration(
                false, false, false, false, 0, false, 0, 0, "m", false);
        SyncProfileConfiguration delta1 = new SyncProfileConfiguration(
                true, false, false, false, 0, false, 0, 0, "m", false);
        SyncProfileConfiguration delta2 = new SyncProfileConfiguration(
                false, true, false, false, 0, false, 0, 0, "m", false);
        SyncProfileConfiguration delta3 = new SyncProfileConfiguration(
                false, false, true, false, 0, false, 0, 0, "m", false);
        SyncProfileConfiguration delta4 = new SyncProfileConfiguration(
                false, false, false, true, 0, false, 0, 0, "m", false);
        SyncProfileConfiguration delta5 = new SyncProfileConfiguration(
                false, false, false, false, 9, false, 0, 0, "m", false);
        SyncProfileConfiguration delta6 = new SyncProfileConfiguration(
                false, false, false, false, 0, true, 0, 0, "m", false);
        SyncProfileConfiguration delta7 = new SyncProfileConfiguration(
                false, false, false, false, 0, false, 9, 0, "m", false);
        SyncProfileConfiguration delta8 = new SyncProfileConfiguration(
                false, false, false, false, 0, false, 0, 9, "m", false);
        SyncProfileConfiguration delta9 = new SyncProfileConfiguration(
                false, false, false, false, 0, false, 0, 0, "h", false);
        SyncProfileConfiguration delta10 = new SyncProfileConfiguration(
                false, false, false, false, 0, false, 0, 0, "h", true);

        assertFalse( base.equals(delta1),"delta1");
        assertFalse( base.equals(delta2),"delta2");
        assertFalse( base.equals(delta3),"delta3");
        assertFalse( base.equals(delta4),"delta4");
        assertFalse( base.equals(delta5),"delta5");
        assertFalse( base.equals(delta6),"delta6");
        assertFalse( base.equals(delta7),"delta7");
        assertFalse( base.equals(delta8),"delta8");
        assertFalse( base.equals(delta9),"delta9");
        assertFalse( base.equals(delta10),"delta10");
    }

}
