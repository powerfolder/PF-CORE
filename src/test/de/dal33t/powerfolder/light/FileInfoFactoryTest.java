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
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by sprajc on 23.05.17.
 */
public class FileInfoFactoryTest {


    public void xtestPFC3428() {
        String diskFile = "KV402505_Buch Resonanzräume.PDF";
        String funame = "KV402505_Buch Resonanzräume.PDF";
        assertFalse(funame.equals(diskFile));
        FileInfo fileInfo = FileInfoFactory.unmarshallExistingFile(
                FolderInfoFactory.lookupInstance("123"), funame, null, 100, null, null,
                new Date(), 0, null, false, null);

        // Should be converted internally
        assertEquals(diskFile, fileInfo.getRelativeName());
    }

    @Test
    public void testRenameConflictResolve() {
        FolderInfo z = FolderInfoFactory.unmarshallExistingTopFolder("ID", "Z", 1);
        FolderInfo zResolved = FolderInfoFactory.resolveConflict(z);
        assertEquals(z.getId(), zResolved.getId());
        assertEquals(z.getName(), zResolved.getName());
        assertEquals(2, zResolved.getVersion());
    }

    @Test
    public void testChangedFolderInfoLookupInstancePreservesDirectoryType() {
        FolderInfo folderA = FolderInfoFactory.newTopFolder("A", "FolderA");
        FolderInfo folderB = FolderInfoFactory.newTopFolder("B", "FolderB");

        DirectoryInfo dirInfo = (DirectoryInfo) FileInfoFactory.lookupInstance(folderA, "some/directory", true);
        assertTrue(dirInfo.isLookupInstance(), "Original should be lookup instance");
        assertTrue(dirInfo.isDiretory(), "Original should be DirectoryInfo");

        FileInfo result = FileInfoFactory.changedFolderInfo(dirInfo, folderB);

        assertTrue(result.isLookupInstance(), "Result should be lookup instance");
        assertTrue(result.isDiretory(), "Result must remain DirectoryInfo");
        assertFalse(result.isFile(), "Result must not be FileInfo");
        assertEquals("some/directory", result.getRelativeName());
        assertEquals(folderB, result.getFolderInfo());
    }

    @Test
    public void testChangedFolderInfoLookupInstanceFileInfoStaysFileInfo() {
        FolderInfo folderA = FolderInfoFactory.newTopFolder("A", "FolderA");
        FolderInfo folderB = FolderInfoFactory.newTopFolder("B", "FolderB");

        FileInfo fileInfo = FileInfoFactory.lookupInstance(folderA, "some/file.txt", false);
        assertTrue(fileInfo.isLookupInstance(), "Original should be lookup instance");
        assertTrue(fileInfo.isFile(), "Original should be FileInfo");

        FileInfo result = FileInfoFactory.changedFolderInfo(fileInfo, folderB);

        assertTrue(result.isLookupInstance(), "Result should be lookup instance");
        assertTrue(result.isFile(), "Result must remain FileInfo");
        assertEquals("some/file.txt", result.getRelativeName());
        assertEquals(folderB, result.getFolderInfo());
    }

    @Test
    public void testChangedFolderInfoNonLookupDirectoryPreservesFields() {
        FolderInfo folderA = FolderInfoFactory.newTopFolder("A", "FolderA");
        FolderInfo folderB = FolderInfoFactory.newTopFolder("B", "FolderB");

        DirectoryInfo dirInfo = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(
                folderA, "some/dir", null, 0, null, null, new Date(), 3, null, true, null);
        assertFalse(dirInfo.isLookupInstance(), "Should not be lookup instance");
        assertTrue(dirInfo.isDiretory(), "Should be DirectoryInfo");

        FileInfo result = FileInfoFactory.changedFolderInfo(dirInfo, folderB);

        assertFalse(result.isLookupInstance(), "Result should not be lookup instance");
        assertTrue(result.isDiretory(), "Result must remain DirectoryInfo");
        assertEquals(3, result.getVersion());
        assertEquals(folderB, result.getFolderInfo());
    }

    @Test
    public void testSetTagsFileIncrementsVersionKeepsHash() {
        FolderInfo folder = FolderInfoFactory.newTopFolder("A", "FolderA");
        Date date = new Date();
        FileInfo original = FileInfoFactory.unmarshallExistingFile(
                folder, "docs/report.txt", "oid1", 123L, null, null, date, 4, "HASH", false, null);
        assertNull(original.getTags(), "Precondition: tags empty");

        FileInfo tagged = FileInfoFactory.withTags(original, "[\"Contract\",\"2026\"]");

        assertEquals("[\"Contract\",\"2026\"]", tagged.getTags());
        assertEquals(5, tagged.getVersion(), "Version must increment");
        assertEquals("HASH", tagged.getHashes(), "Content hash unchanged");
        assertEquals(123L, tagged.getSize());
        assertEquals(date, tagged.getModifiedDate());
        assertEquals("docs/report.txt", tagged.getRelativeName());
        assertTrue(tagged.isFile());
        assertFalse(tagged.isLookupInstance());
        assertNull(original.getTags(), "Original must stay immutable");
        assertEquals(4, original.getVersion());
    }

    @Test
    public void testSetTagsDirectoryStaysDirectory() {
        FolderInfo folder = FolderInfoFactory.newTopFolder("B", "FolderB");
        DirectoryInfo dir = (DirectoryInfo) FileInfoFactory.unmarshallExistingFile(
                folder, "some/dir", "oidD", 0L, null, null, new Date(), 3, "H", true, null);
        FileInfo tagged = FileInfoFactory.withTags(dir, "[\"X\"]");
        assertTrue(tagged.isDiretory(), "Must remain DirectoryInfo");
        assertFalse(tagged.isFile());
        assertEquals(4, tagged.getVersion());
        assertEquals("[\"X\"]", tagged.getTags());
        assertEquals("H", tagged.getHashes());
    }

    @Test
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
