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
 * $Id: SettingsTab.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.folder.settings;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.l2fprod.common.swing.JDirectoryChooser;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderPreviewHelper;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.event.DiskItemFilterListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.PatternChangedEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.FolderRemovePanel;
import de.dal33t.powerfolder.ui.dialog.PreviewToJoinPanel;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.pattern.CompiledPattern;
import de.dal33t.powerfolder.util.pattern.Pattern;
import de.dal33t.powerfolder.util.pattern.PatternFactory;
import de.dal33t.powerfolder.util.ui.ArchiveModeSelectorPanel;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SwingWorker;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * UI component for the information settings tab
 */
public class SettingsTab extends PFUIComponent {

    // Model and other stuff
    private final ServerClient serverClient;
    private Folder folder;
    private ValueModel modeModel;
    private ValueModel versionModel;
    private final ValueModel scriptModel;
    private DefaultListModel patternsListModel = new DefaultListModel();
    private final SelectionModel selectionModel;
    private FolderMembershipListener membershipListner;
    private final DiskItemFilterListener patternChangeListener;

    /**
     * Folders with this setting will backup files before replacing them with
     * newer downloaded ones.
     */
    private final RemoveFolderAction removeFolderAction;

    // UI Components
    private JPanel uiComponent;
    private final SyncProfileSelectorPanel transferModeSelectorPanel;
    private final ArchiveModeSelectorPanel archiveModeSelectorPanel;
    private JList patternsList;
    private final JTextField localFolderField;
    private final JButton localFolderButton;
    private ActionLabel confOSActionLabel;
    private ActionLabel previewFolderActionLabel;
    private JButtonMini editButton;
    private JButtonMini removeButton;
    private boolean settingFolder = false;

    /**
     * Constructor
     * 
     * @param controller
     */
    public SettingsTab(Controller controller) {
        super(controller);
        serverClient = controller.getOSClient();
        transferModeSelectorPanel = new SyncProfileSelectorPanel(
            getController());
        MyActionListener myActionListener = new MyActionListener();
        selectionModel = new SelectionModel();
        localFolderField = new JTextField();
        localFolderField.setEditable(false);
        localFolderButton = new JButtonMini(Icons.getIconById(Icons.DIRECTORY),
            Translation.getTranslation("settings_tab.select_directory.text"));
        localFolderButton.setEnabled(false);
        localFolderButton.addActionListener(myActionListener);
        patternChangeListener = new MyPatternChangeListener();
        patternsListModel = new DefaultListModel();
        removeFolderAction = new RemoveFolderAction(getController());
        serverClient.addListener(new MyServerClientListener());
        membershipListner = new MyFolderMembershipListener();
        scriptModel = new ValueHolder(null, false);
        MyValueChangeListener listener = new MyValueChangeListener();
        modeModel = new ValueHolder(); // <ArchiveMode>
        modeModel.addValueChangeListener(listener);
        versionModel = new ValueHolder(); // <Integer>
        versionModel.addValueChangeListener(listener);
        archiveModeSelectorPanel = new ArchiveModeSelectorPanel(controller,
            modeModel, versionModel);
    }

