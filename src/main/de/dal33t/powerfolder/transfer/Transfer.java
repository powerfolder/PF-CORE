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
package de.dal33t.powerfolder.transfer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Date;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * Abstract version of a Transfer.<BR>
 * Serializable for remembering completed Downloads in DownLoadTableModel.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.14 $
 */
public abstract class Transfer extends Loggable implements Serializable {

    private static final long serialVersionUID = 100L;

    private transient TransferManager transferManager;
    private transient Member partner;
    private MemberInfo partnerInfo;

    private FileInfo file;
    private Date startTime;
    // time where this transfer was initialized
    private Date initTime;
    private long startOffset;
    private TransferCounter counter;

    // Details of the latest transfer problem.
    private TransferProblem transferProblem;
    private String problemInformation;

    protected final State state = new State();

    public enum TransferState {
        NONE("None", 10), // NONE WHAT DO YOU THINK?
        FILERECORD_REQUEST("transfers.requested", 20), // DOWNLOAD
        FILEHASHING("transfers.hashing", 30), // UPLOAD only
        REMOTEMATCHING("transfers.remote_hashing", 40), // UPLOAD only
        DOWNLOADING("Downloading", 50), // DOWNLOAD only
        UPLOADING("Uploading", 60), // UPLOAD only
        MATCHING("transfers.hashing", 70), // DOWNLOAD ONLY
        COPYING("transfers.copying", 80), // DOWNLOAD ONLY
        VERIFYING("transfers.verifying", 90), // DOWNLOAD ONLY
        DONE("transfers.completed", 100);

        private int orderIndex;

        public int getOrderIndex() {
            return orderIndex;
        }

        private String translationId;

        TransferState(String key, int orderIndex) {
            this.translationId = key;
            this.orderIndex = orderIndex;
        }

        public String getTranslationId() {
            return translationId;
        }

    }

    public static class State implements Serializable, Comparable<State> {
        private static final long serialVersionUID = 100L;

        private TransferState state = TransferState.NONE;
        private double progress = -1;
        private Date completedDate;

        public synchronized TransferState getState() {
            return state;
        }

        public synchronized void setState(TransferState state) {
            if (!this.state.equals(state)) {
                progress = -1;
                this.state = state;
                if (state.equals(TransferState.DONE)) {
                    completedDate = new Date();
                }
            }
        }

        /**
         * Sets the progress of the current state (0 till 1). Values < 0
         * indicate that no measurement is possible
         *
         * @param progress
         */
        public synchronized void setProgress(double progress) {
            if (progress > 1) {
                this.progress = 1;
            } else {
                this.progress = progress;
            }
        }

        /**
         * Gets the progress of the current state (0 till 1). Values < 0
         * indicate that no measurement is possible
         *
         * @return the progress in percentage or a value < 0 if that's not
         *         possible
         */
        public synchronized double getProgress() {
            return progress;
        }

        /**
         * @return the date the transfer completed.
         */
        public Date getCompletedDate() {
            return completedDate;
        }

        public int compareTo(State o) {
            int comp = Integer.valueOf(state.orderIndex).compareTo(
                o.state.orderIndex);
            if (comp == 0) {
                // Same state. Compare by progress
                comp = Double.valueOf(progress).compareTo(o.progress);
            }
            return comp;
        }
    }

    /** for Serialization */
    public Transfer() {

    }

    /**
     * Initializes a new Transfer
     *
     * @param transferManager
     * @param file
     * @param partner
     */
    protected Transfer(TransferManager transferManager, FileInfo file,
        Member partner)
    {
        Reject.ifNull(transferManager, "TransferManager is null");
        Reject.ifNull(file, "FileInfo is null");
        this.transferManager = transferManager;
        this.file = file;
        setPartner(partner);
        this.initTime = new Date();
    }

    /**
     * Re-initalized the Transfer with the TransferManager. Use this only if you
     * are know what you are doing.
     *
     * @param aTransferManager
     */
    void init(TransferManager aTransferManager) {
        if (transferManager != null) {
            logSevere("Unable to set TransferManager. Having already one. "
                + this);
            return;
        }
        this.transferManager = aTransferManager;
        if (this.partnerInfo != null) {
            this.partner = this.partnerInfo.getNode(getController(), true);
        } else {
            this.partner = null;
        }
        this.startTime = null;

        // FIX for #878
        if (isCompleted()) {
            state.setProgress(1);
        }
    }

    /**
     * @return the file.
     */
    public FileInfo getFile() {
        return file;
    }

    /**
     * maybe null if loaded from serialized file
     *
     * @return the partner
     */
    public final Member getPartner() {
        if (partner == null && partnerInfo != null) {
            return partnerInfo.getNode(getController(), true);
        }
        return partner;
    }

    /**
     * Sets the parter for this download
     *
     * @param aPartner
     */
    protected final void setPartner(Member aPartner) {
        this.partner = aPartner;
        if (partner != null) {
            this.partnerInfo = aPartner.getInfo();
        } else {
            this.partnerInfo = null;
        }
    }

