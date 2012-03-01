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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;

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
    private File outputFile;
    private File testScript;

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
        testScript.delete();
        outputFile.delete();
    }

    public void testExecuteAfterDownloadMutli() throws Exception {
        for (int i = 0; i < 15; i++) {
            testExecuteAfterDownload();
            tearDown();
            setUp();
        }
    }

    public void testExecuteAfterDownload() throws IOException {
        assertEquals(0, outputFile.length());
        File f = TestHelper.createRandomFile(new TFile(getFolderAtBart()
            .getLocalBase(), "subdir1"));
        scanFolder(getFolderAtBart());
        FileInfo fInfo = getFolderAtBart().getKnownFiles().iterator().next();
        assertFileMatch(f, fInfo, getContollerBart());

        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public boolean reached() {
                return outputFile.length() > 0;
            }

            public String message() {
                try {
                    InputStream in = new TFileInputStream(outputFile);
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
        assertTrue(outputFile.length() > 0);
        InputStream in = new TFileInputStream(outputFile);
        String content = new String(StreamUtils.readIntoByteArray(in));
        in.close();

        File fLisa = fInfo
            .getDiskFile(getContollerLisa().getFolderRepository());
        assertNotNull(fInfo);
        assertTrue(fLisa.exists());

        // Content of output file should contain name with full path info of
        // downloaded file
        String expected = fLisa.getAbsolutePath();
        expected += " ";
        expected += fLisa.getParent();
        expected += " ";
        expected += getFolderAtLisa().getLocalBase().getAbsolutePath();
        expected += " Bart";

        assertEquals(expected, content.trim());
    }

    public void testMultiDownloadExecute() throws IOException {
        assertEquals(0, outputFile.length());
        int nFiles = 20;
        List<File> testFiles = new ArrayList<File>();
        for (int i = 0; i < nFiles; i++) {
            File f = TestHelper.createRandomFile(new TFile(getFolderAtBart()
                .getLocalBase(), "subdir1"));
            testFiles.add(f);
        }

        scanFolder(getFolderAtBart());
        assertEquals(nFiles, getFolderAtBart().getKnownFiles().size());

        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public boolean reached() {
                return outputFile.length() > 0;
            }

            public String message() {
                try {
                    InputStream in = new TFileInputStream(outputFile);
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

        assertTrue(outputFile.length() > 0);
        InputStream in = new TFileInputStream(outputFile);
        String content = new String(StreamUtils.readIntoByteArray(in));
        in.close();

        for (File file : testFiles) {
            assertTrue(
                "Content file did not contain filename " + file.getName()
                    + ":\n" + content, content.contains(file.getName()));
        }
        System.out.println(content);
    }

    public void testExecuteBrokenScript() throws IOException {
        getFolderAtLisa().setDownloadScript(createBrokenScript());
        assertEquals(0, outputFile.length());
        File f = TestHelper.createRandomFile(new TFile(getFolderAtBart()
            .getLocalBase(), "subdir1"));
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
        testScript = File.createTempFile("script", ".bat");
        outputFile = File.createTempFile("output", ".txt");
        byte[] content;
        String cmdLine;
        String params = "$file $path $folderpath $sources";

        if (OSUtil.isWindowsSystem()) {
            content = ("echo %1 %2 %3 %4 %5 >>" + outputFile.getAbsolutePath() + "\nexit")
                .getBytes();
            cmdLine = "cmd /C start " + testScript.getAbsolutePath() + ' '
                + params;
        } else {
            content = ("echo $* >>\"" + outputFile.getAbsolutePath() + '"')
                .getBytes();
            cmdLine = "sh " + testScript.getAbsolutePath() + ' ' + params;
        }
        FileUtils.copyFromStreamToFile(new ByteArrayInputStream(content),
            testScript);

        return cmdLine;

    }

    private String createBrokenScript() throws IOException {
        testScript = File.createTempFile("script", ".bat");
        outputFile = File.createTempFile("output", ".txt");
        byte[] content;
        String cmdLine;
        String params = "$file $path $folderpath $sources";

        if (OSUtil.isWindowsSystem()) {
            content = ("echo %1 %2 %3 %4 %5 >>" + outputFile.getAbsolutePath() + "\npause")
                .getBytes();
            cmdLine = "cmd /C start " + testScript.getAbsolutePath() + ' '
                + params;
        } else {
            content = ("echo $* >>\"" + outputFile.getAbsolutePath() + "\"\nread -p \"Press any key ...\"")
                .getBytes();
            cmdLine = "sh " + testScript.getAbsolutePath() + ' ' + params;
        }
        FileUtils.copyFromStreamToFile(new ByteArrayInputStream(content),
            testScript);

        return cmdLine;

    }
}
