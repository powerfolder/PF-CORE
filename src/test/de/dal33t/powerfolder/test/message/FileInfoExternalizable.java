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
* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.test.message;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;

/**
 * File information of a local or remote file
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.33 $
 */
public class FileInfoExternalizable implements Externalizable {

    /** The filename (including the path from the base of the folder) */
    public String fileName;

    /** The size of the file */
    public Long size;

    /** modified info */
    public String modifiedBy;
    /** modified in folder on date */
    public Date lastModifiedDate;

    /** Version number of this file */
    public int version;

    /** the deleted flag */
    public boolean deleted;

    /** the folder */
    public String folderInfo;

    // Serialization optimization *********************************************

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        fileName = in.readUTF();
        size = Long.valueOf(in.readLong());
        modifiedBy = in.readUTF();
        lastModifiedDate = (Date) in.readObject();
        version = in.readInt();
        deleted = in.readBoolean();
        folderInfo = in.readUTF();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(fileName);
        out.writeLong(size.longValue());
        out.writeUTF(modifiedBy);
        out.writeObject(lastModifiedDate);
        out.writeInt(version);
        out.writeBoolean(deleted);
        out.writeUTF(folderInfo);
    }
}