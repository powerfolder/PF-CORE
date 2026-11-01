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

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * PFC-3643: modern, monochrome, vector line-icons for the main-window action
 * column (Pause/Play, Preferences, Create Folder, Close).
 * <p>
 * They are painted in the owning component's <b>foreground</b> colour, so they
 * are automatically black in light mode and white in dark mode (matching the
 * folder icons) and stay razor-sharp at any HiDPI scale (pure vector, no
 * bitmaps).
 */
public final class ActionIcons {

    public enum Type {
        PAUSE, PLAY, PREFERENCES, CREATE_FOLDER, CLOSE
    }

    private ActionIcons() {
    }

    /**
     * @param type the glyph.
     * @param size icon size in px (drawn on a 22-unit grid, scaled to fit).
     * @return a dark-mode-aware vector icon.
     */
    public static Icon get(final Type type, final int size) {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }

            @Override
            public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                    g.translate(x, y);
                    double s = size / 22.0;
                    g.scale(s, s);
                    Color col = c != null ? c.getForeground() : Color.BLACK;
                    g.setColor(col);
                    // PFC-3643 feedback: thin, light stroke - the bottom-bar glyphs
                    // should not look bold.
                    g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                    paint(type, g);
                } finally {
                    g.dispose();
                }
            }
        };
    }

    // All glyphs drawn on a 22x22 grid in the current colour.
    private static void paint(Type type, Graphics2D g) {
        switch (type) {
            case PAUSE :
                // PFC-3643 feedback: thin bars so Pause is not bold.
                g.fillRect(6, 3, 3, 16);
                g.fillRect(13, 3, 3, 16);
                break;
            case PLAY :
                java.awt.geom.Path2D.Double tri = new java.awt.geom.Path2D.Double();
                tri.moveTo(6, 3);
                tri.lineTo(6, 19);
                tri.lineTo(18, 11);
                tri.closePath();
                g.fill(tri);
                break;
            case PREFERENCES :
                gear(g);
                break;
            case CREATE_FOLDER :
                folder(g, true);
                break;
            case CLOSE :
                g.draw(new Line2D.Double(5, 5, 17, 17));
                g.draw(new Line2D.Double(17, 5, 5, 17));
                break;
            default :
                break;
        }
    }

    private static void folder(Graphics2D g, boolean plus) {
        g.draw(new RoundRectangle2D.Double(1, 6, 20, 13, 3, 3));
        GeneralPath p = new GeneralPath();
        p.moveTo(1, 8);
        p.lineTo(3, 4);
        p.lineTo(10, 4);
        p.lineTo(12, 7);
        g.draw(p);
        if (plus) {
            g.draw(new Line2D.Double(11, 9, 11, 16));
            g.draw(new Line2D.Double(7.5, 12.5, 14.5, 12.5));
        }
    }

    private static void gear(Graphics2D g) {
        // PFC-3643: a modern filled gear with rounded teeth and a hollow centre.
        double cx = 11, cy = 11;
        double body = 6.0;      // filled body radius
        int teeth = 8;
        double toothW = 3.2;    // tooth width
        double toothLen = 3.2;  // how far a tooth sticks out past the body
        double hole = 2.4;      // centre hole radius
        java.awt.geom.Area gear = new java.awt.geom.Area(
            new Ellipse2D.Double(cx - body, cy - body, 2 * body, 2 * body));
        for (int i = 0; i < teeth; i++) {
            double a = Math.PI * 2 * i / teeth;
            // A rounded-rect tooth on the radial axis, overlapping the body so it
            // merges cleanly, then rotated into place.
            java.awt.geom.RoundRectangle2D.Double tooth =
                new java.awt.geom.RoundRectangle2D.Double(-toothW / 2,
                    -(body + toothLen), toothW, toothLen + 1.6,
                    toothW * 0.55, toothW * 0.55);
            java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
            at.translate(cx, cy);
            at.rotate(a);
            java.awt.geom.Area ta = new java.awt.geom.Area(tooth);
            ta.transform(at);
            gear.add(ta);
        }
        gear.subtract(new java.awt.geom.Area(
            new Ellipse2D.Double(cx - hole, cy - hole, 2 * hole, 2 * hole)));
        // PFC-3643 feedback: draw the gear as a thin outline (like Create-Folder) instead of a solid fill.
        g.draw(gear);
    }
}
