/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.message;

import java.util.Date;

import de.dal33t.powerfolder.util.Format;

/**
 * Information about peers transferstatus
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.7 $
 */
public class TransferStatus extends Message {
    private static final long serialVersionUID = 100L;

    public Date time;

    public int activeUploads;
    public int queuedUploads;
    public double maxUploadCPS;
    public long currentUploadCPS;
    public long uploadedBytesTotal;

    public int activeDownloads;
    public int queuedDownloads;
    public int maxDownloads;
    public double maxDownloadCPS;
    public long currentDownloadCPS;
    public long downloadedBytesTotal;

    public TransferStatus() {
        time = new Date();
    }

    /**
     * TODO: Calculate this with the "real" maximum upload cps
     *
     * @return the available upload bandwidth.
     */
    public long getAvailbleUploadCPS() {
        return Math.max((long) maxUploadCPS - currentUploadCPS, 0);
    }

    public String toString() {
        return "Transfer status: DLs ( " + activeDownloads + " / "
            + queuedDownloads + " ) (" + Format.formatBytes(currentDownloadCPS)
            + "/s), ULs ( " + activeUploads + " / " + queuedUploads + " ) ("
            + Format.formatBytes(currentUploadCPS) + "/s, max: "
            + Format.formatBytes((long) maxUploadCPS) + "/s)";
    }
}