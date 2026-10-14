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
package de.dal33t.powerfolder.ui.tray;

import de.dal33t.powerfolder.ui.tray.sni.DBusMenuDef;
import de.dal33t.powerfolder.ui.tray.sni.StatusNotifierItemDef;
import de.dal33t.powerfolder.ui.tray.sni.StatusNotifierWatcher;
import de.dal33t.powerfolder.util.os.OSUtil;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A StatusNotifierItem (SNI) system-tray icon over D-Bus (PFC-3096).
 * <p>
 * Modern GNOME removed the legacy XEmbed notification area, so
 * {@link java.awt.SystemTray} reports unsupported and the client had no tray
 * icon, no minimize-to-tray and (consequently) exited on window close. This
 * class provides the tray via the freedesktop/KDE StatusNotifierItem +
 * {@code com.canonical.dbusmenu} protocol, which KDE Plasma supports natively
 * and GNOME supports through the widely deployed AppIndicator/KStatusNotifier
 * shell extension.
 * <p>
 * The implementation is pure Java (dbus-java with the JDK Unix-domain-socket
 * transport) - no native libraries, so nothing platform-specific has to be built
 * or bundled per distribution/architecture.
 * <p>
 * <b>Threading:</b> incoming D-Bus method calls (menu clicks, icon activation)
 * arrive on dbus-java worker threads; menu {@link Action}s are dispatched to the
 * EDT before running so callers can touch Swing safely.
 */
public final class SniTray {

    private static final Logger LOG = Logger.getLogger(SniTray.class.getName());

    /** Well-known D-Bus name of the host-side watcher we register with. */
    private static final String WATCHER_NAME = "org.kde.StatusNotifierWatcher";
    private static final String WATCHER_PATH = "/StatusNotifierWatcher";
    private static final String ITEM_PATH = "/StatusNotifierItem";
    private static final String MENU_PATH = "/MenuBar";

    /** A single click callback for a menu entry. */
    public interface Action {
        void run();
    }

    /** One menu entry (or separator) in the tray popup. */
    private static final class Entry {
        final int id;
        String label;
        final boolean separator;
        boolean enabled = true;
        final Action action;

        Entry(int id, String label, boolean separator, Action action) {
            this.id = id;
            this.label = label;
            this.separator = separator;
            this.action = action;
        }
    }

    private final String appId;
    private final String iconName;
    private final String title;
    private volatile String toolTipText;

    private final List<Entry> entries = new ArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final AtomicInteger menuRevision = new AtomicInteger(1);

    private DBusConnection connection;
    private String busName;
    private ItemImpl item;
    private MenuImpl menu;
    private Action activateAction;

    /**
     * @param appId    application id (used as the SNI Id and menu grouping); the
     *                 installed themed icon should carry this name.
     * @param iconName freedesktop icon-theme name to display (typically the same
     *                 as {@code appId}; the client installs
     *                 {@code /usr/share/icons/hicolor/128x128/apps/<name>.png}).
     * @param title    human-readable application title (tooltip fallback).
     */
    public SniTray(String appId, String iconName, String title) {
        this.appId = appId;
        this.iconName = iconName;
        this.title = title;
        this.toolTipText = title;
    }

    /**
     * @return {@code true} if a StatusNotifierWatcher is present on the current
     *         Linux session bus (KDE, or GNOME with the AppIndicator extension),
     *         i.e. registering an SNI tray icon can succeed.
     */
    public static boolean isAvailable() {
        if (!OSUtil.isLinux()) {
            return false;
        }
        DBusConnection probe = null;
        try {
            probe = DBusConnectionBuilder.forSessionBus().build();
            DBus bus = probe.getRemoteObject("org.freedesktop.DBus",
                "/org/freedesktop/DBus", DBus.class);
            boolean present = bus.NameHasOwner(WATCHER_NAME);
            LOG.fine("StatusNotifierWatcher present on session bus: " + present);
            return present;
        } catch (Throwable t) {
            LOG.log(Level.FINE, "SNI tray not available: " + t, t);
            return false;
        } finally {
            closeQuietly(probe);
        }
    }

    /** Set the callback run on primary activation (left-click) of the icon. */
    public void setActivateAction(Action a) {
        this.activateAction = a;
    }

