/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test.util;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.test.TestHelper;

public class FileUtilsTest extends TestCase {

    public void testZipFile() throws IOException {
        byte[] b = new byte[1024 * 1024 * 3];
        for (int i = 0; i < 1024 * 100; i++) {
            b[(int) (Math.random() * b.length)] = (byte) (Math.random() * 256);
        }
        File t = TestHelper.createTestFile(TestHelper.getTestDir(),
            "file.plain", b);
        File zip = new File(TestHelper.getTestDir(), "file.zip");
        FileUtils.zipFile(t, zip);
        assertTrue(zip.exists());
        assertTrue(zip.length() < t.length());

        t = TestHelper.createTestFile(TestHelper.getTestDir(), "file2.txt",
            "Test contents in here, nothing much about it!".getBytes("UTF-8"));
        zip = new File(TestHelper.getTestDir(), "file2.zip");
        FileUtils.zipFile(t, zip);
        assertTrue(zip.exists());
    }
}
