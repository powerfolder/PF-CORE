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
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.panel.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.util.Translation;

/**
 * Dialog for creatigng or editing profile configuration. User can select a
 * default profile and then adjust the configuration.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.01 $
 */
public class DeleteSyncProfileDialog extends BaseDialog
{

    private JButton deleteButton;

    private JComboBox syncProfilesCombo;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;

    /**
     * Constructor.
     *
     * @param controller
     * @param syncProfileSelectorPanel
     */
    public DeleteSyncProfileDialog(Controller controller,
        SyncProfileSelectorPanel syncProfileSelectorPanel)
    {
        super(Senior.NONE, controller, true);
        this.syncProfileSelectorPanel = syncProfileSelectorPanel;
    }

    /**
     * Gets the title of the dialog.
     *
     * @return
     */
    public String getTitle() {
        return Translation.getTranslation("dialog.delete_profile.title");
    }

    /**
     * Gets the icon for the dialog.
     *
     * @return
     */
    protected Icon getIcon() {
        return null;
    }

    /**
     * Creates the visual component.
     *
     * @return
     */
    protected JComponent getContent() {
        initComponents();
        FormLayout layout = new FormLayout(
            "right:pref, 3dlu, pref",
            "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        SyncProfile syncProfile = syncProfileSelectorPanel.getSyncProfile();

        // Message
        builder
            .add(
                new JLabel(Translation.getTranslation(
                    "transfer_mode.delete.profile",
                    (syncProfile.getName() + '?'))), cc.xyw(1, 1, 3));

        // Substitute
        builder.add(
            new JLabel(Translation
                .getTranslation("transfer_mode.substitute.profile")), cc.xy(1,
                3));
        builder.add(syncProfilesCombo, cc.xy(3, 3));

        return builder.getPanel();
    }

    /**
     * Initialize the dialog components.
     */
    private void initComponents() {

        SyncProfile initialProfile = syncProfileSelectorPanel.getSyncProfile();

        // Combo
        syncProfilesCombo = new JComboBox();
        for (SyncProfile syncProfile : SyncProfile.getSyncProfilesCopy()) {

            // Don't add the profile being deleted.
            if (!syncProfile.equals(initialProfile)) {
                syncProfilesCombo.addItem(syncProfile.getName());
            }
        }
    }

    /**
     * The Delete / Cancel buttons.
     *
     * @return
     */
    protected Component getButtonBar() {

        deleteButton = new JButton(Translation.getTranslation("general.delete"));
        deleteButton.setMnemonic(Translation.getTranslation("general.delete.key")
            .trim().charAt(0));
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deletePressed();
            }
        });

        JButton cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelPressed();
            }
        });

        return ButtonBarFactory.buildCenteredBar(deleteButton, cancelButton);
    }

    protected JButton getDefaultButton() {
        return deleteButton;
    }

    // Methods fo FolderPreferencesPanel **************************************

    /**
     * If user clicks delete, delete the profile.
     */
    private void deletePressed() {

        // Scan all folders and set new profile if it has the one to be deleted.
        SyncProfile oldProfile = syncProfileSelectorPanel.getSyncProfile();
        String newProfileName = (String) syncProfilesCombo.getSelectedItem();
        for (SyncProfile newProfile : SyncProfile.getSyncProfilesCopy()) {
            if (newProfile.getName().equals(newProfileName)) {

                // Found the required folder. Set in required folders.
                for (Folder folder : getController().getFolderRepository().getFolders()) {
                    if (folder.getSyncProfile().equals(oldProfile)) {
                        folder.setSyncProfile(newProfile);
                    }
                }

                // Set in the selector panel.
                syncProfileSelectorPanel.setSyncProfile(newProfile, true);

                // Delete the profile from the SyncProfile cache.
                SyncProfile.deleteProfile(oldProfile);

                // Finally, update the selector panel combo to remove the 
                // deleted profile from the list.
                syncProfileSelectorPanel.configureCombo(newProfile);

                close();
            }
        }
    }

    /**
     * User does not want to play.
     */
    private void cancelPressed() {
        close();
    }
}