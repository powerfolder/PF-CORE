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
 * $Id: SingleFileRestorePanel.java 19700 2012-09-01 04:48:56Z glasgow $
 */
package de.dal33t.powerfolder.ui.wizard;

import jwf.WizardPanel;

import javax.swing.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.FolderService;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.light.FileInfo;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;

import java.util.List;
import java.util.concurrent.CancellationException;

/**
 * Call this class via PFWizard.
 */
public class SingleFileRestorePanel extends PFWizardPanel {

    private final Folder folder;
    private final FileInfo fileInfoToRestore;
    private final FileInfo selectedFileInfo; // Note - this may be null.

    private final JLabel infoLabel;
    private boolean hasNext;
    private SwingWorker worker;
    private final JProgressBar bar;

    public SingleFileRestorePanel(Controller controller, Folder folder, FileInfo fileInfoToRestore) {
        this(controller, folder, fileInfoToRestore, null);
    }

    public SingleFileRestorePanel(Controller controller, Folder folder, FileInfo fileInfoToRestore,
                                  FileInfo selectedFileInfo) {
        super(controller);
        this.folder = folder;
        this.fileInfoToRestore = fileInfoToRestore;
        this.selectedFileInfo = selectedFileInfo; // Note - this may be null.

        infoLabel = new JLabel();
        bar = new JProgressBar();
    }

    protected JComponent buildContent() {
        FormLayout layout = new FormLayout("140dlu, pref:grow", "pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(infoLabel, cc.xyw(1, 1, 2));

        builder.add(bar, cc.xy(1, 3));

        return builder.getPanel();
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.single_file_restore.title");
    }

    protected void initComponents() {
        bar.setIndeterminate(true);
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
//        tableModel.setFileInfos(new ArrayList<FileInfo>());
        bar.setVisible(true);
//        scrollPane.setVisible(false);

        worker = new VersionLoaderWorker();
        worker.execute();
    }


    public boolean hasNext() {
        return hasNext;
    }

    public WizardPanel next() {
        return null;
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class VersionLoaderWorker extends SwingWorker<List<FileInfo>, FileInfo> {

        protected List<FileInfo> doInBackground() throws Exception {

            // Also try getting versions from OnlineStorage.
            boolean online = folder.hasMember(getController().getOSClient().getServer());
            FolderService folderService = null;
            if (online) {
                ServerClient client = getController().getOSClient();
                if (client != null && client.isConnected() && client.isLoggedIn()) {
                    folderService = client.getFolderService();
                }
            }

            FileArchiver fileArchiver = folder.getFileArchiver();

            List<FileInfo> infoList = fileArchiver.getArchivedFilesInfos(fileInfoToRestore);

            if (folderService != null) {
                try {
                    List<FileInfo> serviceList = folderService.getArchivedFilesInfos(fileInfoToRestore);
                    for (FileInfo serviceListInfo : serviceList) {
                        boolean haveIt = false;
                        for (FileInfo infoListInfo : infoList) {
                            if (serviceListInfo.isVersionDateAndSizeIdentical(infoListInfo)) {
                                haveIt = true;
                                break;
                            }
                        }
                        if (!haveIt) {
                            infoList.add(serviceListInfo);
                        }
                    }
                } catch (Exception e) {
                    // Maybe gone offline. No worries.
                }
            }
            return infoList;
        }

        @Override
        protected void done() {
            try {
                if (get().size() == 0) {
                    infoLabel.setText(Translation.getTranslation("wizard.single_file_restore.retrieved_none.text",
                            fileInfoToRestore.getFilenameOnly()));
                } else {
                    //hasNext = true; @todo
                    infoLabel.setText(Translation.getTranslation("wizard.single_file_restore.retrieved.text",
                            String.valueOf(get().size())));
                }
            } catch (CancellationException e) {
                infoLabel.setText(Translation.getTranslation("wizard.single_file_restore.retrieve_cancelled.text"));
            } catch (Exception e) {
                infoLabel.setText(Translation.getTranslation("wizard.single_file_restore.retrieve_exception.text",
                        e.getMessage()));
            }
            bar.setVisible(false);
        }
    }
}
