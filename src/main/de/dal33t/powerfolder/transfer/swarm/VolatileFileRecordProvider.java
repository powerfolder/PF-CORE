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
 * $Id: AbstractDownloadManager.java 5151 2008-09-04 21:50:35Z bytekeeper $
 */
package de.dal33t.powerfolder.transfer.swarm;

import java.io.IOException;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.ProgressListener;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;

/**
 * This {@link FileRecordProvider} never caches or remembers
 * {@link FilePartsRecord}s. Every request performs a new computation of the
 * record.
 *
 * @author Dennis "Bytekeeper" Waldherr
 */
public class VolatileFileRecordProvider extends AbstractFileRecordProvider {

    public VolatileFileRecordProvider(Controller controller) {
        super(controller);
    }

    public FilePartsRecord retrieveRecord(FileInfo fileInfo,
        ProgressListener obs) throws IOException
    {
        Reject.ifNull(fileInfo, "FileInfo is null!");
        return computeFilePartsRecord(fileInfo, obs);
    }

    public void shutdown() {
    }
}
