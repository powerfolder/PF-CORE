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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import jwf.WizardPanel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FolderOnlineStoragePanel extends PFWizardPanel {

    private static final Logger log =
            Logger.getLogger(FolderOnlineStoragePanel.class.getName());

    private Folder folder;
    private JLabel folderLabel;
    private boolean hasJoined;

    public FolderOnlineStoragePanel(Controller controller, Folder folder) {
        super(controller);
        this.folder = folder;
        hasJoined = controller.getOSClient().hasJoined(folder);
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
        if (hasJoined) {
            int result = DialogFactory.genericDialog(getController(),
                    Translation.getTranslation(
                            "wizard.folder_online_storage.warning_title"),
                    Translation.getTranslation(
                            "wizard.folder_online_storage.warning_message"),
                    new String[]{Translation.getTranslation(
                            "wizard.folder_online_storage.warning_stop_backing"),
                            Translation.getTranslation("general.cancel")}, 0,
                    GenericDialogType.WARN);
            return result == 0; // Stop backing up
        } else {
            return true;
        }
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("$wlabel, $lcg, $wfield, 0:g",
                "pref, 6dlu, pref, 6dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        if (hasJoined) {
            builder.addLabel(Translation
                    .getTranslation("wizard.webservice.unmirror_folder"),
                    cc.xyw(1, 1, 4));
        } else {
            builder.addLabel(Translation
                    .getTranslation("wizard.webservice.mirror_folder"),
                    cc.xyw(1, 1, 4));
        }

        builder.addLabel(Translation.getTranslation("general.folder"), cc.xy(1,
            3));
        builder.add(folderLabel, cc.xy(3, 3));

        LinkLabel link = new LinkLabel(getController(), Translation
            .getTranslation("wizard.webservice.learn_more"),
            ConfigurationEntry.PROVIDER_ABOUT_URL.getValue(getController()));
        builder.add(link.getUiComponent(), cc.xyw(1, 5, 3));
        return builder.getPanel();
    }

    public WizardPanel next() {
        // Actually setup mirror
        try {

            if (hasJoined) {
                getController().getOSClient().getFolderService().removeFolder(
                        folder.getInfo(), true);
                getController().getOSClient().refreshAccountDetails();
                return new TextPanelPanel(getController(),
                        Translation.getTranslation("wizard.folder_online_storage.remove_success_title"),
                        Translation.getTranslation("wizard.folder_online_storage.remove_success_message",
                                folder.getName()));
            } else {
                getController().getOSClient().getFolderService().createFolder(
                        folder.getInfo(), SyncProfile.BACKUP_TARGET_NO_CHANGE_DETECT);
                getController().getOSClient().refreshAccountDetails();
                return new TextPanelPanel(getController(),
                        Translation.getTranslation("wizard.folder_online_storage.backup_success_title"),
                        Translation.getTranslation("wizard.folder_online_storage.backup_success_message",
                                folder.getName()));
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "", e);

            // Split long error message into multiple lines on ':'.
            String message = e.getMessage();
            String[] strings = message.split(":");
            StringBuilder sb = new StringBuilder();
            for (String string : strings) {
                sb.append(string.trim()).append('\n');
            }

            return new TextPanelPanel(getController(),
                    Translation.getTranslation("wizard.folder_online_storage.failure_title"),
                    Translation.getTranslation("wizard.folder_online_storage.failure_message",
                            folder.getName(), sb.toString().trim()));
        }

    }

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {
        ServerClient ws = getController().getOSClient();
        List<Folder> folders = new ArrayList<Folder>(getController()
                .getFolderRepository().getFoldersAsCollection());
        folders.removeAll(ws.getJoinedFolders());
        folderLabel = new JLabel(folder.getInfo().name);
        updateButtons();
    }

    protected JComponent getPictoComponent() {
        return new JLabel(Icons.getIconById(Icons.WEB_SERVICE_PICTO));
    }

    protected String getTitle() {
        if (hasJoined) {
            return Translation.getTranslation("wizard.webservice.unmirror_setup");
        } else {
            return Translation.getTranslation("wizard.webservice.mirror_setup");
        }
    }
}