    void shutdown() {
    }

    void setCompleted() {
        // Set final state.
        state.setState(TransferState.DONE);
        state.setProgress(1);
    }

    /**
     * @return the time of the transfer start
     */
    public final Date getStartTime() {
        return startTime;
    }

    /**
     * Answers if this transfer has already started
     *
     * @return true if started
     */
    public boolean isStarted() {
        return startTime != null;
    }

    /**
     * Sets this transfer as started
     */
    protected synchronized void setStarted() {
        boolean wasStarted = isStarted();
        if (!wasStarted) {
            // Start now
            startTime = new Date();
            getTransferManager().setStarted(this);
        } else {
            logWarning("Got already started transfer", new RuntimeException(
                "from here"));
        }
    }

    protected TransferManager getTransferManager() {
        return transferManager;
    }

    protected Controller getController() {
        return transferManager.getController();
    }

    /**
     * @return the time when the transfer was initalized/constructed
     */
    protected Date getInitTime() {
        return initTime;
    }

    /**
     * Sets the startoffset for this transfer
     *
     * @param startOffset
     */
    protected void setStartOffset(long startOffset) {
        this.startOffset = startOffset;
    }

    /**
     * @return the start offset
     */
    public long getStartOffset() {
        return startOffset;
    }

    /**
     * @return if this download is completed
     */
    public boolean isCompleted() {
        return state != null && state.getState() != null
            && state.getState().equals(TransferState.DONE);
    }

    public Date getCompletedDate() {
        return state != null && state.getCompletedDate() != null ? state
            .getCompletedDate() : null;
    }

    /**
     * @return the transfer counter
     */
    public TransferCounter getCounter() {
        if (counter == null) {
            counter = new TransferCounter(getStartOffset(), getFile().getSize());
        }
        return counter;
    }

    /**
     * @return if this transfer is broken, override, but call super
     */
    public boolean isBroken() {
        if (isCompleted()) {
            // The transfer is not broken
            return false;
        }
        if (getPartner() == null) {
            if (isFine()) {
                logFine("Break cause: partner is null.");
            }
            return true;
        }
        if (!getPartner().isCompletelyConnected()) {
            if (isFine()) {
                logFine("Break cause: " + getPartner().getNick()
                    + " not connected.");
            }
            return true;
        }
        boolean partnerOnFolder = stillPartnerOnFolder();
        if (!partnerOnFolder) {
            // broken if partner left folder
            if (isFine()) {
                logFine("Break cause: " + getPartner().getNick()
                    + " not on folder.");
            }
            return true;
        }

        // Not done here, may cause too much CPU.
        // #2532: Done in TransferManager.checkActiveTranfersForExcludes();
//        Folder folder = getFile().getFolder(
//            getController().getFolderRepository());
//        if (folder != null) {
//            if (folder.getDiskItemFilter().isExcluded(getFile())) {
//                return true;
//            }
//        }

        return false;
    }

    /**
     * @return if this transfer is still queued at the remote side. if not this
     *         transfer should be set broken
     */
    protected boolean stillQueuedAtPartner() {
        if (getPartner() == null) {
            return false;
        }
        return getPartner().isConnected();
    }

    /**
     * @return if the partner still on the destination folder of file
     */
    protected boolean stillPartnerOnFolder() {
        Folder folder = getFile().getFolder(
            getController().getFolderRepository());
        if (folder == null) {
            // folder not found
            return false;
        }
        return folder.hasMember(getPartner());
    }

    /**
     * Gets the latest transfer problem.
     *
     * @return the latest transfer problem
     */
    public TransferProblem getTransferProblem() {
        return transferProblem;
    }

    /**
     * Sets the latest transfer problem.
     *
     * @param transferProblem
     *            the transfer problem
     */
    public void setTransferProblem(TransferProblem transferProblem) {
        this.transferProblem = transferProblem;
    }

    /**
     * Gets additional information about the latest transfer problem.
     *
     * @return the latest transfer problem information
     */
    public String getProblemInformation() {
        return problemInformation;
    }

    /**
     * Sets additional information about the latest transfer problem.
     *
     * @param problemInformation
     *            the latest transfer problem information
     */
    public void setProblemInformation(String problemInformation) {
        this.problemInformation = problemInformation;
    }

    public State getState() {
        return state;
    }

    public TransferState getTransferState() {
        return state == null ? null : state.getState();
    }

    public double getStateProgress() {
        return state == null ? 0 : state.getProgress();
    }

    //
    // // General
    // ****************************************************************
    //
    // @Override
    // public String getLoggerName() {
    // return getClass().getSimpleName() + " '" +
    // getRelativeFile().getFilenameOnly()
    // + "'";
    // }

    private void readObject(ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();
        this.partnerInfo = partnerInfo != null ? partnerInfo.intern() : null;
    }
}