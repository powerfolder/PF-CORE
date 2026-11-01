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
package de.dal33t.powerfolder.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Color;

import org.junit.jupiter.api.Test;

import de.dal33t.powerfolder.util.UIThemeState;

/**
 * Test for the brand accent extraction and the dark-mode state of {@link LookAndFeelSupport}.
 */
public class LookAndFeelSupportTest {

    @Test
    public void testPrimaryColorTypeBeforeValue() {
        String xml = "<color type=\"BACKGROUND\" value=\"#112233\"/>\n"
            + "<color type=\"BACKGROUND\" value=\"#A1B2C3\" /> <!-- Primary color -->\n";
        assertEquals("#A1B2C3", LookAndFeelSupport.primaryColorHex(xml));
    }

    @Test
    public void testPrimaryColorValueBeforeType() {
        String xml = "<color value=\"#0a0b0c\"\n    type=\"BACKGROUND\"/>\n\t<!--   Primary color -->";
        assertEquals("#0a0b0c", LookAndFeelSupport.primaryColorHex(xml));
    }

    @Test
    public void testPrimaryColorMissingMarker() {
        String xml = "<color type=\"BACKGROUND\" value=\"#112233\"/> <!-- Secondary color -->";
        assertNull(LookAndFeelSupport.primaryColorHex(xml));
    }

    @Test
    public void testAccentColorIsCachedAndDefaultsWithoutSkin() {
        Color first = LookAndFeelSupport.getAccentColor();
        assertEquals(Color.decode("#34495c"), first);
        assertEquals(first, LookAndFeelSupport.getAccentColor());
    }

    @Test
    public void testIsDarkModeFollowsUIThemeState() {
        boolean before = UIThemeState.isDark();
        try {
            UIThemeState.setDark(true);
            assertSame(Boolean.TRUE, LookAndFeelSupport.isDarkMode());
            UIThemeState.setDark(false);
            assertSame(Boolean.FALSE, LookAndFeelSupport.isDarkMode());
        } finally {
            UIThemeState.setDark(before);
        }
    }
}
