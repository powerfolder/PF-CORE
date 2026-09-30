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
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import java.util.List;
import java.util.Map;

/**
 * The {@code com.canonical.dbusmenu} interface we export (PFC-3096) to provide
 * the tray icon's menu. GNOME's AppIndicator extension does not deliver a
 * left-click {@code Activate}; it opens this menu, so the menu is what makes the
 * tray usable there. KDE consumes it too.
 * <p>
 * Only the subset hosts actually call is declared: {@link #GetLayout}, the
 * property accessors {@link #GetGroupProperties}/{@link #GetProperty}, the
 * {@link #Event} click callback and {@link #AboutToShow}. The menu's own
 * properties (Version, Status, TextDirection) are served via the standard
 * {@code org.freedesktop.DBus.Properties} interface (see {@code SniTray}).
 *
 * @see <a href="https://github.com/AyatanaIndicators/libdbusmenu">com.canonical.dbusmenu</a>
 */
@DBusInterfaceName("com.canonical.dbusmenu")
public interface DBusMenuDef extends DBusInterface {

    /**
     * Return the (sub)menu layout below {@code parentId}.
     *
     * @param parentId       root of the requested layout (0 = whole menu).
     * @param recursionDepth -1 = all levels.
     * @param propertyNames  requested item properties (empty = all).
     * @return a {@code (u(ia{sv}av))} tuple of (layout revision, root item).
     */
    LayoutTuple GetLayout(int parentId, int recursionDepth,
        List<String> propertyNames);

    /**
     * Return properties for a set of item ids: {@code a(ia{sv})}.
     */
    List<ItemProperties> GetGroupProperties(List<Integer> ids,
        List<String> propertyNames);

    /**
     * Return a single property of a single item.
     */
    Variant<?> GetProperty(int id, String name);

    /**
     * A menu event happened (the important one is {@code eventId == "clicked"}).
     */
    void Event(int id, String eventId, Variant<?> data, UInt32 timestamp);

    /**
     * The host is about to show the (sub)menu {@code id}.
     *
     * @return {@code true} if the layout needs updating before display.
     */
    boolean AboutToShow(int id);

    /**
     * A recursive menu item: {@code (ia{sv}av)} = (id, properties,
     * children-as-variants). Each child variant wraps another {@link Layout}.
     */
    class Layout extends Struct {
        @Position(0)
        private final int id;
        @Position(1)
        private final Map<String, Variant<?>> properties;
        @Position(2)
        private final List<Variant<?>> children;

        public Layout(int id, Map<String, Variant<?>> properties,
            List<Variant<?>> children)
        {
            this.id = id;
            this.properties = properties;
            this.children = children;
        }
    }

    /**
     * The two out-arguments of {@link #GetLayout}: {@code (u (ia{sv}av))} =
     * (revision, root layout).
     */
    class LayoutTuple extends Tuple {
        @Position(0)
        private final UInt32 revision;
        @Position(1)
        private final Layout layout;

        public LayoutTuple(UInt32 revision, Layout layout) {
            this.revision = revision;
            this.layout = layout;
        }
    }

    /**
     * Per-item properties entry for {@link #GetGroupProperties}: {@code (ia{sv})}.
     */
    class ItemProperties extends Struct {
        @Position(0)
        private final int id;
        @Position(1)
        private final Map<String, Variant<?>> properties;

        public ItemProperties(int id, Map<String, Variant<?>> properties) {
            this.id = id;
            this.properties = properties;
        }
    }

    /**
     * Emitted when the menu layout changed so hosts re-fetch it via
     * {@link #GetLayout}. Arguments: (revision, updated parent id).
     */
    class LayoutUpdated extends DBusSignal {
        private final UInt32 revision;
        private final int parent;

        public LayoutUpdated(String path, UInt32 revision, int parent)
            throws DBusException
        {
            super(path, revision, parent);
            this.revision = revision;
            this.parent = parent;
        }
    }
}
