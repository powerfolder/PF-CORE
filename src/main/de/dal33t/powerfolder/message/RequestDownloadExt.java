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
 * $Id: RequestDownload.java 6972 2009-02-09 07:26:06Z tot $
 */
package de.dal33t.powerfolder.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;

/**
 * Request to start download a file
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class RequestDownloadExt extends RequestDownload implements
    Externalizable
{
    private static final long serialVersionUID = -6907422071162117250L;

    public RequestDownloadExt() {
        super();
    }

    public RequestDownloadExt(FileInfo file, long startOffset) {
        super(file, startOffset);
    }

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        file = FileInfoFactory.readExt(in);
        startOffset = in.readLong();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        file.writeExternal(out);
        out.writeLong(startOffset);
    }

}