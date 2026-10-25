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
import de.dal33t.powerfolder.util.UIThemeState;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;

import javax.swing.*;
import java.awt.Color;
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
        // Single source of truth, also read by non-ui code (e.g. the log DocumentHandler).
        UIThemeState.setDark(dark);
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
            return isWindowsDarkMode();
        }
        return isLinuxDarkMode();
    }

    private static final String WINDOWS_PERSONALIZE_KEY =
        "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";

    /** AppsUseLightTheme (0 = dark apps, 1 = light apps), read in-process via JNA instead of spawning reg.exe. */
    private static boolean isWindowsDarkMode() {
        try {
            if (com.sun.jna.platform.win32.Advapi32Util.registryValueExists(
                com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER, WINDOWS_PERSONALIZE_KEY, "AppsUseLightTheme"))
            {
                return com.sun.jna.platform.win32.Advapi32Util.registryGetIntValue(
                    com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER, WINDOWS_PERSONALIZE_KEY,
                    "AppsUseLightTheme") == 0;
            }
        } catch (Throwable t) {
            log.fine("AppsUseLightTheme: " + t);
        }
        return UIThemeState.isDark();
    }

    /**
     * Linux dark-mode probes, tried in order: the desktop-agnostic freedesktop
     * portal, then GNOME (gsettings), KDE Plasma (kreadconfig), XFCE (xfconf-query),
     * and finally the GTK theme name. Each returns {@code null} when it cannot answer.
     */
    private static final java.util.List<java.util.function.Supplier<Boolean>> LINUX_PROBES = java.util.List.of(
        LookAndFeelSupport::probePortal, LookAndFeelSupport::probeGnomeColorScheme,
        () -> probeKde("kreadconfig6"), () -> probeKde("kreadconfig5"), LookAndFeelSupport::probeXfce,
        LookAndFeelSupport::probeGtkTheme);

    /** Index of the first Linux probe that answered, or -1; only that one is queried while it keeps answering. */
    private static volatile int linuxProbe = -1;

    /** Dark-mode detection across the common Linux desktops; unknown setups fall back to the current theme. */
    private static boolean isLinuxDarkMode() {
        int known = linuxProbe;
        if (known >= 0) {
            Boolean dark = LINUX_PROBES.get(known).get();
            if (dark != null) {
                return dark;
            }
            linuxProbe = -1;
        }
        for (int i = 0; i < LINUX_PROBES.size(); i++) {
            Boolean dark = LINUX_PROBES.get(i).get();
            if (dark != null) {
                linuxProbe = i;
                return dark;
            }
        }
        return UIThemeState.isDark();
    }

    /** freedesktop portal color-scheme: 0 = no preference, 1 = prefer dark, 2 = prefer light. */
    private static Boolean probePortal() {
        String portal = readProcessOutput("gdbus", "call", "--session",
            "--dest", "org.freedesktop.portal.Desktop",
            "--object-path", "/org/freedesktop/portal/desktop",
            "--method", "org.freedesktop.portal.Settings.Read",
            "org.freedesktop.appearance", "color-scheme");
        if (portal != null) {
            if (portal.contains("uint32 1")) {
                return true;
            }
            if (portal.contains("uint32 2") || portal.contains("uint32 0")) {
                return false;
            }
        }
        return null;
    }

    /** GNOME: color-scheme = 'prefer-dark'. */
    private static Boolean probeGnomeColorScheme() {
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
        return null;
    }

    /** KDE Plasma: the active ColorScheme name (e.g. "BreezeDark"). */
    private static Boolean probeKde(String tool) {
        String kde = readProcessOutput(tool, "--group", "General", "--key", "ColorScheme");
        return kde != null && !kde.trim().isEmpty() ? kde.toLowerCase().contains("dark") : null;
    }

    /** XFCE: the GTK theme name from xsettings. */
    private static Boolean probeXfce() {
        String xfce = readProcessOutput("xfconf-query", "-c", "xsettings", "-p", "/Net/ThemeName");
        return xfce != null && !xfce.trim().isEmpty() ? xfce.toLowerCase().contains("dark") : null;
    }

    /** Last resort: the GNOME gtk-theme name (often ends in "-dark"). */
    private static Boolean probeGtkTheme() {
        String theme = readProcessOutput("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme");
        return theme != null && theme.toLowerCase().contains("dark") ? Boolean.TRUE : null;
    }

    /** Run a short-lived command and return its (stdout+stderr) output, or {@code null} on error/timeout. */
    private static String readProcessOutput(String... command) {
        return de.dal33t.powerfolder.util.ProcessUtils.runBounded(2, command);
    }

    /** How often the OS appearance is checked; a change only triggers a restart prompt, so this need not be fast. */
    private static final long THEME_POLL_MILLIS = 15_000;

    private static Thread themeWatcher;
    /** The running controller, so the watcher can offer a restart on change. */
    private static volatile de.dal33t.powerfolder.Controller themeController;
    /** True once the restart prompt was shown for the current mismatch. */
    private static volatile boolean restartPrompted;

    /**
     * Register the controller so the system-appearance watcher can prompt for a
     * restart when the OS light/dark setting changes. Safe to call more than once.
     */
    public static void setThemeController(de.dal33t.powerfolder.Controller controller) {
        themeController = controller;
    }

    /**
     * Start a background watcher (macOS, Windows and Linux) that detects OS
     * light/dark changes and offers the user a restart to apply the new theme.
     * Live switching is intentionally avoided: repeatedly re-theming the running
     * UI (FlatLaf.updateUI()) froze the client after a few switches. The
     * appearance check may spawn a short-lived process (macOS, Linux), so it runs
     * off the EDT; the restart prompt is shown on the EDT.
     */
    private static synchronized void startSystemThemeWatcher() {
        if (themeWatcher != null) {
            return;
        }
        themeWatcher = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(THEME_POLL_MILLIS);
                    boolean dark = isSystemDarkMode();
                    if (dark == UIThemeState.isDark()) {
                        // Back in sync with the applied theme; allow prompting
                        // again on a future change.
                        restartPrompted = false;
                    } else if (!restartPrompted) {
                        // Appearance changed. We do NOT live-switch (repeated
                        // FlatLaf.updateUI() froze the UI after a few switches);
                        // instead offer a restart, once per change.
                        restartPrompted = true;
                        final boolean target = dark;
                        SwingUtilities.invokeLater(
                            () -> promptRestartForThemeChange(target));
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

    /** True if the client is currently showing the dark theme. */
    public static boolean isDarkMode() {
        return UIThemeState.isDark();
    }

    /**
     * The OS light/dark setting changed. Instead of re-theming the running UI
     * (which froze the client after a few switches), offer the user a restart so
     * the new theme is applied cleanly at startup. If declined, the current theme
     * stays until the next (manual) restart. Runs on the EDT.
     */
    private static void promptRestartForThemeChange(boolean dark) {
        de.dal33t.powerfolder.Controller c = themeController;
        if (c == null || !c.isUIOpen()) {
            return; // no UI to prompt / restart yet
        }
        log.info("System appearance changed to " + (dark ? "Dark" : "Light")
            + "; offering restart to apply the new theme.");
        try {
            int choice = de.dal33t.powerfolder.ui.dialog.DialogFactory.genericDialog(
                c,
                Translation.get("preferences.dialog.restart.title"),
                Translation.get("preferences.dialog.restart.text"),
                new String[] {
                    Translation.get("preferences.dialog.restart.restart"),
                    Translation.get("general.cancel") },
                0,
                de.dal33t.powerfolder.ui.dialog.GenericDialogType.QUESTION);
            if (choice == 0) {
                c.shutdownAndRequestRestart();
            }
        } catch (Throwable t) {
            log.warning("theme-change restart prompt failed: " + t);
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

    /** {@link UIThemeState#DARK_FOREGROUND} as a FlatLaf hex value. */
    private static final String DARK_FOREGROUND =
        String.format("#%06x", UIThemeState.DARK_FOREGROUND.getRGB() & 0xFFFFFF);

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
            String hex = accentHex();
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
     * The brand primary is the BACKGROUND colour tagged with the "Primary color" comment in the skin's synth.xml.
     * Both attribute orders and flexible whitespace match, so a harmless reformat does not drop the accent; the
     * comment marker disambiguates (there are several BACKGROUND entries).
     */
    private static final java.util.regex.Pattern[] PRIMARY_COLOR_PATTERNS = {
        java.util.regex.Pattern.compile(
            "type=\"BACKGROUND\"[^>]*?value=\"(#[0-9a-fA-F]{6})\"[^>]*?/>\\s*<!--\\s*Primary color",
            java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile(
            "value=\"(#[0-9a-fA-F]{6})\"[^>]*?type=\"BACKGROUND\"[^>]*?/>\\s*<!--\\s*Primary color",
            java.util.regex.Pattern.CASE_INSENSITIVE)};

    /** The skin {@link #accentHex} was resolved for, and the result; resolved once per skin. */
    private static Skin accentSkin;
    private static String accentHex;

    /**
     * PFC-3643: the resolved brand accent colour - the active skin's primary colour,
     * or {@link #DEFAULT_ACCENT} when none is found. Single source of truth for the
     * avatar disc ({@code AvatarLabel}), the account-header status glyphs
     * ({@code Icons}) and the splash loading bar ({@code SplashScreen}), so they all
     * match the FlatLaf {@code @accentColor}. Cached per active skin, so cheap on paint paths.
     */
    public static Color getAccentColor() {
        try {
            return Color.decode(accentHex());
        } catch (RuntimeException ex) {
            return Color.decode(DEFAULT_ACCENT);
        }
    }

    /** @return the accent hex of the active skin, read from its synth.xml only when the skin changed. */
    private static synchronized String accentHex() {
        Skin skin = activeSkin;
        if (accentHex == null || skin != accentSkin) {
            accentHex = accentColorHex(skin);
            accentSkin = skin;
        }
        return accentHex;
    }

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
                String hex = primaryColorHex(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                if (hex != null) {
                    return hex;
                }
                // A branded skin with no recognisable primary colour is a real
                // problem (the UI silently falls back to the generic accent), so make
                // it visible instead of hiding it at fine level.
                log.warning(skin.getName()
                    + ": no 'Primary color' BACKGROUND entry in " + resource
                    + " - falling back to the default accent " + DEFAULT_ACCENT);
            }
        } catch (Throwable t) {
            log.fine("accent extraction failed: " + t);
        }
        return DEFAULT_ACCENT;
    }

    /** @return the "Primary color" BACKGROUND hex of a synth.xml, or {@code null} if it has none. */
    static String primaryColorHex(String synthXml) {
        for (java.util.regex.Pattern pattern : PRIMARY_COLOR_PATTERNS) {
            java.util.regex.Matcher m = pattern.matcher(synthXml);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
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
