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
package de.dal33t.powerfolder.test.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.UUID;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.ByteSerializer;
import de.dal33t.powerfolder.util.Format;

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
