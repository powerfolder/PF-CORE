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
package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.util.logging.Loggable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Helper class to handle message listener/firing
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.8 $
 */
public class MessageListenerSupport extends Loggable {

    // AWT system check
    private static final boolean AWT_AVAILABLE = Util.isAwtAvailable();

    private Loggable source;

    // Listeners for incoming messages
    private final Map<Class<?>, CopyOnWriteArrayList<MessageListener>> messageListenersInDispatchThread;
    private final Map<Class<?>, CopyOnWriteArrayList<MessageListener>> messageListenersNotInDispatchThread;

    /**
     * Initializes the the message listener support with a logger from the
     * parent
     *
     * @param source
     *            the source
     */
    public MessageListenerSupport(Loggable source) {
        this.source = source;
        Reject.ifNull(source, "Source");
        messageListenersNotInDispatchThread = new ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<MessageListener>>(
            2, 0.75f, 8);
        messageListenersInDispatchThread = new ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<MessageListener>>(
            2, 0.75f, 8);
    }

    // Message listener de-/registering ***************************************

    /**
     * Adds a message listener, which is triggerd on all received message
     *
     * @param aListener
     */
    public void addMessageListener(MessageListener aListener) {
        addMessageListener(All.class, aListener);
    }

    /**
     * Adds a message listener, which is only triggerd if a message of type
     * <code>messageType</code> is received.
     *
     * @param messageType
     * @param aListener
     */
    public synchronized void addMessageListener(Class<?> messageType,
        MessageListener aListener)
    {
        if (aListener == null) {
            return;
        }
        CopyOnWriteArrayList<MessageListener> listeners;
        if (aListener.fireInEventDispatchThread()) {
            listeners = messageListenersInDispatchThread.get(messageType);
        } else {
            listeners = messageListenersNotInDispatchThread.get(messageType);
        }
        if (listeners == null) {
            // Build new list for this type of message
            listeners = new CopyOnWriteArrayList<MessageListener>();
            if (aListener.fireInEventDispatchThread()) {
                messageListenersInDispatchThread.put(messageType, listeners);
            } else {
                messageListenersNotInDispatchThread.put(messageType, listeners);
            }
        }
        listeners.addIfAbsent(aListener);
    }

    /**
     * Removes a message listener completely from the message listener support
     *
     * @param aListener
     */
    public void removeMessageListener(MessageListener aListener) {
        if (aListener.fireInEventDispatchThread()) {
            synchronized (messageListenersInDispatchThread) {
                for (Collection<MessageListener> listeners : messageListenersInDispatchThread
                    .values())
                {
                    listeners.remove(aListener);
                }
            }
        } else {
            synchronized (messageListenersNotInDispatchThread) {
                for (Collection<MessageListener> listeners : messageListenersNotInDispatchThread
                    .values())
                {
                    listeners.remove(aListener);
                }
            }
        }
    }

    /**
     * Removes all message listener
     */
    public void removeAllListeners() {
        synchronized (messageListenersInDispatchThread) {
            for (Collection<MessageListener> listeners : messageListenersInDispatchThread
                .values())
            {
                listeners.clear();
            }
        }
        synchronized (messageListenersNotInDispatchThread) {
            for (Collection<MessageListener> listeners : messageListenersNotInDispatchThread
                .values())
            {
                listeners.clear();
            }
        }
    }

    // Message fire code ******************************************************

    /**
     * Fires a message to all message listeners
     *
     * @param theSource
     *            the source member from the message
     * @param message
     *            the message to fire
     */
    public void fireMessage(final Member theSource, final Message message) {
        if (message == null) {
            return;
        }
        if (theSource == null) {
            throw new NullPointerException(
                "Unable to fire message, source is null");
        }

        // Fire general listener
        Collection<MessageListener> generalListeners = messageListenersNotInDispatchThread
            .get(All.class);
        if (generalListeners != null && !generalListeners.isEmpty()) {
            for (MessageListener genListener : generalListeners) {
                try {
                    genListener.handleMessage(theSource, message);
                } catch (Exception e) {
                    logWarning(source
                        + ": Exception while handling message in listener of "
                        + theSource + ". msg: " + message + ". " + e, e);
                }
            }
        }

        // Fire special listeners
        Class messageClass = message.getClass();
        do {
            Collection<MessageListener> specialListeners = messageListenersNotInDispatchThread.get(messageClass);
            if (specialListeners != null && !specialListeners.isEmpty()) {
                for (MessageListener specListener : specialListeners) {
                    try {
                        specListener.handleMessage(theSource, message);
                    } catch (Exception e) {
                        logWarning(source
                                + ": Exception while handling message in listener of "
                                + theSource + ". msg: " + message + ". " + e, e);
                    }
                }
            }
            messageClass = messageClass.getSuperclass();
        } while (messageClass != null && !messageClass.equals(Message.class) && !messageClass.equals(Object.class));

        // java.util.List<E>
        final List<MessageListener> generalEDTListener = messageListenersInDispatchThread.get(All.class);

        List<MessageListener> specialEDTListener = null;
        messageClass = message.getClass();
        do {
            List<MessageListener> possibleSpecialEDTListener = messageListenersInDispatchThread.get(messageClass);
            if (!possibleSpecialEDTListener.isEmpty()) {
                if (specialEDTListener == null) {
                    specialEDTListener = new LinkedList<>(possibleSpecialEDTListener);
                } else {
                    specialEDTListener.addAll(possibleSpecialEDTListener);
                }
            }
            messageClass = messageClass.getSuperclass();
        } while (messageClass != null && !messageClass.equals(Message.class) && !messageClass.equals(Object.class));

        boolean executeEDT = (generalEDTListener != null && !generalEDTListener.isEmpty())
            || (specialEDTListener != null && !specialEDTListener.isEmpty());
        if (!executeEDT) {
            // SKIP EDT executing.
            return;
        }

        final List<MessageListener> finalSpecialEDTListener = specialEDTListener;
        Runnable edtRunner = () -> {
            if (generalEDTListener != null && !generalEDTListener.isEmpty()) {
                for (MessageListener genListener : generalEDTListener) {
                    try {
                        genListener.handleMessage(theSource, message);
                    } catch (Exception e) {
                        logWarning(source + ": Exception while handling message in listener of "
                                + theSource + ". msg: " + message + ". " + e, e);
                    }
                }
            }

            // Fire special listeners
            if (finalSpecialEDTListener != null && !finalSpecialEDTListener.isEmpty()) {
                for (MessageListener specListener : finalSpecialEDTListener) {
                    try {
                        specListener.handleMessage(theSource, message);
                    } catch (Exception e) {
                        logWarning(source + ": Exception while handling message in listener of "
                                + theSource + ". msg: " + message + ". " + e, e);
                    }
                }
            }
        };
        if (!AWT_AVAILABLE || EventQueue.isDispatchThread()) {
            // No awt system ? do not put in swing thread
            // Already in swing thread ? also don't wrap
            edtRunner.run();
        } else {
            // Put runner in swingthread
            SwingUtilities.invokeLater(edtRunner);
        }
    }

    private static class All {
    }
}