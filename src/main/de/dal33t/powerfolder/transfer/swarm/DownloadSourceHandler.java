/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
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
