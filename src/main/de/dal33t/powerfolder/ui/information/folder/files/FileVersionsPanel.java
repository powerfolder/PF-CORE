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
package de.dal33t.powerfolder.ui.information.folder.files;

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.FileVersionInfo;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.light.FileInfo;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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

    private JPanel panel;
    private JLabel emptyLabel;
    private JScrollPane scrollPane;
    private volatile FileInfo fileInfo;

    public FileVersionsPanel(Controller controller) {
        super(controller);
    }

    public Component getPanel() {
        if (panel == null) {

            // Initalize components
            initComponents();

            FormLayout layout = new FormLayout(
                        "pref:grow",
                        "fill:0:grow");
            DefaultFormBuilder builder = new DefaultFormBuilder(layout);
            CellConstraints cc = new CellConstraints();

            // emptyLabel and scrollPane occupy the same slot.
            builder.add(emptyLabel, cc.xy(1, 1));
            builder.add(scrollPane, cc.xy(1, 1));

            panel = builder.getPanel();

            setEmptyState(true, false);
        }
        return panel;
    }

    private void initComponents() {
        emptyLabel = new JLabel(Translation.getTranslation(
                "file_version_tab.no_versions_available"), SwingConstants.CENTER);
        emptyLabel.setEnabled(false);

        scrollPane = new JScrollPane();
    }

    public void setFileInfo(FileInfo fileInfo) {
        if (panel == null) {
            // Panel not initalized yet
            return;
        }

        this.fileInfo = fileInfo;

        if (fileInfo == null) {
            setEmptyState(true, false);
            return;
        }

        VersionHistoryLoader loader = new VersionHistoryLoader();
        loader.execute();
    }

    /**
     * Display empty / loading text, or the actual results.
     *
     * @param empty
     *           empty ? show text : show results
     * @param loading
     *           loading ? show 'loading text' : show 'empty' text
     */
    private void setEmptyState(boolean empty, boolean loading) {
        if (panel == null) {
            return;
        }

        emptyLabel.setVisible(empty);
        if (loading) {
            emptyLabel.setText(Translation.getTranslation(
                    "file_version_tab.loading"));
        } else {
            emptyLabel.setText(Translation.getTranslation(
                    "file_version_tab.no_versions_available"));
        }
        scrollPane.setVisible(!empty);
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
            setEmptyState(true, true);

            // @todo harry work in progress...
            if (fileInfo != null) {
                Folder folder = fileInfo.getFolder(getController()
                        .getFolderRepository());
                FileArchiver fileArchiver = folder.getFileArchiver();
                List<FileVersionInfo> archivedFilesVersions =
                        fileArchiver.getArchivedFilesVersions(getController(),
                                fileInfo);
            }

            // Loaded...
            return null;
        }

        protected void done() {
            if (fileInfo == null) {
                // Huh. Loaded the history, but now no FileInfo is selected.
                setEmptyState(true, false);
            } else {
                // Got it. Show it.
                setEmptyState(false, false);
            }
        }
    }
}
