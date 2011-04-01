/*
 * Copyright 2004 - 2011 Christian Sprajc. All rights reserved.
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

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.*;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;
import jwf.WizardPanel;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

/**
 * Class that lets the user configure a new folder candidate found in the
 * localbase.
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class LoadCandidatePanel extends PFWizardPanel {

    private static final Logger log = Logger
            .getLogger(LoadCandidatePanel.class.getName());

    private JLabel locationHintLabel;
    private JTextField locationField;

    private JLabel folderHintLabel;
    private JTextField folderNameLabel;

    private JLabel estimatedSizeHintLabel;
    private JLabel estimatedSize;

    private JLabel syncProfileHintLabel;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;

    public LoadCandidatePanel(Controller controller) {
        super(controller);
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public WizardPanel next() {

        // Set sync profile
        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                syncProfileSelectorPanel.getSyncProfile());

        // Do not prompt for send invitation afterwards
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, false);

        getWizardContext().setAttribute(SAVE_INVITE_LOCALLY, Boolean.FALSE);

        return new FolderCreatePanel(getController());
    }

    @Override
    public boolean validateNext() {
        return true;
    }

    @Override
    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("pref, 3dlu, 140dlu, pref:grow",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, "
                        + "3dlu, pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Please select invite...
        builder.addLabel(Translation
                .getTranslation("wizard.load_candidate.select_file"), cc.xy(3, 1));

        // Invite selector
        builder.add(locationHintLabel, cc.xy(1, 3));
        builder.add(locationField, cc.xy(3, 3));

        // Folder
        builder.add(folderHintLabel, cc.xy(1, 5));
        builder.add(folderNameLabel, cc.xy(3, 5));

        // Est size
        builder.add(estimatedSizeHintLabel, cc.xy(1, 11));
        builder.add(estimatedSize, cc.xy(3, 11));

        // Sync
        builder.add(syncProfileHintLabel, cc.xy(1, 13));
        JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
        p.setOpaque(false);

        FormLayout layout2 = new FormLayout("pref, pref:grow", "pref");
        PanelBuilder builder2 = new PanelBuilder(layout2);
        builder2.add(p, cc.xy(1, 1));

        JPanel panel = builder2.getPanel();
        builder.add(panel, cc.xyw(3, 13, 2));
        panel.setOpaque(false);

        return builder.getPanel();
    }

    /**
     * Initalizes all nessesary components
     */
    @Override
    protected void initComponents() {

        ValueModel locationModel = new ValueHolder();

        // Invite behavior
        locationModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                loadInvitation((String) evt.getNewValue());
                updateButtons();
            }
        });

        // Invite selector
        locationHintLabel = new JLabel(Translation
                .getTranslation("general.directory"));
        locationField = new JTextField();
        locationField.setEnabled(false);

        // Folder name label
        folderHintLabel = new JLabel(Translation
                .getTranslation("general.folder_name"));
        folderHintLabel.setEnabled(false);
        folderNameLabel = new JTextField();

        // Estimated size
        estimatedSizeHintLabel = new JLabel(Translation
                .getTranslation("general.estimated_size"));
        estimatedSizeHintLabel.setEnabled(false);
        estimatedSize = SimpleComponentFactory.createLabel();

        // Sync profile
        syncProfileHintLabel = new JLabel(Translation
                .getTranslation("general.transfer_mode"));
        syncProfileHintLabel.setEnabled(false);
        syncProfileSelectorPanel = new SyncProfileSelectorPanel(getController());
        syncProfileSelectorPanel.setEnabled(false);
    }

    @Override
    protected String getTitle() {
        return Translation.getTranslation("wizard.load_invitation.select");
    }

    private void loadInvitation(String file) {
        if (file == null) {
            return;
        }
        estimatedSizeHintLabel.setEnabled(true);

        syncProfileHintLabel.setEnabled(true);
        syncProfileSelectorPanel.setEnabled(true);
    }
}