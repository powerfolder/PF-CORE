/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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

import java.io.File;
import java.util.Collection;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.transfer.Transfer.State;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;

/**
 * @author Dennis "Bytekeeper" Waldherr
 */
public interface DownloadManager {
    /**
     * Called if the download should be aborted.
     */
    void abort();

    /**
     * Aborts the download and deletes any temporary file used.
     */
    void abortAndCleanup();

    /**
     * Called when a new download source is available. If the given download is
     * completed, this manager should set itself to completed as well and not
     * transfer anything.
     * 
     * @param download
     */
    void addSource(Download download);

    /**
     * Returns true if a source using that member is allowed.
     * 
     * @param member
     * @return
     */
    boolean allowsSourceFor(Member member);

    /**
     * Returns the TransferCounter.
     * 
     * @return
     */
    TransferCounter getCounter();

    /**
     * @return the state of the transfer
     */
    State getState();

    /**
     * Returns the FileInfo that belongs to the file being downloaded.
     * 
     * @return
     */
    FileInfo getFileInfo();

    /**
     * Returns the download belonging to the given member.
     * 
     * @param member
     *            the download of the member or null if there isn't one
     * @return
     */
    Download getSourceFor(Member member);

    /**
     * Returns a collection containing all sources of this swarm. Any changes to
     * the returned collection are <b>not</b> reflected in the actual list.
     * 
     * @return
     */
    Collection<Download> getSources();

    /**
     * Returns the temporary file used.
     * 
     * @return
     */
    File getTempFile();

    /**
     * Returns true if there are sources left to download from.
     * 
     * @return
     */
    boolean hasSources();

    /**
     * Returns true if this download is broken.
     * 
     * @return
     */
    boolean isBroken();

    /**
     * Returns true if the download has been completed.
     * 
     * @return
     */
    boolean isCompleted();

    /**
     * Returns true if this download was requested automatically.
     * 
     * @return
     */
    boolean isRequestedAutomatic();

    /**
     * Returns true if the download has started.
     * 
     * @return
     */
    boolean isStarted();

    /**
     * Called if an uploader is ready for the downloader to send requests. This
     * is only the case if the uploader has enabled delta sync or part requests
     * - same for the local client.
     * 
     * @param download
     */
    void readyForRequests(Download download);

    /**
     * Called when a download received a file chunk.
     * 
     * @param download
     * @param chunk
     */
    void receivedChunk(Download download, FileChunk chunk);

    /**
     * Called when a download received a FilePartsRecord
     * 
     * @param download
     * @param record
     */
    void receivedFilePartsRecord(Download download, FilePartsRecord record);

    /**
     * Called when a download stops being available as a source.
     * 
     * @param download
     */
    void removeSource(Download download);

    /**
     * @return true if this manager is done, either by completing the file or by
     *         being aborted/broken
     */
    boolean isDone();

    void broken(String reason);

}
