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
 * $Id: FileDetailsPanel.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.folder.files;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

/**
 * A Panel to display detail infos about a file
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.17 $
 */
public class FileDetailsPanel extends PFUIComponent {

    private final boolean includeSeparator;
    private JPanel panel;
    private JTextField nameField;
    private JTextField locationField;
    private JLabel folderField;
    private JTextField sizeField;
    private JLabel statusField;
    private JTextField sourcesField;
    private JLabel modifiedByField;
    private JTextField modifiedDateField;
    private JTextField versionField;
    private JTextField localCopyAtField;

    /**
     * Initalizes the panel with empty content
     *
     * @param controller
     */
    public FileDetailsPanel(Controller controller, boolean includeSeparator) {
        super(controller);
        this.includeSeparator = includeSeparator;
    }

    /**
     * Sets the information on this panel by the file
     *
     * @param fileInfo
     */
    public void setFileInfo(FileInfo fileInfo) {
        if (panel == null) {
            // Panel not initalizes yet
            return;
        }

        if (fileInfo == null) {
            clearComponents();
            return;
        }

        // Prepare some values
        List<Member> sources;
        if (getController().getFolderRepository().hasJoinedFolder(
            fileInfo.getFolderInfo()))
        {
            sources = getController().getTransferManager().getSourcesFor(
                fileInfo);
        } else {
            sources = new ArrayList<Member>();
        }
        int nSources = sources == null ? 0 : sources.size();
        StringBuilder sourcesText = new StringBuilder();
        if (sources != null && !sources.isEmpty()) {
            sourcesText.append(sources.get(0).getNick());
            for (int i = 1; i < sources.size(); i++) {
                sourcesText.append(", " + sources.get(i).getNick());
            }
        }

        // Prepare status
        StringBuilder status;
        Icon statusIcon = Icons.getIconFor(getController(), fileInfo);

        if (fileInfo.isUploading(getController())) {
            status = new StringBuilder(Translation
                .get("file_details_panel.uploading"));
            // FIXME: Hack, this overwrites the default status icons, since we
            // don't want upload icons in our table view
            // Maybe we need to split up Icons.getIconFor to know its context
            statusIcon = Icons.getIconById(Icons.UPLOAD);
        } else if (fileInfo.isDownloading(getController())) {
            DownloadManager dl = getController().getTransferManager()
                .getActiveDownload(fileInfo);
            status = new StringBuilder(Translation
                .get("file_details_panel.downloading"));
            if (dl != null && dl.isStarted()) {
                status.append(" ("
                    + Format.formatDecimal(dl.getCounter()
                        .calculateCompletionPercentage()) + "%)");
            }
        } else if (fileInfo.isDeleted()) {
            status = new StringBuilder(Translation
                .get("file_details_panel.deleted"));
        } else if (fileInfo.isExpected(getController().getFolderRepository())) {
            status = new StringBuilder(Translation
                .get("file_details_panel.expected"));
        } else if (hasJoinedFolder(fileInfo)) {
            if (fileInfo
                .isNewerAvailable(getController().getFolderRepository()))
            {
                FileInfo newestVersion = fileInfo
                    .getNewestVersion(getController().getFolderRepository());
                String verStr = newestVersion != null ? newestVersion
                    .getVersion()
                    + "" : "?";
                status = new StringBuilder(Translation.get(
                    "file_details_panel.newer_available", verStr));
            } else {
                status = new StringBuilder(Translation
                    .get("file_details_panel.normal"));
            }
        } else if (nSources > 0) {
            status = new StringBuilder(Translation
                .get("file_details_panel.available"));
        } else {
            status = new StringBuilder(Translation
                .get("file_details_panel.not_available"));
        }

        // Prepare diskfile
        Path diskFile = fileInfo.getDiskFile(getController()
            .getFolderRepository());
        if (diskFile != null) {
            if (Files.notExists(diskFile)) {
                diskFile = null;
            }
        }

        nameField.setText(fileInfo.getFilenameOnly());
        nameField.setCaretPosition(0);
        locationField.setText(fileInfo.getRelativeName());
        locationField.setCaretPosition(0);

        folderField.setText(fileInfo.getFolderInfo().getLocalizedName());
        sizeField.setText(Format.formatBytes(fileInfo.getSize()));

        statusField.setText(status.toString());
        statusField.setIcon(statusIcon);

        MemberInfo modifiedBy = fileInfo.getModifiedBy();
        modifiedByField.setText(modifiedBy.nick);
        modifiedDateField.setText(Format.formatDateShort(fileInfo
            .getModifiedDate()));

        versionField.setText(String.valueOf(fileInfo.getVersion()));

        sourcesField.setText(sourcesText.toString());

        localCopyAtField
            .setText(diskFile != null ? diskFile.toAbsolutePath().toString() : "- "
                + Translation.get("general.not_available") + " -");
        localCopyAtField.setCaretPosition(0);
    }

