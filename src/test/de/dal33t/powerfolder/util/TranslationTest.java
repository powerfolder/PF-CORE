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

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.Translation;

public class TranslationTest extends TestCase {

    public void testParams() {
        String text = Translation
            .get("uicontroller.remote_mass_delete.warning_message");
        assertTrue("Param key missing: " + text, text.contains("{0}"));
        assertTrue("Param key missing: " + text, text.contains("{1}"));
        assertTrue("Param key missing: " + text, text.contains("{2}"));
        assertTrue("Param key missing: " + text, text.contains("{3}"));
        assertTrue("Param key missing: " + text, text.contains("{4}"));
        text = Translation.get(
            "uicontroller.remote_mass_delete.warning_message", "PARAM0",
            "PARAM1", "PARAM2", "PARAM3", "PARAM4");
        assertTrue("Param replacement missing: " + text, text
            .contains("PARAM0"));
        assertTrue("Param replacement missing: " + text, text
            .contains("PARAM1"));
        assertTrue("Param replacement missing: " + text, text
            .contains("PARAM2"));
        assertTrue("Param replacement missing: " + text, text
            .contains("PARAM3"));
        assertTrue("Param replacement missing: " + text, text
            .contains("PARAM4"));
    }

    public void testPlaceholders() {
        String text = Translation
            .get("action_login.description");
        assertTrue("Text fail: " + text, text.contains("PowerFolder"));
        assertFalse("Text fail: " + text, text.contains("{PowerFolder}"));
        Translation.setPlaceHolder("APPNAME", "XXX");
        text = Translation.get("action_login.description");
        assertTrue("Text fail: " + text, text.contains("XXX"));
        assertFalse("Text fail: " + text, text.contains("{XXX}"));

        Translation.setPlaceHolder("APPNAME", null);
        text = Translation.get("action_login.description");
        assertTrue("Text fail: " + text, text.contains("{APPNAME}"));
    }
}
