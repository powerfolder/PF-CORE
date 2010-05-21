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
package de.dal33t.powerfolder.event;

import java.awt.EventQueue;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import de.dal33t.powerfolder.util.Profiling;
import de.dal33t.powerfolder.util.ProfilingEntry;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Factory used to created event/listener support upon eventlistner interfaces.
 * <P>
 * Created Listenersupport implementation maintains a listener list and handles
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
        .getLogger(ListenerSupportFactory.class.getName());

    // AWT system check
    private static final boolean AWT_AVAILABLE = UIUtil.isAwtAvailable();

    /**
     * Creates a listener support for the listener event interface.
     * <p>
     * All calls to methods on that object will fire that event to its
     * registered listeners.
     * <p>
     * 
     * @param <T>
     * @param listenerInterface
     * @return the event support
     */
    @SuppressWarnings("unchecked")
    public static <T> T createListenerSupport(Class<T> listenerInterface) {
        if (listenerInterface == null) {
            throw new NullPointerException("Listener interface is empty");
        }
        if (!listenerInterface.isInterface()) {
            throw new IllegalArgumentException(
                "Listener interface class is not an java Interface!");
        }
        ClassLoader cl = listenerInterface.getClassLoader();
        InvocationHandler handler = new ListenerSupportInvocationHandler<T>(
            listenerInterface);
        T listenerSupportImpl = (T) Proxy.newProxyInstance(cl,
            new Class[]{listenerInterface}, handler);
        LOG.finer("Created event listener support for interface '"
            + listenerInterface.getName() + '\'');
        return listenerSupportImpl;
    }

    /**
     * Suspends (or resumes) a listener support, it set to true this listener
     * support will not file events until set to false. The listener support has
     * to be created via <code>createListenerSupport</code> before. Also the
     * listener needs to implement the listener event interface. Otherwise an
     * exception is thrown
     * 
     * @param listenerSupport
     * @param suspended
     */
    @SuppressWarnings("unchecked")
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
     *            The listenerSupport where the listener should be added to.
     * @param listener
     *            The event listener to add.
     */
    public static void addListener(CoreListener listenerSupport,
        CoreListener listener)
    {
        addListener(listenerSupport, listener, false);
    }

    /**
     * Adds a listener to a listener support. The listener support has to be
     * created via <code>createListenerSupport</code> factory method. Also the
     * listener needs to implement the listener event interface otherwise an
     * exception is thrown (see ListenerSupportInvocationHandler.checkListener).
     * 
     * @param listenerSupport
     *            The listenerSupport where the listener should be added to.
     * @param listener
     *            The event listener to add.
     * @param weak
     *            Listener gets removed if not other references to it is hold
     *            except by this listener support (or any other weak or soft
     *            reference).
     */
    @SuppressWarnings("unchecked")
    public static void addListener(CoreListener listenerSupport,
        CoreListener listener, boolean weak)
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
            if (weak) {
                lsInvHandler.addWeakListener(listener);
            } else {
                lsInvHandler.addListener(listener);
            }
        } else {
            throw new IllegalArgumentException(
                "Listener support is not valid. Seems not to be created with createListenerSupport.");
        }
    }

    /**
     * Removes a listener from a listener support. The listener support has to
     * be created via <code>createListenerSupport</code> factory method. Also
     * the listener needs to implement the listener event interface otherwise an
     * exception is thrown (see ListenerSupportInvocationHandler.checkListener).
     * 
     * @param listenerSupport
     * @param listener
     */
    @SuppressWarnings("unchecked")
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
     * to be created via <code>createListenerSupport</code> before. Otherwise an
     * exception is thrown
     * 
     * @param listenerSupport
     */
    @SuppressWarnings("unchecked")
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
     * The invocation handler, which delegates fire event method calls to the
     * listener. Maybe suspended, in this state it will not fire events.
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private static class ListenerSupportInvocationHandler<T> implements
        InvocationHandler
    {
        private Class<T> listenerInterface;
        private List<CoreListener> listenersNonEDT;
        private List<Reference<CoreListener>> weakListenersNonEDT;
        private List<CoreListener> listenersInEDT;
        private List<Reference<CoreListener>> weaklistenersInEDT;
        private boolean suspended;

        /**
         * Creates an invocation handler which basically handles the event
         * support.
         * 
         * @param listenerInterface
         *            the listener event interface
         */
        private ListenerSupportInvocationHandler(Class<T> listenerInterface) {
            this.listenerInterface = listenerInterface;
            this.listenersInEDT = new CopyOnWriteArrayList<CoreListener>();
            this.listenersNonEDT = new CopyOnWriteArrayList<CoreListener>();
            this.weakListenersNonEDT = new CopyOnWriteArrayList<Reference<CoreListener>>();
            this.weaklistenersInEDT = new CopyOnWriteArrayList<Reference<CoreListener>>();
        }

        /**
         * Adds a listener to this support impl
         * 
         * @param listener
         */
        private void addListener(CoreListener listener) {
            if (checkListener(listener)) {
                // Okay, add listener
                if (listener.fireInEventDispatchThread()) {
                    listenersInEDT.add(listener);
                } else {
                    listenersNonEDT.add(listener);
                }
            }
        }

        /**
         * Adds a weak listener to this support impl. Listener gets removed if
         * not other references to it is hold except by this listener support
         * (or any other weak or soft referece).
         * 
         * @param listener
         */
        private void addWeakListener(CoreListener listener) {
            if (checkListener(listener)) {
                // Okay, add listener
                Reference<CoreListener> ref = new WeakReference<CoreListener>(
                    listener);
                if (listener.fireInEventDispatchThread()) {
                    weaklistenersInEDT.add(ref);
                } else {
                    weakListenersNonEDT.add(ref);
                }
            }
        }

        /**
         * Removes a listener from this support impl
         * 
         * @param listener
         */
        private void removeListener(CoreListener listener) {
            if (checkListener(listener)) {
                listenersInEDT.remove(listener);
                listenersNonEDT.remove(listener);
                for (Reference<CoreListener> ref : weaklistenersInEDT) {
                    CoreListener candidate = ref.get();
                    if (candidate != null && candidate.equals(listener)) {
                        weaklistenersInEDT.remove(ref);
                    }
                }
                for (Reference<CoreListener> ref : weakListenersNonEDT) {
                    CoreListener candidate = ref.get();
                    if (candidate != null && candidate.equals(listener)) {
                        weakListenersNonEDT.remove(ref);
                    }
                }
            }
        }

        /**
         * Removes all listeners from this support impl
         */
        private void removeAllListeners() {
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
        public Object invoke(Object proxy, final Method method,
            final Object[] args) throws Throwable
        {
            if (listenersInEDT.isEmpty() && listenersNonEDT.isEmpty()
                && weaklistenersInEDT.isEmpty()
                && weakListenersNonEDT.isEmpty())
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

        private void fireToNonEDTListeners(final Method method,
            final Object[] args)
        {
            for (CoreListener listener : listenersNonEDT) {
                fire(method, args, listener);
            }
        }

        private void fireToEDTListeners(final Method method, final Object[] args)
        {
            Runnable runner = new Runnable() {
                public void run() {
                    for (CoreListener listener : listenersInEDT) {
                        fire(method, args, listener);
                    }
                }
            };
            if (!AWT_AVAILABLE || EventQueue.isDispatchThread()) {
                // NO awt system ? do not put in swing thread
                // Already in swing thread ? also don't wrap
                runner.run();
            } else {
                // Put runner in swingthread
                SwingUtilities.invokeLater(runner);
            }
        }

        private void fireToWeakNonEDTListeners(final Method method,
            final Object[] args)
        {
            for (Reference<CoreListener> ref : weakListenersNonEDT) {
                CoreListener listener = ref.get();
                if (listener == null) {
                    LOG.warning("Removed weak GCed core listener");
                    weakListenersNonEDT.remove(listener);
                    continue;
                }
                fire(method, args, listener);
            }
        }

        private void fireToWeakEDTListeners(final Method method,
            final Object[] args)
        {
            Runnable runner = new Runnable() {
                public void run() {
                    for (Reference<CoreListener> ref : weaklistenersInEDT) {
                        CoreListener listener = ref.get();
                        if (listener == null) {
                            LOG.warning("Removed weak GCed EDT core listener");
                            weaklistenersInEDT.remove(listener);
                            continue;
                        }
                        fire(method, args, listener);
                    }
                }
            };
            if (!AWT_AVAILABLE || EventQueue.isDispatchThread()) {
                // NO awt system ? do not put in swing thread
                // Already in swing thread ? also don't wrap
                runner.run();
            } else {
                // Put runner in swingthread
                SwingUtilities.invokeLater(runner);
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
                LOG.log(Level.SEVERE, "Received an exception from listener '"
                    + listener + "', class '" + listener.getClass().getName()
                    + '\'', e);
                // Also log original exception
                LOG.log(Level.FINER, "", e);
            } finally {
                Profiling.end(profilingEntry, 50);
            }
        }

        private void setSuspended(boolean suspended) {
            this.suspended = suspended;
        }
    }

}
