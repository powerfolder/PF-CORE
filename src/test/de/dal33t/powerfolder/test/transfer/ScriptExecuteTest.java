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
package de.dal33t.powerfolder.test.transfer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Tests the script execution ability of PowerFolder.
 * <P>
 * #1538
 * <P>
 * http://www.powerfolder.com/wiki/Script_execution
 *
 * @author sprajc
 */
public class ScriptExecuteTest extends TwoControllerTestCase {
    private Path outputFile;
    private Path testScript;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
        getFolderAtLisa().setDownloadScript(createTestScript());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Files.delete(testScript);
        Files.delete(outputFile);
    }

    public void testExecuteAfterDownloadMutli() throws Exception {
        for (int i = 0; i < 15; i++) {
            testExecuteAfterDownload();
            tearDown();
            setUp();
        }
    }

    public void testExecuteAfterDownload() throws IOException {
        assertEquals(0, Files.size(outputFile));
        Path f = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase().resolve("subdir1"));
        scanFolder(getFolderAtBart());
        FileInfo fInfo = getFolderAtBart().getKnownFiles().iterator().next();
        assertFileMatch(f, fInfo, getContollerBart());

        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public boolean reached() {
                try {
                    return Files.size(outputFile) > 0;
                } catch (IOException ioe) {
                    return false;
                }
            }

            public String message() {
                try {
                    InputStream in = Files.newInputStream(outputFile);
                    String content = new String(StreamUtils
                        .readIntoByteArray(in));
                    in.close();
                    return "Output file does contain: '" + content + "'";
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Error reading output file: " + e;
                }
            }
        });
        assertTrue(Files.size(outputFile) > 0);
        InputStream in = Files.newInputStream(outputFile);
        String content = new String(StreamUtils.readIntoByteArray(in));
        in.close();

        Path fLisa = fInfo
            .getDiskFile(getContollerLisa().getFolderRepository());
        assertNotNull(fInfo);
        assertTrue(Files.exists(fLisa));

        // Content of output file should contain name with full path info of
        // downloaded file
        String expected = fLisa.toAbsolutePath().toString();
        expected += " ";
        expected += fLisa.getParent();
        expected += " ";
        expected += getFolderAtLisa().getLocalBase().toAbsolutePath().toString();
        expected += " Bart";

        assertEquals(expected, content.trim());
    }

    public void testMultiDownloadExecute() throws IOException {
        assertEquals(0, Files.size(outputFile));
        int nFiles = 20;
        List<Path> testFiles = new ArrayList<Path>();
        for (int i = 0; i < nFiles; i++) {
            Path f = TestHelper.createRandomFile(getFolderAtBart()
                .getLocalBase().resolve("subdir1"));
            testFiles.add(f);
        }

        scanFolder(getFolderAtBart());
        assertEquals(nFiles, getFolderAtBart().getKnownFiles().size());

        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public boolean reached() {
                try {
                    return Files.size(outputFile) > 0;
                } catch (IOException ioe) {
                    return false;
                }
            }

            public String message() {
                try {
                    InputStream in = Files.newInputStream(outputFile);
                    String content = new String(StreamUtils
                        .readIntoByteArray(in));
                    in.close();
                    return "Output file does contain: '" + content + "'";
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Error reading output file: " + e;
                }
            }
        });
        TestHelper.waitMilliSeconds(2500);

        assertTrue(Files.size(outputFile) > 0);
        InputStream in = Files.newInputStream(outputFile);
        String content = new String(StreamUtils.readIntoByteArray(in));
        in.close();

        for (Path file : testFiles) {
            assertTrue(
                "Content file did not contain filename " + file.getFileName().toString()
                    + ":\n" + content, content.contains(file.getFileName().toString()));
        }
        System.out.println(content);
    }

    public void testExecuteBrokenScript() throws IOException {
        getFolderAtLisa().setDownloadScript(createBrokenScript());
        assertEquals(0, Files.size(outputFile));
        Path f = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase().resolve("subdir1"));
        scanFolder(getFolderAtBart());
        FileInfo fInfo = getFolderAtBart().getKnownFiles().iterator().next();
        assertFileMatch(f, fInfo, getContollerBart());

        TestHelper.waitForCondition(3, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countCompletedDownloads() == 1;
            }

            public String message() {
                return "Lisa did not complete the download in time: "
                    + getContollerLisa().getTransferManager()
                        .countCompletedDownloads();
            }
        });
    }

    private String createTestScript() throws IOException {
        testScript = Files.createTempFile("script", ".bat");
        outputFile = Files.createTempFile("output", ".txt");
        byte[] content;
        String cmdLine;
        String params = "$file $path $folderpath $sources";

        if (OSUtil.isWindowsSystem()) {
            content = ("echo %1 %2 %3 %4 %5 >>" + outputFile.toAbsolutePath().toString() + "\nexit")
                .getBytes();
            cmdLine = "cmd /C start " + testScript.toAbsolutePath().toString() + ' '
                + params;
        } else {
            content = ("echo $* >>\"" + outputFile.toAbsolutePath().toString() + '"')
                .getBytes();
            cmdLine = "sh " + testScript.toAbsolutePath().toString() + ' ' + params;
        }
        PathUtils.copyFromStreamToFile(new ByteArrayInputStream(content),
            testScript);

        return cmdLine;

    }

    private String createBrokenScript() throws IOException {
        testScript = Files.createTempFile("script", ".bat");
        outputFile = Files.createTempFile("output", ".txt");
        byte[] content;
        String cmdLine;
        String params = "$file $path $folderpath $sources";

        if (OSUtil.isWindowsSystem()) {
            content = ("echo %1 %2 %3 %4 %5 >>" + outputFile.toAbsolutePath().toString() + "\npause")
                .getBytes();
            cmdLine = "cmd /C start " + testScript.toAbsolutePath().toString() + ' '
                + params;
        } else {
            content = ("echo $* >>\"" + outputFile.toAbsolutePath().toString() + "\"\nread -p \"Press any key ...\"")
                .getBytes();
            cmdLine = "sh " + testScript.toAbsolutePath().toString() + ' ' + params;
        }
        PathUtils.copyFromStreamToFile(new ByteArrayInputStream(content),
            testScript);

        return cmdLine;

    }
}
