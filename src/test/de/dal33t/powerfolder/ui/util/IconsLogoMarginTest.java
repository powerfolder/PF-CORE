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
package de.dal33t.powerfolder.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/**
 * PFC-3652: the header logo is moved left by its transparent margin - never resized.
 */
public class IconsLogoMarginTest {

    @Test
    public void testTransparentColumnsOnTheLeftAreTheMargin() {
        assertEquals(3, Icons.transparentLeftMargin(logo(10, 3)));
    }

    @Test
    public void testContentAtTheEdgeHasNoMargin() {
        assertEquals(0, Icons.transparentLeftMargin(logo(10, 0)));
    }

    @Test
    public void testFullyTransparentImageHasNoMargin() {
        assertEquals(0, Icons.transparentLeftMargin(new BufferedImage(10, 4, BufferedImage.TYPE_INT_ARGB)));
    }

    @Test
    public void testCropKeepsHeightAndDropsOnlyTheMargin() {
        BufferedImage cropped = Icons.cropLeft(logo(10, 3), 3);
        assertEquals(7, cropped.getWidth());
        assertEquals(4, cropped.getHeight());
        assertEquals(0, Icons.transparentLeftMargin(cropped));
    }

    @Test
    public void testNoCropWithoutMargin() {
        BufferedImage logo = logo(10, 0);
        assertSame(logo, Icons.cropLeft(logo, 0));
        assertSame(logo, Icons.cropLeft(logo, 10));
    }

    @Test
    public void testMasterIsCutByTheSameShareAsTheBase() {
        // The @4x master carries the base's margin four times; cut by the base's share both start at their content.
        BufferedImage base = logo(10, 3);
        BufferedImage master = logo(40, 12);
        int margin = Icons.transparentLeftMargin(base);
        BufferedImage cut = Icons.cropLeft(master, (int) Math.round((double) margin * master.getWidth() / 10));
        assertEquals(28, cut.getWidth());
        assertEquals(0, Icons.transparentLeftMargin(cut));
    }

    /** A {@code width} x 4 logo, transparent in its {@code margin} leftmost columns and opaque after. */
    private static BufferedImage logo(int width, int margin) {
        BufferedImage image = new BufferedImage(width, 4, BufferedImage.TYPE_INT_ARGB);
        for (int x = margin; x < width; x++) {
            for (int y = 0; y < 4; y++) {
                image.setRGB(x, y, 0xFF003269);
            }
        }
        return image;
    }
}
