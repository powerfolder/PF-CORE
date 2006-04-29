/* $Id: WebServiceClientEvent.java,v 1.1 2006/01/08 00:06:31 totmacherr Exp $
 * 
 * Copyright (c) DAKOSY AG and Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.webservice.OwnStatus;
import de.dal33t.powerfolder.webservice.WebServiceClient;

/**
 * Events fired by web service client class
 * 
 * @see de.dal33t.powerfolder.webservice.WebServiceClient
 * @see de.dal33t.powerfolder.event.WebServiceClientListener
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.1 $
 */
public class WebServiceClientEvent extends EventObject {
    private OwnStatus ownStatus;
    
    /**
     * Constructs a event used to inform listener about new own status
     * @param status
     */
    public WebServiceClientEvent(WebServiceClient source, OwnStatus status) {
        super(source);
        ownStatus = status;
    }
    
    public OwnStatus getOwnStatus() {
        return ownStatus;
    }
}
