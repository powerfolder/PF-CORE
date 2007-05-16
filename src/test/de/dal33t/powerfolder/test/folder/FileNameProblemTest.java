package de.dal33t.powerfolder.test.folder;

import java.lang.reflect.Method;

import junit.framework.TestCase;
import de.dal33t.powerfolder.disk.FilenameProblem;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;

public class FileNameProblemTest extends TestCase {

    public void testForWindows() {
        assertFalse(FilenameProblem
            .containsIllegalWindowsChars("a valid filename.txt"));
        // /\?*<":>+[]

        assertTrue(FilenameProblem.containsIllegalWindowsChars("fhf/fjf"));
        assertTrue(FilenameProblem.containsIllegalWindowsChars("hhhh\\"));
        assertTrue(FilenameProblem.containsIllegalWindowsChars("?hhh"));
        assertTrue(FilenameProblem.containsIllegalWindowsChars("ddfgd*"));
        assertTrue(FilenameProblem.containsIllegalWindowsChars("<hhf"));
        assertTrue(FilenameProblem.containsIllegalWindowsChars("\"gfgfg"));
        assertTrue(FilenameProblem.containsIllegalWindowsChars(":sds"));
        assertTrue(FilenameProblem.containsIllegalWindowsChars("gfgf>"));
        assertTrue(FilenameProblem.containsIllegalWindowsChars("ssdffd<"));
        assertFalse(FilenameProblem.containsIllegalWindowsChars("日本語でのテスト"));

        // controll chars
        for (int i = 0; i <= 31; i++) {
            assertFalse(FilenameProblem.containsIllegalWindowsChars(((char) i)
                + "123"));
        }
        // >=32 is no controll char
        assertFalse(FilenameProblem.containsIllegalWindowsChars(((char) 32)
            + "123"));

        // reserved windows words like AUX (extentions behind it are also not
        // allowed)
        assertTrue(FilenameProblem.isReservedWindowsFilename("AUX"));
        assertTrue(FilenameProblem.isReservedWindowsFilename("AUX.txt"));
        assertTrue(FilenameProblem.isReservedWindowsFilename("LPT1"));
        assertFalse(FilenameProblem.isReservedWindowsFilename("xLPT1"));
        assertFalse(FilenameProblem.isReservedWindowsFilename("xAUX.txt"));

    }

    public void testFilenameProblems() {
        FolderInfo folderInfo = new FolderInfo("testFolder", "ID", true, true);
        assertFalse(FilenameProblem.hasProblems("a valid filename.whatever"));
        // cannot end with . and space ( ) on windows
        assertEquals(1, FilenameProblem.getProblems(
            new FileInfo(folderInfo, "dddd.")).size());
        assertEquals(1, FilenameProblem.getProblems(
            new FileInfo(folderInfo, "dddd ")).size());

        // Windows/Unix/Mac
        // problems with slashes are not detectable becuase we assume they are
        // folder seperators
        // assertEquals(3, FilenameProblem.getProblems(new FileInfo(folderInfo,
        // "ddd/d")).size());
        // windows/Mac
        assertEquals(2, FilenameProblem.getProblems(
            new FileInfo(folderInfo, "ddd:d")).size());
        // windows
        assertEquals(1, FilenameProblem.getProblems(
            new FileInfo(folderInfo, "AUX")).size());
        assertEquals(1, FilenameProblem.getProblems(
            new FileInfo(folderInfo, "aux")).size());
        assertEquals(1, FilenameProblem.getProblems(
            new FileInfo(folderInfo, "aux.txt")).size());
        // 255 chars
        assertFalse(FilenameProblem
            .hasProblems("012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234"));
        // 256 chars
        assertEquals(
            1,
            FilenameProblem
                .getProblems(
                    new FileInfo(
                        folderInfo,
                        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345"))
                .size());
    }

    public void testStripExtension() {
        try {
            boolean foundMethod = false;
            final Method[] methods = FilenameProblem.class.getDeclaredMethods();
            for (int i = 0; i < methods.length; ++i) {
                Method method = methods[i];
                if (method.getName().equals("stripExtension")) {
                    foundMethod = true;
                    method.setAccessible(true);
                    // null because it's a static method
                    assertEquals("test", (String) method.invoke(null,
                        new Object[]{"test.text"}));
                    assertEquals("test", (String) method.invoke(null,
                        new Object[]{"test"}));
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
