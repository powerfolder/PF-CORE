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
 * $Id: NewFolderAction.java 5419 2008-09-29 12:18:20Z harry $
 */
package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.wizard.FolderCreatePanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.wizard.TextPanelPanel;
import de.dal33t.powerfolder.ui.wizard.FolderCreateItem;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.*;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ArchiveMode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * Action which opens folder create wizard.
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0 $
 */
public class NewFolderAction extends BaseAction {

    public NewFolderAction(Controller controller) {
        super("action_new_folder", controller);
    }

    public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Check login
                getUIController().getApplicationModel().getServerClientModel()
                    .checkAndSetupAccount();

                // Suspend new folder search in the FolderRepository.
                FolderRepository folderRepository = getController()
                    .getFolderRepository();
                folderRepository.setSuspendNewFolderSearch(true);

                try {
                    // Select directory
                    List<File> files = DialogFactory.chooseDirectory(
                        getUIController(),
                        folderRepository.getFoldersBasedir(), true);
                    if (files == null || files.isEmpty()) {
                        return;
                    }

                    if (isPowerFolderRootSelected(files)) {
                        return;
                    }

                    if (isNonPowerFolderRootAllowedSelected(files)) {
                        return;
                    }

                    // Setup success panel of this wizard path
                    FolderCreatePanel createPanel = new FolderCreatePanel(
                        getController());

                    TextPanelPanel successPanel = new TextPanelPanel(
                        getController(),
                        Translation.getTranslation("wizard.setup_success"),
                        Translation
                            .getTranslation("wizard.what_to_do.folder_backup_success")
                            + Translation
                                .getTranslation("wizard.what_to_do.pcs_join"));

                    PFWizard wizard = new PFWizard(getController(), Translation
                        .getTranslation("wizard.pfwizard.folder_title"));

                    wizard.getWizardContext().setAttribute(
                        PFWizard.SUCCESS_PANEL, successPanel);
                    wizard.getWizardContext().setAttribute(SAVE_INVITE_LOCALLY,
                        false);
                    wizard.getWizardContext().setAttribute(
                        BACKUP_ONLINE_STOARGE,
                        getController().getOSClient().isBackupByDefault());

                    List<FolderCreateItem> folderCreateItems = new ArrayList<FolderCreateItem>();

                    outer : for (File file : files) {
                        // Has user already got this folder?
                        for (Folder folder : folderRepository.getFolders()) {
                            if (folder.getBaseDirectoryInfo()
                                .getDiskFile(folderRepository).equals(file))
                            {
                                continue outer;
                            }
                        }
                        // Prevent user from syncing the base directory.
                        if (file.equals(folderRepository
                            .getFoldersAbsoluteDir()))
                        {
                            continue;
                        }

                        // FolderInfo
                        String name = FileUtils.getSuggestedFolderName(file);
                        String folderId = '[' + IdGenerator.makeId() + ']';
                        FolderInfo fi = new FolderInfo(name, folderId);

                        FolderCreateItem item = new FolderCreateItem(file);
                        item.setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
                        item.setFolderInfo(fi);
                        item.setArchiveHistory(ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS
                            .getValueInt(getController()));
                        item.setArchiveMode(ArchiveMode.FULL_BACKUP);
                        folderCreateItems.add(item);

                    }

                    if (folderCreateItems.isEmpty()) {
                        return;
                    }

                    // Wizard will also suspend new folder search.
                    wizard.getWizardContext().setAttribute(FOLDER_CREATE_ITEMS,
                        folderCreateItems);
                    wizard.open(createPanel);
                    // Wizard unsuspends new folder search.

                } finally {
                    try {
                        // Unsuspend new folder search in the FolderRepository.
                        folderRepository.setSuspendNewFolderSearch(false);
                    } catch (Exception ex) {
                        // Nothing much can be done now.
                    }
                }
            }
        });
    }

    /**
     * Is user is only allowed to select folder base subdirs and selects outside?
     * Disallow (#2226).
     *
     * @param files
     * @return
     */
    private boolean isNonPowerFolderRootAllowedSelected(List<File> files) {
        if (ConfigurationEntry.FOLDER_CREATE_IN_BASEDIR_ONLY.getValueBoolean(getController())) {
            for (File file : files) {
                if (!file.getParentFile().equals(getController().getFolderRepository().getFoldersAbsoluteDir())) {
                    String title = Translation.getTranslation("general.directory");
                    String message =  Translation.getTranslation("new_folder_action.non_basedir_error.text");
                    DialogFactory.genericDialog(getController(), title, message, GenericDialogType.ERROR);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Is one of the files the PowerFolder base directory?
     * A bad thing if true. Should be managing a subdirectory of this.
     *
     * @param files
     * @return
     */
    private boolean isPowerFolderRootSelected(List<File> files) {
        String baseDir =
                getController().getFolderRepository().getFoldersBasedir();
        for (File file : files) {
            if (file.getAbsolutePath().equals(baseDir)) {
                String title = Translation.getTranslation("general.directory");
                String message =  Translation.getTranslation("new_folder_action.basedir_error.text");
                DialogFactory.genericDialog(getController(), title, message, GenericDialogType.ERROR);
                return true;
            }
        }
        return false;
    }
}
