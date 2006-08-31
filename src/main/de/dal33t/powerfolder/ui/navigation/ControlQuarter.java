/* $Id: ControlQuarter.java,v 1.11 2006/03/09 13:24:37 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.navigation;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.uif_lite.panel.SimpleInternalFrame;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.*;
import de.dal33t.powerfolder.event.NavigationEvent;
import de.dal33t.powerfolder.event.NavigationListener;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.*;
import de.dal33t.powerfolder.ui.folder.DirectoryPanel;
import de.dal33t.powerfolder.ui.folder.FolderPanel;
import de.dal33t.powerfolder.ui.render.NavTreeCellRenderer;
import de.dal33t.powerfolder.ui.widget.AutoScrollingJTree;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.OSUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.TreeNodeList;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Controler Quarter.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11 $
 */
public class ControlQuarter extends PFUIComponent {

    /* Complete panel */
    private JPanel uiPanel;

    /* Navtree */
    private JTree uiTree;
    private NavTreeModel navTreeModel;

    /* The popup menu */
    private JPopupMenu repositoryMenu;
    private JPopupMenu folderMenu;
    private JPopupMenu unjoinedFolderMenu;
    private JPopupMenu memberMenu;
    private JPopupMenu friendsListMenu;
    private JPopupMenu directoryMenu;
    /* Models */
    /** The parent of the currently selected value in our selection model */
    private Object selectionParent;
    /** The currently selected item */
    private SelectionModel selectionModel;

    private NavigationModel navigationModel;
    /**
     * The path in the tree that was last expanded, use to restore the tree
     * state if a tree structure change was fired.
     */
    private TreePath lastExpandedPath;

    /**
     * Constructs a new navigation tree for a controller
     * 
     * @param controller
     */
    public ControlQuarter(Controller controller) {
        super(controller);
        navTreeModel = new NavTreeModel(getController());
        selectionModel = new SelectionModel();
        selectionParent = null;
    }

    /*
     * Exposing methods for UI Component **************************************
     * components get initalized lazy
     */

    /**
     * TODO move this into a <code>UIModel</code>
     * 
     * @return the uis navigation model
     */
    public NavigationModel getNavigationModel() {
        return navigationModel;
    }

    /**
     * Answers and builds if needed the complete ui component
     * 
     * @return
     */
    public JComponent getUIComponent() {
        if (uiPanel == null) {
            FormLayout layout = new FormLayout("fill:pref:grow", "fill:0:grow");
            PanelBuilder builder = new PanelBuilder(layout);

            CellConstraints cc = new CellConstraints();

            // Make preferred size smaller.
            JScrollPane pane = new JScrollPane(getUITree());
            pane.setBorder(Borders.EMPTY_BORDER);
            UIUtil.setZeroHeight(pane);
            Dimension dims = pane.getPreferredSize();
            dims.width = 10;
            pane.setPreferredSize(dims);

            builder.add(pane, cc.xy(1, 1));

            SimpleInternalFrame frame = new SimpleInternalFrame(Translation
                .getTranslation("navtree.title"));
            frame.setToolBar(new NavigationToolBar(getController(),
                navigationModel).getUIComponent());
            frame.add(builder.getPanel());
            uiPanel = frame;
        }
        return uiPanel;
    }

