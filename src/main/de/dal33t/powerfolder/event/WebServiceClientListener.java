/* $Id: WebServiceClientListener.java,v 1.1 2006/01/08 00:06:31 totmacherr Exp $
 * 
 * Copyright (c) DAKOSY AG and Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.event;

/**
 * Listener interface for the PowerFolder webservice
 * 
 * @see de.dal33t.powerfolder.webservice.WebServiceClient
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.1 $
 */
public interface WebServiceClientListener {
    /**
     * Events that is fired when a new own status was received
     * 
     * @param event
     */
    void receivedOwnStatus(WebServiceClientEvent event);
}
