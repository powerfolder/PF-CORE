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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.FolderService;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.problem.FileConflictProblem;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Translation;
import jwf.WizardPanel;

public class FileRestoringPanel extends PFWizardPanel {

    private static final Logger log = Logger.getLogger(MultiFileRestorePanel.class.getName());

    private final List<FileInfo> fileInfosToRestore;
    private final JLabel statusLabel;
    private final JProgressBar bar;
    private SwingWorker<List<FileInfo>, FileInfo> worker;
    private int filesProcessedSuccessfully;
    private Path alternateLocation;
    private String alternateName;

    public FileRestoringPanel(Controller controller, List<FileInfo> fileInfosToRestore) {
        super(controller);
        this.fileInfosToRestore = fileInfosToRestore;
        bar = new JProgressBar(0, 100);
        statusLabel = new JLabel();
    }

    /**
     * Either alternateLocation or alternateName can be non-null
     * @param controller
     * @param folder
     * @param fileInfo
     * @param alternateLocation
     * @param alternateName
     */
    private FileRestoringPanel(Controller controller, FileInfo fileInfo, Path alternateLocation,
                              String alternateName) {
        this(controller, Collections.singletonList(fileInfo));
        if (alternateLocation != null && alternateName != null) {
            throw new IllegalArgumentException("Can't have both alternates.");
        }
        this.alternateLocation = alternateLocation;
        this.alternateName = alternateName;
    }

    public FileRestoringPanel(Controller controller, Folder folder, FileInfo fileInfo) {
        this(controller, fileInfo, null, null);
    }

    public FileRestoringPanel(Controller controller, Folder folder, FileInfo fileInfo, Path alternateFile) {
        this(controller, fileInfo, alternateFile, null);
    }

    public FileRestoringPanel(Controller controller, Folder folder, FileInfo fileInfo, String alternateName) {
        this(controller, fileInfo, null, alternateName);
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
        return Translation.get("wizard.file_restoring.title");
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
        String message = Translation.get("wizard.file_restoring.processed_success.text",
                String.valueOf(filesProcessedSuccessfully));
        if (filesProcessedSuccessfully < fileInfosToRestore.size()) {
            // Some failures.
            message += "\n\n" + Translation.get("wizard.file_restoring.processed_fail.text",
                    String.valueOf(fileInfosToRestore.size() - filesProcessedSuccessfully));
        }
        return new TextPanelPanel(getController(), Translation.get("wizard.file_restoring.processed.title"),
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

                // If there is an alternateLocation, restore from the local archive to the new location.
                boolean restoreLocalToAlternateLocation = alternateLocation != null;

                // If there is an alternateName, restore from the server with the new name.
                boolean restoreServerToAlternateName = alternateName != null;

                Folder fo = fileInfo.getFolder(getController().getFolderRepository());
                if (fo == null) {
                    log.fine("Could not find folder for file " + fileInfo + " to restore.");
                    return;
                }
                FileArchiver fileArchiver = fo.getFileArchiver();
                boolean restored = false;

                // Try local restore first.
                if (!restoreServerToAlternateName) {
                    Path restoreTo;
                    if (restoreLocalToAlternateLocation) {
                        // Restore to an alternate location.
                        restoreTo = alternateLocation.resolve(fileInfo.getFilenameOnly());
                    } else {
                        // Just restore to existing location.
                        restoreTo = fileInfo.getDiskFile(getController().getFolderRepository());
                    }
                    restored = fileArchiver.restore(fileInfo, restoreTo);
                    if (restored) {
                        fo.scanChangedFile(fileInfo);
                        fo.scanAllParentDirectories(fileInfo);
                        log.info("Restored " + fileInfo.getFilenameOnly()
                            + " from local version history");
                        filesProcessedSuccessfully++;
                    }
                }

                // Try server restore if no local restore done.
                if (!restored && !restoreLocalToAlternateLocation) {
                    if (fo.hasMember(getController().getOSClient().getServer())) {
                        ServerClient client = getController().getOSClient();
                        if (client.isConnected() && client.isLoggedIn()) {
                            FolderService service = client.getFolderService();
                            String relativeName = fileInfo.getRelativeName();
                            String targetRelativeName;
                            if (restoreServerToAlternateName) {
                                // Change the file name to alternateName.
                                targetRelativeName = FileInfo.renameRelativeFileName(relativeName, alternateName);
                            } else {
                                // Just restore to existing location.
                                targetRelativeName = relativeName;
                            }
                            FileInfo onlineRestoredFileInfo = service.restore(fileInfo, targetRelativeName);
                            log.info("Restored " + onlineRestoredFileInfo.toDetailString() + " from cloud");
                            filesProcessedSuccessfully++;
                            restored = true;
                        }
                    }
                }

                if (restored) {
                    // PFC-2831:
                    removeFileConflictProblem(fo, fileInfo);
                } else {
                    log.warning(
                        "Failed to restore " + fileInfo.getFilenameOnly());
                }

                publish(fileInfo);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to restore " + fileInfo.getFilenameOnly(), e);
            }
        }

        private void removeFileConflictProblem(Folder fo, FileInfo fileInfo) {
            for (Problem problem : fo.getProblems()) {
                if (problem instanceof FileConflictProblem) {
                    FileConflictProblem fcp = (FileConflictProblem) problem;
                    if (fcp.getFileInfo().equals(fileInfo)) {
                        fo.removeProblem(problem);
                    }
                }
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
                statusLabel.setText(Translation.get("general.processed", String.valueOf(fileInfosProcessed),
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
