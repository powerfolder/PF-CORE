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
package de.dal33t.powerfolder.util.os.Win32;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import de.dal33t.powerfolder.util.os.OSUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PFC-3643: give the running client process an explicit Windows Application
 * User Model ID (AUMID).
 * <p>
 * Without an explicit AUMID, Windows derives the taskbar grouping identity from
 * the JVM launcher executable, so the client groups its taskbar button and jump
 * list under the generic Java runtime ("OpenJDK Platform binary") instead of
 * PowerFolder. Setting an explicit AUMID early - before the first window is
 * shown - makes the taskbar button, its grouping and the jump list belong to
 * PowerFolder.
 * <p>
 * Note: this affects the <em>taskbar / shell</em> identity only. The name and
 * icon shown in Task Manager's "Apps" list come from the launcher executable's
 * version resource and icon (branded in the installer build), not from here.
 */
public final class WinAppUserModelID {

    private static final Logger LOG = Logger
        .getLogger(WinAppUserModelID.class.getName());

    /** Windows caps the AUMID at 128 characters and forbids spaces. */
    private static final int MAX_LENGTH = 128;

    private WinAppUserModelID() {
    }

    private interface Shell32 extends StdCallLibrary {
        Shell32 INSTANCE = Native.load("shell32", Shell32.class,
            W32APIOptions.DEFAULT_OPTIONS);

        // HRESULT SetCurrentProcessExplicitAppUserModelID(PCWSTR AppID);
        int SetCurrentProcessExplicitAppUserModelID(WString appID);
    }

    /**
     * Set the explicit AUMID for the current process. No-op off Windows, on a
     * blank id, or on any failure (missing shell32 entry point, JNA not
     * loadable) - this is purely an enhancement and must never be fatal.
     *
     * @param appName
     *     a human app name, e.g. "PowerFolder". Spaces are stripped and the
     *     value is truncated to {@value #MAX_LENGTH} characters to satisfy the
     *     Windows AUMID constraints.
     */
    public static void setForCurrentProcess(String appName) {
        if (!OSUtil.isWindowsSystem()) {
            return;
        }
        String appID = sanitize(appName);
        if (appID.isEmpty()) {
            return;
        }
        try {
            int hr = Shell32.INSTANCE
                .SetCurrentProcessExplicitAppUserModelID(new WString(appID));
            if (hr != 0) {
                LOG.fine("SetCurrentProcessExplicitAppUserModelID('" + appID
                    + "') returned HRESULT 0x" + Integer.toHexString(hr));
            } else {
                LOG.fine("AppUserModelID set to '" + appID + "'");
            }
        } catch (Throwable t) {
            // UnsatisfiedLinkError / NoClassDefFoundError (JNA absent) / etc.
            // Never fatal - the client just keeps the default taskbar identity.
            LOG.log(Level.FINE, "Could not set AppUserModelID: " + t, t);
        }
    }

    private static String sanitize(String appName) {
        if (appName == null) {
            return "";
        }
        String id = appName.replace(" ", "").trim();
        if (id.length() > MAX_LENGTH) {
            id = id.substring(0, MAX_LENGTH);
        }
        return id;
    }
}
