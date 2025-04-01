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

import static de.dal33t.powerfolder.light.FolderInfoFactory.newTopFolder;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.BACKUP_ONLINE_STOARGE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_CREATE_ITEMS;
import static de.dal33t.powerfolder.util.StringUtils.isBlank;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.wizard.FolderCreateItem;
import de.dal33t.powerfolder.ui.wizard.FolderCreatePanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.wizard.TextPanelPanel;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Translation;

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
                    // Benutzereingabe für neuen Ordnernamen
                    String folderName = DialogFactory.inputDialog(
                            getController(),
                            Translation.get("dialog.new_folder_name.title"),
                            Translation.get("dialog.new_folder_name.prompt"),
                            "");

                    if (isBlank(folderName)) {
                        return;
                    }
                    folderName = folderName.trim();
                    Path baseDir = folderRepository.getFoldersBasedir();
                    Path newFolderPath = baseDir.resolve(PathUtils.removeInvalidFilenameChars(folderName));

                    if (syncFolderWithSamePath(newFolderPath) || ownsFolderWithSameName(folderName)) {
                        DialogFactory.genericDialog(
                                getController(),
                                Translation.get("general.directory"),
                                Translation.get("general.folder_already_exists", folderName),
                                GenericDialogType.ERROR);
                        return;
                    }

                    // Wizard-Setup
                    FolderCreatePanel createPanel = new FolderCreatePanel(getController());
                    TextPanelPanel successPanel = new TextPanelPanel(
                            getController(),
                            Translation.get("wizard.setup_success"),
                            Translation.get("wizard.what_to_do.folder_backup_success") +
                                    Translation.get("wizard.what_to_do.pcs_join"));

                    PFWizard wizard = new PFWizard(getController(),
                            Translation.get("wizard.pfwizard.folder_title"));

                    wizard.getWizardContext().setAttribute(
                            PFWizard.SUCCESS_PANEL, successPanel);
                    wizard.getWizardContext().setAttribute(
                            BACKUP_ONLINE_STOARGE,
                            getController().getOSClient().isBackupByDefault());

                    FolderInfo folderInfo = newTopFolder(folderName);
                    FolderCreateItem item = new FolderCreateItem(newFolderPath);
                    item.setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
                    item.setFolderInfo(folderInfo);
                    item.setArchiveHistory(ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS
                            .getValueInt(getController()));

                    List<FolderCreateItem> folderCreateItems = new ArrayList<>();
                    folderCreateItems.add(item);

                    wizard.getWizardContext().setAttribute(FOLDER_CREATE_ITEMS, folderCreateItems);
                    wizard.open(createPanel);

                } finally {
                    try {
                        folderRepository.setSuspendNewFolderSearch(false);
                    } catch (Exception ex) {
                        // Ignore
                    }
                }
            }
        });
    }

    private boolean ownsFolderWithSameName(String folderName) {
        Account account = getController().getOSClient().getAccount();
        if (!account.isValid()) {
            return false;
        }
        for (FolderInfo ownerFolderInfo : getController().getOSClient().getAccount().getFoldersCharged()) {
            if (folderName.equalsIgnoreCase(ownerFolderInfo.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean syncFolderWithSamePath(Path newFolderPath) {
        FolderRepository folderRepository = getController().getFolderRepository();
        return folderRepository.findExistingFolder(newFolderPath) != null;
    }
}
