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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * Helper class to handle message listener/firing
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.8 $
 */
public class MessageListenerSupport {
    private Loggable source;
    // Listeners for incoming messages
    private Map<Class<?>, CopyOnWriteArrayList<MessageListener>> messageListener;

    /**
     * Initializes the the message listener support with a logger from the
     * parent
     * 
     * @param source
     *            the source
     */
    public MessageListenerSupport(Loggable source) {
        this.source = source;
        if (this.source == null) {
            throw new NullPointerException("Source in null");
        }
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
    public void addMessageListener(Class<?> messageType,
        MessageListener aListener)
    {
        if (aListener == null) {
            return;
        }
        synchronized (this) {
            CopyOnWriteArrayList<MessageListener> listeners = getListenersMap()
                .get(messageType);
            if (listeners == null) {
                // Build new list for this type of message
                listeners = new CopyOnWriteArrayList<MessageListener>();
                getListenersMap().put(messageType, listeners);
            }
            listeners.addIfAbsent(aListener);
        }
    }

    /**
     * Removes a message listener completely from the message listener support
     * 
     * @param aListener
     */
    public void removeMessageListener(MessageListener aListener) {
        if (messageListener == null) {
            return;
        }
        synchronized (this) {
            for (Collection<MessageListener> listeners : messageListener
                .values())
            {
                listeners.remove(aListener);
            }
        }
    }

    /**
     * Removes all message listener
     */
    public void removeAllListeners() {
        if (messageListener == null) {
            return;
        }
        synchronized (this) {
            // Remove message listeners
            for (Collection<MessageListener> listeners : messageListener
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
    public void fireMessage(Member theSource, Message message) {
        if (message == null) {
            return;
        }
        if (messageListener == null) {
            // No Listener / No-one to fire to.
            return;
        }
        if (messageListener.isEmpty()) {
            return;
        }
        if (theSource == null) {
            throw new NullPointerException(
                "Unable to fire message, source is null");
        }

        int lGenCount = 0;
        int lSpcCount = 0;
        // Fire general listener

        Collection<MessageListener> generalListeners = messageListener
            .get(All.class);
        if (generalListeners != null && !generalListeners.isEmpty()) {
            for (MessageListener genListener : generalListeners) {
                genListener.handleMessage(theSource, message);
                lGenCount++;
            }
        }

        // Fire special listeners
        Collection<MessageListener> specialListeners = messageListener
            .get(message.getClass());
        if (specialListeners != null && !specialListeners.isEmpty()) {
            for (MessageListener sepcListener : specialListeners) {
                sepcListener.handleMessage(theSource, message);
                lSpcCount++;
            }
        }

        if (lSpcCount > 0 || lGenCount > 0) {
            // theSource.getLogger().verbose(
            // "Deligated message (" + message.getClass().getName() + ") to "
            // + lGenCount + " general and " + lSpcCount
            // + " special message listener");
        }
    }

    private synchronized Map<Class<?>, CopyOnWriteArrayList<MessageListener>> getListenersMap()
    {
        if (messageListener == null) {
            messageListener = new ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<MessageListener>>();
        }
        return messageListener;
    }

    private class All {
    }
}