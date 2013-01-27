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

import de.dal33t.powerfolder.ui.wizard.data.SingleFileRestoreItem;
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
    private SwingWorker<List<SingleFileRestoreItem>, Object> worker;
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
        tableModel.setFileInfos(new ArrayList<SingleFileRestoreItem>());
        bar.setVisible(true);
        scrollPane.setVisible(false);

        worker = new VersionLoaderWorker();
        worker.execute();
    }


    public boolean hasNext() {
        return hasNext;
    }

    public WizardPanel next() {
        FileInfo fileInfo = table.getSelectedRestoreItem().getFileInfo(); // @todo need to actually pass the restore item.
        if (fileInfo != null) {
            List<FileInfo> list = new ArrayList<FileInfo>();
            list.add(fileInfo);
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

    private class VersionLoaderWorker extends SwingWorker<List<SingleFileRestoreItem>, Object> {

        protected List<SingleFileRestoreItem> doInBackground() {

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

            List<FileInfo> tempArchiverFileInfos = fileArchiver.getArchivedFilesInfos(fileInfoToRestore);
            List<SingleFileRestoreItem> infoList = new ArrayList<SingleFileRestoreItem>();
            for (FileInfo fileInfo : tempArchiverFileInfos) {
                infoList.add(new SingleFileRestoreItem(fileInfo, true));
            }

            if (folderService != null) {
                try {
                    List<FileInfo> serviceList = folderService.getArchivedFilesInfos(fileInfoToRestore);
                    for (FileInfo serviceListInfo : serviceList) {
                        boolean haveIt = false;
                        for (SingleFileRestoreItem restoreItem : infoList) {
                            if (serviceListInfo.isVersionDateAndSizeIdentical(restoreItem.getFileInfo())) {
                                haveIt = true;
                                break;
                            }
                        }
                        if (!haveIt) {
                            infoList.add(new SingleFileRestoreItem(serviceListInfo, false));
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
                if (get().isEmpty()) {
                    infoLabel.setText(Translation.getTranslation("wizard.single_file_restore.retrieved_none.text",
                            fileInfoToRestore.getFilenameOnly()));
                } else {
                    infoLabel.setText(Translation.getTranslation("wizard.single_file_restore.retrieved.text",
                            String.valueOf(get().size()), fileInfoToRestore.getFilenameOnly()));
                }
                List<SingleFileRestoreItem> restoreItems = get();
                Collections.sort(restoreItems, new Comparator<SingleFileRestoreItem>() {
                    public int compare(SingleFileRestoreItem o1, SingleFileRestoreItem o2) {
                        return o1.getFileInfo().getVersion() - o2.getFileInfo().getVersion();
                    }
                });
                tableModel.setFileInfos(restoreItems);
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

    private class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            hasNext = table.getSelectedRow() >= 0;
            updateButtons();
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
