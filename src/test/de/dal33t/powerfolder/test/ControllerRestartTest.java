
package de.dal33t.powerfolder.test;

import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

public class ControllerRestartTest extends ControllerTestCase {
    public void testRestart() {
        getController().shutdown();
        Debug.dumpThreadStacks();
    }
}
