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
 */
package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.test.TestHelper;
import junit.framework.TestCase;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    public void testCopyStreamCrypto() throws IOException {
        Files.createDirectories(Paths.get("build/test"));

        Path inFile = TestHelper.createRandomFile(Paths.get("build/test/randomfile.crypto"));
        Path outFile = Paths.get("build/test/randomfile_out.crypto");

        OutputStream outputStream = Files.newOutputStream(outFile);
        StreamUtils.copyToStream(inFile, outputStream);
        outputStream.close();

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


    public void testReadIndexOutOfBoundsOffset() throws IOException {
        PipedInputStream inputStream = new PipedInputStream();
        PipedOutputStream outputStream = new PipedOutputStream();

        inputStream.connect(outputStream);

        new WriterThread("1234567890".getBytes(), 5000, outputStream, true).start();

        byte[] buf = new byte[1000];
        try {
            int read = StreamUtils.read(inputStream, buf, -10, 10);
            fail("Exception was not thrown when offset is < 0");
        } catch (IndexOutOfBoundsException e){
            // It's expected to throw an exception since offset is < 0
        }
    }

    public void testReadIndexOutOfBoundsLength() throws IOException {
        PipedInputStream inputStream = new PipedInputStream();
        PipedOutputStream outputStream = new PipedOutputStream();

        inputStream.connect(outputStream);

        new WriterThread("1234567890".getBytes(), 5000, outputStream, true).start();

        byte[] buf = new byte[1000];
        try {
            int read = StreamUtils.read(inputStream, buf, 0, -1);
            fail("Exception was not thrown when size is < 0");
        } catch (IndexOutOfBoundsException e){
            // It's expected to throw an exception since offset is < 0
        }
    }

    public void testReadIndexOutOfBoundsLengthSmallerThanArray() throws IOException {
        PipedInputStream inputStream = new PipedInputStream();
        PipedOutputStream outputStream = new PipedOutputStream();

        inputStream.connect(outputStream);

        new WriterThread("1234567890".getBytes(), 5000, outputStream, true).start();

        byte[] buf = new byte[5];
        try {
            //size - nTotalRead (20 - 0 = 20) > buf.length - offset (5-0 = 5)
            int read = StreamUtils.read(inputStream, buf, 0, 20);
            fail("Exception was not thrown when size is < 0");
        } catch (IndexOutOfBoundsException e){
            // It's expected to throw an exception since offset is < 0
        }
    }

    public void testReadEndOfFile() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream();
        in.connect(out);

        new WriterThread("12345".getBytes(), 5000, out, true).start();

        byte[] buf = new byte[1000];
        try {
            int read = StreamUtils.read(in, buf, 0, 10);
            fail("Should have thrown EOF exception because InputStream.read returned -1");
        } catch (EOFException e) {
            //OK since there will be nothing more to read
        }
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

    public void testReadIntoByteArrayNullInputStream(){
        try {
            StreamUtils.readIntoByteArray(null);
            fail("Did not throw null pointer exception when input stream was null");
        } catch (NullPointerException e){
            //OK Since it is expected to
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testReadIntoByteArrayOk() throws IOException {
        byte[] array = new byte[99];
        for (int i = 0; i < 99; i++) {
            array[i] = (byte) i;
        }
        InputStream inputStream = new ByteArrayInputStream(array);
        byte[] out = StreamUtils.readIntoByteArray(inputStream);
        for (int index = 0; index < 99; index++) {
            assertEquals(array[index], out[index]);
        }
    }

    public void testReadIntOk() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream();
        in.connect(out);

        byte[] bytes = {1,2,3,4};
        new WriterThread(bytes, 10000, out, false).start();

        int read = StreamUtils.readInt(in);
        assertEquals(16909060, read);
    }

    public void testReadIntThreeElements() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream();
        in.connect(out);

        byte[] bytes = {1,2,3};
        new WriterThread(bytes, 10000, out, false).start();

        try {
            int read = StreamUtils.readInt(in);
            fail("Did not throw IO exception");
        } catch (IOException e){
            //OK, supposed to throw IOException
            out.close();
        }
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