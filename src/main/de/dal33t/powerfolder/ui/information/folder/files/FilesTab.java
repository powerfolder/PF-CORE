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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.DiskItem;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.information.folder.files.breadcrumb.FilesBreadcrumbPanel;
import de.dal33t.powerfolder.ui.information.folder.files.table.FilesTablePanel;
import de.dal33t.powerfolder.ui.widget.FileFilterTextField;
import de.dal33t.powerfolder.ui.wizard.MultiFileRestorePanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.Translation;

/**
 * UI component for the folder files tab
 */
public class FilesTab extends PFUIComponent implements DirectoryFilterListener {

    private JPanel uiComponent;
    private FilesTablePanel tablePanel;
    private FileFilterTextField filterTextField;
    private JComboBox filterSelectionComboBox;
    private FilesStatsPanel statsPanel;
    private DirectoryFilter directoryFilter;
    private ValueModel flatMode;
    private Folder folder;
    private JCheckBox flatViewCB;
    private FilesBreadcrumbPanel breadcrumbPanel;

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
        directoryFilter = new DirectoryFilter(controller,
            filterTextField.getSearchTextValueModel(),
            filterTextField.getSearchModeValueModel());
        directoryFilter.addListener(this);

        tablePanel = new FilesTablePanel(controller, this);
        directoryFilter.addListener(tablePanel);
        directoryFilter.setFlatMode(flatMode);

