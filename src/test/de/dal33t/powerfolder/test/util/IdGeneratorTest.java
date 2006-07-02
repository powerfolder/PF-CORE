/* $Id$
 */
package de.dal33t.powerfolder.test.util;

import de.dal33t.powerfolder.util.IdGenerator;
import junit.framework.TestCase;

/**
 * Tests the id generator.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class IdGeneratorTest extends TestCase {
    /**
     * Tests the uniqueness of the idgenerator.
     */
    public void testIdGeneration() {
        for (int i = 0; i < 500000; i++) {
            assertFalse(IdGenerator.makeId().equals(IdGenerator.makeId()));
        }
    }
}
