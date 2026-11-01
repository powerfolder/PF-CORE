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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.Test;

/**
 * PFC-3652: the loading bar of the splash stays visible on the dark track.
 */
public class SplashScreenContrastTest {

    private static final Color DARK_TRACK = new Color(0x2B2B2B);

    @Test
    public void testBlackOnWhiteIsTheMaximum() {
        assertEquals(21.0, SplashScreen.contrast(Color.BLACK, Color.WHITE), 0.01);
        assertEquals(1.0, SplashScreen.contrast(DARK_TRACK, DARK_TRACK), 0.01);
    }

    @Test
    public void testDarkBrandAccentsVanishOnTheDarkTrack() {
        assertTrue(SplashScreen.contrast(new Color(0x003269), DARK_TRACK) < 3, "FAUbox navy");
        assertTrue(SplashScreen.contrast(new Color(0x34495c), DARK_TRACK) < 3, "default accent");
    }

    @Test
    public void testTheDarkModeNeutralStandsOutOnTheDarkTrack() {
        assertTrue(SplashScreen.contrast(new Color(0xA9B8C8), DARK_TRACK) >= 3);
        assertTrue(SplashScreen.contrast(new Color(0xF59425), DARK_TRACK) >= 3, "Moser orange keeps its colour");
    }
}
