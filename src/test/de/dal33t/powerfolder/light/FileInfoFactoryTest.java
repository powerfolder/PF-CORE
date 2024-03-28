/*
 * Copyright 2004 - 2017 Christian Sprajc. All rights reserved.
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


    public void testPFC3428() {
        String diskFile = "KV402505_Buch Resonanzräume.PDF";
        String funame = "KV402505_Buch Resonanzräume.PDF";
        assertFalse(funame.equals(diskFile));
        FileInfo fileInfo = FileInfoFactory.unmarshallExistingFile(
                FolderInfoFactory.lookupInstance("123"), funame, null, 100, null, null,
                new Date(), 0, null, false, null);

        // Should be converted internally
        assertEquals(diskFile, fileInfo.getRelativeName());
    }

    public void testPFC3375() {
        String input = StringUtils.AEL_SPECIAL_ENCODING_UNICODE + StringUtils.AEU_SPECIAL_ENCODING_UNICODE
                + StringUtils.OEL_SPECIAL_ENCODING_UNICODE + StringUtils.OEU_SPECIAL_ENCODING_UNICODE
                + StringUtils.UEL_SPECIAL_ENCODING_UNICODE + StringUtils.UEU_SPECIAL_ENCODING_UNICODE;
        String expected = "äÄöÖüÜ";
        assertEquals(expected, FileInfoFactory.encodeIllegalChars(input));
    }

    public void testRenameConflictResolve() {
        FolderInfo z = FolderInfoFactory.unmarshallExistingTopFolder("ID", "Z", 1);
        FolderInfo zResolved = FolderInfoFactory.resolveConflict(z);
        assertEquals(z.getId(), zResolved.getId());
        assertEquals(z.getName(), zResolved.getName());
        assertEquals(2, zResolved.getVersion());
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
