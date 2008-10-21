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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Helper class to handle message listener/firering
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.8 $
 */
public class MessageListenerSupport {
    // Listeners for incoming messages
    private Map<Class, Collection<MessageListener>> messageListener;

    // Message listener de-/registering ***************************************

    /**
     * Adds a message listener, which is triggerd on all received message
     * 
     * @param aListener
     */
    public synchronized void addMessageListener(MessageListener aListener) {
        addMessageListener(null, aListener);
    }

    /**
     * Adds a message listener, which is only triggerd if a message of type
     * <code>messageType</code> is received.
     * 
     * @param messageType
     * @param aListener
     */
    public synchronized void addMessageListener(Class messageType,
        MessageListener aListener)
    {
        if (aListener == null) {
            return;
        }
        Collection<MessageListener> listeners = getListenersMap()
            .get(messageType);
        if (listeners == null) {
            // Build new list for this type of message
            listeners = Collections
                .synchronizedSet(new HashSet<MessageListener>());
            getListenersMap().put(messageType, listeners);
        }

        synchronized (listeners) {
            // Check for duplicate listeners
            if (!listeners.contains(aListener)) {
                listeners.add(aListener);
            }
        }
        // String msgType = (messageType == null) ? "all messages" : messageType
        // .getName();
        // source.logFiner(
        // "Added message listener (" + aListener + ") for " + msgType);
    }

    /**
     * Removes a message listener completely from the message listner support
     * 
     * @param aListener
     */
    public synchronized void removeMessageListener(MessageListener aListener) {
        if (messageListener == null) {
            return;
        }
        for (Iterator it = messageListener.values().iterator(); it.hasNext();) {
            Collection listeners = (Collection) it.next();
            listeners.remove(aListener);
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
            for (Iterator it = messageListener.values().iterator(); it
                .hasNext();)
            {
                Collection listeners = (Collection) it.next();
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
        synchronized (this) {
            Collection<MessageListener> generalListeners = messageListener
                .get(null);
            if (generalListeners != null && !generalListeners.isEmpty()) {
                MessageListener[] genListener;
                synchronized (generalListeners) {
                    genListener = new MessageListener[generalListeners.size()];
                    generalListeners.toArray(genListener);
                }
                for (int i = 0; i < genListener.length; i++) {
                    genListener[i].handleMessage(theSource, message);
                    lGenCount++;
                }
            }

            // Fire special listeners
            Collection<MessageListener> specialListeners = messageListener
                .get(message.getClass());
            if (specialListeners != null && !specialListeners.isEmpty()) {
                MessageListener[] specListener;
                synchronized (specialListeners) {
                    specListener = new MessageListener[specialListeners.size()];
                    specialListeners.toArray(specListener);
                }
                for (int i = 0; i < specListener.length; i++) {
                    specListener[i].handleMessage(theSource, message);
                    lSpcCount++;
                }
            }
        }

        if (lSpcCount > 0 || lGenCount > 0) {
            // theSource.logFiner(
            // "Deligated message (" + message.getClass().getName() + ") to "
            // + lGenCount + " general and " + lSpcCount
            // + " special message listener");
        }
    }

    private synchronized Map<Class, Collection<MessageListener>> getListenersMap() {
        if (messageListener == null) {
            messageListener = Collections
                .synchronizedMap(new HashMap<Class, Collection<MessageListener>>(
                    1));
        }
        return messageListener;
    }
}