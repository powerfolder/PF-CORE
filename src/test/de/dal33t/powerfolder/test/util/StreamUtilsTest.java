package de.dal33t.powerfolder.test.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import junit.framework.TestCase;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.util.StreamUtils;

public class StreamUtilsTest extends TestCase {

    public void testByteArrayStream() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[12324];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (Math.random() * 256);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        StreamUtils.copyToStream(in, out);
        out.close();
        assertTrue(byteArrayEquals(data, out.toByteArray()));
    }

    public void testFileCopy() throws IOException {
        new File("build/test").mkdirs();
        File inFile = TestHelper.createRandomFile(new File(
            "build/test/randomfile.txt"));
        File outFile = new File(
        "build/test/randomfile_out.txt");
        OutputStream out = new FileOutputStream(outFile);
        StreamUtils.copyToStream(inFile, out);
        out.close();
        assertTrue(outFile.exists());
        assertEquals(inFile.length(), outFile.length());
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
}
