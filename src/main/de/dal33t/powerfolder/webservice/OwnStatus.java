/* $Id: OwnStatus.java,v 1.1 2005/11/21 00:04:56 totmacherr Exp $
 * 
 * Copyright (c) DAKOSY AG and Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.webservice;

import java.util.Collection;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Message;

/**
 * Message received from the PowerFolder webservice via xmlrpc. Contai
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.1 $
 */
public class OwnStatus extends Message {
    private static final long serialVersionUID = 100L;
    public static final OwnStatus NO_SERVICE = new OwnStatus(Product.NONE);

    public enum Product {
        NONE, WEBSERVICE
    }

    public Collection<FolderInfo> folders;
    public Product product;

    public OwnStatus(Product product) {
        this.product = product;
    }

    public OwnStatus(Collection<FolderInfo> folders) {
        if (folders == null) {
            throw new NullPointerException("folders are null");
        }
        this.product = Product.WEBSERVICE;
        this.folders = folders;
    }

    // General ****************************************************************

    public String toString() {
        return "Own Status: " + product + ", folders: "
            + (folders != null ? folders.size() : 0);
    }
}