    /** Add a clickable menu entry. Returns its id (for later label updates). */
    public int addItem(String label, Action action) {
        Entry e = new Entry(nextId.getAndIncrement(), label, false, action);
        entries.add(e);
        return e.id;
    }

    /** Add a separator line to the menu. */
    public void addSeparator() {
        entries.add(new Entry(nextId.getAndIncrement(), null, true, null));
    }

    /** Update an entry's label and notify the host to re-read the menu. */
    public void updateItemLabel(int id, String label) {
        for (Entry e : entries) {
            if (e.id == id) {
                e.label = label;
                break;
            }
        }
        signalMenuLayoutUpdated();
    }

    /** Update an entry's enabled state and notify the host. */
    public void setItemEnabled(int id, boolean enabled) {
        for (Entry e : entries) {
            if (e.id == id) {
                e.enabled = enabled;
                break;
            }
        }
        signalMenuLayoutUpdated();
    }

    /** Change the tooltip text and notify the host to re-read it. */
    public void setToolTip(String text) {
        this.toolTipText = text != null ? text : "";
        if (connection != null) {
            try {
                connection.sendMessage(
                    new StatusNotifierItemDef.NewToolTip(ITEM_PATH));
            } catch (Exception ex) {
                LOG.log(Level.FINE, "NewToolTip signal failed: " + ex, ex);
            }
        }
    }

    /**
     * Connect to the session bus, export the item + menu and register with the
     * watcher. Call once, after all menu entries have been added.
     *
     * @throws Exception if the connection, export or registration fails.
     */
    public void show() throws Exception {
        connection = DBusConnectionBuilder.forSessionBus().build();
        busName = "org.kde.StatusNotifierItem-" + ProcessHandle.current().pid()
            + "-1";
        connection.requestBusName(busName);

        item = new ItemImpl();
        menu = new MenuImpl();
        connection.exportObject(item);
        connection.exportObject(menu);

        StatusNotifierWatcher watcher = connection.getRemoteObject(WATCHER_NAME,
            WATCHER_PATH, StatusNotifierWatcher.class);
        watcher.RegisterStatusNotifierItem(busName);
        LOG.info("Registered StatusNotifierItem tray icon '" + busName + "'");
    }

