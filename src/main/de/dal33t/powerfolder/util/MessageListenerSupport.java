/* $Id: MessageListenerSupport.java,v 1.8 2005/05/09 15:08:14 totmacherr Exp $
 */
package de.dal33t.powerfolder.util;

import java.util.*;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;

/**
 * Helper class to handle message listener/firering
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.8 $
 */
public class MessageListenerSupport {
    private Loggable source;
    // Listeners for incoming messages
    private Map messageListener = Collections.synchronizedMap(new HashMap());

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
        Collection listeners = (Collection) messageListener.get(messageType);
        if (listeners == null) {
            // Build new list for this type of message
            listeners = Collections.synchronizedSet(new HashSet());
            messageListener.put(messageType, listeners);
        }

        synchronized (listeners) {
            // Check for duplicate listeners
            if (!listeners.contains(aListener)) {
                listeners.add(aListener);
            }
        }
//        String msgType = (messageType == null) ? "all messages" : messageType
//            .getName();
//        source.getLogger().verbose(
//            "Added message listener (" + aListener + ") for " + msgType);
    }

    /**
     * Removes a message listener completely from the message listner support
     * 
     * @param aListener
     */
    public synchronized void removeMessageListener(MessageListener aListener) {
        for (Iterator it = messageListener.values().iterator(); it.hasNext();) {
            Collection listeners = (Collection) it.next();
            listeners.remove(aListener);
        }
    }

    /**
     * Removes all message listener
     */
    public void removeAllListener() {
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
            Collection generalListeners = (Collection) messageListener
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
            Collection specialListeners = (Collection) messageListener
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
            theSource.getLogger().verbose(
                "Deligated message (" + message.getClass().getName() + ") to "
                    + lGenCount + " general and " + lSpcCount
                    + " special message listener");
        }
    }
}