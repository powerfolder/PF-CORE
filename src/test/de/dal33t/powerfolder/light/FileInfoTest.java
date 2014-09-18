/*
 * Copyright 2004 - 2013 Christian Sprajc. All rights reserved.
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
 * $Id: FileInfoTest.java 20159 2012-11-24 19:44:04Z sprajc $
 */
package de.dal33t.powerfolder.light;

import java.io.IOException;
import java.util.Date;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.ByteSerializer;
import de.dal33t.powerfolder.util.IdGenerator;

public class FileInfoTest extends TestCase {

    public void testRenameRelativeFileName() throws Exception {

        // Check root level file name change.
        assertEquals("Simple", "myFile.txt",
            FileInfo.renameRelativeFileName("oldFile.txt", "myFile.txt"));

        // Check single-depth change.
        assertEquals("Directory", "directory/myFile.txt",
            FileInfo.renameRelativeFileName("directory/oldFile.txt",
                "myFile.txt"));

        // Check multiple directory depth change.
        assertEquals("Subdirectory", "directory/subdirectory/myFile.txt",
            FileInfo.renameRelativeFileName(
                "directory/subdirectory/oldFile.txt", "myFile.txt"));

        // Check illegal '/' character in new file name.
        try {
            FileInfo.renameRelativeFileName("directory/oldFile.txt",
                "duff/File.txt");
            fail("Unix separator");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testSerialize() throws IOException, ClassNotFoundException {
        FolderInfo foInfo = new FolderInfo("Random name of folder "
            + IdGenerator.makeFolderId(), IdGenerator.makeFolderId());
        MemberInfo mInfo = new MemberInfo("Nickname", IdGenerator.makeId(),
            IdGenerator.makeId());
        FileInfo fInfo = FileInfoFactory
            .unmarshallExistingFile(foInfo, "subdir/Xyz/Filename.xlsx", null,
                6300404, mInfo, new Date(), 4711, null, false, null);

        byte[] buf = ByteSerializer.serializeStatic(fInfo, true);

        FileInfo copy = (FileInfo) ByteSerializer.deserializeStatic(buf, true);

        // Test
        assertEquals(fInfo, copy);
        assertEquals(fInfo.getFolderInfo(), copy.getFolderInfo());
        assertEquals(fInfo.getRelativeName(), copy.getRelativeName());
        assertEquals(fInfo.getFilenameOnly(), copy.getFilenameOnly());
        assertEquals(fInfo.getExtension(), copy.getExtension());
        assertEquals(fInfo.getSize(), copy.getSize());
        assertEquals(fInfo.getModifiedBy(), copy.getModifiedBy());
        assertEquals(fInfo.getModifiedDate(), copy.getModifiedDate());
        assertEquals(fInfo.getVersion(), copy.getVersion());

    }
}
