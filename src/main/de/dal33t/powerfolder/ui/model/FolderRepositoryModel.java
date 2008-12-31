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
package de.dal33t.powerfolder.ui.model;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;
import de.dal33t.powerfolder.util.Reject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Prepares core data as (swing) ui models. e.g. <code>TreeModel</code>
 * <p>
 * 
 * @see de.dal33t.powerfolder.disk.FolderRepository
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderRepositoryModel extends PFUIComponent {

    private FoldersTableModel myFoldersTableModel;

    private final ValueModel hidePreviewsVM;

    private Map<Folder, FolderModel> folderModelMap = Collections
        .synchronizedMap(new HashMap<Folder, FolderModel>());

    public FolderRepositoryModel(Controller controller) {
        super(controller);

        // Table model initalization
        myFoldersTableModel = new FoldersTableModel(getController()
            .getFolderRepository(), getController());

        hidePreviewsVM = new ValueHolder();
        hidePreviewsVM.setValue(Boolean.FALSE);
        hidePreviewsVM.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                ConfigurationEntry.HIDE_PREVIEW_FOLDERS.setValue(
                    getController(), Boolean.valueOf(
                        (Boolean) evt.getNewValue()).toString());
                getController().saveConfig();
                folderStructureChanged();
                getMyFoldersTableModel().folderStructureChanged();
            }
        });

        // Register listener
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());

    }

    // Inizalization **********************************************************

    /**
     * Initalizes the listener on the core components and sets an inital state
     * of the repo model. Adds all folder models as childs
     */
    public void initalize() {
        // Add inital model state
        Folder[] folders = getController().getFolderRepository().getFolders();
        for (Folder folder : folders) {
            if (!hideFolder(folder)) {
                FolderModel folderModel = new FolderModel(getController(),
                    this, folder);
                folderModelMap.put(folder, folderModel);
//                myFoldersTreeNode.addChild(folderModel.getTreeNode());
            }
        }

        Runnable runner = new Runnable() {
            public void run() {
                expandFolderRepository();
            }
        };
        getUIController().invokeLater(runner);
    }

    // Exposing ***************************************************************

    public ValueModel getHidePreviewsValueModel() {
        return hidePreviewsVM;
    }

    public boolean isHidePreviews() {
        return (Boolean) hidePreviewsVM.getValue();
    }

    /**
     * @return a tablemodel containing my folders
     */
    public FoldersTableModel getMyFoldersTableModel() {
        return myFoldersTableModel;
    }

    // Helper methods *********************************************************

    /**
     * @param folder
     * @return a folder model
     */
    public FolderModel locateFolderModel(Folder folder) {
        Reject.ifNull(folder, "Folder is required");
        return folderModelMap.get(folder);
    }

    /**
     * Expands the folder repository, only done once
     */
    private void expandFolderRepository() {
//        if (myFoldersTreeNode.getChildCount() > 0 && !expandedMyFolders) {
//            logFiner("Expanding foined folders on nav_tree");
//            // Expand joined folders
//            // getController().getUIController().getControlQuarter().getUITree()
//            // .expandPath(myFoldersTreeNode.getPathTo());
//            expandedMyFolders = true;
//        }
    }

    /**
     * Synchronizes a folder.
     */
    public void syncFolder(FolderInfo folderInfo) {
        Folder folder = getController().getFolderRepository().getFolder(folderInfo);

        if (SyncProfile.MANUAL_SYNCHRONIZATION.equals(folder.getSyncProfile()))
        {
            // Ask for more sync options on that folder if on project sync
            new SyncFolderPanel(getController(), folder).open();
        } else {

            // Let other nodes scan now!
            folder.broadcastScanCommand();

            // Recommend scan on this
            folder.recommendScanOnNextMaintenance();

            // Now trigger the scan
            getController().getFolderRepository().triggerMaintenance();

            // Trigger file requesting.
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting(folderInfo);
        }
    }

    /**
     * Add or remove folder models from the tree depending on the folder preview
     * state.
     */
    public void folderStructureChanged() {
        Folder[] folders = getController().getFolderRepository().getFolders();
        for (Folder folder : folders) {
            FolderModel folderModel = locateFolderModel(folder);
            if (folderModel != null) {
//                if (hideFolder(folder)
//                    && myFoldersTreeNode.contains(folderModel.getTreeNode()))
//                {
//                    removeFolder(folder, this);
//                } else if (!hideFolder(folder)
//                    && !myFoldersTreeNode.contains(folderModel.getTreeNode()))
//                {
//                    addFolder(folder, this, false);
//                }
            }
        }
    }

    /** update Folder treenode for a folder */
    void updateFolderTreeNode(FolderModel folderModel) {
        // Update tree on that folder
        if (isFiner()) {
            logFiner("Updating files of folder " + folderModel.getFolder());
        }
//        TreeNodeList list = myFoldersTreeNode;
//        Object[] path = new Object[]{navTreeModel.getRoot(), list,
//            folderModel.getTreeNode()};
//        TreeModelEvent te = new TreeModelEvent(this, path);
//        navTreeModel.fireTreeStructureChanged(te);
    }

    // Internal code **********************************************************

    /**
     * Listens for changes in the folder repository. Changes the prepared model
     * and fires the appropiate events on the swing models.
     */
    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {

        public void folderRemoved(FolderRepositoryEvent e) {
            Folder folder = e.getFolder();
            FolderModel folderModel = locateFolderModel(folder);
//            if (folderModel == null
//                || !myFoldersTreeNode.contains(folderModel.getTreeNode()))
//            {
//                return;
//            }

            removeFolder(folder, e.getSource());
        }

        public void folderCreated(FolderRepositoryEvent e) {
            Folder folder = e.getFolder();
            FolderModel folderModel = locateFolderModel(folder);
//            if (folderModel != null
//                && myFoldersTreeNode.contains(folderModel.getTreeNode()))
//            {
//                // Already have this one
//                return;
//            }

            if (!hideFolder(folder)) {
                addFolder(folder, e.getSource(), true);
            }
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            updateFolderTreeNode(e);
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            updateFolderTreeNode(e);
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

        private void updateFolderTreeNode(FolderRepositoryEvent event) {
            Folder folder = event.getFolder();
            if (folder == null) {
                return;
            }
            FolderModel folderModel = locateFolderModel(folder);
//            if (folderModel == null
//                || !myFoldersTreeNode.contains(folderModel.getTreeNode()))
//            {
//                return;
//            }
            boolean maintenance = folder.equals(getController()
                .getFolderRepository().getCurrentlyMaintainingFolder());
            if (folder.isTransferring() || folder.isScanning() || maintenance) {
                getUIController().getBlinkManager().addChatBlinking(folder,
                    Icons.FOLDER);
            } else {
                getUIController().getBlinkManager().removeBlinking(folder);
            }
        }
    }

    /**
     * Only show folders if not preview or show preview config is true.
     * 
     * @param folder
     * @return
     */
    private boolean hideFolder(Folder folder) {
        return folder.isPreviewOnly()
            && ConfigurationEntry.HIDE_PREVIEW_FOLDERS
                .getValueBoolean(getController());
    }

    private void removeFolder(Folder folder, Object eventSource) {
//        TreeNodeList tnl = myFoldersTreeNode;
//        FolderModel folderModel = locateFolderModel(folder);
//        if (folderModel != null) {
//            myFoldersTreeNode.remove(folderModel.getTreeNode());
//            folderModel.dispose();
//
//            // Fire tree model event
//            TreeModelEvent te = new TreeModelEvent(eventSource, tnl.getPathTo());
//            navTreeModel.fireTreeStructureChanged(te);
//        }
//
//        // Select my folders
//        getUIController().getControlQuarter().setSelected(tnl);
    }

    private void addFolder(Folder folder, Object eventSource, boolean select) {
//        FolderModel folderModel = new FolderModel(getController(), this, folder);
//        folderModelMap.put(folder, folderModel);
//        myFoldersTreeNode.addChild(folderModel.getTreeNode());
//        // Fire tree model event
//        TreeModelEvent te = new TreeModelEvent(eventSource, myFoldersTreeNode
//            .getPathTo());
//        navTreeModel.fireTreeStructureChanged(te);
//
//        expandFolderRepository();
//
        if (select) {
            // Select folder

        }
    }

}
