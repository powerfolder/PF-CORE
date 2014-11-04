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
package de.dal33t.powerfolder.net;

import de.dal33t.powerfolder.light.MemberInfo;

/**
 * Interface defines a callback to something that can decide if a node should be
 * added to the internal database.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface NodeFilter {
    /**
     * Answers if this node should be added to the internal node database.
     *
     * @param nodeInfo
     *            the node to be added
     * @return true if this node should be added to the internal node database.
     */
    boolean shouldAddNode(MemberInfo nodeInfo);
}
