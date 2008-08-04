package de.dal33t.powerfolder.transfer.swarm;

import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;

public interface DownloadSourceHandler {
    /**
     * Called if an uploader is ready for the downloader to send requests. This
     * is only the case if the uploader has enabled delta sync or part requests
     * - same for the local client.
     * 
     * @param download
     */
    void readyForRequests(Download source);

    /**
     * Called when a download received a file chunk.
     * 
     * @param download
     * @param chunk
     */
    void chunkReceived(Download source, FileChunk chunk);

    /**
     * Called when a download received a FilePartsRecord
     * 
     * @param download
     * @param record
     */
    void filePartsRecordReceived(Download source, FilePartsRecord record);
}
