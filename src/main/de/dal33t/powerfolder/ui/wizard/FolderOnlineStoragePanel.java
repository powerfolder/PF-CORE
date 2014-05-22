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
 * $Id$
 */
package de.dal33t.powerfolder.ui.wizard;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;

public class FolderOnlineStoragePanel extends PFWizardPanel {

    private static final Logger log = Logger
        .getLogger(FolderOnlineStoragePanel.class.getName());

    private FolderInfo foInfo;
    private boolean removeFolder;
    private JLabel folderLabel;

    public FolderOnlineStoragePanel(Controller controller, FolderInfo foInfo) {
        super(controller);
        Reject.ifNull(foInfo, "FolderInfo");
        this.foInfo = foInfo;
        Folder folder = getController().getFolderRepository().getFolder(foInfo);
        boolean osJoined = folder != null
            && controller.getOSClient().joinedByCloud(folder);
        removeFolder = osJoined;
    }

    // From WizardPanel *******************************************************

    public boolean hasNext() {
        return true;
    }

    /**
     * Give user warning if stopping backing up.
     * 
     * @param panelList
     * @return
     */
    public boolean validateNext() {
        if (removeFolder) {
            int result = DialogFactory
                .genericDialog(
                    getController(),
                    Translation
                        .getTranslation("wizard.folder_online_storage.warning_title"),
                    Translation
                        .getTranslation("wizard.folder_online_storage.warning_message"),
                    new String[]{
                        Translation
                            .getTranslation("wizard.folder_online_storage.warning_stop_backing"),
                        Translation.getTranslation("general.cancel")}, 0,
                    GenericDialogType.WARN);
            return result == 0; // Stop backing up
        } else {
            return true;
        }
    }

    public WizardPanel next() {
        // Actually setup mirror
        Runnable task;
        WizardPanel next;

        if (removeFolder) {
            task = new Runnable() {
                public void run() {
                    // Keep folder permission
                    getController().getOSClient().getFolderService()
                        .removeFolder(foInfo, true, false);
                }

            };
            next = new TextPanelPanel(
                getController(),
                Translation
                    .getTranslation("wizard.folder_online_storage.remove_success_title"),
                Translation.getTranslation(
                    "wizard.folder_online_storage.remove_success_message",
                    foInfo.getLocalizedName()), true);
        } else {
            task = new Runnable() {
                public void run() {
                    getController().getOSClient().getFolderService()
                        .createFolder(foInfo, null);
                }
            };

            next = new TextPanelPanel(
                getController(),
                Translation
                    .getTranslation("wizard.folder_online_storage.backup_success_title"),
                Translation.getTranslation(
                    "wizard.folder_online_storage.backup_success_message",
                    foInfo.name));
        }
        return new SwingWorkerPanel(getController(), task,
            Translation.getTranslation("wizard.folder_online_storage.working"),
            Translation
                .getTranslation("wizard.folder_online_storage.working.text"),
            next);
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("$wlabel, $lcg, $wfield, 0:g",
            "pref, 6dlu, pref, 6dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        if (removeFolder) {
            builder
                .addLabel(Translation
                    .getTranslation("wizard.webservice.unmirror_folder"), cc
                    .xyw(1, 1, 4));
        } else {
            builder.addLabel(
                Translation.getTranslation("wizard.webservice.mirror_folder"),
                cc.xyw(1, 1, 4));
        }

        builder.addLabel(Translation.getTranslation("general.folder"),
            cc.xy(1, 3));
        builder.add(folderLabel, cc.xy(3, 3));

        LinkLabel link = new LinkLabel(getController(),
            Translation.getTranslation("wizard.webservice.learn_more"),
            ConfigurationEntry.PROVIDER_ABOUT_URL.getValue(getController()));
        builder.add(link.getUIComponent(), cc.xyw(1, 5, 3));
        return builder.getPanel();
    }

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {
        ServerClient ws = getController().getOSClient();
        List<Folder> folders = new ArrayList<Folder>(getController()
            .getFolderRepository().getFolders());
        folders.removeAll(ws.getJoinedCloudFolders());
        folderLabel = new JLabel(foInfo.name);
        updateButtons();
    }

    protected String getTitle() {
        if (removeFolder) {
            return Translation
                .getTranslation("wizard.webservice.unmirror_setup");
        } else {
            return Translation.getTranslation("wizard.webservice.mirror_setup");
        }
    }
}
