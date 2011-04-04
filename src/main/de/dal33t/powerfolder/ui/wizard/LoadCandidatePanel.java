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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.*;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SwingWorker;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;
import jwf.WizardPanel;

import javax.swing.*;
import java.io.File;
import java.util.logging.Level;
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

    private final File directory;

    private JLabel locationHintLabel;
    private JTextField locationField;

    private JLabel folderHintLabel;
    private JTextField folderNameLabel;

    private JLabel totalSizeLabel;

    private JLabel syncProfileHintLabel;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;

    private JCheckBox onlineStorageCB;

    public LoadCandidatePanel(Controller controller, File directory) {
        super(controller);
        this.directory = directory;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public WizardPanel next() {

        getWizardContext().setAttribute(FOLDER_LOCAL_BASE,
                directory);

        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                syncProfileSelectorPanel.getSyncProfile());

        getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE,
                onlineStorageCB.isSelected());

        getWizardContext().setAttribute(SAVE_INVITE_LOCALLY, false);

        return new FolderCreatePanel(getController());
    }

    @Override
    public boolean validateNext() {
        return true;
    }

    @Override
    protected void afterDisplay() {
        SwingWorker worker = new MyFolderSizeSwingWorker();
        worker.start();
    }

    @Override
    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("pref, 3dlu, 140dlu, pref:grow",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Directory location
        builder.add(locationHintLabel, cc.xy(1, 1));
        builder.add(locationField, cc.xy(3, 1));

        // Folder
        builder.add(folderHintLabel, cc.xy(1, 3));
        builder.add(folderNameLabel, cc.xy(3, 3));

        // Sync
        builder.add(syncProfileHintLabel, cc.xy(1, 5));
        JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
        p.setOpaque(false);

        FormLayout layout2 = new FormLayout("pref, pref:grow", "pref");
        PanelBuilder builder2 = new PanelBuilder(layout2);
        builder2.add(p, cc.xy(1, 1));

        JPanel panel = builder2.getPanel();
        builder.add(panel, cc.xyw(3, 5, 2));
        panel.setOpaque(false);

        // Total size
        builder.add(totalSizeLabel, cc.xy(3, 7));

        // Online storage
        builder.add(onlineStorageCB, cc.xy(3, 9));

        return builder.getPanel();
    }

    /**
     * Initalizes all nessesary components
     */
    @Override
    protected void initComponents() {
        // Invite selector
        locationHintLabel = new JLabel(Translation
                .getTranslation("general.directory"));
        locationField = new JTextField(directory.getAbsolutePath());
        locationField.setEditable(false);

        // Folder name label
        folderHintLabel = new JLabel(Translation
                .getTranslation("general.folder_name"));
        folderNameLabel = new JTextField(directory.getName());

        // Total size
        totalSizeLabel = new JLabel(Translation
                .getTranslation("wizard.load_candidate.total_directory_size"));

        // Sync profile
        syncProfileHintLabel = new JLabel(Translation
                .getTranslation("general.transfer_mode"));
        syncProfileSelectorPanel = new SyncProfileSelectorPanel(getController());

        onlineStorageCB = new JCheckBox(Translation.getTranslation(
                "wizard.load_candidate.backup_by_online_storage"));
        onlineStorageCB.setOpaque(false);

    }

    @Override
    protected String getTitle() {
        return Translation.getTranslation("wizard.load_candidate.select_file");
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private class MyFolderSizeSwingWorker extends SwingWorker {

        private int recursiveFileCount = 0;
        private long totalDirectorySize = 0;

        protected void beforeConstruct() {
            totalSizeLabel.setText(Translation.getTranslation(
                    "wizard.load_candidate.calculating_directory_size"));
        }

        public Object construct() {
            try {
                recursiveFileCount = 0;
                totalDirectorySize = 0;
                Long[] longs = FileUtils
                        .calculateDirectorySizeAndCount(directory);
                totalDirectorySize += longs[0];
                recursiveFileCount += longs[1];
            } catch (Exception e) {
                Logger.getAnonymousLogger().log(Level.WARNING, e.toString(), e);
            }
            return null;
        }

        public void finished() {
            totalSizeLabel.setText(Translation.getTranslation(
                    "wizard.choose_disk_location.total_directory_size",
                    Format.formatBytes(totalDirectorySize),
                    Format.formatLong(recursiveFileCount)));
            updateButtons();
        }
    }
}