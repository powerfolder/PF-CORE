package de.dal33t.powerfolder.test.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.UUID;

import de.dal33t.powerfolder.util.ByteSerializer;
import de.dal33t.powerfolder.util.Format;

import junit.framework.TestCase;

public class SerializationCompareTest extends TestCase {

    public void testManySerializeable() throws IOException {
        FileInfoSerializable[] files = new FileInfoSerializable[10000];
        for (int i = 0; i < files.length; i++) {
            files[i] = createRandomFileInfo();
        }
        long start = System.currentTimeMillis();
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bOut);
        for (int i = 0; i < files.length; i++) {
            out.writeObject(files[i]);
        }
        out.close();
        long took = System.currentTimeMillis() - start;
        System.out.println("(Serializable) Files " + files.length + ", size: "
            + bOut.toByteArray().length + ", took " + took + "ms");
    }

    public void testSingleSerializeable() throws IOException {
        FileInfoSerializable fileInfo = createRandomFileInfo();
        long start = System.currentTimeMillis();
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bOut);
        out.writeObject(fileInfo);
        out.close();
        long took = System.currentTimeMillis() - start;
        System.out.println("(Serializable) 1 File, size: "
            + bOut.toByteArray().length + ", took " + took + "ms");
    }

    public void testManyExternalizable() throws IOException {
        FileInfoExternalizable[] files = new FileInfoExternalizable[10000];
        for (int i = 0; i < files.length; i++) {
            files[i] = createRandomFileInfo2();
        }
        long start = System.currentTimeMillis();
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bOut);
        for (int i = 0; i < files.length; i++) {
            out.writeObject(files[i]);
        }
        out.close();
        long took = System.currentTimeMillis() - start;
        System.out.println("(Externalizable) Files " + files.length
            + ", size: " + bOut.toByteArray().length + ", took " + took + "ms");
    }

    public void testSingleExternalizable() throws IOException {
        FileInfoExternalizable fileInfo = createRandomFileInfo2();
        long start = System.currentTimeMillis();
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bOut);
        out.writeObject(fileInfo);
        out.close();
        long took = System.currentTimeMillis() - start;
        System.out.println("(Externalizable) 1 Files, size: "
            + bOut.toByteArray().length + ", took " + took + "ms");
    }

    public void testZipNonZipComapreTest() throws IOException {
        FileInfoSerializable[] files = new FileInfoSerializable[10000];
        for (int i = 0; i < files.length; i++) {
            files[i] = createRandomFileInfo();
            if (i % 200 == 0) {
            ByteSerializer.serializeStatic(files[i], true);
            }
        }
        long start = System.currentTimeMillis();
        byte[] compressed = ByteSerializer.serializeStatic(files, true);
        byte[] uncompressed = ByteSerializer.serializeStatic(files, false);
        long took = System.currentTimeMillis() - start;
        System.out.println("Compression compare: "
            + Format.formatBytes(compressed.length) + " - "
            + Format.formatBytes(uncompressed.length));
    }

    private static FileInfoSerializable createRandomFileInfo() {
        FileInfoSerializable f = new FileInfoSerializable();
        f.fileName = UUID.randomUUID().toString() + "/"
            + UUID.randomUUID().toString();
        f.deleted = false;
        f.folderInfo = UUID.randomUUID().toString();
        f.modifiedBy = UUID.randomUUID().toString();
        f.lastModifiedDate = new Date();
        f.size = new Long((long) (Math.random() * 1000000));
        f.version = (int) (Math.random() * 100);
        return f;
    }

    private static FileInfoExternalizable createRandomFileInfo2() {
        FileInfoExternalizable f = new FileInfoExternalizable();
        f.fileName = UUID.randomUUID().toString() + "/"
            + UUID.randomUUID().toString();
        f.deleted = false;
        f.folderInfo = UUID.randomUUID().toString();
        f.modifiedBy = UUID.randomUUID().toString();
        f.lastModifiedDate = new Date();
        f.size = new Long((long) (Math.random() * 1000000));
        f.version = (int) (Math.random() * 100);
        return f;
    }
}
