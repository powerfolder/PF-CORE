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
package de.dal33t.powerfolder.test.folder;

import java.lang.reflect.Method;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.problem.FilenameProblemHelper;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.PreferencesEntry;

public class FileNameProblemTest extends ControllerTestCase {

    public void testForWindows() {
        assertFalse(FilenameProblemHelper
            .containsIllegalWindowsChars("a valid filename.txt"));
        // /\?*<":>+[]

        assertTrue(FilenameProblemHelper.containsIllegalWindowsChars("fhf/fjf"));
        assertTrue(FilenameProblemHelper.containsIllegalWindowsChars("hhhh\\"));
        assertTrue(FilenameProblemHelper.containsIllegalWindowsChars("?hhh"));
        assertTrue(FilenameProblemHelper.containsIllegalWindowsChars("ddfgd*"));
        assertTrue(FilenameProblemHelper.containsIllegalWindowsChars("<hhf"));
        assertTrue(FilenameProblemHelper.containsIllegalWindowsChars("\"gfgfg"));
        assertTrue(FilenameProblemHelper.containsIllegalWindowsChars(":sds"));
        assertTrue(FilenameProblemHelper.containsIllegalWindowsChars("gfgf>"));
        assertTrue(FilenameProblemHelper.containsIllegalWindowsChars("ssdffd<"));
        assertFalse(FilenameProblemHelper
            .containsIllegalWindowsChars("日本語でのテスト"));

        // controll chars
        for (int i = 0; i <= 31; i++) {
            assertFalse(FilenameProblemHelper
                .containsIllegalWindowsChars((char) i + "123"));
        }
        // >=32 is no controll char
        assertFalse(FilenameProblemHelper.containsIllegalWindowsChars((char) 32
            + "123"));

        // reserved windows words like AUX (extentions behind it are also not
        // allowed)
        assertTrue(FilenameProblemHelper.isReservedWindowsFilename("AUX"));
        assertTrue(FilenameProblemHelper.isReservedWindowsFilename("AUX.txt"));
        assertTrue(FilenameProblemHelper.isReservedWindowsFilename("LPT1"));
        assertFalse(FilenameProblemHelper.isReservedWindowsFilename("xLPT1"));
        assertFalse(FilenameProblemHelper.isReservedWindowsFilename("xAUX.txt"));

    }

    public void testFilenameProblems() {
        PreferencesEntry.FILE_NAME_CHECK.setValue(getController(), true);
        FolderInfo folderInfo = new FolderInfo("testFolder", "ID");
        assertFalse(FilenameProblemHelper
            .hasProblems("a valid filename.whatever"));
        // cannot end with . and space ( ) on windows
        assertEquals(1, FilenameProblemHelper.getProblems(getController(),
            FileInfoFactory.lookupInstance(folderInfo, "dddd.")).size());
        assertEquals(1, FilenameProblemHelper.getProblems(getController(),
            FileInfoFactory.lookupInstance(folderInfo, "dddd ")).size());

        // Windows/Unix/Mac
        // problems with slashes are not detectable becuase we assume they are
        // folder seperators
        // assertEquals(3,
        // FilenameProblem.getProblems(FileInfoFactory.lookupInstance(folderInfo,
        // "ddd/d")).size());
        // windows/Mac
        assertEquals(2, FilenameProblemHelper.getProblems(getController(),
            FileInfoFactory.lookupInstance(folderInfo, "ddd:d")).size());
        // windows
        assertEquals(1, FilenameProblemHelper.getProblems(getController(),
            FileInfoFactory.lookupInstance(folderInfo, "AUX")).size());
        assertEquals(1, FilenameProblemHelper.getProblems(getController(),
            FileInfoFactory.lookupInstance(folderInfo, "aux")).size());
        assertEquals(1, FilenameProblemHelper.getProblems(getController(),
            FileInfoFactory.lookupInstance(folderInfo, "aux.txt")).size());
        // 255 chars
        assertFalse(FilenameProblemHelper
            .hasProblems("012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234"));
        // 256 chars
        assertEquals(
            1,
            FilenameProblemHelper
                .getProblems(
                    getController(),
                    FileInfoFactory
                        .lookupInstance(
                            folderInfo,
                            "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345"))
                .size());
    }

