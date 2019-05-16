package de.dal33t.powerfolder.util;

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
