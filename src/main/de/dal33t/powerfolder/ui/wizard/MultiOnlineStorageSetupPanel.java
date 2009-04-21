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
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;
import de.javasoft.synthetica.addons.DirectoryChooser;
import jwf.WizardPanel;

import javax.swing.*;
import java.util.*;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.File;

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

        List<FolderCreateItem> folderCreateItems =
                new ArrayList<FolderCreateItem>();

        for (FolderInfo folderInfo : folderProfileMap.keySet()) {
            SyncProfile sp = folderProfileMap.get(folderInfo);
            File localBase = folderLocalBaseMap.get(folderInfo);
            FolderCreateItem fci = new FolderCreateItem(localBase);
            fci.setSyncProfile(sp);
            fci.setFolderInfo(folderInfo);
            folderCreateItems.add(fci);
        }

        getWizardContext().setAttribute(WizardContextAttributes.
                FOLDER_CREATE_ITEMS, folderCreateItems);

        getWizardContext().setAttribute(
            WizardContextAttributes.SAVE_INVITE_LOCALLY, false);


        return new FolderCreatePanel(getController());
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("right:pref, 3dlu, 122dlu, 3dlu, 15dlu, pref:grow",
            "pref, 6dlu, pref, 6dlu, pref, 6dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addLabel(Translation.getTranslation("general.directory"),
                cc.xy(1, 1));
        builder.add(folderInfoCombo, cc.xyw(3, 1, 3));

        builder.add(new JLabel(Translation.getTranslation(
                "wizard.multi_online_storage_setup.local_folder_location")),
                cc.xy(1, 3));
        builder.add(localFolderField, cc.xy(3, 3));
        builder.add(localFolderButton, cc.xy(5, 3));

        builder.add(new JLabel(Translation
            .getTranslation("general.transfer_mode")), cc.xy(1, 5));
        JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
        p.setOpaque(false);
        builder.add(p, cc.xyw(3, 5, 4));

        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {

        localFolderField = new JTextField();
        localFolderField.setEditable(false);
        localFolderButton = new JButtonMini(Icons.getIconById(Icons.DIRECTORY),
                Translation.getTranslation(
                        "wizard.multi_online_storage_setup.select_directory"));
        MyActionListener myActionListener = new MyActionListener();
        localFolderButton.addActionListener(myActionListener);

        syncProfileSelectorPanel = new SyncProfileSelectorPanel(getController());
        syncProfileSelectorPanel.addModelValueChangeListener(
                new MyPropertyValueChangeListener());

        folderInfoComboModel = new DefaultComboBoxModel();
        folderInfoCombo = new JComboBox(folderInfoComboModel);

        folderInfoCombo.addItemListener(new MyItemListener());

        getWizardContext().setAttribute(PFWizard.PICTO_ICON,
            Icons.FILE_SHARING_PICTO);
    }

    /**
     * Build map of foInfo and syncProfs
     */
    public void afterDisplay() {
        folderProfileMap = new HashMap<FolderInfo, SyncProfile>();
        folderLocalBaseMap = new HashMap<FolderInfo, File>();
        String folderBasedir = ConfigurationEntry.FOLDER_BASEDIR.getValue(getController());

        List<FolderInfo> folderInfoList = (List<FolderInfo>) getWizardContext()
                .getAttribute(WizardContextAttributes.FOLDER_INFOS);
        for (FolderInfo folderInfo : folderInfoList) {
            folderProfileMap.put(folderInfo,
                    SyncProfile.AUTOMATIC_SYNCHRONIZATION);
            folderLocalBaseMap.put(folderInfo, new File(folderBasedir,
                    folderInfo.name));
            folderInfoComboModel.addElement(folderInfo.name);
        }
    }

    protected JComponent getPictoComponent() {
        return new JLabel(getContextPicto());
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.multi_online_storage_setup.title");
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
            localFolderField.setText(folderLocalBaseMap.get(
                    selectedFolderInfo).getAbsolutePath());
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
            PropertyChangeListener {

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
            DirectoryChooser dc = new DirectoryChooser();
            dc.setCurrentDirectory(folderLocalBaseMap.get(selectedFolderInfo));
            int i = dc.showOpenDialog(getController().getUIController().getActiveFrame());
            if (i == JFileChooser.APPROVE_OPTION) {
                File selectedFile = dc.getSelectedFile();
                localFolderField.setText(selectedFile.getAbsolutePath());
                folderLocalBaseMap.put(selectedFolderInfo, selectedFile);
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