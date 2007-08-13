package de.dal33t.powerfolder.event;

/** Adapter class, when only a few methods are needed  */
public abstract class TransferAdapter implements TransferManagerListener {

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
    
    public void pendingDownloadEnqueud(TransferManagerEvent event) {
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

    public void transferProblem(TransferManagerEvent event) {
    }

    public void clearTransferProblems() {
    }


}
