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
package de.dal33t.powerfolder.net;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.message.Ping;
import de.dal33t.powerfolder.util.*;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides basic IO stuff.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class IOProvider extends PFComponent {
    private static final Logger log = Logger.getLogger(IOProvider.class.getName());
    private static final long CONNECTION_KEEP_ALIVE_TIMOUT_MS = Constants.CONNECTION_KEEP_ALIVE_TIMEOUT * 1000L;
    private static final long TIME_WITHOUT_KEEPALIVE_LEGACY_UNTIL_PING = CONNECTION_KEEP_ALIVE_TIMOUT_MS / 3L;
    private static final long TIME_WITHOUT_KEEPALIVE_WEBSOCKET_UNTIL_PING = CONNECTION_KEEP_ALIVE_TIMOUT_MS / 9L;

    /**
     * The threadpool executing the basic I/O connections to the nodes.
     */
    private ExecutorService ioThreadPool;

    /**
     * PFS-5311: Dedicated bounded thread pool for search indexing work. Prevents indexing from saturating
     * the general IO pool and limits concurrent Tika/OCR/Lucene operations across all folders.
     */
    private ThreadPoolExecutor indexingThreadPool;

    /**
     * The connection handler factory.
     */
    private ConnectionHandlerFactory conHanFactory;

    /**
     * Manager of relayed connection
     */
    private RelayedConnectionManager relayedConManager;

    /**
     * Manager of UDT socket connections
     */
    private UDTSocketConnectionManager udtConManager;

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
        relayedConManager = new RelayedConnectionManager(controller);
        udtConManager = new UDTSocketConnectionManager(controller,
            Range.getRangeByNumbers(1024, 65535));
    }

    public void start() {
        // For basic IO
        ioThreadPool = new WrapperExecutorService(
            Executors.newCachedThreadPool(new NamedThreadFactory("IOThread-")));

        // PFS-5311: Bounded pool for search indexing — prevents Tika/OCR from saturating CPU/IO during
        // startup with hundreds of folders.
        int maxIndexThreads = Math.max(2,
            ConfigurationEntry.SEARCH_INDEX_MAX_THREADS.getValueInt(getController()));
        NamedThreadFactory indexThreadFactory = new NamedThreadFactory("SearchIndexThread-");
        indexingThreadPool = new ThreadPoolExecutor(maxIndexThreads, maxIndexThreads, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), r -> {
                Thread t = indexThreadFactory.newThread(r);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });
        logInfo("Search indexing thread pool started: maxThreads=" + maxIndexThreads);

        started = true;
        getController().scheduleAndRepeat(new KeepAliveChecker(),
            TIME_WITHOUT_KEEPALIVE_WEBSOCKET_UNTIL_PING);
        relayedConManager.start();
    }

    public void shutdown() {
        started = false;
        if (indexingThreadPool != null) {
            logFine("Shutting down search indexing threadpool");
            indexingThreadPool.shutdownNow();
        }
        if (ioThreadPool != null) {
            logFine("Shutting down connection I/O threadpool");
            ioThreadPool.shutdownNow();
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
        logFiner("Setting new connection factory: " + conHanFactory);
        this.conHanFactory = conHanFactory;
    }

    /**
     * @return the connection handler factory to create connection handler with.
     */
    public ConnectionHandlerFactory getConnectionHandlerFactory() {
        return conHanFactory;
    }

    /**
     * @return the relayed connection manager.
     */
    public RelayedConnectionManager getRelayedConnectionManager() {
        return relayedConManager;
    }

    public UDTSocketConnectionManager getUDTSocketConnectionManager() {
        return udtConManager;
    }

    /**
     * Starts a general connection handling working.
     *
     * @param ioWorker a io worker
     * @return
     */
    public Future<?> startIO(final Runnable ioWorker) {
        Reject.ifNull(ioWorker, "IO Worker is null");
        if (ioThreadPool.isTerminated() || ioThreadPool.isShutdown()) {
            logFine("Rejected executing of ioWorker, already stopped: "
                + ioWorker);
            return null;
        }
        if (isFiner()) {
            logFiner("Starting IO for " + ioWorker);
        }
        try {
            return ioThreadPool.submit(ioWorker);
        } catch (OutOfMemoryError oom) {
            // PFS-1722
            oom.printStackTrace();
            logSevere("Out of memory while starting " + ioWorker + ": "
                + oom.toString(), oom);
            logSevere("Shutting down java virtual machine. Exit code: 107");
            
            if (oom.getMessage() != null && oom.getMessage().toLowerCase().contains("thread")) {
                logWarning("Current threads: ");
                logWarning(Debug.dumpCurrentStacktraces(false));
            }

            getController().exit(107);
            throw oom;
        }
    }

    /**
     * PFS-5311: Submits an indexing task to the dedicated bounded indexing thread pool.
     * Unlike {@link #startIO(Runnable)}, this pool has a configurable maximum thread count
     * to prevent CPU/IO saturation.
     *
     * @param worker the indexing worker to execute
     * @return a Future representing the pending task, or null if rejected
     */
    public Future<?> startIndexing(final Runnable worker) {
        Reject.ifNull(worker, "Indexing worker is null");
        if (indexingThreadPool == null || indexingThreadPool.isShutdown()) {
            logFine("Rejected indexing worker, pool not available: " + worker);
            return null;
        }
        try {
            return indexingThreadPool.submit(worker);
        } catch (RejectedExecutionException e) {
            logWarning("Indexing task rejected (pool full): " + e.getMessage());
            return null;
        }
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

    /**
     * Removes this connection handler to get checked for keepalive.
     *
     * @param conHan
     *            the connection handler to remove
     */
    public void removeKeepAliveCheck(ConnectionHandler conHan) {
        Reject.ifNull(conHan, "Connection handler is null");
        keepAliveList.remove(conHan);
    }

    private class KeepAliveChecker implements Runnable {

        public void run() {
            if (!started) {
                return;
            }
            if (log.isLoggable(Level.FINE)) {
                logFine("Checking " + keepAliveList.size()
                    + " con handlers for keepalive");
            }
            Collection<ConnectionHandler> list = new HashSet<ConnectionHandler>(
                keepAliveList);
            if (getController().getNodeManager() != null) {
                Collection<Member> nodes = getController().getNodeManager()
                    .getNodesAsCollection();
                // Might happen on startup
                if (nodes != null) {
                    for (Member node : nodes) {
                        ConnectionHandler peer = node.getPeer();
                        if (peer == null) {
                            continue;
                        }
                        if (!peer.isConnected()) {
                            continue;
                        }
                        if (!list.contains(peer)) {
                            logFine("ConHan not in keepalive list of " + node);
                            list.add(peer);
                        }
                    }
                }
            }
            for (ConnectionHandler conHan : list) {
                if (!conHan.isConnected()) {
                    keepAliveList.remove(conHan);
                }
                if (!checkIfOk(conHan)) {
                    keepAliveList.remove(conHan);
                }
            }
        }

        private boolean checkIfOk(ConnectionHandler conHan) {
            boolean newPing;
            Date lastKeepaliveMessage = conHan.getLastKeepaliveMessageTime();
            if (lastKeepaliveMessage == null) {
                newPing = true;
            } else {
                long timeWithoutKeepalive = System.currentTimeMillis() - lastKeepaliveMessage.getTime();
                boolean legacy = conHan instanceof AbstractSocketConnectionHandler
                        || conHan instanceof AbstractUDTSocketConnectionHandler
                        || conHan instanceof AbstractRelayedConnectionHandler;
                long timeWithoutKeepaliveAllowed = legacy ? TIME_WITHOUT_KEEPALIVE_LEGACY_UNTIL_PING : TIME_WITHOUT_KEEPALIVE_WEBSOCKET_UNTIL_PING;
                newPing = timeWithoutKeepalive >= timeWithoutKeepaliveAllowed;
                if (isFiner()) {
                    logFiner("Keep-alive check. Received last keep alive message "
                        + timeWithoutKeepalive
                        + "ms ago, ping required? "
                        + newPing + ". Node: " + conHan.getMember());
                }
                if (timeWithoutKeepalive > CONNECTION_KEEP_ALIVE_TIMOUT_MS) {
                    logFine("Shutting down. Dead connection detected ("
                        + (timeWithoutKeepalive / 1000) + "s timeout) to "
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
