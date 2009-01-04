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
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.information.HasDetailsPanel;
import de.dal33t.powerfolder.ui.information.folder.files.DirectoryFilterListener;
import de.dal33t.powerfolder.ui.information.folder.files.FileDetailsPanel;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryEvent;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryModel;
import de.dal33t.powerfolder.ui.information.folder.files.tree.DirectoryTreeNodeUserObject;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.SwingWorker;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class FilesTablePanel extends PFUIComponent implements HasDetailsPanel,
        TreeSelectionListener, DirectoryFilterListener {

    private JPanel uiComponent;
    private FileDetailsPanel fileDetailsPanel;
    private FilesTableModel tableModel;
    private FilesTable table;
    private OpenFileAction openFileAction;
    private DeleteFileAction deleteFileAction;
    private RestoreFileAction restoreFileAction;
    private DownloadFileAction downloadFileAction;
    private AbortDownloadAction abortDownloadAction;
    private AddIgnoreAction addIgnoreAction;
    private RemoveIgnoreAction removeIgnoreAction;
    private UnmarkAction unmarkAction;
    private JPopupMenu fileMenu;

    public FilesTablePanel(Controller controller) {
        super(controller);
        fileDetailsPanel = new FileDetailsPanel(controller);
        tableModel = new FilesTableModel(controller);
        table = new FilesTable(tableModel);
        table.getSelectionModel().addListSelectionListener(new MyListSelectionListener());
        table.getTableHeader().addMouseListener(new TableHeaderMouseListener());
        table.addMouseListener(new TableMouseListener());
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

        buildPopupMenus();

        uiComponent = builder.getPanel();
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        openFileAction = new OpenFileAction();
        openFileAction.setEnabled(false);
        downloadFileAction = new DownloadFileAction();
        downloadFileAction.setEnabled(false);
        restoreFileAction = new RestoreFileAction();
        restoreFileAction.setEnabled(false);
        abortDownloadAction = new AbortDownloadAction(getController());
        abortDownloadAction.setEnabled(false);
        deleteFileAction = new DeleteFileAction();
        deleteFileAction.setEnabled(false);
        addIgnoreAction = new AddIgnoreAction(getController());
        addIgnoreAction.setEnabled(false);
        removeIgnoreAction = new RemoveIgnoreAction(getController());
        removeIgnoreAction.setEnabled(false);
        unmarkAction = new UnmarkAction(getController());
        unmarkAction.setEnabled(false);

        bar.addGridded(new JToggleButton(new DetailsAction(getController())));
        bar.addRelatedGap();
        bar.addGridded(new JButton(downloadFileAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(openFileAction));
        return bar.getPanel();
    }

    /**
     * Builds the popup menus
     */
    private void buildPopupMenus() {
        fileMenu = new JPopupMenu();
        fileMenu.add(downloadFileAction);
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            fileMenu.add(openFileAction);
        }
        fileMenu.add(abortDownloadAction);
        fileMenu.add(deleteFileAction);
        fileMenu.add(restoreFileAction);
        fileMenu.add(addIgnoreAction);
        fileMenu.add(removeIgnoreAction);
        fileMenu.add(unmarkAction);
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

    public void openSelectedFile() {
        if (table == null || tableModel == null) {
            return;
        }
        int[] rows = table.getSelectedRows();
        boolean singleRowSelected = rows.length == 1;
        if (singleRowSelected) {
            FileInfo fileInfo = tableModel.getFileInfoAtRow(rows[0]);
            File file = fileInfo.getDiskFile(
                    getController().getFolderRepository());
            if (file != null && file.exists()) {
                try {
                    FileUtils.openFile(file);
                } catch (IOException ex) {
                    logSevere(ex);
                }
            }
        }
    }

    private void restoreSelectedFile() {
        if (table == null || tableModel == null) {
            return;
        }
        int[] rows = table.getSelectedRows();
        boolean singleRowSelected = rows.length == 1;
        if (singleRowSelected) {
            final FileInfo fileInfo = tableModel.getFileInfoAtRow(rows[0]);

            SwingWorker worker = new ActivityVisualizationWorker(
                    getController().getUIController().getMainFrame().getUIComponent()) {

                @Override
                protected String getTitle() {
                    return Translation.getTranslation("restore.busy.title");
                }

                @Override
                protected String getWorkingText() {
                    return Translation.getTranslation("restore.busy.description");
                }

                public Object construct() {
                    boolean succes = true;
                    RecycleBin recycleBin = getController().getRecycleBin();
                    if (recycleBin.isInRecycleBin(fileInfo)) {
                        if (!recycleBin.restoreFromRecycleBin(fileInfo)) {
                            succes = false;
                        }
                    }
                    return succes;
                }
            };

            // do in different thread
            worker.start();
        }
    }

    private void deleteSelectedFile() {
        if (table == null || tableModel == null) {
            return;
        }
        int[] rows = table.getSelectedRows();
        boolean singleRowSelected = rows.length == 1;
        final FileInfo fileInfo = tableModel.getFileInfoAtRow(rows[0]);
        if (singleRowSelected) {
            SwingWorker worker = new ActivityVisualizationWorker(getUIController()) {
                public Object construct() {
                    FolderRepository repo = getController().getFolderRepository();
                    Folder folder = fileInfo.getFolder(repo);
                    folder.removeFilesLocal(fileInfo);
                    return null;
                }

                protected String getTitle() {
                    return Translation.getTranslation("delete.busy.title");
                }

                protected String getWorkingText() {
                    return Translation.getTranslation("delete.busy.description");
                }
            };
            worker.start();
        }
    }

    private FileInfo getSelectedRow() {
        int min = table.getSelectionModel().getMinSelectionIndex();
        int max = table.getSelectionModel().getMaxSelectionIndex();
        if (min == max && min >= 0) {
            return tableModel.getFileInfoAtRow(min);
        }

        return null;
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    private class DownloadFileAction extends BaseAction {
        DownloadFileAction() {
            super("action_download_file",
                    FilesTablePanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            SwingWorker worker = new ActivityVisualizationWorker(getUIController()) {

                @Override
                public Object construct() {
                    FileInfo fileInfo = getSelectedRow();
                    if (fileInfo != null) {
                        FolderRepository repo = getController().getFolderRepository();
                        Folder folder = fileInfo.getFolder(repo);
                        if (folder == null) {
                            return null;
                        }
                        if (fileInfo.isDownloading(getController())) {
                            return null;
                        }
                        getController().getTransferManager().downloadNewestVersion(fileInfo);
                    }
                    return null;
                }

                @Override
                protected String getTitle() {
                    return Translation.getTranslation("download.busy.title");
                }

                @Override
                protected String getWorkingText() {
                    return Translation.getTranslation("download.busy.description");
                }

            };

            worker.start();
        }
    }

    private class DeleteFileAction extends BaseAction {
        DeleteFileAction() {
            super("action_delete_file",
                    FilesTablePanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            deleteSelectedFile();
        }
    }

    private class RestoreFileAction extends BaseAction {
        RestoreFileAction() {
            super("action_restore_file",
                    FilesTablePanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            restoreSelectedFile();
        }
    }

    private class OpenFileAction extends BaseAction {
        OpenFileAction() {
            super("action_open_file",
                    FilesTablePanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            openSelectedFile();
        }
    }

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
            FileInfo fileInfo = getSelectedRow();
            TransferManager tm = getController().getTransferManager();
            if (fileInfo != null) {
                fileDetailsPanel.setFileInfo(fileInfo);
                openFileAction.setEnabled(true);

                // Enable download action if file is download-able.
                FolderRepository repo = getController()
                        .getFolderRepository();
                boolean state = true;
                if (fileInfo.diskFileExists(getController())
                        && !fileInfo.isNewerAvailable(repo)) {
                    state = false;
                } else if (!(fileInfo.isDeleted()
                        || fileInfo.isExpected(repo)
                        || fileInfo.isNewerAvailable(repo))) {
                    state = false;
                } else {
                    if (tm.getActiveDownload(fileInfo) != null) {
                        return;
                    }
                }
                downloadFileAction.setEnabled(state);

                // Enable restore / (!delete) action if file is restore-able.
                state = fileInfo.isDeleted() && getController()
                        .getRecycleBin().isInRecycleBin(fileInfo);
                restoreFileAction.setEnabled(state);
                deleteFileAction.setEnabled(!state);

                DownloadManager dl = getController().getTransferManager()
                        .getActiveDownload(fileInfo);
                abortDownloadAction.setEnabled(dl != null);

                boolean retained = tableModel.getFolder()
                        .getDiskItemFilter().isRetained(fileInfo);
                addIgnoreAction.setEnabled(retained);
                removeIgnoreAction.setEnabled(!retained);

                unmarkAction.setEnabled(tm.isCompletedDownload(fileInfo));

                return;
            }

            fileDetailsPanel.setFileInfo(null);
            openFileAction.setEnabled(false);
            openFileAction.setEnabled(false);
            restoreFileAction.setEnabled(false);
            deleteFileAction.setEnabled(false);
            abortDownloadAction.setEnabled(false);
            addIgnoreAction.setEnabled(false);
            removeIgnoreAction.setEnabled(false);
            unmarkAction.setEnabled(false);
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

    private class TableMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        private void showContextMenu(MouseEvent evt) {
            fileMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    /**
     * Action to abort the current download.
     */
    private class AbortDownloadAction extends BaseAction {

        private AbortDownloadAction(Controller controller) {
            super("action_abort_download", controller);
        }

        public void actionPerformed(ActionEvent e) {
            FileInfo fileInfo = getSelectedRow();
            if (fileInfo != null) {
                TransferManager tm =  getController().getTransferManager();
                DownloadManager dl = tm.getActiveDownload(fileInfo);
                if (dl != null) {
                    dl.abort();
                }
            }
        }
    }

    private class AddIgnoreAction extends BaseAction {

        private AddIgnoreAction(Controller controller) {
            super("action_add_ignore", controller);
        }

        public void actionPerformed(ActionEvent e) {
            FileInfo fileInfo = getSelectedRow();
            if (fileInfo != null) {
                tableModel.getFolder().getDiskItemFilter().addPattern(
                        fileInfo.getName());
            }
        }
    }

    private class RemoveIgnoreAction extends BaseAction {

        private RemoveIgnoreAction(Controller controller) {
            super("action_remove_ignore", controller);
        }

        public void actionPerformed(ActionEvent e) {
            FileInfo fileInfo = getSelectedRow();
            if (fileInfo != null) {
                tableModel.getFolder().getDiskItemFilter().removePattern(
                        fileInfo.getName());
            }
        }
    }

    private class UnmarkAction extends BaseAction {

        private UnmarkAction(Controller controller) {
            super("action_unmark", controller);
        }

        public void actionPerformed(ActionEvent e) {
            FileInfo fileInfo = getSelectedRow();
            if (fileInfo != null) {
                TransferManager transferManager = getController()
                        .getTransferManager();
                if (transferManager.isCompletedDownload(fileInfo)) {
                    transferManager.clearCompletedDownload(fileInfo);
                }
            }
        }
    }
}
