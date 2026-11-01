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

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;

import java.util.List;

/**
 * The {@code org.kde.StatusNotifierItem} interface we export (PFC-3096) so the
 * desktop shell shows a tray icon on GNOME (with the AppIndicator extension) and
 * KDE, where the legacy AWT {@link java.awt.SystemTray} is unavailable.
 * <p>
 * Only the methods and signals the host actually invokes/consumes are declared.
 * The item's <em>properties</em> (Category, Id, Title, Status, IconName, ToolTip,
 * Menu, ItemIsMenu) are served through the standard {@code org.freedesktop.DBus.Properties}
 * interface implemented alongside this one (see {@code SniTray}).
 *
 * @see <a href="https://www.freedesktop.org/wiki/Specifications/StatusNotifierItem/">
 *      StatusNotifierItem specification</a>
 */
@DBusInterfaceName("org.kde.StatusNotifierItem")
public interface StatusNotifierItemDef extends DBusInterface {

    /** Right-click / context-menu request at screen coordinates. */
    void ContextMenu(int x, int y);

    /** Primary activation (usually left-click). */
    void Activate(int x, int y);

    /** Secondary activation (usually middle-click). */
    void SecondaryActivate(int x, int y);

    /** Mouse-wheel over the icon. */
    void Scroll(int delta, String orientation);

    /**
     * The tooltip changed; hosts re-read the {@code ToolTip} property. Carries no
     * arguments per the specification.
     */
    class NewToolTip extends DBusSignal {
        public NewToolTip(String path) throws DBusException {
            super(path);
        }
    }

    /**
     * The icon changed; hosts re-read {@code IconName}/{@code IconPixmap}. Carries
     * no arguments per the specification.
     */
    class NewIcon extends DBusSignal {
        public NewIcon(String path) throws DBusException {
            super(path);
        }
    }

    /**
     * The item's {@code Status} changed. The new status is delivered as the single
     * signal argument (and is also readable as the {@code Status} property).
     */
    class NewStatus extends DBusSignal {
        private final String status;

        public NewStatus(String path, String status) throws DBusException {
            super(path, status);
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }

    /**
     * The {@code ToolTip} property struct: {@code (s a(iiay) s s)} =
     * (icon-name, icon-pixmaps, title, description). We always send an empty
     * pixmap list and rely on {@code iconName}/{@code title}.
     */
    class ToolTip extends Struct {
        @Position(0)
        private final String iconName;
        @Position(1)
        private final List<Pixmap> iconPixmap;
        @Position(2)
        private final String title;
        @Position(3)
        private final String description;

        public ToolTip(String iconName, List<Pixmap> iconPixmap, String title,
            String description)
        {
            this.iconName = iconName;
            this.iconPixmap = iconPixmap;
            this.title = title;
            this.description = description;
        }
    }

    /**
     * A single ARGB32 icon pixmap: {@code (iiay)} = (width, height, bytes). Kept
     * for spec-completeness of the {@code ToolTip} struct; we currently always
     * pass an empty pixmap list (icon is supplied by name).
     */
    class Pixmap extends Struct {
        @Position(0)
        private final int width;
        @Position(1)
        private final int height;
        @Position(2)
        private final byte[] data;

        public Pixmap(int width, int height, byte[] data) {
            this.width = width;
            this.height = height;
            this.data = data;
        }
    }
}
