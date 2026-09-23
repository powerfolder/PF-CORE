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
 *
 */
package de.dal33t.powerfolder.util;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.PropertiesUtil;
import de.dal33t.powerfolder.util.StreamUtils;

/**
 * @author sprajc
 */
public class PropertiesUtilTest {

    @Test
    public void testWriteSpecialChars() throws IOException {
        Properties p = new Properties();
        p.put("wrapper.java.additional.2", "-XX:MaxPermSize=256m");
        Path testFile = Files.createTempFile("test", ".properties");
        PropertiesUtil.saveConfig(testFile, p, "Test header");

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        StreamUtils.copyToStream(testFile, bOut);
        String s = new String(bOut.toByteArray(), Convert.UTF8);
        // Should not contain escaped chars
        assertFalse( s.contains("\\"),s);

        Properties loaded = new Properties();
        InputStream in = Files.newInputStream(testFile);
        loaded.load(in);
        in.close();
        assertEquals(p, loaded);
    }
}
