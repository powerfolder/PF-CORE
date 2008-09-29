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
package de.dal33t.powerfolder.ui.dialog;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * A Panel to display detail infos about a file
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.17 $
 */
public class FileDetailsPanel extends PFUIComponent implements
    SelectionChangeListener
{
    private FileInfo file;
    private JPanel panel;
    private JPanel embeddedPanel;
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
    public FileDetailsPanel(Controller controller) {
        super(controller);
    }

    /*
     * not used Initalizes panel with the given file @param controller @param
     * file
     */
    /*
     * public FileDetailsPanel(Controller controller, FileInfo file) {
     * super(controller); setFile(file); }
     */

    /**
     * Initalizes panel with the given ValueModel, holding the file Listens on
     * changes of the model
     * 
     * @param controller
     * @param fileModel
     *            the model containing the file
     */
    public FileDetailsPanel(Controller controller, SelectionModel fileModel) {
        super(controller);

        if (fileModel.getSelection() instanceof FileInfo) {
            setFile((FileInfo) fileModel.getSelection());
        }
        fileModel.addSelectionChangeListener(this);
    }

    public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
        Object selection = selectionChangeEvent.getSelection();

        if (selection instanceof FileInfo) {
            setFile((FileInfo) selection);
        }

    }

    // Setter/Getter **********************************************************

    /**
     * @return the currently displayed file
     */
    public FileInfo getFile() {
        return file;
    }

    /**
     * Sets the information on this panel by the file
     * 
     * @param file
     */
    public void setFile(FileInfo file) {
        if (file == null) {
            throw new NullPointerException("File may not be null");
        }
        this.file = file;
        if (panel == null) {
            // Panel not initalizes yet
            return;
        }

        // Prepare some values
        List<Member> sources;
        if (getController().getFolderRepository().hasJoinedFolder(
            file.getFolderInfo()))
        {
            sources = getController().getTransferManager().getSourcesFor(file);
        } else {
            sources = Collections.EMPTY_LIST;
        }
        int nSources = (sources == null ? 0 : sources.size());
        String sourcesText = nSources + " Source" + (nSources != 1 ? "s" : "");

        // Member[] sources =
        // getController().getTransferManager().getSourcesFor(fInfo);
        sourcesText = "";
        if (sources != null && !sources.isEmpty()) {
            sourcesText += sources.get(0).getNick();
            for (int i = 1; i < sources.size(); i++) {
                sourcesText += ", " + sources.get(i).getNick();
            }
        }
        // sourcesText = Arrays.asList(sources).toString();

        // Prepare status
        String status;
        Icon statusIcon = Icons.getIconFor(getController(), file);

        if (file.isUploading(getController())) {
            status = Translation.getTranslation("fileinfo.uploading");
            // FIXME: Hack, this overwrites the default status icons, since we
            // don't want upload icons in our table view
            // Maybe we need to split up Icons.getIconFor to know its context
            statusIcon = Icons.UPLOAD;
        } else if (file.isDownloading(getController())) {
            DownloadManager dl = getController().getTransferManager()
                .getActiveDownload(file);
            status = Translation.getTranslation("fileinfo.downloading");
            if (dl != null && dl.isStarted()) {
                status += " ("
                    + Format.formatNumber(dl.getCounter()
                        .calculateCompletionPercentage()) + "%)";
            }
        } else if (file.isDeleted()) {
            status = Translation.getTranslation("fileinfo.deleted");
        } else if (file.isExpected(getController().getFolderRepository())) {
            status = Translation.getTranslation("fileinfo.expected");
        } else if (hasJoinedFolder()) {
            if (file.isNewerAvailable(getController().getFolderRepository())) {
                status = Translation.getTranslation("fileinfo.newer_available");
            } else {
                status = Translation.getTranslation("fileinfo.normal");
            }
        } else if (nSources > 0) {
            status = Translation.getTranslation("fileinfo.available");
        } else {
            status = Translation.getTranslation("fileinfo.not_available");
        }

        // Prepare diskfile
        File diskFile = file.getDiskFile(getController().getFolderRepository());
        if (diskFile != null) {
            if (!diskFile.exists()) {
                diskFile = null;
            }
        }

        nameField.setText(file.getFilenameOnly());
        nameField.setCaretPosition(0);
        locationField.setText(file.getLocationInFolder());
        locationField.setCaretPosition(0);

        folderField.setText(file.getFolderInfo().name);
        folderField.setIcon(Icons.FOLDER);
        sizeField.setText(Format.formatBytes(file.getSize()));

        statusField.setText(status);
        statusField.setIcon(statusIcon);

        Member node = file.getModifiedBy().getNode(getController());
        modifiedByField.setText(file.getModifiedBy().nick);
        modifiedByField.setIcon(Icons.getIconFor(node));
        modifiedDateField.setText(Format.formatDate(file.getModifiedDate()));

        versionField.setText("" + file.getVersion());

        sourcesField.setText(sourcesText);

        localCopyAtField.setText(diskFile != null
            ? diskFile.getAbsolutePath()
            : "- " + Translation.getTranslation("general.not_available") + " -");
        localCopyAtField.setCaretPosition(0);
    }

    // Helper code ************************************************************

    /**
     * Answers if we have joined the the folder of the file
     * 
     * @return
     */
    private boolean hasJoinedFolder() {
        return getFolderOfFile() != null;
    }

    /**
     * Answers the folder of the file, if joined
     * 
     * @return
     */
    private Folder getFolderOfFile() {
        return file.getFolder(getController().getFolderRepository());
    }

    // UI Methods *************************************************************

    /**
     * Gets the panel as embedded panel. White background etc
     * 
     * @return
     */
    public JPanel getEmbeddedPanel() {
        if (embeddedPanel == null) {
            FormLayout layout = new FormLayout("pref:grow", "pref");
            DefaultFormBuilder builder = new DefaultFormBuilder(layout);
            CellConstraints cc = new CellConstraints();

            // Set color and border
            getPanel().setBackground(Color.WHITE);
            getPanel().setBorder(Borders.DLU7_BORDER);

            builder.add(getPanel(), cc.xy(1, 1));
            embeddedPanel = builder.getPanel();
        }

        return embeddedPanel;
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

            FormLayout layout = new FormLayout(
                "right:max(p;50dlu), 7dlu, 107dlu, 40dlu, right:p, 7dlu, 107dlu",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");
            DefaultFormBuilder builder = new DefaultFormBuilder(layout);
            CellConstraints cc = new CellConstraints();

            // Set all labels to black color to fix color on themechange

            // Top
            builder.addLabel(Translation.getTranslation("fileinfo.name"),
                cc.xy(1, 1)).setForeground(Color.BLACK);
            builder.add(nameField, cc.xywh(3, 1, 5, 1));

            // First column
            builder.addLabel(Translation.getTranslation("fileinfo.location"),
                cc.xy(1, 3)).setForeground(Color.BLACK);
            builder.add(locationField, cc.xy(3, 3));

            builder.addLabel(Translation.getTranslation("general.size"),
                cc.xy(1, 5)).setForeground(Color.BLACK);
            builder.add(sizeField, cc.xy(3, 5));

            builder.addLabel(Translation.getTranslation("fileinfo.status"),
                cc.xy(1, 7)).setForeground(Color.BLACK);
            builder.add(statusField, cc.xy(3, 7));

            // Second column
            builder.addLabel(Translation.getTranslation("general.folder"),
                cc.xy(5, 3)).setForeground(Color.BLACK);
            builder.add(folderField, cc.xy(7, 3));

            builder.addLabel(Translation.getTranslation("fileinfo.modified_by"),
                cc.xy(5, 5)).setForeground(Color.BLACK);
            builder.add(modifiedByField, cc.xy(7, 5));

            builder.addLabel(
                Translation.getTranslation("fileinfo.modified_date"),
                cc.xy(5, 7)).setForeground(Color.BLACK);
            builder.add(modifiedDateField, cc.xy(7, 7));

            builder.addLabel(Translation.getTranslation("fileinfo.version"),
                cc.xy(5, 9)).setForeground(Color.BLACK);
            builder.add(versionField, cc.xy(7, 9));

            builder.addLabel(
                Translation.getTranslation("fileinfo.availability"),
                cc.xy(1, 9)).setForeground(Color.BLACK);
            builder.add(sourcesField, cc.xy(3, 9));

            // Bottom
            builder.addLabel(Translation.getTranslation("general.local_copy_at"),
                cc.xy(1, 11)).setForeground(Color.BLACK);
            builder.add(localCopyAtField, cc.xywh(3, 11, 5, 1));

            // Set file info
            if (file != null) {
                setFile(file);
            }

            panel = builder.getPanel();
        }

        return panel;
    }

    /**
     * Initalizes all needed components
     */
    private void initComponents() {
        nameField = SimpleComponentFactory.createTextField(false);
        locationField = SimpleComponentFactory.createTextField(false);
        folderField = SimpleComponentFactory.createLabel();
        folderField.setForeground(Color.BLACK);
        sizeField = SimpleComponentFactory.createTextField(false);
        localCopyAtField = SimpleComponentFactory.createTextField(false);
        statusField = SimpleComponentFactory.createLabel();
        statusField.setForeground(Color.BLACK);
        sourcesField = SimpleComponentFactory.createTextField(false);
        modifiedByField = SimpleComponentFactory.createLabel();
        modifiedByField.setForeground(Color.BLACK);
        modifiedDateField = SimpleComponentFactory.createTextField(false);
        versionField = SimpleComponentFactory.createTextField(false);
    }
}