    /**
     * Set the tab with details for a folder.
     * 
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        if (folder != null) {
            folder.getDiskItemFilter().removeListener(patternChangeListener);
            folder.removeMembershipListener(membershipListner);
        }
        settingFolder = true;
        folder = getController().getFolderRepository().getFolder(folderInfo);
        folder.getDiskItemFilter().addListener(patternChangeListener);
        folder.addMembershipListener(membershipListner);
        transferModeSelectorPanel.setUpdateableFolder(folder);
        scriptModel.setValue(folder.getDownloadScript());
        archiveModeSelectorPanel.setArchiveMode(folder.getFileArchiver()
            .getArchiveMode(), folder.getFileArchiver().getVersionsPerFile());
        settingFolder = false;
        update();
        enableConfigOSAction();
        enablePreviewFolderAction();
    }

    /**
     * @return the ui component
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Builds the ui component.
     */
    private void buildUIComponent() {
        // label folder butn padding
        FormLayout layout = new FormLayout(
            "3dlu, right:pref, 3dlu, 140dlu, 3dlu, pref, pref:grow",
            "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 12dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 2;
        builder.add(new JLabel(Translation
            .getTranslation("general.transfer_mode")), cc.xy(2, row));
        builder.add(transferModeSelectorPanel.getUIComponent(), cc.xyw(4, row,
            4));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("general.archive_mode")), cc.xy(2, row));
        builder.add(archiveModeSelectorPanel.getUIComponent(), cc
            .xyw(4, row, 4));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("settings_tab.ignore_patterns")), cc.xy(2, row,
            "right, top"));
        builder.add(createPatternsPanel(), cc.xyw(4, row, 4));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("settings_tab.local_folder_location")), cc.xy(2,
            row));
        builder.add(localFolderField, cc.xy(4, row));
        builder.add(localFolderButton, cc.xy(6, row));
        row += 2;

        if (Feature.DOWNLOAD_SCRIPT.isEnabled()) {
            builder.addLabel(Translation
                .getTranslation("settings_tab.download_script"), cc.xy(2, row));
            builder.add(createScriptField(), cc.xyw(4, row, 4));
        }

        row += 2;
        builder.add(createConfigurePanel(), cc.xy(4, row));

        row += 2;
        builder.add(createPreviewPanel(), cc.xy(4, row));

        row += 2;
        builder.add(createDeletePanel(), cc.xy(4, row));

        addSelectionListener();

        uiComponent = builder.getPanel();
    }

    private void addSelectionListener() {
        selectionModel.addSelectionChangeListener(new SelectionChangeListener()
        {
            public void selectionChanged(SelectionChangeEvent event) {
                int selectionsLength = selectionModel.getSelections() == null
                    ? 0
                    : selectionModel.getSelections().length;
                editButton.setEnabled(selectionsLength > 0);
                removeButton.setEnabled(selectionsLength > 0);
            }
        });
    }

    private JPanel createDeletePanel() {
        FormLayout layout = new FormLayout("pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        ActionLabel label = new ActionLabel(getController(), removeFolderAction);
        builder.add(label.getUIComponent(), cc.xy(1, 1));
        label.convertToBigLabel();
        return builder.getPanel();
    }

    private JPanel createConfigurePanel() {
        FormLayout layout = new FormLayout("pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        confOSActionLabel = new ActionLabel(getController(),
            new FolderOnlineStorageAction(getController()));
        confOSActionLabel.convertToBigLabel();
        builder.add(confOSActionLabel.getUIComponent(), cc.xy(1, 1));
        return builder.getPanel();
    }

    private JPanel createPreviewPanel() {
        FormLayout layout = new FormLayout("pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        previewFolderActionLabel = new ActionLabel(getController(),
            new PreviewFolderAction(getController()));
        previewFolderActionLabel.convertToBigLabel();
        builder.add(previewFolderActionLabel.getUIComponent(), cc.xy(1, 1));
        return builder.getPanel();
    }

    private JPanel createPatternsPanel() {
        patternsList = new JList(patternsListModel);
        patternsList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                selectionModel.setSelection(patternsList.getSelectedValue());
            }

        });

        Dimension size = new Dimension(200, 100);

        JScrollPane scroller = new JScrollPane(patternsList);
        scroller.setPreferredSize(size);

        FormLayout layout = new FormLayout("140dlu", "pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(scroller, cc.xy(1, 1));
        builder.add(createButtonBar(), cc.xy(1, 3));
        return builder.getPanel();
    }

    /**
     * Creates a pair of location text field and button.
     * 
     * @return
     */
    private JComponent createScriptField() {
        scriptModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                folder.setDownloadScript((String) evt.getNewValue());
            }
        });

        FormLayout layout = new FormLayout("140dlu, 3dlu, pref, pref", "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JTextField locationTF = BasicComponentFactory.createTextField(
            scriptModel, false);
        locationTF.setEditable(true);
        builder.add(locationTF, cc.xy(1, 1));

        JButton locationButton = new JButtonMini(Icons
            .getIconById(Icons.DIRECTORY), Translation
            .getTranslation("settings_tab.download_script"));
        locationButton.addActionListener(new SelectScriptAction());
        builder.add(locationButton, cc.xy(3, 1));
        builder.add(Help.createWikiLinkButton(getController(),
            WikiLinks.SCRIPT_EXECUTION), cc.xy(4, 1));
        return builder.getPanel();
    }

    /**
     * refreshes the UI elements with the current data
     */
    private void update() {
        rebuildPatterns();
        localFolderField.setText(folder.getLocalBase().getAbsolutePath());
        localFolderButton.setEnabled(!folder.isPreviewOnly());
    }

    private void rebuildPatterns() {
        patternsListModel.clear();
        List<String> stringList = folder.getDiskItemFilter().getPatterns();
        for (String s : stringList) {
            patternsListModel.addElement(s);
        }
    }

    private JPanel createButtonBar() {
        AddAction addAction = new AddAction(getController());
        EditAction editAction = new EditAction(getController());
        RemoveAction removeAction = new RemoveAction(getController());

        FormLayout layout = new FormLayout("pref, pref, pref, pref:grow",
            "pref");
        PanelBuilder bar = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        editButton = new JButtonMini(editAction);
        removeButton = new JButtonMini(removeAction);

        editButton.setEnabled(false);
        removeButton.setEnabled(false);

        bar.add(new JButtonMini(addAction), cc.xy(1, 1));
        bar.add(editButton, cc.xy(2, 1));
        bar.add(removeButton, cc.xy(3, 1));

        return bar.getPanel();
    }

    /**
     * Removes any patterns for this file name. Directories should have "/*"
     * added to the name.
     * 
     * @param patterns
     */
    public void removePatterns(String patterns) {

        String[] options = {
            Translation.getTranslation("remove_pattern.remove"),
            Translation.getTranslation("remove_pattern.dont"),
            Translation.getTranslation("general.cancel")};

        String[] patternArray = patterns.split("\\n");
        for (String pattern : patternArray) {

            // Match any patterns for this file.
            Pattern patternMatch = PatternFactory.createPattern(pattern);
            for (String blackListPattern : folder.getDiskItemFilter()
                .getPatterns())
            {
                if (patternMatch.isMatch(blackListPattern)) {

                    // Confirm that the user wants to remove this.
                    int result = DialogFactory.genericDialog(getController(),
                        Translation.getTranslation("remove_pattern.title"),
                        Translation.getTranslation("remove_pattern.prompt",
                            pattern), options, 0, GenericDialogType.INFO);
                    // Default is remove.
                    if (result == 0) { // Remove
                        // Remove pattern and update.
                        folder.getDiskItemFilter().removePattern(
                            blackListPattern);
                    } else if (result == 2) { // Cancel
                        // Abort for all other patterns.
                        break;
                    }
                }
            }
        }
    }

    public void showAddPane(String initialPatterns) {

        Reject.ifNull(initialPatterns, "Patterns required");

        String[] patternArray = initialPatterns.split("\\n");
        if (patternArray.length == 1) {
            String pattern = patternArray[0];
            String title = Translation
                .getTranslation("settings_tab.add_a_pattern.title");
            String text = Translation
                .getTranslation("settings_tab.add_a_pattern.text");
            String patternResult = (String) JOptionPane.showInputDialog(
                getUIController().getActiveFrame(), text, title,
                JOptionPane.PLAIN_MESSAGE, null, null, pattern);
            if (!StringUtils.isBlank(patternResult)) {
                folder.getDiskItemFilter().addPattern(patternResult);
            }

        } else {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String pattern : patternArray) {
                sb.append("    ");
                if (count++ >= 10) {
                    // Too many selections - enough!!!
                    sb.append(Translation
                        .getTranslation("general.more.lower_case")
                        + "...\n");
                    break;
                }
                sb.append(pattern + '\n');
            }
            String message = Translation
                .getTranslation("settings_tab.add_patterns.text_1")
                + "\n\n" + sb.toString();
            String title = Translation
                .getTranslation("settings_tab.add_patterns.title");
            int result = DialogFactory.genericDialog(getController(), title,
                message, new String[]{Translation.getTranslation("general.ok"),
                    Translation.getTranslation("general.cancel")}, 0,
                GenericDialogType.QUESTION);
            if (result == 0) {
                for (String pattern : patternArray) {
                    folder.getDiskItemFilter().addPattern(pattern);
                }
            }
        }

        patternsList.getSelectionModel().clearSelection();
    }

    /**
     * Controls the movement of a folder directory.
     */
    private void moveLocalFolder() {

        // Find out if the user wants to move the content of the current folder
        // to the new one.
        int moveContent = shouldMoveContent();

        if (moveContent == 2) {
            // Cancel
            return;
        }

        File originalDirectory = folder.getLocalBase();

        // Select the new folder.
        JDirectoryChooser dc = new JDirectoryChooser();
        if (originalDirectory != null) {
            dc.setCurrentDirectory(originalDirectory);
        }
        int i = dc.showOpenDialog(getController().getUIController()
            .getActiveFrame());
        if (i == JFileChooser.APPROVE_OPTION) {
            File selectedFile = dc.getSelectedFile();
            if (FileUtils.isSubdirectory(originalDirectory, selectedFile)) {
                DialogFactory.genericDialog(getController(), Translation
                    .getTranslation("settings_tab.subdir.title"), Translation
                    .getTranslation("settings_tab.subdir.text"),
                    GenericDialogType.ERROR);
            } else {
                File foldersBaseDir = new File(getController()
                    .getFolderRepository().getFoldersBasedir());
                if (selectedFile.equals(foldersBaseDir)) {
                    DialogFactory
                        .genericDialog(getController(), Translation
                            .getTranslation("settings_tab.basedir.title"),
                            Translation
                                .getTranslation("settings_tab.basedir.text"),
                            GenericDialogType.ERROR);
                } else {
                    moveDirectory(originalDirectory, selectedFile,
                        moveContent == 0);
                }
            }
        }
    }

    /**
     * Should the content of the existing folder be moved to the new location?
     * 
     * @return true if should move.
     */
    private int shouldMoveContent() {
        return DialogFactory.genericDialog(getController(), Translation
            .getTranslation("settings_tab.move_content.title"), Translation
            .getTranslation("settings_tab.move_content"), new String[]{
            Translation.getTranslation("settings_tab.move_content.move"),
            Translation.getTranslation("settings_tab.move_content.dont"),
            Translation.getTranslation("general.cancel"),}, 0,
            GenericDialogType.INFO);
    }

    /**
     * Move the directory.
     */
    public void moveDirectory(File originalDirectory, File newDirectory,
        boolean moveContent)
    {
        if (!newDirectory.equals(originalDirectory)) {

            // Check for any problems with the new folder.
            if (checkNewLocalFolder(newDirectory)) {

                // Confirm move.
                if (shouldMoveLocal(newDirectory)) {
                    try {
                        // Move contentes selected
                        ActivityVisualizationWorker worker = new MyActivityVisualizationWorker(
                            moveContent, originalDirectory, newDirectory);
                        worker.start();
                    } catch (Exception e) {
                        // Probably failed to create temp directory.
                        DialogFactory
                            .genericDialog(
                                getController(),
                                Translation
                                    .getTranslation("settings_tab.move_error.title"),
                                Translation
                                    .getTranslation("settings_tab.move_error.temp"),
                                getController().isVerbose(), e);
                    }
                }
            }
        }
    }

    /**
     * Confirm that the user really does want to go ahead with the move.
     * 
     * @param newDirectory
     * @return true if the user wishes to move.
     */
    private boolean shouldMoveLocal(File newDirectory) {
        String title = Translation
            .getTranslation("settings_tab.confirm_local_folder_move.title");
        String message = Translation.getTranslation(
            "settings_tab.confirm_local_folder_move.text", folder
                .getLocalBase().getAbsolutePath(), newDirectory
                .getAbsolutePath());

        return DialogFactory.genericDialog(getController(), title, message,
            new String[]{Translation.getTranslation("general.continue"),
                Translation.getTranslation("general.cancel")}, 0,
            GenericDialogType.INFO) == 0;
    }

    /**
     * Do some basic validation. Warn if moving to a folder that has files /
     * directories in it.
     * 
     * @param newDirectory
     * @return
     */
    private boolean checkNewLocalFolder(File newDirectory) {

        // Warn if target directory is not empty.
        if (newDirectory != null && newDirectory.exists()
            && newDirectory.listFiles().length > 0)
        {
            int result = DialogFactory.genericDialog(getController(),
                Translation
                    .getTranslation("settings_tab.folder_not_empty.title"),
                Translation.getTranslation("settings_tab.folder_not_empty",
                    newDirectory.getAbsolutePath()), new String[]{
                    Translation.getTranslation("general.continue"),
                    Translation.getTranslation("general.cancel")}, 1,
                GenericDialogType.WARN); // Default is cancel.
            if (result != 0) {
                // User does not want to move to new folder.
                return false;
            }
        }

        // All good.
        return true;
    }

    /**
     * Moves the contents of a folder to another via a temporary directory.
     * 
     * @param moveContent
     * @param originalDirectory
     * @param newDirectory
     * @return
     */
    private Object transferFolder(boolean moveContent, File originalDirectory,
        File newDirectory)
    {
        try {

            // Copy the files to the new local base
            if (!newDirectory.exists()) {
                if (!newDirectory.mkdirs()) {
                    throw new IOException("Failed to create directory: "
                        + newDirectory);
                }
            }

            // Remove the old folder from the repository.
            FolderRepository repository = getController().getFolderRepository();
            repository.removeFolder(folder, false);

            // Move it.
            if (moveContent) {
                FileUtils.recursiveMove(originalDirectory, newDirectory);
            }

            // Create the new Folder in the repository.
            FolderInfo fi = new FolderInfo(folder);
            FolderSettings fs = new FolderSettings(newDirectory, folder
                .getSyncProfile(), false, folder.getFileArchiver()
                .getArchiveMode(), folder.isPreviewOnly(), folder
                .getDownloadScript(), folder.getFileArchiver()
                .getVersionsPerFile());
            folder = repository.createFolder(fi, fs);
            if (!moveContent) {
                folder.addDefaultExcludes();
            }

            // Update with new folder info.
            update();

        } catch (Exception e) {
            return e;
        }
        return null;
    }

    /**
     * Displays an error if the folder move failed.
     * 
     * @param e
     *            the error
     */
    private void displayError(Exception e) {
        DialogFactory.genericDialog(getController(), Translation
            .getTranslation("settings_tab.move_error.title"), Translation
            .getTranslation("settings_tab.move_error.other", e.getMessage()),
            GenericDialogType.WARN);
    }

    /**
     * Listen to changes in onlineStorage / folder and enable the configOS
     * button as required. Also config action on whether already joined OS.
     */
    private void enableConfigOSAction() {

        boolean enabled = false;
        if (folder != null && serverClient.isConnected()
            && serverClient.isLoggedIn())
        {
            enabled = true;
            boolean osConfigured = serverClient.hasJoined(folder);
            if (osConfigured) {
                confOSActionLabel.setText(Translation
                    .getTranslation("action_stop_online_storage.name"));
                confOSActionLabel.setToolTipText(Translation
                    .getTranslation("action_stop_online_storage.description"));
            } else {
                confOSActionLabel.setText(Translation
                    .getTranslation("action_backup_online_storage.name"));
                confOSActionLabel
                    .setToolTipText(Translation
                        .getTranslation("action_backup_online_storage.description"));
            }
        }
        confOSActionLabel.getUIComponent().setVisible(enabled);
    }

    /**
     * Listen to changes in onlineStorage / folder and enable the configOS
     * button as required. Also config action on whether already joined OS.
     */
    private void enablePreviewFolderAction() {

        boolean enabled = false;
        if (folder != null) {
            enabled = true;
            if (folder.isPreviewOnly()) {
                previewFolderActionLabel.setText(Translation
                    .getTranslation("action_stop_preview_folder.name"));
                previewFolderActionLabel.setToolTipText(Translation
                    .getTranslation("action_stop_preview_folder.description"));
            } else {
                previewFolderActionLabel.setText(Translation
                    .getTranslation("action_preview_folder.name"));
                previewFolderActionLabel.setToolTipText(Translation
                    .getTranslation("action_preview_folder.description"));
            }

        }
        previewFolderActionLabel.setEnabled(enabled);
    }

    private void updateArchiveMode(Object oldValue, Object newValue) {
        ArchiveMode am = (ArchiveMode) modeModel.getValue();
        if (am == ArchiveMode.NO_BACKUP) {
            folder.setArchiveMode(ArchiveMode.NO_BACKUP);
        } else {
            folder.setArchiveMode(ArchiveMode.FULL_BACKUP);
            Integer versions = (Integer) versionModel.getValue();
            folder.setArchiveVersions(versions);

            // If the versions is reduced, offer to delete excess.
            if (newValue != null && oldValue != null
                && newValue instanceof Integer && oldValue instanceof Integer
                && (Integer) newValue < (Integer) oldValue)
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        int i = DialogFactory
                            .genericDialog(
                                getController(),
                                Translation
                                    .getTranslation("settings_tab.offer_maintenance.title"),
                                Translation
                                    .getTranslation("settings_tab.offer_maintenance.text"),
                                new String[]{
                                    Translation
                                        .getTranslation("general.delete"),
                                    Translation
                                        .getTranslation("general.cancel")}, 0,
                                GenericDialogType.QUESTION);
                        if (i == 0) {
                            MySwingWorker worker = new MySwingWorker();
                            worker.start();
                        }
                    }
                });
            }
        }
    }

    public static void doPreviewChange(Controller controller, Folder fldr) {
        if (fldr.isPreviewOnly()) {

            // Join preview folder.
            PreviewToJoinPanel panel = new PreviewToJoinPanel(controller, fldr);
            panel.open();

        } else {

            int result = DialogFactory
                .genericDialog(
                    controller,
                    Translation
                        .getTranslation("settings_tab.preview_warning_title"),
                    Translation
                        .getTranslation("settings_tab.preview_warning_message"),
                    new String[]{
                        Translation
                            .getTranslation("settings_tab.preview_warning_convert"),
                        Translation.getTranslation("general.cancel")}, 0,
                    GenericDialogType.WARN);

            if (result == 0) { // Convert to preview

                // Convert folder to preview.
                FolderPreviewHelper.convertFolderToPreview(controller, fldr);
            }
        }
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    /**
     * Local class to handle action events.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(localFolderButton)) {
                moveLocalFolder();
            }
        }
    }

    /**
     * Visualisation worker for folder move.
     */
    private class MyActivityVisualizationWorker extends
        ActivityVisualizationWorker
    {

        private final boolean moveContent;
        private final File originalDirectory;
        private final File newDirectory;

        MyActivityVisualizationWorker(boolean moveContent,
            File originalDirectory, File newDirectory)
        {
            super(getUIController());
            this.moveContent = moveContent;
            this.originalDirectory = originalDirectory;
            this.newDirectory = newDirectory;
        }

        @Override
        public Object construct() {
            return transferFolder(moveContent, originalDirectory, newDirectory);
        }

        @Override
        protected String getTitle() {
            return Translation.getTranslation("settings_tab.working.title");
        }

        @Override
        protected String getWorkingText() {
            return Translation
                .getTranslation("settings_tab.working.description");
        }

        @Override
        public void finished() {
            if (get() != null) {
                displayError((Exception) get());
            }
        }
    }

    private class RemoveFolderAction extends BaseAction {

        private RemoveFolderAction(Controller controller) {
            super("action_remove_folder", controller);
        }

        public void actionPerformed(ActionEvent e) {
            FolderRemovePanel panel = new FolderRemovePanel(getController(),
                folder.getInfo());
            panel.open();
        }
    }

    private class PreviewFolderAction extends BaseAction {

        private PreviewFolderAction(Controller controller) {
            super("action_preview_folder", controller);
        }

        public void actionPerformed(ActionEvent e) {
            doPreviewChange(getController(), folder);
        }
    }

    private class FolderOnlineStorageAction extends BaseAction {

        private FolderOnlineStorageAction(Controller controller) {
            super("action_backup_online_storage", controller);
        }

        public void actionPerformed(ActionEvent e) {
            // FolderOnlineStoragePanel knows if folder already joined :-)
            PFWizard.openMirrorFolderWizard(getController(), folder);
        }
    }

    private class MyPatternChangeListener implements DiskItemFilterListener {

        public void patternAdded(PatternChangedEvent e) {
            rebuildPatterns();
        }

        public void patternRemoved(PatternChangedEvent e) {
            rebuildPatterns();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /**
     * Listener to ServerClient for connection changes.
     */
    private class MyServerClientListener implements ServerClientListener {

        public void login(ServerClientEvent event) {
            enableConfigOSAction();
        }

        public void accountUpdated(ServerClientEvent event) {
            enableConfigOSAction();
        }

        public void serverConnected(ServerClientEvent event) {
            enableConfigOSAction();
        }

        public void serverDisconnected(ServerClientEvent event) {
            enableConfigOSAction();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyFolderMembershipListener implements
        FolderMembershipListener
    {

        public void memberJoined(FolderMembershipEvent folderEvent) {
            if (getController().getOSClient().isServer(folderEvent.getMember()))
            {
                enableConfigOSAction();
            }
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            if (getController().getOSClient().isServer(folderEvent.getMember()))
            {
                enableConfigOSAction();
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    /**
     * opens a popup, input dialog to edit the selected pattern
     */
    private class EditAction extends BaseAction {
        EditAction(Controller controller) {
            super("action_edit_ignore", controller);
        }

        public void actionPerformed(ActionEvent e) {
            String text = Translation
                .getTranslation("settings_tab.edit_a_pattern.text");
            String title = Translation
                .getTranslation("settings_tab.edit_a_pattern.title");

            String pattern = (String) JOptionPane.showInputDialog(UIUtil
                .getParentWindow(e), text, title, JOptionPane.PLAIN_MESSAGE,
                null, null,
                // the text to edit:
                selectionModel.getSelection());
            if (!StringUtils.isBlank(pattern)) {
                folder.getDiskItemFilter().removePattern(
                    (String) selectionModel.getSelection());
                folder.getDiskItemFilter().addPattern(pattern);
            }
            patternsList.getSelectionModel().clearSelection();
        }
    }

    /**
     * Add a pattern to the backlist, opens a input dialog so user can enter
     * one.
     */
    private class AddAction extends BaseAction {
        private AddAction(Controller controller) {
            super("action_add_ignore", controller);

        }

        public void actionPerformed(ActionEvent e) {
            showAddPane(Translation
                .getTranslation("settings_tab.add_a_pattern.example"));
        }
    }

    /**
     * Action listener for the location button. Opens a choose dir dialog and
     * sets the location model with the result.
     */
    private class SelectScriptAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            String initial = (String) scriptModel.getValue();
            JFileChooser chooser = DialogFactory.createFileChooser();
            chooser.setSelectedFile(new File(initial));
            int res = chooser.showDialog(getUIController().getMainFrame()
                .getUIComponent(), Translation
                .getTranslation("settings_tab.download_script.select"));

            if (res == JFileChooser.APPROVE_OPTION) {
                String script = chooser.getSelectedFile().getAbsolutePath();
                scriptModel.setValue(script);
            }
        }
    }

    /**
     * removes the selected pattern from the blacklist
     */
    private class RemoveAction extends BaseAction {
        private RemoveAction(Controller controller) {
            super("action_remove_ignore", controller);
        }

        public void actionPerformed(ActionEvent e) {
            for (Object object : selectionModel.getSelections()) {
                String selection = (String) object;
                folder.getDiskItemFilter().removePattern(selection);
            }
            patternsList.getSelectionModel().clearSelection();
        }
    }

    private class MyValueChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() == modeModel || evt.getSource() == versionModel)
            {
                if (!settingFolder) {
                    updateArchiveMode(evt.getOldValue(), evt.getNewValue());
                }
            }
        }
    }

    private class MySwingWorker extends SwingWorker {
        public Object construct() {
            try {
                return folder.getFileArchiver().maintain();
            } catch (Exception e) {
                logSevere(e);
                return null;
            }
        }
    }

}
