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

    protected FileInfo file;
    private Date startTime;
    // time where this transfer was initialized
    private Date initTime;
    private long startOffset;
    private TransferCounter counter;
    
    protected transient RandomAccessFile raf;

    /** for Serialization */
    public Transfer() {

    }
    
    /** for compare reasons only */
    public Transfer(FileInfo fileInfo) {
        this.file = fileInfo;
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
        if (transferManager == null) {
            throw new NullPointerException("TransferManager is null");
        }
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        this.transferManager = transferManager;
        this.file = file;
        setPartner(partner);
        this.initTime = new Date();
    }

    /**
     * Re-initalized the Transfer with the TransferManager. Use this only if you
     * are know what you are doing .
     * 
     * @param transferManager
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
    }

    /**
     * @return
     */
    public final FileInfo getFile() {
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
        if (this.partner != null) {
//            log().error(
//                "Overwriting old partner of transfer: " + partner.getNick()
//                    + ". " + this);
        }
        this.partner = aPartner;
        if (partner != null) {
            this.partnerInfo = aPartner.getInfo();
        } else {
            this.partnerInfo = null;
        }
    }

    
    void shutdown() {
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException e) {
                log().warn("Failed to close transfer file on abort!, e");           
            }
        }
    }
    
    void setCompleted() {
        // Make sure the file is closed
    	if (raf != null) {
    		try {
				raf.close();
			} catch (IOException e) {
				log().warn("Failes to close transfer file!", e);
			}
    	}
        
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
     * indicates if this transfer is completed
     * 
     * @return true if transfer is completed else false
     */
    public abstract boolean isCompleted();

    /**
     * Returns the transfer counter
     * 
     * @return
     */
    public TransferCounter getCounter() {
        if (counter == null) {
            counter = new TransferCounter(getStartOffset(), file.getSize());
        }
        return counter;
    }

    /**
     * Answers if this transfer is broken, override, but call super
     * 
     * @return
     */
    public boolean isBroken() {
        if (isCompleted()) {
            // The transfer is not broken
            return false;
        }
        // broken if partner left folder
        return !stillPartnerOnFolder();
    }

    /**
     * Answers if this transfer is still queued at the remote side. if not this
     * transfer should be set broken
     * 
     * @return
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
     * Answers if the partner still on the destination folder of file
     * 
     * @return
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
}