        breadcrumbPanel = new FilesBreadcrumbPanel(getController(), this);

        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());

        filterSelectionComboBox = new JComboBox();
        filterSelectionComboBox.setToolTipText(Translation
            .getTranslation("files_tab.combo.tool_tip"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("files_tab.combo.local_and_incoming"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("files_tab.combo.local_files_only"));
        // filterSelectionComboBox.addItem(Translation
        // .getTranslation("files_tab.combo.incoming_files_only"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("files_tab.combo.new_files_only"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("files_tab.combo.deleted_and_previous_files"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("files_tab.combo.unsynchronized_files"));
        filterSelectionComboBox.addActionListener(new MyActionListener());

    }

    public void scheduleDirectoryFiltering() {
        directoryFilter.scheduleFiltering();
    }

    /**
     * Set the tab with details for a folder.
     * 
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        folder = getController().getFolderRepository().getFolder(folderInfo);
        updateNodes();
        directoryFilter.setFolder(folder,
            FileInfoFactory.lookupDirectory(folder.getInfo(), ""));
        tablePanel.setFolder(folder);
        flatViewCB.setSelected(false);
        flatMode.setValue(flatViewCB.isSelected());

        // Triggers mode change and schedule filtering (MyActionListener).
        setFilterComboBox(DirectoryFilter.FILE_FILTER_MODE_LOCAL_AND_INCOMING);
        filterTextField.reset();
        breadcrumbPanel.setRoot(folderInfo);
    }

    /**
     * Set the tab with details for a folder with new set and sort date
     * descending.
     * 
     * @param folderInfo
     */
    public void setFolderInfoLatest(FolderInfo folderInfo) {
        folder = getController().getFolderRepository().getFolder(folderInfo);
        updateNodes();
        directoryFilter.setFolder(folder,
            FileInfoFactory.lookupDirectory(folder.getInfo(), ""));
        tablePanel.setFolder(folder);
        tablePanel.sortLatestDate();
        flatViewCB.setSelected(true);
        flatMode.setValue(flatViewCB.isSelected());

        // Triggers mode change and schedule filtering (MyActionListener).
        setFilterComboBox(DirectoryFilter.FILE_FILTER_MODE_NEW_ONLY);
        filterTextField.reset();
    }

    public void setFolderInfoDeleted(FolderInfo folderInfo) {
        folder = getController().getFolderRepository().getFolder(folderInfo);
        updateNodes();
        directoryFilter.setFolder(folder,
            FileInfoFactory.lookupDirectory(folder.getInfo(), ""));
        tablePanel.setFolder(folder);
        tablePanel.sortLatestDate();
        flatMode.setValue(flatViewCB.isSelected());

        // Triggers mode change and schedule filtering (MyActionListener).
        setFilterComboBox(DirectoryFilter.FILE_FILTER_MODE_DELETED_PREVIOUS);
        filterTextField.reset();
    }

    public void setFolderInfoUnsynced(FolderInfo folderInfo) {
        folder = getController().getFolderRepository().getFolder(folderInfo);
        updateNodes();
        directoryFilter.setFolder(folder,
            FileInfoFactory.lookupDirectory(folder.getInfo(), ""));
        tablePanel.setFolder(folder);
        tablePanel.sortLatestDate();
        flatViewCB.setSelected(true);
        flatMode.setValue(flatViewCB.isSelected());

        // Triggers mode change and schedule filtering (MyActionListener).
        setFilterComboBox(DirectoryFilter.FILE_FILTER_MODE_UNSYNCHRONIZED);
        filterTextField.reset();
    }

    /**
     * Set the tab with details for a folder with incoming files.
     * 
     * @param folderInfo
     */
    // public void setFolderInfoIncoming(FolderInfo folderInfo) {
    // folder = getController().getFolderRepository().getFolder(folderInfo);
    // updateNodes();
    // directoryFilter.setFolder(folder, FileInfoFactory.lookupDirectory(
    // folder.getInfo(), ""));
    // tablePanel.setFolder(folder);
    // flatViewCB.setSelected(true);
    // flatMode.setValue(flatViewCB.isSelected());
    //
    // // Triggers mode change and schedule filtering (MyActionListener).
    // setFilterComboBox(DirectoryFilter.FILE_FILTER_MODE_INCOMING_ONLY);
    // filterTextField.reset();
    // }

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
            "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, fill:pref:grow, 3dlu, pref, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createToolBar(), cc.xy(2, 2));
        builder.add(padBreadcrumb(), cc.xy(2, 4));
        builder.addSeparator(null, cc.xyw(1, 6, 3));

        builder.add(tablePanel.getUIComponent(), cc.xy(2, 8));
        builder.addSeparator(null, cc.xy(2, 10));
        builder.add(statsPanel.getUiComponent(), cc.xy(2, 11));
        uiComponent = builder.getPanel();
    }

    private Component padBreadcrumb() {
        FormLayout layout = new FormLayout("pref, pref:grow",
            "pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(breadcrumbPanel.getUiComponent(), cc.xy(1, 1));
        return builder.getPanel();
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {

        DetailsAction detailsAction = new DetailsAction(getController());
        JToggleButton detailsButton = new JToggleButton(detailsAction);
        detailsButton.setIcon(null);

        JButton fileArchiveButton = new JButton(new MyFileArchiveAction(
            getController()));
        fileArchiveButton.setIcon(null);

        flatViewCB = new JCheckBox(
            Translation.getTranslation("files_tab.flat_view.text"));
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
        bar.addGridded(fileArchiveButton);

        builder.add(bar.getPanel(), cc.xy(1, 1));
        builder.add(flatViewCB, cc.xy(3, 1));
        builder.add(filterSelectionComboBox, cc.xy(5, 1));
        builder.add(filterTextField.getUIComponent(), cc.xy(7, 1));

        return builder.getPanel();
    }

    public void adviseOfChange(FilteredDirectoryEvent event) {
        statsPanel.setStats(event.getLocalFiles(), event.getDeletedFiles());
    }

    public void adviseOfFilteringBegin() {
    }

    public void invalidate() {
        statsPanel.setStats(0, 0);
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

    public void selectionChanged(String relativeName) {
        DirectoryInfo dir = FileInfoFactory.lookupDirectory(folder.getInfo(),
            relativeName);
        directoryFilter.setFolder(folder, dir);
        breadcrumbPanel.setDirectory(folder.getInfo(), dir);
    }

    public void fileArchive() {
        if (folder != null) {
            DiskItem[] diskItems = tablePanel.getSelectedRows();
            List<FileInfo> fileInfosToRestore = new ArrayList<FileInfo>();
            if (diskItems.length > 0) {
                for (DiskItem diskItem : diskItems) {
                    if (diskItem instanceof DirectoryInfo) {
                        DirectoryInfo di = (DirectoryInfo) diskItem;
                        FileInfoCriteria criteria = new FileInfoCriteria();
                        criteria.addWriteMembersAndMyself(folder);
                        criteria.setType(FileInfoCriteria.Type.FILES_ONLY);
                        criteria.setPath(di);
                        criteria.setRecursive(true);
                        Collection<FileInfo> infoCollection = folder.getDAO()
                            .findFiles(criteria);
                        for (FileInfo fileInfo : infoCollection) {
                            fileInfosToRestore.add(fileInfo);
                        }
                    } else if (diskItem instanceof FileInfo) {
                        fileInfosToRestore.add((FileInfo) diskItem);
                    }
                }
            } else {
                // Nothing selected. Do include everything also deleted
                FileInfoCriteria criteria = new FileInfoCriteria();
                criteria.addWriteMembersAndMyself(folder);
                criteria.setType(FileInfoCriteria.Type.FILES_ONLY);
                criteria.setPath(directoryFilter.getCurrentDirectoryInfo());
                criteria.setRecursive(true);
                Collection<FileInfo> infoCollection = folder.getDAO()
                    .findFiles(criteria);
                for (FileInfo fileInfo : infoCollection) {
                    fileInfosToRestore.add(fileInfo);
                }
            }

            PFWizard wizard = new PFWizard(getController(),
                Translation.getTranslation("wizard.pfwizard.restore_title"));
            MultiFileRestorePanel panel = new MultiFileRestorePanel(
                getController(), folder, fileInfosToRestore);
            wizard.open(panel);
        }
    }

    public void resetFilters() {
        filterTextField.reset();
        flatViewCB.setSelected(false);
        setFilterComboBox(DirectoryFilter.FILE_FILTER_MODE_LOCAL_AND_INCOMING);
    }

    private void setFilterComboBox(int index) {
        int oldValue = filterSelectionComboBox.getSelectedIndex();
        if (oldValue != index) {
            filterSelectionComboBox.setSelectedIndex(index);
        }
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

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

    @SuppressWarnings("serial")
    private class DetailsAction extends BaseAction {

        DetailsAction(Controller controller) {
            super("action_details", controller);
        }

        public void actionPerformed(ActionEvent e) {
            tablePanel.toggleDetails();
        }
    }

    @SuppressWarnings("serial")
    private class MyFileArchiveAction extends BaseAction {

        private MyFileArchiveAction(Controller controller) {
            super("action_file_archive", controller);
        }

        public void actionPerformed(ActionEvent e) {
            fileArchive();
        }
    }

    private class MyNodeManagerListener implements NodeManagerListener {
        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateNodes();
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateNodes();
        }

        public void friendAdded(NodeManagerEvent e) {
        }

        public void friendRemoved(NodeManagerEvent e) {
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnecting(NodeManagerEvent e) {
        }

        public void nodeOffline(NodeManagerEvent e) {
        }

        public void nodeOnline(NodeManagerEvent e) {
        }

        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public void startStop(NodeManagerEvent e) {
        }

    }

}