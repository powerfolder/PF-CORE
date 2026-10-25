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
package de.dal33t.powerfolder.ui.widget;

import de.dal33t.powerfolder.ui.LookAndFeelSupport;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * PFC-3643: a circular account avatar for the main-window header. Paints the account image circle-cropped, or the
 * account's initials on an accent-coloured disc while no image is set. Loading the image is up to the caller
 * ({@code ServerClient.loadAvatar(...)}).
 */
public class AvatarLabel extends JComponent {

    private static final long serialVersionUID = 1L;

    private final int size;
    private String initials = "";
    private volatile BufferedImage avatar;

    public AvatarLabel(int size) {
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setOpaque(false);
        setFocusable(true);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    /** Set the display name; the first letters of up to two words are shown and it is the accessible name. */
    @Override
    public void setName(String name) {
        this.initials = initialsFor(name);
        getAccessibleContext().setAccessibleName(name);
        repaint();
    }

    static String initialsFor(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.toString();
    }

    /** Show {@code image} circle-cropped; {@code null} shows the initials. May be called from any thread. */
    public void setAvatar(BufferedImage image) {
        avatar = image;
        SwingUtilities.invokeLater(this::repaint);
    }

    /** Drop any image and show the initials fallback again. */
    public void clearAvatar() {
        setAvatar(null);
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                private static final long serialVersionUID = 1L;

                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.PUSH_BUTTON;
                }
            };
        }
        return accessibleContext;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            Ellipse2D circle = new Ellipse2D.Double(0, 0, size, size);
            BufferedImage img = avatar;
            if (img != null) {
                Graphics2D gi = (Graphics2D) g.create();
                gi.setClip(circle);
                gi.drawImage(img, 0, 0, size, size, null);
                gi.dispose();
            } else {
                g.setColor(LookAndFeelSupport.getAccentColor());
                g.fill(circle);
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, Math.round(size * 0.36f)));
                int tw = g.getFontMetrics().stringWidth(initials);
                int asc = g.getFontMetrics().getAscent();
                int th = g.getFontMetrics().getHeight();
                g.drawString(initials, (size - tw) / 2f, (size - th) / 2f + asc);
            }
            if (isFocusOwner()) {
                // Keyboard focus ring inside the disc edge.
                Color focus = UIManager.getColor("Component.focusColor");
                g.setColor(focus != null ? focus : Color.GRAY);
                g.setStroke(new BasicStroke(2f));
                g.draw(new Ellipse2D.Double(1, 1, size - 2, size - 2));
            }
        } finally {
            g.dispose();
        }
    }
}
