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
package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.message.clientserver.Request;
import de.dal33t.powerfolder.message.clientserver.Response;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.Reject;

/**
 * Performs a basic request - response message cycle.
 * <p>
 * Rationale: Executes a request on a remote node.
 * <p>
 * Is thread safe. Can execute only one request.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RequestExecutor extends PFComponent {

    private Object waitForResponseLock = new Object();
    private Member node;

    private DisconnectListener discoListener;
    private ResponseMessageListener messageListener;
    private String requestId;
    private Response response;

    public RequestExecutor(Controller controller, Member node) {
        super(controller);
        Reject.ifNull(node, "Node is null");
        this.node = node;
        this.discoListener = new DisconnectListener();
        this.messageListener = new ResponseMessageListener();
    }

    public synchronized Response execute(Request request)
        throws ConnectionException
    {
        if (!node.isConnected()) {
            throw new ConnectionException("Not connected to " + node.getNick());
        }

        // Prepare
        response = null;
        requestId = request.getRequestId();

        if (isFiner()) {
            logFiner("Sending request to " + node.getNick() + " (" + requestId
                + "): " + request);
        }
        // Listen to receive the response
        getController().getNodeManager().addNodeManagerListener(discoListener);
        node.addMessageListener(messageListener);
        node.sendMessage(request);

        try {
            waitForResponse(Constants.REQUEST_RESPONSE_TIMEOUT);

            if (response == null) {
                if (!node.isConnected()) {
                    throw new ConnectionException(node.getNick()
                        + " disconnected");
                }
                throw new ConnectionException("Timeout to " + node.getNick());
            }

            if (isFiner()) {
                logFiner("Response from " + node.getNick() + " (" + requestId
                    + "): " + response + " to " + request);
            }
        } finally {
            notifyAndcleanup();
        }

        return response;
    }

    // Internal helper ********************************************************

    private void waitForResponse(long seconds) {
        synchronized (waitForResponseLock) {
            if (response != null) {
                // Already got response
                return;
            }
            try {
                waitForResponseLock.wait(1000L * seconds);
            } catch (InterruptedException e) {
                logWarning("Interrupted while waiting for response (" + node
                    + "): " + e);
                logFiner("InterruptedException", e);
            }
        }
    }

    private void notifyAndcleanup() {
        if (isFiner()) {
            logFiner("Cleanup of request: " + requestId);
        }
        synchronized (waitForResponseLock) {
            waitForResponseLock.notifyAll();
        }
        node.removeMessageListener(messageListener);
        getController().getNodeManager().removeNodeManagerListener(
            discoListener);
    }

    private class DisconnectListener extends NodeManagerAdapter {
        @Override
        public void nodeConnected(NodeManagerEvent e) {
            if (!e.getNode().equals(node)) {
                return;
            }
            // Break request
            // notifyAndcleanup();
        }

        @Override
        public void nodeDisconnected(NodeManagerEvent e) {
            if (!e.getNode().equals(node)) {
                return;
            }
            // Break request
            notifyAndcleanup();
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }

    private class ResponseMessageListener implements MessageListener {

        public void handleMessage(Member source, Message message) {
            if (!source.equals(node)) {
                return;
            }
            if (!(message instanceof Response)) {
                return;
            }
            Response canidate = (Response) message;
            if (requestId.equals(canidate.requestId)) {
                response = canidate;
                notifyAndcleanup();
            }
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }
}