    public void testFilenameProblemsNoCheck() {
        PreferencesEntry.FILE_NAME_CHECK.setValue(getController(), false);
        FolderInfo folderInfo = new FolderInfo("testFolder", "ID");
        assertFalse(FilenameProblemHelper
            .hasProblems("a valid filename.whatever"));
        // cannot end with . and space ( ) on windows
        assertEquals(0, FilenameProblemHelper.getProblems(getController(),
            FileInfoFactory.lookupInstance(folderInfo, "dddd.")).size());
    }

    public void testStripExtension() {
        try {
            boolean foundMethod = false;
            Method[] methods = FilenameProblemHelper.class.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("stripExtension")) {
                    foundMethod = true;
                    method.setAccessible(true);
                    // null because it's a static method
                    assertEquals("test", (String) method.invoke(null,
                        "test.text"));
                    assertEquals("test", (String) method.invoke(null, "test"));
                    break;
                }
            }
            assertTrue(foundMethod);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test the getShorterFilename() method in FilenameProblemHelper
     */
    public void testShorterFileName() {
        setupTestFolder(SyncProfile.BACKUP_SOURCE);

        // Test that abcdef gets shortened to abc because of other files.
        TestHelper.createRandomFile(getFolder().getLocalBase(), "abcd");
        TestHelper.createRandomFile(getFolder().getLocalBase(), "abcde");
        TestHelper.createRandomFile(getFolder().getLocalBase(), "abcdef");
        String s = FilenameProblemHelper.getShorterFilename(getController(),
            FileInfoFactory.lookupInstance(getFolder().getInfo(), "abcdef"));
        assertEquals("Failed to shorten abcdef to abc", s, "abc");

        // Test that other does not get touched.
        s = FilenameProblemHelper.getShorterFilename(getController(),
            FileInfoFactory.lookupInstance(getFolder().getInfo(), "other"));
        assertEquals("Other affected", s, "other");

        // Test that R E A L L Y long names get shortened.
        s = FilenameProblemHelper
            .getShorterFilename(
                getController(),
                FileInfoFactory
                    .lookupInstance(
                        getFolder().getInfo(),
                        "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"));
        assertEquals(
            "Not shortened",
            s,
            "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstu");
    }

    /**
     * Test the makeUnique() method in FilenameProblemHelper
     */
    public void testUniqueFileName() {
        setupTestFolder(SyncProfile.BACKUP_SOURCE);

        // Test that abcd gets changed to abcd-2 because of other files.
        TestHelper.createRandomFile(getFolder().getLocalBase(), "abcd");
        TestHelper.createRandomFile(getFolder().getLocalBase(), "abcd-1");
        String s = FilenameProblemHelper.makeUnique(getController(),
            FileInfoFactory.lookupInstance(getFolder().getInfo(), "abcd"));
        assertEquals("Failed to make unique abcd to abcd-2", s, "abcd-2");

        // Test that abcd gets changed to abcd-2 because of other files.
        TestHelper.createRandomFile(getFolder().getLocalBase(), "subdir/abcd");
        TestHelper
            .createRandomFile(getFolder().getLocalBase(), "subdir/abcd-1");
        s = FilenameProblemHelper.makeUnique(getController(), FileInfoFactory
            .lookupInstance(getFolder().getInfo(), "abcd"));
        assertEquals("Failed to make unique abcd to abcd-2", s, "abcd-2");
    }

    /**
     * Test the makeUnique() method in FilenameProblemHelper with file extension
     */
    public void testUniqueFileNameExt() {
        setupTestFolder(SyncProfile.BACKUP_SOURCE);

        // Test that abcd gets changed to abcd-2 because of other files.
        TestHelper.createRandomFile(getFolder().getLocalBase(), "abcd.txt");
        TestHelper.createRandomFile(getFolder().getLocalBase(), "abcd-1.txt");
        String s = FilenameProblemHelper.makeUnique(getController(),
            FileInfoFactory.lookupInstance(getFolder().getInfo(), "abcd.txt"));
        assertEquals("Failed to make unique abcd.txt to abcd-2.txt", s,
            "abcd-2.txt");
    }
}
