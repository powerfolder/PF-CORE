/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id: FolderCreatePanel.java 20999 2013-03-11 13:19:11Z glasgow $
 */
package de.dal33t.powerfolder.ui.wizard;

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.BACKUP_ONLINE_STOARGE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_CREATE_ITEMS;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_IS_INVITE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_LOCAL_BASE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.MAKE_FRIEND_AFTER;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.PREVIEW_FOLDER_ATTIRBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SET_DEFAULT_SYNCHRONIZED_FOLDER;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jwf.WizardPanel;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.ProUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.UserDirectories;
import de.dal33t.powerfolder.util.os.Win32.ShellLink;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;

/**
 * A panel that actually starts the creation process of a folder on display.
 * Automatically switches to the next panel when succeeded otherwise prints
 * error.
 * <p>
 * Extracts the settings for the folder from the
 * <code>WizardContextAttributes</code>.
 *
 * @author Christian Sprajc
 * @version $Revision$
 */
public class FolderCreatePanel extends SwingWorkerPanel {

    private static final Logger log = Logger.getLogger(FolderCreatePanel.class
        .getName());

    private boolean sendInvitations;
    private final List<Folder> folders;

    public FolderCreatePanel(Controller controller) {
        super(controller, null, Translation
            .get("wizard.create_folder.title"), Translation
            .get("wizard.create_folder.working"), null);
        setTask(new MyFolderCreateWorker());
        folders = new ArrayList<Folder>();
    }

    @Override
    protected String getTitle() {
        return Translation.get("wizard.create_folder.title");
    }

    @Override
    public boolean hasNext() {
        return !folders.isEmpty();
    }

    @Override
    public WizardPanel next() {
        WizardPanel next;
        if (sendInvitations) {
            next = new SendInvitationsPanel(getController());
        } else {
            next = (WizardPanel) getWizardContext().getAttribute(
                PFWizard.SUCCESS_PANEL);
        }
        return next;
    }

    private static FolderInfo createFolderInfo(Path localBase) {
        // Create new folder info
        String name = PathUtils.getSuggestedFolderName(localBase);
        String folderId = IdGenerator.makeFolderId();
        return new FolderInfo(name, folderId);
    }

    private class MyFolderCreateWorker implements Runnable {

