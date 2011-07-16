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
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.wizard.FolderCreatePanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.wizard.TextPanelPanel;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.*;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

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

        // Select directory
        FolderRepository folderRepository =
                getController().getFolderRepository();
        List<File> files = DialogFactory.chooseDirectory(getUIController(),
                folderRepository.getFoldersBasedir(),
                false);
        if (files == null || files.size() != 1) {
            return;
        }
        File file  = files.get(0);

        // Has user already got this folder?
        for (Folder folder : folderRepository.getFolders()) {
            if (folder.getBaseDirectoryInfo().getDiskFile(folderRepository)
                    .equals(file)) {
                return;
            }
        }

        // FolderInfo
        String name = FileUtils.getSuggestedFolderName(file);
        String folderId = '[' + IdGenerator.makeId() + ']';
        FolderInfo fi = new FolderInfo(name, folderId);

        // FolderSettings
        File localBaseDir = new File(folderRepository.getFoldersBasedir());

        // Setup sucess panel of this wizard path
        FolderCreatePanel createPanel = new FolderCreatePanel(getController());

        TextPanelPanel successPanel = new TextPanelPanel(getController(),
            Translation.getTranslation("wizard.setup_success"), Translation
                .getTranslation("wizard.what_to_do.folder_backup_success")
                + Translation.getTranslation("wizard.what_to_do.pcs_join"));

        PFWizard wizard = new PFWizard(getController(),
            Translation.getTranslation("wizard.pfwizard.folder_title"));

        wizard.getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                successPanel);

        wizard.getWizardContext().setAttribute(FOLDER_LOCAL_BASE, localBaseDir);
        wizard.getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                SyncProfile.AUTOMATIC_DOWNLOAD);
        wizard.getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, fi);
        wizard.getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
                false);
        wizard.getWizardContext().setAttribute(SAVE_INVITE_LOCALLY, false);

        wizard.open(createPanel);

    }
}
