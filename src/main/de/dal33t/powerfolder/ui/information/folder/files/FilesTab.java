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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.uif_lite.component.UIFSplitPane;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.information.folder.files.table.FilesTablePanel;
import de.dal33t.powerfolder.ui.information.folder.files.tree.FilesTreePanel;
import de.dal33t.powerfolder.ui.widget.FilterTextField;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * UI component for the folder files tab
 */
public class FilesTab extends PFUIComponent
        implements DirectoryFilterListener {
    private JPanel uiComponent;
    private JSplitPane splitPane;
    private FilesTablePanel tablePanel;
    private FilterTextField filterTextField;
    private JComboBox filterSelectionComboBox;
    private FilesStatsPanel statsPanel;
    private DirectoryFilter directoryFilter;

    /**
     * Constructor
     *
     * @param controller
     */
    public FilesTab(Controller controller) {
        super(controller);

        statsPanel = new FilesStatsPanel(getController());

        directoryFilter = new DirectoryFilter(controller);
        directoryFilter.addListener(this);

        FilesTreePanel treePanel = new FilesTreePanel(controller);
        directoryFilter.addListener(treePanel);

        tablePanel = new FilesTablePanel(controller);
        directoryFilter.addListener(tablePanel);
        treePanel.addTreeSelectionListener(tablePanel);

        splitPane = new UIFSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                treePanel.getUIComponent(), tablePanel.getUIComponent());
        int dividerLocation = getController().getPreferences().getInt(
                "files.tab.location", 150);
        splitPane.setDividerLocation(dividerLocation);
        splitPane.addPropertyChangeListener(new MyPropertyChangeListner());
        filterTextField = new FilterTextField(12,
                Translation.getTranslation("files_tab.filter_by_file_name.hint"),
                Translation.getTranslation("files_tab.filter_by_file_name.tool_tip"));
        directoryFilter.setSearchField(filterTextField.getValueModel());
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
        Folder folder = getController().getFolderRepository().getFolder(folderInfo);
        directoryFilter.setFolder(folder);
        tablePanel.setFolder(folder);
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
            directoryFilter.setFilterMode(directoryFilterMode);
            directoryFilter.scheduleFiltering();
        }
    }

    /**
     * Set the tab with details for a folder with local / incoming set
     * and sort date descending.
     *
     * @param folderInfo
     */
    public void setFolderInfoLatest(FolderInfo folderInfo) {

        Folder folder = getController().getFolderRepository().getFolder(
                folderInfo);
        directoryFilter.setFolder(folder);
        tablePanel.setFolder(folder);
        tablePanel.sortLatestDate();

        // Triggers mode change and schedule filtering (MyActionListener).
        filterSelectionComboBox.setSelectedIndex(
                DirectoryFilter.MODE_LOCAL_AND_INCOMING);
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
        FormLayout layout = new FormLayout("pref, fill:pref:grow, pref",
                "pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(filterTextField.getUIComponent(), cc.xy(3, 1));
        builder.add(filterSelectionComboBox, cc.xy(1, 1));
        return builder.getPanel();
    }

    public void adviseOfChange(FilteredDirectoryEvent event) {
        statsPanel.setStats(event.getLocalFiles(), event.getIncomingFiles(),
                event.getDeletedFiles(), event.getRecycledFiles());
    }

    /**
     * Detect changes to the split pane location.
     */
    private class MyPropertyChangeListner implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource().equals(splitPane)
                    && evt.getPropertyName().equals("dividerLocation")) {
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
                directoryFilter.setFilterMode(filterSelectionComboBox
                        .getSelectedIndex());
                directoryFilter.scheduleFiltering();
            }
        }
    }
}