    /**
     * @return
     */
    public synchronized JTree getUITree() {
        if (uiTree == null) {
            uiTree = new AutoScrollingJTree(getNavigationTreeModel());
            getNavigationTreeModel().expandFriendList();
            uiTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);

            // dont expand nodeManager
            /*
             * TreePath nodeManager = new TreePath( new Object[] {
             * navTreeModel.getRoot(),
             * getController().getNodeManager().getTreeNode()});
             * uiTree.expandPath(nodeManager);
             */

            // Expand folders
            log().verbose("Expanding folders on navtree");
            TreePath folders = getUIController().getFolderRepositoryModel()
                .getMyFoldersTreeNode().getPathTo();
            uiTree.expandPath(folders);

            // Selection listener to update selection model
            uiTree.getSelectionModel().addTreeSelectionListener(
                new TreeSelectionListener() {
                    public void valueChanged(TreeSelectionEvent e) {
                        TreePath selectionPath = e.getPath();
                        if (logVerbose) {
                            log().verbose(selectionPath.toString());
                        }
                        // First set parent of selection
                        if (selectionPath.getPathCount() > 1) {
                            selectionParent = UIUtil
                                .getUserObject(selectionPath
                                    .getPathComponent(selectionPath
                                        .getPathCount() - 2));
                        } else {
                            // Parent of selection empty
                            selectionParent = null;
                        }

                        Object newSelection = UIUtil.getUserObject(selectionPath
                            .getLastPathComponent());
                        selectionModel.setSelection(newSelection);
                        if (logVerbose) {
                            log().verbose(
                                "Selection: " + selectionModel.getSelection()
                                    + ", parent: " + selectionParent);
                        }
                    }
                });

            // build popup menus
            buildPopupMenus();

            // Set renderer
            uiTree.setCellRenderer(new NavTreeCellRenderer(getController()));

            uiTree.addMouseListener(new NavTreeListener());

            // remember the last expanded path
            // make it null if a parent of the last expanded path is closed
            uiTree.addTreeExpansionListener(new TreeExpansionListener() {
                public void treeCollapsed(TreeExpansionEvent treeExpansionEvent)
                {
                    TreePath closedPath = treeExpansionEvent.getPath();                    
                    //log().debug("closed path          : " + closedPath);
                    //log().debug("lastExpandedPath path: " + lastExpandedPath);
                    
                    //note that this method name maybe confusing
                    //it is true if lastExpandedPath is a descendant of closedPath 
                    if (closedPath.isDescendant(lastExpandedPath)) {
                      //  log().debug("isDescendant!");
                        lastExpandedPath = null;
                    }
                }

                public void treeExpanded(TreeExpansionEvent treeExpansionEvent)
                {
                    lastExpandedPath = treeExpansionEvent.getPath();
                }
            });
            if (DirectoryPanel.enableDragAndDrop) {
                new DropTarget(uiTree, DnDConstants.ACTION_COPY,
                    new MyDropTargetListener(), true);
            }
            navigationModel = new NavigationModel(uiTree.getSelectionModel());
            navigationModel.addNavigationListener(new NavigationListener() {
                public void navigationChanged(NavigationEvent event) {
                    TreePath newTreePath = event.getTreePath();
                    TreePath current = uiTree.getSelectionModel()
                        .getSelectionPath();
                    if (current != null && newTreePath != null
                        && !newTreePath.equals(current))
                    {
                        setSelectedTreePath(newTreePath);
                    }
                }
            });
        }
        return uiTree;
    }

    /**
     * Builds the popup menues
     */
    private void buildPopupMenus() {
        // Popupmenus
        // Folder repository menu
        repositoryMenu = new JPopupMenu();
        repositoryMenu.add(getUIController().getFolderCreateAction());
        repositoryMenu.add(getUIController().getToggleSilentModeAction());
        repositoryMenu.add(getUIController().getScanFolderAction());

        // create popup menu for member
        memberMenu = new JPopupMenu();
        memberMenu
            .add(new OpenChatAction(getController(), getSelectionModel()));

        memberMenu.add(new ChangeFriendStatusAction(getController(),
            getSelectionModel()));
        memberMenu.add(getUIController().getInviteUserAction());

        memberMenu.addSeparator();
        memberMenu.add(getUIController().getReconnectAction());
        if (getController().isVerbose()) {
            // show request debug only in verbose mode
            memberMenu.add(getUIController().getRequestReportAction());
        }

        // create popup menu for directory

        directoryMenu = new JPopupMenu();
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            directoryMenu.add(new OpenLocalFolder(getController()));
        }

        // create popup menu for folder
        folderMenu = new JPopupMenu();
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            folderMenu.add(new OpenLocalFolder(getController()));
        }
        folderMenu
            .add(new OpenChatAction(getController(), getSelectionModel()));
        folderMenu.add(new InviteAction(Icons.NODE, getController(),
            getSelectionModel()));

        // Separator
        folderMenu.addSeparator();

        folderMenu.add(getUIController().getFolderJoinLeaveAction());
        folderMenu.add(getUIController().getScanFolderAction());
        // Build sync profile menu
        JMenu syncProfileMenu = new JMenu(Translation
            .getTranslation("general.syncprofile"));
        syncProfileMenu.setIcon(Icons.SYNC_MODE);
        for (int i = 0; i < SyncProfile.DEFAULT_SYNC_PROFILES.length; i++) {
            SyncProfile syncProfile = SyncProfile.DEFAULT_SYNC_PROFILES[i];
            Action changeSyncProfileAction = new ChangeSyncProfileAction(
                syncProfile, getSelectionModel());
            syncProfileMenu.add(changeSyncProfileAction);
        }

        folderMenu.add(syncProfileMenu);

        if (getUIController().getFolderCreateShortcutAction()
            .getValue(CreateShortcutAction.SUPPORTED) == Boolean.TRUE) {
            // External actions
            folderMenu.addSeparator();
            folderMenu.add(getUIController().getFolderCreateShortcutAction());
        }
        
        // context menu for unjoined folders
        unjoinedFolderMenu = new JPopupMenu();
        unjoinedFolderMenu.add(getUIController().getFolderJoinLeaveAction());
        unjoinedFolderMenu.add(getUIController().getRequestFileListAction());

        // Friends list popup menu
        friendsListMenu = new JPopupMenu();
        friendsListMenu.add(getUIController().getSetMasterNodeAction());
    }

    // Exposing ***************************************************************

    /**
     * used in RootTable and on init, should alway be a level 1 or 2 treeNode,
     * the root or just below the root
     */
    public void setSelected(TreeNode node) {
        if (node != null) {
            if (node == navTreeModel.getRootNode()) {
                TreeNode[] path = new TreeNode[1];
                path[0] = navTreeModel.getRootNode();
                setSelectedPath(path);
            } else {
                TreeNode[] path = new TreeNode[2];
                path[0] = navTreeModel.getRootNode();
                path[1] = node;
                setSelectedPath(path);
            }
        }
    }

    private void setSelectedPath(TreeNode[] pathArray) {
        TreePath treePath = new TreePath(pathArray);
        setSelectedTreePath(treePath);
    }

    private void setSelectedTreePath(final TreePath path) {
        Runnable runner = new Runnable() {
            public void run() {
                uiTree.setSelectionPath(path);
                uiTree.scrollPathToVisible(path);
            }
        };
        if (EventQueue.isDispatchThread()) {
            runner.run();
        } else {
            EventQueue.invokeLater(runner);
        }
    }

    /**
     * sets the selected Directory in a tree
     * 
     * @param directory
     *            The newly selected directory
     */
    public void setSelected(Directory directory) {
        log().verbose("setSelected:" + directory);
        if (directory != null) {
            Folder folder = directory.getRootFolder();
            List pathToDirTreeNode = directory.getTreeNodePath();
            TreeNode[] path = new TreeNode[3 + pathToDirTreeNode.size()];
            path[0] = navTreeModel.getRootNode();
            path[1] = getUIController().getFolderRepositoryModel()
                .getMyFoldersTreeNode();
            path[2] = folder.getTreeNode();
            for (int i = 0; i < pathToDirTreeNode.size(); i++) {
                path[path.length - (i + 1)] = (TreeNode) pathToDirTreeNode
                    .get(i);
            }
            setSelectedPath(path);
        }
    }

    public void setSelected(FolderDetails folderDetails) {
        log().verbose("setSelected:" + folderDetails);
        if (folderDetails != null) {
            TreeNodeList previewTreeNode = getUIController()
                .getFolderRepositoryModel().getPublicFoldersTreeNode();

            // Add to preview
            previewTreeNode.addChild(folderDetails);

            TreeNode[] path = new TreeNode[3];
            path[0] = navTreeModel.getRootNode();
            path[1] = previewTreeNode;
            int index = previewTreeNode.indexOf(folderDetails);
            path[2] = previewTreeNode.getChildAt(index);

            setSelectedPath(path);
        }
    }

    /**
     * navigation uses this to reopen the expanded nodes if model has changed
     */
    public JTree getTree() {
        return uiTree;
    }

    public void setSelected(Folder folder) {
        MutableTreeNode node = folder.getTreeNode();
        TreeNode[] path = new TreeNode[3];
        path[0] = navTreeModel.getRootNode();
        path[1] = getUIController().getFolderRepositoryModel()
            .getMyFoldersTreeNode();
        path[2] = node;
        setSelectedPath(path);
    }

    public void setSelected(Member member) {
        if (member.isFriend()) { // try to select the friend node
            TreeNodeList friendsNode = getUIController().getNodeManagerModel()
                .getFriendsTreeNode();
            for (int i = 0; i < friendsNode.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) friendsNode
                    .getChildAt(i);
                if (member == node.getUserObject()) {
                    TreeNode[] path = new TreeNode[3];
                    path[0] = navTreeModel.getRootNode();
                    path[1] = friendsNode;
                    path[2] = node;
                    setSelectedPath(path);
                    return;
                }
            }
        } else { // else try to find the member in "chats"
            TreeNodeList chatsNode = getUIController().getNodeManagerModel()
                .getNotInFriendsTreeNodes();
            for (int i = 0; i < chatsNode.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) chatsNode
                    .getChildAt(i);
                if (member == node.getUserObject()) {
                    TreeNode[] path = new TreeNode[3];
                    path[0] = navTreeModel.getRootNode();
                    path[1] = chatsNode;
                    path[2] = node;
                    setSelectedPath(path);
                    return;
                }
            }
        }
        // Neither a friend nor in a chat:
        // select the connected member node
        if (getController().isVerbose()) {
            TreeNodeList otherNode = getUIController().getNodeManagerModel()
                .getConnectedTreeNode();
            for (int i = 0; i < otherNode.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) otherNode
                    .getChildAt(i);
                if (member == node.getUserObject()) {
                    TreeNode[] path = new TreeNode[3];
                    path[0] = navTreeModel.getRootNode();
                    path[1] = otherNode;
                    path[2] = node;
                    setSelectedPath(path);
                    return;
                }
            }
        }
    }

    /**
     * Answers the navtree model
     * 
     * @return
     */
    public NavTreeModel getNavigationTreeModel() {
        return navTreeModel;
    }

    /**
     * The path in the tree that was last expanded, use to restore the tree
     * state if a tree structure change was fired.
     */
    protected TreePath getLastExpandedPath() {
        return lastExpandedPath;
    }

    /**
     * Convience method for getSelectedItem. Returns the selected folder, or
     * null if nothing or not a folder is selected
     * 
     * @return the selected folder or null
     */
    public Folder getSelectedFolder() {
        Object item = getSelectedItem();
        if (item instanceof Folder) {
            return (Folder) item;
        }
        return null;
    }

    /**
     * Returns the selection model, contains the model for the selected item on
     * navtree. If you need information about the parent of the current
     * selection see <code>getSelectionParentModel</code>
     * 
     * @see #getSelectionParentModel()
     * @return
     */
    public SelectionModel getSelectionModel() {
        return selectionModel;
    }

    /**
     * @return The selected item in navtree
     */
    public Object getSelectedItem() {
        return getSelectionModel().getSelection();
    }

    /**
     * Returns the parent tree item of the selected item. Listens/Act on
     * selection changes by listening to our <code>SelectionModel</code>
     * 
     * @see #getSelectionModel()
     * @return
     */
    public Object getSelectionParent() {
        return selectionParent;
    }

    /**
     * Navtree listner, cares for selection and popup menus
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.11 $
     */
    private class NavTreeListener extends MouseAdapter {
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
            TreePath path = uiTree.getPathForLocation(evt.getX(), evt.getY());
            if (path == null) {
                return;
            }
            Object selection = UIUtil.getUserObject(path.getLastPathComponent());
            if (path.getLastPathComponent() != getSelectedItem()) {
                setSelectedTreePath(path);
            }
            if (selection instanceof FolderRepository) {
                repositoryMenu.show(evt.getComponent(), evt.getX(), evt.getY());
            } else if (selection instanceof Member) {
                // show menu
                memberMenu.show(evt.getComponent(), evt.getX(), evt.getY());
            } else if (selection instanceof Folder) {
                // show menu
                folderMenu.show(evt.getComponent(), evt.getX(), evt.getY());
            } else if (selection instanceof FolderDetails) {
                // show popup menu
                unjoinedFolderMenu.show(evt.getComponent(), evt.getX(), evt
                    .getY());
            } else if (selection == getUIController().getNodeManagerModel()
                .getFriendsTreeNode())
            {
                if (getController().isVerbose()) {
                    friendsListMenu.show(evt.getComponent(), evt.getX(), evt
                        .getY());
                } else {
                    log().warn("Not displaing friendlist/master user selection context menu");
                }
            } else if (selection instanceof Directory) {
                if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
                    directoryMenu.show(evt.getComponent(), evt.getX(), evt
                        .getY());
                }
            }
        }
    }

    /*
     * General ****************************************************************
     */

    public String toString() {
        return "Navigation tree";
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
            Object selection = getSelectedItem();
            if (selection instanceof Folder) {
                Folder folder = (Folder) selection;
                File localBase = folder.getLocalBase();
                try {
                    FileUtils.executeFile(localBase);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } else if (selection instanceof Directory) {
                Directory directory = (Directory) selection;
                if (directory != null) {
                    Folder folder = directory.getRootFolder();
                    File localBase = folder.getLocalBase();
                    File path = new File(localBase.getAbsolutePath() + "/"
                        + directory.getPath());
                    while (!path.exists()) { // try finding the first path
                        // that
                        // exists
                        String pathStr = path.getAbsolutePath();
                        int index = pathStr.lastIndexOf(File.separatorChar);
                        if (index == -1)
                            return;
                        path = new File(pathStr.substring(0, index));
                    }
                    try {
                        FileUtils.executeFile(path);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * class that handles the Drag and Drop TO the uiTree. will expand tree
     * items after a delay if drag above them
     */
    private class MyDropTargetListener implements DropTargetListener {
        long timeEntered;
        int delay = 500;
        Object lastSelection;

        public void dragEnter(DropTargetDragEvent dtde) {
            if ((dtde.getDropAction() == DnDConstants.ACTION_COPY || dtde
                .getDropAction() == DnDConstants.ACTION_MOVE)
                && Arrays.asList(dtde.getCurrentDataFlavors()).contains(
                    DataFlavor.javaFileListFlavor))
            {
                Point location = dtde.getLocation();
                TreePath path = uiTree.getPathForLocation(location.x,
                    location.y);
                if (path == null) {
                    return;
                }
                Object selection = UIUtil.getUserObject(path
                    .getLastPathComponent());

                if (selection instanceof Folder
                    || selection instanceof Directory)
                {
                    // reject if current filelist is the source
                    if (getUIController().getInformationQuarter()
                        .getFolderPanel().getDirectoryPanel().amIDragSource(
                            dtde))
                    {
                        dtde.rejectDrag();
                    } else {
                        timeEntered = System.currentTimeMillis();
                        lastSelection = selection;
                        dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    }
                } else {
                    dtde.rejectDrag();
                }
            } else {
                dtde.rejectDrag();
            }
        }

        public void dragExit(DropTargetEvent dte) {
            timeEntered = 0;
            lastSelection = null;
        }

        public void dragOver(DropTargetDragEvent dtde) {
            Point location = dtde.getLocation();
            TreePath path = uiTree.getPathForLocation(location.x, location.y);
            if (path == null) {
                return;
            }
            Object selection = UIUtil.getUserObject(path.getLastPathComponent());
            if (!(selection instanceof Folder || selection instanceof Directory))
            {
                dtde.rejectDrag();
            } else {
                if (getUIController().getInformationQuarter().getFolderPanel()
                    .getDirectoryPanel().amIDragSource(dtde))
                {
                    dtde.rejectDrag();
                } else {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                }
            }

            if (!selection.equals(lastSelection)) {
                // restart the delay if different object if being draged
                // over
                timeEntered = System.currentTimeMillis();
                lastSelection = selection;
                return;
            }
            lastSelection = selection;
            if ((System.currentTimeMillis() - timeEntered) > delay) {
                // open current item if closed
                if (selection instanceof Folder
                    || selection instanceof Directory)
                {
                    if (uiTree.isCollapsed(path)) {
                        uiTree.expandPath(path);
                    }
                    if (path.getLastPathComponent() != getSelectedItem()) {
                        setSelectedTreePath(path);
                    }
                    FolderPanel folderPanel = getUIController()
                        .getInformationQuarter().getFolderPanel();
                    folderPanel.setTab(FolderPanel.FILES_TAB);
                }
            }
        }

        public void drop(DropTargetDropEvent dtde) {
            timeEntered = 0;
            if (Arrays.asList(dtde.getCurrentDataFlavors()).contains(
                DataFlavor.javaFileListFlavor))
            {

                // test if there is a directory to drop onto
                DirectoryPanel directoryPanel = getUIController()
                    .getInformationQuarter().getFolderPanel()
                    .getDirectoryPanel();
                Directory directory = directoryPanel.getDirectoryTable()
                    .getDirectory();
                if (directory != null) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    if (directoryPanel.drop(dtde.getTransferable())) {
                        dtde.dropComplete(true);
                    } else {
                        dtde.dropComplete(false);
                    }
                }
            }
            dtde.dropComplete(false);
        }

        public void dropActionChanged(DropTargetDragEvent dtde) {

        }

    }
}