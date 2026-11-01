/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 */
package de.dal33t.powerfolder.util;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class SerializationTest {
    @Test
    public void testFinalField() throws SecurityException,
        NoSuchFieldException, IllegalArgumentException, IllegalAccessException,
        IOException, ClassNotFoundException
    {
        Sample t = new Sample();

        assertNotNull(t.test);
        Field field = Sample.class.getDeclaredField("test");
        field.setAccessible(true);
        field.set(t, null);
        assertNull(t.test);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bout);
        oo.writeObject(t);
        oo.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream oin = new ObjectInputStream(bin);
        Sample t2 = (Sample) oin.readObject();

        oin.close();

        assertEquals(t.test, t2.test);
    }

    public void xtestPrintSerTest() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(out);

        oout.writeObject(new Sample());

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

    private static class Sample implements Serializable {
        private static final long serialVersionUID = 1L;
        final Object test = 1;
        final Object newfield = 2;
        final boolean supportsNewStuff;
        final boolean supportsEvenMore = true;
        final Boolean supportsMucho = Boolean.TRUE;

        private Sample() {
            supportsNewStuff = true;
        }
    }
}
