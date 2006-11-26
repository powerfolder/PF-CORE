/* $Id$
 */
package de.dal33t.powerfolder.test.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.Util;

/**
 * Test for the Util class.
 * 
 * @see Util
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class UtilTest extends TestCase {

    public void testSplitArray() throws UnsupportedEncodingException {
        byte[] testArray = new byte[94];
        List<byte[]> output = Util.splitArray(testArray, 100);
        assertEquals(1, output.size());
        assertEquals(testArray.length, output.get(0).length);

        output = Util.splitArray(testArray, 10);
        assertEquals(10, output.size());
        assertEquals(4, output.get(9).length);

        testArray = "HALLO_DIESE_TESTX_MACHT_NIXEX".getBytes("UTF-8");
        output = Util.splitArray(testArray, 6);
        assertEquals(5, output.size());
        assertEquals("HALLO_", new String(output.get(0), "UTF-8"));
        assertEquals("DIESE_", new String(output.get(1), "UTF-8"));
        assertEquals("TESTX_", new String(output.get(2), "UTF-8"));
        assertEquals("MACHT_", new String(output.get(3), "UTF-8"));
        assertEquals("NIXEX", new String(output.get(4), "UTF-8"));
    }
    
    public void testMergeArray() throws UnsupportedEncodingException {
        List<byte[]> arrayList = new ArrayList<byte[]>();
        arrayList.add("TEST".getBytes("UTF-8"));
        arrayList.add("1".getBytes("UTF-8"));
        arrayList.add("|||HEYHO".getBytes("UTF-8"));
        arrayList.add("XXX".getBytes("UTF-8"));
        
        byte[] output = Util.mergeArrayList(arrayList);
        String outputStr = new String(output, "UTF-8");
        assertEquals("TEST1|||HEYHOXXX", outputStr);
    }
}
