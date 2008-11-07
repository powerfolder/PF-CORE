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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.ScanResult.ResultState;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

import javax.swing.tree.MutableTreeNode;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;

/**
 * UI-Model for a folder. Prepare folder data in a "swing-compatible" way. E.g.
 * as <code>TreeNode</code> or <code>TableModel</code>.
 * <p>
 * TODO: rebuilt is called way too often.
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 3.1 $
 */
public class FolderModel extends PFUIComponent {
    private final static long DELAY = 500;

    private FolderRepositoryModel repoModel;

    /** The folder associated with this model. */
    private Folder folder;

    /** The ui node */
    private TreeNodeList treeNode;

    private MyFolderListener listener;

    private MyTimerTask task;

    /**
     * Constructor. Takes controller and the associated folder.
     * 
     * @param controller
     * @param repoModel
     * @param folder
     */
    public FolderModel(Controller controller, FolderRepositoryModel repoModel,
        Folder folder)
    {
        super(controller);
        Reject.ifNull(repoModel, "FolderRepo model can not be null");
        Reject.ifNull(folder, "Folder can not be null");
        this.repoModel = repoModel;
        this.folder = folder;
        initialize();
        listener = new MyFolderListener();
        folder.addFolderListener(listener);
        folder.addMembershipListener(listener);
    }

    /**
     * Initialize the tree node and rebuild.
     */
    private void initialize() {
        treeNode = new TreeNodeList(folder, getController().getUIController()
                .getApplicationModel().getFolderRepositoryModel()
                .getMyFoldersTreeNode());
        rebuild();
        repoModel.updateFolderTreeNode(this);
    }

    // Public API *************************************************************

    public Folder getFolder() {
        return folder;
    }

    /**
     * @return the treenode representation of this object.
     */
    public MutableTreeNode getTreeNode() {
        return treeNode;
    }

    void dispose() {
        folder.removeFolderListener(listener);
        folder.removeMembershipListener(listener);
    }

    public List<DirectoryModel> getSubdirectories() {
        List<DirectoryModel> list = new ArrayList<DirectoryModel>();
        int childs = treeNode.getChildCount();
        for (int i = 0; i < childs; i++) {
            list.add((DirectoryModel) treeNode.getChildAt(i));
        }
        return list;
    }

    // Private stuff **********************************************************

    private void scheduleRebuild() {
        if (task != null) {
            // Already scheduled.
            return;
        }
        task = new MyTimerTask();
        getController().schedule(task, DELAY);
    }

    /**
     * Build up the directory tree from the folder.
     * <p>
     * ATTENTION: Method not thread-safe. Has to be executed in EDT.
     */
    private void rebuild() {
        // Cleanup
        treeNode.removeAllChildren();
        Collection<Directory> folderSubdirectories = folder.getDirectory()
            .getSubDirectoriesAsCollection();
        for (Directory subdirectory : folderSubdirectories) {
            if (subdirectory.isDeleted()) {
                continue;
            }
            DirectoryModel directoryModel = new DirectoryModel(treeNode,
                subdirectory);
            buildSubDirectoryModels(directoryModel);
            treeNode.addChild(directoryModel);
        }
        treeNode.sortBy(new DirectoryModel.Comparator());
    }

    private static void buildSubDirectoryModels(DirectoryModel model) {
        Collection<Directory> subDirectories = model.getDirectory()
            .getSubDirectoriesAsCollection();
        for (Directory subDirectory : subDirectories) {
            if (subDirectory.isDeleted()) {
                // Skip
                continue;
            }
            DirectoryModel subdirectoryModel = new DirectoryModel(model,
                subDirectory);
            model.addChild(subdirectoryModel);
            buildSubDirectoryModels(subdirectoryModel);
        }
        model.sortSubDirectories();
    }

    private class MyFolderListener implements FolderListener,
        FolderMembershipListener
    {

        public boolean fireInEventDispathThread() {
            return false;
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
            scheduleRebuild();
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
            scheduleRebuild();
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
            scheduleRebuild();
        }

        public void scanResultCommited(FolderEvent folderEvent) {
            ScanResult sr = folderEvent.getScanResult();
            if (!sr.getResultState().equals(ResultState.SCANNED)) {
                return;
            }
            if (sr.isChangeDetected()) {
                scheduleRebuild();
            }
        }

        public void fileChanged(FolderEvent folderEvent) {
            scheduleRebuild();
        }

        public void filesDeleted(FolderEvent folderEvent) {
            scheduleRebuild();
        }

        public void memberJoined(FolderMembershipEvent folderEvent) {
            scheduleRebuild();
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            scheduleRebuild();
        }
    }

    private class MyTimerTask extends TimerTask {
        public void run() {
            // Move into EDT, otherwise another thread might change the
            // model during update of Tree.
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    rebuild();
                    repoModel.updateFolderTreeNode(FolderModel.this);
                    task = null;
                }
            });
        }
    }
}
