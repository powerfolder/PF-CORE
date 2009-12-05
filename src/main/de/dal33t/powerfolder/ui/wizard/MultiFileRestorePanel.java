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

import de.dal33t.powerfolder.ui.wizard.table.RestoreFilesTableModel;
import de.dal33t.powerfolder.ui.wizard.table.RestoreFilesTable;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.FolderService;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.ui.SwingWorker;
import de.dal33t.powerfolder.util.ui.UIUtil;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import jwf.WizardPanel;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;

/**
 * Dialog for selecting a number of users.
 */
public class MultiFileRestorePanel extends PFWizardPanel {

    private static final Logger log = Logger.getLogger(MultiFileRestorePanel
            .class.getName());

    private final Folder folder;
    private final List<FileInfo> deletedFileInfos;
    private JProgressBar bar;
    private final JLabel infoLabel;
    private boolean hasNext;
    private JScrollPane scrollPane;
    private final RestoreFilesTableModel tableModel;
    private final List<FileInfo> fileInfosToRestore;
    private JDateChooser dateChooser;
    private JRadioButton latestVersionButton;
    private JRadioButton dateVersionButton;
    public MultiFileRestorePanel(Controller controller, Folder folder,
                                 List<FileInfo> deletedFileInfos) {
        super(controller);
        infoLabel = new JLabel();
        this.folder = folder;
        this.deletedFileInfos = deletedFileInfos;
        tableModel = new RestoreFilesTableModel(controller);
        fileInfosToRestore = new ArrayList<FileInfo>();
    }

    protected JComponent buildContent() {
        FormLayout layout = new FormLayout("140dlu, 3dlu, pref, 3dlu, pref:grow",
                "pref, 3dlu, pref, 6dlu, pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        int row = 1;

//        builder.add(latestVersionButton, cc.xy(1, row));
//
//        row += 2;
//
//        builder.add(dateVersionButton, cc.xy(1, row));
//        builder.add(dateChooser, cc.xy(3, row));
//
//        row += 2;

        builder.add(infoLabel, cc.xy(1, row, CellConstraints.CENTER,
                CellConstraints.DEFAULT));

        row += 2;

        bar.setIndeterminate(true);
        builder.add(bar, cc.xy(1, row));

        RestoreFilesTable table = new RestoreFilesTable(tableModel);
        scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        scrollPane.setVisible(false);
        UIUtil.removeBorder(scrollPane);
        UIUtil.setZeroWidth(scrollPane);
        builder.add(scrollPane, cc.xyw(1, row, 5));

        return builder.getPanel();
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.multi_file_restore_panel.title");
    }

    protected void afterDisplay() {

        // In case user comes back from next screen.
        bar.setVisible(true);
        hasNext = false;
        updateButtons();
        scrollPane.setVisible(false);

        SwingWorker worker = new MyFolderCreateWorker();
        worker.start();
    }

    protected void initComponents() {
        bar = new JProgressBar();
        latestVersionButton = new JRadioButton(Translation.getTranslation(
                "wizard.multi_file_restore_panel.button_latest"));
        latestVersionButton.setSelected(true);
        dateVersionButton = new JRadioButton(Translation.getTranslation(
                "wizard.multi_file_restore_panel.button_date"));
        ButtonGroup bg = new ButtonGroup();
        bg.add(latestVersionButton);
        bg.add(dateVersionButton);

        dateChooser = new JDateChooser();

        MyActionListener actionListener = new MyActionListener();
        latestVersionButton.addActionListener(actionListener);
        dateVersionButton.addActionListener(actionListener);

        latestVersionButton.setOpaque(false);
        dateVersionButton.setOpaque(false);
        
        updateDateChooser();
    }

    public boolean hasNext() {
        return hasNext;
    }

    public WizardPanel next() {
        return new MultiFileRestoringPanel(getController(), folder,
                fileInfosToRestore);
    }

    private class MyFolderCreateWorker extends SwingWorker {
        public Object construct() {
            List<FileInfo> versions = new ArrayList<FileInfo>();
            try {
                FileArchiver fileArchiver = folder.getFileArchiver();
                MemberInfo myInfo = getController().getMySelf().getInfo();
                // Also try getting versions from OnlineStorage.
                boolean online = folder.hasMember(getController()
                        .getOSClient().getServer());
                FolderService service = null;
                if (online) {
                    ServerClient client = getController().getOSClient();
                    if (client != null && client.isConnected()
                            && client.isLoggedIn()) {
                        service = client.getFolderService();
                    }
                }

                int count = 1;
                for (FileInfo fileInfo : deletedFileInfos) {
                    infoLabel.setText(Translation.getTranslation(
                            "wizard.multi_file_restore_panel.retrieving",
                            Format.formatLong(count++),
                            Format.formatLong(deletedFileInfos.size())));

                    List<FileInfo> infoList = fileArchiver
                            .getArchivedFilesInfos(fileInfo, myInfo);
                    FileInfo mostRecent = null;
                    for (FileInfo info : infoList) {
                        if (mostRecent == null || mostRecent.getVersion()
                                < info.getVersion()) {
                            mostRecent = info;
                        }
                    }

                    if (service != null) {
                        List<FileInfo> serviceList = service
                                .getArchivedFilesInfos(fileInfo);
                        for (FileInfo info : serviceList) {
                            if (mostRecent == null || mostRecent.getVersion()
                                    < info.getVersion()) {
                                mostRecent = info;
                            }
                        }
                    }

                    if (mostRecent != null) {
                        versions.add(mostRecent);
                    }
                }

                bar.setVisible(false);

                if (versions.isEmpty()) {
                    infoLabel.setText(Translation.getTranslation(
                            "wizard.multi_file_restore_panel.retrieving_none"));
                    return null;
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, "Exception", e);
                infoLabel.setText(Translation.getTranslation(
                        "wizard.multi_file_restore_panel.retrieving_failure"));
            }
            return versions;
        }

        @SuppressWarnings({"unchecked"})
        protected void afterConstruct() {
            final List<FileInfo> versions = (List<FileInfo>) getValue();
            if (versions != null) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        scrollPane.setVisible(true);
                        bar.setVisible(false);
                        tableModel.setVersions(versions);
                        hasNext = true;
                        infoLabel.setText(Translation.getTranslation(
                                "wizard.multi_file_restore_panel.retrieving_success"));
                        fileInfosToRestore.clear();
                        fileInfosToRestore.addAll(versions);
                        updateButtons();
                    }
                });
            }
        }
    }

    private void updateDateChooser() {
        dateChooser.setVisible(dateVersionButton.isSelected());
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == latestVersionButton ||
                    e.getSource() == dateVersionButton) {
                updateDateChooser();
            }
        }
    }
}