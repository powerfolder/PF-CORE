package de.dal33t.powerfolder.clientserver;

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
 * Rationale: Execute a request on a remote node.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
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

    public Response execute(Request request) throws ConnectionException {
        if (!node.isCompleteyConnected()) {
            throw new ConnectionException(
                "Unable to execute request, node is disconnected (" + node
                    + ").");
        }

        // Prepare
        response = null;
        requestId = request.getRequestId();

        if (logVerbose) {
            log().verbose(
                "Sending request to " + node.getNick() + " (" + requestId
                    + "): " + request);
        }
        // Listen to receive the response
        getController().getNodeManager().addNodeManagerListener(discoListener);
        node.addMessageListener(messageListener);
        node.sendMessagesAsynchron(request);

        // 60 secs timeout
        waitForResponse(60);

        if (response == null) {
            throw new ConnectionException(
                "Did not receive a response after timeout or node disconnected ("
                    + node + ").");
        }

        if (logVerbose) {
            log().verbose(
                "Response from " + node.getNick() + " (" + requestId + "): "
                    + response);
        }

        notifyAndcleanup();
        return response;
    }

    // Internal helper ********************************************************

    private void waitForResponse(long seconds) throws ConnectionException {
        synchronized (waitForResponseLock) {
            try {
                waitForResponseLock.wait(seconds * 1000);
            } catch (InterruptedException e) {
                throw new ConnectionException(
                    "Interrupted while waiting for response (" + node + ").", e);
            }
        }
    }

    private void notifyAndcleanup() {
        if (logVerbose) {
            log().verbose("Cleanup of request: " + requestId);
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
            notifyAndcleanup();
        }

        @Override
        public void nodeDisconnected(NodeManagerEvent e) {
            if (!e.getNode().equals(node)) {
                return;
            }
            // Break request
            notifyAndcleanup();
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
    }
}
