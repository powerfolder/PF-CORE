/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: SecurityManager.java 8728 2009-07-26 03:29:38Z tot $
 */
package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.ListenerSupportFactory;

/**
 * The security manager for the client.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public abstract class AbstractSecurityManager extends PFComponent implements
    SecurityManager
{
    private SecurityManagerListener listners;

    public AbstractSecurityManager(Controller controller) {
        super(controller);
        listners = ListenerSupportFactory
            .createListenerSupport(SecurityManagerListener.class);
    }

    // Event handling *********************************************************

    protected void fireNodeAccountStateChanged(Member node) {
        listners.nodeAccountStateChanged(new SecurityManagerEvent(node));
    }

    public void addListener(SecurityManagerListener listner) {
        ListenerSupportFactory.addListener(listners, listner);
    }

    public void removeListener(SecurityManagerListener listner) {
        ListenerSupportFactory.removeListener(listners, listner);
    }
}
