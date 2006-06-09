package de.dal33t.powerfolder.ui.folder;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.swing.*;
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
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.PreviewPanel;
import de.dal33t.powerfolder.ui.action.*;
import de.dal33t.powerfolder.ui.dialog.FileDetailsPanel;
import de.dal33t.powerfolder.util.FileCopier;
import de.dal33t.powerfolder.util.FileInfoComparator;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Shows a Directory, holds a FileFilterPanel to filter the filelist and enable
 * the user to selected for a recursive view.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.8 $ *
 */
public class DirectoryPanel extends PFUIComponent {
    private static final String showPreviewPanelPropertyName = "show_preview_panel";
    /** enable drag and drop */
    public static final boolean enableDragAndDrop = true;
    /**
     * FileCopier, used if files are added eg from a drag and drop
     */
    private FileCopier fileCopier;

    /** the flavors we have for drag and from FROM this filelist */
    private static DataFlavor[] flavors = {DataFlavor.javaFileListFlavor,
        DataFlavor.stringFlavor};

    private JPanel panel;
    private JPopupMenu fileMenu;
    private DirectoryTable directoryTable;
    private FileFilterPanel fileFilterPanel;
    private FileFilterModel fileFilterModel;
    private JScrollPane directoryTableScrollPane;
    private JCheckBox recursiveSelection;
    private JPanel toolBar;
    private JPanel bottomToolBar;

    /**
     * gets a different action based on status of file, download if file is
     * "available" or Start if file is local(on windows)
     */
    // private JButton downloadOrStartButton;
    private JComponent fileDetailsPanelComp;

    /** The currently selected items */
    private SelectionModel selectionModel;
    private DownloadFileAction downloadFileAction;
    private DoNotAutoDownloadFileAction doNotAutoDownloadFileAction;
    private JCheckBoxMenuItem doNotAutoDownloadJCheckBoxMenuItem;
    private StartFileAction startFileAction;
    // private DownloadOrStartAction downloadOrStartAction;
    private RemoveFileAction removeFileAction;
    private RestoreFileAction restoreFileAction;
    private AbortTransferAction abortTransferAction;
    private OpenLocalFolder openLocalFolder;
    private Action showHideFileDetailsAction;

    private MyFolderListener myFolderListener;
    private MyFolderMembershipListener myFolderMembershipListener;
    private MyNodeManagerListener myNodeManegerListener;
    /**
     * if the number of rows in the table is more than MAX_ITEMS the updates
     * will be delayed to a maximum one update every minute
     */
    private int MAX_ITEMS = 200;
    /** are we now updating? */
    private boolean isUpdating = false;
    /** one minute */
    private final int DELAY = DateUtils.MILLIS_IN_MINUTE;
    /** time in milli sec of last update finish */
    private long lastUpdate;

    private MyTimerTask task;

    public DirectoryPanel(Controller controller) {
        super(controller);
        fileFilterModel = new FileFilterModel(getController());
        selectionModel = new SelectionModel();
        myFolderListener = new MyFolderListener();
        myFolderMembershipListener = new MyFolderMembershipListener();
        myNodeManegerListener = new MyNodeManagerListener();
        controller.getNodeManager().addNodeManagerListener(
            myNodeManegerListener);
        controller.getTransferManager().addListener(
            new MyTransferManagerListener());
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
            FormLayout layout = new FormLayout("fill:pref:grow",
                "pref, fill:pref:grow, pref, pref, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            // Filter bar
            builder.add(toolBar, cc.xy(1, 1));

            // Content
            builder.add(directoryTableScrollPane, cc.xy(1, 2));
            builder.addSeparator(null, cc.xy(1, 3));
            builder.add(fileDetailsPanelComp, cc.xy(1, 4));
            builder.add(bottomToolBar, cc.xy(1, 5));
            panel = builder.getPanel();
        }
        return panel;
    }

