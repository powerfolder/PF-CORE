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
package de.dal33t.powerfolder.ui.cloudproviders;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * Marker for the freedesktop {@code org.freedesktop.CloudProviders.Provider}
 * interface (PFC-3643, Linux file-manager sync integration).
 * <p>
 * The interface only has read-only properties ({@code Name}); they are served
 * through {@code org.freedesktop.DBus.Properties} and delivered to file managers
 * via {@code org.freedesktop.DBus.ObjectManager.GetManagedObjects}, so no methods
 * are declared here.
 *
 * @see <a href="https://gitlab.gnome.org/GNOME/libcloudproviders">libcloudproviders</a>
 */
@DBusInterfaceName("org.freedesktop.CloudProviders.Provider")
public interface CloudProviderProviderDef extends DBusInterface {
}
