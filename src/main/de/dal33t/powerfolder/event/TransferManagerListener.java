/* $Id: TransferManagerListener.java,v 1.4 2006/02/06 23:24:07 totmacherr Exp $
 */
package de.dal33t.powerfolder.event;


/**
 * A Listener interface for the transfer manager.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public interface TransferManagerListener extends CoreListener {

    // Download listening
    public void downloadRequested(TransferManagerEvent event);
    
    public void downloadQueued(TransferManagerEvent event);

    public void downloadStarted(TransferManagerEvent event);

    public void downloadAborted(TransferManagerEvent event);

    public void downloadBroken(TransferManagerEvent event);

    public void downloadCompleted(TransferManagerEvent event);
    
    public void completedDownloadRemoved(TransferManagerEvent event);
    
    public void pendingDownloadEnqueud(TransferManagerEvent event);

    // Upload listening
    public void uploadRequested(TransferManagerEvent event);

    public void uploadStarted(TransferManagerEvent event);
    
    public void uploadAborted(TransferManagerEvent event);

    public void uploadBroken(TransferManagerEvent event);

    public void uploadCompleted(TransferManagerEvent event);
}