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
 * $Id$
 */
package de.dal33t.powerfolder.ui.folder;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.apache.commons.lang.time.DateUtils;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FileFilterChangeListener;
import de.dal33t.powerfolder.event.FileFilterChangedEvent;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.PreviewPanel;
import de.dal33t.powerfolder.ui.action.AbortTransferAction;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.action.ChangeFriendStatusAction;
import de.dal33t.powerfolder.ui.action.DownloadFileAction;
import de.dal33t.powerfolder.ui.action.HasDetailsPanel;
import de.dal33t.powerfolder.ui.action.RemoveFileAction;
import de.dal33t.powerfolder.ui.action.RestoreFileAction;
import de.dal33t.powerfolder.ui.action.SelectionBaseAction;
import de.dal33t.powerfolder.ui.action.ShowHideFileDetailsAction;
import de.dal33t.powerfolder.ui.action.StartFileAction;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.ui.dialog.FileDetailsPanel;
import de.dal33t.powerfolder.util.DragDropChecker;
import de.dal33t.powerfolder.util.FileCopier;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Loggable;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Shows a Directory, holds a FileFilterPanel to filter the filelist and enable
 * the user to selected for a recursive view.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.8 $ *
 */
public class FilesTab extends PFUIComponent implements FolderTab,
    HasDetailsPanel, FileFilterChangeListener
{

    /** enable/disable drag and drop */
    public static final boolean ENABLE_DRAG_N_DROP = false;

    private final FolderPanel folderPanel;

    /**
     * FileCopier, used if files are added eg from a drag and drop
     */
    private FileCopier fileCopier;

    private JComponent panel;
    private JPopupMenu fileMenu;
    private DirectoryTable directoryTable;
    private FileFilterPanel fileFilterPanel;
    private FileFilterModel fileFilterModel;
    private JScrollPane directoryTableScrollPane;
    private JCheckBox recursiveSelection;
    private JPanel filterBar;
    private JPanel toolbar;
    private JToggleButton showHideFileDetailsButton;
    private JPanel fileStatusPanel;

    private JComponent fileDetailsPanelComp;

    /** The currently selected items */
    private SelectionModel selectionModel;
    private DownloadFileAction downloadFileAction;
    private BlackWhitelistAction blackWhitelistAction;
    private UnBlackWhitelistAction unBlackWhitelistAction;
    private StartFileAction startFileAction;
    private RemoveFileAction removeFileAction;
    private RestoreFileAction restoreFileAction;
    private AbortTransferAction abortTransferAction;
    private UnmarkAction unmarkAction;
    private OpenLocalFolder openLocalFolder;

    private MyFolderListener myFolderListener;
    private MyFolderMembershipListener myFolderMembershipListener;
    /**
     * if the number of rows in the table is more than MAX_ITEMS the updates
     * will be delayed to a maximum one update every DELAY
     */
    private static final int MAX_ITEMS = 200;
    /** are we now updating? */
    private boolean isUpdating;
    /** one minute */
    private static final long DELAY = DateUtils.MILLIS_PER_MINUTE;
    /** time in milli sec of last update finish */
    private long lastUpdate;

    private JLabel localFilesLabel;
    private JLabel incomingFilesLabel;
    private JLabel deletedFilesLabel;
    private JLabel recycledFilesLabel;

    private MyTimerTask task;

    public FilesTab(Controller controller, FolderPanel folderPanel) {
        super(controller);
        this.folderPanel = folderPanel;
        fileFilterModel = new FileFilterModel(getController());
        selectionModel = new SelectionModel();
        myFolderListener = new MyFolderListener();
        myFolderMembershipListener = new MyFolderMembershipListener();
        MyNodeManagerListener myNodeManegerListener = new MyNodeManagerListener();
        controller.getNodeManager().addNodeManagerListener(
            myNodeManegerListener);
        controller.getTransferManager().addListener(
            new MyTransferManagerListener());
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
            ContentPanelBuilder builder = new ContentPanelBuilder();
            builder.setFilterbar(filterBar);
            builder.setToolbar(toolbar);
            builder.setContent(createContentPanel());
            panel = builder.getPanel();
        }
        return panel;
    }

    private JComponent createContentPanel() {
        FormLayout layout = new FormLayout("fill:pref:grow",
            "0, fill:0:grow, pref, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        // builder.add(filterBar, cc.xywh(1, 1, 1, 1, "right, center"));
        builder.add(directoryTableScrollPane, cc.xy(1, 2));
        builder.add(fileDetailsPanelComp, cc.xy(1, 3));
        builder.add(fileStatusPanel, cc.xy(1, 4));
        return builder.getPanel();
    }

    /**
     * Initalize all nessesary components
     */
    private void initComponents() {
        downloadFileAction = new DownloadFileAction(getController(),
            selectionModel);
        blackWhitelistAction = new BlackWhitelistAction(getController(),
            selectionModel);
        unBlackWhitelistAction = new UnBlackWhitelistAction(getController(),
            selectionModel);
        unmarkAction = new UnmarkAction(getController(),
            selectionModel);
        startFileAction = new StartFileAction(getController(), selectionModel);
        removeFileAction = new RemoveFileAction(getController(), selectionModel);
        restoreFileAction = new RestoreFileAction(getController(),
            selectionModel);
        abortTransferAction = new AbortTransferAction(getController(),
            selectionModel);
        openLocalFolder = new OpenLocalFolder(getController());

        fileFilterPanel = new FileFilterPanel(fileFilterModel);
        directoryTable = new DirectoryTable(getController(), fileFilterModel);
        directoryTableScrollPane = new JScrollPane(directoryTable);
        directoryTable.addMouseListener(new TableMouseListener());
        directoryTable.addKeyListener(new DeleteKeyListener());
        directoryTable.addMouseWheelListener(new TableMouseListener());
        UIUtil.whiteStripTable(directoryTable);
        UIUtil.removeBorder(directoryTableScrollPane);
        UIUtil.setZeroHeight(directoryTableScrollPane);

        recursiveSelection = new JCheckBox(Translation
            .getTranslation("filelist.recursive"));
        recursiveSelection.setToolTipText(Translation
            .getTranslation("files_tab.recursive_selection.tooltip"));
        recursiveSelection.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean recursive = recursiveSelection.isSelected();
                directoryTable.getDirectoryTableModel().setRecursive(recursive,
                    true);
            }
        });

        // Add self as FileFilderChangeListener to FileFilterModel,
        // respond to changes in # files.
        fileFilterModel.addFileFilterChangeListener(this);

        // Add selection listener for updating selection model
        directoryTable.getSelectionModel().addListSelectionListener(
            new DirectoryListSelectionListener());

        directoryTable.getTableHeader().addMouseListener(
            new TableHeaderMouseListener());

        filterBar = createFilterBar();
        fileDetailsPanelComp = createFileDetailsPanel();
        // Details not visible @ start
        fileDetailsPanelComp.setVisible(false);

        localFilesLabel = new JLabel("", SwingConstants.CENTER);
        incomingFilesLabel = new JLabel("", SwingConstants.CENTER);
        deletedFilesLabel = new JLabel("", SwingConstants.CENTER);
        recycledFilesLabel = new JLabel("", SwingConstants.CENTER);
        updateFileNumbers(0, 0, 0, 0);

        fileStatusPanel = createFileStatusPanel();

        Action showHideFileDetailsAction = new ShowHideFileDetailsAction(this,
            getController());
        showHideFileDetailsButton = new JToggleButton(showHideFileDetailsAction);

        toolbar = createToolBar();

        // build the popup menus
        buildPopupMenus();

        if (ENABLE_DRAG_N_DROP) {
            // drag support
            DragSource dragSource = DragSource.getDefaultDragSource();
            dragSource.createDefaultDragGestureRecognizer(directoryTable,
                DnDConstants.ACTION_COPY, new MyDragGestureListener());
            // drop on table and scroll pane
            new DropTarget(directoryTable, DnDConstants.ACTION_COPY,
                new MyDropTargetListener(), true);
            new DropTarget(directoryTableScrollPane, DnDConstants.ACTION_COPY,
                new MyDropTargetListener(), true);
        }

    }

    private JPanel createFileStatusPanel() {
        FormLayout layout = new FormLayout(
            "fill:pref:grow, pref, 3dlu, pref, "
                + "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu",
            "pref, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addSeparator(null, cc.xyw(1, 1, 15));

        JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
        sep1.setPreferredSize(new Dimension(2, 12));

        JSeparator sep2 = new JSeparator(SwingConstants.VERTICAL);
        sep2.setPreferredSize(new Dimension(2, 12));

        JSeparator sep3 = new JSeparator(SwingConstants.VERTICAL);
        sep3.setPreferredSize(new Dimension(2, 12));

        builder.add(localFilesLabel, cc.xy(2, 2));
        builder.add(sep1, cc.xy(4, 2));
        builder.add(incomingFilesLabel, cc.xy(6, 2));
        builder.add(sep2, cc.xy(8, 2));
        builder.add(deletedFilesLabel, cc.xy(10, 2));
        builder.add(sep3, cc.xy(12, 2));
        builder.add(recycledFilesLabel, cc.xy(14, 2));

        return builder.getPanel();
    }

    private JPanel createFilterBar() {
        FormLayout layout = new FormLayout("pref, 3dlu, fill:pref:grow", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("1dlu, 1dlu, 1dlu, 0dlu"));
        CellConstraints cc = new CellConstraints();

        builder.add(recursiveSelection, cc.xy(1, 1));
        builder.add(fileFilterPanel.getUIComponent(), cc.xy(3, 1));

        return builder.getPanel();
    }

    private JPanel createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        // bar.addGridded(downloadOrStartButton);
        bar.addGridded(new JButton(downloadFileAction));
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            bar.addRelatedGap();
            bar.addGridded(new JButton(startFileAction));
        }

        bar.addRelatedGap();

        bar.addGridded(showHideFileDetailsButton);

        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            bar.addRelatedGap();
            bar.addGridded(new JButton(openLocalFolder));
        }
        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }

    private FileCopier getFileCopier() {
        if (fileCopier == null) {
            fileCopier = new FileCopier(getController());
        }
        return fileCopier;
    }

    public DirectoryTable getDirectoryTable() {
        return directoryTable;
    }

    /**
     * @return the file details panel and sets it to the internal variable
     */
    private JComponent createFileDetailsPanel() {
        FileDetailsPanel fileDetailsPanel = new FileDetailsPanel(
            getController(), selectionModel);
        // check property to enable preview
        // preview of images is memory hungry
        // may cause OutOfMemoryErrors
        // TODO Cleanup this UI building mess.
        if (PreferencesEntry.SHOW_PREVIEW_PANEL
            .getValueBoolean(getController()))
        {
            PreviewPanel previewPanel = new PreviewPanel(getController(),
                selectionModel, this);
            FormLayout layout = new FormLayout("pref, fill:pref:grow",
                "pref, 3dlu, pref, fill:pref, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.addSeparator(null, cc.xy(1, 1));
            builder.addSeparator(null, cc.xy(1, 3));
            builder.add(fileDetailsPanel.getEmbeddedPanel(), cc.xy(1, 4));
            builder.add(previewPanel.getUIComponent(), cc.xy(2, 4));
            return builder.getPanel();
        }
        FormLayout layout = new FormLayout("fill:pref:grow",
            "pref, 3dlu, pref, fill:pref, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addSeparator(null, cc.xy(1, 1));
        builder.addSeparator(null, cc.xy(1, 3));
        builder.add(fileDetailsPanel.getEmbeddedPanel(), cc.xy(1, 4));
        return builder.getPanel();
    }

    public void setFolder(Folder folder) {
        setDirectory(folder.getDirectory());
        updateBlackWhiteActions(folder.isWhitelist());
    }

    public void updateBlackWhiteActions(boolean whitelist) {
        blackWhitelistAction.setWhitelist(whitelist);
        unBlackWhitelistAction.setWhitelist(whitelist);
    }

    public String getTitle() {
        return Translation.getTranslation("general.files");
    }

    public void setDirectory(Directory directory) {
        Directory oldDirectory = directoryTable.getDirectory();
        Folder newFolder = directory.getRootFolder();
        if (oldDirectory != null) {
            Folder oldFolder = directoryTable.getDirectory().getRootFolder();
            if (newFolder != oldFolder) {
                fileFilterPanel.reset();
            }
        }

        if (oldDirectory != null) {
            Folder oldFolder = directoryTable.getDirectory().getRootFolder();
            if (oldFolder == newFolder) { // same folder
                if (oldDirectory != directory) {
                    directoryTable.setDirectory(directory, true, null);
                    directoryTable.setColumnSizes();
                } else {
                    // well we got the same dir here so dont do anything....?
                }
            } else { // other folder
                oldFolder.removeFolderListener(myFolderListener);
                oldFolder.removeMembershipListener(myFolderMembershipListener);
                Folder folder = directory.getRootFolder();
                folder.addFolderListener(myFolderListener);
                folder.addMembershipListener(myFolderMembershipListener);
                directoryTable.setDirectory(directory, true, null);
                directoryTable.setColumnSizes();
            }
        } else { // no dir before this new one...
            Folder folder = directory.getRootFolder();
            folder.addFolderListener(myFolderListener);
            folder.addMembershipListener(myFolderMembershipListener);
            directoryTable.setDirectory(directory, true, null);
            directoryTable.setColumnSizes();
        }
    }

    /**
     * Builds the popup menus
     */
    private void buildPopupMenus() {
        fileMenu = new JPopupMenu();
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            fileMenu.add(startFileAction);
            fileMenu.add(openLocalFolder);
        }
        fileMenu.add(downloadFileAction);
        fileMenu.add(abortTransferAction);
        fileMenu.add(removeFileAction);
        fileMenu.add(restoreFileAction);
        fileMenu.add(blackWhitelistAction);
        fileMenu.add(unBlackWhitelistAction);
        fileMenu.add(unmarkAction);
        fileMenu.addSeparator();

        fileMenu.add(new ChangeFriendStatusAction(getController(),
            selectionModel));
    }

    /**
     * @return the selection model. Changes upon selection.
     */
    public SelectionModel getSelectionModel() {
        return selectionModel;
    }

    /**
     * Respond to changes in numbers of files.
     *
     * @param event
     */
    public void filterChanged(FileFilterChangedEvent event) {
        updateFileNumbers(event.getLocalFiles(), event.getIncomingFiles(),
            event.getDeletedFiles(), event.getRecycledFiles());
    }

    private void updateFileNumbers(int localFiles, int incomingFiles,
        int deletedFiles, int recycledFiles)
    {
        localFilesLabel.setText(Translation.getTranslation("files_tab.local",
            localFiles));
        incomingFilesLabel.setText(Translation.getTranslation(
            "files_tab.incoming", incomingFiles));
        deletedFilesLabel.setText(Translation.getTranslation(
            "files_tab.deleted", deletedFiles));
        recycledFilesLabel.setText(Translation.getTranslation(
            "files_tab.recycled", recycledFiles));
    }

    /** updates the SelectionModel if some selection has changed in the table */
    private class DirectoryListSelectionListener implements
        ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e) {
            int[] selectedRows = directoryTable.getSelectedRows();
            if (selectedRows.length != 0 && !e.getValueIsAdjusting()) {
                Object[] selectedObjects = new Object[selectedRows.length];

                for (int i = 0; i < selectedRows.length; i++) {
                    selectedObjects[i] = directoryTable
                        .getDirectoryTableModel()
                        .getValueAt(selectedRows[i], 0);

                }
                selectionModel.setSelections(selectedObjects);
            } else {
                selectionModel.setSelection(null);
            }
        }
    }

    /**
     * updates the selection based on scrollwheel turns, scrolls the newly
     * selected item to center if outside the current view.
     *
     * @param clicksTurned
     *            the number of clicks on the scrollwheel, maybe negative!
     */
    private void scrollWheel(int clicksTurned) {
        Object currentSelection = selectionModel.getSelection();
        DirectoryTableModel directoryTableModel = (DirectoryTableModel) directoryTable
            .getModel();
        int indexOfSelection = directoryTableModel.getIndexOf(currentSelection);
        if (indexOfSelection != -1) {
            int newSelectionIndex = indexOfSelection + clicksTurned;
            // check bounds
            if (newSelectionIndex >= 0
                && (directoryTableModel.getRowCount() - 1) >= newSelectionIndex)
            {
                ListSelectionModel model = directoryTable.getSelectionModel();
                model.clearSelection();
                model
                    .addSelectionInterval(newSelectionIndex, newSelectionIndex);
                // assume the table is in a Scroll pane
                JViewport viewport = (JViewport) directoryTable.getParent();
                Rectangle rect = directoryTable.getCellRect(newSelectionIndex,
                    0, true);
                // The location of the view relative to the table
                Rectangle viewRect = viewport.getViewRect();
                // if selection out of view then adjust view
                int y = (rect.y + rect.height) - viewRect.y;
                if (y > viewRect.height || rect.y < viewRect.y) {
                    directoryTable.scrollToCenter(newSelectionIndex, 0);
                }
            }
        }
    }

    /**
     * Mouse (wheel) Listener on table
     */
    private class TableMouseListener extends MouseAdapter implements
        MouseWheelListener
    {
        /** if mouse wheel is turned the next item in the table is selected */
        public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
            // may be negative...!
            int clicksTurned = mouseWheelEvent.getWheelRotation();
            if (mouseWheelEvent.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
            {
                scrollWheel(clicksTurned);
            }
        }

        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {

                Object item = selectionModel.getSelection();
                // double click is now open subfolder
                if (item instanceof Directory) {
                    getUIController().getInformationQuarter().displayDirectory(
                        (Directory) item);
                } else { // get the doubleclicked
                    if (item instanceof FileInfo) {
                        FileInfo fInfo = (FileInfo) item;
                        FolderRepository repo = getController()
                            .getFolderRepository();
                        if (fInfo.isDeleted() || fInfo.isExpected(repo)
                            || fInfo.isNewerAvailable(repo))
                        { // download file
                            downloadFileAction.actionPerformed(null);
                        } else if (!fInfo.isDeleted()) {
                            startFileAction.actionPerformed(null);
                        }
                    }
                }
            }
        }

        public void mousePressed(MouseEvent evt) {
            if (evt.isPopupTrigger()) {
                showContextMenu(evt);
            }
        }

        public void mouseReleased(MouseEvent evt) {
            if (evt.isPopupTrigger()) {
                showContextMenu(evt);
            }
        }

        private void showContextMenu(MouseEvent evt) {
            fileMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    /**
     * Listener on table header, takes care about the sorting of table
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class TableHeaderMouseListener extends MouseAdapter {
        public final void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                JTableHeader tableHeader = (JTableHeader) e.getSource();
                int columnNo = tableHeader.columnAtPoint(e.getPoint());
                TableColumn column = tableHeader.getColumnModel().getColumn(
                    columnNo);
                int modelColumnNo = column.getModelIndex();
                TableModel model = tableHeader.getTable().getModel();
                if (model instanceof DirectoryTableModel) {
                    DirectoryTableModel fModel = (DirectoryTableModel) model;
                    boolean freshSorted = fModel.sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        fModel.reverseList();
                    }
                }
            }
        }
    }

    /**
     * get the list of File objects that are currently selected. Return null if
     * nothing is selected. Returns the subdirs as a File object to.
     */
    private List<File> getSelectedFiles() {
        Object[] selectedValues = selectionModel.getSelections();
        if (selectedValues == null || selectedValues.length == 0) {
            return null;
        }
        // check for dirs:
        List<File> returnValues = new ArrayList<File>();
        for (Object selectedValue : selectedValues) {
            if (selectedValue instanceof FileInfo) {
                FileInfo fileInfo = (FileInfo) selectedValue;
                File file = fileInfo.getDiskFile(getController()
                    .getFolderRepository());
                if (file.exists()) {// only use files that exists
                    returnValues.add(file);
                }
            } else if (selectedValue instanceof Directory) {
                Directory directory = (Directory) selectedValue;
                File file = directory.getFile();
                returnValues.add(file);
            }
        }
        return returnValues;
    }

    /** class that handles the start of a Drag and Drop FROM this FileList */
    private class MyDragGestureListener implements DragGestureListener {
        public void dragGestureRecognized(DragGestureEvent event) {
            Object[] selectedValues = getSelectionModel().getSelections();
            if (event.getDragAction() == DnDConstants.ACTION_COPY
                && selectedValues != null)
            {
                List<File> draggedValues = getSelectedFiles();
                if (draggedValues != null) {
                    Transferable transferable = new FileListTransferable(
                        directoryTable.getDirectory(), draggedValues.toArray());
                    event.startDrag(null, transferable);
                }
            }
        }
    }

    /**
     * helper class to enable drops of more file and one overwrite / skipp/
     * dialog and allows drop of directories
     */
    private class Dropper extends Loggable {
        // count items to see if there are more for the dialog
        // options
        private int count = 0;
        private boolean overwriteAll = false;
        private boolean skipAll = false;
        private boolean cancel = false;
        private int total;
        private boolean move;

        public Dropper(int total, boolean move) {
            this.total = total;
            this.move = move;
        }

        private boolean drop(File file, Directory directory) {
            // test if a dir is dropped
            if (file.isDirectory()) {
                Directory subDirectory = directory.getCreateSubDirectory(file
                    .getName());
                File[] files = file.listFiles();
                total += files.length;

                for (File subFile : files) {
                    // drop all files in this dir using a new dropper
                    if (subFile.exists() && subFile.canRead()) {
                        if (!drop(subFile, subDirectory)) {
                            return false;
                        }
                        // if dialog was shown we take over the choice
                        if (cancel) {
                            break;
                        }
                        if (skipAll) {
                            break;
                        }
                    }
                }
                return true;
            }
            // normal file:
            count++;
            // check overwriting
            if (directory.alreadyHasFileOnDisk(file)) {
                if (skipAll) { // skip all duplicates
                    return true;
                }
                if (!overwriteAll) {
                    if (count < total) {// dialog for more items
                        Object[] options = {
                            Translation.getTranslation("general.overwrite"),
                            Translation.getTranslation("general.overwrite_all"),
                            Translation.getTranslation("general.skip"),
                            Translation
                                .getTranslation("folderpanel.filestab.dropfile_duplicate_dialog.skipall"),
                            Translation.getTranslation("general.cancel")};
                        int option = JOptionPane
                            .showOptionDialog(
                                getUIController().getMainFrame()
                                    .getUIComponent(),
                                Translation
                                    .getTranslation(
                                        "folderpanel.filestab.dropfile_duplicate_dialog.text",
                                        file.getName()),
                                Translation
                                    .getTranslation("folderpanel.filestab.dropfile_duplicate_dialog.title"),

                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE, null, options,
                                options[2]);
                        switch (option) {
                            case 0 : { // overwrite
                                break;
                            }
                            case 1 : { // overwrite all
                                overwriteAll = true;
                                break;
                            }
                            case 2 : { // skip
                                return true;
                            }
                            case 3 : { // skip all dupes
                                skipAll = true;
                                return true;
                            }
                            case 4 : { // cancel
                                cancel = true;
                                break;
                            }
                        }
                    } else {// dialog for just one item
                        Object[] options = {
                            Translation.getTranslation("general.overwrite"),
                            Translation.getTranslation("general.skip"),
                            Translation.getTranslation("general.cancel")};
                        int option = JOptionPane
                            .showOptionDialog(
                                getUIController().getMainFrame()
                                    .getUIComponent(),
                                Translation
                                    .getTranslation(
                                        "folderpanel.filestab.dropfile_duplicate_dialog.text",
                                        file.getName()),
                                Translation
                                    .getTranslation("folderpanel.filestab.dropfile_duplicate_dialog.title"),
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE, null, options,
                                options[2]);
                        switch (option) {
                            case 0 : { // overwrite
                                break;
                            }
                            case 1 : { // skip
                                return true;
                            }
                            case 2 : { // cancel
                                cancel = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (cancel) {
                return true;
            }
            FilesTab.this.directoryTable.getParent().setCursor(
                Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            if (move) {
                log().debug("Moving!: " + file + " to: " + directory);
                if (!directory.moveFileFrom(file)) {
                    log().error("something failed in drop/move");
                    FilesTab.this.directoryTable.getParent().setCursor(
                        Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    // something failed
                    return false;
                }
            } else {
                log().debug("copy: " + file + " to: " + directory);
                if (!directory.copyFileFrom(file, getFileCopier())) {
                    FilesTab.this.directoryTable.getParent().setCursor(
                        Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    // something failed
                    log().error("something failed in drop/copy");
                    return false;
                }
            }
            FilesTab.this.directoryTable.getParent().setCursor(
                Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return true;
        }
    }

    /**
     * drop a transferable on this filelist, must be of the javafilelist flavor
     *
     * @param trans
     * @return true if succeeded
     */
    public boolean drop(Transferable trans) {
        Directory target = directoryTable.getDirectory();
        return drop(target, trans);
    }

    /**
     * drop a transferable on this directory, must be of the javafilelist flavor
     *
     * @param directory
     * @param trans
     * @return true if succeeded
     */
    public boolean drop(Directory directory, Transferable trans) {
        try {
            List<File> fileList = (List<File>) trans
                .getTransferData(DataFlavor.javaFileListFlavor);
            Dropper dropper = null;

            if (!DragDropChecker.allowDrop(fileList, directory.getFile())) {
                return false;
            }
            if (trans.isDataFlavorSupported(Directory.getDataFlavor())) {
                Directory sourceDir = (Directory) trans
                    .getTransferData(Directory.getDataFlavor());
                if (sourceDir.getRootFolder() == directory.getRootFolder()) {
                    // file move inside of folder
                    dropper = new Dropper(fileList.size(), true); // move!
                }
            }
            // normal drop
            if (dropper == null) {
                dropper = new Dropper(fileList.size(), false); // do not move
            }
            for (File file : fileList) {
                if (!dropper.drop(file, directory)) {
                    return false;
                }
                if (dropper.cancel) {
                    break;
                }
            }
            return true;
        } catch (UnsupportedFlavorException ufe) {
            log().error(ufe);
        } catch (IOException ioe) {
            log().error(ioe);
        }
        return false;
    }

    /** class that handles the Drop TO this FileList. */
    private class MyDropTargetListener implements DropTargetListener {
        public void dragEnter(DropTargetDragEvent dtde) {
            if (DragDropChecker.allowDropCopy(getController(), dtde)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY);
                return;
            }
            dtde.rejectDrag();
        }

        public void dragExit(DropTargetEvent dtde) {
        }

        public void dragOver(DropTargetDragEvent dtde) {
            if (DragDropChecker.allowDropCopy(getController(), dtde)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY);
            } else {
                dtde.rejectDrag();
            }
            // select a folder if hover over with drag
            Point location = dtde.getLocation();
            int row = directoryTable.rowAtPoint(location);
            if (row < 0) {
                return;
            }
            Object object = directoryTable.getModel().getValueAt(row, 0);
            if (object instanceof Directory) {
                directoryTable.getSelectionModel().setSelectionInterval(row,
                    row);
            } else {
                // deselects a directory that was selected by the lines
                // above if hover above something else
                directoryTable.getSelectionModel().clearSelection();
            }
        }

        public void drop(DropTargetDropEvent dtde) {
            Point location = dtde.getLocation();
            int row = directoryTable.rowAtPoint(location);
            if (row < 0) {
                return;
            }
            Object object = directoryTable.getModel().getValueAt(row, 0);
            // drop into directory
            if (object instanceof Directory) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                boolean succes = FilesTab.this.drop((Directory) object, dtde
                    .getTransferable());
                dtde.dropComplete(succes);
                return;
            }

            dtde.acceptDrop(DnDConstants.ACTION_COPY);
            boolean succes = FilesTab.this.drop(dtde.getTransferable());
            dtde.dropComplete(succes);
        }

        public void dropActionChanged(DropTargetDragEvent dtde) {
            if (DragDropChecker.allowDropCopy(getController(), dtde)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY);
            } else {
                dtde.rejectDrag();
            }
        }
    }

    /** Helper class, Opens the local folder on action * */
    private class OpenLocalFolder extends BaseAction {
        public OpenLocalFolder(Controller controller) {
            super("open_local_folder", controller);
        }

        /**
         * opens the folder currently in view in the operatings systems file
         * explorer
         */
        public void actionPerformed(ActionEvent e) {
            Directory directory = directoryTable.getDirectoryTableModel()
                .getDirectory();
            if (directory != null) {
                Folder folder = directory.getRootFolder();
                File localBase = folder.getLocalBase();
                File path = new File(localBase.getAbsolutePath() + '/'
                    + directory.getPath());
                while (!path.exists()) { // try finding the first path that
                    // exists
                    String pathStr = path.getAbsolutePath();
                    int index = pathStr.lastIndexOf(File.separatorChar);
                    if (index == -1) {
                        return;
                    }
                    path = new File(pathStr.substring(0, index));
                }
                try {
                    FileUtils.openFile(path);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    /** helper class to delete files on delete key */
    private class DeleteKeyListener implements KeyListener {
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                // invoke delete action
                removeFileAction.actionPerformed(null);
            }
        }

        public void keyReleased(KeyEvent e) {
        }

        public void keyTyped(KeyEvent e) {
        }
    }

    private class MyFolderListener extends FolderAdapter {

        public void scanResultCommited(FolderEvent folderEvent) {
            updateFolder(folderEvent.getFolder());
        }

        public void fileChanged(FolderEvent folderEvent) {
            updateFolder(folderEvent.getFolder());
        }

        public void filesDeleted(FolderEvent folderEvent) {
            updateFolder(folderEvent.getFolder());
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
            updateFolder(folderEvent.getFolder());
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

        private void updateFolder(Folder folder) {
            Directory dir = directoryTable.getDirectory();
            if (dir != null && folder == dir.getRootFolder()) {
                update();
            } else {
                log().debug("not listening to folder " + folder);
            }
        }
    }

    private void update() {
        if (isUpdating) {
            return;
        }
        long millisPast = System.currentTimeMillis() - lastUpdate;
        if (task != null) {
            return;
        }
        if (millisPast > DELAY
            || directoryTable.getDirectoryTableModel().getRowCount() < MAX_ITEMS)
        {
            if (millisPast > DateUtils.MILLIS_PER_SECOND * 5) {
                // immediately if last > 5 seconds ago
                setUpdateIn(0);
            } else {
                setUpdateIn(DateUtils.MILLIS_PER_SECOND * 5); // max every 5
                // seconds
            }
        } else {
            setUpdateIn(DELAY);
        }
    }

    private void update0() {
        Runnable runner = new Runnable() {
            public void run() {
                isUpdating = true;
                Directory dir = directoryTable.getDirectory();
                SelectionModel oldSelections = selectionModel;
                Object[] selections = oldSelections.getSelections();
                int compType = directoryTable.getDirectoryTableModel()
                    .getComparatorType();
                directoryTable
                    .setModel(new DirectoryTableModel(fileFilterModel));
                directoryTable.getDirectoryTableModel()
                    .setTable(directoryTable);
                directoryTable.setColumnSizes();
                boolean recursive = recursiveSelection.isSelected();
                directoryTable.getDirectoryTableModel().setRecursive(recursive,
                    false);
                directoryTable.getDirectoryTableModel().sortBy(compType, false); // not_now
                directoryTable.setDirectory(dir, false, selections);
                lastUpdate = System.currentTimeMillis();
                isUpdating = false;
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    private class MyTimerTask extends TimerTask {
        public void run() {
            update0();
            task = null;
        }
    }

    private void setUpdateIn(long timeToWait) {
        if (task != null) {
            return;
        }
        task = new MyTimerTask();
        getController().schedule(task, timeToWait);
    }

    private class MyFolderMembershipListener implements
        FolderMembershipListener
    {
        public void memberJoined(FolderMembershipEvent folderEvent) {
            update();
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            update();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    /** makes sure that we get green if we download something */
    private class MyTransferManagerListener extends TransferAdapter {
        /** test if the transfer is on this folder */
        private void update(TransferManagerEvent event) {
            FileInfo fileInfo = event.getFile();
            if (directoryTable != null) {
                DirectoryTableModel directoryTableModel = directoryTable
                    .getDirectoryTableModel();
                Directory directory = directoryTableModel.getDirectory();
                if (directory != null) {
                    Folder folder = directory.getRootFolder();
                    if (fileInfo.getFolder(
                        getController().getFolderRepository()).equals(folder))
                    {
                        directoryTableModel.markAsChanged(fileInfo);
                    }
                }
            }
        }

        @Override
        public void downloadRequested(TransferManagerEvent event) {
            update(event);
        }

        @Override
        public void downloadStarted(TransferManagerEvent event) {
            update(event);
        }

        @Override
        public void downloadCompleted(TransferManagerEvent event) {
            update(event);
        }

        public boolean fireInEventDispathThread() {
            return false;
        }

    }

    private class BlackWhitelistAction extends SelectionBaseAction {

        BlackWhitelistAction(Controller controller,
            SelectionModel selectionModel)
        {
            super("black_list", controller, selectionModel);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            Object[] selections = getSelectionModel().getSelections();
            if (selections != null) {
                Folder folder = null;
                Object displayTarget = getUIController()
                    .getInformationQuarter().getDisplayTarget();
                // Different for files and directories.
                if (displayTarget instanceof Directory) {
                    folder = ((Directory) displayTarget).getRootFolder();
                } else if (displayTarget instanceof Folder) {
                    folder = (Folder) displayTarget;
                }
                if (folder != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < selections.length; i++) {
                        Object selection = selections[i];
                        String pattern = null;
                        if (selection instanceof FileInfo) {
                            FileInfo fileInfo = (FileInfo) selection;
                            pattern = fileInfo.getName();
                        } else if (selection instanceof Directory) {
                            Directory dir = (Directory) selection;
                            pattern = dir.getPath() + "/*";
                        }
                        if (pattern != null) {
                            sb.append(pattern);
                            if (i != selections.length - 1) {
                                sb.append('\n');
                            }
                        }
                    }
                    folderPanel.addPatterns(sb.toString());
                }
            }
        }

        public void selectionChanged(SelectionChangeEvent event) {
            enableIgnoreUnignore();
        }

        public void setWhitelist(boolean whitelist) {
            configureFromActionId(whitelist ? "white_list" : "black_list");
        }
    }

    private class UnBlackWhitelistAction extends SelectionBaseAction {

        UnBlackWhitelistAction(Controller controller,
            SelectionModel selectionModel)
        {
            super("un_black_list", controller, selectionModel);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            Object[] selections = getSelectionModel().getSelections();
            if (selections != null) {
                Folder folder = null;
                Object displayTarget = getUIController()
                    .getInformationQuarter().getDisplayTarget();
                // Different for files and directories.
                if (displayTarget instanceof Directory) {
                    folder = ((Directory) displayTarget).getRootFolder();
                } else if (displayTarget instanceof Folder) {
                    folder = (Folder) displayTarget;
                }
                if (folder != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < selections.length; i++) {
                        Object selection = selections[i];
                        String pattern = null;
                        if (selection instanceof FileInfo) {
                            FileInfo fileInfo = (FileInfo) selection;
                            pattern = fileInfo.getName();
                        } else if (selection instanceof Directory) {
                            Directory dir = (Directory) selection;
                            pattern = dir.getPath() + "/*";
                        }
                        if (pattern != null) {
                            sb.append(pattern);
                            if (i != selections.length - 1) {
                                sb.append('\n');
                            }
                        }
                    }
                    folderPanel.removePatterns(sb.toString());
                }
            }
        }

        public void selectionChanged(SelectionChangeEvent event) {
            enableIgnoreUnignore();
        }

        public void setWhitelist(boolean whitelist) {
            configureFromActionId(whitelist ? "un_white_list" : "un_black_list");
        }
    }

    private class UnmarkAction extends SelectionBaseAction {

        UnmarkAction(Controller controller,
            SelectionModel selectionModel)
        {
            super("un_mark", controller, selectionModel);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            Object[] selections = getSelectionModel().getSelections();
            if (selections != null) {
                TransferManager transferManager = getController().getTransferManager();
                for (Object selection : selections) {
                    if (selection instanceof FileInfo) {
                        FileInfo fileInfo = (FileInfo) selection;
                        if (getController().getTransferManager()
                                .isCompletedDownload(fileInfo)) {
                            transferManager
                                    .clearCompletedDownload(fileInfo);
                        }
                    } else if (selection instanceof Directory) {
                        Directory dir = (Directory) selection;
                        for (FileInfo fileInfo : dir.getFilesRecursive()) {
                            if (getController().getTransferManager()
                                    .isCompletedDownload(fileInfo)) {
                                transferManager
                                        .clearCompletedDownload(fileInfo);
                            }
                        }
                    }
                }
            }
        }

        public void selectionChanged(SelectionChangeEvent event) {

            Object[] selections = selectionModel.getSelections();

            if (selections == null || selections.length == 0) {
                setEnabled(false);
                return;
            }
            boolean enable = true;
            for (Object selection : selections) {
                if (selection instanceof FileInfo) {
                    // Only enable if recently downloaded.
                    FileInfo fileInfo = (FileInfo) selection;
                    if (!getController().getTransferManager()
                            .isCompletedDownload(fileInfo)) {
                        enable = false;
                        break;
                    }
                } else if (selection instanceof Directory) {
                    Directory dir = (Directory) selection;
                    if (!hasCompletedDownloads(dir)) {
                        enable = false;
                        break;
                    }
                }
            }
            setEnabled(enable);
        }

        private boolean hasCompletedDownloads(Directory dir) {
            TransferManager transferManager = getController()
                    .getTransferManager();
            for (FileInfo fileInfo : dir.getFilesRecursive()) {
                if (transferManager.isCompletedDownload(fileInfo)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void enableIgnoreUnignore() {

        Object[] selections = selectionModel.getSelections();

        if (selections == null || selections.length == 0) {
            blackWhitelistAction.setEnabled(false);
            unBlackWhitelistAction.setEnabled(false);
            return;
        }

        Folder folder = null;
        Object displayTarget = getUIController().getInformationQuarter()
            .getDisplayTarget();

        // Different for files and directories.
        if (displayTarget instanceof Directory) {
            folder = ((Directory) displayTarget).getRootFolder();
        } else if (displayTarget instanceof Folder) {
            folder = (Folder) displayTarget;
        }

        boolean enableIgnore = true;
        boolean enableUnignore = true;
        if (folder != null) {
            for (Object selection : selections) {
                if (selection instanceof FileInfo) {
                    // Only enable if not already ignored.
                    FileInfo fileInfo = (FileInfo) selection;
                    enableIgnore &= folder.getDiskItemFilter().isRetained(
                        fileInfo)
                        ^ folder.isWhitelist();
                    enableUnignore &= folder.getDiskItemFilter().isRetained(
                        fileInfo)
                        ^ !folder.isWhitelist();
                } else if (selection instanceof Directory) {

                    // Only enable if subs not ignored.
                    Directory dir = (Directory) selection;
                    enableIgnore &= folder.getDiskItemFilter().isRetained(dir)
                        ^ folder.isWhitelist();
                    enableUnignore &= folder.getDiskItemFilter()
                        .isRetained(dir)
                        ^ !folder.isWhitelist();
                }
            }
        }

        blackWhitelistAction.setEnabled(enableIgnore);
        unBlackWhitelistAction.setEnabled(enableUnignore);

    }

    public static class FileListTransferable implements Transferable {

        /** the flavors we have for drag and from FROM this filelist */
        private static final DataFlavor[] FLAVORS = {
            DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor,
            Directory.getDataFlavor()};

        private java.util.List<File> fileList;
        private Directory directory;

        public FileListTransferable(Directory directory, Object[] files) {
            this.directory = directory;
            this.fileList = new ArrayList(Arrays.asList(files));
        }

        public DataFlavor[] getTransferDataFlavors() {
            return FLAVORS;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return Arrays.asList(FLAVORS).contains(flavor);
        }

        public synchronized Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException
        {
            if (flavor.equals(DataFlavor.javaFileListFlavor)) {
                return fileList;
            } else if (flavor.equals(DataFlavor.stringFlavor)) {
                return fileList.toString();
            } else if (flavor.equals(Directory.getDataFlavor())) {
                return directory;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }
    }

    private class MyNodeManagerListener implements NodeManagerListener {

        public void friendAdded(NodeManagerEvent e) {
        }

        public void friendRemoved(NodeManagerEvent e) {
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnected(NodeManagerEvent e) {

        }

        /** update the filelist if member of this folder disconnects* */
        public void nodeDisconnected(NodeManagerEvent e) {
            if (directoryTable != null) {
                Directory dir = directoryTable.getDirectory();
                if (dir != null) {
                    Folder folder = dir.getRootFolder();
                    if (folder.hasMember(e.getNode())) {
                        update();
                    }
                }
            }
        }

        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    public void toggeDetails() {
        fileDetailsPanelComp.setVisible(!fileDetailsPanelComp.isVisible());
        showHideFileDetailsButton.setSelected(fileDetailsPanelComp.isVisible());
    }
}