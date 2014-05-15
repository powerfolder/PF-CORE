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
 * $Id: FileChunk.java 10015 2009-10-13 14:05:22Z harry $
 */
package de.dal33t.powerfolder.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;

/**
 * A file chunk, part of a upload / donwload
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class FileChunkExt extends FileChunk implements Externalizable {

    public FileChunkExt() {
        super();
    }

    public FileChunkExt(FileInfo file, long offset, byte[] data) {
        super(file, offset, data);
    }

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        file = FileInfoFactory.readExt(in);
        offset = in.readLong();
        int length = in.readInt();
        data = new byte[length];
        int read = in.read(data);
        while (read < length) {
            read += in.read(data, read, data.length - read);
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        file.writeExternal(out);
        out.writeLong(offset);
        out.writeInt(data.length);
        out.write(data);
    }

}