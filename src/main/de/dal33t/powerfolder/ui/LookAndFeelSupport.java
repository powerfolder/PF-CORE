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

import de.dal33t.powerfolder.util.Translation;
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
        setSyntheticaLicense();
        UIManager.setLookAndFeel(laf);
        SyntheticaLookAndFeel.setFont(getBaseFontName(), 11);
        setSyntheticaLicense();
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
        String language = locale != null ? locale.getLanguage() : "";
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
