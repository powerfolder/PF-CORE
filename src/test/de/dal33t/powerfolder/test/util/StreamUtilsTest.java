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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.test.TestHelper;

public class StreamUtilsTest extends TestCase {

    public void testByteArrayStream() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[12324];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (Math.random() * 256);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        long read = StreamUtils.copyToStream(in, out);
        out.close();
        assertEquals(data.length, read);
        assertTrue(byteArrayEquals(data, out.toByteArray()));
    }

    public void testFileCopy() throws IOException {
        Files.createDirectories(Paths.get("build/test"));
        Path inFile = TestHelper.createRandomFile(Paths.get(
            "build/test/randomfile.txt"));
        Path outFile = Paths.get("build/test/randomfile_out.txt");
        OutputStream out = Files.newOutputStream(outFile);
        StreamUtils.copyToStream(inFile, out);
        out.close();
        assertTrue(Files.exists(outFile));
        assertEquals(Files.size(inFile), Files.size(outFile));
        Files.delete(inFile);
    }

    public void testReadOk() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream();
        in.connect(out);

        new WriterThread("1234567890".getBytes(), 5000, out, true).start();

        byte[] buf = new byte[1000];
        int read = StreamUtils.read(in, buf, 0, 10);
        assertEquals(10, read);
    }

    public void testReadFail() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream();
        in.connect(out);

        new WriterThread("1234567890123456789".getBytes(), 2000, out, true)
            .start();

        byte[] buf = new byte[1000];
        int read = 0;
        try {
            read = StreamUtils.read(in, buf, 0, 200);
            fail("Inputstream should have been closed, but read did not fail!");
        } catch (IOException e) {
            // IS OK! Should wait for 200 bytes, but the inputstream gets closed
            // before.
        }
        assertEquals(0, read);
    }

    public void testCopyStream() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream();
        in.connect(out);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        StringBuilder b = new StringBuilder();
        int testLength = 12 * 1000;
        for (int i = 0; i < testLength; i++) {
            b.append((char) (Math.random() * 1000));
        }
        byte[] buf = b.toString().getBytes();
        new WriterThread(buf, 10000, out, false).start();

        long read = StreamUtils.copyToStream(in, bOut, testLength - 1000);
        byte[] output = bOut.toByteArray();
        assertEquals("Too much data written sto stream!", testLength - 1000,
            output.length);
        assertEquals(new String(buf, 0, testLength - 1000), new String(output));
        assertEquals(testLength - 1000, read);
    }

    private boolean byteArrayEquals(byte[] b1, byte[] b2) {
        if (b2.length > b1.length) {
            return false;
        }
        for (int i = 0; i < b2.length; i++) {
            if (b1[i] != b2[i]) {
                return false;
            }
        }
        return true;
    }

    private class WriterThread extends Thread {
        private byte[] buf;
        private long waitTime;
        private OutputStream out;
        private boolean close;

        private WriterThread(byte[] buf, long waitTime, OutputStream out,
            boolean close)
        {
            super();
            this.buf = buf;
            this.waitTime = waitTime;
            this.out = out;
            this.close = close;
        }

        public void run() {
            try {
                Thread.sleep(waitTime);
                out.write(buf);
                if (close) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}
