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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.FileUtils;


/**
 * A Testclass for testing bug conditions on serveral machines
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.7 $
 */
public class Test {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws IOException {
        Test test = new Test();
        log.info("------------- Starting tests ----------------------------------");
        
        test.testWebStartMime();
        log.info("---------------------------------------------------------------");

        test.testCorruptZipFile();
        log.info("---------------------------------------------------------------");

        test.testInt2Bytes();
        log.info("---------------------------------------------------------------");

        test.testFileChunkSize();
        log.info("---------------------------------------------------------------");

        if (args != null && args.length > 0) {
            test.testSocketResolving(args[0]);
            log.info("---------------------------------------------------------------");
        }

        test.testTimeCalculations();
        log.info("---------------------------------------------------------------");

        test.testFileWrite();
        log.info("---------------------------------------------------------------");
    }

    private void testWebStartMime() {
        try {
            URL url = new URL(
                "http://webstart.powerfolder.com/release/PowerFolder.jnlp");
            log.info("Testing mime type for webstart URL '" + url + "'");
            log.info("Mime type is "
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
        log.info(file + " is a valid zip ? " + FileUtils.isValidZipFile(file));
    }

    private void testInt2Bytes() {
        int i = 12345678;
        log.info("Testing int 2 byte conversion with " + i);
        byte b1 = (byte) (i & 0xFF);
        byte b2 = (byte) (0xFF & (i >> 8));
        byte b3 = (byte) (0xFF & (i >> 16));
        byte b4 = (byte) (0xFF & (i >> 24));
        log.info("Byte 1: " + b1);
        log.info("Byte 2: " + b2);
        log.info("Byte 3: " + b3);
        log.info("Byte 4: " + b4);

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

        log.info("Converted back to: " + w);
    }

    private void testFileChunkSize() throws IOException {
        FileChunk chunk = new FileChunk();
        chunk.data = new byte[Integer
            .valueOf(ConfigurationEntry.TRANSFERS_MAX_FILE_CHUNK_SIZE
                .getDefaultValue())];
        File file = new File("test.chunk");
        log.info("Writing test chunk (" + chunk.data.length + " bytes) to "
            + file);
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
            file));
        out.writeObject(chunk);
        out.close();
    }

    private void testSocketResolving(String dyndns) {
        log.info("Testing local socket resolving for '" + dyndns + "'");
        InetSocketAddress addr = new InetSocketAddress(dyndns, 1337);
        log.info("Socket successfully created: " + addr);
        log.info("Is fully resolved ?: " + !addr.isUnresolved());
    }

    private void testTimeCalculations() {
        log.info("Testing date calculation/UTC");
        log.info("Current time in UTC: "
            + new Date(Convert.convertToUTC(new Date())));
    }

    private boolean testFileWrite() throws IOException {
        File testFile = new File("test.xxx");
        log.info("Testing modified Date differ on file " + testFile);
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
                log.warning("Last-modfied differs from last-modified on file:");
                log.warning("Set:  " + new Date(currentMS) + " in ms: "
                    + currentMS);
                log.warning("File: " + new Date(fileModified) + " in ms: "
                    + fileModified);
                log.warning("Expt: " + new Date(expectedMS) + " in ms: "
                    + expectedMS);
                incorrect++;
            }
            try {
                Thread.sleep((long) (Math.random() * 500));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        log.info("testFileWrite finished. matches: " + matches
            + ", incorrect dates: " + incorrect);
        return incorrect != 0;
    }
}