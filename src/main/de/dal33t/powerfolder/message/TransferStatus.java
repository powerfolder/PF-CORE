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

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.TransferStatusProto;
import de.dal33t.powerfolder.util.Format;

/**
 * Information about peers transferstatus
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.7 $
 */
public class TransferStatus extends Message implements D2DObject {
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

    @Override
    public String toString() {
        return "Transfer status: DLs ( " + activeDownloads + " / "
            + queuedDownloads + " ) (" + Format.formatBytes(currentDownloadCPS)
            + "/s), ULs ( " + activeUploads + " / " + queuedUploads + " ) ("
            + Format.formatBytes(currentUploadCPS) + "/s, max: "
            + Format.formatBytes((long) maxUploadCPS) + "/s)";
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void initFromD2D(AbstractMessage mesg) {
      if(mesg instanceof TransferStatusProto.TransferStatus) {
          TransferStatusProto.TransferStatus proto =
              (TransferStatusProto.TransferStatus)mesg;

          this.time = (-1 == proto.getTime()
              ? null
              : new Date(proto.getTime()));
          this.activeUploads        = proto.getActiveUploads();
          this.queuedUploads        = proto.getQueuedUploads();
          this.maxUploadCPS         = proto.getMaxUploadCps();
          this.currentUploadCPS     = proto.getCurrentUploadCps();
          this.uploadedBytesTotal   = proto.getUploadedBytesTotal();
          this.activeDownloads      = proto.getActiveDownloads();
          this.queuedDownloads      = proto.getQueuedDownloads();
          this.maxDownloads         = proto.getMaxDownloads();
          this.maxDownloadCPS       = proto.getMaxDownloadCps();
          this.currentDownloadCPS   = proto.getCurrentDownloadCps();
          this.downloadedBytesTotal = proto.getDownloadedBytesTotal();
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage toD2D() {
      TransferStatusProto.TransferStatus.Builder builder =
          TransferStatusProto.TransferStatus.newBuilder();

      builder.setClazzName(this.getClass().getSimpleName());

      builder.setTime(null == this.time ? -1 : this.time.getTime());
      builder.setActiveUploads(this.activeUploads);
      builder.setQueuedUploads(this.queuedUploads);
      builder.setMaxUploadCps(this.maxUploadCPS);
      builder.setCurrentUploadCps(this.currentUploadCPS);
      builder.setUploadedBytesTotal(this.uploadedBytesTotal);
      builder.setActiveDownloads(this.activeDownloads);
      builder.setQueuedDownloads(this.queuedDownloads);
      builder.setMaxDownloads(this.maxDownloads);
      builder.setMaxDownloadCps(this.maxDownloadCPS);
      builder.setCurrentUploadCps(this.currentUploadCPS);
      builder.setDownloadedBytesTotal(this.downloadedBytesTotal);

      return builder.build();
    }
}