    /** Unregister and disconnect. Safe to call more than once. */
    public void dispose() {
        try {
            if (connection != null) {
                if (item != null) {
                    connection.unExportObject(ITEM_PATH);
                }
                if (menu != null) {
                    connection.unExportObject(MENU_PATH);
                }
                if (busName != null) {
                    try {
                        connection.releaseBusName(busName);
                    } catch (Exception ignore) {
                        // best effort
                    }
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.FINE, "SNI dispose: " + ex, ex);
        } finally {
            closeQuietly(connection);
            connection = null;
        }
    }

    private void signalMenuLayoutUpdated() {
        if (connection != null) {
            try {
                connection.sendMessage(new DBusMenuDef.LayoutUpdated(MENU_PATH,
                    new UInt32(menuRevision.incrementAndGet()), 0));
            } catch (Exception ex) {
                LOG.log(Level.FINE, "LayoutUpdated signal failed: " + ex, ex);
            }
        }
    }

    private static void runOnEdt(Action a) {
        if (a == null) {
            return;
        }
        SwingUtilities.invokeLater(a::run);
    }

    private static void closeQuietly(DBusConnection c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignore) {
                // ignored
            }
        }
    }

    // ------------------------------------------------------------------
    // Exported: org.kde.StatusNotifierItem (+ org.freedesktop.DBus.Properties)
    // ------------------------------------------------------------------
    private final class ItemImpl implements StatusNotifierItemDef, Properties {

        @Override
        public String getObjectPath() {
            return ITEM_PATH;
        }

        @Override
        public void ContextMenu(int x, int y) {
            // The menu is exposed via the Menu property; hosts render it
            // themselves. Nothing to do here.
        }

        @Override
        public void Activate(int x, int y) {
            runOnEdt(activateAction);
        }

        @Override
        public void SecondaryActivate(int x, int y) {
            runOnEdt(activateAction);
        }

        @Override
        public void Scroll(int delta, String orientation) {
            // no-op
        }

        private Map<String, Object> rawProperties() {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("Category", "ApplicationStatus");
            p.put("Id", appId);
            p.put("Title", title);
            p.put("Status", "Active");
            p.put("IconName", iconName);
            p.put("ToolTip", new ToolTip(iconName,
                Collections.<Pixmap> emptyList(), title, toolTipText));
            p.put("Menu", new DBusPath(MENU_PATH));
            p.put("ItemIsMenu", Boolean.FALSE);
            return p;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <A> A Get(String interfaceName, String propertyName) {
            return (A) rawProperties().get(propertyName);
        }

        @Override
        public <A> void Set(String interfaceName, String propertyName, A value) {
            // read-only
        }

        @Override
        public Map<String, Variant<?>> GetAll(String interfaceName) {
            Map<String, Variant<?>> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : rawProperties().entrySet()) {
                out.put(e.getKey(), new Variant<>(e.getValue()));
            }
            return out;
        }
    }

    // ------------------------------------------------------------------
    // Exported: com.canonical.dbusmenu (+ org.freedesktop.DBus.Properties)
    // ------------------------------------------------------------------
    private final class MenuImpl implements DBusMenuDef, Properties {

        @Override
        public String getObjectPath() {
            return MENU_PATH;
        }

        private Map<String, Variant<?>> propsFor(Entry e) {
            Map<String, Variant<?>> m = new LinkedHashMap<>();
            if (e.separator) {
                m.put("type", new Variant<>("separator"));
            } else {
                m.put("label", new Variant<>(e.label != null ? e.label : ""));
                m.put("enabled", new Variant<>(e.enabled));
                m.put("visible", new Variant<>(Boolean.TRUE));
            }
            return m;
        }

        @Override
        public LayoutTuple GetLayout(int parentId, int recursionDepth,
            List<String> propertyNames)
        {
            List<Variant<?>> children = new ArrayList<>();
            for (Entry e : entries) {
                Layout child = new Layout(e.id, propsFor(e),
                    Collections.<Variant<?>> emptyList());
                children.add(new Variant<>(child));
            }
            Map<String, Variant<?>> rootProps = new LinkedHashMap<>();
            rootProps.put("children-display", new Variant<>("submenu"));
            Layout root = new Layout(0, rootProps, children);
            return new LayoutTuple(new UInt32(menuRevision.get()), root);
        }

        @Override
        public List<ItemProperties> GetGroupProperties(List<Integer> ids,
            List<String> propertyNames)
        {
            List<ItemProperties> out = new ArrayList<>();
            for (Entry e : entries) {
                if (ids == null || ids.isEmpty() || ids.contains(e.id)) {
                    out.add(new ItemProperties(e.id, propsFor(e)));
                }
            }
            return out;
        }

        @Override
        public Variant<?> GetProperty(int id, String name) {
            for (Entry e : entries) {
                if (e.id == id) {
                    Variant<?> v = propsFor(e).get(name);
                    if (v != null) {
                        return v;
                    }
                }
            }
            return new Variant<>("");
        }

        @Override
        public void Event(int id, String eventId, Variant<?> data,
            UInt32 timestamp)
        {
            if (!"clicked".equals(eventId)) {
                return;
            }
            for (Entry e : entries) {
                if (e.id == id && e.action != null) {
                    runOnEdt(e.action);
                    return;
                }
            }
        }

        @Override
        public boolean AboutToShow(int id) {
            // Layout is always current; no update needed before showing.
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <A> A Get(String interfaceName, String propertyName) {
            return (A) menuRawProperties().get(propertyName);
        }

        @Override
        public <A> void Set(String interfaceName, String propertyName, A value) {
            // read-only
        }

        @Override
        public Map<String, Variant<?>> GetAll(String interfaceName) {
            Map<String, Variant<?>> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : menuRawProperties().entrySet()) {
                out.put(e.getKey(), new Variant<>(e.getValue()));
            }
            return out;
        }

        private Map<String, Object> menuRawProperties() {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("Version", new UInt32(3));
            p.put("Status", "normal");
            p.put("TextDirection", "ltr");
            return p;
        }
    }
}
