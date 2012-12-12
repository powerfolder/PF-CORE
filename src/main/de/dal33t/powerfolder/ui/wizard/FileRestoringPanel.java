/*
 * Copyright 2004 - 2012 Christian Sprajc. All rights reserved.
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
 * $Id: FileRestoringPanel.java 19700 2012-09-01 04:48:56Z glasgow $
 */
package de.dal33t.powerfolder.ui.wizard;

import jwf.WizardPanel;

import javax.swing.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.FolderService;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FileArchiver;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;

public class FileRestoringPanel extends PFWizardPanel {

    private static final Logger log = Logger.getLogger(MultiFileRestorePanel.class.getName());

    private final Folder folder;
    private final List<FileInfo> fileInfosToRestore;
    private final JLabel statusLabel;
    private final JProgressBar bar;
    private final File alternateDirectory;
    private SwingWorker<List<FileInfo>, FileInfo> worker;
    private int filesProcessedSuccessfully;

    public FileRestoringPanel(Controller controller, Folder folder, List<FileInfo> fileInfosToRestore,
                              File alternateDirectory) {
        super(controller);
        this.folder = folder;
        this.fileInfosToRestore = fileInfosToRestore;
        bar = new JProgressBar(0, 100);
        statusLabel = new JLabel();
        this.alternateDirectory = alternateDirectory;
    }

    public FileRestoringPanel(Controller controller, Folder folder, List<FileInfo> fileInfosToRestore) {
        this(controller, folder, fileInfosToRestore, null);
    }

    protected JComponent buildContent() {
        FormLayout layout = new FormLayout("140dlu, 3dlu, pref:grow",
            "pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        builder.add(statusLabel, cc.xy(1, 1));

        builder.add(bar, cc.xy(1, 3));

        return builder.getPanel();
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.file_restoring.title");
    }

    protected void initComponents() {
        bar.setIndeterminate(true);
    }

    public boolean hasNext() {
        return false;
    }

    public boolean canGoBackTo() {
        // Don't let user repeat this step. Need to start process again.
        return false;
    }

    public WizardPanel next() {
        String message = Translation.getTranslation("wizard.file_restoring.processed_success.text",
                String.valueOf(filesProcessedSuccessfully));
        if (filesProcessedSuccessfully < fileInfosToRestore.size()) {
            // Some failures.
            message += "\n\n" + Translation.getTranslation("wizard.file_restoring.processed_fail.text",
                    String.valueOf(fileInfosToRestore.size() - filesProcessedSuccessfully));
        }
        return new TextPanelPanel(getController(), Translation.getTranslation("wizard.file_restoring.processed.title"),
                message);
    }

    @Override
    protected void afterDisplay() {
        if (worker != null) {
            worker.cancel(false);
        }
        worker = new RestoreWorker();
        worker.execute();
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class RestoreWorker extends SwingWorker<List<FileInfo>, FileInfo> {

        private int fileInfosProcessed;

        protected List<FileInfo> doInBackground() {
            filesProcessedSuccessfully = 0;
            List<FileInfo> results = new ArrayList<FileInfo>();
            for (FileInfo fileInfo : fileInfosToRestore) {
                results.add(fileInfo);
                restore(fileInfo);
            }
            return results;
        }

        private void restore(FileInfo fileInfo) {
            try {
                File restoreTo;
                boolean alternate = alternateDirectory != null &&
                        alternateDirectory.exists() &&
                        alternateDirectory.canWrite();
                if (alternate) {
                    restoreTo = new File(alternateDirectory, fileInfo.getFilenameOnly());
                } else {
                    restoreTo = fileInfo.getDiskFile(getController().getFolderRepository());
                }
                FileArchiver fileArchiver = folder.getFileArchiver();
                if (fileArchiver.restore(fileInfo, restoreTo)) {
                    folder.scanChangedFile(fileInfo);
                    folder.scanAllParentDirectories(fileInfo);
                    log.info("Restored " + fileInfo.getFilenameOnly() + " from local archive");
                    filesProcessedSuccessfully++;
                } else if (folder.hasMember(getController().getOSClient().getServer())) {
                    ServerClient client = getController().getOSClient();
                    if (client.isConnected() && client.isLoggedIn()) {
                        // Doesn't work :-(
                        // FolderService service = client.getFolderService();
                        // FileInfo onlineRestoredFileInfo = service.restore(fileInfo, restoreTo);
                        // log.info("Restored " + onlineRestoredFileInfo.toDetailString() + " from OS archive");
                        filesProcessedSuccessfully++;
                    }
                } else {
                    log.info("Failed to restore " + fileInfo.getFilenameOnly());
                }

                publish(fileInfo);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to restore " + fileInfo.getFilenameOnly(), e);
            }
        }

        /**
         * Intermediate results, tap the progress bar.
         *
         * @param chunks
         */
        protected void process(List<FileInfo> chunks) {
            fileInfosProcessed += chunks.size();   
            if (fileInfosToRestore.isEmpty()) {
                bar.setIndeterminate(true);
            } else {
                bar.setIndeterminate(false);
                statusLabel.setText(Translation.getTranslation("general.processed", String.valueOf(fileInfosProcessed),
                        String.valueOf(fileInfosToRestore.size())));
                bar.setValue(100 * fileInfosProcessed / fileInfosToRestore.size());
            }
        }

        /**
         * All done, show the results.
         */
        protected void done() {
            getWizard().next();
        }
    }
}
