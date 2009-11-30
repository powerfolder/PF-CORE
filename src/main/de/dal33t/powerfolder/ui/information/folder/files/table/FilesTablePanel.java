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

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.information.HasDetailsPanel;
import de.dal33t.powerfolder.ui.information.folder.files.DirectoryFilterListener;
import de.dal33t.powerfolder.ui.information.folder.files.FileDetailsPanel;
import de.dal33t.powerfolder.ui.information.folder.files.FilesTab;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryEvent;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryModel;
import de.dal33t.powerfolder.ui.information.folder.files.tree.DirectoryTreeNodeUserObject;
import de.dal33t.powerfolder.ui.information.folder.files.versions.FileVersionsPanel;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.ui.wizard.MultiFileRestorePanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.SwingWorker;
import de.dal33t.powerfolder.util.ui.UIUtil;

public class FilesTablePanel extends PFUIComponent implements HasDetailsPanel,
    TreeSelectionListener, DirectoryFilterListener
{

    private JPanel uiComponent;
    private FileDetailsPanel fileDetailsPanel;
    private FileVersionsPanel fileVersionsPanel;
    private JPanel detailsPanel;
    private FilesTableModel tableModel;
    private FilesTable table;
    private OpenFileAction openFileAction;
    private DeleteFileAction deleteFileAction;
    private DownloadFileAction downloadFileAction;
    private AbortDownloadAction abortDownloadAction;
    private AddIgnoreAction addIgnoreAction;
    private RemoveIgnoreAction removeIgnoreAction;
    private UnmarkAction unmarkAction;
    private SingleFileTransferAction singleFileTransferAction;
    private RestoreArchiveAction restoreArchiveAction;
    private JPopupMenu fileMenu;
    private JScrollPane tableScroller;
    private JLabel emptyLabel;
    private FilesTab parent;

    public FilesTablePanel(Controller controller, FilesTab parent) {
        super(controller);
        this.parent = parent;
        fileDetailsPanel = new FileDetailsPanel(controller, false);
        fileVersionsPanel = new FileVersionsPanel(controller);
        detailsPanel = createDetailsPanel();
        detailsPanel.setVisible(false);
        tableModel = new FilesTableModel(controller);
        tableModel.addTableModelListener(new MyTableModelListener());
        table = new FilesTable(tableModel);
        table.getSelectionModel().addListSelectionListener(
            new MyListSelectionListener());
        table.addMouseListener(new TableMouseListener());

        table.registerKeyboardAction(new SelectAllAction(),
		KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK),
		JComponent.WHEN_FOCUSED);
        
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

        createToolBar();

        FormLayout layout = new FormLayout("fill:pref:grow",
            "fill:0:grow, 3dlu, pref");
        // table, details
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        tableScroller = new JScrollPane(table);
        emptyLabel = new JLabel(Translation
            .getTranslation("files_table_panel.no_files_available"),
            SwingConstants.CENTER);
        emptyLabel.setEnabled(false);

        UIUtil.whiteStripTable(table);
        UIUtil.setZeroHeight(tableScroller);
        UIUtil.removeBorder(tableScroller);

        // tableScroller and emptyLabel occupy the same slot
        builder.add(tableScroller, cc.xy(1, 1));
        builder.add(emptyLabel, cc.xy(1, 1));

        builder.add(detailsPanel, cc.xy(1, 3));

        buildPopupMenus();

        uiComponent = builder.getPanel();
        updateEmptyLabel();
    }

    /**
     * @return the toolbar
     */
    private void createToolBar() {
        openFileAction = new OpenFileAction();
        openFileAction.setEnabled(false);
        downloadFileAction = new DownloadFileAction();
        downloadFileAction.setEnabled(false);
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
        singleFileTransferAction = new SingleFileTransferAction(getController());
        singleFileTransferAction.setEnabled(false);
        restoreArchiveAction = new RestoreArchiveAction(getController());
        restoreArchiveAction.setEnabled(false);
    }

    /**
     * Builds the popup menus
     */
    private void buildPopupMenus() {
        fileMenu = new JPopupMenu();
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            fileMenu.add(openFileAction);
        }
        fileMenu.add(downloadFileAction);
        fileMenu.add(abortDownloadAction);
        fileMenu.add(deleteFileAction);
        fileMenu.add(addIgnoreAction);
        fileMenu.add(removeIgnoreAction);
        fileMenu.add(unmarkAction);
        fileMenu.add(restoreArchiveAction);
        // fileMenu.add(singleFileTransferAction);
    }

    /**
     * Toggle the details panel visibility.
     */
    public void toggleDetails() {
        detailsPanel.setVisible(!detailsPanel.isVisible());
    }

    /**
     * Find the correct model in the tree to display when a change occurs.
     * 
     * @param event
     */
    public void adviseOfChange(FilteredDirectoryEvent event) {
        // Try to find the correct FilteredDirectoryModel for the selected
        // directory.
        FilteredDirectoryModel filteredDirectoryModel = event.getFlatModel();
        tableModel.setFilteredDirectoryModel(filteredDirectoryModel, event
            .isFlat());
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
                    DirectoryTreeNodeUserObject dtnuo = (DirectoryTreeNodeUserObject) userObject;
                    tableModel.setSelectedRelativeName(dtnuo.getRelativeName());
                    return;
                }
            }
        }

        // Failed to set file - clear selection.
        tableModel.setSelectedRelativeName(null);
    }

    public void setFolder(Folder folder) {
        tableModel.setFolder(folder);
    }

    public void openSelectedFile() {
        if (table == null || tableModel == null) {
            return;
        }
        int[] rows = table.getSelectedRows();
        DiskItem[] diskItems = tableModel.getDiskItemsAtRows(rows);
        for (DiskItem diskItem : diskItems) {
            if (diskItem instanceof Directory) {
                Directory directory = (Directory) diskItem;
                File file = directory.getAbsoluteFile();
                if (file.exists()) {
                    try {
                        FileUtils.openFile(file);
                    } catch (IOException ex) {
                        logSevere(ex);
                    }
                }
            } else if (diskItem instanceof FileInfo) {
                FileInfo fileInfo = (FileInfo) diskItem;
                File file = fileInfo.getDiskFile(getController()
                        .getFolderRepository());
                if (file != null && file.exists()) {
                    try {
                        FileUtils.openFile(file);
                    } catch (IOException ex) {
                        logSevere(ex);
                    }
                }
            }
        }
    }

    private void deleteSelectedFiles() {
        if (table == null || tableModel == null) {
            return;
        }
        int[] rows = table.getSelectedRows();
        final DiskItem[] diskItems = tableModel.getDiskItemsAtRows(rows);
        SwingWorker worker = new ActivityVisualizationWorker(getUIController())
        {
            public Object construct() {
                FolderRepository repo = getController().getFolderRepository();
                for (DiskItem diskItem : diskItems) {
                    try {
                        if (diskItem instanceof FileInfo) {
                            FileInfo fileInfo = (FileInfo) diskItem;
                            Folder folder = fileInfo.getFolder(repo);
                            folder.removeFilesLocal(fileInfo);
                        } else if (diskItem instanceof Directory) {
                            Directory directory = (Directory) diskItem;
                            Collection<FileInfo> fileInfoList = directory.getFileInfos();
                            Folder folder = directory.getRootFolder();
                            folder.removeFilesLocal(fileInfoList);
                        }
                    } catch (Exception e) {
                        logSevere(e);
                    }
                }
                return null;
            }

            protected String getTitle() {
                return Translation.getTranslation("delete.busy.title");
            }

            protected String getWorkingText() {
                return Translation
                    .getTranslation("delete.busy.description");
            }
        };
        worker.start();
    }

    private DiskItem[] getSelectedRows() {
        int[] rows = table.getSelectedRows();
        return tableModel.getDiskItemsAtRows(rows);
    }

    private void updateEmptyLabel() {
        if (tableScroller != null) {
            tableScroller.setVisible(tableModel.getRowCount() != 0);
        }
        if (emptyLabel != null) {
            emptyLabel.setVisible(tableModel.getRowCount() == 0);
        }

    }

    public void sortLatestDate() {
        tableModel.sortLatestDate();
    }

    /**
     * When a user double-clicks a row
     */
    private void handleDoubleClick() {
            int index = table.getSelectionModel().getLeadSelectionIndex();
            DiskItem diskItem = tableModel.getDiskItemsAtRows(new int[]{index})[0];
            if (diskItem != null) {
                if (diskItem instanceof Directory) {
                    Directory directory = (Directory) diskItem;
                    // Double click on a directory makes that directory the
                    // selected one in the tree.
                    parent.setSelection(directory);
                } else if (diskItem instanceof FileInfo) {
                    // Default to download if possible, else try open.
                    if (downloadFileAction.isEnabled()) {
                        ActionEvent ae = new ActionEvent(this, 0,
                                downloadFileAction.getName());
                        downloadFileAction.actionPerformed(ae);
                    } else if (openFileAction.isEnabled()) {
                        ActionEvent ae = new ActionEvent(this, 0,
                                openFileAction.getName());
                        openFileAction.actionPerformed(ae);
                    }
                }
            }
    }

    private JPanel createDetailsPanel() {
        FormLayout layout = new FormLayout("fill:pref:grow", "pref, 3dlu, pref");
        // spacer, tabs
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Spacer
        builder.addSeparator(null, cc.xy(1, 1));

        JTabbedPane tabbedPane = new JTabbedPane();
        builder.add(tabbedPane, cc.xy(1, 3));

        tabbedPane.add(fileDetailsPanel.getPanel(), Translation
            .getTranslation("files_table_panel.file_details_tab.text"));
        tabbedPane.setToolTipTextAt(0, Translation
            .getTranslation("files_table_panel.file_details_tab.tip"));
        tabbedPane.setIconAt(0, Icons.getIconById(Icons.FILE_DETAILS));

        tabbedPane.add(fileVersionsPanel.getPanel(), Translation
            .getTranslation("files_table_panel.file_versions_tab.text"));
        tabbedPane.setToolTipTextAt(1, Translation
            .getTranslation("files_table_panel.file_versions_tab.tip"));
        tabbedPane.setIconAt(1, Icons.getIconById(Icons.FILE_VERSION));

        return builder.getPanel();
    }

    // /////////////////
    // Inner Classes //
    // /////////////////

    private class DownloadFileAction extends BaseAction {
        DownloadFileAction() {
            super("action_download_file", FilesTablePanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            SwingWorker worker = new ActivityVisualizationWorker(
                getUIController())
            {

                @Override
                public Object construct() {
                    for (DiskItem diskItem : getSelectedRows()) {
                        if (diskItem instanceof FileInfo) {
                            FileInfo fileInfo = (FileInfo) diskItem;
                            FolderRepository repo = getController()
                                .getFolderRepository();
                            Folder folder = fileInfo.getFolder(repo);
                            if (folder == null) {
                                return null;
                            }
                            if (fileInfo.isDownloading(getController())) {
                                return null;
                            }
                            getController().getTransferManager()
                                .downloadNewestVersion(fileInfo);
                        }
                    }
                    return null;
                }

                @Override
                protected String getTitle() {
                    return Translation.getTranslation("download.busy.title");
                }

                @Override
                protected String getWorkingText() {
                    return Translation
                        .getTranslation("download.busy.description");
                }

            };

            worker.start();
        }
    }

    private class DeleteFileAction extends BaseAction {
        DeleteFileAction() {
            super("action_delete_file", FilesTablePanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            deleteSelectedFiles();
        }
    }

    private class OpenFileAction extends BaseAction {
        OpenFileAction() {
            super("action_open_file", FilesTablePanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            openSelectedFile();
        }
    }

    private class MyListSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            boolean done = false;
            if (getSelectedRows().length > 0) {
                DiskItem diskItem = getSelectedRows()[0];
                if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
                    openFileAction.setEnabled(diskItem != null);
                }
                if (diskItem != null && diskItem instanceof FileInfo) {
                    TransferManager tm = getController().getTransferManager();
                    FileInfo fileInfo = (FileInfo) diskItem;
                    fileDetailsPanel.setFileInfo(fileInfo);
                    fileVersionsPanel.setFileInfo(fileInfo);

                    // Enable download action if file is download-able.
                    FolderRepository repo = getController().getFolderRepository();
                    boolean state = true;
                    if (fileInfo.diskFileExists(getController())
                        && !fileInfo.isNewerAvailable(repo))
                    {
                        state = false;
                    } else if (!(fileInfo.isDeleted() || fileInfo.isExpected(repo) || fileInfo
                        .isNewerAvailable(repo)))
                    {
                        state = false;
                    } else {
                        if (tm.getActiveDownload(fileInfo) != null) {
                            return;
                        }
                    }
                    downloadFileAction.setEnabled(state);

                    // Enable restore / (!delete) action if file is restore-able.
                    deleteFileAction.setEnabled(!state);

                    DownloadManager dl = getController().getTransferManager()
                        .getActiveDownload(fileInfo);
                    abortDownloadAction.setEnabled(dl != null);

                    boolean retained = tableModel.getFolder().getDiskItemFilter()
                        .isRetained(fileInfo);
                    addIgnoreAction.setEnabled(retained);
                    removeIgnoreAction.setEnabled(!retained);

                    unmarkAction.setEnabled(tm.isCompletedDownload(fileInfo));

                    singleFileTransferAction.setEnabled(true);

                    restoreArchiveAction.setEnabled(fileInfo.isDeleted());

                    done = true;
                } else if (diskItem != null && diskItem instanceof Directory) {
                    Directory diretory = (Directory) diskItem;
                    boolean retained = tableModel.getFolder().getDiskItemFilter()
                        .isRetained(diretory);
                    addIgnoreAction.setEnabled(retained);
                    removeIgnoreAction.setEnabled(!retained);
                    deleteFileAction.setEnabled(true);

                    fileDetailsPanel.setFileInfo(null);
                    fileVersionsPanel.setFileInfo(null);

                    done = true;
                }
            }

            if (!done) {
                fileDetailsPanel.setFileInfo(null);
                fileVersionsPanel.setFileInfo(null);
                deleteFileAction.setEnabled(false);
                abortDownloadAction.setEnabled(false);
                addIgnoreAction.setEnabled(false);
                removeIgnoreAction.setEnabled(false);
                unmarkAction.setEnabled(false);
                singleFileTransferAction.setEnabled(false);
                restoreArchiveAction.setEnabled(false);
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

        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (e.getClickCount() == 2) {
                    handleDoubleClick();
                }
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
            for (DiskItem diskItem : getSelectedRows()) {
                if (diskItem instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) diskItem;
                    TransferManager tm = getController().getTransferManager();
                    DownloadManager dl = tm.getActiveDownload(fileInfo);
                    if (dl != null) {
                        dl.abort();
                    }
                }
            }
        }
    }

    private class AddIgnoreAction extends BaseAction {

        private AddIgnoreAction(Controller controller) {
            super("action_add_ignore", controller);
        }

        public void actionPerformed(ActionEvent e) {
            for (DiskItem diskItem : getSelectedRows()) {
                if (diskItem != null && diskItem instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) diskItem;
                    tableModel.getFolder().getDiskItemFilter().addPattern(
                        fileInfo.getRelativeName());
                } else if (diskItem != null && diskItem instanceof Directory) {
                    Directory directory = (Directory) diskItem;
                    tableModel.getFolder().getDiskItemFilter().addPattern(
                        directory.getRelativeName() + "/*");
                }
            }
        }
    }

    private class RestoreArchiveAction extends BaseAction {

        private RestoreArchiveAction(Controller controller) {
            super("action_restore_archive", controller);
        }

        public void actionPerformed(ActionEvent e) {
            List<FileInfo> fileInfosToRestore = new ArrayList<FileInfo>();
            for (DiskItem diskItem : getSelectedRows()) {
                if (diskItem != null && diskItem instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) diskItem;
                    fileInfosToRestore.add(fileInfo);
                }
            }

            PFWizard wizard = new PFWizard(getController());

            MultiFileRestorePanel panel = new MultiFileRestorePanel(
                    getController(), tableModel.getFolder(),
                    fileInfosToRestore);
            wizard.open(panel);
        }
    }

    private class RemoveIgnoreAction extends BaseAction {

        private RemoveIgnoreAction(Controller controller) {
            super("action_remove_ignore", controller);
        }

        public void actionPerformed(ActionEvent e) {
            for (DiskItem diskItem : getSelectedRows()) {
                if (diskItem != null && diskItem instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) diskItem;
                    tableModel.getFolder().getDiskItemFilter().removePattern(
                        fileInfo.getRelativeName());
                } else if (diskItem != null && diskItem instanceof Directory) {
                    Directory directory = (Directory) diskItem;
                    tableModel.getFolder().getDiskItemFilter().removePattern(
                        directory.getRelativeName() + "/*");
                }
            }
        }
    }

    private class UnmarkAction extends BaseAction {

        private UnmarkAction(Controller controller) {
            super("action_unmark", controller);
        }

        public void actionPerformed(ActionEvent e) {
            for (DiskItem diskItem : getSelectedRows()) {
                if (diskItem != null && diskItem instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) diskItem;
                    TransferManager transferManager = getController()
                        .getTransferManager();
                    if (transferManager.isCompletedDownload(fileInfo)) {
                        transferManager.clearCompletedDownload(fileInfo);
                        parent.scheduleDirectoryFiltering();
                    }
                }
            }
        }
    }

    private class MyTableModelListener implements TableModelListener {

        public void tableChanged(TableModelEvent e) {
            updateEmptyLabel();
        }
    }

    private class SingleFileTransferAction extends BaseAction {

        private SingleFileTransferAction(Controller controller) {
            super("action_single_file_transfer", controller);
        }

        public void actionPerformed(ActionEvent e) {
            for (DiskItem diskItem : getSelectedRows()) {
                if (diskItem != null && diskItem instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) diskItem;
                    getUIController()
                        .transferSingleFile(
                            fileInfo.getDiskFile(getController()
                                .getFolderRepository()), null);
                }
            }
        }
    }

    private class SelectAllAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            table.selectAll();
        }
    }
    
}