    /**
     * Initalize all nessesary components
     */
    private void initComponents() {
        downloadFileAction = new DownloadFileAction(getController(),
            selectionModel);
        doNotAutoDownloadFileAction = new DoNotAutoDownloadFileAction(
            getController(), selectionModel);
        startFileAction = new StartFileAction(getController(), selectionModel);
        // downloadOrStartAction = new DownloadOrStartAction(getController(),
        // selectionModel);
        removeFileAction = new RemoveFileAction(getController(), selectionModel);
        restoreFileAction = new RestoreFileAction(getController(),
            selectionModel);
        abortTransferAction = new AbortTransferAction(getController(),
            selectionModel);
        openLocalFolder = new OpenLocalFolder(getController());

        fileFilterPanel = new FileFilterPanel(getController(), fileFilterModel);
        directoryTable = new DirectoryTable(getController(), fileFilterModel);
        directoryTableScrollPane = new JScrollPane(directoryTable);
        directoryTable.addMouseListener(new TableMouseListener());
        directoryTable.addKeyListener(new DeleteKeyListener());
        directoryTable.addMouseWheelListener(new TableMouseListener());
        Util.whiteStripTable(directoryTable);
        Util.removeBorder(directoryTableScrollPane);
        Util.setZeroHeight(directoryTableScrollPane);

        recursiveSelection = new JCheckBox(Translation
            .getTranslation("filelist.recursive"));
        recursiveSelection.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean recursive = recursiveSelection.isSelected();
                directoryTable.getDirectoryTableModel().setRecursive(recursive,
                    true);
            }
        });
        // Add selection listener for updating selection model
        directoryTable.getSelectionModel().addListSelectionListener(
            new DirectoryListSelectionListener());

        directoryTable.getTableHeader().addMouseListener(
            new TableHeaderMouseListener());

        toolBar = createTopToolBar();
        fileDetailsPanelComp = createFileDetailsPanel();
        // Details not visible @ start
        fileDetailsPanelComp.setVisible(false);

        showHideFileDetailsAction = new ShowHideFileDetailsAction(
            fileDetailsPanelComp, getController());

        bottomToolBar = createBottomToolBar();

        // build the popup menus
        buildPopupMenus();

        if (enableDragAndDrop) {
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

    private JPanel createTopToolBar() {
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addRelatedGap();
        bar.addFixed(recursiveSelection);
        bar.addRelatedGap();
        bar.addFixed(fileFilterPanel.getUIComponent());
        bar.addRelatedGap();
        bar.setBorder(Borders.createEmptyBorder("1dlu, 1dlu, 1dlu, 1dlu"));
        return bar.getPanel();
    }

    private JPanel createBottomToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        // bar.addGridded(downloadOrStartButton);
        bar.addGridded(new JButton(downloadFileAction));
        if (Util.isWindowsSystem() || Util.isMacOS()) {
            bar.addRelatedGap();
            bar.addGridded(new JButton(startFileAction));
        }

        bar.addRelatedGap();
        bar.addGridded(new JToggleButton(showHideFileDetailsAction));

        if (Util.isWindowsSystem() || Util.isMacOS()) {
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
     * Creates the file details panel and sets it to the internal variable
     * 
     * @return
     */
    private JComponent createFileDetailsPanel() {
        FileDetailsPanel fileDetailsPanel = new FileDetailsPanel(
            getController(), selectionModel);
        Properties config = getController().getConfig();
        // check property to enable preview
        // preview of images is memory hungry
        // may cause OutOfMemoryErrors
        if (config.containsKey(showPreviewPanelPropertyName)) {
            if (config.getProperty(showPreviewPanelPropertyName).toString()
                .equalsIgnoreCase("true"))
            {
                PreviewPanel previewPanel = new PreviewPanel(getController(),
                    selectionModel, this);
                FormLayout layout = new FormLayout("pref, fill:pref:grow",
                    "3dlu, pref, fill:pref, pref");
                PanelBuilder builder = new PanelBuilder(layout);
                CellConstraints cc = new CellConstraints();
                builder.addSeparator(null, cc.xy(1, 2));
                builder.add(fileDetailsPanel.getEmbeddedPanel(), cc.xy(1, 3));
                builder.add(previewPanel.getUIComponent(), cc.xy(2, 3));
                builder.addSeparator(null, cc.xy(1, 4));
                return builder.getPanel();
            }
        }
        FormLayout layout = new FormLayout("fill:pref:grow",
            "3dlu, pref, fill:pref, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addSeparator(null, cc.xy(1, 2));
        builder.add(fileDetailsPanel.getEmbeddedPanel(), cc.xy(1, 3));
        builder.addSeparator(null, cc.xy(1, 4));
        return builder.getPanel();
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
        if (Util.isWindowsSystem() || Util.isMacOS()) {
            fileMenu.add(startFileAction);
            fileMenu.add(openLocalFolder);
        }
        fileMenu.add(downloadFileAction);
        doNotAutoDownloadJCheckBoxMenuItem = new JCheckBoxMenuItem(
            doNotAutoDownloadFileAction);

        fileMenu.add(doNotAutoDownloadJCheckBoxMenuItem);
        fileMenu.add(abortTransferAction);
        fileMenu.add(removeFileAction);
        fileMenu.add(restoreFileAction);
        fileMenu.addSeparator();

        fileMenu.add(new ChangeFriendStatusAction(getController(),
            selectionModel));
        fileMenu.add(new CopyFileListToClipboardAction(getController(),
            selectionModel));
    }

    // Actions ****************************************************************

    private class CopyFileListToClipboardAction extends SelectionBaseAction {

        public CopyFileListToClipboardAction(Controller controller,
            SelectionModel selectionModel)
        {
            super("linktoclipboard", controller, selectionModel);
        }

        public void actionPerformed(ActionEvent e) {
            Object sel = selectionModel.getSelection();
            if (sel instanceof FileInfo) {
                // Set clibboard contents
                Util.setClipboardContents(((FileInfo) sel).toPowerFolderLink());
            }
        }

        public void selectionChanged(SelectionChangeEvent event) {
            Object[] selections = selectionModel.getSelections();
            setEnabled(selections != null && selections.length == 1
                && selections[0] instanceof FileInfo);

        }

    }

    /**
     * Returns the selection model. Changes upon selection.
     * 
     * @return
     */
    public SelectionModel getSelectionModel() {
        return selectionModel;
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
     * Listner on table header, takes care about the sorting of table
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
        Object[] selectedValues = getSelectionModel().getSelections();
        if (selectedValues == null || selectedValues.length == 0) {
            return null;
        }
        // check for dirs:
        List<File> returnValues = new ArrayList();
        for (int i = 0; i < selectedValues.length; i++) {
            if (selectedValues[i] instanceof FileInfo) {
                FileInfo fileInfo = (FileInfo) selectedValues[i];
                File file = fileInfo.getDiskFile(getController()
                    .getFolderRepository());
                if (file.exists()) {// only use files that exists
                    returnValues.add(file);
                }
            } else if (selectedValues[i] instanceof Directory) {
                Directory directory = (Directory) selectedValues[i];
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
                        draggedValues.toArray());
                    event.startDrag(null, transferable);
                }
            }
        }
    }

    /**
     * determents if we are the source of this drag and drop TODO: fix if drag
     * from a Folder, then drag over other folder and then beck to this one it
     * will return false but should return true.
     */
    public boolean amIDragSource(DropTargetDragEvent dtde) {
        Transferable trans = dtde.getTransferable();
        try {
            List<File> fileList = (List<File>) trans
                .getTransferData(DataFlavor.javaFileListFlavor);
            List<File> selectedFiles = getSelectedFiles();
            if (selectedFiles != null
                && fileList.size() == selectedFiles.size())
            {
                // if the filelist is the same as the currently selected
                // one we assume we are the source of this drag and drop
                // and we will reject this.
                if (selectedFiles.containsAll(fileList)) {
                    return true;
                }
            }
            return false;
        } catch (UnsupportedFlavorException ufe) {
            throw new IllegalStateException(ufe);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * helper class to enable drops of more file and one overwtrite / skipp/
     * dialog and allows drop of directories
     */
    private class Dropper {
        // count items to see if there are more for the dialog
        // options
        private int count = 0;
        private boolean overwriteAll = false;
        private boolean skipAll = false;
        private boolean cancel = false;
        private int total;

        public Dropper(int total) {
            this.total = total;
        }

        private boolean drop(File file, Directory directory) {
            // test if a dir is dropped
            if (file.isDirectory()) {
                Directory subDirectory = directory.getCreateSubDirectory(file
                    .getName());
                File[] files = file.listFiles();
                total += files.length;
                // Dropper dropper = new Dropper(subDirectory, files.length);

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
            if (directory.contains(file)) {
                if (skipAll) { // skip all duplicates
                    // continueDropping = true;
                    return true;
                }
                if (!overwriteAll) {
                    if (count < total) {// dialog for more items
                        Object[] options = {
                            Translation.getTranslation("general.overwrite"),
                            Translation.getTranslation("general.overwrite_all"),
                            Translation.getTranslation("general.skip"),
                            Translation
                                .getTranslation("directorypanel.dropfile_duplicate_dialog.skipall"),
                            Translation.getTranslation("general.cancel")};
                        int option = JOptionPane
                            .showOptionDialog(
                                getUIController().getMainFrame()
                                    .getUIComponent(),
                                Translation
                                    .getTranslation(
                                        "directorypanel.dropfile_duplicate_dialog.text",
                                        file.getName()),
                                Translation
                                    .getTranslation("directorypanel.dropfile_duplicate_dialog.title"),

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
                                        "directorypanel.dropfile_duplicate_dialog.text",
                                        file.getName()),
                                Translation
                                    .getTranslation("directorypanel.dropfile_duplicate_dialog.title"),
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
            DirectoryPanel.this.directoryTable.getParent().setCursor(
                Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            if (!directory.copyFileFrom(file, getFileCopier())) {
                // something failed
                return false;
            }
            DirectoryPanel.this.directoryTable.getParent().setCursor(
                Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return true;
        }
    }

    /** drop a transferable on this filelist, must be of the javafilelist flavor */
    public boolean drop(Transferable trans) {
        try {
            List<File> fileList = (List<File>) trans
                .getTransferData(DataFlavor.javaFileListFlavor);
            Dropper dropper = new Dropper(fileList.size());
            Directory directory = directoryTable.getDirectory();
            if (directory == null) {
                log().error("directory null");
                return false;
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

    /** drop a transferable on this directory, must be of the javafilelist flavor */
    public boolean drop(Directory directory, Transferable trans) {
        try {
            List<File> fileList = (List<File>) trans
                .getTransferData(DataFlavor.javaFileListFlavor);
            Dropper dropper = new Dropper(fileList.size());
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

    /** class that handles the Drag and Drop TO this FileList. */
    private class MyDropTargetListener implements DropTargetListener {
        public void dragEnter(DropTargetDragEvent dtde) {
            if ((dtde.getDropAction() == DnDConstants.ACTION_COPY || dtde
                .getDropAction() == DnDConstants.ACTION_MOVE)
                && Arrays.asList(dtde.getCurrentDataFlavors()).contains(
                    DataFlavor.javaFileListFlavor))
            {
                if (amIDragSource(dtde)) {
                    dtde.rejectDrag();
                } else {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                }
            } else {
                dtde.rejectDrag();
            }
        }

        public void dragExit(DropTargetEvent dtde) {
        }

        public void dragOver(DropTargetDragEvent dtde) {
            // check the source (maybe table or scrollpane)
            Object source = ((DropTarget) dtde.getSource()).getComponent();
            if (source == directoryTable) {
                Point location = dtde.getLocation();
                int row = directoryTable.rowAtPoint(location);
                if (row < 0) {
                    return;
                }
                Object object = directoryTable.getModel().getValueAt(row, 0);
                if (object instanceof Directory) {
                    directoryTable.getSelectionModel().setSelectionInterval(
                        row, row);
                } else {
                    // deselects a directory that was selected by the lines
                    // above if hover above something else
                    directoryTable.getSelectionModel().clearSelection();
                }
            }
        }

        public void drop(DropTargetDropEvent dtde) {

            if (Arrays.asList(dtde.getCurrentDataFlavors()).contains(
                DataFlavor.javaFileListFlavor))
            {
                Object source = ((DropTarget) dtde.getSource()).getComponent();
                if (source == directoryTable) {
                    Point location = dtde.getLocation();
                    int row = directoryTable.rowAtPoint(location);
                    if (row < 0) {
                        return;
                    }
                    Object object = directoryTable.getModel()
                        .getValueAt(row, 0);
                    // drop into directory
                    if (object instanceof Directory) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        boolean succes = DirectoryPanel.this.drop(
                            (Directory) object, dtde.getTransferable());
                        dtde.dropComplete(succes);
                        return;
                    }
                }
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                boolean succes = DirectoryPanel.this.drop(dtde
                    .getTransferable());
                dtde.dropComplete(succes);

            }
        }

        public void dropActionChanged(DropTargetDragEvent dtde) {
            if (dtde.getDropAction() == DnDConstants.ACTION_COPY
                || dtde.getDropAction() == DnDConstants.ACTION_MOVE)
            {
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
                File path = new File(localBase.getAbsolutePath() + "/"
                    + directory.getPath());
                while (!path.exists()) { // try finding the first path that
                    // exists
                    String pathStr = path.getAbsolutePath();
                    int index = pathStr.lastIndexOf(File.separatorChar);
                    if (index == -1)
                        return;
                    path = new File(pathStr.substring(0, index));
                }
                try {
                    Util.executeFile(path);
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

    private class MyFolderListener implements FolderListener {
        public void folderChanged(FolderEvent folderEvent) {
            Folder folder = (Folder) folderEvent.getSource();
            if (folder == directoryTable.getDirectory().getRootFolder()) {
                update();
            } else {
                log().debug("not listening to folder " + folder);
            }
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
            Folder folder = (Folder) folderEvent.getSource();
            if (folder == directoryTable.getDirectory().getRootFolder()) {
                update();
            } else {
                log().debug("not listening to folder " + folder);
            }
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
        }
        
        public boolean fireInEventDispathThread() {
            return false;
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
            if (millisPast > DateUtils.MILLIS_IN_SECOND * 5) {
                // immediately if last > 5 seconds ago
                setUpdateIn(0);
            } else {
                setUpdateIn(DateUtils.MILLIS_IN_SECOND * 5); // max every 5
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
                FileInfoComparator comp = directoryTable
                    .getDirectoryTableModel().getComparator();
                directoryTable
                    .setModel(new DirectoryTableModel(fileFilterModel));
                directoryTable.getDirectoryTableModel()
                    .setTable(directoryTable);
                directoryTable.setColumnSizes();
                boolean recursive = recursiveSelection.isSelected();
                directoryTable.getDirectoryTableModel().setRecursive(recursive,
                    false);
                directoryTable.getDirectoryTableModel().sortBy(comp, false); // not_now
                directoryTable.setDirectory(dir, false, selections);
                lastUpdate = System.currentTimeMillis();
                isUpdating = false;
            }
        };
        SwingUtilities.invokeLater(runner);
    }

    private class MyTimerTask extends TimerTask {
        public void run() {
            update0();
            task = null;
        }
    }

    private void setUpdateIn(int timeToWait) {
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
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            update();
        }
        
        public boolean fireInEventDispathThread() {
            return false;
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
        public void downloadRequested(TransferManagerEvent event)
        {
            update(event);
        }

        @Override
        public void downloadStarted(TransferManagerEvent event)
        {
            update(event);
        }

        @Override
        public void downloadCompleted(TransferManagerEvent event)
        {
            update(event);
        }
        
        public boolean fireInEventDispathThread() {
            return false;
        }

    }

    /**
     * Action, which marks file as "do not donwload" (in an auto download
     * syncprofile)
     * 
     * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
     * @version $Revision: 1.8 $
     */
    private class DoNotAutoDownloadFileAction extends SelectionBaseAction {
        /**
         * @param controller
         */
        public DoNotAutoDownloadFileAction(Controller controller,
            SelectionModel selectionModel)
        {
            super("donotdownloadfile", controller, selectionModel);
            setEnabled(false);
            putValue(Action.SMALL_ICON, null);
        }

        public void selectionChanged(SelectionChangeEvent event) {
            Object[] selections = getSelectionModel().getSelections();
            Object target = getUIController().getInformationQuarter()
                .getDisplayTarget();
            final Folder folder;
            if (target instanceof Directory) {
                folder = ((Directory) target).getRootFolder();
            } else if (target instanceof Folder) {
                folder = (Folder) target;
            } else {
                log().warn(
                    "Unable to mark file for auto download files on target: "
                        + target);
                return;
            }

            if (selections != null && selections.length != 0) {
                setEnabled(true);
                doNotAutoDownloadJCheckBoxMenuItem.setSelected(false);
                boolean setValueTrue = false; // detect different status
                boolean setValueFalse = false; // detect different status
                // uses some complex checks to make sure we dont have
                // conflicting status of files/dirs. only set this menu item to
                // enable if all are the same.
                for (int i = 0; i < selections.length; i++) {
                    if (selections[i] instanceof FileInfo) {
                        FileInfo fileInfo = (FileInfo) selections[i];
                        if (fileInfo.diskFileExists(getController())) {
                            setEnabled(false);
                            break;
                        } else if (folder.isInBlacklist(fileInfo)) {
                            if (setValueFalse) {
                                setEnabled(false);
                                doNotAutoDownloadJCheckBoxMenuItem
                                    .setSelected(false);
                                break;
                            }
                            doNotAutoDownloadJCheckBoxMenuItem
                                .setSelected(true);
                            setValueTrue = true;
                        } else { // detect differnt status
                            setValueFalse = true;
                            if (setValueTrue) {
                                setEnabled(false);
                                doNotAutoDownloadJCheckBoxMenuItem
                                    .setSelected(false);
                                break;
                            }
                        }

                    } else if (selections[i] instanceof Directory) {
                        Directory directory = (Directory) selections[i];
                        if (!directory.isExpected(getController()
                            .getFolderRepository()))
                        {
                            setEnabled(false);
                            break;
                        } else if (folder.isInBlacklist(directory
                            .getFilesRecursive()))
                        {
                            // detect differnt status:
                            if (setValueFalse) {
                                setEnabled(false);
                                doNotAutoDownloadJCheckBoxMenuItem
                                    .setSelected(false);
                                break;
                            }

                            doNotAutoDownloadJCheckBoxMenuItem
                                .setSelected(true);
                            setValueTrue = true;
                        } else { // detect differnt status
                            setValueFalse = true;
                            if (setValueTrue) {
                                setEnabled(false);
                                doNotAutoDownloadJCheckBoxMenuItem
                                    .setSelected(false);
                                break;
                            }
                        }
                    } else {
                        setEnabled(false);
                        break;
                    }
                }
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == doNotAutoDownloadJCheckBoxMenuItem) {
                boolean add = doNotAutoDownloadJCheckBoxMenuItem.isSelected();
                Object target = getUIController().getInformationQuarter()
                    .getDisplayTarget();
                final Folder folder;
                if (target instanceof Directory) {
                    folder = ((Directory) target).getRootFolder();
                } else if (target instanceof Folder) {
                    folder = (Folder) target;
                } else {
                    log().warn(
                        "Unable to mark file for auto download files on target: "
                            + target);
                    return;
                }
                Object[] selections = getSelectionModel().getSelections();
                if (selections != null && selections.length > 0) {
                    for (int i = 0; i < selections.length; i++) {
                        if (selections[i] instanceof FileInfo) {
                            FileInfo fileInfo = (FileInfo) selections[i];
                            if (add) {
                                folder.addToBlacklist(fileInfo);
                            } else {
                                folder.removeFromBlacklist(fileInfo);

                            }
                        } else if (selections[i] instanceof Directory) {
                            Directory directory = (Directory) selections[i];
                            List<FileInfo> fileInfos = directory
                                .getFilesRecursive();
                            if (add) {
                                folder.addAllToBlacklist(fileInfos);
                            } else {
                                folder.removeAllFromBlacklist(fileInfos);
                            }
                        } else {
                            log().debug(
                                "cannot toIgnore: "
                                    + selections[i].getClass().getName());
                            return;
                        }
                    }
                }
                if (!add) {
                    // trigger download if something was removed for the
                    // exclusions
                    folder.requestMissingFilesForAutodownload();
                } else {// something removed
                    // abort all autodownloads on this folder
                    getController().getTransferManager().abortAllAutodownloads(
                        folder);
                    // and request those still needed
                    folder.requestMissingFilesForAutodownload();
                }
            }
        }
    }

    public class FileListTransferable implements Transferable {

        private java.util.List fileList;

        public FileListTransferable(Object[] files) {
            fileList = new ArrayList(Arrays.asList(files));
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return Arrays.asList(flavors).contains(flavor);
        }

        public synchronized Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException
        {
            if (flavor.equals(DataFlavor.javaFileListFlavor)) {
                return fileList;
            } else if (flavor.equals(DataFlavor.stringFlavor)) {
                return fileList.toString();
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
        /**update the filelist if member of this folder disconnects**/
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
        
        public boolean fireInEventDispathThread() {
            return false;
        }
    }
}