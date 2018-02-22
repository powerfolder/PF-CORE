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

import junit.framework.TestCase;

/**
 * Created by sprajc on 23.05.17.
 */
public class FileInfoFactoryTest extends TestCase {

    public void testEncodeDecodeIllegalCharacters() {
        String testString = "PhD_GOE&CPH-221216.";
        String encoded = FileInfoFactory.encodeIllegalChars(testString);
        String returned = FileInfoFactory.decodeIllegalChars(encoded);
        assertEquals(testString, returned);

        testString = "PhDFiles\\PhD_GOE&CPH-221216.";
        encoded = FileInfoFactory.encodeIllegalChars(testString);
        returned = FileInfoFactory.decodeIllegalChars(encoded);
        assertEquals(testString, returned);

        String decoded = FileInfoFactory.decodeIllegalChars(testString);
        returned = FileInfoFactory.encodeIllegalChars(decoded);
        assertEquals(testString, returned);

        testString = "$%$";
        decoded = FileInfoFactory.decodeIllegalChars(testString);
        System.out.println(decoded);
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
    }
}
