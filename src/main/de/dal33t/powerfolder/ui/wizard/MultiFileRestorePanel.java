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
 * $Id: MultiFileRestorePanel.java 19932 2012-10-14 06:01:18Z glasgow $
 */
package de.dal33t.powerfolder.ui.wizard;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.FolderService;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.wizard.table.MultiFileRestoreTable;
import de.dal33t.powerfolder.ui.wizard.table.MultiFileRestoreTableModel;
import de.dal33t.powerfolder.light.FileInfo;

import javax.swing.*;

import jwf.WizardPanel;

import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.awt.*;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;

/**
 * Call this class via PFWizard.
 */
public class MultiFileRestorePanel extends PFWizardPanel {

    private final Folder folder;
    private final List<FileInfo> fileInfosToRestore;
    private final JLabel infoLabel;
    private final JLabel warningLabel;

    private JProgressBar bar;
    private JScrollPane scrollPane;
    private boolean hasNext;
    private SwingWorker<List<FileInfo>, FileInfo> worker;
    private MultiFileRestoreTableModel tableModel = new MultiFileRestoreTableModel(getController());

    public MultiFileRestorePanel(Controller controller, Folder folder, List<FileInfo> fileInfosToRestore) {
        super(controller);
        this.folder = folder;
        this.fileInfosToRestore = fileInfosToRestore;
        infoLabel = new JLabel();
        warningLabel = new JLabel();
    }

    protected JComponent buildContent() {
        FormLayout layout = new FormLayout("140dlu, pref:grow", "pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(infoLabel, cc.xyw(1, 1, 2));

        // bar and scrollPane share the same row.
        builder.add(scrollPane, cc.xyw(1, 3, 2));
        builder.add(bar, cc.xy(1, 3));

        builder.add(warningLabel, cc.xyw(1, 5, 2));

        return builder.getPanel();
    }

    @Override
    protected void afterDisplay() {
        loadVersions();
    }

    private void loadVersions() {
        infoLabel.setText(Translation.getTranslation("wizard.multi_file_restore.retrieving.text"));
        hasNext = false;
        updateButtons();
        if (worker != null) {
            worker.cancel(false);
        }
        tableModel.setFileInfos(new ArrayList<FileInfo>());
        bar.setVisible(true);
        scrollPane.setVisible(false);

        worker = new VersionLoaderWorker();
        worker.execute();
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.multi_file_restore.title");
    }

    protected void initComponents() {
        tableModel = new MultiFileRestoreTableModel(getController());
        MultiFileRestoreTable table = new MultiFileRestoreTable(tableModel);
        scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        scrollPane.setVisible(false);
        UIUtil.removeBorder(scrollPane);
        UIUtil.setZeroWidth(scrollPane);

        bar = new JProgressBar(0, 100);
    }

    public boolean hasNext() {
        return hasNext;
    }

    public WizardPanel next() {
        return new FileRestoringPanel(getController(), folder, tableModel.getFileInfos());
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class VersionLoaderWorker extends SwingWorker<List<FileInfo>, FileInfo> {

        private int fileInfosProcessed;

        public List<FileInfo> doInBackground() {
            bar.setIndeterminate(true);
            bar.setValue(0);
            warningLabel.setText("");
            
            List<FileInfo> versions = new ArrayList<FileInfo>(fileInfosToRestore.size());
            try {

                // Also try getting versions from OnlineStorage.
                boolean online = folder.hasMember(getController().getOSClient().getServer());
                FolderService folderService = null;
                if (online) {
                    ServerClient client = getController().getOSClient();
                    if (client != null && client.isConnected() && client.isLoggedIn())
                    {
                        folderService = client.getFolderService();
                    }
                }

                List<FileInfo> fileInfos = new ArrayList<FileInfo>();
                fileInfos.addAll(fileInfosToRestore);

                FileArchiver fileArchiver = folder.getFileArchiver();
                for (FileInfo fileInfo : fileInfos) {

                    if (isCancelled()) {
                        return Collections.emptyList();
                    }

                    List<FileInfo> infoList = fileArchiver.getArchivedFilesInfos(fileInfo);
                    FileInfo mostRecent = null;
                    for (FileInfo info : infoList) {
                        if (isBetterVersion(mostRecent, info)) {
                            mostRecent = info;
                        }
                    }

                    if (folderService != null) {
                        try {
                            List<FileInfo> serviceList = folderService
                                .getArchivedFilesInfos(fileInfo);
                            for (FileInfo info : serviceList) {
                                if (isBetterVersion(mostRecent, info))
                                {
                                    mostRecent = info;
                                }
                            }
                        } catch (Exception e) {
                            // Maybe gone offline. No worries.
                        }
                    }

                    if (mostRecent != null) {
                        versions.add(mostRecent);
                        publish(mostRecent);
                    }
                }
            } catch (Exception e) {
                // Hmmmmm.
            }
            Collections.reverse(versions);
            return versions;
        }

        private boolean isBetterVersion(FileInfo mostRecent, FileInfo info) {
            return mostRecent == null || mostRecent.getVersion() < info.getVersion();
        }

        /**
         * Intermediate results. Tap the progress bar on.
         *
         * @param chunks
         */
        protected void process(List<FileInfo> chunks) {
            fileInfosProcessed += chunks.size();
            if (fileInfosToRestore.isEmpty()) {
                bar.setIndeterminate(true);
            } else {
                bar.setIndeterminate(false);
                bar.setValue(100 * fileInfosProcessed / fileInfosToRestore.size());
                infoLabel.setText(Translation.getTranslation("general.processed", String.valueOf(fileInfosProcessed), 
                        String.valueOf(fileInfosToRestore.size())));
            }
        }

        protected void done() {
            scrollPane.setVisible(true);
            bar.setVisible(false);
            hasNext = false;
            try {
                tableModel.setFileInfos(get());
                if (get().isEmpty()) {
                    infoLabel.setText(Translation.getTranslation("wizard.multi_file_restore.retrieved_none.text"));
                } else {
                    infoLabel.setText(Translation.getTranslation("wizard.multi_file_restore.retrieved.text"));
                    hasNext = true;
                    if (fileInfosProcessed < fileInfosToRestore.size()) {
                        warningLabel.setText(Translation.getTranslation("wizard.multi_file_restore.some.text"));
                    }
                }
            } catch (CancellationException e) {
                infoLabel.setText(Translation.getTranslation("wizard.multi_file_restore.retrieve_cancelled.text"));
            } catch (Exception e) {
                infoLabel.setText(Translation.getTranslation("wizard.multi_file_restore.retrieve_exception.text",
                        e.getMessage()));
            }

            updateButtons();
        }
    }
}
