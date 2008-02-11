package de.dal33t.powerfolder.net;

import java.util.Collection;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.RelayedMessage;
import de.dal33t.powerfolder.message.RelayedMessage.Type;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Waiter;

/**
 * Listens for incoming relayed messages and
 * <p>
 * 1) Processes it if destination = myself. = Let RelayedConHandler of Member
 * process the message.
 * <p>
 * 2) Send the message to the destination if connected.
 * <p>
 * TRAC #597.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RelayedConnectionManager extends PFComponent {
    private static long nextConnectionId = 0;

    /**
     * Connection handler that are in pending state. Pending means there is a
     * ConnectionHanlder which is not yet connected with it's member (node).
     */
    private Collection<AbstractRelayedConnectionHandler> pendingConHans;
    private TransferCounter counter;
    private boolean printStats;
    private long nRelayedMsgs;
    private boolean tringToConnect;

    public RelayedConnectionManager(Controller controller) {
        super(controller);
        pendingConHans = new CopyOnWriteArrayList<AbstractRelayedConnectionHandler>();
        counter = new TransferCounter();
        printStats = false;
    }

    public void start() {
        getController().scheduleAndRepeat(new RelayConnectTask(), 1000L * 20,
            1000L * 20);
    }

    /**
     * Creates and initializes a relayed channel via a relay. The returned
     * ConnectionHandler is in init state.
     * 
     * @param destination
     *            the remote destination to connect to
     * @return
     * @throws ConnectionException
     */
    public ConnectionHandler initRelayedConnectionHandler(MemberInfo destination)
        throws ConnectionException
    {
        if (getController().getMySelf().getInfo().equals(destination)) {
            throw new ConnectionException(
                "Illegal relayed loopback connection detection to myself");
        }
        Member relay = getRelay();
        if (relay == null) {
            throw new ConnectionException(
                "Unable to open relayed connection to " + destination
                    + ". No relay found!");
        }
        log().warn("Using relay " + relay);

        log().warn("Sending SYN for relayed connection to " + destination);
        long connectionId;
        synchronized (RelayedConnectionManager.class) {
            connectionId = nextConnectionId++;
        }
        RelayedMessage synMsg = new RelayedMessage(Type.SYN, getController()
            .getMySelf().getInfo(), destination, connectionId, null);
        relay.sendMessage(synMsg);

        AbstractRelayedConnectionHandler relHan = getController()
            .getIOProvider()
            .getConnectionHandlerFactory()
            .constructRelayedConnectionHandler(destination, connectionId, relay);

        pendingConHans.add(relHan);
        if (pendingConHans.size() > 20) {
            log().error(
                pendingConHans.size()
                    + " PENDING RELAYED CONNECTION HANDLERS found!");
        }

        waitForAck(relHan);
        relHan.init();
        return relHan;
    }

    /**
     * Callback from <code>AbstractRelayedConnectionHandler</code> to inform,
     * that the handler is not longer pending (=on shutdown or assigend to his
     * <code>Member</code>).
     * 
     * @param conHan
     */
    public void removePedingRelayedConnectionHandler(
        AbstractRelayedConnectionHandler conHan)
    {
        Reject.ifNull(conHan, "ConnectionHandler is null");
        pendingConHans.remove(conHan);
    }

    /**
     * Callback method from <code>Member</code>.
     * 
     * @param receivedFrom
     *            the node/relay which relayed the message
     * @param message
     *            the message
     */
    public void handleRelayedMessage(final Member receivedFrom,
        final RelayedMessage message)
    {
        if (getController().getMySelf().getInfo().equals(
            message.getDestination()))
        {
            // This is a message for US!
            processMessageForMySelf(receivedFrom, message);
        } else {
            // Route message to destination member if possible.
            relayMessage(receivedFrom, message);
        }
    }

    /**
     * @return the relaying node or null if no relay found
     */
    public Member getRelay() {
        if (getController().getNodeManager() == null) {
            log().warn("Not getting relay, NodeManager not created yet");
            return null;
        }
        for (Member node : getController().getNodeManager().getConnectedNodes())
        {
            if (isRelay(node.getInfo())) {
                return node;
            }
        }
        return null;
    }

    public boolean isRelay(MemberInfo node) {
        return node.id.toUpperCase().contains("INFRASTRUCTURE")
            || node.id.toUpperCase().contains("RELAY");
    }

    public TransferCounter getTransferCounter() {
        return counter;
    }

    // Internal ***************************************************************

    private void relayMessage(final Member receivedFrom,
        final RelayedMessage message)
    {
        Member destinationMember = message.getDestination().getNode(
            getController(), true);
        if (!destinationMember.isCompleteyConnected()) {
            RelayedMessage eofMsg = new RelayedMessage(Type.NACK, message
                .getDestination(), message.getSource(), message
                .getConnectionId(), null);
            receivedFrom.sendMessagesAsynchron(eofMsg);
            log().warn(
                "Unable to deliver relayed message to " + destinationMember
                    + ". " + message);
            return;
        }
        log().warn(
            "Relaying msg to " + destinationMember.getNick() + ". msg: "
                + message);

        if (!printStats) {
            printStats = true;
            log().warn(
                "Acting as relay. Received from " + receivedFrom.getNick()
                    + ", msg: " + message);
            getController().scheduleAndRepeat(new TimerTask() {
                @Override
                public void run() {
                    log().warn(
                        "Relayed Con Stats: " + nRelayedMsgs
                            + " msgs relayed: " + counter);
                }
            }, 5000);
        }

        try {
            destinationMember.sendMessage(message);
            if (message.getPayload() != null) {
                counter.bytesTransferred(message.getPayload().length);
                nRelayedMsgs++;
            }
        } catch (ConnectionException e) {
            log().warn(
                "Connection broken while relaying message to "
                    + destinationMember.getNick(), e);
            RelayedMessage eofMsg = new RelayedMessage(Type.EOF, message
                .getDestination(), message.getSource(), message
                .getConnectionId(), null);
            receivedFrom.sendMessagesAsynchron(eofMsg);
        }
    }

    private void processMessageForMySelf(final Member receivedFrom,
        final RelayedMessage message)
    {

        // Deliver to RelayedConnectionHanlder of Remote member
        AbstractRelayedConnectionHandler peer = resolveRelHan(message);

        switch (message.getType()) {
            case SYN :
                log().warn("SYN received from " + message.getSource());
                final AbstractRelayedConnectionHandler relHan = getController()
                    .getIOProvider().getConnectionHandlerFactory()
                    .constructRelayedConnectionHandler(message.getSource(),
                        message.getConnectionId(), receivedFrom);

                pendingConHans.add(relHan);
                Runnable acceptor = new Runnable() {
                    public void run() {
                        try {
                            log().warn("Sending ACK to " + message.getSource());
                            RelayedMessage ackMsg = new RelayedMessage(
                                Type.ACK,
                                getController().getMySelf().getInfo(), message
                                    .getSource(), relHan.getConnectionId(),
                                null);
                            receivedFrom.sendMessagesAsynchron(ackMsg);

                            relHan.init();
                            getController().getNodeManager().acceptConnection(
                                relHan);
                        } catch (ConnectionException e) {
                            relHan.shutdown();
                            log().error(
                                "Unable to accept connection: " + relHan + ". "
                                    + e.toString(), e);
                            RelayedMessage eofMsg = new RelayedMessage(
                                Type.NACK, getController().getMySelf()
                                    .getInfo(), message.getDestination(),
                                relHan.getConnectionId(), null);
                            receivedFrom.sendMessagesAsynchron(eofMsg);
                        } finally {
                            pendingConHans.remove(relHan);
                        }
                    }
                };
                getController().getIOProvider().startIO(acceptor);
                return;
            case ACK :
                log().warn("ACK received. taget " + message.getSource());
                if (peer != null) {
                    peer.setAckReceived(true);
                }
                return;
            case NACK :
                log().warn("NACK received target " + message.getSource());
                if (peer != null) {
                    peer.shutdownWithMember();
                    removePedingRelayedConnectionHandler(peer);
                }
                return;
            case EOF :
                log().warn("EOF received from " + message.getSource());
                if (peer != null) {
                    peer.shutdownWithMember();
                    removePedingRelayedConnectionHandler(peer);
                }
                return;
        }

        Reject.ifFalse(message.getType().equals(Type.DATA_ZIPPED),
            "Only zipped data allowed");

        // if (!sourceMember.isCompleteyConnected()) {
        // log()
        // .warn("Relayed connection was shutdown to " + sourceMember);
        // RelayedMessage eofMsg = new RelayedMessage(Type.EOF,
        // getController().getMySelf().getInfo(), message.getSource(),
        // null);
        // receivedFrom.sendMessagesAsynchron(eofMsg);
        // }

        if (peer == null) {
            log().warn(
                "Got unknown peer, while processing relayed message: " + peer);
            // RelayedMessage eofMsg = new RelayedMessage(Type.EOF,
            // getController().getMySelf().getInfo(), message.getSource(),
            // null);
            // receivedFrom.sendMessagesAsynchron(eofMsg);
            return;
        }

        // Actual relay of message
        peer.receiveRelayedMessage(message);
    }

    private AbstractRelayedConnectionHandler resolveRelHan(
        RelayedMessage message)
    {
        Member sourceMember = message.getSource()
            .getNode(getController(), true);
        ConnectionHandler peer = sourceMember.getPeer();

        if (peer == null) {
            // Search in pending con handlers
            for (AbstractRelayedConnectionHandler relHel : pendingConHans) {
                if (relHel.getRemote().equals(message.getSource())) {
                    // FOund in pending!
                    peer = relHel;
                    break;
                }
            }
        }

        if (peer instanceof AbstractRelayedConnectionHandler) {
            AbstractRelayedConnectionHandler relCon = (AbstractRelayedConnectionHandler) peer;
            if (relCon.getConnectionId() == message.getConnectionId()) {
                return relCon;
            }
            log().error(
                "Connection ID mismatch. got " + message.getConnectionId()
                    + ", expected " + relCon.getConnectionId());
        }
        return null;
    }

    private void waitForAck(AbstractRelayedConnectionHandler relHan)
        throws ConnectionException
    {
        Waiter waiter = new Waiter(60L * 1000L);
        log().warn("Waiting for ack on " + relHan);
        while (!waiter.isTimeout()) {
            if (relHan.isAckReceived()) {
                log().warn("Got ack on " + relHan);
                return;
            }
            waiter.waitABit();
        }
        if (!relHan.isAckReceived()) {
            throw new ConnectionException(
                "Did not receive a ack after 60s from " + relHan);
        }
    }

    // Internal classes *******************************************************

    private class RelayConnectTask extends TimerTask {
        @Override
        public void run() {
            if (getRelay() != null) {
                return;
            }
            if (isRelay(getController().getMySelf().getInfo())) {
                return;
            }
            if (tringToConnect) {
                return;
            }
            if (getController().isLanOnly()) {
                return;
            }
            if (!ConfigurationEntry.AUTO_CONNECT
                .getValueBoolean(getController()))
            {
                return;
            }
            tringToConnect = true;
            Runnable connector = new Runnable() {
                public void run() {
                    log().debug("Triing to connect to a Relay");
                    for (Member canidate : getController().getNodeManager()
                        .getNodesAsCollection())
                    {
                        if (canidate.isCompleteyConnected()) {
                            continue;
                        }
                        if (!isRelay(canidate.getInfo())) {
                            continue;
                        }

                        try {
                            log().warn(
                                "Triing to connect to relay: " + canidate);
                            if (canidate.reconnect()) {
                                break;
                            }
                        } catch (ConnectionException e) {
                            log().warn(
                                "Unable to connect to relay: " + canidate, e);
                        }
                    }
                    tringToConnect = false;
                }
            };
            getController().getIOProvider().startIO(connector);
        }
    }
}
