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

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.wizard.data.FileInfoLocation;
import jwf.WizardPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.wizard.table.SingleFileRestoreTableModel;
import de.dal33t.powerfolder.ui.wizard.table.SingleFileRestoreTable;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CancellationException;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Call this class via PFWizard.
 */
public class SingleFileRestorePanel extends PFWizardPanel {

    private final Folder folder;
    private final FileInfo fileInfoToRestore;
    private final FileInfo selectedFileInfo;

    private final JLabel infoLabel;
    private boolean hasNext;
    private SwingWorker<List<FileInfoLocation>, FileInfoLocation> worker;
    private final JProgressBar bar;
    private JScrollPane scrollPane;
    private final SingleFileRestoreTableModel tableModel;
    private final SingleFileRestoreTable table;

    private final JRadioButton originalRadio;
    private final JLabel originalLabel;

    private final JRadioButton alternateRadio;
    private final JTextField alternateTF;
    private final JButton alternateButton;

    public SingleFileRestorePanel(Controller controller, Folder folder, FileInfo fileInfoToRestore) {
        this(controller, folder, fileInfoToRestore, null);
    }

    public SingleFileRestorePanel(Controller controller, Folder folder, FileInfo fileInfoToRestore,
                                  FileInfo selectedFileInfo) {
        super(controller);
        this.folder = folder;
        this.fileInfoToRestore = fileInfoToRestore;
        this.selectedFileInfo = selectedFileInfo;

        infoLabel = new JLabel();
        bar = new JProgressBar();
        tableModel = new SingleFileRestoreTableModel(getController());
        table = new SingleFileRestoreTable(tableModel);

        originalRadio = new JRadioButton(Translation.getTranslation("wizard.single_file_restore.original.text"));
        originalLabel = new JLabel();

        alternateRadio = new JRadioButton(Translation.getTranslation("wizard.single_file_restore.alternate.text"));
        alternateTF = new JTextField();
        alternateButton = new JButtonMini(Icons.getIconById(Icons.DIRECTORY),
                Translation.getTranslation("wizard.single_file_restore.select_directory.tip"));
    }

