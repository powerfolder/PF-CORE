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
 * $Id: PreviewToJoinDialog.java 20999 2013-03-11 13:19:11Z glasgow $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderPreviewHelper;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.ui.panel.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * Panel displayed when wanting to move a folder from preview to join
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow </a>
 * @version $Revision: 2.3 $
 */
public class PreviewToJoinDialog extends BaseDialog {

    private final Folder folder;
    private JButton joinButton;
    private JButton cancelButton;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;
    private ValueModel locationModel;
    private JTextField locationTF;

    /**
     * Contructor when used on choosen folder
     *
     * @param controller
     * @param folder
     */
    public PreviewToJoinDialog(Controller controller, Folder folder) {
        super(Senior.NONE, controller, true);
        Reject.ifFalse(folder.isPreviewOnly(), "Folder should be a preview");
        this.folder = folder;
    }

    /**
     * Initalizes all ui components
     */
    private void initComponents() {

        final FolderSettings existingFoldersSettings = FolderSettings.load(
            getController(), folder.getConfigEntryId());

        syncProfileSelectorPanel = new SyncProfileSelectorPanel(
            getController(), existingFoldersSettings.getSyncProfile());

        locationModel = new ValueHolder(existingFoldersSettings
            .getLocalBaseDir().toAbsolutePath().toString());

        // Behavior
        locationModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                locationTF.setText((String) evt.getNewValue());
            }
        });

        // Buttons
        joinButton = new JButton(Translation.getTranslation("folder_join.join"));
        joinButton.setMnemonic(Translation
            .getTranslation("folder_join.join.key").trim().charAt(0));

        joinButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                // Dispose before parent is closed.
                close();

                FolderSettings newFolderSettings = new FolderSettings(Paths
                    .get((String) locationModel.getValue()),
                    syncProfileSelectorPanel.getSyncProfile(),
                    existingFoldersSettings.getDownloadScript(),
                    existingFoldersSettings.getVersions(),
                    existingFoldersSettings.isSyncPatterns());

                FolderPreviewHelper.convertFolderFromPreview(getController(),
                    folder, newFolderSettings, false);
            }
        });

        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }

    // Methods for BaseDialog *************************************************

    @Override
    public String getTitle() {
        return Translation.getTranslation("folder_join.dialog.title",
            folder.getName());
    }

    @Override
    protected Icon getIcon() {
        return null;
    }

    @Override
    protected JComponent getContent() {
        initComponents();

        FormLayout layout = new FormLayout(
            "right:pref, 3dlu, max(140dlu;pref):grow", "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.addLabel(Translation.getTranslation("general.synchonisation"),
            cc.xy(1, row));
        builder.add(syncProfileSelectorPanel.getUIComponent(), cc.xy(3, row));

        row += 2;

        builder.addLabel(Translation.getTranslation("general.local_copy_at"),
            cc.xy(1, row));
        builder.add(createLocationField(), cc.xy(3, row));

        row += 2;

        return builder.getPanel();
    }

    @Override
    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(joinButton, cancelButton);
    }

    protected JButton getDefaultButton() {
        return joinButton;
    }

    /**
     * Creates a pair of location text field and button.
     *
     * @return
     */
    private JComponent createLocationField() {
        FormLayout layout = new FormLayout("100dlu, 3dlu, 15dlu", "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        locationTF = new JTextField();
        locationTF.setEditable(false);
        locationTF.setText((String) locationModel.getValue());
        builder.add(locationTF, cc.xy(1, 1));

        JButtonMini locationButton = new JButtonMini(
            Icons.getIconById(Icons.DIRECTORY),
            Translation.getTranslation("folder_join.location.tip"));
        locationButton.addActionListener(new MyActionListener());
        builder.add(locationButton, cc.xy(3, 1));
        return builder.getPanel();
    }

    /**
     * Action listener for the location button. Opens a choose dir dialog and
     * sets the location model with the result.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String initial = (String) locationModel.getValue();
            List<Path> files = DialogFactory.chooseDirectory(getController()
                .getUIController(), initial, false);
            if (!files.isEmpty()) {
                locationModel.setValue(files.get(0).toAbsolutePath().toString());
            }
        }
    }
}