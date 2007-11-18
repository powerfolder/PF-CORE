package de.dal33t.powerfolder.test;

import de.dal33t.powerfolder.Feature;
import junit.framework.TestCase;

public class FeatureTest extends TestCase {

    public void testFeatureDefaults() {
        assertTrue(Feature.OS_CLIENT.isEnabled());
        assertTrue(Feature.EXIT_ON_SHUTDOWN.isEnabled());
    }

    public void testFeatureChange() {
        Feature.OS_CLIENT.disable();
        assertFalse(Feature.OS_CLIENT.isEnabled());
        assertTrue(Feature.EXIT_ON_SHUTDOWN.isEnabled());

        Feature.OS_CLIENT.enable();
        assertTrue(Feature.OS_CLIENT.isEnabled());
    }
}
