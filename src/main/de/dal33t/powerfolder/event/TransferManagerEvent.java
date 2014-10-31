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
package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.TransferProblem;
import de.dal33t.powerfolder.transfer.Upload;

/**
 * Event fired by TransferManager
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class TransferManagerEvent extends EventObject {
    private Download download;
    private FileInfo file;
    private Upload upload;
    private TransferProblem transferProblem;
    private String problemInformation;

    public TransferManagerEvent(TransferManager source,
                                Download download,
                                TransferProblem transferProblem,
                                String problemInformation) {
        super(source);
        this.download = download;
        this.file = download.getFile();
        this.transferProblem = transferProblem;
        this.problemInformation = problemInformation;
    }

    public TransferManagerEvent(TransferManager source, Download download) {
        super(source);
        this.download = download;
        this.file = download.getFile();
    }

    public TransferManagerEvent(TransferManager source, Download download,
        FileInfo file)
    {
        super(source);
        this.download = download;
        this.file = file;
    }

    public TransferManagerEvent(TransferManager source, Upload upload) {
        super(source);
        this.upload = upload;
        this.file = upload.getFile();
    }

    public Download getDownload() {
        return download;
    }

    /**
     * Returns the affected file of upload/download event
     *
     * @return
     */
    public FileInfo getFile() {
        if (file != null) {
            return file;
        } else if (download != null) {
            return download.getFile();
        } else if (upload != null) {
            return upload.getFile();
        }
        // Unable to resolve
        return null;
    }

    public Upload getUpload() {
        return upload;
    }

    public TransferProblem getTransferProblem() {
        return transferProblem;
    }

    public String getProblemInformation() {
        return problemInformation;
    }
}