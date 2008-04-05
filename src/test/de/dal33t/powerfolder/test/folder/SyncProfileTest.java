package de.dal33t.powerfolder.test.folder;

import junit.framework.TestCase;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.SyncProfileConfiguration;

/**
 * Class to run some basic tests on sync profiles.
 */
public class SyncProfileTest extends TestCase {

    /**
     * Checks that profiles cannot have the same name or config as each other.
     */
    public void testDuplicates() {

        SyncProfile tweedledee = SyncProfile
                .getSyncProfileByFieldList("true,false,true,false,333");
        SyncProfile tweedledum = SyncProfile
                .getSyncProfileByFieldList("false,true,false,true,334");

        // Test setting custom name with another custom name
        String tweedledumName = tweedledum.getProfileName();
        boolean passed = false;
        try {
            tweedledee.setProfileName(tweedledumName);
        } catch (RuntimeException e) {
            e.printStackTrace();
            passed = true;
        }
        assertTrue("Should not have been able to set two profiles with the same name",
                passed);

        // Test setting custom name with preset name
        String presetProfileName = SyncProfile.getSyncProfilesCopy().get(0)
                .getProfileName();
        passed = false;
        try {
            tweedledee.setProfileName(presetProfileName);
        } catch (RuntimeException e) {
            e.printStackTrace();
            passed = true;
        }
        assertTrue("Should not have been able to set a profile name with a preset name",
                passed);

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
        assertTrue("Should not have been able to set two profiles with the same config",
                passed);

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
        assertTrue("Should not have been able to set a profile configuration with a preset configuration",
                passed);
    }

    /**
     * Checks that preset profiles cannot have name or config set.
     */
    public void testPresets() {

        SyncProfile presetProfile = SyncProfile.getSyncProfilesCopy().get(0);

        // Check cannot set name.
        boolean passed = false;
        try {
            presetProfile.setProfileName("Snowball");
        } catch (RuntimeException e) {
            e.printStackTrace();
            passed = true;
        }
        assertTrue("Should not have been able to set a preset profile name",
                passed);

        // Check cannot set config.
        passed = false;
        try {
            presetProfile.setConfiguration(new SyncProfileConfiguration(
                    false, false, true, false, 1024));
        } catch (RuntimeException e) {
            e.printStackTrace();
            passed = true;
        }
        assertTrue("Should not have been able to set a preset profile configuration",
                passed);
    }

    /**
     * Checks that two profiles with the same config but different name,
     * load as one profile.
     */
    public void testSameConfig() {

        int initialSize = SyncProfile.getSyncProfilesCopy().size();
        SyncProfile patty = SyncProfile
                .getSyncProfileByFieldList("true,true,true,true,555,false,12,0,m,patty");
        SyncProfile marge = SyncProfile
                .getSyncProfileByFieldList("true,true,true,true,444,false,12,0,m,marge");
        SyncProfile selma = SyncProfile
                .getSyncProfileByFieldList("true,true,true,true,555,false,12,0,m,selma");
        int finalSize = SyncProfile.getSyncProfilesCopy().size();

        assertSame("Loading identical config profiles should only create one new profile (1)",
                finalSize - initialSize, 2);
        assertEquals("Loading identical config profiles should only create one new profile (2)",
                patty.getProfileName(), selma.getProfileName());
        assertFalse("Loading different config profiles should create two new profile",
                patty.getProfileName().equals(marge.getProfileName()));
    }

    /**
     * Thorough check of the SyncProfileConfig equals method
     */
    public void testConfigEquals() {
        
        SyncProfileConfiguration base = new SyncProfileConfiguration(
                false, false, false, false, 0, false, 0, 0, "m");
        SyncProfileConfiguration delta1 = new SyncProfileConfiguration(
                true, false, false, false, 0, false, 0, 0, "m");
        SyncProfileConfiguration delta2 = new SyncProfileConfiguration(
                false, true, false, false, 0, false, 0, 0, "m");
        SyncProfileConfiguration delta3 = new SyncProfileConfiguration(
                false, false, true, false, 0, false, 0, 0, "m");
        SyncProfileConfiguration delta4 = new SyncProfileConfiguration(
                false, false, false, true, 0, false, 0, 0, "m");
        SyncProfileConfiguration delta5 = new SyncProfileConfiguration(
                false, false, false, false, 9, false, 0, 0, "m");
        SyncProfileConfiguration delta6 = new SyncProfileConfiguration(
                false, false, false, false, 0, true, 0, 0, "m");
        SyncProfileConfiguration delta7 = new SyncProfileConfiguration(
                false, false, false, false, 0, false, 9, 0, "m");
        SyncProfileConfiguration delta8 = new SyncProfileConfiguration(
                false, false, false, false, 0, false, 0, 9, "m");
        SyncProfileConfiguration delta9 = new SyncProfileConfiguration(
                false, false, false, false, 0, false, 0, 0, "h");

        assertFalse("delta1", base.equals(delta1));
        assertFalse("delta2", base.equals(delta2));
        assertFalse("delta3", base.equals(delta3));
        assertFalse("delta4", base.equals(delta4));
        assertFalse("delta5", base.equals(delta5));
        assertFalse("delta6", base.equals(delta6));
        assertFalse("delta7", base.equals(delta7));
        assertFalse("delta8", base.equals(delta8));
        assertFalse("delta9", base.equals(delta9));
    }

}
