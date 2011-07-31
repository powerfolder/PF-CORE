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

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.*;

import javax.swing.*;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;

/**
 * Class to do folder creation for a specified invite.
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class FolderAutoCreatePanel extends PFWizardPanel {

    private final FolderInfo folderInfo;

    private JLabel folderNameLabel;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;
    private JCheckBox useCloudCB;

    public FolderAutoCreatePanel(Controller controller, FolderInfo folderInfo)
    {
        super(controller);
        this.folderInfo = folderInfo;
    }

    /**
     * Can procede if an invitation exists.
     */
    @Override
    public boolean hasNext() {
        return folderInfo != null;
    }

    public WizardPanel next() {

        // Set sync profile
        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
            syncProfileSelectorPanel.getSyncProfile());

        // Cloud
        getWizardContext().setAttribute(USE_CLOUD_STORAGE,
            useCloudCB.isSelected());

        // FolderInfo
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE,
            folderInfo);



        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
            Translation.getTranslation("wizard.setup_success"),
                Translation.getTranslation("wizard.success_configure"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        return new FolderAutoConfigPanel(getController());
    }

    @Override
    protected JPanel buildContent() {

        FormLayout layout = new FormLayout("right:pref, 3dlu, pref, pref:grow",
            "pref, 30dlu, pref, 3dlu, pref, 3dlu, pref, "
                + "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        int row = 1;

        // Info
        builder.addLabel(Translation.getTranslation(
                "wizard.folder_auto_create.info"),
                cc.xy(3, row));
         row += 2;

        // Name
        builder.addLabel(Translation.getTranslation("general.folder"),
                cc.xy(1, row));
        builder.add(folderNameLabel, cc.xy(3, row));
        row += 2;

        // Sync
        builder.addLabel(Translation.getTranslation("general.synchonisation"),
                cc.xy(1, row));
        JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
        p.setOpaque(false);
        builder.add(p, cc.xyw(3, row, 2));
        row += 2;

        // Cloud space
        builder.add(useCloudCB, cc.xyw(3, row, 2));
        row += 2;

        return builder.getPanel();
    }

    /**
     * Initalizes all necesary components
     */
    @Override
    protected void initComponents() {

        // Folder name label
        folderNameLabel = SimpleComponentFactory.createLabel();
        folderNameLabel.setText(folderInfo.getName());

        // Sync profile
        syncProfileSelectorPanel =
                new SyncProfileSelectorPanel(getController());
        Folder folder = getController().getFolderRepository().getFolder(
                folderInfo);
        SyncProfile syncProfile = folder.getSyncProfile();
        syncProfileSelectorPanel.setSyncProfile(syncProfile, false);

        // Cloud space
        useCloudCB = new JCheckBox(Translation.getTranslation(
                "wizard.folder_auto_create.cloud_space"));
        useCloudCB.setOpaque(false);
        useCloudCB.setSelected(
                PreferencesEntry.USE_ONLINE_STORAGE.getValueBoolean(
            getController()));
    }

    @Override
    protected String getTitle() {
        return Translation.getTranslation("wizard.folder_auto_create.title");
    }
}