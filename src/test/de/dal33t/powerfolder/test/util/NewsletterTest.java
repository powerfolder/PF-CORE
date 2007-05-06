
package de.dal33t.powerfolder.test.util;

import de.dal33t.powerfolder.util.Newsletter;
import junit.framework.TestCase;

public class NewsletterTest extends TestCase {
    public void testSubscribe() {
        assertTrue(Newsletter.subscribe("totmacher@powerfolder.com"));
    }
}
