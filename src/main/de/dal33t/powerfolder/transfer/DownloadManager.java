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
 * $Id: DownloadManager.java 7541 2009-03-31 12:27:02Z harry $
 */
package de.dal33t.powerfolder.transfer;

import java.nio.file.Path;
import java.util.Date;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.Transfer.State;
import de.dal33t.powerfolder.transfer.swarm.DownloadControl;
import de.dal33t.powerfolder.transfer.swarm.DownloadSourceHandler;
import de.dal33t.powerfolder.util.TransferCounter;

/**
 * @author Dennis "Bytekeeper" Waldherr
 */
public interface DownloadManager extends DownloadSourceHandler, DownloadControl
{
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
     * Returns the temporary file used.
     *
     * @return
     */
    Path getTempFile();

    /**
     * Returns true if there are sources left to download from.
     *
     * @return
     */
    boolean hasSources();

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
     * @return true if this manager is done, either by completing the file or by
     *         being aborted/broken
     */
    boolean isDone();

    boolean hasSource(Download download);

    Date getCompletedDate();
}
