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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.util.DialogFactory;
import de.dal33t.powerfolder.ui.util.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.util.UserDirectories;
import de.dal33t.powerfolder.util.UserDirectory;

/**
 * Class to do sync profile configuration for OS joins.
 * 
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class MultiOnlineStorageSetupPanel extends PFWizardPanel {

    private Map<FolderInfo, SyncProfile> folderProfileMap;
    private Map<FolderInfo, File> folderLocalBaseMap;
    private JComboBox folderInfoCombo;
    private JTextField folderInfoField;
    private DefaultComboBoxModel folderInfoComboModel;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;
    private boolean changingSelecton;

    private JTextField localFolderField;
    private JButton localFolderButton;

    /**
     * Constuctor
     * 
     * @param controller
     */
    public MultiOnlineStorageSetupPanel(Controller controller) {
        super(controller);
    }

    public boolean hasNext() {
        return true;
    }

    public WizardPanel next() {

        List<FolderCreateItem> folderCreateItems = new ArrayList<FolderCreateItem>();

        for (FolderInfo folderInfo : folderProfileMap.keySet()) {
            SyncProfile sp = folderProfileMap.get(folderInfo);
            File localBase = folderLocalBaseMap.get(folderInfo);
            FolderCreateItem fci = new FolderCreateItem(localBase);
            fci.setSyncProfile(sp);
            fci.setFolderInfo(folderInfo);
            folderCreateItems.add(fci);
        }

        getWizardContext().setAttribute(
            WizardContextAttributes.FOLDER_CREATE_ITEMS, folderCreateItems);

        getWizardContext().setAttribute(
            WizardContextAttributes.SAVE_INVITE_LOCALLY, false);

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

        // folderInfoCombo & folderInfoField share the same slot.
        builder.add(folderInfoCombo, cc.xy(3, 1));
        builder.add(folderInfoField, cc.xy(3, 1));

        builder.add(new JLabel(Translation.getTranslation(
                "wizard.multi_online_storage_setup.local_folder_location")),
                cc.xy(1, 3));
        builder.add(localFolderField, cc.xy(3, 3));
        builder.add(localFolderButton, cc.xy(5, 3));

        if (PreferencesEntry.ADVANCED_MODE.getValueBoolean(getController())) {
            builder
                .add(
                    new JLabel(Translation
                        .getTranslation("general.transfer_mode")), cc.xy(1, 5));
            JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
            p.setOpaque(false);
            builder.add(p, cc.xyw(3, 5, 4));
        } else {
            syncProfileSelectorPanel.getUIComponent();
        }

        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {

        localFolderField = new JTextField();
        localFolderField.setEditable(false);
        localFolderButton = new JButtonMini(
            Icons.getIconById(Icons.DIRECTORY),
            Translation
                .getTranslation("wizard.multi_online_storage_setup.ge_setup.select_directory"));
        MyActionListener myActionListener = new MyActionListener();
        localFolderButton.addActionListener(myActionListener);

        syncProfileSelectorPanel = new SyncProfileSelectorPanel(getController());
        syncProfileSelectorPanel
            .addModelValueChangeListener(new MyPropertyValueChangeListener());

        folderInfoComboModel = new DefaultComboBoxModel();
        folderInfoCombo = new JComboBox(folderInfoComboModel);

        folderInfoCombo.addItemListener(new MyItemListener());
        folderInfoField = new JTextField();
        folderInfoField.setEditable(false);
    }

    /**
     * Build map of foInfo and syncProfs
     */
    @SuppressWarnings({"unchecked"})
    public void afterDisplay() {
        boolean showAppData = PreferencesEntry.ADVANCED_MODE
            .getValueBoolean(getController());
        Map<String, UserDirectory> userDirs = UserDirectories
            .getUserDirectoriesFiltered(getController(), showAppData);

        folderProfileMap = new HashMap<FolderInfo, SyncProfile>();
        folderLocalBaseMap = new HashMap<FolderInfo, File>();
        String folderBasedir = getController().getFolderRepository()
            .getFoldersBasedir();

        List<FolderInfo> folderInfoList = (List<FolderInfo>) getWizardContext()
            .getAttribute(WizardContextAttributes.FOLDER_INFOS);

        // If we have just one folder info, display as text field,
        // BUT still have the combo, as this is linked to the maps.
        if (folderInfoList.size() == 1) {
            folderInfoField.setText(folderInfoList.get(0).getName());
            folderInfoCombo.setVisible(false);
        } else {
            folderInfoField.setVisible(false);
        }

        for (FolderInfo folderInfo : folderInfoList) {
            folderProfileMap.put(folderInfo,
                SyncProfile.AUTOMATIC_SYNCHRONIZATION);
            // Suggest user dir.
            File dirSuggestion;
            if (userDirs.get(folderInfo.name) == null) {
                dirSuggestion = new File(folderBasedir,
                    FileUtils.removeInvalidFilenameChars(folderInfo.name));
            } else {
                dirSuggestion = userDirs.get(folderInfo.name).getDirectory();
            }
            folderLocalBaseMap.put(folderInfo, dirSuggestion);
            folderInfoComboModel.addElement(folderInfo.name);
        }
    }

    protected String getTitle() {
        return Translation
            .getTranslation("wizard.multi_online_storage_setup.title");
    }

    /**
     * Update name and profile fields when base selection changes.
     */
    private void folderInfoComboSelectionChange() {
        changingSelecton = true;

        Object selectedItem = folderInfoCombo.getSelectedItem();
        FolderInfo selectedFolderInfo = null;
        for (FolderInfo folderInfo : folderProfileMap.keySet()) {
            if (folderInfo.name.equals(selectedItem)) {
                selectedFolderInfo = folderInfo;
                break;
            }
        }
        if (selectedFolderInfo != null) {
            localFolderField.setText(folderLocalBaseMap.get(selectedFolderInfo)
                .getAbsolutePath());
            syncProfileSelectorPanel.setSyncProfile(
                folderProfileMap.get(selectedFolderInfo), false);
        }

        changingSelecton = false;
    }

    private void syncProfileSelectorPanelChange() {
        if (!changingSelecton) {
            Object selectedItem = folderInfoCombo.getSelectedItem();
            FolderInfo selectedFolderInfo = null;
            for (FolderInfo folderInfo : folderProfileMap.keySet()) {
                if (folderInfo.name.equals(selectedItem)) {
                    selectedFolderInfo = folderInfo;
                    break;
                }
            }
            if (selectedFolderInfo != null) {
                folderProfileMap.put(selectedFolderInfo,
                    syncProfileSelectorPanel.getSyncProfile());
            }
        }
    }

    private class MyItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            folderInfoComboSelectionChange();
        }
    }

    private class MyPropertyValueChangeListener implements
        PropertyChangeListener
    {

        public void propertyChange(PropertyChangeEvent evt) {
            syncProfileSelectorPanelChange();
        }
    }

    private void configureLocalFolder() {
        Object selectedItem = folderInfoCombo.getSelectedItem();
        FolderInfo selectedFolderInfo = null;
        for (FolderInfo folderInfo : folderLocalBaseMap.keySet()) {
            if (folderInfo.name.equals(selectedItem)) {
                selectedFolderInfo = folderInfo;
                break;
            }
        }
        if (selectedFolderInfo != null) {
            List<File> files = DialogFactory.chooseDirectory(getController()
                .getUIController(), folderLocalBaseMap.get(selectedFolderInfo),
                    false);
            if (!files.isEmpty()) {
                File file = files.get(0);
                localFolderField.setText(file.getAbsolutePath());
                folderLocalBaseMap.put(selectedFolderInfo, file);
            }
        }
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(localFolderButton)) {
                configureLocalFolder();
            }
        }
    }
}