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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

import de.dal33t.powerfolder.util.Util;

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

    static final Logger LOG = Logger.getLogger(ListenerSupportFactory.class
        .getName());

    // AWT system check
    static final boolean AWT_AVAILABLE = Util.isAwtAvailable();

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
    @SuppressWarnings("rawtypes")
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
    @SuppressWarnings("rawtypes")
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
    @SuppressWarnings("rawtypes")
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
    @SuppressWarnings("rawtypes")
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

}
