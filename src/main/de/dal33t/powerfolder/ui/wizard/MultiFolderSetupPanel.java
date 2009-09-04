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
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_CREATE_ITEMS;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.util.ui.ArchiveModeSelectorPanel;
import jwf.WizardPanel;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to do folder creations for optional specified FolderCreateItems.
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class MultiFolderSetupPanel extends PFWizardPanel {

    private List<FolderCreateItem> folderCreateItems;
    private JComboBox localBaseCombo;
    private DefaultComboBoxModel localBaseComboModel;

    private FolderCreateItem selectedItem;
    private ValueModel modeModel;
    private ValueModel versionsModel;

    private JTextField nameField;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;

    private ArchiveModeSelectorPanel archiveModeSelectorPanel;

    /**
     * Constuctor
     *
     * @param controller
     */
    public MultiFolderSetupPanel(Controller controller) {
        super(controller);

    }

    /**
     * Can procede if an invitation exists.
     */
    public boolean hasNext() {
        return true;
    }

    public boolean validateNext() {

        // Check that all folders have names.
        for (FolderCreateItem folderCreateItem : folderCreateItems) {
            if (folderCreateItem.getFolderInfo().name == null
                    || folderCreateItem.getFolderInfo().name.length() == 0) {
                DialogFactory.genericDialog(getController(), Translation
                        .getTranslation("wizard.multi_folder_setup.no_name.title"),
                        Translation.getTranslation(
                                "wizard.multi_folder_setup.no_name.text",
                                folderCreateItem.getLocalBase().getAbsolutePath()),
                        GenericDialogType.ERROR);
                return false;
            }
        }
        return true;
    }

    public WizardPanel next() {

        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
                Translation.getTranslation("wizard.setup_success"), Translation
                        .getTranslation("wizard.success_join"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        getWizardContext().setAttribute(
                WizardContextAttributes.SAVE_INVITE_LOCALLY, Boolean.TRUE);

        return new FolderCreatePanel(getController());
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
                "right:pref, 3dlu, 140dlu, pref:grow",
                "pref, 6dlu, pref, 6dlu, pref, 6dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addLabel(Translation.getTranslation("general.directory"), cc
                .xy(1, 1));
        builder.add(localBaseCombo, cc.xy(3, 1));

        builder.addLabel(Translation.getTranslation("general.folder_name"), cc
                .xy(1, 3));
        builder.add(nameField, cc.xy(3, 3));

        builder.add(new JLabel(Translation
                .getTranslation("general.transfer_mode")), cc.xy(1, 5));
        JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
        p.setOpaque(false);
        builder.add(p, cc.xyw(3, 5, 2));

        builder.add(new JLabel(Translation
                .getTranslation("general.archive_mode")), cc.xy(1, 7));
        builder.add(archiveModeSelectorPanel.getUIComponent(), cc.xyw(3, 7, 2));

        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {

        syncProfileSelectorPanel = new SyncProfileSelectorPanel(getController());
        syncProfileSelectorPanel
                .addModelValueChangeListener(new MyPropertyValueChangeListener());

        KeyListener myKeyListener = new MyKeyListener();

        folderCreateItems = new ArrayList<FolderCreateItem>();

        localBaseComboModel = new DefaultComboBoxModel();
        localBaseCombo = new JComboBox(localBaseComboModel);

        nameField = new JTextField();
        nameField.addKeyListener(myKeyListener);

        PropertyChangeListener listener = new MyPropertyChangeListener();
        modeModel = new ValueHolder();
        versionsModel = new ValueHolder();
        modeModel.addValueChangeListener(listener);
        versionsModel.addValueChangeListener(listener);
        archiveModeSelectorPanel = new ArchiveModeSelectorPanel(getController(),
                modeModel, versionsModel);

        MyItemListener itemListener = new MyItemListener();

        localBaseCombo.addItemListener(itemListener);
    }

    public void afterDisplay() {
        localBaseComboModel.removeAllElements();
        Object attribute = getWizardContext().getAttribute(FOLDER_CREATE_ITEMS);
        if (attribute != null && attribute instanceof List) {
            List list = (List) attribute;
            for (Object o : list) {
                if (o instanceof FolderCreateItem) {
                    FolderCreateItem item = (FolderCreateItem) o;

                    // Create folder info if none exists.
                    if (item.getFolderInfo() == null) {
                        createFolderInfo(item);
                    }
                    folderCreateItems.add(item);
                    localBaseComboModel.addElement(item.getLocalBase()
                            .getAbsolutePath());
                }
            }
        }

        localBaseComboSelectionChanged();
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.multi_folder_setup.title");
    }

    /**
     * Update name and profile fields when base selection changes.
     */
    private void localBaseComboSelectionChanged() {
        String dirName = (String) localBaseComboModel.getSelectedItem();
        for (FolderCreateItem item : folderCreateItems) {
            if (item.getLocalBase().getAbsolutePath().equals(dirName)) {
                selectedItem = item;
                FolderInfo folderInfo = item.getFolderInfo();
                nameField.setText(folderInfo.name);
                SyncProfile profile = item.getSyncProfile();
                if (profile == null) {
                    profile = SyncProfile.AUTOMATIC_SYNCHRONIZATION;
                }
                syncProfileSelectorPanel.setSyncProfile(profile, false);
                break;
            }
        }
    }

    /**
     * Create folder info now if none exists, and assign to item.
     *
     * @param item
     * @return
     */
    private static void createFolderInfo(FolderCreateItem item) {
        // Default sync folder has user name...
        String name = item.getLocalBase().getName();
        FolderInfo folderInfo = new FolderInfo(name,
                '[' + IdGenerator.makeId() + ']');
        item.setFolderInfo(folderInfo);
    }

    private void syncProfileSelectorPanelChange() {
        if (selectedItem != null) {
            selectedItem.setSyncProfile(syncProfileSelectorPanel
                    .getSyncProfile());
        }
    }

    private void updateModeAndVersion() {
        if (selectedItem != null) {
            ArchiveMode am = (ArchiveMode) modeModel.getValue();
            if (am == ArchiveMode.NO_BACKUP) {
                selectedItem.setArchiveMode(ArchiveMode.NO_BACKUP);
            } else {
                selectedItem.setArchiveMode(ArchiveMode.FULL_BACKUP);
                int version = (Integer) versionsModel.getValue();
                selectedItem.setArchiveHistory(version);
            }
        }
    }

    // /////////////
    // Inner Classes
    // /////////////

    private class MyItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            if (e.getSource() == localBaseCombo) {
                localBaseComboSelectionChanged();
            }
        }
    }

    private class MyKeyListener implements KeyListener {
        public void keyTyped(KeyEvent e) {
        }

        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
            if (selectedItem != null) {
                selectedItem.getFolderInfo().name = nameField.getText();
            }
        }
    }

    private class MyPropertyValueChangeListener implements
            PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            syncProfileSelectorPanelChange();
        }
    }

    private class MyPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() == modeModel || evt.getSource() == versionsModel) {
                updateModeAndVersion();
            }
        }
    }

}