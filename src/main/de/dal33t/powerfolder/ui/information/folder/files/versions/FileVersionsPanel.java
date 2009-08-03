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
* $Id: FileDetailsPanel.java 5457 2009-07-31 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.files.versions;

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.RestoreArchiveDialog;
import de.dal33t.powerfolder.disk.FileVersionInfo;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.light.FileInfo;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Set;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.DefaultFormBuilder;

/**
 * A Panel to display version history about a file
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0 $
 */
public class FileVersionsPanel extends PFUIComponent {

    private static final int STATE_EMPTY = 0;
    private static final int STATE_LOADING = 1;
    private static final int STATE_RESULTS = 2;

    private JPanel panel;
    private JLabel emptyLabel;
    private JScrollPane scrollPane;
    private FileVersionsTableModel fileVersionsTableModel;
    private FileVersionsTable fileVersionsTable;
    private volatile FileInfo fileInfo;
    private RestoreAction restoreAction;

    public FileVersionsPanel(Controller controller) {
        super(controller);
    }

    public Component getPanel() {
        if (panel == null) {

            // Initalize components
            initComponents();

            scrollPane = new JScrollPane(fileVersionsTable);

            FormLayout layout = new FormLayout(
                    "pref:grow", "pref, 3dlu, pref, 3dlu, fill:0:grow");
            DefaultFormBuilder builder = new DefaultFormBuilder(layout);
            CellConstraints cc = new CellConstraints();


            builder.add(createButtonPanel(), cc.xy(1, 1));
            builder.addSeparator(null, cc.xy(1, 3));

            // emptyLabel and scrollPane occupy the same slot.
            builder.add(emptyLabel, cc.xy(1, 5));
            builder.add(scrollPane, cc.xy(1, 5));

            panel = builder.getPanel();

            setState(STATE_EMPTY);
        }
        return panel;
    }

    private Component createButtonPanel() {
        FormLayout layout = new FormLayout(
                "pref, fill:0:grow", "pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(new JButton(restoreAction), cc.xy(1, 1));
        return builder.getPanel();
    }

    private void initComponents() {

        fileVersionsTableModel = new FileVersionsTableModel(getController());
        fileVersionsTable = new FileVersionsTable(fileVersionsTableModel);
        fileVersionsTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        enableRestoreAction();
                    }
                });

        emptyLabel = new JLabel(Translation.getTranslation(
                "file_version_tab.no_versions_available"), SwingConstants.CENTER);
        emptyLabel.setEnabled(false);

        restoreAction = new RestoreAction(getController());
    }

    public void setFileInfo(FileInfo fileInfo) {
        if (panel == null) {
            // Panel not initalized yet
            return;
        }

        this.fileInfo = fileInfo;

        if (fileInfo == null) {
            setState(STATE_EMPTY);
            return;
        }

        VersionHistoryLoader loader = new VersionHistoryLoader();
        loader.execute();
    }

    /**
     * Display empty text or the actual results.
     *
     * @param state
     */
    private void setState(int state) {
        if (panel == null) {
            return;
        }

        emptyLabel.setVisible(state != STATE_RESULTS);
        scrollPane.setVisible(state == STATE_RESULTS);

        if (state == STATE_LOADING) {
            emptyLabel.setText("");
        } else if (state == STATE_EMPTY) {
            emptyLabel.setText(Translation.getTranslation(
                    "file_version_tab.no_versions_available"));
        }

        enableRestoreAction();
    }

    private void enableRestoreAction() {
        restoreAction.setEnabled(scrollPane.isVisible() &&
                fileVersionsTable.getSelectedRow() > -1);
    }

    /**
     * Restore a file archive
     */
    private void restoreFile() {
        if (fileInfo != null) {
            FileVersionInfo selectedInfo = fileVersionsTable.getSelectedInfo();
            RestoreArchiveDialog dialog = new RestoreArchiveDialog(
                    getController(), selectedInfo);
            dialog.open();
        }
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    /**
     * Swing worker to load the versions in the background.
     */
    private class VersionHistoryLoader extends SwingWorker {

        protected Object doInBackground() {

            // Loading...
            setState(STATE_LOADING);
            try {
                if (fileInfo != null) {
                    Folder folder = fileInfo.getFolder(getController()
                            .getFolderRepository());
                    FileArchiver fileArchiver = folder.getFileArchiver();
                    Set<FileVersionInfo> archivedFilesVersions =
                            fileArchiver.getArchivedFilesVersions(fileInfo);
                    if (archivedFilesVersions.isEmpty()) {
                        setState(STATE_EMPTY);
                    } else {
                        setState(STATE_RESULTS);
                        fileVersionsTableModel.setVersionInfos(archivedFilesVersions);
                    }
                } else {
                    setState(STATE_EMPTY);
                }
            } catch (Exception e) {
                // Huh?
                logSevere(e);
            }

            return null;
        }
    }

    private class RestoreAction extends BaseAction {

        RestoreAction(Controller controller) {
            super("action_restore_archive", controller);
        }

        public void actionPerformed(ActionEvent e) {
            restoreFile();
        }
    }

}