    protected JComponent buildContent() {
        FormLayout layout = new FormLayout("140dlu, pref:grow", "pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(infoLabel, cc.xyw(1, 1, 2));

        builder.add(buildLocationPanel(), cc.xyw(1, 3, 2));

        builder.add(bar, cc.xy(1, 5));
        builder.add(scrollPane, cc.xyw(1, 5, 2));

        return builder.getPanel();
    }

    private JComponent buildLocationPanel() {
        FormLayout layout = new FormLayout("pref, 3dlu, 140dlu, 3dlu, pref", "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(originalRadio, cc.xy(1, 1));
        builder.add(originalLabel, cc.xyw(3, 1, 3));
        
        builder.add(alternateRadio, cc.xy(1, 3));
        builder.add(alternateTF, cc.xy(3, 3));
        builder.add(alternateButton, cc.xy(5, 3));
        alternateButton.addActionListener(new MyActionListener());

        return builder.getPanel();
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.single_file_restore.title");
    }

    protected void initComponents() {
        bar.setIndeterminate(true);
        scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        scrollPane.setVisible(false);
        UIUtil.removeBorder(scrollPane);
        UIUtil.setZeroWidth(scrollPane);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new MyListSelectionListener());
        ButtonGroup bg = new ButtonGroup();
        bg.add(originalRadio);
        bg.add(alternateRadio);
        originalRadio.setSelected(true);
        originalLabel.setText(fileInfoToRestore.getDiskFile(getController().getFolderRepository()).getParent());
        alternateTF.setEditable(false);
        originalRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLocations();
            }
        });
        alternateRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLocations();
            }
        });
        updateLocations();
        updateOriginalAlternate(false);
    }

    private void updateLocations() {
        originalLabel.setEnabled(originalRadio.isSelected());
        alternateTF.setEnabled(alternateButton.isSelected());
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
        tableModel.setFileInfoLocations(new ArrayList<FileInfoLocation>());
        bar.setVisible(true);
        scrollPane.setVisible(false);

        worker = new VersionLoaderWorker();
        worker.execute();
    }


    public boolean hasNext() {
        return hasNext;
    }

    public WizardPanel next() {
        FileInfoLocation fileInfoLocation = table.getSelectedFileInfoLocation();
        if (fileInfoLocation != null) {
            List<FileInfo> list = new ArrayList<FileInfo>();
            list.add(fileInfoLocation.getFileInfo());
            if (alternateRadio.isSelected()) {
                String alternateDirectory = alternateTF.getText();
                if (alternateDirectory != null && alternateDirectory.trim().length() > 0) {
                    File alternateFile = new File(alternateDirectory.trim());
                    if (alternateFile.isDirectory() && alternateFile.canWrite()) {
                        return new FileRestoringPanel(getController(), folder, list, alternateFile);
                    }
                }
            }
            return new FileRestoringPanel(getController(), folder, list);
        }
        throw new IllegalStateException("Could not find the selected file info.");
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class VersionLoaderWorker extends SwingWorker<List<FileInfoLocation>, FileInfoLocation> {

        protected List<FileInfoLocation> doInBackground() {

            // Get local versions.
            FileArchiver fileArchiver = folder.getFileArchiver();
            List<FileInfo> localFileInfos = fileArchiver.getArchivedFilesInfos(fileInfoToRestore);
            List<FileInfoLocation> localFileInfoLocations = convertToFileInfoLocation(localFileInfos, false);

            // Set up the combined as the local ones.
            List<FileInfoLocation> combinedFileInfoLocations = new ArrayList<FileInfoLocation>();
            combinedFileInfoLocations.addAll(localFileInfoLocations);

            // Also try getting versions from OnlineStorage.
            FolderService folderService = null;
            Member server = getController().getOSClient().getServer();
            if (folder.hasMember(server)) {
                ServerClient client = getController().getOSClient();
                if (client != null && client.isConnected() && client.isLoggedIn()) {
                    folderService = client.getFolderService();
                }
            }
            if (folderService != null) {
                try {
                    List<FileInfo> onlineFileInfos = folderService.getArchivedFilesInfos(fileInfoToRestore);
                    List<FileInfoLocation> onlineFileInfoLocations = convertToFileInfoLocation(onlineFileInfos, true);
                    for (FileInfoLocation onlineFileInfoLocation : onlineFileInfoLocations) {
                        boolean fileInfoLocal = false;
                        for (FileInfoLocation localFileInfoLocation : localFileInfoLocations) {
                            if (onlineFileInfoLocation.getFileInfo().isVersionDateAndSizeIdentical(
                                    localFileInfoLocation.getFileInfo())) {
                                // Local and online, so set online status true.
                                fileInfoLocal = true;
                                onlineFileInfoLocation.setOnline(true);
                                break;
                            }
                        }

                        // Found an online version that is not local, so add.
                        if (!fileInfoLocal) {
                            combinedFileInfoLocations.add(new FileInfoLocation(onlineFileInfoLocation.getFileInfo(),
                                    false, true));
                        }
                    }
                } catch (Exception e) {
                    // Maybe gone offline. No worries.
                }
            }
            return combinedFileInfoLocations;
        }

        @Override
        protected void done() {
            try {
                if (get().isEmpty()) {
                    infoLabel.setText(Translation.getTranslation("wizard.single_file_restore.retrieved_none.text",
                            fileInfoToRestore.getFilenameOnly()));
                } else {
                    infoLabel.setText(Translation.getTranslation("wizard.single_file_restore.retrieved.text",
                            String.valueOf(get().size()), fileInfoToRestore.getFilenameOnly()));
                }
                List<FileInfoLocation> fileInfoLocations = get();
                Collections.sort(fileInfoLocations, new Comparator<FileInfoLocation>() {
                    public int compare(FileInfoLocation o1, FileInfoLocation o2) {
                        if (o1.getFileInfo().getVersion() == o2.getFileInfo().getVersion()) {
                            return o1.getLocation() - o2.getLocation();
                        }
                        return o1.getFileInfo().getVersion() - o2.getFileInfo().getVersion();
                    }
                });
                tableModel.setFileInfoLocations(fileInfoLocations);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        table.setSelectedFileInfo(selectedFileInfo);
                    }
                });
            } catch (CancellationException e) {
                infoLabel.setText(Translation.getTranslation("wizard.single_file_restore.retrieve_cancelled.text"));
            } catch (Exception e) {
                infoLabel.setText(Translation.getTranslation("wizard.single_file_restore.retrieve_exception.text",
                        e.getMessage()));
            }
            bar.setVisible(false);
            scrollPane.setVisible(true);
        }
    }

    private static List<FileInfoLocation> convertToFileInfoLocation(List<FileInfo> fileInfos, boolean online) {
        List<FileInfoLocation> results = new ArrayList<FileInfoLocation>(fileInfos.size());
        for (FileInfo fileInfo : fileInfos) {
            results.add(new FileInfoLocation(fileInfo, !online, online));
        }
        return results;
    }

    private class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            hasNext = table.getSelectedRow() >= 0;
            FileInfoLocation fileInfoLocation = table.getSelectedFileInfoLocation();
            if (fileInfoLocation != null) {
                boolean local = fileInfoLocation.isLocal();
                updateOriginalAlternate(local);
            }
            updateButtons();
        }
    }

    private void updateOriginalAlternate(boolean local) {
        // Can't restore to an alternate location if not online.
        alternateRadio.setEnabled(local);
        alternateButton.setEnabled(local);
        if (!local) {
            originalRadio.setSelected(true);
            alternateTF.setText("");
        }
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            // Help the user by ensuring alternate set is selected if the button is clicked.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    alternateRadio.setSelected(true);
                }
            });
            List<File> files = DialogFactory.chooseDirectory(getController().getUIController(), alternateTF.getText(),
                    false);
            if (files.isEmpty()) {
                return;
            }

            File file = files.get(0);
            alternateTF.setText(file.getPath());
        }
    }
}
