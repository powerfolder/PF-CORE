/* $Id$
 */
package de.dal33t.powerfolder.net;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
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
     * Threadpool to handle incoming connections.
     */
    private ExecutorService threadPool;

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
            .newCachedThreadPool(new DefaultThreadFactory("ConnectionHandler-"));
        // Starting own threads, which cares about incoming node connections
        threadPool = Executors.newFixedThreadPool(
            Constants.MAX_INCOMING_CONNECTIONS, new DefaultThreadFactory(
                "Incoming-Connection-"));
        // Alternative:
        // Executors.newCachedThreadPool();;
    }

    public void shutdown() {
        // Stop threadpool
        if (threadPool != null) {
            log().debug("Shutting down incoming connection threadpool");
            threadPool.shutdown();
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
        if (logVerbose) {
            log().verbose("Starting IO for " + ioSender + " " + ioReceiver);
        }
        connectionThreadPool.submit(ioSender);
        connectionThreadPool.submit(ioReceiver);
    }

    /**
     * The default thread factory
     */
    private static class DefaultThreadFactory implements ThreadFactory {
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        DefaultThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix
                + threadNumber.getAndIncrement());
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

}
