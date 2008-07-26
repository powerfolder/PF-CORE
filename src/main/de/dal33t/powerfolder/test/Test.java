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
* $Id$
*/
package de.dal33t.powerfolder.test;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Loggable;

/**
 * A Testclass for testing bug conditions on serveral machines
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.7 $
 */
public class Test extends Loggable {

    public static void main(String[] args) throws IOException {
        Test test = new Test();
        Loggable.logInfoStatic(Test.class,
                "------------- Starting tests ----------------------------------");
        
        test.testWebStartMime();
        Loggable.logInfoStatic(Test.class,
                "---------------------------------------------------------------");

        test.testCorruptZipFile();
        Loggable.logInfoStatic(Test.class,
                "---------------------------------------------------------------");

        test.testInt2Bytes();
        Loggable.logInfoStatic(Test.class,
                "---------------------------------------------------------------");

        test.testFileChunkSize();
        Loggable.logInfoStatic(Test.class,
                "---------------------------------------------------------------");

        if (args != null && args.length > 0) {
            test.testSocketResolving(args[0]);
            Loggable.logInfoStatic(Test.class,
                    "---------------------------------------------------------------");
        }

        test.testTimeCalculations();
        Loggable.logInfoStatic(Test.class,
                "---------------------------------------------------------------");

        test.testFileWrite();
        Loggable.logInfoStatic(Test.class,
                "---------------------------------------------------------------");
    }

    private void testWebStartMime() {
        try {
            URL url = new URL(
                "http://webstart.powerfolder.com/release/PowerFolder.jnlp");
            logInfo("Testing mime type for webstart URL '" + url + "'");
            logInfo("Mime type is "
                + url.openConnection().getContentType());
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void testCorruptZipFile() {
        File file = new File("test.jar");
        logInfo(file + " is a valid zip ? " + FileUtils.isValidZipFile(file));
    }

    private void testInt2Bytes() {
        int i = 12345678;
        logInfo("Testing int 2 byte conversion with " + i);
        byte b1 = (byte) (i & 0xFF);
        byte b2 = (byte) (0xFF & (i >> 8));
        byte b3 = (byte) (0xFF & (i >> 16));
        byte b4 = (byte) (0xFF & (i >> 24));
        logInfo("Byte 1: " + b1);
        logInfo("Byte 2: " + b2);
        logInfo("Byte 3: " + b3);
        logInfo("Byte 4: " + b4);

        int w = 0;
        if (b4 < 0)
            w = b4 + 256;
        else
            w = b4;
        w <<= 8;
        if (b3 < 0)
            w += b3 + 256;
        else
            w += b3;
        w <<= 8;
        if (b2 < 0)
            w += b2 + 256;
        else
            w += b2;
        w <<= 8;
        if (b1 < 0)
            w += b1 + 256;
        else
            w += b1;

        w = Convert.convert2Int(new byte[]{b4, b3, b2, b1});

        logInfo("Converted back to: " + w);
    }

    private void testFileChunkSize() throws IOException {
        FileChunk chunk = new FileChunk();
        chunk.data = new byte[TransferManager.MAX_CHUNK_SIZE];
        File file = new File("test.chunk");
        logInfo("Writing test chunk (" + chunk.data.length + " bytes) to "
            + file);
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
            file));
        out.writeObject(chunk);
        out.close();
    }

    private void testSocketResolving(String dyndns) {
        logInfo("Testing local socket resolving for '" + dyndns + "'");
        InetSocketAddress addr = new InetSocketAddress(dyndns, 1337);
        logInfo("Socket successfully created: " + addr);
        logInfo("Is fully resolved ?: " + !addr.isUnresolved());
    }

    private void testTimeCalculations() {
        logInfo("Testing date calculation/UTC");
        logInfo("Current time in UTC: "
            + new Date(Convert.convertToUTC(new Date())));
    }

    private boolean testFileWrite() throws IOException {
        File testFile = new File("test.xxx");
        logInfo("Testing modified Date differ on file " + testFile);
        String test = "xx";
        int matches = 0;
        int incorrect = 0;

        for (int i = 0; i < 20; i++) {
            FileOutputStream fOut = new FileOutputStream(testFile, true);
            test += test;
            fOut.write(test.getBytes());
            fOut.close();
            long currentMS = Convert.convertToGlobalPrecision(System
                .currentTimeMillis());
            testFile.setLastModified(currentMS);
            long fileModified = testFile.lastModified();
            long expectedMS = Convert.convertToGlobalPrecision(currentMS);
            if (fileModified == currentMS) {
                matches++;
            } else {
                logWarning("Last-modfied differs from last-modified on file:");
                logWarning("Set:  " + new Date(currentMS) + " in ms: "
                    + currentMS);
                logWarning("File: " + new Date(fileModified) + " in ms: "
                    + fileModified);
                logWarning("Expt: " + new Date(expectedMS) + " in ms: "
                    + expectedMS);
                incorrect++;
            }
            try {
                Thread.sleep((long) (Math.random() * 500));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        logInfo("testFileWrite finished. matches: " + matches
            + ", incorrect dates: " + incorrect);
        return incorrect != 0;
    }
}