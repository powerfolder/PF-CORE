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
package de.dal33t.powerfolder.ui.tray.sni;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * Remote proxy for the freedesktop/KDE StatusNotifierWatcher (PFC-3096).
 * <p>
 * This is the D-Bus service ({@code org.kde.StatusNotifierWatcher}) that hosts
 * (KDE Plasma natively, GNOME via the AppIndicator/KStatusNotifier extension)
 * expose so applications can register their StatusNotifierItem tray icon. We
 * only ever call {@link #RegisterStatusNotifierItem(String)}; the rest of the
 * interface (properties, signals) is not needed on the client side.
 *
 * @see <a href="https://www.freedesktop.org/wiki/Specifications/StatusNotifierItem/">
 *      StatusNotifierItem specification</a>
 */
@DBusInterfaceName("org.kde.StatusNotifierWatcher")
public interface StatusNotifierWatcher extends DBusInterface {

    /**
     * Register a StatusNotifierItem with the watcher.
     *
     * @param service the well-known or unique bus name under which our
     *                StatusNotifierItem is exported.
     */
    void RegisterStatusNotifierItem(String service);
}
