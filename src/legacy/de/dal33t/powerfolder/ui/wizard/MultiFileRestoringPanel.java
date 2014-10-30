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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.FolderService;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.util.SwingWorker;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

/**
 * Dialog for restoring file versions.
 */
public class MultiFileRestoringPanel extends PFWizardPanel {

    private static final Logger log = Logger
        .getLogger(MultiFileRestoringPanel.class.getName());

    private final List<FileInfo> fileInfosToRestore;
    private final Folder folder;

    /** If no archive is available, redownload from peers. */
    private final boolean redownloadIfMissing;

    private JLabel statusLabel;
    private long successCount;
    private long totalCount;

    public MultiFileRestoringPanel(Controller controller, Folder folder,
        List<FileInfo> fileInfosToRestore, boolean redownloadIfMissing)
    {
        super(controller);
        this.fileInfosToRestore = fileInfosToRestore;
        this.folder = folder;
        this.redownloadIfMissing = redownloadIfMissing;
    }

    protected JComponent buildContent() {
        FormLayout layout = new FormLayout("140dlu, 3dlu, pref:grow",
            "pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.add(statusLabel, cc.xy(1, row));

        row += 2;
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        builder.add(bar, cc.xy(1, row));

        return builder.getPanel();
    }

    protected void afterDisplay() {
        SwingWorker worker = new RestoreWorker();
        worker.start();
    }

    protected String getTitle() {
        return Translation
            .getTranslation("wizard.multi_file_restore_panel.title");
    }

    protected void initComponents() {
        statusLabel = new JLabel("...");
    }

    public boolean hasNext() {
        return false;
    }

    public WizardPanel next() {
        if (successCount == totalCount) {
            return new TextPanelPanel(
                getController(),
                Translation
                    .getTranslation("wizard.multi_file_restoring_panel.success_title"),
                Translation
                    .getTranslation("wizard.multi_file_restoring_panel.success_text"),
                true);
        } else if (successCount > 0) {
            return new TextPanelPanel(
                getController(),
                Translation
                    .getTranslation("wizard.multi_file_restoring_panel.success_title"),
                Translation.getTranslation(
                    "wizard.multi_file_restoring_panel.partial_text", Format
                        .formatLong(successCount), Format
                        .formatLong(totalCount)));
        } else {
            return new TextPanelPanel(
                getController(),
                Translation
                    .getTranslation("wizard.multi_file_restoring_panel.fail_title"),
                Translation
                    .getTranslation("wizard.multi_file_restoring_panel.fail_text"));
        }
    }

    private class RestoreWorker extends de.dal33t.powerfolder.ui.util.SwingWorker {

        public Object construct() {
            int i = 1;
            for (FileInfo fileInfoToRestore : fileInfosToRestore) {
                statusLabel.setText(Translation.getTranslation(
                    "wizard.multi_file_restoring_panel.working", Format
                        .formatLong(i++), Format.formatLong(fileInfosToRestore
                        .size())));
                restore0(folder, fileInfoToRestore);
            }
            return null;
        }

        /**
         * Restore from the archiver, or failing that from online storage.
         * 
         * @param folder
         * @param fileInfoToRestore
         */
        private void restore0(Folder folder, FileInfo fileInfoToRestore) {
            try {
                File restoreTo = fileInfoToRestore.getDiskFile(getController().getFolderRepository());
                FileArchiver fileArchiver = folder.getFileArchiver();
                boolean restored = false;
                FileInfo onlineRestoredFileInfo = null;
                if (fileArchiver.restore(fileInfoToRestore, restoreTo)) {
                    log.info("Restored " + fileInfoToRestore.getFilenameOnly()
                        + " from local archive");
                    folder.scanChangedFile(fileInfoToRestore);
                    folder.scanAllParentDirectories(fileInfoToRestore);
                    restored = true;
                } else if (folder.hasMember(getController().getOSClient()
                    .getServer()))
                {
                    ServerClient client = getController().getOSClient();
                    if (client.isConnected() && client.isLoggedIn()) {
                        FolderService service = client.getFolderService();
                        onlineRestoredFileInfo = service.restore(
                            fileInfoToRestore, true);
                        log.info("Restored "
                            + onlineRestoredFileInfo.toDetailString()
                            + " from OS archive");
                        restored = true;
                    }
                }

                // If no archive available, just redownload from best source.
                if (redownloadIfMissing && !restored) {
                    // Delete from db, then request from peers.
                    folder.removeDeletedFileInfo(fileInfoToRestore);

                    getController().getTransferManager().downloadNewestVersion(
                        fileInfoToRestore, false);
                    // Backup request
                    getController().getFolderRepository().getFileRequestor()
                        .triggerFileRequesting(folder.getInfo());

                    log.info("Redownloading "
                        + fileInfoToRestore.getFilenameOnly());
                    restored = true;
                }

                // If restored online download file now.
                if (onlineRestoredFileInfo != null) {
                    getController().getTransferManager().downloadNewestVersion(
                        onlineRestoredFileInfo, false);
                }

                if (!restored) {
                    throw new IOException("Restore of "
                        + fileInfoToRestore.getFilenameOnly() + " failed");
                }
                successCount++;
            } catch (Exception e) {
                log.log(Level.WARNING, "Exception: " + e);
                log.log(Level.FINE, e.getMessage(), e);
            } finally {
                totalCount++;
            }
        }

        protected void afterConstruct() {
            getWizard().next();
        }
    }
}