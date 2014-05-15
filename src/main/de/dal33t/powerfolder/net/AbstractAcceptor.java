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
 * $Id: Controller.java 6331 2009-01-03 18:40:42Z tot $
 */
package de.dal33t.powerfolder.net;

import java.util.Date;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Profiling;
import de.dal33t.powerfolder.util.ProfilingEntry;

/**
 * An acceptor is a task to fully handshake a new incoming connection.
 *
 * @author Christian Sprajc
 * @version $Revision$
 */
public abstract class AbstractAcceptor extends PFComponent implements Runnable {
    private Date startTime;
    private ConnectionHandler handler;

    protected AbstractAcceptor(Controller controller) {
        super(controller);
    }

    /**
     * @return information about the connection, e.g. remote address
     */
    public abstract String getConnectionInfo();

    /**
     * Shuts the acceptor down and closes the socket
     */
    protected abstract void shutdown();

    /**
     * Overriding behavior. Performs the actual work. If Exception occurs the
     * acceptor gets shut down.
     */
    protected abstract void accept() throws ConnectionException;

    /**
     * @return if this acceptor has a timeout
     */
    boolean hasTimeout() {
        if (startTime == null) {
            // Not started yet
            return false;
        }

        Date lastKeepAlive = handler != null ? handler
            .getLastKeepaliveMessageTime() : null;
        if (lastKeepAlive == null) {
            // No handler/We cannot check the last keepalive message.
            return System.currentTimeMillis() > startTime.getTime()
                + (1000L * Constants.CONNECTION_KEEP_ALIVE_TIMOUT);
        }
        // Check if the last keepalive timeout
        return System.currentTimeMillis() > lastKeepAlive.getTime()
            + (1000L * Constants.CONNECTION_KEEP_ALIVE_TIMOUT);
    }

    boolean isShutdown() {
        return handler != null && !handler.isConnected();
    }

    public final void run() {
        if (!getController().getNodeManager().isStarted()) {
            logWarning("NodeManager already shut down. "
                + "Closing incoming connection attempt: " + this);
            return;
        }
        ProfilingEntry pe = Profiling.start();
        startTime = new Date();
        try {
            accept();
        } catch (ConnectionException e) {
            logFiner("Unable to accept incoming connection handler " + this, e);
            shutdown();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof InterruptedException) {
                logFiner("Acceptor interrupted, closing. " + this);
                shutdown();
            } else {
                throw e;
            }
        } finally {
            // Remove from acceptors list
            getController().getNodeManager().acceptors.remove(this);
            Profiling.end(pe);
        }
    }

    /**
     * Convenience method to accept the new Connection Handler.
     *
     * @param handler
     */
    protected void acceptConnection(ConnectionHandler handler) {
        try {
            this.handler = handler;
            getController().getNodeManager().acceptConnection(handler);
        } catch (ConnectionException e) {
            logFiner("Unable to accept incoming connection handler " + handler,
                e);
            handler.shutdown();
            shutdown();
            // Remove from acceptors list
            getController().getNodeManager().acceptors.remove(this);
        }
    }

    public String toString() {
        return getClass().getSimpleName() + ":" + getConnectionInfo();
    }
}
