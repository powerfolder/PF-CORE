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
package de.dal33t.powerfolder.util;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import de.dal33t.powerfolder.util.Translation;

public class TranslationTest {

    @Test
    public void testParams() {
        String text = Translation
            .get("uicontroller.remote_mass_delete.warning_message");
        assertTrue( text.contains("{0}"),"Param key missing: " + text);
        assertTrue( text.contains("{1}"),"Param key missing: " + text);
        assertTrue( text.contains("{2}"),"Param key missing: " + text);
        assertTrue( text.contains("{3}"),"Param key missing: " + text);
        assertTrue( text.contains("{4}"),"Param key missing: " + text);
        text = Translation.get(
            "uicontroller.remote_mass_delete.warning_message", "PARAM0",
            "PARAM1", "PARAM2", "PARAM3", "PARAM4");
        assertTrue( text
            .contains("PARAM0"),"Param replacement missing: " + text);
        assertTrue( text
            .contains("PARAM1"),"Param replacement missing: " + text);
        assertTrue( text
            .contains("PARAM2"),"Param replacement missing: " + text);
        assertTrue( text
            .contains("PARAM3"),"Param replacement missing: " + text);
        assertTrue( text
            .contains("PARAM4"),"Param replacement missing: " + text);
    }

    @Test
    public void testPlaceholders() {
        String text = Translation
            .get("action_login.description");
        assertTrue( text.contains("PowerFolder"),"Text fail: " + text);
        assertFalse( text.contains("{PowerFolder}"),"Text fail: " + text);
        Translation.setPlaceHolder("APPNAME", "XXX");
        text = Translation.get("action_login.description");
        assertTrue( text.contains("XXX"),"Text fail: " + text);
        assertFalse( text.contains("{XXX}"),"Text fail: " + text);

        Translation.setPlaceHolder("APPNAME", null);
        text = Translation.get("action_login.description");
        assertTrue( text.contains("{APPNAME}"),"Text fail: " + text);
    }
}
