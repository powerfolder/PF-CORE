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
import java.net.URLEncoder;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import de.dal33t.powerfolder.util.Base64;
import de.dal33t.powerfolder.util.Reject;

/**
 * Contains important information about a server
 *
 * @author Christian Sprajc
 * @version $Revision$
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ServerInfo implements Serializable {
    private static final long serialVersionUID = 100L;
    public static final String PROPERTYNAME_ID = "id";
    public static final String PROPERTYNAME_NODE = "node";

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "memberInfo_id")
    private MemberInfo node;
    private String webUrl;
    private String httpTunnelUrl;

    protected ServerInfo() {
        // NOP - only for Hibernate
    }

    private ServerInfo(MemberInfo node, String webUrl, String httpTunnelUrl) {
        super();
        this.node = node;
        this.webUrl = webUrl;
        this.httpTunnelUrl = httpTunnelUrl;
        if (node != null) {
            // Cluster server
            this.id = node.id;
        } else {
            // Federated service
            this.id = webUrl;
        }
        Reject.ifBlank(this.id, "Unable to set ID of ServerInfo");
    }

    /**
     * PFC-2455: Creates a {@link ServerInfo} instance representing a server of
     * the local cluster.
     * 
     * @see #isClusterServer()
     * @param node
     *            the node information to connect to.
     * @param webUrl
     * @param httpTunnelUrl
     * @return an {@link ServerInfo} object that represents a local server.
     */
    public static ServerInfo newClusterServer(MemberInfo node, String webUrl,
        String httpTunnelUrl)
    {
        return new ServerInfo(node, webUrl, httpTunnelUrl);
    }

    /**
     * PFC-2455: Creates a {@link ServerInfo} instance representing a federated
     * service
     * 
     * @param webUrl
     * @param httpTunnelUrl
     * @return an {@link ServerInfo} object that represents the federated
     *         service.
     */
    public static ServerInfo newFederatedService(String webUrl,
        String httpTunnelUrl)
    {
        return new ServerInfo(null, webUrl, httpTunnelUrl);
    }

    /**
     * PFC-2455
     * 
     * @return true if this represents a server of the local cluster serving.
     */
    public boolean isClusterServer() {
        return node != null;
    }

    /**
     * PFC-2455
     * 
     * @return true if this represents a federated remote service.
     */
    public boolean isFederatedService() {
        return node == null;
    }

    public MemberInfo getNode() {
        return node;
    }

    public void setNode(MemberInfo node) {
        Reject.ifNull(node, "Node is null");
        this.node = node;
        this.id = node.id;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    /**
     * @param uri
     * @return the absolute URL for the URI at the server/service.
     */
    public String getURL(String uri) {
        String theBaseURL = webUrl;
        if (theBaseURL == null) {
            theBaseURL = "";
        }
        if (uri == null) {
            return theBaseURL;
        }
        boolean slashOk = uri.startsWith("/") || theBaseURL.endsWith("/");
        String slash = slashOk ? "" : "/";
        return theBaseURL + slash + uri;
    }

    /**
     * @param controller
     * @param folder
     *            the folder.
     * @return the URL to the given folder.
     */
    public String getURL(FolderInfo folder) {
        String uri = "/files/";
        String snippet = Base64.encode4URL(folder.getId());
        if (snippet.endsWith("=")) {
            snippet = snippet.substring(0, snippet.length() - 1);
        }
        if (snippet.endsWith("=")) {
            snippet = snippet.substring(0, snippet.length() - 1);
        }
        uri += URLEncode(snippet);
        return getURL(uri);
    }

    public String getHTTPTunnelUrl() {
        return httpTunnelUrl;
    }

    public void setHTTPTunnelUrl(String httpTunnelUrl) {
        this.httpTunnelUrl = httpTunnelUrl;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        if (isFederatedService()) {
            return webUrl;
        }
        return node.getNick();
    }

    public void migrateId() {
        if (node != null) {
            this.id = node.id;            
        }
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
        if (!(obj instanceof ServerInfo))
            return false;
        final ServerInfo other = (ServerInfo) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    public String toString() {
        if (isFederatedService()) {
            return "Federated service: " + webUrl;
        }
        return "Server " + node.nick + '/' + node.networkId + '/' + node.id
            + ", web: " + webUrl + ", tunnel: " + httpTunnelUrl;
    }
    
    private String URLEncode(String url) {
        try {
            String newUrl = URLEncoder.encode(url, "UTF-8");
            // FIX1: Corrected relative filenames including path separator /
            // FIX2: To make download servlet work with Apache proxy
            return newUrl.replace("%2F", "/").replace("+", "%20");
        } catch (Exception e) {
            return url;
        }
    }
}
