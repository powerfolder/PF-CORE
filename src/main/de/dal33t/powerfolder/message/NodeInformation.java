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
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.MemberInfo;

/**
 * Node information
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class NodeInformation extends Message {
    private static final long serialVersionUID = 101L;

    public MemberInfo node;
    public String programVersion;
    public String debugReport;

    /**
     * Constructs a node information
     *
     * @param c
     */
    public NodeInformation(Controller c) {
        debugReport = c.getDebugReport();
        node = c.getMySelf().getInfo();
        programVersion = Controller.PROGRAM_VERSION;
    }

    public String toString() {
        return "Node information";
    }
}