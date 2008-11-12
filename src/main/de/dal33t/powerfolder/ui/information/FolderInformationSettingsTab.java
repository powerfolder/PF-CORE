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
* $Id: FolderInformationSettingsTab.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_DONT_RECYCLE;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_PREFIX;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.actionold.SelectionBaseAction;
import de.dal33t.powerfolder.ui.model.DiskItemFilterPatternsListModel;
import de.dal33t.powerfolder.util.PatternMatch;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;
import org.apache.commons.lang.StringUtils;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * UI component for the information settings tab
 */
public class FolderInformationSettingsTab extends PFUIComponent
        implements FolderInformationTab {

    private JPanel uiComponent;
    private Folder folder;
    private SyncProfileSelectorPanel transferModeSelectorPanel;
    private JCheckBox useRecycleBinBox;
    private DiskItemFilterPatternsListModel patternsListModel;
    private JList patternsList;
    private SelectionModel selectionModel;

    /**
     * Constructor
     *
     * @param controller
     */
    public FolderInformationSettingsTab(Controller controller) {
        super(controller);
        transferModeSelectorPanel = new SyncProfileSelectorPanel(getController());
        useRecycleBinBox = new JCheckBox(Translation.getTranslation(
                "folder_information_settings_tab.use_recycle_bin"));
        useRecycleBinBox.addActionListener(new MyActionListener());
        selectionModel = new SelectionModel();
        patternsListModel = new DiskItemFilterPatternsListModel(null);
    }

    /**
     * Set the tab with details for a folder.
     *
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        folder = getController().getFolderRepository().getFolder(folderInfo);
        transferModeSelectorPanel.setUpdateableFolder(folder);
        useRecycleBinBox.setSelected(folder.isUseRecycleBin());
        patternsListModel.setDiskItemFilter(folder.getDiskItemFilter());
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

        FormLayout layout = new FormLayout(
            "3dlu, right:pref, 3dlu, pref, 3dlu, pref:grow",
                "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Translation.getTranslation(
                "folder_information_settings_tab.transfer_mode")),
                cc.xy(2, 2));
        builder.add(transferModeSelectorPanel.getUIComponent(), cc.xy(4, 2));

        builder.add(useRecycleBinBox, cc.xy(4, 4));

        builder.add(new JLabel(Translation.getTranslation(
                "folder_information_settings_tab.ignore_patterns")),
                cc.xy(2, 6));
        builder.add(createPatternsPanel(), cc.xy(4, 6));

        uiComponent = builder.getPanel();
    }

    private JPanel createPatternsPanel() {
        patternsList = new JList(patternsListModel);
        patternsList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                selectionModel.setSelection(patternsList.getSelectedValue());
            }

        });

        Dimension size = new Dimension(200, 150);

        JScrollPane scroller = new JScrollPane(patternsList);
        scroller.setPreferredSize(size);

        FormLayout layout = new FormLayout("pref", "pref, 4dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(scroller, cc.xy(1, 1));
        builder.add(createButtonBar(), cc.xy(1, 3));
        return builder.getPanel();
    }

    /** refreshes the UI elements with the current data */
    private void update() {
        if (patternsList != null) {
            patternsListModel.fireUpdate();
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
            patternsListModel.fireUpdate();
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
            for (Iterator<String> iter = folder.getDiskItemFilter().getPatterns()
                .iterator(); iter.hasNext();)
            {
                String blackListPattern = iter.next();
                if (PatternMatch.isMatch(pattern.toLowerCase(),
                    blackListPattern))
                {

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
                        patternsListModel.fireUpdate();
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
                patternsListModel.fireUpdate();
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
                patternsListModel.fireUpdate();
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
                patternsListModel.fireUpdate();
            }
        }

        patternsList.getSelectionModel().clearSelection();
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
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
