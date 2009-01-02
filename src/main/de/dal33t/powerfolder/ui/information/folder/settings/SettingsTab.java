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

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.PatternChangeListener;
import de.dal33t.powerfolder.event.PatternChangedEvent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_DONT_RECYCLE;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_PREFIX;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.action.SelectionBaseAction;
import de.dal33t.powerfolder.ui.information.folder.FolderInformationTab;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.PatternMatch;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.*;
import de.javasoft.synthetica.addons.DirectoryChooser;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * UI component for the information settings tab
 */
public class SettingsTab extends PFUIComponent
        implements FolderInformationTab {

    private JPanel uiComponent;
    private Folder folder;
    private SyncProfileSelectorPanel transferModeSelectorPanel;
    private JCheckBox useRecycleBinBox;
    private DefaultListModel patternsListModel = new DefaultListModel();
    private JList patternsList;
    private SelectionModel selectionModel;
    private JTextField localFolderField;
    private JButton localFolderButton;
    private PatternChangeListener patternChangeListener;

    /**
     * Constructor
     *
     * @param controller
     */
    public SettingsTab(Controller controller) {
        super(controller);
        transferModeSelectorPanel = new SyncProfileSelectorPanel(getController());
        useRecycleBinBox = new JCheckBox(Translation.getTranslation(
                "folder_information_settings_tab.use_recycle_bin"));
        MyActionListener myActionListener = new MyActionListener();
        useRecycleBinBox.addActionListener(myActionListener);
        selectionModel = new SelectionModel();
        localFolderField = new JTextField();
        localFolderField.setEditable(false);
        localFolderButton = new JButtonMini(Icons.DIRECTORY,
                Translation.getTranslation(
                        "folder_information_settings_tab.select_directory.text"));
        localFolderButton.setEnabled(false);
        localFolderButton.addActionListener(myActionListener);
        patternChangeListener = new MyPatternChangeListener();
        patternsListModel = new DefaultListModel();
    }

    /**
     * Set the tab with details for a folder.
     *
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        if (folder != null) {
            folder.getDiskItemFilter().removeListener(patternChangeListener);
        }
        folder = getController().getFolderRepository().getFolder(folderInfo);
        folder.getDiskItemFilter().addListener(patternChangeListener);
        transferModeSelectorPanel.setUpdateableFolder(folder);
        useRecycleBinBox.setSelected(folder.isUseRecycleBin());
        update();
    }

    /**
     * Gets the ui component
     *
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Bulds the ui component.
     */
    private void buildUIComponent() {
                  // label           folder       butn
        FormLayout layout = new FormLayout(
            "3dlu, right:pref, 3dlu, 210dlu, 3dlu, pref",
                "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Translation.getTranslation(
                "folder_information_settings_tab.transfer_mode")),
                cc.xy(2, 2));
        builder.add(transferModeSelectorPanel.getUIComponent(), cc.xyw(4, 2, 3));

        builder.add(useRecycleBinBox, cc.xyw(4, 4, 3));

        builder.add(new JLabel(Translation.getTranslation(
                "folder_information_settings_tab.ignore_patterns")),
                cc.xy(2, 6));
        builder.add(createPatternsPanel(), cc.xyw(4, 6, 3));
        
        builder.add(new JLabel(Translation.getTranslation(
                "folder_information_settings_tab.local_folder_location")),
                cc.xy(2, 8));
        builder.add(localFolderField, cc.xy(4, 8));
        builder.add(localFolderButton, cc.xy(6, 8));

        builder.add(new JLabel("Online Storage"), cc.xy(2, 10));
        builder.add(createConfigurePanel(), cc.xy(4, 10));

        uiComponent = builder.getPanel();
    }

    private JPanel createConfigurePanel() {
        FormLayout layout = new FormLayout("pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(new JButton(new ConfigureFolderOnlineStorageAction(
                getController())), cc.xy(1, 1));
        return builder.getPanel();
    }

    private JPanel createPatternsPanel() {
        patternsList = new JList(patternsListModel);
        patternsList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                selectionModel.setSelection(patternsList.getSelectedValue());
            }

        });

        Dimension size = new Dimension(200, 150);

        JScrollPane scroller = new JScrollPane(patternsList);
        scroller.setPreferredSize(size);

        FormLayout layout = new FormLayout("pref", "pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(scroller, cc.xy(1, 1));
        builder.add(createButtonBar(), cc.xy(1, 3));
        return builder.getPanel();
    }

    /** refreshes the UI elements with the current data */
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
        EditAction editAction = new EditAction(getController(), selectionModel);
        RemoveAction removeAction = new RemoveAction(getController(),
            selectionModel);

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(addAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(editAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(removeAction));

        return bar.getPanel();
    }

    /** removes the selected pattern from the blacklist */
    private class RemoveAction extends SelectionBaseAction {
        private RemoveAction(Controller controller, SelectionModel selectionModel)
        {
            super("action_remove_ignore", controller,
                selectionModel);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            for (Object object : selectionModel.getSelections()) {
                String selection = (String) object;
                folder.getDiskItemFilter().removePattern(selection);
            }
            patternsList.getSelectionModel().clearSelection();
        }

        public void selectionChanged(SelectionChangeEvent event) {
            setEnabled(selectionModel.getSelection() != null);
        }
    }

    /**
     * Removes any patterns for this file name. Directories should have "/*"
     * added to the name.
     *
     * @param patterns
     */
    public void removePatterns(String patterns) {

        String[] options = new String[]{
            Translation.getTranslation("remove_pattern.remove"),
            Translation.getTranslation("remove_pattern.dont"),
            Translation.getTranslation("general.cancel")};

        StringTokenizer st = new StringTokenizer(patterns, "\n");
        while (st.hasMoreTokens()) {
            String pattern = st.nextToken();

            // Match any patterns for this file.
            for (String blackListPattern :
                    folder.getDiskItemFilter().getPatterns()) {
                if (PatternMatch.isMatch(pattern.toLowerCase(),
                        blackListPattern)) {

                    // Confirm that the user wants to remove this.
                    int result = DialogFactory.genericDialog(getController()
                            .getUIController().getMainFrame().getUIComponent(),
                            Translation.getTranslation("remove_pattern.title"),
                            Translation.getTranslation("remove_pattern.prompt",
                                    pattern), options, 0, GenericDialogType.INFO); // Default
                    // is
                    // remove.
                    if (result == 0) { // Remove
                        // Remove pattern and update.
                        folder.getDiskItemFilter().removePattern(blackListPattern);
                    } else if (result == 2) { // Cancel
                        // Abort for all other patterns.
                        break;
                    }
                }
            }
        }
    }

    /** opens a popup, input dialog to edit the selected pattern */
    private class EditAction extends SelectionBaseAction {
        EditAction(Controller controller, SelectionModel selectionModel) {
            super("action_edit_ignore", controller,
                selectionModel);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            String text = Translation
                .getTranslation("folder_information_settings_tab.edit_a_pattern.text");
            String title = Translation
                .getTranslation("folder_information_settings_tab.edit_a_pattern.title");

            String pattern = (String) JOptionPane.showInputDialog(
                getUIController().getMainFrame().getUIComponent(), text, title,
                JOptionPane.PLAIN_MESSAGE, null, null,
                // the text to edit:
                selectionModel.getSelection());
            if (!StringUtils.isBlank(pattern)) {
                folder.getDiskItemFilter().removePattern(
                    (String) selectionModel.getSelection());
                folder.getDiskItemFilter().addPattern(pattern);
            }
            patternsList.getSelectionModel().clearSelection();
        }

        public void selectionChanged(SelectionChangeEvent event) {
            setEnabled(selectionModel.getSelection() != null);
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
                .getTranslation("folder_information_settings_tab.add_a_pattern.example"));
        }
    }

    public void showAddPane(String initialPatterns) {

        Reject.ifNull(initialPatterns, "Patterns required");

        StringTokenizer st = new StringTokenizer(initialPatterns, "\n");
        if (st.countTokens() == 1) {
            String pattern = st.nextToken();
            String title = Translation
                .getTranslation("folder_information_settings_tab.add_a_pattern.title");
            String text = Translation
                .getTranslation("folder_information_settings_tab.add_a_pattern.text");
            String patternResult = (String) JOptionPane.showInputDialog(
                getUIController().getMainFrame().getUIComponent(), text, title,
                JOptionPane.PLAIN_MESSAGE, null, null, pattern);
            if (!StringUtils.isBlank(patternResult)) {
                folder.getDiskItemFilter().addPattern(patternResult);
            }

        } else {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            while (st.hasMoreTokens()) {
                String pattern = st.nextToken();
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
                .getTranslation("folder_information_settings_tab.add_patterns.text_1")
                + "\n\n" + sb.toString();
            String title = Translation
                .getTranslation("folder_information_settings_tab.add_patterns.title");
            int result = DialogFactory.genericDialog(getUIController()
                .getMainFrame().getUIComponent(), title, message, new String[]{
                Translation.getTranslation("general.ok"),
                Translation.getTranslation("general.cancel")}, 0,
                GenericDialogType.QUESTION);
            if (result == 0) {
                StringTokenizer st2 = new StringTokenizer(initialPatterns, "\n");
                while (st2.hasMoreTokens()) {
                    folder.getDiskItemFilter().addPattern(st2.nextToken());
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
        DirectoryChooser dc = new DirectoryChooser();
        if (originalDirectory != null) {
            dc.setCurrentDirectory(originalDirectory);
        }
        int i = dc.showOpenDialog(getController().getUIController()
            .getMainFrame().getUIComponent());
        if (i == JFileChooser.APPROVE_OPTION) {
            File selectedFile = dc.getSelectedFile();
            moveDirectory(originalDirectory, selectedFile, moveContent == 0);
        }
    }

    /**
     * Should the content of the existing folder be moved to the new location?
     *
     * @return true if should move.
     */
    private int shouldMoveContent() {
        return DialogFactory.genericDialog(getController()
            .getUIController().getMainFrame().getUIComponent(), Translation
            .getTranslation("folder_information_settings_tab.move_content.title"),
            Translation.getTranslation("folder_information_settings_tab.move_content"),
            new String[]{
                Translation
                    .getTranslation("folder_information_settings_tab.move_content.move"),
                Translation
                    .getTranslation("folder_information_settings_tab.move_content.dont"),
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
                        ActivityVisualizationWorker worker = new
                                MyActivityVisualizationWorker(moveContent,
                                originalDirectory, newDirectory);
                        worker.start();
                    } catch (Exception e) {
                        // Probably failed to create temp directory.
                        DialogFactory
                            .genericDialog(
                                getController().getUIController()
                                    .getMainFrame().getUIComponent(),
                                Translation.getTranslation(
                                        "folder_information_settings_tab.move_error.title"),
                                Translation.getTranslation(
                                        "folder_information_settings_tab.move_error.temp"),
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
            .getTranslation("folder_information_settings_tab.confirm_local_folder_move.title");
        String message = Translation.getTranslation(
            "folder_information_settings_tab.confirm_local_folder_move.text", folder
                .getLocalBase().getAbsolutePath(), newDirectory
                .getAbsolutePath());

        return DialogFactory.genericDialog(getController().getUIController()
            .getMainFrame().getUIComponent(), title, message, new String[]{
            Translation.getTranslation("general.continue"),
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
            int result = DialogFactory
                .genericDialog(
                    getController().getUIController().getMainFrame()
                        .getUIComponent(),
                    Translation
                        .getTranslation("folder_information_settings_tab.folder_not_empty.title"),
                    Translation.getTranslation(
                        "folder_information_settings_tab.folder_not_empty", newDirectory
                            .getAbsolutePath()), new String[]{
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
                .getSyncProfile(), false, folder.isUseRecycleBin(), false,
                false);
            folder = repository.createFolder(fi, fs);

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
        DialogFactory.genericDialog(getController().getUIController()
            .getMainFrame().getUIComponent(), Translation
            .getTranslation("folder_information_settings_tab.move_error.title"),
            Translation.getTranslation(
                "folder_information_settings_tab.move_error.other", e.getMessage()),
            GenericDialogType.WARN);
    }

    /**
     * Local class to handel action events.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(localFolderButton)) {
                moveLocalFolder();
            } else if (e.getSource().equals(useRecycleBinBox)) {
                folder.setUseRecycleBin(useRecycleBinBox.isSelected());
                Properties config = getController().getConfig();
                // Inverse logic for backward compatability.
                config.setProperty(FOLDER_SETTINGS_PREFIX + folder.getName()
                    + FOLDER_SETTINGS_DONT_RECYCLE, String
                    .valueOf(!useRecycleBinBox.isSelected()));
                getController().saveConfig();
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
            File originalDirectory, File newDirectory) {
            super(getUIController());
            this.moveContent = moveContent;
            this.originalDirectory = originalDirectory;
            this.newDirectory = newDirectory;
        }

        public Object construct() {
            return transferFolder(moveContent, originalDirectory, newDirectory);
        }

        protected String getTitle() {
            return Translation
                .getTranslation("folder_information_settings_tab.working.title");
        }

        protected String getWorkingText() {
            return Translation
                .getTranslation("folder_information_settings_tab.working.description");
        }

        public void finished() {
            if (get() != null) {
                displayError((Exception) get());
            }
        }
    }

    private class ConfigureFolderOnlineStorageAction extends BaseAction {
        private ConfigureFolderOnlineStorageAction(Controller controller) {
            super("action_configure_folder_online_storage", controller);
        }

        public void actionPerformed(ActionEvent e) {
            PFWizard.openMirrorFolderWizard(getController(), folder);
        }
    }

    private class MyPatternChangeListener implements PatternChangeListener {

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
}
