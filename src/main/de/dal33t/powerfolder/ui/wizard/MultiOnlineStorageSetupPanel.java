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
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;
import jwf.WizardPanel;

import javax.swing.*;
import java.util.List;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Class to do configuration for optional specified OS joins.
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class MultiOnlineStorageSetupPanel extends PFWizardPanel {

    private List<FolderInfo> folderInfos;
    private JComboBox folderInfoCombo;
    private DefaultComboBoxModel folderInfoComboModel;

    private SyncProfileSelectorPanel syncProfileSelectorPanel;

    /**
     * Constuctor
     *
     * @param controller
     * @param folderName
     *            the recommended folder name.
     */
    public MultiOnlineStorageSetupPanel(Controller controller) {
        super(controller);

    }

    /**
     * Can procede if an invitation exists.
     */
    public boolean hasNext() {
        return true;
    }

    public boolean validateNext() {
        return true;
    }

    public WizardPanel next() {
        return new FolderCreatePanel(getController());
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("right:pref, 3dlu, 140dlu, pref:grow",
            "pref, 6dlu, pref, 6dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addLabel(Translation.getTranslation("general.directory"),
                cc.xy(1, 1));
        builder.add(folderInfoCombo, cc.xy(3, 1));

        builder.add(new JLabel(Translation
            .getTranslation("general.transfer_mode")), cc.xy(1, 5,
                CellConstraints.DEFAULT, CellConstraints.TOP));
        JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
        p.setOpaque(false);
        builder.add(p, cc.xyw(3, 5, 2));

        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {

        syncProfileSelectorPanel = new SyncProfileSelectorPanel(getController());
        syncProfileSelectorPanel.addModelValueChangeListener(
                new MyPropertyValueChangeListener());

        KeyListener myKeyListener = new MyKeyListener();

        folderInfoComboModel = new DefaultComboBoxModel();
        folderInfoCombo = new JComboBox(folderInfoComboModel);

        folderInfoCombo.addItemListener(new MyItemListener());

        getWizardContext().setAttribute(PFWizard.PICTO_ICON,
            Icons.FILE_SHARING_PICTO);
    }

    public void afterDisplay() {
        folderInfos = (List<FolderInfo>) getWizardContext().getAttribute(
                WizardContextAttributes.FOLDER_INFOS);
        for (FolderInfo folderInfo : folderInfos) {
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
    private void localBaseComboSelectionChange() {
    }

    private void syncProfileSelectorPanelChange() {
//        if (selectedItem != null) {
//            selectedItem.setSyncProfile(
//                    syncProfileSelectorPanel.getSyncProfile());
//        }
    }

    private class MyItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            localBaseComboSelectionChange();
        }
    }

    private class MyKeyListener implements KeyListener {
        public void keyTyped(KeyEvent e) {
        }

        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
//            if (selectedItem != null) {
//                selectedItem.getFolderInfo().name = nameField.getText();
//            }
        }
    }

    private class MyPropertyValueChangeListener implements
            PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            syncProfileSelectorPanelChange();
        }
    }
}