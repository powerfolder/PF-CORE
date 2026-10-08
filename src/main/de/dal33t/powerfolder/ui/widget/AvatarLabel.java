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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /** @see #loadAvatar(String, String) with no authorization. */
    public void loadAvatar(final String url) {
        loadAvatar(url, (String) null);
    }

    /**
     * Asynchronously load the avatar image from {@code url}. No-op on a blank
     * URL; failures are swallowed (initials remain).
     * <p>
     * The server avatar endpoint ({@code /avatars/<oid>}) is "unsecured" but its
     * handler still needs an authenticated caller for non-public avatars (a bare
     * request gets 403). We therefore send an {@code Authorization} header - a
     * device-token {@code Bearer}, or, when the client has no token, HTTP
     * {@code Basic} login credentials (the server's web session layer accepts both;
     * see {@code ServerClient.getWebAuthorizationHeader()}).
     *
     * @param url           the avatar image URL (from ServerClient.getAvatarURL).
     * @param authorization the full {@code Authorization} header value
     *                      (ServerClient.getWebAuthorizationHeader()), or
     *                      {@code null}/blank for an unauthenticated request.
     */
    public void loadAvatar(final String url, final String authorization) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        final Object token = new Object();
        loadToken = token;
        Thread t = new Thread(() -> {
            try {
                BufferedImage img = fetch(url, authorization);
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
     * Like {@link #loadAvatar(String, String)}, but the {@code Authorization} header
     * is produced by {@code authSupplier} ON the loader thread - use this when
     * obtaining the header may be slow (e.g. it mints a device token), so it never
     * runs on the EDT.
     */
    public void loadAvatar(final String url,
        final java.util.function.Supplier<String> authSupplier)
    {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        final Object token = new Object();
        loadToken = token;
        Thread t = new Thread(() -> {
            try {
                String authorization = authSupplier != null
                    ? authSupplier.get() : null;
                BufferedImage img = fetch(url, authorization);
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
     * Fetch and decode the avatar, sending the given {@code Authorization} header.
     * Returns {@code null} (not an exception) on any non-200 response - e.g. 403
     * (no/invalid auth) or 404 (account has no avatar) - so the caller keeps the
     * initials fallback.
     */
    /**
     * PFC-3643: also carry the token as a {@code ?Token=} URL parameter (the scheme
     * the Android client uses and the server honours on the unsecured /avatars path),
     * in addition to the Authorization header. Some servers authenticate the param
     * but not the Bearer header on that path, which otherwise 403s.
     */
    private static String withTokenParam(String url, String authorization) {
        if (url != null && authorization != null
            && authorization.startsWith("Bearer "))
        {
            String tok = authorization.substring("Bearer ".length()).trim();
            if (!tok.isEmpty()) {
                String sep = url.contains("?") ? "&" : "?";
                return url + sep + "Token="
                    + java.net.URLEncoder.encode(tok, StandardCharsets.UTF_8);
            }
        }
        return url;
    }

    private static BufferedImage fetch(String url, String authorization)
        throws IOException
    {
        HttpURLConnection con = (HttpURLConnection) new URL(
            withTokenParam(url, authorization)).openConnection();
        try {
            con.setRequestMethod("GET");
            con.setConnectTimeout(10_000);
            con.setReadTimeout(10_000);
            con.setInstanceFollowRedirects(true);
            if (authorization != null && !authorization.trim().isEmpty()) {
                con.setRequestProperty("Authorization", authorization);
            }
            int code = con.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                // Make the (otherwise silent) failure diagnosable: 404 = no avatar
                // stored for the account; 401/403 = not authenticated/allowed;
                // 5xx = server error.
                LOG.info("Avatar not loaded: HTTP " + code + " for " + url
                    + (authorization == null || authorization.trim().isEmpty()
                        ? " (no auth header sent)" : " (auth header sent)"));
                return null;
            }
            try (InputStream in = con.getInputStream()) {
                return javax.imageio.ImageIO.read(in);
            }
        } finally {
            con.disconnect();
        }
    }

    /** Drop any loaded image and show the initials fallback again. */
    public void clearAvatar() {
        // Invalidate any in-flight load so it cannot re-populate the image.
        loadToken = new Object();
        avatar = null;
        SwingUtilities.invokeLater(this::repaint);
    }

    /**
     * Upload {@code image} as the account avatar via a multipart POST to the
     * avatar endpoint (form field {@code "file"} - the name the server's
     * ThumbnailServlet expects), authenticated with the device token.
     *
     * @param url           the avatar endpoint, e.g. {@code /avatars/<oid>} (no
     *                      {@code thumbnail} query - from getAvatarURL(info, false)).
     * @param authorization the full {@code Authorization} header value
     *                      (ServerClient.getWebAuthorizationHeader()).
     * @param image         the local image file to upload.
     * @return {@code true} on a 2xx response (the server replies 201 CREATED).
     */
    public static boolean upload(String url, String authorization, Path image)
        throws IOException
    {
        String boundary = "PFAvatarBoundary" + System.nanoTime();
        HttpURLConnection con = (HttpURLConnection) new URL(withTokenParam(url, authorization)).openConnection();
        try {
            con.setRequestMethod("POST");
            con.setConnectTimeout(15_000);
            con.setReadTimeout(30_000);
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);
            if (authorization != null && !authorization.trim().isEmpty()) {
                con.setRequestProperty("Authorization", authorization);
            }
            String fileName = image.getFileName().toString();
            String contentType = Files.probeContentType(image);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            try (OutputStream out = con.getOutputStream()) {
                String header = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\""
                    + fileName + "\"\r\n"
                    + "Content-Type: " + contentType + "\r\n\r\n";
                out.write(header.getBytes(StandardCharsets.UTF_8));
                Files.copy(image, out);
                out.write(("\r\n--" + boundary + "--\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            }
            int code = con.getResponseCode();
            return code >= 200 && code < 300;
        } finally {
            con.disconnect();
        }
    }

    /**
     * DELETE the account avatar on the server.
     *
     * @return {@code true} on a 2xx response.
     */
    public static boolean delete(String url, String authorization)
        throws IOException
    {
        HttpURLConnection con = (HttpURLConnection) new URL(withTokenParam(url, authorization)).openConnection();
        try {
            con.setRequestMethod("DELETE");
            con.setConnectTimeout(15_000);
            con.setReadTimeout(15_000);
            if (authorization != null && !authorization.trim().isEmpty()) {
                con.setRequestProperty("Authorization", authorization);
            }
            int code = con.getResponseCode();
            return code >= 200 && code < 300;
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
