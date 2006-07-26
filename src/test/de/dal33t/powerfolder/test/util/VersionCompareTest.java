/* $Id: VersionCompareTest.java,v 1.1.2.1 2006/04/29 00:27:15 totmacherr Exp $
 */
package de.dal33t.powerfolder.test.util;

import java.lang.reflect.Method;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.UpdateChecker;

/**
 * Test the version string compare of Util
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.1.2.1 $
 */
public class VersionCompareTest extends TestCase {
    public void testCompare() {
        try {
            boolean foundMethod = false;
            final Method[] methods = UpdateChecker.class.getDeclaredMethods();
            for (int i = 0; i < methods.length; ++i) {
                Method method = methods[i];
                if (method.getName().equals("compareVersions")) {
                    foundMethod = true;                    
                    method.setAccessible(true);   
                    //null because it's a static method
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"1", "0.9.3"}));
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"1 devel", "0.9.3"}));
                    assertFalse((Boolean) method.invoke(null, new Object[]{"0.3 devel", "0.9.3"}));
                    assertFalse((Boolean) method.invoke(null, new Object[]{"0.3.0", "0.9.3"}));
                    assertFalse((Boolean) method.invoke(null, new Object[]{"0.3", "0.9.3"}));
                    assertFalse((Boolean) method.invoke(null, new Object[]{"0", "0.9.3"}));
                    assertFalse((Boolean) method.invoke(null, new Object[]{"0.9.3", "0.9.3"}));
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"1.0.0", "0.9.3"}));
                    assertFalse((Boolean) method.invoke(null, new Object[]{"0.9.3", "1.0.0"}));
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"0.9.3", "0.9.3 devel"}));
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"0.9.4", "0.9.3 devel"}));
                    assertFalse((Boolean) method.invoke(null, new Object[]{"1.0.1 devel", "1.0.1"}));
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"1.0.1", "1.0.0"}));                    
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"1.0.2", "1.0.1"}));
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"1.0.2", "1.0.2 devel"}));
                    assertFalse((Boolean) method.invoke(null,  new Object[]{"1.0.2 devel", "1.1"}));
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"1.1", "1.0.2 devel"}));
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"1.1.0", "1.1.0 devel"}));
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"1.1.1", "1.1 devel"}));
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"1.1.1", "1.1.1 devel"}));
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"1.1.2", "1.1.1"}));
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"2", "1.1.1"}));
                    assertTrue((Boolean) method.invoke(null,  new Object[]{"2.0.1", "2.0.0"}));
                    assertFalse((Boolean) method.invoke(null,  new Object[]{"2.0.0", "2.0.1"}));
                    break;
                }
            }
            assertTrue(foundMethod);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }       
    }
}
