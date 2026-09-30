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

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PFC-3643: a circular account avatar for the main-window header.
 * <p>
 * Shows the account's initials on a brand-coloured disc immediately, and - as a
 * pure enhancement - asynchronously loads the real avatar image from the server
 * ({@code ServerClient.getAvatarURL(...)}) off the EDT and, if it arrives, draws
 * it circle-cropped. Any failure (offline, no avatar, error) simply keeps the
 * initials, so it never blocks startup or shows a blank/hanging avatar.
 */
public class AvatarLabel extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(AvatarLabel.class.getName());
    private static final Color BRAND = new Color(0x34495c);

    private final int size;
    private String initials = "";
    private volatile BufferedImage avatar;
    /** Guards against a stale async load overwriting a newer one. */
    private volatile Object loadToken;

    public AvatarLabel() {
        this(46);
    }

    public AvatarLabel(int size) {
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setOpaque(false);
    }

    /** Set the display name; the first letters of up to two words are shown. */
    public void setName(String name) {
        this.initials = initialsFor(name);
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

    /** @see #loadAvatar(String, String) with no auth token. */
    public void loadAvatar(final String url) {
        loadAvatar(url, null);
    }

    /**
     * Asynchronously load the avatar image from {@code url}. No-op on a blank
     * URL; failures are swallowed (initials remain).
     * <p>
     * The server avatar endpoint ({@code /avatars/user/...}) is registered
     * "unsecured" but its handler still requires an authenticated caller for
     * non-public avatars, so a bare request gets a 403 ("Can't get input stream
     * from URL"). We therefore authenticate with the device token via the
     * {@code Authorization: Bearer <token>} header (see server WebSession), which
     * is the same scheme the REST API uses.
     *
     * @param url         the avatar image URL (from ServerClient.getAvatarURL).
     * @param bearerToken the device token (ServerClient.getDeviceToken()), or
     *                    {@code null}/blank for an unauthenticated request.
     */
    public void loadAvatar(final String url, final String bearerToken) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        final Object token = new Object();
        loadToken = token;
        Thread t = new Thread(() -> {
            try {
                BufferedImage img = fetch(url, bearerToken);
                if (img != null && loadToken == token) {
                    avatar = img;
                    SwingUtilities.invokeLater(this::repaint);
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "Could not load avatar from " + url + ": " + e);
            }
        }, "avatar-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Fetch and decode the avatar, authenticating with a Bearer token. Returns
     * {@code null} (not an exception) on any non-200 response - e.g. 403 (no/
     * invalid auth) or 404 (account has no avatar) - so the caller keeps the
     * initials fallback.
     */
    private static BufferedImage fetch(String url, String bearerToken)
        throws IOException
    {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        try {
            con.setRequestMethod("GET");
            con.setConnectTimeout(10_000);
            con.setReadTimeout(10_000);
            con.setInstanceFollowRedirects(true);
            if (bearerToken != null && !bearerToken.trim().isEmpty()) {
                con.setRequestProperty("Authorization", "Bearer " + bearerToken);
            }
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            try (InputStream in = con.getInputStream()) {
                return javax.imageio.ImageIO.read(in);
            }
        } finally {
            con.disconnect();
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            Ellipse2D circle = new Ellipse2D.Double(0, 0, size, size);
            BufferedImage img = avatar;
            if (img != null) {
                g.setClip(circle);
                g.drawImage(img, 0, 0, size, size, null);
            } else {
                g.setColor(BRAND);
                g.fill(circle);
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, Math.round(size * 0.36f)));
                int tw = g.getFontMetrics().stringWidth(initials);
                int asc = g.getFontMetrics().getAscent();
                int th = g.getFontMetrics().getHeight();
                g.drawString(initials, (size - tw) / 2f,
                    (size - th) / 2f + asc);
            }
        } finally {
            g.dispose();
        }
    }
}
