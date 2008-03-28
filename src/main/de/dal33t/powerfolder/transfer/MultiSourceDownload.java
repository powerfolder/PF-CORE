package de.dal33t.powerfolder.transfer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;

/**
 * @author Dennis "Bytekeeper" Waldherr
 *
 */
public interface MultiSourceDownload {
    /**
     * Returns a collection containing all sources of this swarm.
     * Any changes to the returned collection are <b>not</b> reflected in the actual list. 
     * @return
     */
    Collection<Download> getSources();
    
    /**
     * Called when a new download source is available.
     * @param download
     */
    void addSource(Download download);
    
    /**
     * Called when a download stops being available as a source.
     * @param download
     */
    void removeSource(Download download);
    
    /**
     * Called when a download received a FilePartsRecord
     * @param download
     * @param record
     */
    void receivedFilePartsRecord(Download download, FilePartsRecord record);
    
    /**
     * Called when a download received a file chunk.
     * @param download
     * @param chunk
     */
    void receivedChunk(Download download, FileChunk chunk) throws IOException;
    
    /**
     * Returns the download belonging to the given member.
     * @param member the download of the member or null if there isn't one
     * @return
     */
    Download getSourceFor(Member member);
    
    /**
     * Returns true if this download is using part requests (and therefore allows more than one source).
     * @return
     */
    boolean isUsingPartRequests();
    
    /**
     * Returns true if the download has been completed.
     * @return
     */
    boolean isCompleted();
    
    /**
     * Returns the FileInfo that belongs to the file being downloaded.
     * @return
     */
    FileInfo getFileInfo();
    
    /**
     * Returns the temporary file used.
     * @return
     */
    File getTempFile();

    /**
     * Called if an uploader is ready for the downloader to send requests.
     * This is only the case if the uploader has enabled delta sync or part requests - same for the local client.
     * @param download
     */
    void readyForRequests(Download download);

    /**
     * Returns true if there are sources left to download from.
     * @return
     */
    boolean hasSources();

    /**
     * Called if the download should be set to broken. 
     */
    void setBroken();

    /**
     * Called if the download should be aborted and any temporary file used be deleted. 
     */
    void abortAndCleanup();

    /**
     * Returns true if the download has started.
     * @return
     */
    boolean isStarted();

    /**
     * Called if the download should be aborted. 
     */
    void abort();

    /**
     * Returns the TransferCounter.
     * @return
     */
    TransferCounter getCounter();
}
