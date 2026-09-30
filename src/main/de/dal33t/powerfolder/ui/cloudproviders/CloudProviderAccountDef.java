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
 * Marker for the freedesktop {@code org.freedesktop.CloudProviders.Account}
 * interface (PFC-3643, Linux file-manager sync integration).
 * <p>
 * Read-only properties: {@code Name} (s), {@code Path} (s), {@code Icon} (s),
 * {@code Status} (i, see the CloudProvidersAccountStatus enum:
 * 0=invalid, 1=idle, 2=syncing, 3=error) and {@code StatusDetails} (s). They are
 * served through {@code org.freedesktop.DBus.Properties} and delivered via
 * {@code ObjectManager.GetManagedObjects}, so no methods are declared here.
 *
 * @see <a href="https://gitlab.gnome.org/GNOME/libcloudproviders">libcloudproviders</a>
 */
@DBusInterfaceName("org.freedesktop.CloudProviders.Account")
public interface CloudProviderAccountDef extends DBusInterface {
}
