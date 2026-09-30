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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.os.OSUtil;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Linux file-manager sync-status integration via the freedesktop
 * <b>libcloudproviders</b> D-Bus protocol (PFC-3643).
 * <p>
 * Modern GNOME Files (Nautilus), KDE Dolphin and elementary Files show a
 * cloud-provider entry with a live sync status (idle / syncing / error) when an
 * application exports the {@code org.freedesktop.CloudProviders.*} interfaces.
 * PowerFolder had no Linux file-manager integration at all (the liferay-nativity
 * shell overlays are Windows/macOS only and need a native Nautilus extension we
 * do not ship). This exposes the PowerFolders base directory as a cloud provider
 * account so its sync status appears in the file manager sidebar.
 * <p>
 * Implementation is pure Java (dbus-java on the session bus) - no native
 * libraries. We:
 * <ol>
 *   <li>own a session-bus name and export an {@link ObjectManager} at our base
 *       object path, whose managed objects are one Provider object and one
 *       Account object (= the PowerFolders base dir);</li>
 *   <li>write a provider registration file to
 *       {@code $XDG_DATA_HOME/cloud-providers/} so file managers discover us;</li>
 *   <li>recompute the account {@code Status} periodically from the controller and
 *       emit {@code PropertiesChanged} so the file manager updates live.</li>
 * </ol>
 * Note: per-file emblem overlays are out of scope of libcloudproviders (that is a
 * separate, file-manager-specific mechanism); this provides the account-level
 * status the modern freedesktop stack is built around.
 */
public final class CloudProvidersIntegration {

    private static final Logger LOG = Logger
        .getLogger(CloudProvidersIntegration.class.getName());

    // CloudProvidersAccountStatus (see libcloudproviders cloudprovidersaccount.h).
    // Package-visible so unit tests can assert the exact enum values.
    static final int STATUS_INVALID = 0;
    static final int STATUS_IDLE = 1;
    static final int STATUS_SYNCING = 2;
    static final int STATUS_ERROR = 3;

    static final String PROVIDER_IFACE =
        "org.freedesktop.CloudProviders.Provider";
    static final String ACCOUNT_IFACE =
        "org.freedesktop.CloudProviders.Account";

    static final String BUS_NAME = "de.dal33t.powerfolder.CloudProvider";
    static final String BASE_PATH = "/de/dal33t/powerfolder/CloudProvider";
    static final String PROVIDER_PATH = BASE_PATH + "/Provider";
    static final String ACCOUNT_PATH = BASE_PATH + "/account1";

    private final Controller controller;
    private final String providerName;
    private final String accountName;
    private final String basePath;
    private final String iconName;

    private volatile int status = STATUS_INVALID;
    private volatile String statusDetails = "";

    private DBusConnection connection;
    private ScheduledExecutorService scheduler;
    private Path providerFile;

    /**
     * @param controller   the running controller (for sync-state queries).
     * @param providerName human-readable product/provider name.
     * @param basePath     absolute PowerFolders base directory.
     * @param iconName     freedesktop icon-theme name for the account entry.
     */
    public CloudProvidersIntegration(Controller controller, String providerName,
        String basePath, String iconName)
    {
        this.controller = controller;
        this.providerName = providerName;
        this.accountName = providerName;
        this.basePath = basePath;
        this.iconName = iconName;
    }

    /** @return {@code true} on Linux, where this integration applies. */
    public static boolean isApplicable() {
        return OSUtil.isLinux();
    }

