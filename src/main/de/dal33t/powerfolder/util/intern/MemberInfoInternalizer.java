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
 * $Id: NodeManager.java 12576 2010-06-14 14:28:23Z tot $
 */
package de.dal33t.powerfolder.util.intern;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.util.Reject;

/**
 * To internalize MemberInfos against a {@link NodeManager}s database.
 *
 * @author sprajc
 */
public class MemberInfoInternalizer implements Internalizer<MemberInfo> {
    private NodeManager nodeManager;

    private volatile int hits;
    private volatile int misses;

    public MemberInfoInternalizer(NodeManager nodeManager) {
        super();
        Reject.ifNull(nodeManager, "NodeManager");
        this.nodeManager = nodeManager;
    }

    public MemberInfo intern(MemberInfo item) {
        if (item == null) {
            return null;
        }
        Member node = nodeManager.getNode(item);
        if (node != null) {
            hits++;
            return node.getInfo();
        } else {
            hits++;
            return nodeManager.addNode(item).getInfo();
        }
    }
}
