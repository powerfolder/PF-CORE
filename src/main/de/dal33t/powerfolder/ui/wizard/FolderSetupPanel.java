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

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.INITIAL_FOLDER_NAME;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.PROMPT_TEXT_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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
import de.dal33t.powerfolder.ui.panel.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;

/**
 * Class to do folder creation for an optional specified folderInfo.
 * 
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class FolderSetupPanel extends PFWizardPanel {

    private JTextField folderNameTextField;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;

    /**
     * Constuctor
     * 
     * @param controller
     * @param folderName
     *            the recommended folder name.
     */
    public FolderSetupPanel(Controller controller) {
        super(controller);

    }

    /**
     * Can procede if an invitation exists.
     */
    public boolean hasNext() {
        return StringUtils.isNotBlank(folderNameTextField.getText());
    }

    public WizardPanel next() {

        // Set FolderInfo
        // NOTE this is more or less a copy of ConfirmDiskLocationPanel next(),
        // for experts.
        // Changes may need to be applied to both.
        FolderInfo folderInfo = new FolderInfo(folderNameTextField.getText()
            .trim(), IdGenerator.makeFolderId());
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, folderInfo);

        // Set sync profile
        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
            syncProfileSelectorPanel.getSyncProfile());

        // Setup choose disk location panel
        getWizardContext().setAttribute(PROMPT_TEXT_ATTRIBUTE,
                Translation.getTranslation(
                        "wizard.what_to_do.invite.select_local"));

        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
                Translation.getTranslation("wizard.setup_success"),
                Translation.getTranslation("wizard.success_join"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        return new FolderCreatePanel(getController());
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
            "right:pref, 3dlu, 140dlu, pref:grow",
            "pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Folder Name
        builder.add(new JLabel(Translation.getTranslation("file_info.name")),
            cc.xy(1, 1));
        builder.add(folderNameTextField, cc.xy(3, 1));

        // Sync
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            builder
                .add(
                    new JLabel(Translation
                        .getTranslation("general.transfer_mode")), cc.xy(1, 3));
            JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
            p.setOpaque(false);
            builder.add(p, cc.xyw(3, 3, 2));
        }
        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {
        // Folder name label
        folderNameTextField = SimpleComponentFactory.createTextField(true);
        String initialFolderName = (String) getWizardContext().getAttribute(
            INITIAL_FOLDER_NAME);
        if (initialFolderName != null) {
            folderNameTextField.setText(initialFolderName);
        }
        folderNameTextField.addKeyListener(new MyKeyListener());

        // Sync profile
        Object object = getWizardContext().getAttribute(SYNC_PROFILE_ATTRIBUTE);
        if (object != null && object instanceof SyncProfile) {
            syncProfileSelectorPanel = new SyncProfileSelectorPanel(
                getController(), (SyncProfile) object);
        } else {
            // Use default
            syncProfileSelectorPanel = new SyncProfileSelectorPanel(
                getController());
        }
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.folder_setup.title");
    }

    private class MyKeyListener implements KeyListener {
        public void keyPressed(KeyEvent e) {
            // Not implemented
        }

        public void keyReleased(KeyEvent e) {
            // Not implemented
        }

        public void keyTyped(KeyEvent e) {
            updateButtons();
        }
    }
}