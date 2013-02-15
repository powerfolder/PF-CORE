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
package de.dal33t.powerfolder.event;

/** Adapter class, when only a few methods are needed  */
public abstract class TransferManagerAdapter implements TransferManagerListener {

    public void downloadRequested(TransferManagerEvent event) {
    }
    
    public void downloadQueued(TransferManagerEvent event) {
    }

    public void downloadStarted(TransferManagerEvent event) {
    }

    public void downloadAborted(TransferManagerEvent event) {
    }

    public void downloadBroken(TransferManagerEvent event) {
    }

    public void downloadCompleted(TransferManagerEvent event) {
    }
    
    public void completedDownloadRemoved(TransferManagerEvent event) {
    }
    
    public void pendingDownloadEnqueued(TransferManagerEvent event) {
    }

    public void uploadRequested(TransferManagerEvent event) {
    }

    public void uploadStarted(TransferManagerEvent event) {
    }

    public void uploadAborted(TransferManagerEvent event) {
    }

    public void uploadBroken(TransferManagerEvent event) {
    }

    public void uploadCompleted(TransferManagerEvent event) {
    }

    public void completedUploadRemoved(TransferManagerEvent event) {
    }


}
