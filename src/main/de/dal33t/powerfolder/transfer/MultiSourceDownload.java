package de.dal33t.powerfolder.transfer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileChunk;
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
    
    boolean isCompleted();
    
    FileInfo getFileInfo();
    
    File getTempFile();

    void readyForRequests(Download download);

    boolean hasSources();

    void setBroken();
}
