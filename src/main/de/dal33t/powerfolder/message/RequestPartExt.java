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
 * $Id: RequestPart.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.Range;

public class RequestPartExt extends RequestPart implements Externalizable {
    private static final long serialVersionUID = 2643953297593509510L;
        
    public RequestPartExt() {
    }

    public RequestPartExt(FileInfo file, Range range, double progress) {
        super(file, range, progress);
    }

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        file = FileInfoFactory.readExt(in);
        range = Range.getRangeByLength(in.readLong(), in.readLong());
        progress = in.readDouble();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        file.writeExternal(out);
        out.writeLong(range.getStart());
        out.writeLong(range.getLength());
        out.writeDouble(progress);
    }

}
