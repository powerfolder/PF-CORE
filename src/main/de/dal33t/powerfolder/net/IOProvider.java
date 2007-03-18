/* $Id$
 */
package de.dal33t.powerfolder.net;

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

    public IOProvider(Controller controller) {
        super(controller);
        // Create default connection factory. not set this in
        conHanFactory = new ConnectionHandlerFactory();
    }

    public void start() {
        // For basic IO
        connectionThreadPool = Executors
            .newCachedThreadPool(new NamedThreadFactory("ConnectionHandler-"));
    }

    public void shutdown() {
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
        if (logVerbose) {
            log().verbose("Starting IO for " + ioSender + " " + ioReceiver);
        }
        connectionThreadPool.submit(ioSender);
        connectionThreadPool.submit(ioReceiver);
    }
}