    // Helper code ************************************************************

    /**
     * Answers if we have joined the the folder of the file
     *
     * @return
     */
    private boolean hasJoinedFolder(FileInfo fileInfo) {
        return getFolderOfFile(fileInfo) != null;
    }

    /**
     * Answers the folder of the file, if joined
     *
     * @return
     */
    private Folder getFolderOfFile(FileInfo fileInfo) {
        return fileInfo.getFolder(getController().getFolderRepository());
    }

    /**
     * Returns the ui component for the fileinfo panel
     *
     * @return the panel component
     */
    public JPanel getPanel() {
        if (panel == null) {
            // Initalize components
            initComponents();

            FormLayout layout;
            if (includeSeparator) {
                layout = new FormLayout(
                    "right:max(p;50dlu), 3dlu, 107dlu, 40dlu, right:p, 3dlu, 107dlu, p:g",
                    "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu");
            } else {
                layout = new FormLayout(
                    "right:max(p;50dlu), 3dlu, 107dlu, 40dlu, right:p, 3dlu, 107dlu, p:g",
                    "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu");
            }
            DefaultFormBuilder builder = new DefaultFormBuilder(layout);
            CellConstraints cc = new CellConstraints();

            int row = 1;

            if (includeSeparator) {
                builder.addSeparator(null, cc.xyw(1, row, 8));

                row += 2;
            }

            builder.addLabel(Translation
                .get("file_details_panel.name"), cc.xy(1, row));
            builder.add(nameField, cc.xywh(3, row, 5, 1));

            row += 2;

            builder.addLabel(Translation
                .get("file_details_panel.location"), cc.xy(1, row));
            builder.add(locationField, cc.xy(3, row));

            builder.addLabel(Translation.get("general.folder"), cc
                .xy(5, row));
            builder.add(folderField, cc.xy(7, row));

            row += 2;

            builder.addLabel(Translation.get("general.size"), cc.xy(
                1, row));
            builder.add(sizeField, cc.xy(3, row));

            builder.addLabel(Translation
                .get("file_details_panel.modified_by"), cc
                .xy(5, row));
            builder.add(modifiedByField, cc.xy(7, row));

            row += 2;

            builder.addLabel(Translation
                .get("file_details_panel.status"), cc.xy(1, row));
            builder.add(statusField, cc.xy(3, row));

            builder.addLabel(Translation
                .get("file_details_panel.modified_date"), cc.xy(5,
                row));
            builder.add(modifiedDateField, cc.xy(7, row));

            row += 2;

            builder.addLabel(Translation
                .get("file_details_panel.version"), cc.xy(5, row));
            builder.add(versionField, cc.xy(7, row));

            builder.addLabel(Translation
                .get("file_details_panel.availability"), cc.xy(1,
                row));
            builder.add(sourcesField, cc.xy(3, row));

            row += 2;

            builder.addLabel(Translation
                .get("general.local_copy_at"), cc.xy(1, row));
            builder.add(localCopyAtField, cc.xywh(3, row, 5, 1));

            panel = builder.getPanel();
            panel.setVisible(false);
        }

        return panel;
    }

    /**
     * Clears all component details
     */
    private void clearComponents() {
        nameField.setText("");
        locationField.setText("");
        folderField.setText("");
        folderField.setIcon(null);
        sizeField.setText("");
        localCopyAtField.setText("");
        statusField.setText("");
        sourcesField.setText("");
        modifiedByField.setText("");
        modifiedByField.setIcon(null);
        modifiedDateField.setText("");
        versionField.setText("");
    }

    /**
     * Initalizes all needed components
     */
    private void initComponents() {
        nameField = SimpleComponentFactory.createTextField(false);
        locationField = SimpleComponentFactory.createTextField(false);
        folderField = SimpleComponentFactory.createLabel();
        sizeField = SimpleComponentFactory.createTextField(false);
        localCopyAtField = SimpleComponentFactory.createTextField(false);
        statusField = SimpleComponentFactory.createLabel();
        sourcesField = SimpleComponentFactory.createTextField(false);
        modifiedByField = SimpleComponentFactory.createLabel();
        modifiedDateField = SimpleComponentFactory.createTextField(false);
        versionField = SimpleComponentFactory.createTextField(false);
    }
}