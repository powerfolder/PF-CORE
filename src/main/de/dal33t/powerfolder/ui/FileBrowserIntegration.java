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
package de.dal33t.powerfolder.ui;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.liferay.nativity.control.NativityControl;
import com.liferay.nativity.control.NativityControlUtil;
import com.liferay.nativity.listeners.SocketOpenListener;
import com.liferay.nativity.modules.contextmenu.ContextMenuControlUtil;
import com.liferay.nativity.modules.fileicon.FileIconControl;
import com.liferay.nativity.modules.fileicon.FileIconControlUtil;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.Locking;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.contextmenu.ContextMenuHandler;
import de.dal33t.powerfolder.ui.cloudproviders.CloudProvidersIntegration;
import de.dal33t.powerfolder.ui.iconoverlay.IconOverlayHandler;
import de.dal33t.powerfolder.ui.iconoverlay.IconOverlayIndex;
import de.dal33t.powerfolder.ui.iconoverlay.IconOverlayUpdateListener;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.mac.MacUtils;

/**
 * Enable Overlay Icons and context menu entries on the different platforms
 * using the library of Liferay Nativity.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class FileBrowserIntegration extends PFComponent {

    private NativityControl nc;
    private IconOverlayHandler iconOverlayHandler;
    private IconOverlayUpdateListener updateListener;
    private FileIconControl iconControl;
    /** PFC-3643: Linux file-manager integration (libcloudproviders). */
    private CloudProvidersIntegration cloudProviders;

    public FileBrowserIntegration(Controller controller) {
        super(controller);
    }

    /**
     * Start up the shell extensions according to the OS we are running on. Adds
     * different listeners and visitors to {@link Folder Folders},
     * {@link FolderRepository}, {@link Locking} and the {@link TransferManager}
     * 
     * @return {@code True} if the shell extensions could be loaded/started,
     *         {@code false} otherwise.
     */
    public boolean start() {
        logFine("Starting file browser integration");

        // PFC-3643: Linux has no liferay-nativity shell-overlay support (it needs
        // a native Nautilus extension we do not ship). Use the freedesktop
        // libcloudproviders D-Bus integration instead, which shows the base dir's
        // sync status in the file-manager sidebar (GNOME Files, KDE Dolphin, ...).
        if (OSUtil.isLinux()) {
            logFine("Connect file browser integration to Linux (libcloudproviders)");
            return fbLinux();
        }

        if (nc == null) {
            nc = NativityControlUtil.getNativityControl();

            if (nc == null) {
                logFine("Could not start file browser integration");
                return false;
            }
        }

        // Initializing icon overlays
        if (iconOverlayHandler == null) {
            iconOverlayHandler = new IconOverlayHandler(getController());
            iconControl = FileIconControlUtil
                .getFileIconControl(nc, iconOverlayHandler);
        }

        // Initializing updates to icon overlays
        if (updateListener == null) {
            updateListener = new IconOverlayUpdateListener(getController(),
                iconControl, iconOverlayHandler);
        }

        // Initializing context menu
        if (PreferencesEntry.ENABLE_CONTEXT_MENU
            .getValueBoolean(getController()))
        {
            logFine("Initializing context menu");
            ContextMenuControlUtil.getContextMenuControl(nc,
                new ContextMenuHandler(getController()));
        }

        // Setting some listeners
        FolderRepository repo = getController().getFolderRepository();
        for (Folder folder : repo.getFolders()) {
            folder.addFolderListener(updateListener);
        }
        repo.addFolderRepositoryListener(updateListener);
        repo.getLocking().addListener(updateListener);
        getController().getTransferManager().addListener(updateListener);

        // Actually set up and connect to the native shell extension
        if (OSUtil.isWindowsSystem()) {
            logFine("Connect file browser integration to Windows");
            return fbWindows();
        } else if (OSUtil.isMacOS()) {
            logFine("Connect file browser integration to OS X");
            return fbApple();
        }

        return false;
    }

    /**
     * Prepare the local socket for communication with the OS X AppleScript
     * Finder integration.
     * 
     * @return {@code True} if connection was set up correctly, {@code false}
     *         otherwise.
     */
    private boolean fbApple() {
        try {
            logFine("Preparing icons");
            Path resourcesPath = Paths
                .get(MacUtils.getInstance().getRecourcesLocation())
                .toAbsolutePath();
//            resourcesPath = Paths.get("/Users/krickl/git/PF-CORE/src/resources/");
            Path okIcon = resourcesPath
                .resolve(IconOverlayIndex.OK_OVERLAY.getFilename());
            Path syncingIcon = resourcesPath
                .resolve(IconOverlayIndex.SYNCING_OVERLAY.getFilename());
            Path warningIcon = resourcesPath
                .resolve(IconOverlayIndex.WARNING_OVERLAY.getFilename());
            Path ignoredIcon = resourcesPath
                .resolve(IconOverlayIndex.IGNORED_OVERLAY.getFilename());
            Path lockedIcon = resourcesPath
                .resolve(IconOverlayIndex.LOCKED_OVERLAY.getFilename());

            logFine("Registering icons");
            nc.addSocketOpenListener(new SocketOpenListener() {

                @Override
                public void onSocketOpen() {
                    iconControl.registerIconWithId(okIcon.toString(),
                        IconOverlayIndex.OK_OVERLAY.getLabel(),
                        String.valueOf(IconOverlayIndex.OK_OVERLAY.getIndex()));
                    iconControl.registerIconWithId(syncingIcon.toString(),
                        IconOverlayIndex.SYNCING_OVERLAY.getLabel(), String
                            .valueOf(IconOverlayIndex.SYNCING_OVERLAY.getIndex()));
                    iconControl.registerIconWithId(warningIcon.toString(),
                        IconOverlayIndex.WARNING_OVERLAY.getLabel(), String
                            .valueOf(IconOverlayIndex.WARNING_OVERLAY.getIndex()));
                    iconControl.registerIconWithId(ignoredIcon.toString(),
                        IconOverlayIndex.IGNORED_OVERLAY.getLabel(), String
                            .valueOf(IconOverlayIndex.IGNORED_OVERLAY.getIndex()));
                    iconControl.registerIconWithId(lockedIcon.toString(),
                        IconOverlayIndex.LOCKED_OVERLAY.getLabel(),
                        String.valueOf(IconOverlayIndex.LOCKED_OVERLAY.getIndex()));
                    iconControl.enableFileIcons();

                    List<String> volumes = new ArrayList<>();
                    volumes.add("/");

                    // Get all volumes to add to the 
                    try (DirectoryStream<Path> volumePaths = Files
                        .newDirectoryStream(Paths.get("/Volumes")))
                    {
                        for (Path volumePath : volumePaths) {
                            if (Files.isDirectory(volumePath)) {
                                volumes.add(volumePath.toString() + "/");
                            } else {
                                logFine("Ignoring " + volumePath.toString());
                            }
                        }

                        for (String vol : volumes) {
                            logInfo("Base directory for Finder Sync: " + vol);
                        }
                    } catch (RuntimeException | IOException e) {
                        logWarning(
                            "Error while determening the base volume paths to register for Finder Sync. "
                                + e,
                            e);
                    }

                    // Register the volumes
                    nc.setFilterFolders(volumes.toArray(new String[0]));
                }
            });

            if (!nc.connect()) {
                logWarning("Could not connect to finder sync.");
                return false;
            }

            logFine("Connected to finder sync.");

            try {
                logFine("Auto enabling extension.");
                String[] cmd = new String[5];
                //cmd[0] = "sudo";
                cmd[0] = "pluginkit";
                cmd[1] = "-e";
                cmd[2] = "use";
                cmd[3] = "-i";
                cmd[4] = "com.liferay.nativity.LiferayFinderSync";
                Runtime.getRuntime().exec(cmd);
            } catch (Exception e) {
                logWarning(
                    "Could not activate FinderSync extension automatically! "
                        + e);
            }

            return true;
        } catch (Exception re) {
            logWarning("Could not start finder sync. " + re);
            return false;
        }
    }

    /**
     * Prepare the local socket for communictaion with the windows dlls.
     * 
     * @return {@code True} if connection was set up correctly, {@code false}
     *         otherwise.
     */
    private boolean fbWindows() {
        try {
            if (!nc.connect()) {
                logWarning("Could not connect to shell extensions!");
                return false;
            }
            nc.setFilterFolder("");
            iconControl.enableFileIcons();
            logFine("Connected to shell extensions.");
            return true;
        } catch (RuntimeException re) {
            logWarning("Could not start shell extensions. " + re);
            return false;
        }
    }

    /**
     * Start the Linux file-manager integration via libcloudproviders (PFC-3643).
     * Exposes the PowerFolders base directory as a cloud-provider account whose
     * sync status is shown in the file-manager sidebar. Pure D-Bus, no native
     * libraries; unlike Windows/macOS this does not use liferay-nativity.
     *
     * @return {@code true} if the integration started.
     */
    private boolean fbLinux() {
        try {
            Path baseDir = getController().getFolderRepository()
                .getFoldersBasedir();
            if (baseDir == null) {
                logFine("No folders base directory; skipping cloud providers");
                return false;
            }
            String basePath = baseDir.toAbsolutePath().toString();
            String name = Translation.get("general.application.name");
            if (name == null || name.trim().isEmpty() || name.startsWith("- ")) {
                name = "PowerFolder";
            }
            // Icon-theme name for the account entry. The launcher sets awt.appName
            // to the branded binary name (which is also the installed hicolor icon
            // name); fall back to the generic "folder" icon otherwise.
            String icon = System.getProperty("awt.appName");
            if (icon == null || icon.trim().isEmpty()) {
                icon = "folder";
            }
            cloudProviders = new CloudProvidersIntegration(getController(), name,
                basePath, icon);
            if (!cloudProviders.start()) {
                cloudProviders = null;
                return false;
            }
            logInfo("Started Linux file-manager integration (libcloudproviders)");
            return true;
        } catch (Throwable t) {
            logWarning("Could not start Linux cloud providers integration. " + t);
            cloudProviders = null;
            return false;
        }
    }

    /**
     * Lifecycle management. Removes the listeners and visitors.<br />
     * Should be called, when shutting down the client.
     */
    public void shutdown() {
        // PFC-3643: Linux libcloudproviders path uses none of the liferay state.
        if (cloudProviders != null) {
            cloudProviders.dispose();
            cloudProviders = null;
            return;
        }
        if (nc == null) {
            return;
        }

        FileIconControlUtil.getFileIconControl(nc, iconOverlayHandler)
            .disableFileIcons();

        getController().getFolderRepository().getLocking()
            .removeListener(updateListener);
        getController().getFolderRepository().removeFolderRepositoryListener(
            updateListener);
        getController().getTransferManager().removeListener(updateListener);
        FolderRepository repo = getController().getFolderRepository();
        for (Folder folder : repo.getFolders()) {
            folder.removeFolderListener(updateListener);
        }
    }
}
