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
 * $Id$
 */
package de.dal33t.powerfolder.message;

import java.io.IOException;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;

/**
 * Reply to a RequestFilePartsRecord message.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */
public class ReplyFilePartsRecord extends Message {
    private static final long serialVersionUID = 100L;

    private FileInfo file;
    private FilePartsRecord record;

    public ReplyFilePartsRecord() {
    }

    public ReplyFilePartsRecord(FileInfo file, FilePartsRecord record) {
        super();
        this.file = file;
        this.record = record;

        validate();
    }

    public FileInfo getFile() {
        return file;
    }

    public FilePartsRecord getRecord() {
        return record;
    }

    private void validate() {
        Reject.noNullElements(file, record);
    }

    private void readObject(java.io.ObjectInputStream stream)
        throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        validate();
    }
}
