/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
package de.dal33t.powerfolder.light;

import de.dal33t.powerfolder.util.StringUtils;
import junit.framework.TestCase;

import java.util.Date;

/**
 * Created by sprajc on 23.05.17.
 */
public class FileInfoFactoryTest extends TestCase {


    public void xtestPFC3428() {
        String diskFile = "KV402505_Buch Resonanzräume.PDF";
        String funame = "KV402505_Buch Resonanzräume.PDF";
        assertFalse(funame.equals(diskFile));
        FileInfo fileInfo = FileInfoFactory.unmarshallExistingFile(
                FolderInfoFactory.lookupInstance("123"), funame, null, 100, null, null,
                new Date(), 0, null, false, null);

        // Should be converted internally
        assertEquals(diskFile, fileInfo.getRelativeName());
    }

    public void testRenameConflictResolve() {
        FolderInfo z = FolderInfoFactory.unmarshallExistingTopFolder("ID", "Z", 1);
        FolderInfo zResolved = FolderInfoFactory.resolveConflict(z);
        assertEquals(z.getId(), zResolved.getId());
        assertEquals(z.getName(), zResolved.getName());
        assertEquals(2, zResolved.getVersion());
    }

    public void testChangedFolderInfoLookupInstancePreservesDirectoryType() {
        FolderInfo folderA = FolderInfoFactory.newTopFolder("A", "FolderA");
        FolderInfo folderB = FolderInfoFactory.newTopFolder("B", "FolderB");

        DirectoryInfo dirInfo = (DirectoryInfo) FileInfoFactory.lookupInstance(folderA, "some/directory", true);
        assertTrue("Original should be lookup instance", dirInfo.isLookupInstance());
        assertTrue("Original should be DirectoryInfo", dirInfo.isDiretory());

        FileInfo result = FileInfoFactory.changedFolderInfo(dirInfo, folderB);

        assertTrue("Result should be lookup instance", result.isLookupInstance());
        assertTrue("Result must remain DirectoryInfo", result.isDiretory());
        assertFalse("Result must not be FileInfo", result.isFile());
        assertEquals("some/directory", result.getRelativeName());
        assertEquals(folderB, result.getFolderInfo());
    }

    public void testChangedFolderInfoLookupInstanceFileInfoStaysFileInfo() {
        FolderInfo folderA = FolderInfoFactory.newTopFolder("A", "FolderA");
        FolderInfo folderB = FolderInfoFactory.newTopFolder("B", "FolderB");

        FileInfo fileInfo = FileInfoFactory.lookupInstance(folderA, "some/file.txt", false);
        assertTrue("Original should be lookup instance", fileInfo.isLookupInstance());
        assertTrue("Original should be FileInfo", fileInfo.isFile());

        FileInfo result = FileInfoFactory.changedFolderInfo(fileInfo, folderB);

        assertTrue("Result should be lookup instance", result.isLookupInstance());
        assertTrue("Result must remain FileInfo", result.isFile());
        assertEquals("some/file.txt", result.getRelativeName());
        assertEquals(folderB, result.getFolderInfo());
    }

    public void testChangedFolderInfoNonLookupDirectoryPreservesFields() {
        FolderInfo folderA = FolderInfoFactory.newTopFolder("A", "FolderA");
        FolderInfo folderB = FolderInfoFactory.newTopFolder("B", "FolderB");

        DirectoryInfo dirInfo = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(
                folderA, "some/dir", null, 0, null, null, new Date(), 3, null, true, null);
        assertFalse("Should not be lookup instance", dirInfo.isLookupInstance());
        assertTrue("Should be DirectoryInfo", dirInfo.isDiretory());

        FileInfo result = FileInfoFactory.changedFolderInfo(dirInfo, folderB);

        assertFalse("Result should not be lookup instance", result.isLookupInstance());
        assertTrue("Result must remain DirectoryInfo", result.isDiretory());
        assertEquals(3, result.getVersion());
        assertEquals(folderB, result.getFolderInfo());
    }

    public void testEncodeDecodeIllegalCharacters() {
        String testString = "PhD_GOE&CPH-221216.";
        String encoded = FileInfoFactory.encodeIllegalChars(testString);
        String returned = FileInfoFactory.decodeIllegalChars(encoded);
        assertEquals(testString, returned);

        testString = "PhDFiles\\PhD_GOE&CPH-221216.";
        encoded = FileInfoFactory.encodeIllegalChars(testString);
        returned = FileInfoFactory.decodeIllegalChars(encoded);
        assertEquals(testString, returned);

        testString = "$%$";
        String decoded = FileInfoFactory.decodeIllegalChars(testString);
        returned = FileInfoFactory.encodeIllegalChars(decoded);
        assertEquals(testString, returned);

        testString = "$%$%/$%csdf$%$%%$";
        decoded = FileInfoFactory.decodeIllegalChars(testString);
        returned = FileInfoFactory.encodeIllegalChars(decoded);
        assertEquals(testString, returned);

        testString = "$%$%$%$%$";
        decoded = FileInfoFactory.decodeIllegalChars(testString);
        returned = FileInfoFactory.encodeIllegalChars(decoded);
        assertEquals(testString, returned);

        testString = "  |||:::*?<>  . ";
        encoded = FileInfoFactory.encodeIllegalChars(testString);
        returned = FileInfoFactory.decodeIllegalChars(encoded);
        assertEquals(testString, returned);

        testString = "  |||:::*?<>  ";
        encoded = FileInfoFactory.encodeIllegalChars(testString);
        returned = FileInfoFactory.decodeIllegalChars(encoded);
        assertEquals(testString, returned);
    }
}
