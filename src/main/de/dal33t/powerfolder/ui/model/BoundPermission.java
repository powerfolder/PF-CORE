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
 * $Id: MainFrame.java 11813 2010-03-20 03:20:21Z harry $
 */
package de.dal33t.powerfolder.ui.model;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.security.Permission;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.Reject;

/**
 * Generic helper to check if a permission is set / changes.
 * <p>
 * Does only check permission if
 * {@link ConfigurationEntry#SECURITY_PERMISSIONS_STRICT} is set to true.
 * Otherwise always calls hasPermission(true)
 *
 * @author sprajc
 */
public abstract class BoundPermission extends PFComponent {

    private ServerClientListener listener;
    private Permission permission;
    private boolean hasPermission;

    // Construction / Destruction *********************************************

    public BoundPermission(Controller controller, Permission permission) {
        super(controller);
        Reject.ifNull(permission, "Permission");
        this.permission = permission;
        // Hold original listener. Should only be GCed when the BoundPermission
        // object gets collected - NOT earlier.
        this.listener = new MyServerClientListener();
        getController().getOSClient().addWeakListener(this.listener);
        getController().schedule(new Runnable() {
            public void run() {
                checkPermission(true);
            }
        }, 0);
    }

    public Permission getPermission() {
        return permission;
    }

    // Abstract behavior ******************************************************

    /**
     * Called in EDT if the permission actual changed. Called ONCE on
     * construction to set initial value.
     *
     * @param hasPermission
     */
    public abstract void hasPermission(boolean hasPermission);

    // Internal helper ********************************************************

    private synchronized void checkPermission(boolean initial) {
        if (!ConfigurationEntry.SECURITY_PERMISSIONS_STRICT
            .getValueBoolean(getController()))
        {
            // Not using this.
            return;
        }

        boolean hadPermission = hasPermission;

        // Alternative thru security manager.
        // AccountInfo aInfo = getController().getOSClient().getAccountInfo();
        // hasPermission = getController().getSecurityManager().hasPermission(
        // aInfo, permission);

        // Faster:
        hasPermission = getController().getOSClient().getAccount()
            .hasPermission(permission);
        boolean changed = hasPermission != hadPermission;
        if (changed || initial) {
            // Prevent unwanted while sitting in EDT queue.
            final boolean thisHasPermission = hasPermission;
            UIUtil.invokeLaterInEDT(new Runnable() {
                public void run() {
                    hasPermission(thisHasPermission);
                }
            });
        }
    }

    private final class MyServerClientListener implements ServerClientListener {
        public boolean fireInEventDispatchThread() {
            return false;
        }

        public void serverDisconnected(ServerClientEvent event) {
        }

        public void serverConnected(ServerClientEvent event) {
        }

        public void login(ServerClientEvent event) {
            getController().schedule(new Runnable() {
                public void run() {
                    checkPermission(false);
                }
            }, 0);

        }

        public void accountUpdated(ServerClientEvent event) {
            getController().schedule(new Runnable() {
                public void run() {
                    checkPermission(false);
                }
            }, 0);
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
        }
    }

    public void dispose() {
        getController().getOSClient().removeListener(listener);
    }

    @Override
    public String toString() {
        return "BoundPermission [permission=" + permission + "]";
    }
}
