/* $Id: Upload.java,v 1.13 2006/01/30 00:51:18 totmacherr Exp $
 */
package de.dal33t.powerfolder.transfer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.Util;

/**
 * Simple class for a scheduled Upload
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
public class Upload extends Transfer {
    private boolean aborted;
    private boolean completed;

    /**
     * Constructs a new uploads, package protected, can only be called by
     * transfermanager
     * 
     * @param manager
     * @param file
     * @param member
     */
    Upload(TransferManager manager, Member member, RequestDownload dl) {
        super(manager, (dl == null) ? null : dl.file, member);
        if (dl == null) {
            throw new NullPointerException("Download request is null");
        }
        if (dl.file == null) {
            throw new NullPointerException("File is null");
        }
        setStartOffset(dl.startOffset);
        aborted = false;
    }

    /**
     * Starts the upload in a own thread using the give transfer manager
     * 
     * @param tm
     */
    synchronized void start() {
        if (isStarted()) {
            log().warn("Upload already started. " + this);
            return;
        }

        // Mark upload as started
        setStarted();

        Runnable uploadPerfomer = new Runnable() {
            public void run() {
                try {
                    sendChunks();
                    getTransferManager().setCompleted(Upload.this);
                } catch (TransferException e) {
                    // TODO Inform other side
                    log().warn("Upload broken: " + Upload.this, e);
                    getTransferManager().setBroken(Upload.this);
                }
            }

            public String toString() {
                return "Upload " + getFile().getFilenameOnly() + " to "
                    + getPartner().getNick();
            }
        };

        // Perfom upload in threadpool
        getTransferManager().perfomUpload(uploadPerfomer);
    }

    /** @return true if transfer is completed */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Aborts this dl if currently transferrings
     */
    synchronized void abort() {
        log().verbose("Upload aborted: " + this);
        aborted = true;
    }

    /**
     * Shuts down this upload if currently active
     */
    void shutdown() {
        abort();
        super.shutdown();
    }

    public boolean isAborted() {
        return aborted;
    }

    /**
     * @return if this upload is broken
     */
    public boolean isBroken() {
        if (super.isBroken()) {
            return true;
        }

        if (!stillQueuedAtPartner()) {
            log().warn(
                "Upload broken because not enqued @ partner: queedAtPartner: "
                    + stillQueuedAtPartner()
                    + ", folder: "
                    + getFile()
                        .getFolder(getController().getFolderRepository())
                    + ", diskfile: "
                    + getFile().getDiskFile(
                        getController().getFolderRepository())
                    + ", last contime: " + getPartner().getLastConnectTime());
        }

        File diskFile = getFile().getDiskFile(
            getController().getFolderRepository());
        if (diskFile == null || !diskFile.exists()) {
            log().warn(
                "Upload broken because diskfile is not available, folder: "
                    + getFile()
                        .getFolder(getController().getFolderRepository())
                    + ", diskfile: " + diskFile + ", last contime: "
                    + getPartner().getLastConnectTime());
            return true;
        }

        return !stillQueuedAtPartner();
    }

    /*
     * General ****************************************************************
     */

    public int hashCode() {
        int hash = 0;
        if (getFile() != null) {
            hash += getFile().hashCode();
        }
        if (getPartner() != null) {
            hash += getPartner().hashCode();
        }
        return hash;
    }

    public boolean equals(Object o) {
        if (o instanceof Upload) {
            Upload other = (Upload) o;
            return Util.equals(this.getFile(), other.getFile())
                && Util.equals(this.getPartner(), other.getPartner());
        }

        return false;
    }

    public String toString() {
        String msg = getFile().toString() + " to '" + getPartner().getNick()
            + "'";
        if (getPartner().isOnLAN()) {
            msg += " (local-net)";
        }
        return msg;
    }

    /**
     * Transfers a file to a member.
     * 
     * @throws TransferException
     *             if something unexepected occoured.
     */
    private void sendChunks() throws TransferException {
        if (this == null) {
            throw new NullPointerException("Upload is null");
        }
        if (getPartner() == null) {
            throw new NullPointerException("Upload member is null");
        }
        if (getFile() == null) {
            throw new NullPointerException("Upload file is null");
        }

        if (isBroken()) {
            throw new TransferException("Upload broken: " + this);
        }

        Member member = getPartner();
        FileInfo theFile = getFile();

        // connection still alive ?
        if (!member.isConnected()) {
            throw new TransferException("Upload broken, member disconnected: "
                + this);
        }

        // TODO: check if member is in folder of file
        File f = theFile.getDiskFile(getController().getFolderRepository());
        if (f == null) {
            throw new TransferException(this + ": Myself not longer on "
                + theFile.getFolderInfo());
        }
        if (raf == null) {
            if (!f.exists()) {
                throw new TransferException(theFile
                    + " not found, download canceled. '" + f.getAbsolutePath()
                    + "'");
            }
            if (!f.canRead()) {
                throw new TransferException("Cannot read file. '"
                    + f.getAbsolutePath() + "'");
            }

            boolean lastModificationDataMismatch = Convert
                .convertToGlobalPrecision(f.lastModified()) != Convert
                .convertToGlobalPrecision(theFile.getModifiedDate().getTime());
            if (lastModificationDataMismatch) {
                throw new TransferException(
                    "Last modification date mismatch. '"
                        + f.getAbsolutePath()
                        + "': expected "
                        + Convert.convertToGlobalPrecision(theFile
                            .getModifiedDate().getTime()) + ", actual "
                        + Convert.convertToGlobalPrecision(f.lastModified()));
            }
        }
        log().info(
            "Upload started " + this + " starting at " + getStartOffset());
        long startTime = System.currentTimeMillis();

        try {
            if (f.length() == 0) {
                // Handle files with zero size.
                // Just send one empty FileChunk
                FileChunk chunk = new FileChunk(theFile, 0, new byte[]{});
                member.sendMessage(chunk);
            } else {
                // Handle usual files. size > 0

                // Chunk size
                int chunkSize = member.isOnLAN()
                    ? TransferManager.MAX_CHUNK_SIZE
                    : (int) getTransferManager().getAllowedUploadCPSForWAN();
                if (chunkSize == 0) {
                    chunkSize = TransferManager.MAX_CHUNK_SIZE;
                }
                // Keep care of maximum chunk size
                chunkSize = Math.min(chunkSize, TransferManager.MAX_CHUNK_SIZE);

                if (chunkSize <= 0) {
                    log().error("Illegal chunk size: " + chunkSize);
                }

                if (raf == null) {
                    raf = new RandomAccessFile(f, "r");
                }

                // InputStream fin = new BufferedInputStream(
                // new FileInputStream(f));
                // starting at offset
                // fin.skip(upload.getStartOffset());
                long offset = getStartOffset();

                byte[] buffer = new byte[chunkSize];
                int read;
                do {
                    if (isAborted()) {
                        throw new TransferException(
                            "Upload aborted: " + this);
                    }
                    if (isBroken()) {
                        throw new TransferException(
                            "Upload broken: " + this);
                    }

                    raf.seek(offset);
                    read = raf.read(buffer);
                    // read = fin.read(buffer);
                    if (read < 0) {
                        // stop ul
                        break;
                    }

                    byte[] data;

                    if (read == buffer.length) {
                        // Take buffer unchanged as data
                        data = buffer;
                    } else {
                        // We have read less bytes then our buffer, copy
                        // data
                        data = new byte[read];
                        System.arraycopy(buffer, 0, data, 0, read);
                    }

                    FileChunk chunk = new FileChunk(theFile, offset, data);
                    offset += data.length;

                    long start = System.currentTimeMillis();
                    member.sendMessage(chunk);
                    getCounter().chunkTransferred(chunk);
                    getTransferManager().getUploadCounter().chunkTransferred(
                        chunk);

                    if (logVerbose) {
                        // log().verbose(
                        // "Chunk, "
                        // + Format.NUMBER_FORMATS.format(chunkSize)
                        // + " bytes, uploaded in "
                        // + (System.currentTimeMillis() - start)
                        // + "ms to " + member.getNick());
                    }
                } while (read > 0);

                // fin.close();
            }

            long took = System.currentTimeMillis() - startTime;
            getTransferManager().logTransfer(false, took, theFile, member);

            // upload completed successfully
        } catch (FileNotFoundException e) {
            throw new TransferException("File not found to upload. " + theFile,
                e);
        } catch (IOException e) {
            throw new TransferException("Problem reading file. " + theFile, e);
        } catch (ConnectionException e) {
            throw new TransferException(
                "Connection problem to " + getPartner(), e);
        }
    }
}