        public void run() {
            Map<FolderInfo, FolderSettings> configurations = new HashMap<FolderInfo, FolderSettings>();
            Map<FolderInfo, String> joinFolders = new HashMap<FolderInfo, String>();

            // Preview folder
            Object attribute = getWizardContext().getAttribute(
                PREVIEW_FOLDER_ATTIRBUTE);

            // Online storage
            attribute = getWizardContext().getAttribute(BACKUP_ONLINE_STOARGE);
            boolean backupByOS = false;
            if (attribute != null && attribute instanceof Boolean) {
                backupByOS = (Boolean) attribute;
            }
            if (backupByOS) {
                getController().getUIController().getApplicationModel()
                    .getServerClientModel().checkAndSetupAccount();
            }

            // Send invitation after
            attribute = getWizardContext().getAttribute(
                SEND_INVIATION_AFTER_ATTRIBUTE);
            sendInvitations = false;
            if (attribute != null && attribute instanceof Boolean) {
                sendInvitations = (Boolean) attribute;
            }

            // Either we have FOLDER_CREATE_ITEMS ...
            List<FolderCreateItem> folderCreateItems = (List<FolderCreateItem>) getWizardContext()
                .getAttribute(FOLDER_CREATE_ITEMS);
            if (folderCreateItems != null && !folderCreateItems.isEmpty()) {
                for (FolderCreateItem folderCreateItem : folderCreateItems) {
                    Path localBase = PathUtils
                        .removeInvalidFilenameChars(folderCreateItem
                            .getLocalBase());
                    Reject.ifNull(localBase,
                        "Local base for folder is null/not set");
                    SyncProfile syncProfile = folderCreateItem.getSyncProfile();
                    if (syncProfile == null) {
                        syncProfile = SyncProfile.getDefault(getController());
                    }
                    FolderInfo folderInfo = folderCreateItem.getFolderInfo();
                    if (folderInfo == null) {
                        folderInfo = createFolderInfo(localBase);
                    }
                    int archiveHistory = folderCreateItem.getArchiveHistory();
                    if (!StringUtils.isBlank(folderCreateItem
                        .getLinkToOnlineFolder()))
                    {
                        joinFolders.put(folderInfo,
                            folderCreateItem.getLinkToOnlineFolder());
                    }
                    FolderSettings folderSettings = new FolderSettings(
                        localBase, syncProfile, null, archiveHistory, true);
                    configurations.put(folderInfo, folderSettings);
                }
            } else {

                // ... or FOLDER_LOCAL_BASE + SYNC_PROFILE_ATTRIBUTE + optional
                // FOLDERINFO_ATTRIBUTE...
                Path localBase = PathUtils
                    .removeInvalidFilenameChars((Path) getWizardContext()
                        .getAttribute(FOLDER_LOCAL_BASE));
                Reject.ifNull(localBase,
                    "Local base for folder is null/not set");
                SyncProfile syncProfile = (SyncProfile) getWizardContext()
                    .getAttribute(SYNC_PROFILE_ATTRIBUTE);

                if (syncProfile == null) {
                    syncProfile = SyncProfile.getDefault(getController());
                }

                // Optional
                FolderInfo folderInfo = (FolderInfo) getWizardContext()
                    .getAttribute(FOLDERINFO_ATTRIBUTE);
                if (folderInfo == null) {
                    folderInfo = createFolderInfo(localBase);
                }

                FolderSettings folderSettings = new FolderSettings(localBase,
                    syncProfile, null,
                    ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS
                        .getValueInt(getController()), true);
                configurations.put(folderInfo, folderSettings);
            }

            // Reset
            folders.clear();
            updateButtons();

            ServerClient client = getController().getOSClient();

            Collection<FolderInfo> onlineFolderInfos = client
                .getAccountFolders();

            for (Map.Entry<FolderInfo, FolderSettings> entry : configurations
                .entrySet())
            {
                FolderInfo folderInfo = entry.getKey();
                FolderSettings folderSettings = entry.getValue();
                String joinFolderName = joinFolders.get(folderInfo);

                if (joinFolderName == null) {

                    // Don't try to join online folders by name if this is an
                    // invite. Invites always join the invite folder.
                    Boolean folderIsInvite = (Boolean) getWizardContext()
                        .getAttribute(FOLDER_IS_INVITE);
                    if (folderIsInvite == null || !folderIsInvite) {

                        // Look for folders where there is already an online
                        // folder with the same name. Join instead of creating
                        // duplicates.
                        for (FolderInfo onlineFolderInfo : onlineFolderInfos) {
                            // PFC-2562
                            if (onlineFolderInfo.getName().equals(
                                folderInfo.getName())
                                && !ProUtil.isZyncro(getController()))
                            {
                                if (!onlineFolderInfo.equals(folderInfo)) {
                                    log.info("Found online folder with same name: "
                                        + folderInfo.getName() + ". Using it");

                                    // User actually wants to join, so use
                                    // online.
                                    folderInfo = onlineFolderInfo;
                                    log.info("Changed folder info to online version: "
                                        + folderInfo.getName());
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    // User already specified online folder to join - join it.
                    boolean gotIt = false;
                    for (FolderInfo onlineFolderInfo : onlineFolderInfos) {
                        if (onlineFolderInfo.getName().equals(joinFolderName)) {
                            log.info("Joining specified folder "
                                + joinFolderName);
                            folderInfo = onlineFolderInfo;
                            gotIt = true;
                            break;
                        }
                    }
                    if (!gotIt) {
                        // Hmmm - link folder specified but can not find it now?
                        log.warning("Could not find link folder "
                            + joinFolderName + " for " + folderInfo);
                    }
                }

                Folder folder = getController().getFolderRepository()
                    .createFolder(folderInfo, folderSettings);

                folder.addDefaultExcludes();
                createShortcutToFolder(folderInfo, folderSettings);

                folders.add(folder);
                if (configurations.size() == 1) {
                    // Set for SendInvitationsPanel
                    getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE,
                        folder.getInfo());
                }

                // Is there a member to make a friend?
                // Invitation invitors are automatically made friends.
                attribute = getWizardContext().getAttribute(
                    MAKE_FRIEND_AFTER);
                if (attribute != null && attribute instanceof MemberInfo) {
                    MemberInfo memberInfo = (MemberInfo) attribute;
                    Member member = getController().getNodeManager().getNode(
                        memberInfo);
                    if (member != null) {
                        if (!member.isFriend()) {
                            member.setFriend(true, null);
                        }
                    }
                }

                if (backupByOS && client.isLoggedIn()) {
                    // Try to back this up by online storage.
                    if (client.joinedByCloud(folder)) {
                        // Already have this os folder.
                        log.log(Level.WARNING, "Already have os folder "
                            + folderInfo.getLocalizedName());
                        continue;
                    }

                    client.getFolderService().createFolder(folderInfo, null);

                    // Set as default synced folder?
                    attribute = getWizardContext().getAttribute(
                        SET_DEFAULT_SYNCHRONIZED_FOLDER);
                    if (attribute != null && (Boolean) attribute) {
                        // TODO: Ugly. Use abstraction: Runnable? Callback
                        // with
                        // folder? Which is placed on WizardContext.
                        client.getFolderService().setDefaultSynchronizedFolder(
                            folderInfo);
                        createDefaultFolderHelpFile(folder);
                        folder.recommendScanOnNextMaintenance();
                        PathUtils.openFile(folder.getLocalBase());
                    }
                }
            }
        }

        private void createShortcutToFolder(FolderInfo folderInfo,
            FolderSettings folderSettings)
        {
            FolderRepository folderRepo = getController().getFolderRepository();
            Path baseDir = folderRepo.getFoldersBasedir();
            if (Files.notExists(baseDir)) {
                log.info(String.format("Creating basedir: %s",
                    baseDir.toAbsolutePath()));
                try {
                    Files.createDirectories(baseDir);
                }
                catch (IOException ioe) {
                    log.info(ioe.getMessage());
                }
            }

            Path existingFolder = baseDir.resolve(folderInfo.getLocalizedName());
            if (Files.exists(existingFolder)) {
                log.finer("Folder is an existing subdirectory in basedir: "
                    + existingFolder);
                return;
            }
            
            // PFC-2284:
            if (folderSettings.getLocalBaseDir().startsWith(baseDir)) {
                log.finer("Folder is an existing subdirectory in basedir: "
                    + existingFolder);
                return;
            }

            Path shortcutFile = baseDir.resolve(folderInfo.getLocalizedName() + Constants.LINK_EXTENSION);
            String shortcutPath = shortcutFile.toAbsolutePath().toString();
            String filePath = folderSettings.getLocalBaseDir()
                .toAbsolutePath().toString();

            if (WinUtils.isSupported()
                && !UserDirectories.getUserDirectories(getController())
                    .containsKey(folderInfo.getName()))
            {
                WinUtils winUtils = WinUtils.getInstance();
                ShellLink shellLink = new ShellLink(null, null, filePath, null);
                try {
                    log.info(String.format(
                        "Attempting to create shortcut %s to %s", shortcutPath,
                        filePath));
                    winUtils.createLink(shellLink, shortcutPath);
                } catch (IOException e) {
                    log.warning(String
                        .format(
                            "An exception was thrown when creating shortcut %s to %s",
                            shortcutPath, filePath));
                }
            }
        }

        private void createDefaultFolderHelpFile(Folder folder) {
            Path helpFile = folder.getLocalBase().resolve(
                "Place files to sync here.txt");
            if (Files.exists(helpFile)) {
                return;
            }
            Writer w = null;
            try {
                w = new OutputStreamWriter(Files.newOutputStream(helpFile));
                w.write("This is the default synchronized folder of PowerFolder.\r\n");
                w.write("Simply place files into this directory to sync them\r\n");
                w.write("across all your computers running PowerFolder.\r\n");
                w.write("\r\n");
                w.write("More information: http://wiki.powerfolder.com/wiki/Default_Folder");
                w.close();
            } catch (IOException e) {
                // Doesn't matter.
            } finally {
                if (w != null) {
                    try {
                        w.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }

    }

}