    /**
     * Connect to the session bus, export the provider/account objects, register
     * the provider file and start the status updater.
     *
     * @return {@code true} if the integration started.
     */
    public boolean start() {
        try {
            connection = DBusConnectionBuilder.forSessionBus().build();
            connection.requestBusName(BUS_NAME);
            connection.exportObject(new ManagerImpl());
            connection.exportObject(new ProviderImpl());
            connection.exportObject(new AccountImpl());
            writeProviderFile();
            status = computeStatus();
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cloudproviders-status");
                t.setDaemon(true);
                return t;
            });
            // Poll the overall sync state and push changes to the file manager.
            scheduler.scheduleWithFixedDelay(this::refresh, 5, 5,
                TimeUnit.SECONDS);
            LOG.info("Registered libcloudproviders account for " + basePath);
            return true;
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Could not start cloud providers integration: "
                + t, t);
            dispose();
            return false;
        }
    }

    /** Recompute the sync status and, if it changed, notify the file manager. */
    private void refresh() {
        try {
            int newStatus = computeStatus();
            if (newStatus != status) {
                status = newStatus;
                emitAccountChanged();
            }
        } catch (Throwable t) {
            LOG.log(Level.FINE, "cloud providers refresh failed: " + t, t);
        }
    }

    /**
     * Map the controller's overall state to a CloudProvidersAccountStatus:
     * not connected -&gt; invalid; any folder below 100% -&gt; syncing; else idle.
     */
    private int computeStatus() {
        if (controller == null || controller.getOSClient() == null
            || !controller.getOSClient().isConnected())
        {
            return STATUS_INVALID;
        }
        boolean anyFolderSyncing = false;
        for (Folder folder : controller.getFolderRepository().getFolders()) {
            double p = folder.getStatistic().getHarmonizedSyncPercentage();
            if (p >= 0 && p < 100.0d) {
                anyFolderSyncing = true;
                break;
            }
        }
        return statusFor(true, anyFolderSyncing);
    }

    /**
     * Pure sync-state mapping (unit-testable, no controller/D-Bus needed).
     *
     * @param connected        whether the online-storage client is connected.
     * @param anyFolderSyncing whether at least one folder is below 100%.
     * @return the CloudProvidersAccountStatus value.
     */
    static int statusFor(boolean connected, boolean anyFolderSyncing) {
        if (!connected) {
            return STATUS_INVALID;
        }
        return anyFolderSyncing ? STATUS_SYNCING : STATUS_IDLE;
    }

    private void emitAccountChanged() {
        if (connection == null) {
            return;
        }
        try {
            Map<String, Variant<?>> changed = new LinkedHashMap<>();
            changed.put("Status", new Variant<>(Integer.valueOf(status)));
            changed.put("StatusDetails", new Variant<>(statusDetails));
            connection.sendMessage(new Properties.PropertiesChanged(ACCOUNT_PATH,
                ACCOUNT_IFACE, changed, Collections.<String> emptyList()));
        } catch (Exception e) {
            LOG.log(Level.FINE, "PropertiesChanged failed: " + e, e);
        }
    }

    /**
     * Write the provider registration key file so file managers discover us:
     * {@code $XDG_DATA_HOME/cloud-providers/powerfolder.ini} (default
     * {@code ~/.local/share/cloud-providers/}).
     */
    private void writeProviderFile() throws IOException {
        String xdg = System.getenv("XDG_DATA_HOME");
        Path dataHome = (xdg != null && !xdg.trim().isEmpty())
            ? Paths.get(xdg)
            : Paths.get(System.getProperty("user.home"), ".local", "share");
        Path dir = dataHome.resolve("cloud-providers");
        Files.createDirectories(dir);
        providerFile = dir.resolve("powerfolder.ini");
        Files.write(providerFile,
            providerFileContent(BUS_NAME, BASE_PATH)
                .getBytes(StandardCharsets.UTF_8));
        LOG.fine("Wrote cloud provider file: " + providerFile);
    }

    /**
     * Build the libcloudproviders registration key-file body (unit-testable).
     *
     * @param busName    the D-Bus name we own.
     * @param objectPath the ObjectManager base path.
     * @return the {@code [Cloud Providers]} key-file content.
     */
    static String providerFileContent(String busName, String objectPath) {
        return "[Cloud Providers]\n"
            + "BusName=" + busName + "\n"
            + "ObjectPath=" + objectPath + "\n";
    }

    /** Stop the updater, unexport, release the name and remove the provider file. */
    public void dispose() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        try {
            if (connection != null) {
                connection.unExportObject(ACCOUNT_PATH);
                connection.unExportObject(PROVIDER_PATH);
                connection.unExportObject(BASE_PATH);
                try {
                    connection.releaseBusName(BUS_NAME);
                } catch (Exception ignore) {
                    // best effort
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "cloud providers dispose: " + e, e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignore) {
                    // ignored
                }
                connection = null;
            }
        }
        if (providerFile != null) {
            try {
                Files.deleteIfExists(providerFile);
            } catch (Exception ignore) {
                // ignored
            }
            providerFile = null;
        }
    }

    private Map<String, Variant<?>> providerProperties() {
        return providerProperties(providerName);
    }

    private Map<String, Variant<?>> accountProperties() {
        return accountProperties(accountName, basePath, iconName, status,
            statusDetails);
    }

    /** Provider D-Bus properties (unit-testable): {@code Name} only. */
    static Map<String, Variant<?>> providerProperties(String name) {
        Map<String, Variant<?>> p = new LinkedHashMap<>();
        p.put("Name", new Variant<>(name));
        return p;
    }

    /**
     * Account D-Bus properties (unit-testable): Name, Path, Icon, Status (i),
     * StatusDetails. Status is wrapped as an Integer so it marshals as D-Bus "i".
     */
    static Map<String, Variant<?>> accountProperties(String name, String path,
        String icon, int status, String details)
    {
        Map<String, Variant<?>> p = new LinkedHashMap<>();
        p.put("Name", new Variant<>(name));
        p.put("Path", new Variant<>(path));
        p.put("Icon", new Variant<>(icon));
        p.put("Status", new Variant<>(Integer.valueOf(status)));
        p.put("StatusDetails", new Variant<>(details));
        return p;
    }

    /**
     * The {@code ObjectManager.GetManagedObjects} map (unit-testable): the
     * Provider object and the Account object, each keyed by its D-Bus path then
     * interface name then property map.
     */
    static Map<DBusPath, Map<String, Map<String, Variant<?>>>> managedObjects(
        String name, String path, String icon, int status, String details)
    {
        Map<DBusPath, Map<String, Map<String, Variant<?>>>> out =
            new LinkedHashMap<>();

        Map<String, Map<String, Variant<?>>> provider = new LinkedHashMap<>();
        provider.put(PROVIDER_IFACE, providerProperties(name));
        out.put(new DBusPath(PROVIDER_PATH), provider);

        Map<String, Map<String, Variant<?>>> account = new LinkedHashMap<>();
        account.put(ACCOUNT_IFACE,
            accountProperties(name, path, icon, status, details));
        out.put(new DBusPath(ACCOUNT_PATH), account);

        return out;
    }

    // ------------------------------------------------------------------
    // Exported: org.freedesktop.DBus.ObjectManager (base path)
    // ------------------------------------------------------------------
    private final class ManagerImpl implements ObjectManager {

        @Override
        public String getObjectPath() {
            return BASE_PATH;
        }

        @Override
        public Map<DBusPath, Map<String, Map<String, Variant<?>>>>
            GetManagedObjects()
        {
            return managedObjects(accountName, basePath, iconName, status,
                statusDetails);
        }
    }

    // ------------------------------------------------------------------
    // Exported: org.freedesktop.CloudProviders.Provider (+ Properties)
    // ------------------------------------------------------------------
    private final class ProviderImpl
        implements CloudProviderProviderDef, Properties
    {
        @Override
        public String getObjectPath() {
            return PROVIDER_PATH;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <A> A Get(String interfaceName, String propertyName) {
            Variant<?> v = providerProperties().get(propertyName);
            return v == null ? null : (A) v.getValue();
        }

        @Override
        public <A> void Set(String interfaceName, String propertyName, A value) {
            // read-only
        }

        @Override
        public Map<String, Variant<?>> GetAll(String interfaceName) {
            return providerProperties();
        }
    }

    // ------------------------------------------------------------------
    // Exported: org.freedesktop.CloudProviders.Account (+ Properties)
    // ------------------------------------------------------------------
    private final class AccountImpl
        implements CloudProviderAccountDef, Properties
    {
        @Override
        public String getObjectPath() {
            return ACCOUNT_PATH;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <A> A Get(String interfaceName, String propertyName) {
            Variant<?> v = accountProperties().get(propertyName);
            return v == null ? null : (A) v.getValue();
        }

        @Override
        public <A> void Set(String interfaceName, String propertyName, A value) {
            // read-only
        }

        @Override
        public Map<String, Variant<?>> GetAll(String interfaceName) {
            return accountProperties();
        }
    }
}
