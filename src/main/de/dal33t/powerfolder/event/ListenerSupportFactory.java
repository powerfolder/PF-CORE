/* $Id: ListenerSupportFactory.java,v 1.8 2006/03/14 13:14:20 schaatser Exp $
 */
package de.dal33t.powerfolder.event;

import java.awt.EventQueue;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Factory used to created event/listener support upon eventlistner interfaces.
 * <P>
 * Created Listenersupport implementaion maintains a listener list and handles
 * swing thread wrapping issues.
 * <p>
 * Listenersupport implementaion can be created upon an event listener
 * interface. E.g. for <code>TransferManagerListener</code>. This
 * Listenersupport implementaion will fire events to all registered listeners.
 * Just call the event method for the eventlistner interface on the
 * implementation returned by <code>createListenerSupport</code>
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.8 $
 */
public class ListenerSupportFactory {
    private static final Logger LOG = Logger
        .getLogger(ListenerSupportFactory.class);

    // AWT system check
    private static final boolean awtAvailable = UIUtil.isAWTAvailable();

    private static final Map<Double, String> PERFORMANCE_MAP =
            Collections.synchronizedMap(new TreeMap<Double, String>());

    // Start a thread to dump stats to the log.
    static {
        Thread t = new Thread() {
            public void run() {
                boolean interrupted = false;
                while (!interrupted) {
                    try {
                        Thread.sleep(60000);
                        int i = 0;
                        if (!PERFORMANCE_MAP.isEmpty()) {
                            if (Logger.isDebugLevelEnabled()) {
                                LOG.debug("Performance statistics") ;
                                LOG.debug("======================");
                                if (PERFORMANCE_MAP.isEmpty()) {
                                    LOG.debug("No statistics");
                                } else {
                                    for (Double time : PERFORMANCE_MAP.keySet()) {
                                        String s = PERFORMANCE_MAP.get(time);
                                        LOG.debug(s);
                                        if (i++ > 100) {
                                            // Only display the top 100 offenders.
                                            // Keys are negative, so longest running
                                            // task is displayed first.
                                            break;
                                        }
                                    }
                                }
                                LOG.debug("======================");
                            }
                            PERFORMANCE_MAP.clear();
                        }
                    } catch (InterruptedException e) {
                        interrupted = true;
                    } catch (Exception e) {
                        LOG.error("Problem with performance statistics", e);
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    /**
     * No instance allowed
     */
    private ListenerSupportFactory() {
        super();
    }

    /**
     * Creats a listener support for the listener event interface. Returned
     * object can directly be casted into the listener event interface.
     * <p>
     * All calls to methods on that object will fire that event to its
     * registered listeners.
     * <p>
     * 
     * @param listenerInterface
     * @return
     */
    public static Object createListenerSupport(Class listenerInterface) {
        if (listenerInterface == null) {
            throw new NullPointerException("Listener interface is empty");
        }
        if (!listenerInterface.isInterface()) {
            throw new IllegalArgumentException(
                "Listener interface class is not an java Interface!");
        }
        ClassLoader cl = listenerInterface.getClassLoader();
        InvocationHandler handler = new ListenerSupportInvocationHandler(
            listenerInterface);
        Object listenerSupportImpl = Proxy.newProxyInstance(cl,
            new Class[]{listenerInterface}, handler);
        LOG.verbose("Created event listener support for interface '"
            + listenerInterface.getName() + "'");
        return listenerSupportImpl;
    }

    /**
     * Suspends (or resumes) a listener support, it set to true this listener
     * supoort will not file events until set to false. The listener support has
     * to be created via <code>createListenerSupport</code> before. Also the
     * listener needs to implement the listener event interface. Otherwise an
     * exception is thrown
     * 
     * @param listenerSupport
     * @param suspended
     */
    public static void setSuspended(Object listenerSupport, boolean suspended) {
        if (listenerSupport == null) {
            throw new NullPointerException("Listener support is null");
        }
        if (!Proxy.isProxyClass(listenerSupport.getClass())) {
            throw new IllegalArgumentException(
                "Listener support is not valid. Seems not to be created with createListenerSupport.");
        }
        InvocationHandler invHandler = Proxy
            .getInvocationHandler(listenerSupport);
        if (invHandler instanceof ListenerSupportInvocationHandler) {
            ListenerSupportInvocationHandler lsInvHandler = (ListenerSupportInvocationHandler) invHandler;
            // Now suspend listener
            lsInvHandler.setSuspended(suspended);
        } else {
            throw new IllegalArgumentException(
                "Listener support is not valid. Seems not to be created with createListenerSupport.");
        }
    }

    /**
     * Adds a listener to a listener support. The listener support has to be
     * created via <code>createListenerSupport</code> factory method. Also the
     * listener needs to implement the listener event interface otherwise an
     * exception is thrown (see ListenerSupportInvocationHandler.checkListener).
     * 
     * @param listenerSupport
     *      The listenerSupport where the listener should be added to. 
     * @param listener
     *      The event listener to add.
     */
    public static void addListener(CoreListener listenerSupport,
        CoreListener listener)
    {
        if (listenerSupport == null) {
            throw new NullPointerException("Listener support is null");
        }
        if (!Proxy.isProxyClass(listenerSupport.getClass())) {
            throw new IllegalArgumentException(
                "Listener support is not valid. Seems not to be created with createListenerSupport.");
        }
        InvocationHandler invHandler = Proxy
            .getInvocationHandler(listenerSupport);
        if (invHandler instanceof ListenerSupportInvocationHandler) {
            ListenerSupportInvocationHandler lsInvHandler = (ListenerSupportInvocationHandler) invHandler;
            // Now add listener
            lsInvHandler.addListener(listener);
        } else {
            throw new IllegalArgumentException(
                "Listener support is not valid. Seems not to be created with createListenerSupport.");
        }
    }

    /**
     * Removes a listener from a listener support. The listener support has to
     * be created via <code>createListenerSupport</code> factory method. Also the
     * listener needs to implement the listener event interface otherwise an
     * exception is thrown (see ListenerSupportInvocationHandler.checkListener).
     * 
     * @param listenerSupport
     * @param listener
     */
    public static void removeListener(Object listenerSupport,
        CoreListener listener)
    {
        if (listenerSupport == null) {
            throw new NullPointerException("Listener support is null");
        }
        if (!Proxy.isProxyClass(listenerSupport.getClass())) {
            throw new IllegalArgumentException(
                "Listener support is not valid. Seems not to be created with createListenerSupport.");
        }
        InvocationHandler invHandler = Proxy
            .getInvocationHandler(listenerSupport);
        if (invHandler instanceof ListenerSupportInvocationHandler) {
            ListenerSupportInvocationHandler lsInvHandler = (ListenerSupportInvocationHandler) invHandler;
            // Now remove the listener
            lsInvHandler.removeListener(listener);
        } else {
            throw new IllegalArgumentException(
                "Listener support is not valid. Seems not to be created with createListenerSupport.");
        }
    }

    /**
     * Removes all listeners from a listener support. The listener support has
     * to be created via <code>createListenerSupport</code> before. Otherwise
     * an exception is thrown
     * 
     * @param listenerSupport
     * @param listener
     */
    public static void removeAllListeners(Object listenerSupport) {
        if (listenerSupport == null) {
            throw new NullPointerException("Listener support is null");
        }
        if (!Proxy.isProxyClass(listenerSupport.getClass())) {
            throw new IllegalArgumentException(
                "Listener support is not valid. Seems not to be created with createListenerSupport.");
        }
        InvocationHandler invHandler = Proxy
            .getInvocationHandler(listenerSupport);
        if (invHandler instanceof ListenerSupportInvocationHandler) {
            ListenerSupportInvocationHandler lsInvHandler = (ListenerSupportInvocationHandler) invHandler;
            // Now remove all listeners
            lsInvHandler.removeAllListeners();
        } else {
            throw new IllegalArgumentException(
                "Listener support is not valid. Seems not to be created with createListenerSupport.");
        }
    }

    // Inner classes **********************************************************

    /**
     * The invocation handler, which deligates fire event method calls to the
     * listener. Maybe suspended, in this state it will not fire events.
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private static class ListenerSupportInvocationHandler implements
        InvocationHandler
    {
        private Class listenerInterface;
        private List<CoreListener> listenersNotInDispatchThread;
        private List<CoreListener> listenersInDispatchThread;
        private boolean suspended;

        /**
         * Creates an invocation handler which basically handles the event
         * support.
         * 
         * @param listenerInterface
         *            the listener event interface
         */
        private ListenerSupportInvocationHandler(Class listenerInterface) {
            this.listenerInterface = listenerInterface;
            this.listenersInDispatchThread = new CopyOnWriteArrayList<CoreListener>();
            this.listenersNotInDispatchThread = new CopyOnWriteArrayList<CoreListener>();
        }

        /**
         * Adds a listener to this support impl
         * 
         * @param listener
         */
        public void addListener(CoreListener listener) {
            if (checkListener(listener)) {
                // Okay, add listener
                if (listener.fireInEventDispathThread()) {
                    listenersInDispatchThread.add(listener);
                } else {
                    listenersNotInDispatchThread.add(listener);
                }
            }
        }

        /**
         * Removes a listener from this support impl
         * 
         * @param listener
         */
        public void removeListener(CoreListener listener) {
            if (checkListener(listener)) {
                // Okay, remove listener
                if (listener.fireInEventDispathThread()) {
                    listenersInDispatchThread.remove(listener);
                } else {
                    listenersNotInDispatchThread.remove(listener);
                }
            }
        }

        /**
         * Removes all listeners from this support impl
         */
        public void removeAllListeners() {
            listenersInDispatchThread.clear();
            listenersNotInDispatchThread.clear();
        }

        /**
         * Checks if the listener is an instance of our supported listener
         * interface.
         * 
         * @param listener The listener to check
         * @return true if succeded, otherwise exception is thrown
         * @throws IllegalArgumentException
         *             if both do not match
         */
        private boolean checkListener(CoreListener listener) {
            if (listener == null) {
                throw new NullPointerException("Listener is null");
            }
            if (!listenerInterface.isInstance(listener)) {
                throw new IllegalArgumentException("Listener '" + listener
                    + "' is not an instance of support listener interface '"
                    + listenerInterface.getName() + "'");
            }
            return true;
        }

        /**
         * Deligates calls to registered listners
         * 
         * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
         *      java.lang.reflect.Method, java.lang.Object[])
         */
        public Object invoke(Object proxy, final Method method,
            final Object[] args) throws Throwable
        {
            if (listenersInDispatchThread.isEmpty()
                && listenersNotInDispatchThread.isEmpty())
            {
                // No listeners, skip
                return null;
            }

            // Create runner
            if (!suspended) {
                Runnable runner = new Runnable() {
                    public void run() {
                        for (CoreListener listener : listenersInDispatchThread)
                        {
                            try {
                                Date startDate = new Date();
                                method.invoke(listener, args);
                                if (Logger.isDebugLevelEnabled()) {
                                    Date endDate = new Date();
                                    logTime(startDate, endDate, method, args, listener);
                                }
                            } catch (IllegalArgumentException e) {
                                LOG.error(
                                    "Received an exception from listener '"
                                        + listener + "', class '"
                                        + listener.getClass().getName() + "'",
                                    e);
                            } catch (IllegalAccessException e) {
                                LOG.error(
                                    "Received an exception from listener '"
                                        + listener + "', class '"
                                        + listener.getClass().getName() + "'",
                                    e);
                            } catch (InvocationTargetException e) {
                                LOG.error(
                                    "Received an exception from listener '"
                                        + listener + "', class '"
                                        + listener.getClass().getName() + "'",
                                    e.getCause());
                                // Also log original exception
                                LOG.verbose(e);
                            }
                        }
                    }
                };

                if (!awtAvailable || EventQueue.isDispatchThread()) {
                    // NO awt system ? do not put in swing thread
                    // Already in swing thread ? also don't wrap
                    runner.run();
                } else {
                    // Put runner in swingthread
                    SwingUtilities.invokeLater(runner);
                }

                for (CoreListener listener : listenersNotInDispatchThread) {
                    try {
                        Date startDate = new Date();
                        method.invoke(listener, args);
                        if (Logger.isDebugLevelEnabled()) {
                            Date endDate = new Date();
                            logTime(startDate, endDate, method, args, listener);
                        }
                    } catch (IllegalArgumentException e) {
                        LOG.error("Received an exception from listener '"
                            + listener + "', class '"
                            + listener.getClass().getName() + "'", e);
                    } catch (IllegalAccessException e) {
                        LOG.error("Received an exception from listener '"
                            + listener + "', class '"
                            + listener.getClass().getName() + "'", e);
                    } catch (InvocationTargetException e) {
                        LOG
                            .error("Received an exception from listener '"
                                + listener + "', class '"
                                + listener.getClass().getName() + "'", e
                                .getCause());
                        // Also log original exception
                        LOG.verbose(e);
                    }
                }
            }
            return null;

        }

        public boolean isSuspended() {
            return suspended;
        }

        public void setSuspended(boolean suspended) {
            this.suspended = suspended;
        }

    }

    /**
     * Log suspicious long-running methods.
     *
     * @param startDate
     * @param endDate
     * @param method
     * @param args
     * @param listener
     */
    private static void logTime(Date startDate, Date endDate, Method method,
                                Object[] args, CoreListener listener) {

        // Calculate how long it took.
        long time = endDate.getTime() - startDate.getTime();

        // Report invokations that take time.
        if (time > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                sb.append(args[i].toString());
                if (i < args.length - 1) {
                    sb.append(", ");
                }
            }
            String message = "Method " + method.getName()
                    + " [" + sb.toString() + "]"
                    + " invoked on listener "
                    + listener + " in " + time
                    + "ns on " + endDate.toString();

            // Include random part so duplicate times do not eject entries.
            // Negate, so longest gets displayed first.
            Double key = -(time + Math.random());
            PERFORMANCE_MAP.put(key, message);
        }
    }
}