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
 * $Id: FilesTab.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.folder.files;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import javax.swing.*;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.PreviewToJoinPanel;
import de.dal33t.powerfolder.ui.information.folder.files.table.FilesTablePanel;
import de.dal33t.powerfolder.ui.information.folder.files.tree.FilesTreePanel;
import de.dal33t.powerfolder.ui.widget.FileFilterTextField;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.wizard.MultiFileRestorePanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * UI component for the folder files tab
 */
public class FilesTab extends PFUIComponent implements DirectoryFilterListener {

    private static final int DELETED_INDEX = 4;

    private JPanel uiComponent;
    private JSplitPane splitPane;
    private FilesTablePanel tablePanel;
    private FileFilterTextField filterTextField;
    private JComboBox filterSelectionComboBox;
    private FilesStatsPanel statsPanel;
    private DirectoryFilter directoryFilter;
    private FilesTreePanel treePanel;
    private ValueModel flatMode;
    private Folder folder;
    private JCheckBox flatViewCB;
    private MyRestoreAction myRestoreAction;

    /**
     * Constructor
     * 
     * @param controller
     */
    public FilesTab(Controller controller) {
        super(controller);

        flatMode = new ValueHolder();

        statsPanel = new FilesStatsPanel(getController());

        filterTextField = new FileFilterTextField(getController());
        directoryFilter = new DirectoryFilter(controller, filterTextField
            .getSearchTextValueModel(), filterTextField
            .getSearchModeValueModel());
        directoryFilter.addListener(this);

        treePanel = new FilesTreePanel(controller);
        directoryFilter.addListener(treePanel);

        tablePanel = new FilesTablePanel(controller, this);
        directoryFilter.addListener(tablePanel);
        directoryFilter.setFlatMode(flatMode);
        treePanel.addTreeSelectionListener(tablePanel);

        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel
            .getUIComponent(), tablePanel.getUIComponent());
        final int dividerLocation = getController().getPreferences().getInt(
            "files.tab.location", 150);
        splitPane.setDividerLocation(dividerLocation);
        UIUtil.invokeLaterInEDT(new Runnable() {
            public void run() {
                splitPane.setDividerLocation(dividerLocation);
            }
        });
        splitPane.addPropertyChangeListener(new MyPropertyChangeListner());
        filterSelectionComboBox = new JComboBox();
        filterSelectionComboBox.setToolTipText(Translation
            .getTranslation("files_tab.combo.tool_tip"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("files_tab.combo.local_and_incoming"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("files_tab.combo.local_files_only"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("files_tab.combo.incoming_files_only"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("files_tab.combo.new_files_only"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("files_tab.combo.deleted_and_previous_files"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("files_tab.combo.unsynchronized_files"));
        filterSelectionComboBox.addActionListener(new MyActionListener());

    }

    /**
     * Set the tab with details for a folder.
     * 
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        Folder f = getController().getFolderRepository().getFolder(folderInfo);
        folder = f;
        updateNodes();
        directoryFilter.setFolder(f);
        tablePanel.setFolder(f);
        flatViewCB.setSelected(false);
        flatMode.setValue(flatViewCB.isSelected());

        // Triggers mode change and schedule filtering (MyActionListener).
        // filterSelectionComboBox
        // .setSelectedIndex(DirectoryFilter.FILE_FILTER_MODE_LOCAL_AND_INCOMING);

        directoryFilter.setFileFilterMode(filterSelectionComboBox
            .getSelectedIndex());
        directoryFilter.scheduleFiltering();
    }

    public void scheduleDirectoryFiltering() {
        directoryFilter.scheduleFiltering();
    }

    /**
     * Set the tab with details for a folder with new set and sort date
     * descending.
     * 
     * @param folderInfo
     */
    public void setFolderInfoLatest(FolderInfo folderInfo) {

        Folder f = getController().getFolderRepository().getFolder(folderInfo);
        directoryFilter.setFolder(f);
        tablePanel.setFolder(f);
        tablePanel.sortLatestDate();
        flatViewCB.setSelected(true);
        flatMode.setValue(flatViewCB.isSelected());

        // Triggers mode change and schedule filtering (MyActionListener).
        filterSelectionComboBox
            .setSelectedIndex(DirectoryFilter.FILE_FILTER_MODE_NEW_ONLY);
    }

    public void setFolderInfoDeleted(FolderInfo folderInfo) {

        Folder f = getController().getFolderRepository().getFolder(folderInfo);
        directoryFilter.setFolder(f);
        tablePanel.setFolder(f);
        tablePanel.sortLatestDate();
        flatViewCB.setSelected(true);
        flatMode.setValue(flatViewCB.isSelected());

        // Triggers mode change and schedule filtering (MyActionListener).
        filterSelectionComboBox
            .setSelectedIndex(DirectoryFilter.FILE_FILTER_MODE_DELETED_PREVIOUS);
    }

    /**
     * Set the tab with details for a folder with incoming files.
     * 
     * @param folderInfo
     */
    public void setFolderInfoIncoming(FolderInfo folderInfo) {

        Folder f = getController().getFolderRepository().getFolder(folderInfo);
        directoryFilter.setFolder(f);
        tablePanel.setFolder(f);
        flatViewCB.setSelected(true);
        flatMode.setValue(flatViewCB.isSelected());

        // Triggers mode change and schedule filtering (MyActionListener).
        filterSelectionComboBox
            .setSelectedIndex(DirectoryFilter.FILE_FILTER_MODE_INCOMING_ONLY);
    }

    /**
     * Gets the ui component
     * 
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUIComponent();
            enableRestoreButton();
        }
        return uiComponent;
    }

    /**
     * Bulds the ui component.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("3dlu, pref:grow, 3dlu",
            "3dlu, pref, 3dlu, pref, 3dlu, fill:pref:grow, 3dlu, pref, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createToolBar(), cc.xy(2, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 3));

        splitPane.setOneTouchExpandable(false);
        builder.add(splitPane, cc.xy(2, 6));
        builder.addSeparator(null, cc.xy(2, 8));
        builder.add(statsPanel.getUiComponent(), cc.xy(2, 9));
        uiComponent = builder.getPanel();
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {

        DetailsAction detailsAction = new DetailsAction(getController());
        JToggleButton detailsButton = new JToggleButton(detailsAction);
        detailsButton.setIcon(null);

        MySyncFolderAction syncFolderAction = new MySyncFolderAction(
            getController());
        JButton syncFolderButton = new JButton(syncFolderAction);
        syncFolderButton.setIcon(null);

        myRestoreAction = new MyRestoreAction(getController());
        JButton restoreButton = new JButton(myRestoreAction);
        restoreButton.setIcon(null);

        flatViewCB = new JCheckBox(Translation
            .getTranslation("files_tab.flat_view.text"));
        flatViewCB.setToolTipText(Translation
            .getTranslation("files_tab.flat_view.tip"));
        flatViewCB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                flatMode.setValue(flatViewCB.isSelected());
            }
        });

        FormLayout layout = new FormLayout(
            "pref, 3dlu:grow, pref, 3dlu, pref, 3dlu, pref", "pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(detailsButton);
        bar.addRelatedGap();
        bar.addGridded(syncFolderButton);
        bar.addRelatedGap();
        bar.addGridded(restoreButton);

        builder.add(bar.getPanel(), cc.xy(1, 1));
        builder.add(flatViewCB, cc.xy(3, 1));
        builder.add(filterSelectionComboBox, cc.xy(5, 1));
        builder.add(filterTextField.getUIComponent(), cc.xy(7, 1));

        return builder.getPanel();
    }

    public void adviseOfChange(FilteredDirectoryEvent event) {
        statsPanel.setStats(event.getLocalFiles(), event.getIncomingFiles(),
            event.getDeletedFiles());
    }

    /**
     * Set the selected tree node to this directory.
     * 
     * @param directoryInfo
     */
    public void setSelection(DirectoryInfo directoryInfo) {
        treePanel.setSelection(directoryInfo);
    }

    /**
     * Update the file filter with available nodes.
     */
    private void updateNodes() {
        if (folder != null) {
            filterTextField.setMembers(Arrays.asList(folder
                .getConnectedMembers()));
        } else {
            filterTextField.setMembers(null);
        }
    }

    private void enableRestoreButton() {
        int index = filterSelectionComboBox.getSelectedIndex();
        myRestoreAction.setEnabled(index == DELETED_INDEX);
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    /**
     * Detect changes to the split pane location.
     */
    private class MyPropertyChangeListner implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource().equals(splitPane)
                && evt.getPropertyName().equals("dividerLocation"))
            {
                getController().getPreferences().putInt("files.tab.location",
                    splitPane.getDividerLocation());
            }
        }
    }

    /**
     * Fire filter event for changes to dropdown selection.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(filterSelectionComboBox)) {
                directoryFilter.setFileFilterMode(filterSelectionComboBox
                    .getSelectedIndex());
                directoryFilter.scheduleFiltering();
                enableRestoreButton();
            }
        }
    }

    private class DetailsAction extends BaseAction {

        DetailsAction(Controller controller) {
            super("action_details", controller);
        }

        public void actionPerformed(ActionEvent e) {
            tablePanel.toggleDetails();
        }
    }

    private class MySyncFolderAction extends BaseAction {

        private MySyncFolderAction(Controller controller) {
            super("action_sync_folder", controller);
        }

        public void actionPerformed(ActionEvent e) {
            if (folder != null) {
                if (folder.isPreviewOnly()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            PreviewToJoinPanel panel = new PreviewToJoinPanel(
                                getController(), folder);
                            panel.open();
                        }
                    });
                } else {
                    getController().getUIController().syncFolder(folder);
                }
            }
        }
    }

    private class MyRestoreAction extends BaseAction {

        private MyRestoreAction(Controller controller) {
            super("action_restore_file", controller);
        }

        public void actionPerformed(ActionEvent e) {
            if (folder != null) {
                DiskItem[] diskItems = tablePanel.getSelectedRows();
                if (diskItems.length == 0) {
                    // Nothing selected, use everything.
                    diskItems = tablePanel.getAllRows();
                }

                List<FileInfo> fileInfosToRestore = new ArrayList<FileInfo>();
                for (DiskItem diskItem : diskItems) {
                    if (diskItem instanceof FileInfo) {
                        fileInfosToRestore.add((FileInfo) diskItem);
                    }
                }
                PFWizard wizard = new PFWizard(getController(), Translation
                    .getTranslation("wizard.pfwizard.restore_title"));

                MultiFileRestorePanel panel = new MultiFileRestorePanel(
                    getController(), folder, fileInfosToRestore);
                wizard.open(panel);
            }

        }
    }

    private class MyNodeManagerListener implements NodeManagerListener {
        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void friendAdded(NodeManagerEvent e) {
            updateNodes();
        }

        public void friendRemoved(NodeManagerEvent e) {
            updateNodes();
        }

        public void nodeAdded(NodeManagerEvent e) {
            updateNodes();
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateNodes();
        }

        public void nodeConnecting(NodeManagerEvent e) {
            updateNodes();
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateNodes();
        }

        public void nodeOffline(NodeManagerEvent e) {
            updateNodes();
        }

        public void nodeOnline(NodeManagerEvent e) {
            updateNodes();
        }

        public void nodeRemoved(NodeManagerEvent e) {
            updateNodes();
        }

        public void settingsChanged(NodeManagerEvent e) {
            updateNodes();
        }

        public void startStop(NodeManagerEvent e) {
            updateNodes();
        }
    }

}