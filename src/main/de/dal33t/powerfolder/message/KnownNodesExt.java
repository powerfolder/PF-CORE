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
 * $Id: KnownNodes.java 7109 2009-03-06 05:48:35Z tot $
 */
package de.dal33t.powerfolder.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.dal33t.powerfolder.light.MemberInfo;

/**
 * {@link Externalizable} version of {@link KnownNodes}
 */
public class KnownNodesExt extends KnownNodes implements Externalizable {
    private static final long serialVersionUID = -5509020009008761039L;
    private static final long extVersionUID = 100L;

    public KnownNodesExt() {
        super();
    }

    public KnownNodesExt(MemberInfo node) {
        super(node);
    }

    public KnownNodesExt(MemberInfo[] nodes) {
        super(nodes);
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

        if (in.readBoolean()) {
            int len = in.readInt();
            nodes = new MemberInfo[len];
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = MemberInfo.readExt(in);
            }
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(extVersionUID);

        out.writeBoolean(nodes != null);
        if (nodes != null) {
            out.writeInt(nodes.length);
            for (MemberInfo nodeInfo : nodes) {
                nodeInfo.writeExternal(out);
            }
        }
    }

}