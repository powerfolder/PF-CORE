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
* $Id: MultisourceDownload.java 6135 2008-12-24 08:04:17Z harry $
*/
package de.dal33t.powerfolder.ui.information.downloads;

import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.TransferProblem;
import de.dal33t.powerfolder.transfer.Transfer;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.Member;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class MultisourceDownload {

    private List<Download> downloads;
    private final FileInfo fileInfo;

    public MultisourceDownload(Download download) {
        downloads = new CopyOnWriteArrayList<Download>();
        downloads.add(download);
        fileInfo = download.getFile();
    }

    public List<Download> getDownloads() {
        return Collections.unmodifiableList(downloads);
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void addDownload(Download dl) {
        for (Download download : downloads) {
            if (dl.getPartner().equals(download.getPartner())) {
                // Already know this one
                return;
            }
        }
        downloads.add(dl);
    }

    public List<Member> getPartners() {
        List<Member> partners = new ArrayList<Member>();
        for (Download download : downloads) {
            partners.add(download.getPartner());
        }
        return partners;
    }

    public boolean isCompleted() {
        for (Download download : downloads) {
            if (download.isCompleted()) {
                return true;
            }
        }
        return false;
    }

    public boolean isStarted() {
        for (Download download : downloads) {
            if (download.isStarted()) {
                return true;
            }
        }
        return false;
    }

    public boolean isBroken() {
        for (Download download : downloads) {
            if (download.isBroken()) {
                return true;
            }
        }
        return false;
    }

    public boolean isPending() {
        for (Download download : downloads) {
            if (download.isPending()) {
                return true;
            }
        }
        return false;
    }

    public boolean isQueued() {
        for (Download download : downloads) {
            if (download.isQueued()) {
                return true;
            }
        }
        return false;
    }

    public void clearCompletedDownloads(TransferManager transferManager) {
        for (Download download : downloads) {
            if (download.isCompleted()) {
                transferManager.clearCompletedDownload(download.getDownloadManager());
            }
        }
    }

    public void abort() {
        for (Download download : downloads) {
            if (download.isCompleted()) {
                download.abort();
            }
        }
    }

    public double getStateProgress() {
        double progress = 0;
        for (Download download : downloads) {
            if (download.getStateProgress() > progress) {
                progress = download.getStateProgress();
            }
        }
        return progress;
    }

    public TransferProblem getTransferProblem() {
        for (Download download : downloads) {
            TransferProblem transferProblem = download.getTransferProblem();
            if (transferProblem != null) {
                return transferProblem;
            }
        }
        return null;
    }

    public String getProblemInformation() {
        for (Download download : downloads) {
            String information = download.getProblemInformation();
            if (information != null && information.length() > 0) {
                return information;
            }
        }
        return null;
    }

    public Transfer.TransferState getState() {
        Transfer.TransferState state = Transfer.TransferState.NONE;
        for (Download download : downloads) {
            Transfer.TransferState state1 = download.getState();
            if (state1.equals(Transfer.TransferState.DONE)) {
                state = Transfer.TransferState.DONE;
            } else if (state1.equals(Transfer.TransferState.DOWNLOADING)
                    && !state.equals(Transfer.TransferState.DONE)) {
                state = Transfer.TransferState.DOWNLOADING;
            }

            // @todo complete state list, returning the most complete

        }
        return state;
    }

    public long calculateEstimatedMillisToCompletion() {
        long est = 0;
        for (Download download : downloads) {
            long emc = download.getCounter().calculateEstimatedMillisToCompletion();
            if (emc > est) {
                est = emc;
            }
        }
        return est;
    }

    public double calculateCurrentKBS() {
        double kps = 0.0;
        for (Download download : downloads) {
            kps += download.getCounter().calculateCurrentKBS();
        }
        return kps;
    }

    public boolean isForDownload(Download download) {
        for (Download download1 : downloads) {
            if (download.equals(download1)) {
                return true;
            }
        }
        return false;
    }
}
