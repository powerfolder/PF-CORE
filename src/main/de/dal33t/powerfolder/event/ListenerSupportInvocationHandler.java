package de.dal33t.powerfolder.event;

import java.awt.EventQueue;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import de.dal33t.powerfolder.util.Profiling;
import de.dal33t.powerfolder.util.ProfilingEntry;

/**
 * The invocation handler, which delegates fire event method calls to the
 * listener. Maybe suspended, in this state it will not fire events.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 */
class ListenerSupportInvocationHandler<T> implements InvocationHandler {
    private static final Logger LOG = Logger
        .getLogger(ListenerSupportInvocationHandler.class.getName());
    private static final int WARN_IF_MORE_LISTENERS = 144;

    private Class<T> listenerInterface;
    private List<CoreListener> listenersNonEDT;
    private List<WeakCoreListener> weakListenersNonEDT;
    private List<CoreListener> listenersInEDT;
    private List<WeakCoreListener> weaklistenersInEDT;
    private boolean suspended;

    /**
     * Creates an invocation handler which basically handles the event support.
     * 
     * @param listenerInterface
     *            the listener event interface
     */
    ListenerSupportInvocationHandler(Class<T> listenerInterface) {
        this.listenerInterface = listenerInterface;
        this.listenersInEDT = new CopyOnWriteArrayList<CoreListener>();
        this.listenersNonEDT = new CopyOnWriteArrayList<CoreListener>();
        this.weakListenersNonEDT = new CopyOnWriteArrayList<WeakCoreListener>();
        this.weaklistenersInEDT = new CopyOnWriteArrayList<WeakCoreListener>();
    }

    /**
     * Adds a listener to this support impl
     * 
     * @param listener
     */
    void addListener(CoreListener listener) {
        if (checkListener(listener)) {
            // Okay, add listener
            int n = 0;
            if (listener.fireInEventDispatchThread()) {
                listenersInEDT.add(listener);
                n = listenersInEDT.size();
            } else {
                listenersNonEDT.add(listener);
                n = listenersNonEDT.size();
            }
            if (LOG.isLoggable(Level.WARNING) && n > WARN_IF_MORE_LISTENERS) {
                LOG.warning(n + " listeners of " + listenerInterface.getName()
                    + " registered");
            }
        }
    }

    /**
     * Adds a weak listener to this support impl. Listener gets removed if not
     * other references to it is hold except by this listener support (or any
     * other weak or soft referece).
     * 
     * @param listener
     */
    void addWeakListener(CoreListener listener) {
        if (checkListener(listener)) {
            // Okay, add listener
            int n = 0;
            WeakCoreListener weakListener = new WeakCoreListener(listener, this);
            if (listener.fireInEventDispatchThread()) {
                weaklistenersInEDT.add(weakListener);
                n = weaklistenersInEDT.size();

                // Do some cleanup
                if (n % 10 == 0) {
                    for (WeakCoreListener candidate : weaklistenersInEDT) {
                        if (!candidate.isValid()) {
                            // LOG.log(Level.WARNING, "Cleanup X of " +
                            // candidate);
                            weaklistenersInEDT.remove(candidate);
                        }
                    }
                }
                n = weaklistenersInEDT.size();
            } else {
                weakListenersNonEDT.add(weakListener);
                n = weakListenersNonEDT.size();

                // Do some cleanup
                if (n % 10 == 0) {
                    for (WeakCoreListener candidate : weakListenersNonEDT) {
                        if (!candidate.isValid()) {
                            // LOG.log(Level.WARNING, "Cleanup Y of " +
                            // candidate);
                            weakListenersNonEDT.remove(candidate);
                        }
                    }
                }
                n = weakListenersNonEDT.size();
            }
            if (LOG.isLoggable(Level.WARNING) && n > WARN_IF_MORE_LISTENERS
                && n % 610 == 0)
            {
                LOG.log(Level.WARNING, n + " weak listeners of "
                    + listenerInterface.getName() + " registered");
            }
        }
    }

