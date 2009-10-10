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

import javax.swing.*;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.information.folder.files.table.FilesTablePanel;
import de.dal33t.powerfolder.ui.information.folder.files.tree.FilesTreePanel;
import de.dal33t.powerfolder.ui.widget.FileFilterTextField;
import de.dal33t.powerfolder.ui.dialog.PreviewToJoinPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SyncIconButtonMini;
import de.dal33t.powerfolder.util.ui.DelayedUpdater;

/**
 * UI component for the folder files tab
 */
public class FilesTab extends PFUIComponent implements DirectoryFilterListener {
    private JPanel uiComponent;
    private JSplitPane splitPane;
    private FilesTablePanel tablePanel;
    private FileFilterTextField filterTextField;
    private JComboBox filterSelectionComboBox;
    private FilesStatsPanel statsPanel;
    private DirectoryFilter directoryFilter;
    private FilesTreePanel treePanel;
    private ValueModel flatMode;
    private SyncIconButtonMini syncFolderButton;
    private Folder folder;
    private DelayedUpdater syncUpdater;
    private JCheckBox flatViewCB;

    /**
     * Constructor
     * 
     * @param controller
     */
    public FilesTab(Controller controller) {
        super(controller);

        syncUpdater = new DelayedUpdater(getController(), 1000L);

        flatMode = new ValueHolder();

        flatViewCB = new JCheckBox(Translation.getTranslation(
                "files_tab.flat_view.text"));

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

        MyTransferManagerListener myTransferManagerListener
                = new MyTransferManagerListener();
        getController().getTransferManager().addListener(
                myTransferManagerListener);


        syncFolderButton = new SyncIconButtonMini(getController());
        MySyncFolderAction syncFolderAction
                = new MySyncFolderAction(getController());
        syncFolderButton.addActionListener(syncFolderAction);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel
            .getUIComponent(), tablePanel.getUIComponent());
        int dividerLocation = getController().getPreferences().getInt(
            "files.tab.location", 150);
        splitPane.setDividerLocation(dividerLocation);
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
        filterSelectionComboBox.addActionListener(new MyActionListener());

    }

    /**
     * Set the tab with details for a folder.
     * 
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        Folder f = getController().getFolderRepository().getFolder(
            folderInfo);
        folder = f;
        directoryFilter.setFolder(f);
        tablePanel.setFolder(f);
    }

    /**
     * Set the tab with details for a folder.
     * 
     * @param folderInfo
     * @param directoryFilterMode
     */
    public void setFolderInfo(FolderInfo folderInfo, int directoryFilterMode) {
        setFolderInfo(folderInfo);
        if (directoryFilterMode >= 0) {
            directoryFilter.setFileFilterMode(directoryFilterMode);
            directoryFilter.scheduleFiltering();
        }
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

        // Triggers mode change and schedule filtering (MyActionListener).
        filterSelectionComboBox.setSelectedIndex(
                DirectoryFilter.FILE_FILTER_MODE_NEW_ONLY);
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
            "pref, 3dlu, pref, 3dlu:grow, pref, 3dlu, pref, 3dlu, pref", "pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(new JToggleButton(new DetailsAction(getController())), cc
            .xy(1, 1));
        builder.add(syncFolderButton, cc.xy(3, 1));
        builder.add(flatViewCB, cc.xy(5, 1));
        builder.add(filterSelectionComboBox, cc.xy(7, 1));
        builder.add(filterTextField.getUIComponent(), cc.xy(9, 1));

        return builder.getPanel();
    }

    public void adviseOfChange(FilteredDirectoryEvent event) {
        statsPanel.setStats(event.getLocalFiles(), event.getIncomingFiles(),
            event.getDeletedFiles());
    }

    /**
     * Set the selected tree node to this directory.
     * 
     * @param directory
     */
    public void setSelection(Directory directory) {
        treePanel.setSelection(directory);
    }

    private void updateSyncButton() {
        if (folder == null) {
            syncFolderButton.spin(false);
            return;
        }
        syncUpdater.schedule(new Runnable() {
            public void run() {
                if (folder == null) {
                    syncFolderButton.spin(false);
                } else {
                    syncFolderButton.spin(folder.isSyncing());
                }
            }
        });
    }

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

    private class MyTransferManagerListener extends TransferAdapter {

        private void updateIfRequired(TransferManagerEvent event) {
            updateSyncButton();
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        public void downloadRequested(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }



}