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
import de.dal33t.powerfolder.util.JavaVersion;

import javax.swing.*;

import java.net.MalformedURLException;
import java.net.URL;

public class DownloadApplet extends JApplet {

    /** Minimum Java version for Win32withoutJava is 1.6.0_6 */
    private static final JavaVersion MIN_JAVA_VERSION =
            new JavaVersion(1, 6, 0, 6);

    public void start() {

        String os = System.getProperty("os.name");
        // Default
        String javascript = "WebStart";
        try {
            if (os != null && os.startsWith("Windows")) {
                // Windows (Java)
                javascript = "Win32withJava";

                // Is current version >= minimum version?
                if (JavaVersion.systemVersion().compareTo(MIN_JAVA_VERSION) >= 0)
                {
                    // Windows (with Java)
                    javascript = "Win32withoutJava";
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        try {
            getAppletContext().showDocument(
                new URL("javascript:selectDownload(\"" + javascript + "\")"));
        } catch (MalformedURLException me) {
            // Hmmmmm.
        }
    }
}
