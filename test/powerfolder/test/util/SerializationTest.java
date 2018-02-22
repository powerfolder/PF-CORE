package de.dal33t.powerfolder.test.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;

import junit.framework.TestCase;

public class SerializationTest extends TestCase {
    public void testFinalField() throws SecurityException,
        NoSuchFieldException, IllegalArgumentException, IllegalAccessException,
        IOException, ClassNotFoundException
    {
        Test t = new Test();

        assertNotNull(t.test);
        Field field = Test.class.getDeclaredField("test");
        field.setAccessible(true);
        field.set(t, null);
        assertNull(t.test);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bout);
        oo.writeObject(t);
        oo.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream oin = new ObjectInputStream(bin);
        Test t2 = (Test) oin.readObject();

        oin.close();

        assertEquals(t.test, t2.test);
    }

    public void xtestPrintSerTest() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(out);

        oout.writeObject(new Test());

        oout.close();

        String tmp = new String(out.toByteArray(), "ISO-8859-1");
        tmp = URLEncoder.encode(tmp, "ISO-8859-1");
        for (int i = 0; i < tmp.length(); i += 40) {
            if (i > 0) {
                System.out.print("+ ");
            }
            System.out.println("\""
                + tmp.substring(i, Math.min(tmp.length(), i + 40)) + "\"");
        }
    }

    public void testMissingFinalFieldDeserialization() throws IOException,
        ClassNotFoundException
    {
        String noField = "%AC%ED%00%05sr%006de.dal33t.powerfolder."
            + "test.util.SerializationTest%24Test%00%00"
            + "%00%00%00%00%00%01%02%00%01L%00%04testt%"
            + "00%12Ljava%2Flang%2FObject%3Bxpsr%00%11j"
            + "ava.lang.Integer%12%E2%A0%A4%F7%81%878%0"
            + "2%00%01I%00%05valuexr%00%10java.lang.Num"
            + "ber%86%AC%95%1D%0B%94%E0%8B%02%00%00xp%0" + "0%00%00%01";

        ByteArrayInputStream bin = new ByteArrayInputStream(URLDecoder.decode(
            noField, "ISO-8859-1").getBytes("ISO-8859-1"));
        ObjectInputStream oin = new ObjectInputStream(bin);
        Test t = (Test) oin.readObject();
        oin.close();

        // If this is null it means newfield WAS initialized to null by
        // deserialization
        assertNull(t.newfield);
        // If this is true it means supportsNewStuff was initialized by true,
        // although it SHOULD have been false
        assertFalse(t.supportsNewStuff);
        // If this is true it means supportsNewStuff was initialized by true,
        // although it SHOULD have been false. (yes this is a "bug")
        assertTrue(t.supportsEvenMore);
        // Check if it works for Boolean at least.
        assertNull(t.supportsMucho);
        assertNotSame(new Test().newfield, t.newfield);
    }

    private static class Test implements Serializable {
        private static final long serialVersionUID = 1L;
        final Object test = 1;
        final Object newfield = 2;
        final boolean supportsNewStuff;
        final boolean supportsEvenMore = true;
        final Boolean supportsMucho = Boolean.TRUE;

        private Test() {
            supportsNewStuff = true;
        }
    }
}
