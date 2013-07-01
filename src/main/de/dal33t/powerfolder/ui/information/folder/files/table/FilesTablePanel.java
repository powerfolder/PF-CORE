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

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria.Type;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.DiskItem;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.information.HasDetailsPanel;
import de.dal33t.powerfolder.ui.information.folder.files.DirectoryFilter;
import de.dal33t.powerfolder.ui.information.folder.files.DirectoryFilterListener;
import de.dal33t.powerfolder.ui.information.folder.files.FileDetailsPanel;
import de.dal33t.powerfolder.ui.information.folder.files.FilesTab;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryEvent;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryModel;
import de.dal33t.powerfolder.ui.information.folder.files.versions.FileVersionsPanel;
import de.dal33t.powerfolder.ui.util.SwingWorker;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;

public class FilesTablePanel extends PFUIComponent implements HasDetailsPanel,
    DirectoryFilterListener
{

    private JPanel uiComponent;
    private FileDetailsPanel fileDetailsPanel;
    private FileVersionsPanel fileVersionsPanel;
    private JPanel detailsPanel;
    private FilesTableModel tableModel;
    private FilesTable table;
    private OpenFileAction openFileAction;
    private BrowserAction browserAction;
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
    private JPanel emptyPanel;
    private JLabel emptyLabel;
    private ActionLabel emptyResetLink;
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
        emptyLabel = new JLabel(
            Translation.getTranslation("files_table_panel.no_files_available"));
        emptyLabel.setEnabled(false);

        emptyResetLink = new ActionLabel(getController(),
            new MyResetFiltersAction(getController()));

        UIUtil.whiteStripTable(table);
        UIUtil.setZeroHeight(tableScroller);
        UIUtil.removeBorder(tableScroller);

        buildEmptyPanel();

        // tableScroller and emptyPanel occupy the same slot
        builder.add(tableScroller, cc.xy(1, 1));
        builder.add(emptyPanel, cc.xy(1, 1));

        builder.add(detailsPanel, cc.xy(1, 3));

        buildPopupMenus();

        uiComponent = builder.getPanel();
        updateEmptyLabel();
    }

    private void buildEmptyPanel() {
        FormLayout outerLayout = new FormLayout(
            "pref:grow, center:pref, pref:grow",
            "pref:grow, center:pref, pref:grow");
        DefaultFormBuilder outerBuilder = new DefaultFormBuilder(outerLayout);

        CellConstraints cc = new CellConstraints();

        FormLayout innerLayout = new FormLayout(
            "pref:grow, center:pref, pref:grow", "pref, 3dlu, pref");
        DefaultFormBuilder innerBuilder = new DefaultFormBuilder(innerLayout);

        innerBuilder.add(emptyLabel, cc.xy(2, 1));
        innerBuilder.add(emptyResetLink.getUIComponent(), cc.xy(2, 3));

        JPanel innerPanel = innerBuilder.getPanel();

        outerBuilder.add(innerPanel, cc.xy(2, 2));

        emptyPanel = outerBuilder.getPanel();
    }

    /**
     * @return the toolbar
     */
    private void createToolBar() {
        openFileAction = new OpenFileAction(getController());
        openFileAction.setEnabled(false);
        browserAction = new BrowserAction(getController());
        browserAction.setEnabled(false);
        downloadFileAction = new DownloadFileAction(getController());
        downloadFileAction.setEnabled(false);
        abortDownloadAction = new AbortDownloadAction(getController());
        abortDownloadAction.setEnabled(false);
        deleteFileAction = new DeleteFileAction(getController());
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
        fileMenu.add(browserAction);
        fileMenu.add(downloadFileAction);
        fileMenu.add(abortDownloadAction);
        fileMenu.add(deleteFileAction);
        fileMenu.add(addIgnoreAction);
        fileMenu.add(removeIgnoreAction);
        fileMenu.add(unmarkAction);
        fileMenu.add(restoreArchiveAction);
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
        FilteredDirectoryModel filteredDirectoryModel = event.getModel();
        tableModel.setFilteredDirectoryModel(filteredDirectoryModel);
        switch (event.getFileFilterMode()) {
            case DirectoryFilter.FILE_FILTER_MODE_LOCAL_AND_INCOMING :
                emptyLabel
                    .setText(Translation
                        .getTranslation("files_table_panel.no_files_available.local_and_incoming"));
                break;
            case DirectoryFilter.FILE_FILTER_MODE_LOCAL_ONLY :
                emptyLabel
                    .setText(Translation
                        .getTranslation("files_table_panel.no_files_available.local_only"));
                break;
//            case DirectoryFilter.FILE_FILTER_MODE_INCOMING_ONLY :
//                emptyLabel
//                    .setText(Translation
//                        .getTranslation("files_table_panel.no_files_available.incoming_only"));
//                break;
            case DirectoryFilter.FILE_FILTER_MODE_NEW_ONLY :
                emptyLabel
                    .setText(Translation
                        .getTranslation("files_table_panel.no_files_available.new_only"));
                break;
            case DirectoryFilter.FILE_FILTER_MODE_DELETED_PREVIOUS :
                emptyLabel
                    .setText(Translation
                        .getTranslation("files_table_panel.no_files_available.deleted_previous"));
                break;
            case DirectoryFilter.FILE_FILTER_MODE_UNSYNCHRONIZED :
                emptyLabel
                    .setText(Translation
                        .getTranslation("files_table_panel.no_files_available.unsynchronized"));
                break;
            default :
                // Generic message.
                emptyLabel.setText(Translation
                    .getTranslation("files_table_panel.no_files_available"));
                break;
        }
        emptyResetLink.setVisible(!event.isDefaultFilter());
    }

    public void adviseOfFilteringBegin() {
        emptyLabel.setText(Translation
            .getTranslation("files_table_panel.finding_files"));
        emptyResetLink.setVisible(false);
    }

    public void invalidate() {
        tableModel
            .setFilteredDirectoryModel(new FilteredDirectoryModel("", ""));
        emptyLabel.setText(Translation
            .getTranslation("files_table_panel.finding_files"));
        emptyResetLink.setVisible(false);
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
            if (diskItem instanceof DirectoryInfo) {
                DirectoryInfo directoryInfo = (DirectoryInfo) diskItem;
                Path file = directoryInfo.getDiskFile(getController()
                    .getFolderRepository());
                if (Files.exists(file)) {
                    PathUtils.openFile(file);
                }
            } else if (diskItem instanceof FileInfo) {
                FileInfo fileInfo = (FileInfo) diskItem;
                Path file = fileInfo.getDiskFile(getController()
                    .getFolderRepository());
                if (file != null && Files.exists(file)) {
                    // Open file ...
                    table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    PathUtils.openFile(file);
                    table.setCursor(Cursor.getDefaultCursor());
                    // ... and clear completed download status.
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
                        if (diskItem instanceof DirectoryInfo) {
                            DirectoryInfo directoryInfo = (DirectoryInfo) diskItem;
                            Folder folder = directoryInfo
                                .getFolder(getController()
                                    .getFolderRepository());
                            FileInfoCriteria crit = new FileInfoCriteria();
                            crit.setPath(directoryInfo);
                            crit.addMember(getController().getMySelf());
                            crit.setRecursive(true);
                            Collection<FileInfo> infoCollection = folder
                                .getDAO().findFiles(crit);
                            folder.removeFilesLocal(infoCollection);
                            folder.removeFilesLocal(directoryInfo);
                        } else if (diskItem instanceof FileInfo) {
                            FileInfo fileInfo = (FileInfo) diskItem;
                            Folder folder = fileInfo.getFolder(repo);
                            folder.removeFilesLocal(fileInfo);
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
                return Translation.getTranslation("delete.busy.description");
            }
        };
        worker.start();
    }

    public DiskItem[] getSelectedRows() {
        int[] rows = table.getSelectedRows();
        return tableModel.getDiskItemsAtRows(rows);
    }

    public DiskItem[] getAllRows() {
        return tableModel.getAllDiskItems();
    }

    private void updateEmptyLabel() {
        if (tableScroller != null) {
            tableScroller.setVisible(tableModel.getRowCount() != 0);
        }
        if (emptyPanel != null) {
            emptyPanel.setVisible(tableModel.getRowCount() == 0);
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
            if (diskItem instanceof DirectoryInfo) {
                DirectoryInfo directoryInfo = (DirectoryInfo) diskItem;
                // Double click on a directory filters on it.
                parent.selectionChanged(directoryInfo.getRelativeName());
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

        tabbedPane.add(fileVersionsPanel.getPanel(), Translation
            .getTranslation("files_table_panel.file_versions_tab.text"));
        tabbedPane.setToolTipTextAt(1, Translation
            .getTranslation("files_table_panel.file_versions_tab.tip"));
        
        tabbedPane.setSelectedIndex(1);

        return builder.getPanel();
    }

    /**
     * Try to download everything within this directory.
     * 
     * @param directoryInfo
     */
    private void downloadDirectory(DirectoryInfo directoryInfo) {
        FolderRepository repo = getController().getFolderRepository();
        Folder folder = directoryInfo.getFolder(repo);
        FileInfoCriteria criteria = new FileInfoCriteria();
        criteria.setPath(directoryInfo);
        criteria.setRecursive(true);
        criteria.setType(Type.FILES_ONLY);
        criteria.addWriteMembersAndMyself(folder);
        Collection<FileInfo> infoCollection = folder.getDAO().findFiles(
            criteria);
        for (FileInfo fileInfo : infoCollection) {
            if (!fileInfo.isDownloading(getController())) {
                getController().getTransferManager().downloadNewestVersion(
                    fileInfo);
            }
        }
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class DownloadFileAction extends BaseAction {
        DownloadFileAction(Controller controller) {
            super("action_download_file", controller);
        }

        public void actionPerformed(ActionEvent e) {
            SwingWorker worker = new ActivityVisualizationWorker(
                getUIController())
            {

                @Override
                public Object construct() {
                    for (DiskItem diskItem : getSelectedRows()) {
                        if (diskItem instanceof DirectoryInfo) {
                            DirectoryInfo directoryInfo = (DirectoryInfo) diskItem;
                            downloadDirectory(directoryInfo);
                        } else if (diskItem instanceof FileInfo) {
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
        DeleteFileAction(Controller controller) {
            super("action_delete_file", controller);
        }

        public void actionPerformed(ActionEvent e) {
            deleteSelectedFiles();
        }
    }

    private class OpenFileAction extends BaseAction {
        OpenFileAction(Controller controller) {
            super("action_open_file", controller);
        }

        public void actionPerformed(ActionEvent e) {
            openSelectedFile();
        }
    }

    private class MyListSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            boolean done = false;
            boolean downloadState = false;
            if (getSelectedRows().length > 0) {
                DiskItem diskItem = getSelectedRows()[0];
                if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
                    openFileAction.setEnabled(diskItem != null);
                }
                browserAction.setEnabled(diskItem != null
                    && diskItem instanceof FileInfo
                    && getSelectedRows().length == 1
                    && ConfigurationEntry.WEB_LOGIN_ALLOWED
                        .getValueBoolean(getController()));
                if (diskItem != null && diskItem instanceof DirectoryInfo) {
                    DirectoryInfo directoryInfo = (DirectoryInfo) diskItem;
                    boolean retained = tableModel.getFolder()
                        .getDiskItemFilter().isRetained(directoryInfo);
                    addIgnoreAction.setEnabled(retained);
                    removeIgnoreAction.setEnabled(!retained);
                    deleteFileAction.setEnabled(true);

                    fileDetailsPanel.setFileInfo(null);
                    fileVersionsPanel.setFileInfo(null);
                    downloadState = true;
                    restoreArchiveAction.setEnabled(true);
                    done = true;
                } else if (diskItem != null && diskItem instanceof FileInfo) {
                    TransferManager tm = getController().getTransferManager();
                    FileInfo fileInfo = (FileInfo) diskItem;
                    fileDetailsPanel.setFileInfo(fileInfo);
                    fileVersionsPanel.setFileInfo(fileInfo);

                    // Enable download action if file is download-able.
                    downloadState = true;
                    FolderRepository repo = getController()
                        .getFolderRepository();
                    if (fileInfo.diskFileExists(getController())
                        && !fileInfo.isNewerAvailable(repo))
                    {
                        downloadState = false;
                    } else if (!fileInfo.isDeleted()
                        && !fileInfo.isExpected(repo)
                        && !fileInfo.isNewerAvailable(repo))
                    {
                        downloadState = false;
                    }

                    // Enable restore / (!delete) action if file is
                    // restore-able.
                    deleteFileAction.setEnabled(!downloadState);

                    DownloadManager dl = getController().getTransferManager()
                        .getActiveDownload(fileInfo);
                    abortDownloadAction.setEnabled(dl != null);

                    boolean retained = tableModel.getFolder()
                        .getDiskItemFilter().isRetained(fileInfo);
                    addIgnoreAction.setEnabled(retained);
                    removeIgnoreAction.setEnabled(!retained);

                    unmarkAction.setEnabled(tm.isCompletedDownload(fileInfo));

                    singleFileTransferAction.setEnabled(true);

                    restoreArchiveAction.setEnabled(true);

                    done = true;
                }
            } else {
                browserAction.setEnabled(false);
            }

            downloadFileAction.setEnabled(downloadState);

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
                if (diskItem != null && diskItem instanceof DirectoryInfo) {
                    DirectoryInfo directoryInfo = (DirectoryInfo) diskItem;
                    tableModel.getFolder().addPattern(
                        directoryInfo.getRelativeName() + "/*");
                } else if (diskItem != null && diskItem instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) diskItem;
                    tableModel.getFolder().addPattern(
                        fileInfo.getRelativeName());
                }
            }
            getController().getTransferManager()
                .checkActiveTranfersForExcludes();
        }
    }

    private class RestoreArchiveAction extends BaseAction {

        private RestoreArchiveAction(Controller controller) {
            super("action_restore_archive", controller);
        }

        public void actionPerformed(ActionEvent e) {
            parent.fileArchive();
        }
    }

    private class RemoveIgnoreAction extends BaseAction {

        private RemoveIgnoreAction(Controller controller) {
            super("action_remove_ignore", controller);
        }

        public void actionPerformed(ActionEvent e) {
            for (DiskItem diskItem : getSelectedRows()) {
                if (diskItem != null && diskItem instanceof DirectoryInfo) {
                    DirectoryInfo directoryInfo = (DirectoryInfo) diskItem;
                    tableModel.getFolder().removePattern(
                        directoryInfo.getRelativeName() + "/*");
                } else if (diskItem != null && diskItem instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) diskItem;
                    tableModel.getFolder().removePattern(
                        fileInfo.getRelativeName());
                }
            }
            // Trigger resync
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting(tableModel.getFolder().getInfo());
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
                    getUIController().transferSingleFile(
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

    private class MyResetFiltersAction extends BaseAction {

        MyResetFiltersAction(Controller controller) {
            super("action_reset_filters", controller);
        }

        public void actionPerformed(ActionEvent e) {
            parent.resetFilters();
        }
    }

    private class BrowserAction extends BaseAction {
        BrowserAction(Controller controller) {
            super("action_browser", controller);
        }

        public void actionPerformed(ActionEvent e) {
            if (getSelectedRows().length == 1) {
                DiskItem diskItem = getSelectedRows()[0];
                if (diskItem instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) diskItem;
                    String fileLinkURL = getController().getOSClient().getFileLinkURL(fileInfo);
                    try {
                        BrowserLauncher.openURL(fileLinkURL);
                    } catch (IOException ex) {
                        logWarning("Unable to open in browser: " + fileLinkURL);
                    }
                }
            }
        }
    }

}
