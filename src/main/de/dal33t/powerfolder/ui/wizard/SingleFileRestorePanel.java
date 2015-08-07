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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.wizard.data.SingleFileRestoreItem;
import de.dal33t.powerfolder.ui.wizard.table.SingleFileRestoreTable;
import de.dal33t.powerfolder.ui.wizard.table.SingleFileRestoreTableModel;
import de.dal33t.powerfolder.util.Translation;

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

    private final JRadioButton alternateLocationRadio;
    private final JTextField alternateLocationTF;
    private final JButton alternateLocationButton;

    private final JRadioButton alternateNameRadio;
    private final JTextField alternateNameTF;

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

        originalRadio = new JRadioButton(Translation.get("wizard.single_file_restore.original.text"));
        originalLabel = new JLabel();

        alternateLocationRadio = new JRadioButton(Translation.get("wizard.single_file_restore.alternate_location.text"));
        alternateLocationTF = new JTextField();
        alternateLocationButton = new JButtonMini(Icons.getIconById(Icons.DIRECTORY), Translation.get("wizard.single_file_restore.select_directory.tip"));

        alternateNameRadio = new JRadioButton(Translation.get("wizard.single_file_restore.alternate_name.text"));
        alternateNameTF = new JTextField();
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
        FormLayout layout = new FormLayout("pref, 3dlu, 140dlu, 3dlu, pref", "pref, 3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(originalRadio, cc.xy(1, 1));
        builder.add(originalLabel, cc.xyw(3, 1, 3));

        builder.add(alternateLocationRadio, cc.xy(1, 3));
        builder.add(alternateLocationTF, cc.xy(3, 3));
        builder.add(alternateLocationButton, cc.xy(5, 3));
        alternateLocationButton.addActionListener(new MyActionListener());

        builder.add(alternateNameRadio, cc.xy(1, 5));
        builder.add(alternateNameTF, cc.xy(3, 5));

        return builder.getPanel();
    }

    protected String getTitle() {
        return Translation.get("wizard.single_file_restore.title");
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
        bg.add(alternateLocationRadio);
        bg.add(alternateNameRadio);

        originalRadio.setSelected(true);
        originalLabel.setText(fileInfoToRestore.getDiskFile(getController().getFolderRepository()).getParent().toString());
        originalRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLocations();
            }
        });

        alternateLocationRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLocations();
            }
        });
        alternateLocationTF.setEditable(false);
        alternateLocationTF.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                updateLocations();
            }
        });

        alternateNameRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                defaultName();
            }
        });
        alternateNameTF.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                updateLocations();
            }
        });

        updateLocations();
    }

    private void defaultName() {
        SingleFileRestoreItem restoreItem = table.getSelectedRestoreItem();
        if (restoreItem != null) {
            // Provide a default of Filename_Version.extension
            String fileName = restoreItem.getFileInfo().getFilenameOnly();
            int version = restoreItem.getFileInfo().getVersion();
            if (fileName.lastIndexOf('.') >= 0) {
                String name = fileName.substring(0, fileName.lastIndexOf('.'));
                String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
                fileName = name + '_' + version + '.' + extension;
            } else {
                fileName = fileName + '_' + version;
            }
            alternateNameTF.setText(fileName);
        } else {
            alternateNameTF.setText("");
        }
        updateLocations();
    }

    private void updateLocations() {
        SingleFileRestoreItem restoreItem = table.getSelectedRestoreItem();
        if (restoreItem == null) {
            // Nothing available until a row is selected.
            originalRadio.setVisible(false);
            originalLabel.setVisible(false);

            alternateLocationRadio.setVisible(false);
            alternateLocationTF.setVisible(false);
            alternateLocationButton.setVisible(false);

            alternateNameRadio.setVisible(false);
            alternateNameTF.setVisible(false);

            hasNext = false;

        } else {

            originalRadio.setVisible(true);
            originalLabel.setVisible(true);
            originalLabel.setEnabled(originalRadio.isSelected());

            // Local restores can't use alternate name.
            if (restoreItem.isLocal()) {
                alternateLocationRadio.setVisible(true);
                alternateLocationTF.setVisible(true);
                alternateLocationButton.setVisible(true);
                alternateNameRadio.setVisible(false);
                alternateNameTF.setVisible(false);

                // Make sure the hidden alternateNameRadio is not selected.
                if (alternateNameRadio.isSelected()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            originalRadio.setSelected(true);
                        }
                    });
                }

            } else { // Server restores can't use alternate location.
                alternateLocationRadio.setVisible(false);
                alternateLocationTF.setVisible(false);
                alternateLocationButton.setVisible(false);
                alternateNameRadio.setVisible(true);
                alternateNameTF.setVisible(true);
                alternateNameTF.setEnabled(alternateNameRadio.isSelected());

                // Make sure the hidden alternateLocationRadio is not selected.
                if (alternateLocationRadio.isSelected()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            originalRadio.setSelected(true);
                        }
                    });
                }
            }

            hasNext = originalRadio.isSelected() ||
                    alternateLocationRadio.isSelected() && alternateLocationTF.getText().length() > 0 ||
                    alternateNameRadio.isSelected() && alternateNameTF.getText().length() > 0;

        }

        updateButtons();
    }

    @Override
    protected void afterDisplay() {
        loadVersions();
    }

    private void loadVersions() {
        infoLabel.setText(Translation.get("wizard.multi_file_restore.retrieving.text"));
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
        SingleFileRestoreItem restoreItem = table.getSelectedRestoreItem();
        if (restoreItem == null) {
            throw new IllegalStateException("Could not find the selected file info.");
        }

        // Alternate location for local restore.
        if (alternateLocationRadio.isSelected()) {
            String alternateDirectory = alternateLocationTF.getText();
            if (alternateDirectory != null && alternateDirectory.trim().length() > 0) {
                Path alternateFile = Paths.get(alternateDirectory.trim());
                if (Files.isDirectory(alternateFile) && Files.isWritable(alternateFile)) {
                    return new FileRestoringPanel(getController(), folder, restoreItem.getFileInfo(), alternateFile);
                }
            }
        }

        // Alternate name for server restore.
        if (alternateNameRadio.isSelected()) {
            return new FileRestoringPanel(getController(), folder, restoreItem.getFileInfo(), alternateNameTF.getText().trim());
        }

        // Restore to original location.
        return new FileRestoringPanel(getController(), folder, restoreItem.getFileInfo());
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
                    infoLabel.setText(Translation.get("wizard.single_file_restore.retrieved_none.text",
                            fileInfoToRestore.getFilenameOnly()));
                } else {
                    infoLabel.setText(Translation.get("wizard.single_file_restore.retrieved.text",
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
                infoLabel.setText(Translation.get("wizard.single_file_restore.retrieve_cancelled.text"));
            } catch (Exception e) {
                infoLabel.setText(Translation.get("wizard.single_file_restore.retrieve_exception.text",
                        e.getMessage()));
            }
            bar.setVisible(false);
            scrollPane.setVisible(true);
        }
    }

    private class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            updateLocations();
        }
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            // Help the user by ensuring alternate set is selected if the button is clicked.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    alternateLocationRadio.setSelected(true);
                    updateLocations();
                }
            });
            List<Path> files = DialogFactory.chooseDirectory(getController().getUIController(), alternateLocationTF.getText(),
                    false);
            if (files.isEmpty()) {
                return;
            }

            Path file = files.get(0);
            alternateLocationTF.setText(file.toString());
            updateLocations();
        }
    }
}
