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
 * $Id: TypicalFolderSetupPanel.java 19108 2012-06-03 23:18:59Z sprajc $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.panel.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.UserDirectories;
import de.dal33t.powerfolder.util.UserDirectory;

/**
 * Class to set up a 'Typical' folder. That is, a UserDirectories folder.
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class TypicalFolderSetupPanel extends PFWizardPanel {

    private JLabel folderTextField;
    private JTextField localFolderField;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;
    private FolderInfo folderInfo;

    /**
     * Constuctor
     *
     * @param controller
     */
    public TypicalFolderSetupPanel(Controller controller) {
        super(controller);
    }

    public boolean hasNext() {
        return true;
    }

    public WizardPanel next() {

        List<FolderCreateItem> folderCreateItems = new ArrayList<FolderCreateItem>();

        Path localBase = Paths.get(localFolderField.getText());
        FolderCreateItem fci = new FolderCreateItem(localBase);
        fci.setArchiveHistory(ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS
            .getValueInt(getController()));
        fci.setSyncProfile(syncProfileSelectorPanel.getSyncProfile());
        fci.setFolderInfo(folderInfo);
        folderCreateItems.add(fci);

        getWizardContext().setAttribute(
            WizardContextAttributes.FOLDER_CREATE_ITEMS, folderCreateItems);

        return new FolderCreatePanel(getController());
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
            "right:pref, 3dlu, 140dlu, 3dlu, 15dlu, pref:grow",
            "pref, 6dlu, pref, 6dlu, pref, 6dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        builder.addLabel(Translation.getTranslation("general.folder"),
            cc.xy(1, 1));
        builder.add(folderTextField, cc.xy(3, 1));

        builder
            .add(
                new JLabel(
                    Translation
                        .getTranslation("wizard.multi_online_storage_setup.local_folder_location")),
                cc.xy(1, 3));
        builder.add(localFolderField, cc.xy(3, 3));

        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            builder
                .add(
                    new JLabel(Translation
                        .getTranslation("general.transfer_mode")), cc.xy(1, 5));
            JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
            p.setOpaque(false);
            builder.add(p, cc.xyw(3, 5, 4));
        }

        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {

        folderTextField = new JLabel();

        localFolderField = new JTextField();
        localFolderField.setEditable(false);

        syncProfileSelectorPanel = new SyncProfileSelectorPanel(getController());
    }

    /**
     * Build map of foInfo and syncProfs
     */
    @SuppressWarnings({"unchecked"})
    public void afterDisplay() {
        folderInfo = (FolderInfo) getWizardContext().getAttribute(
                        WizardContextAttributes.FOLDER_INFO);

        // Try to find a cloud folder with this name, and use that.
        ServerClient client = getController().getOSClient();
        if (client.isConnected() && client.isLoggedIn()) {
            for (FolderInfo accountFolder : client.getAccountFolders()) {
                if (folderInfo.getName().equals(accountFolder.getName())) {
                    // Use this cloud folder instead.
                    folderInfo = accountFolder;
                    break;
                }
            }
        }
        Reject.ifNull(folderInfo, "Expecting a single folder info");
        folderTextField.setText(folderInfo.name);
        boolean showAppData = PreferencesEntry.EXPERT_MODE
            .getValueBoolean(getController());
        Map<String, UserDirectory> userDirectoryMap = UserDirectories
            .getUserDirectoriesFiltered(getController(), showAppData);
        for (String s : userDirectoryMap.keySet()) {
            if (s.equals(folderInfo.name)) {
                UserDirectory userDirectory = userDirectoryMap.get(s);
                localFolderField.setText(userDirectory.getDirectory()
                    .toAbsolutePath().toString());
            }
        }
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.typical_folder_setup.title");
    }
}