    /**
     * Removes a listener from this support impl
     * 
     * @param listener
     */
    void removeListener(CoreListener listener) {
        if (listener instanceof WeakCoreListener) {
            if (!weaklistenersInEDT.isEmpty()) {
                for (WeakCoreListener weakCandidate : weaklistenersInEDT) {
                    CoreListener candidate = weakCandidate.getRef();
                    if ((candidate != null && candidate.equals(listener))
                        || weakCandidate.equals(listener))
                    {
                        weaklistenersInEDT.remove(weakCandidate);
                    }
                }
            }
            if (!weakListenersNonEDT.isEmpty()) {
                for (WeakCoreListener weakCandidate : weakListenersNonEDT) {
                    CoreListener candidate = weakCandidate.getRef();
                    if ((candidate != null && candidate.equals(listener))
                        || weakCandidate.equals(listener))
                    {
                        weakListenersNonEDT.remove(weakCandidate);
                    }
                }
            }
        } else if (checkListener(listener)) {
            boolean removed = listenersInEDT.remove(listener);
            removed = listenersNonEDT.remove(listener) || removed;
            if (!removed) {
                if (!weaklistenersInEDT.isEmpty()) {
                    for (WeakCoreListener weakCandidate : weaklistenersInEDT) {
                        CoreListener candidate = weakCandidate.getRef();
                        if ((candidate != null && candidate.equals(listener))
                            || weakCandidate.equals(listener))
                        {
                            weaklistenersInEDT.remove(weakCandidate);
                        }
                    }
                }
                if (!weakListenersNonEDT.isEmpty()) {
                    for (WeakCoreListener weakCandidate : weakListenersNonEDT) {
                        CoreListener candidate = weakCandidate.getRef();
                        if ((candidate != null && candidate.equals(listener))
                            || weakCandidate.equals(listener))
                        {
                            weakListenersNonEDT.remove(weakCandidate);
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes all listeners from this support impl
     */
    void removeAllListeners() {
        listenersInEDT.clear();
        listenersNonEDT.clear();
        weaklistenersInEDT.clear();
        weakListenersNonEDT.clear();
    }

    /**
     * Checks if the listener is an instance of our supported listener
     * interface.
     * 
     * @param listener
     *            The listener to check
     * @return true if succeeded, otherwise exception is thrown
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
                + listenerInterface.getName() + '\'');
        }
        return true;
    }

    /**
     * Delegates calls to registered listeners
     * 
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
     *      java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, final Method method, final Object[] args)
        throws Throwable
    {
        if (listenersInEDT.isEmpty() && listenersNonEDT.isEmpty()
            && weaklistenersInEDT.isEmpty() && weakListenersNonEDT.isEmpty())
        {
            // No listeners, skip
            return false;
        }

        if (method.getName().equals("fireInEventDispatchThread")) {
            // Uhh we are getting registered somewhere else and
            // the other ListenerSupport is asking us about
            // EDT. Better stay away from the AWT. Let our listeners
            // decide were they want to be executed!
            return false;
        }

        // Create runner
        if (!suspended) {
            if (!listenersInEDT.isEmpty()) {
                fireToEDTListeners(method, args);
            }
            if (!listenersNonEDT.isEmpty()) {
                fireToNonEDTListeners(method, args);
            }
            if (!weaklistenersInEDT.isEmpty()) {
                fireToWeakEDTListeners(method, args);
            }
            if (!weakListenersNonEDT.isEmpty()) {
                fireToWeakNonEDTListeners(method, args);
            }
        }
        return false;
    }

    private void fireToNonEDTListeners(final Method method, final Object[] args)
    {
        for (CoreListener listener : listenersNonEDT) {
            fire(method, args, listener);
        }
    }

    private void fireToEDTListeners(final Method method, final Object[] args) {
        Runnable runner = new Runnable() {
            public void run() {
                for (CoreListener listener : listenersInEDT) {
                    fire(method, args, listener);
                }
            }
        };
        if (!ListenerSupportFactory.AWT_AVAILABLE
            || EventQueue.isDispatchThread())
        {
            // NO awt system ? do not put in swing thread
            // Already in swing thread ? also don't wrap
            runner.run();
        } else {
            // Put runner in swingthread
            try {
                SwingUtilities.invokeLater(runner);
            } catch (Exception e) {
                LOG.fine(e.toString());
            }
        }
    }

    private void fireToWeakNonEDTListeners(final Method method,
        final Object[] args)
    {
        for (WeakCoreListener listner : weakListenersNonEDT) {
            if (listner.isValid()) {
                fire(method, args, listner);
            } else {
                weakListenersNonEDT.remove(listner);
            }
        }
    }

    private void fireToWeakEDTListeners(final Method method, final Object[] args)
    {
        Runnable runner = new Runnable() {
            public void run() {
                for (WeakCoreListener weakListener : weaklistenersInEDT) {
                    if (weakListener.isValid()) {
                        fire(method, args, weakListener);
                    } else {
                        weaklistenersInEDT.remove(weakListener);
                    }
                }
            }
        };
        if (!ListenerSupportFactory.AWT_AVAILABLE
            || EventQueue.isDispatchThread())
        {
            // NO awt system ? do not put in swing thread
            // Already in swing thread ? also don't wrap
            runner.run();
        } else {
            // Put runner in swingthread
            SwingUtilities.invokeLater(runner);
        }
    }

    private void fire(final Method method, final Object[] args,
        WeakCoreListener listener)
    {
        ProfilingEntry profilingEntry = null;
        if (Profiling.ENABLED) {
            profilingEntry = Profiling.start(listener.getClass().getName()
                + ':' + method.getName(), "");
        }
        try {
            listener.invoke(null, method, args);
        } catch (Throwable e) {
            ListenerSupportFactory.LOG.log(Level.SEVERE,
                "Received an exception from listener '" + listener
                    + "', class '" + listener.getClass().getName() + '\'', e);
            // Also log original exception
            ListenerSupportFactory.LOG.log(Level.FINER, "", e);
        } finally {
            Profiling.end(profilingEntry, 50);
        }
    }

    private void fire(final Method method, final Object[] args,
        CoreListener listener)
    {
        ProfilingEntry profilingEntry = null;
        if (Profiling.ENABLED) {
            profilingEntry = Profiling.start(listener.getClass().getName()
                + ':' + method.getName(), "");
        }
        try {
            method.invoke(listener, args);
        } catch (Exception e) {
            ListenerSupportFactory.LOG.log(Level.SEVERE,
                "Received an exception from listener '" + listener
                    + "', class '" + listener.getClass().getName() + '\'', e);
            // Also log original exception
            ListenerSupportFactory.LOG.log(Level.FINER, "", e);
        } finally {
            Profiling.end(profilingEntry, 50);
        }
    }

    void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }
}