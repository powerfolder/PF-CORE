/* $Id$
 */
package de.dal33t.powerfolder.net;

import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.NamedThreadFactory;
import de.dal33t.powerfolder.util.Reject;

/**
 * Provides basic IO stuff.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class IOProvider extends PFComponent {
    /**
     * The threadpool executing the basic I/O connections to the nodes.
     */
    private ExecutorService connectionThreadPool;

    /**
     * The connection handler factory.
     */
    private ConnectionHandlerFactory conHanFactory;

    /**
     * A timer used to perform keep-alive detection tasks. Can be used by
     * ConnectionHandler implementation.
     */
    private Timer keepAliveTimer;

    public IOProvider(Controller controller) {
        super(controller);
        // Create default connection factory. not set this in
        conHanFactory = new ConnectionHandlerFactory();
    }

    public void start() {
        keepAliveTimer = new Timer();
        // For basic IO
        connectionThreadPool = Executors
            .newCachedThreadPool(new NamedThreadFactory("ConnectionHandler-"));
    }

    public void shutdown() {
        if (keepAliveTimer != null) {
            keepAliveTimer.purge();
            keepAliveTimer.cancel();
        }
        if (connectionThreadPool != null) {
            log().debug("Shutting down connection I/O threadpool");
            connectionThreadPool.shutdown();
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
     * @return the keepalive time used to perform low-io keepalive checks.
     */
    protected Timer getKeepAliveTimer() {
        return keepAliveTimer;
    }
}
