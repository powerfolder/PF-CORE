/* $Id$
 */
package de.dal33t.powerfolder.net;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.message.Ping;
import de.dal33t.powerfolder.util.NamedThreadFactory;
import de.dal33t.powerfolder.util.Reject;

/**
 * Provides basic IO stuff.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class IOProvider extends PFComponent {
    private static final long CONNECTION_KEEP_ALIVE_TIMOUT_MS = Constants.CONNECTION_KEEP_ALIVE_TIMOUT * 1000L;
    private static final long TIME_WITHOUT_KEEPALIVE_UNTIL_PING = CONNECTION_KEEP_ALIVE_TIMOUT_MS / 3L;

    /**
     * The threadpool executing the basic I/O connections to the nodes.
     */
    private ExecutorService connectionThreadPool;

    /**
     * The connection handler factory.
     */
    private ConnectionHandlerFactory conHanFactory;

    /**
     * The list of connection handlers to check for keepalive
     */
    private List<ConnectionHandler> keepAliveList;

    private boolean started;

    public IOProvider(Controller controller) {
        super(controller);
        // Create default connection factory. not set this in
        conHanFactory = new ConnectionHandlerFactory(controller);
        keepAliveList = new CopyOnWriteArrayList<ConnectionHandler>();
    }

    public void start() {
        // For basic IO
        connectionThreadPool = Executors
            .newCachedThreadPool(new NamedThreadFactory("ConnectionHandler-"));
        started = true;
        startIO(new KeepAliveChecker());
    }

    public void shutdown() {
        started = false;
        if (connectionThreadPool != null) {
            log().debug("Shutting down connection I/O threadpool");
            connectionThreadPool.shutdownNow();
        }
    }

    /**
     * Sets the connection handler factory, which is responsible for creating
     * connection handler for basic io.
     * 
     * @param conHanFactory
     *            the new factory.
     */
    public synchronized void setConnectionHandlerFactory(
        ConnectionHandlerFactory conHanFactory)
    {
        Reject.ifNull(conHanFactory, "The factory must not be null");
        log().verbose("Setting new connection factory: " + conHanFactory);
        this.conHanFactory = conHanFactory;
    }

    /**
     * @return the connection handler factory to create connection handler with.
     */
    public ConnectionHandlerFactory getConnectionHandlerFactory() {
        return conHanFactory;
    }

    /**
     * Starts the sender and receiver IO in the global threadpool.
     * 
     * @param ioSender
     *            the io sender
     * @param ioReceiver
     *            the io receiver
     */
    public void startIO(Runnable ioSender, Runnable ioReceiver) {
        Reject.ifNull(ioSender, "IO Sender is null");
        Reject.ifNull(ioReceiver, "IO Receiver is null");
        if (logVerbose) {
            log().verbose("Starting IO for " + ioSender + " " + ioReceiver);
        }
        connectionThreadPool.submit(ioSender);
        connectionThreadPool.submit(ioReceiver);
    }

    /**
     * Starts a general connection handling working.
     * 
     * @param ioWorker
     *            a io worker
     */
    public void startIO(Runnable ioWorker) {
        Reject.ifNull(ioWorker, "IO Worker is null");
        if (logVerbose) {
            log().verbose("Starting IO for " + ioWorker);
        }
        connectionThreadPool.submit(ioWorker);
    }

    /**
     * Adds this connection handler to get checked for keepalive. If the
     * connection handler times out is gets shut down.
     * 
     * @param conHan
     *            the connection handler to check
     */
    public void startKeepAliveCheck(ConnectionHandler conHan) {
        Reject.ifNull(conHan, "Connection handler is null");
        if (!conHan.isConnected()) {
            return;
        }
        keepAliveList.add(conHan);
    }

    private class KeepAliveChecker implements Runnable {

        public void run() {
            if (!started) {
                return;
            }
            while (started) {
                log().warn(
                    "Checking " + keepAliveList.size()
                        + " con handlers for keepalive: " + keepAliveList);
                for (ConnectionHandler conHan : keepAliveList) {
                    if (!conHan.isConnected()) {
                        keepAliveList.remove(conHan);
                    }
                    if (!checkIfOk(conHan)) {
                        keepAliveList.remove(conHan);
                    }
                }
                try {
                    Thread.sleep(TIME_WITHOUT_KEEPALIVE_UNTIL_PING);
                } catch (InterruptedException e) {
                    log().verbose(e);
                    return;
                }
            }
        }

        private boolean checkIfOk(ConnectionHandler conHan) {
            boolean newPing;
            Date lastKeepaliveMessage = conHan.getLastKeepaliveMessageTime();
            if (lastKeepaliveMessage == null) {
                newPing = true;
            } else {
                long timeWithoutKeepalive = System.currentTimeMillis()
                    - lastKeepaliveMessage.getTime();
                newPing = timeWithoutKeepalive >= TIME_WITHOUT_KEEPALIVE_UNTIL_PING;
                if (logVerbose) {
                    log().verbose(
                        "Keep-alive check. Received last keep alive message "
                            + timeWithoutKeepalive + "ms ago, ping required? "
                            + newPing + ". Node: " + conHan.getMember());
                }
                if (timeWithoutKeepalive > CONNECTION_KEEP_ALIVE_TIMOUT_MS) {
                    log().warn(
                        "Shutting down. Dead connection detected ("
                            + timeWithoutKeepalive + "ms timeout) to "
                            + conHan.getMember());
                    conHan.shutdownWithMember();
                    return false;
                }
            }
            if (newPing) {
                // Send new ping
                conHan.sendMessagesAsynchron(new Ping(-1));
            }
            return true;
        }
    }
}
