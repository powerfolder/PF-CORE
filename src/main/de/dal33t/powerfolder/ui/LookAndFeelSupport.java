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
 *
 */
package de.dal33t.powerfolder.ui;

import de.dal33t.powerfolder.skin.AbstractSyntheticaSkin;
import de.dal33t.powerfolder.skin.Skin;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;

import javax.swing.*;
import java.awt.Font;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Class which offers several helper methods for handling with LookAndFeels.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class LookAndFeelSupport {

    private static final Logger log = Logger.getLogger(LookAndFeelSupport.class
        .getName());

    private LookAndFeelSupport() {
        // Only static methods available
    }

    /**
     * The skin whose branding (primary/accent colour) drives the FlatLaf accent
     * colour. Set via {@link #setLookAndFeel(LookAndFeel, Skin)}; may be null
     * (then a default accent is used).
     */
    private static Skin activeSkin;

    /**
     * Like {@link #setLookAndFeel(LookAndFeel)} but also records the active skin
     * so the per-brand accent colour is applied to FlatLaf. Call this from the
     * central skin-application path so every branded client gets its accent.
     */
    public static void setLookAndFeel(LookAndFeel laf, Skin skin)
        throws UnsupportedLookAndFeelException
    {
        activeSkin = skin;
        setLookAndFeel(laf);
    }

    /**
     * Sets the look and feel, and sets font so that Asian fonts display okay.
     * See http://www.javasoft.de/jsf/public/products/synthetica/faq#q12
     *
     * @param laf
     * @throws UnsupportedLookAndFeelException
     */
    public static void setLookAndFeel(LookAndFeel laf)
        throws UnsupportedLookAndFeelException
    {
        if (SwingUtilities.isEventDispatchThread()) {
            setLookAndFeelImpl(laf);
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        setLookAndFeelImpl(laf);
                    } catch (UnsupportedLookAndFeelException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warning("Interrupted while setting LookAndFeel");
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException
                    && cause.getCause() instanceof UnsupportedLookAndFeelException) {
                    throw (UnsupportedLookAndFeelException) cause.getCause();
                }
                log.warning("Error setting LookAndFeel: " + cause);
            }
        }
    }

    private static void setLookAndFeelImpl(LookAndFeel laf)
        throws UnsupportedLookAndFeelException
    {
        // PFI-93 UI modernization: use FlatLaf (flat, modern, with built-in light
        // and dark themes) instead of Synthetica. Follow the OS appearance — dark
        // on macOS when the system is in Dark Mode. The passed 'laf' (the legacy
        // Synthetica skin) is intentionally ignored and kept only as a fallback.
        boolean dark = isSystemDarkMode();
        // Per-brand accent colour + a white window/panel background (light mode)
        // applied to FlatLaf before setup so all branded clients keep their
        // identity and the light theme uses a clean white background instead of
        // FlatLaf's default light grey.
        applyThemeDefaults(dark);
        boolean ok = dark
            ? com.formdev.flatlaf.FlatDarkLaf.setup()
            : com.formdev.flatlaf.FlatLightLaf.setup();
        if (!ok) {
            log.warning("FlatLaf setup failed; falling back to legacy look and feel");
            setSyntheticaLicense();
            UIManager.setLookAndFeel(laf);
            SyntheticaLookAndFeel.setFont(getBaseFontName(), 11);
            setSyntheticaLicense();
            return;
        }
        // Base font (covers non-Latin scripts; see getBaseFontName()).
        UIManager.put("defaultFont", new Font(getBaseFontName(), Font.PLAIN, 13));
        log.info("UI look and feel: FlatLaf " + (dark ? "Dark" : "Light"));
        de.dal33t.powerfolder.ui.util.Icons.setDarkMode(dark);
        currentDark = dark;
        startSystemThemeWatcher();
    }

    /**
     * @return {@code true} if the OS is currently in Dark Mode. Detected on
     *         macOS (AppleInterfaceStyle), Windows (AppsUseLightTheme registry
     *         value) and Linux (GNOME/freedesktop {@code color-scheme}). Unknown
     *         setups fall back to the currently applied theme.
     */
    private static boolean isSystemDarkMode() {
        if (OSUtil.isMacOS()) {
            // `defaults read -g AppleInterfaceStyle` prints "Dark" (exit 0) when
            // Dark mode is on, and exits non-zero when the key is absent (light).
            String out = readProcessOutput("defaults", "read", "-g",
                "AppleInterfaceStyle");
            return out != null && out.toLowerCase().contains("dark");
        }
        if (OSUtil.isWindowsSystem()) {
            // AppsUseLightTheme: 0x0 => dark apps, 0x1 => light apps.
            String out = readProcessOutput("reg", "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v", "AppsUseLightTheme");
            if (out != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("AppsUseLightTheme\\s+REG_DWORD\\s+0x0*([0-9a-fA-F]+)")
                    .matcher(out);
                if (m.find()) {
                    return Integer.parseInt(m.group(1), 16) == 0;
                }
            }
            return currentDark;
        }
        // Linux: GNOME 42+/freedesktop expose color-scheme = 'prefer-dark';
        // fall back to the gtk-theme name (often ends in "-dark").
        String scheme = readProcessOutput("gsettings", "get",
            "org.gnome.desktop.interface", "color-scheme");
        if (scheme != null) {
            String v = scheme.toLowerCase();
            if (v.contains("dark")) {
                return true;
            }
            if (v.contains("light") || v.contains("default")) {
                return false;
            }
        }
        String theme = readProcessOutput("gsettings", "get",
            "org.gnome.desktop.interface", "gtk-theme");
        if (theme != null && theme.toLowerCase().contains("dark")) {
            return true;
        }
        return currentDark;
    }

    /**
     * Run a short-lived command and return its (stdout+stderr) output, or
     * {@code null} on error/timeout. Leak-free: the output is fully drained and
     * every stream plus the process itself is always closed/destroyed. (An
     * earlier macOS-only version leaked the process/file descriptors on every
     * poll, which exhausted the JVM after ~20 min and froze the UI.)
     */
    private static String readProcessOutput(String... command) {
        Process p = null;
        try {
            p = new ProcessBuilder(command).redirectErrorStream(true).start();
            String out;
            try (java.io.InputStream in = p.getInputStream()) {
                out = new String(in.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            }
            if (!p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                return null; // don't thrash if it hangs; cleaned up in finally
            }
            return out;
        } catch (Exception e) {
            log.fine("theme detection command failed: " + e);
            return null;
        } finally {
            if (p != null) {
                p.destroy();
                try { p.getOutputStream().close(); } catch (Exception ignore) { }
                try { p.getInputStream().close(); } catch (Exception ignore) { }
                try { p.getErrorStream().close(); } catch (Exception ignore) { }
            }
        }
    }

    /** The dark/light state currently applied to the UI. */
    private static volatile boolean currentDark;
    private static Thread themeWatcher;

    /**
     * Start a background watcher (macOS, Windows and Linux) that flips the FlatLaf
     * theme live when the system appearance changes, so no client restart is
     * needed. The appearance check spawns a short-lived process, so it runs off
     * the EDT; the actual theme switch is applied on the EDT.
     */
    private static synchronized void startSystemThemeWatcher() {
        if (themeWatcher != null) {
            return;
        }
        themeWatcher = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000);
                    boolean dark = isSystemDarkMode();
                    if (dark != currentDark) {
                        SwingUtilities.invokeLater(() -> switchTheme(dark));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Throwable t) {
                    log.fine("theme watcher: " + t);
                }
            }
        }, "system-appearance-watcher");
        themeWatcher.setDaemon(true);
        themeWatcher.start();
    }

    /**
     * Listeners notified after a live light/dark switch, so set-once UI elements
     * (logo, header icons, label colours) can refresh without an app restart.
     */
    private static final java.util.List<Runnable> themeChangeListeners =
        new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * Register a callback fired on the EDT after every live theme switch. Used
     * by {@code MainFrame} (and any window) to re-apply icons/colours that are
     * only set once at build time. Idempotent per instance is the caller's job.
     */
    public static void addThemeChangeListener(Runnable r) {
        if (r != null) {
            themeChangeListeners.add(r);
        }
    }

    /** True if the client is currently showing the dark theme. */
    public static boolean isDarkMode() {
        return currentDark;
    }

    /** Re-install the light/dark FlatLaf theme and refresh all open windows. */
    private static void switchTheme(boolean dark) {
        if (dark == currentDark) {
            return;
        }
        currentDark = dark;
        try {
            applyThemeDefaults(dark);
            boolean ok = dark
                ? com.formdev.flatlaf.FlatDarkLaf.setup()
                : com.formdev.flatlaf.FlatLightLaf.setup();
            if (ok) {
                UIManager.put("defaultFont", new Font(getBaseFontName(), Font.PLAIN, 13));
                de.dal33t.powerfolder.ui.util.Icons.setDarkMode(dark);
                com.formdev.flatlaf.FlatLaf.updateUI();
                // Refresh set-once elements (logo, header icons, label colours)
                // that FlatLaf.updateUI() alone does not re-run.
                for (Runnable r : themeChangeListeners) {
                    try {
                        r.run();
                    } catch (Throwable t) {
                        log.fine("theme change listener failed: " + t);
                    }
                }
                log.info("Switched FlatLaf theme to " + (dark ? "Dark" : "Light")
                    + " (system appearance changed)");
            }
        } catch (Throwable t) {
            log.warning("live theme switch failed: " + t);
        }
    }

    /** Default accent (PowerFolder/origin primary colour) when no brand colour is found. */
    private static final String DEFAULT_ACCENT = "#34495c";

    /**
     * FlatLaf keys forced to white in light mode so the whole window (incl. the
     * header/status panels, menubar, tabs) is white, not FlatLaf's default grey.
     * Removed in dark mode so FlatDark's own backgrounds apply.
     */
    private static final String[] BACKGROUND_KEYS = {
        "@background", "Panel.background", "TabbedPane.background",
        "Viewport.background", "ScrollPane.background", "MenuBar.background",
        "ToolBar.background", "OptionPane.background", "RootPane.background",
        "SplitPane.background"
    };

    /**
     * Bright text colour used in dark mode. FlatDark's default foreground is a
     * dimmed grey (~#bbbbbb); this raises it to a crisp near-white so labels,
     * menus and lists read clearly on the dark background. Tune here if needed.
     */
    private static final String DARK_FOREGROUND = "#f5f5f5";

    /**
     * FlatLaf keys set to {@link #DARK_FOREGROUND} in dark mode so text is bright
     * white. Removed in light mode so FlatLight's own dark text applies.
     */
    private static final String[] FOREGROUND_KEYS = {
        "@foreground", "Label.foreground"
    };

    /**
     * Set FlatLaf's global {@code @accentColor} to the active brand's primary
     * colour so selection/focus/accents match the brand in both light and dark.
     */
    private static void applyThemeDefaults(boolean dark) {
        try {
            String hex = accentColorHex(activeSkin);
            // getGlobalExtraDefaults() returns null when nothing has been set yet
            // (the first startup call), and new HashMap<>(null) throws an NPE -
            // which was silently swallowed below, so the brand accent and the
            // white light-mode background never got applied. Default to an empty
            // map when none exists yet.
            java.util.Map<String, String> existing =
                com.formdev.flatlaf.FlatLaf.getGlobalExtraDefaults();
            java.util.Map<String, String> extra = existing != null
                ? new java.util.HashMap<>(existing)
                : new java.util.HashMap<>();
            extra.put("@accentColor", hex);
            // Light mode: force a clean white background on the window and all
            // container surfaces (FlatLaf's light theme uses light grey for panels/
            // menubar/etc., which left the header grey). Dark mode: remove the
            // overrides so FlatDark's dark backgrounds apply.
            for (String key : BACKGROUND_KEYS) {
                if (dark) {
                    extra.remove(key);
                } else {
                    extra.put(key, "#ffffff");
                }
            }
            // Dark mode: brighten the default text foreground to near-white.
            // Light mode: drop the override so FlatLight's dark text applies.
            for (String key : FOREGROUND_KEYS) {
                if (dark) {
                    extra.put(key, DARK_FOREGROUND);
                } else {
                    extra.remove(key);
                }
            }
            com.formdev.flatlaf.FlatLaf.setGlobalExtraDefaults(extra);
            log.fine("FlatLaf accent=" + hex + " background=" + (dark ? "dark" : "#ffffff"));
        } catch (Throwable t) {
            log.fine("could not apply theme defaults: " + t);
        }
    }

    /**
     * Extract the brand's primary colour from the active skin's synth.xml
     * (the {@code <color type="BACKGROUND" value="#..."/> <!-- Primary color -->}
     * entry). Falls back to {@link #DEFAULT_ACCENT}.
     */
    private static String accentColorHex(Skin skin) {
        if (!(skin instanceof AbstractSyntheticaSkin)) {
            return DEFAULT_ACCENT;
        }
        try {
            String resource = ((AbstractSyntheticaSkin) skin)
                .getDefaultSynthXMLPath().toString().replace('\\', '/');
            try (java.io.InputStream in =
                     LookAndFeelSupport.class.getResourceAsStream(resource)) {
                if (in == null) {
                    return DEFAULT_ACCENT;
                }
                String xml = new String(in.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "BACKGROUND\"\\s+value=\"(#[0-9a-fA-F]{6})\"\\s*/>\\s*<!--\\s*Primary color",
                    java.util.regex.Pattern.CASE_INSENSITIVE).matcher(xml);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (Throwable t) {
            log.fine("accent extraction failed: " + t);
        }
        return DEFAULT_ACCENT;
    }

    /**
     * The default logical "Dialog" font has no glyphs for some scripts (e.g.
     * Devanagari for Hindi or Thai), so text in those languages would render as
     * empty boxes. For those UI languages we pick a platform font that actually
     * covers the script (and Latin), falling back to "Dialog" for every other
     * language or when no suitable font is installed. The base font is applied
     * once at startup; changing the language requires a restart anyway.
     *
     * @return the base font family name to hand to Synthetica.
     */
    private static String getBaseFontName() {
        Locale locale = Translation.getActiveLocale();
        return baseFontNameForLanguage(locale != null ? locale.getLanguage() : "");
    }

    /**
     * Returns a base font family able to render the given language's script.
     * "Dialog" (the default) has no glyphs for some scripts (e.g. Devanagari
     * for Hindi or Thai); for those we pick a platform font that covers the
     * script, with cross-platform fallbacks. Returns "Dialog" for every other
     * language or when no suitable font is installed.
     *
     * @param language an ISO 639 language code (e.g. "hi", "th"), never null.
     * @return the font family name.
     */
    static String baseFontNameForLanguage(String language) {
        switch (language) {
            case "hi" : // Hindi, Devanagari script
                // Sample "radd" (U+0930 U+0926 U+094D U+0926)
                return firstFontThatDisplays(
                    codePoints(0x0930, 0x0926, 0x094D, 0x0926),
                    "Nirmala UI", "Mangal", "Kohinoor Devanagari",
                    "Devanagari MT", "Noto Sans Devanagari", "Lohit Devanagari");
            case "th" : // Thai script
                // Sample "yok loek" (U+0E22 U+0E01 U+0E40 U+0E25 U+0E34 U+0E01)
                return firstFontThatDisplays(
                    codePoints(0x0E22, 0x0E01, 0x0E40, 0x0E25, 0x0E34, 0x0E01),
                    "Leelawadee UI", "Tahoma", "Thonburi",
                    "Noto Sans Thai", "Loma");
            default :
                return "Dialog";
        }
    }

    /**
     * Ensures {@code text} can be rendered for the given {@code locale}. If the
     * supplied {@code current} font already covers every character, it is
     * returned unchanged; otherwise a font of the same style and size from a
     * script-capable family (see {@link #baseFontNameForLanguage(String)}) is
     * returned. Used by cell renderers that show many languages, each in its
     * own script, at the same time (e.g. the language chooser).
     *
     * @param current the font currently in use (may be null).
     * @param text    the text to be displayed.
     * @param locale  the locale whose script {@code text} belongs to.
     * @return a font able to display {@code text}, or {@code current}.
     */
    public static Font fontFor(Font current, String text, Locale locale) {
        if (current != null && current.canDisplayUpTo(text) == -1) {
            return current;
        }
        String family = baseFontNameForLanguage(
            locale != null ? locale.getLanguage() : "");
        if ("Dialog".equals(family)) {
            return current;
        }
        int style = current != null ? current.getStyle() : Font.PLAIN;
        int size = current != null ? current.getSize() : 11;
        return new Font(family, style, size);
    }

    /** Builds a String from Unicode code points (keeps this source ASCII-only). */
    private static String codePoints(int... cps) {
        return new String(cps, 0, cps.length);
    }

    /**
     * Returns the first of the given font families that is actually installed
     * and can display every character of the sample, or "Dialog" if none can.
     */
    private static String firstFontThatDisplays(String sample, String... families)
    {
        for (String family : families) {
            Font font = new Font(family, Font.PLAIN, 11);
            // new Font(name) silently substitutes when the family is missing,
            // so require the resolved family to match and to cover the sample.
            if (font.getFamily().equalsIgnoreCase(family)
                && font.canDisplayUpTo(sample) == -1)
            {
                return family;
            }
        }
        log.warning("No script-capable font installed for language '"
            + Translation.getActiveLocale()
            + "'; falling back to Dialog. Text may not render correctly.");
        return "Dialog";
    }

    public static final void setSyntheticaLicense() {
        String[] li = {"Licensee=PowerFolder", "LicenseRegistrationNumber=235363175", "Product=Synthetica",
                "LicenseType=Small Business License", "ExpireDate=--.--.----", "MaxVersion=2.32.999"};
        UIManager.put("Synthetica.license.info", li);
        UIManager.put("Synthetica.license.key", "2DFF8275-2F698B64-21153E8E-B71AD474");

        String[] li2 = {"Licensee=PowerFolder", "LicenseRegistrationNumber=235363175", "Product=SyntheticaAddons",
                "LicenseType=Small Business License", "ExpireDate=--.--.----", "MaxVersion=1.13.999"};
        UIManager.put("SyntheticaAddons.license.info", li2);
        UIManager.put("SyntheticaAddons.license.key", "832A83CE-BBFAC5B8-69F31D8E-9463FC1D");
    }
}
