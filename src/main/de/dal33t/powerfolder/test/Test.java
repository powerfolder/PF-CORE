/* $Id: Test.java,v 1.7 2005/04/25 02:51:37 totmacherr Exp $
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
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Util;

/**
 * A Testclass for testing bug conditions on serveral machines
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.7 $
 */
public class Test {
    private static final Logger LOG = Logger.getLogger(Test.class);

    public static void main(String[] args) throws IOException {
        Test test = new Test();
        LOG
            .info("------------- Starting tests ----------------------------------");
        
        test.testWebStartMime();
        LOG
            .info("---------------------------------------------------------------");

        test.testCorruptZipFile();
        LOG
            .info("---------------------------------------------------------------");

        test.testInt2Bytes();
        LOG
            .info("---------------------------------------------------------------");

        test.testFileChunkSize();
        LOG
            .info("---------------------------------------------------------------");

        if (args != null && args.length > 0) {
            test.testSocketResolving(args[0]);
            LOG
                .info("---------------------------------------------------------------");
        }

        test.testTimeCalculations();
        LOG
            .info("---------------------------------------------------------------");

        test.testFileWrite();
        LOG
            .info("---------------------------------------------------------------");
    }

    private void testWebStartMime() {
        try {
            URL url = new URL(
                "http://webstart.powerfolder.com/release/PowerFolder.jnlp");
            LOG.info("Testing mime type for webstart URL '" + url + "'");
            LOG.info("Mime type is "
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
        LOG.info(file + " is a valid zip ? " + Util.isValidZipFile(file));
    }

    private void testInt2Bytes() {
        int i = 12345678;
        LOG.info("Testing int 2 byte conversion with " + i);
        byte b1 = (byte) (i & 0xFF);
        byte b2 = (byte) (0xFF & (i >> 8));
        byte b3 = (byte) (0xFF & (i >> 16));
        byte b4 = (byte) (0xFF & (i >> 24));
        LOG.info("Byte 1: " + b1);
        LOG.info("Byte 2: " + b2);
        LOG.info("Byte 3: " + b3);
        LOG.info("Byte 4: " + b4);

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

        LOG.info("Converted back to: " + w);
    }

    private void testFileChunkSize() throws IOException {
        FileChunk chunk = new FileChunk();
        chunk.data = new byte[TransferManager.MAX_CHUNK_SIZE];
        File file = new File("test.chunk");
        LOG.info("Writing test chunk (" + chunk.data.length + " bytes) to "
            + file);
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
            file));
        out.writeObject(chunk);
        out.close();
    }

    private void testSocketResolving(String dyndns) {
        LOG.info("Testing local socket resolving for '" + dyndns + "'");
        InetSocketAddress addr = new InetSocketAddress(dyndns, 1337);
        LOG.info("Socket successfully created: " + addr);
        LOG.info("Is fully resolved ?: " + !addr.isUnresolved());
    }

    private void testTimeCalculations() {
        LOG.info("Testing date calculation/UTC");
        LOG.info("Current time in UTC: "
            + new Date(Convert.convertToUTC(new Date())));
    }

    private boolean testFileWrite() throws IOException {
        File testFile = new File("test.xxx");
        LOG.info("Testing modified Date differ on file " + testFile);
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
                LOG.warn("Last-modfied differs from last-modified on file:");
                LOG.warn("Set:  " + new Date(currentMS) + " in ms: "
                    + currentMS);
                LOG.warn("File: " + new Date(fileModified) + " in ms: "
                    + fileModified);
                LOG.warn("Expt: " + new Date(expectedMS) + " in ms: "
                    + expectedMS);
                incorrect++;
            }
            try {
                Thread.sleep((long) (Math.random() * 500));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        LOG.info("testFileWrite finished. matches: " + matches
            + ", incorrect dates: " + incorrect);
        return incorrect != 0;
    }
}