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
 * $Id: FileInfo.java 5084 2008-08-24 20:46:56Z tot $
 */
package de.dal33t.powerfolder.light;

import java.io.Serializable;

/**
 * Contains important information about a server
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public class ServerInfo implements Serializable {
    private static final long serialVersionUID = 100L;
    public static final String PROPERTYNAME_NODE = "node";

    private MemberInfo node;
    private String webUrl;

    public MemberInfo getNode() {
        return node;
    }

    public void setNode(MemberInfo node) {
        this.node = node;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((node == null) ? 0 : node.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ServerInfo other = (ServerInfo) obj;
        if (node == null) {
            if (other.node != null)
                return false;
        } else if (!node.equals(other.node))
            return false;
        return true;
    }

    public String toString() {
        return "Server " + node + " web @ " + webUrl;
    }
}
