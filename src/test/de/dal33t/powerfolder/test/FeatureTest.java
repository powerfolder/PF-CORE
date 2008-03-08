package de.dal33t.powerfolder.test;

import de.dal33t.powerfolder.Feature;
import junit.framework.TestCase;

public class FeatureTest extends TestCase {

    public void testFeatureDefaults() {
        assertTrue(Feature.OS_CLIENT.isEnabled());
        assertTrue(Feature.EXIT_ON_SHUTDOWN.isEnabled());
        assertTrue(Feature.HIGH_FREQUENT_FOLDER_DB_MAINTENANCE.isDisabled());
    }

    public void testFeatureChange() {
        Feature.OS_CLIENT.disable();
        assertFalse(Feature.OS_CLIENT.isEnabled());
        assertTrue(Feature.EXIT_ON_SHUTDOWN.isEnabled());

        Feature.OS_CLIENT.enable();
        assertTrue(Feature.OS_CLIENT.isEnabled());

        assertFalse(Feature.REMIND_COMPLETED_DOWNLOADS.isDisabled());
    }
}
