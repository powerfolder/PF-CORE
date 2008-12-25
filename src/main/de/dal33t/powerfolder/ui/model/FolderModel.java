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
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.ScanResult.ResultState;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.util.Reject;

import java.awt.*;
import java.util.TimerTask;

/**
 * UI-Model for a folder. Prepare folder data in a "swing-compatible" way. E.g.
 * as <code>TreeNode</code> or <code>TableModel</code>.
 * <p>
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 3.1 $
 */
public class FolderModel extends PFUIComponent {
    private static final long DELAY = 500;

    private FolderRepositoryModel repoModel;

    /** The folder associated with this model. */
    private Folder folder;

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
        repoModel.updateFolderTreeNode(this);
    }

    // Public API *************************************************************

    public Folder getFolder() {
        return folder;
    }

    void dispose() {
        folder.removeFolderListener(listener);
        folder.removeMembershipListener(listener);
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

    private class MyFolderListener implements FolderListener,
        FolderMembershipListener
    {

        public boolean fireInEventDispatchThread() {
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
                    repoModel.updateFolderTreeNode(FolderModel.this);
                    task = null;
                }
            });
        }
    }
}
