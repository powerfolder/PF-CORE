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

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.FolderService;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.ui.SwingWorker;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;

import jwf.WizardPanel;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;

import javax.swing.*;

/**
 * Dialog for restoring file versions.
 */
public class MultiFileRestoringPanel extends PFWizardPanel {

    private static final Logger log = Logger.getLogger(MultiFileRestoringPanel
            .class.getName());

    private final List<FileInfo> fileInfosToRestore;
    private final Folder folder;
    private final boolean redownload;

    private JLabel statusLabel;
    private long successCount;
    private long totalCount;

    public MultiFileRestoringPanel(Controller controller, Folder folder,
                                 List<FileInfo> fileInfosToRestore,
                                 boolean redownload) {
        super(controller);
        this.fileInfosToRestore = fileInfosToRestore;
        this.folder = folder;
        this.redownload = redownload;
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
        return Translation.getTranslation(
                "wizard.multi_file_restore_panel.title");
    }

    protected void initComponents() {
        statusLabel = new JLabel("...");
    }

    public boolean hasNext() {
        return false;
    }

    public WizardPanel next() {
        if (successCount == totalCount) {
            return new TextPanelPanel(getController(), Translation.getTranslation(
                    "wizard.multi_file_restoring_panel.success_title"), Translation
                    .getTranslation("wizard.multi_file_restoring_panel.success_text"
            ));
        } else if (successCount > 0) {
            return new TextPanelPanel(getController(), Translation.getTranslation(
                    "wizard.multi_file_restoring_panel.success_title"), Translation
                    .getTranslation("wizard.multi_file_restoring_panel.partial_text",
                    Format.formatLong(successCount),
                    Format.formatLong(totalCount)
            ));
        } else {
            return new TextPanelPanel(getController(), Translation.getTranslation(
                    "wizard.multi_file_restoring_panel.fail_title"), Translation
                    .getTranslation("wizard.multi_file_restoring_panel.fail_text"
            ));
        }
    }
    
    private class RestoreWorker extends SwingWorker {

        public Object construct() {
            int i = 1;
            for (FileInfo fileInfoToRestore : fileInfosToRestore) {
                statusLabel.setText(Translation
                    .getTranslation("wizard.multi_file_restoring_panel.working",
                        Format.formatLong(i++),
                        Format.formatLong(fileInfosToRestore.size())));
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
                File restoreTo = fileInfoToRestore.getDiskFile(getController()
                        .getFolderRepository());
                FileArchiver fileArchiver = folder.getFileArchiver();
                boolean restored = false;
                if (redownload) {
                    folder.removeDeletedFileInfo(fileInfoToRestore);
                    getController().getTransferManager().downloadNewestVersion(fileInfoToRestore, true);
                    restored = true;
                } else {
                    if (fileArchiver.restore(fileInfoToRestore, restoreTo)) {
                        log.info("Restored " + fileInfoToRestore.getFilenameOnly() +
                                " from local archive");
                        folder.scanChangedFile(fileInfoToRestore);
                        restored = true;
                    } else {
                        // Not local. OnlineStorage perhaps?
                        boolean online = folder.hasMember(getController()
                                .getOSClient().getServer());
                        if (online) {
                            ServerClient client = getController().getOSClient();
                            if (client != null && client.isConnected()
                                    && client.isLoggedIn()) {
                                FolderService service = client.getFolderService();
                                if (service != null) {
                                    service.restore(fileInfoToRestore, true);
                                    log.info("Restored " + fileInfoToRestore
                                            .getFilenameOnly()
                                            + " from OS archive");
                                    restored = true;
                                }
                            }
                        }
                    }
                }

                if (!restored) {
                    throw new IOException("Restore of " + fileInfoToRestore
                            .getFilenameOnly() + " failed");
                }
                successCount++;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Exception", e);
            } finally {
                totalCount++;
            }
        }

        protected void afterConstruct() {
            getWizard().next();
        }
    }
}