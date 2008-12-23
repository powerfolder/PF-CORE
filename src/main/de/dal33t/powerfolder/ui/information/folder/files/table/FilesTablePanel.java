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
* $Id: FilesTablePanel.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.files.table;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.ui.UIUtil;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.information.HasDetailsPanel;
import de.dal33t.powerfolder.ui.information.folder.files.DirectoryFilterListener;
import de.dal33t.powerfolder.ui.information.folder.files.FileDetailsPanel;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryEvent;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryModel;
import de.dal33t.powerfolder.ui.information.folder.files.tree.DirectoryTreeNodeUserObject;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FilesTablePanel extends PFUIComponent implements HasDetailsPanel,
        TreeSelectionListener, DirectoryFilterListener {

    private JPanel uiComponent;
    private FileDetailsPanel fileDetailsPanel;
    private FilesTableModel tableModel;
    private FilesTable table;

    public FilesTablePanel(Controller controller) {
        super(controller);
        fileDetailsPanel = new FileDetailsPanel(controller);
        tableModel = new FilesTableModel(controller);
        table = new FilesTable(tableModel);
        table.getSelectionModel().addListSelectionListener(new MyListSelectionListener());
        table.getTableHeader().addMouseListener(new TableHeaderMouseListener());
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
     * Builds the ui component.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("fill:pref:grow",
                "pref, 3dlu, pref, 3dlu, fill:0:grow, 3dlu, pref");
        //       tools       sep,        table,             details
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JScrollPane tableScroller = new JScrollPane(table);

        UIUtil.whiteStripTable(table);
        UIUtil.setZeroHeight(tableScroller);
        UIUtil.removeBorder(tableScroller);

        builder.add(createToolBar(), cc.xy(1, 1));
        builder.addSeparator(null, cc.xy(1, 3));
        builder.add(tableScroller, cc.xy(1, 5));
        builder.add(fileDetailsPanel.getPanel(), cc.xy(1, 7));
        uiComponent = builder.getPanel();
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JToggleButton(new DetailsAction(getController())));
        return bar.getPanel();
    }

    /**
     * Toggle the details panel visibility.
     */
    public void toggleDetails() {
        fileDetailsPanel.getPanel().setVisible(
                !fileDetailsPanel.getPanel().isVisible());
    }

    /**
     * Find the correct model in the tree to display when a change occurs.
     * @param event
     */
    public void adviseOfChange(FilteredDirectoryEvent event) {
        // Try to find the correct FilteredDirectoryModel for the selected
        // directory.
        FilteredDirectoryModel filteredDirectoryModel = event.getModel();
        tableModel.setFilteredDirectoryModel(filteredDirectoryModel);
    }

    /**
     * Handle tree selection changes, which determine the table entries to
     * display.
     *
     * @param e
     */
    public void valueChanged(TreeSelectionEvent e) {
        if (e.isAddedPath()) {
            Object[] path = e.getPath().getPath();
            Object lastItem = path[path.length - 1];
            if (lastItem instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastItem;
                Object userObject = node.getUserObject();
                if (userObject instanceof DirectoryTreeNodeUserObject) {
                    DirectoryTreeNodeUserObject dtnuo =
                            (DirectoryTreeNodeUserObject) userObject;
                    tableModel.setSelectedDirectory(dtnuo.getFile());
                    return;
                }
            }
        }

        // Failed to set file - clear selection.
        tableModel.setSelectedDirectory(null);
    }

    public void setFolder(Folder folder) {
        tableModel.setFolder(folder);
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    private class DetailsAction extends BaseAction {

        DetailsAction(Controller controller) {
            super("action_details", controller);
        }

        public void actionPerformed(ActionEvent e) {
            toggleDetails();
        }
    }

    private class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            int min = table.getSelectionModel().getMinSelectionIndex();
            int max = table.getSelectionModel().getMaxSelectionIndex();
            if (min == max && min >= 0) {
                DiskItem diskItem = (DiskItem) tableModel.getValueAt(min, 0);
                if (diskItem instanceof FileInfo) {
                    FileInfo fi = (FileInfo) diskItem;
                    fileDetailsPanel.setFileInfo(fi);
                    return;
                }
            }
            fileDetailsPanel.setFileInfo(null);
        }
    }

    /**
     * Listener on table header, takes care about the sorting of table
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private static class TableHeaderMouseListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                JTableHeader tableHeader = (JTableHeader) e.getSource();
                int columnNo = tableHeader.columnAtPoint(e.getPoint());
                TableColumn column = tableHeader.getColumnModel().getColumn(
                    columnNo);
                int modelColumnNo = column.getModelIndex();
                TableModel model = tableHeader.getTable().getModel();
                if (model instanceof FilesTableModel) {
                    FilesTableModel filesTableModel = (FilesTableModel) model;
                    boolean freshSorted = filesTableModel.sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        filesTableModel.reverseList();
                    }
                }
            }
        }
    }

}
