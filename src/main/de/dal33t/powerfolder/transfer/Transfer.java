/* $Id: Transfer.java,v 1.14 2006/02/06 23:24:16 totmacherr Exp $
 */
package de.dal33t.powerfolder.transfer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.Date;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Loggable;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.TransferCounter;

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

    protected final State transferState = new State();

    public enum TransferState {
        NONE("None"), FILERECORD_REQUEST("transfers.requested"), MATCHING(
            "transfers.hashing"), DOWNLOADING("Downloading"), VERIFYING(
            "transfers.verifying"), DONE("transfers.completed"), REMOTEMATCHING(
            "transfers.remotehashing"), UPLOADING("Uploading"), FILEHASHING(
            "transfers.hashing"), COPYING("transfers.copying");

        private String translationId;

        TransferState(String key) {
            translationId = key;
        }

        public String getTranslationId() {
            return translationId;
        }
    }

    public static class State implements Serializable {
        private static final long serialVersionUID = 100L;

        private TransferState state = TransferState.NONE;
        private double progress = -1;

        public synchronized TransferState getState() {
            return state;
        }

        public synchronized void setState(TransferState state) {
            if (!this.state.equals(state)) {
                this.progress = -1;
                this.state = state;
            }
        }

        /**
         * Sets the progress of the current state (0 till 1). Values < 0
         * indicate that no measurement is possible
         * 
         * @param progress
         */
        public synchronized void setProgress(double progress) {
            Reject.ifTrue(progress > 1, "Process set to illegal value: "
                + progress);
            this.progress = progress;
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
    public void init(TransferManager aTransferManager) {
        if (transferManager != null) {
            log().error(
                "Unable to set TransferManager. Having already one. " + this);
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
            transferState.setProgress(1);
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
     * @return
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
        transferState.setState(TransferState.DONE);
        transferState.setProgress(1);
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
     * @return
     */
    public boolean isStarted() {
        return startTime != null;
    }

    /**
     * Sets this transfer as started
     */
    protected void setStarted() {
        if (startTime != null) {
            throw new IllegalStateException("Transfer already started");
        }
        startTime = new Date();

        // Inform transfer manager
        getTransferManager().setStarted(this);
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
     * @return
     */
    public long getStartOffset() {
        return startOffset;
    }

    /**
     * @return if this download is completed
     */
    public boolean isCompleted() {
        return transferState != null && transferState.getState() != null
            && transferState.getState().equals(TransferState.DONE);
    }

    /**
     * Returns the transfer counter
     * 
     * @return
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
            log().warn("Abort cause: partner is null.");
            return true;
        }
        if (!getPartner().isCompleteyConnected()) {
            log().warn(
                "Abort cause: " + getPartner().getNick() + " not connected.");
            return true;
        }
        boolean partnerOnFolder = stillPartnerOnFolder();
        if (!partnerOnFolder) {
            // broken if partner left folder
            log().warn(
                "Abort cause: " + getPartner().getNick() + " not on folder.");
            return true;
        }

        return false;
    }

    /**
     * @return if this transfer is still queued at the remote side. if not this
     *         transfer should be set broken
     */
    protected boolean stillQueuedAtPartner() {
        // FIXME: Find a better way to determine queued status
        if (getPartner() == null) {
            return false;
        }
        boolean stillQueuedAtPartner = getPartner().isConnected();
        if (stillQueuedAtPartner && getPartner().getLastConnectTime() != null) {
            // if peer has reconnected in the meanwhile.
            // we need to break this dl, and re-request it
            stillQueuedAtPartner = getPartner().getLastConnectTime().before(
                getInitTime());
        }
        return stillQueuedAtPartner;
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

    public TransferState getState() {
        return transferState == null ? null : transferState.getState();
    }

    public double getStateProgress() {
        return transferState == null ? 0 : transferState.getProgress();
    }

    // General ****************************************************************

    @Override
    public String getLoggerName() {
        return getClass().getSimpleName() + " '" + getFile().getFilenameOnly()
            + "'";
    }
}