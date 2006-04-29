package de.dal33t.powerfolder.junit;

import junit.framework.Test;
import junit.framework.TestSuite;
import de.dal33t.powerfolder.junit.BandwidthLimitTest;
import de.dal33t.powerfolder.junit.RecycleTest;
import de.dal33t.powerfolder.junit.TransferCounterTest;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test powerfolder");
        //$JUnit-BEGIN$
        suite.addTestSuite(BandwidthLimitTest.class);
        suite.addTestSuite(TransferCounterTest.class);
        suite.addTestSuite(RecycleTest.class);
        //$JUnit-END$
        return suite;
    }

}
