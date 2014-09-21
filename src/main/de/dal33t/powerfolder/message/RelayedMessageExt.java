/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: RelayedMessage.java 10350 2009-11-06 13:53:13Z tot $
 */
package de.dal33t.powerfolder.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.dal33t.powerfolder.light.MemberInfo;

/**
 * Ext version of {@link RelayedMessage}
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RelayedMessageExt extends RelayedMessage implements Externalizable
{
    private static final long serialVersionUID = 7437181683027517530L;
    private static final long extVersionUID = 100L;

    // For serialization
    public RelayedMessageExt() {
        super();
    }

    public RelayedMessageExt(Type type, MemberInfo source,
        MemberInfo destination, long connectionId, byte[] payload)
    {
        super(type, source, destination, connectionId, payload);
    }

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        long extUID = in.readLong();
        if (extUID != extVersionUID) {
            throw new InvalidClassException(this.getClass().getName(),
                "Unable to read. extVersionUID(steam): " + extUID
                    + ", expected: " + extVersionUID);
        }

        connectionId = in.readLong();
        type = Type.valueOf(in.readUTF());
        source = MemberInfo.readExt(in);
        destination = MemberInfo.readExt(in);

        if (in.readBoolean()) {
            int len = in.readInt();
            payload = new byte[len];
            int read = 0;
            while (read < len) {
                read += in.read(payload, read, payload.length - read);
            }
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(extVersionUID);

        out.writeLong(connectionId);
        out.writeUTF(type.name());
        source.writeExternal(out);
        destination.writeExternal(out);

        out.writeBoolean(payload != null);
        if (payload != null) {
            out.writeInt(payload.length);
            out.write(payload);
        }
    }
}
