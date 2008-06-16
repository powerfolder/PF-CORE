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
package de.dal33t.powerfolder.light;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Date;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.util.Util;

/**
 * Member information class. contains all important informations about a member
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
public class MemberInfo implements Serializable {
    private static final long serialVersionUID = 100L;
    // The IP only build from 0s
    // private static final String NULL_IP = "0.0.0.0";
    private static InetAddress NULL_IP;
    static {
        try {
            NULL_IP = InetAddress.getByAddress("0.0.0.0",
                new byte[]{0, 0, 0, 0});
        } catch (UnknownHostException e) {
            NULL_IP = null;
            e.printStackTrace();
        }
    }

    // some idenification marks
    public String nick;
    // The world wide unique id
    public String id;

    // last know address
    private InetSocketAddress connectAddress;
    /**
     * The last time a successfull (physical) connection was possible. Just the
     * initial handshake. Node might not have been completely connected
     * afterwards because handshake was not completed, e.g. because
     * uninteresting.
     */
    public Date lastConnectTime;

    // flag if peer was connected at remote side
    public boolean isConnected;
    public boolean isSupernode;

    // Transient caches
    private transient Boolean hasNullIP;

    public MemberInfo() {
    }

    public MemberInfo(String nick, String id) {
        this.nick = nick;
        this.id = id;
    }

    // Setter/Getter **********************************************************

    public void setConnectAddress(InetSocketAddress newConnectAddress) {
        if (Util.equals(connectAddress, newConnectAddress)) {
            // System.err.println("Skipping set of connect addres");
            return;
        }
        this.connectAddress = newConnectAddress;
        // Clear cache
        hasNullIP = null;
    }

    public InetSocketAddress getConnectAddress() {
        return this.connectAddress;
    }

    // Logic ******************************************************************

    /**
     * @param member
     * @return true if this memberinfo is equal to the memberinfo of the member.
     */
    public boolean matches(Member member) {
        if (member == null) {
            return false;
        }
        return Util.equals(member.getId(), id);
    }

    /**
     * @param searchString
     * @return if this member matches the search string or if it equals the IP
     *         nick contains the search String
     */
    public boolean matches(String searchString) {
        if (connectAddress != null && connectAddress.getAddress() != null) {
            String theIp = connectAddress.getAddress().getHostAddress();
            if (theIp != null && theIp.equals(searchString)) {
                return true;
            }
        }
        return nick.toLowerCase().indexOf(searchString.toLowerCase()) >= 0;
    }

    /**
     * @param controller
     * @return if this member is a friend
     */
    public boolean isFriend(Controller controller) {
        Member node = getNode(controller);
        return node != null && node.isFriend();
    }

    /**
     * @param controller
     *            the controller
     * @return if this member is invalid
     */
    public boolean isInvalid(Controller controller) {
        if (isFriend(controller)) {
            // Friends are never invalid
            return false;
        }
        // Check for valid address
        if (connectAddress == null || connectAddress.getAddress() == null) {
            return true;
        }
        // Check for timeout
        if (lastConnectTime == null) {
            return true;
        }

        if (System.currentTimeMillis() - lastConnectTime.getTime() >= Constants.NODE_TIME_TO_INVALIDATE)
        {
            return true;
        }
        if (lastConnectTime.getTime() > (System.currentTimeMillis() + Constants.NODE_TIME_MAX_IN_FUTURE))
        {
            // The last connect time lies to much in the future, not
            // possible!
            return true;
        }

        if (hasNullIP == null) {
            if (NULL_IP != null) {
                // Using advanced check
                hasNullIP = Boolean.valueOf(NULL_IP.equals(connectAddress
                    .getAddress()));
            } else {
                // Fallback, this works
                byte[] addr = connectAddress.getAddress().getAddress();
                hasNullIP = Boolean.valueOf((addr[0] & 0xff) == 0
                    && (addr[1] & 0xff) == 0 && (addr[2] & 0xff) == 0
                    && (addr[3] & 0xff) == 0);
            }
        }
        return hasNullIP.booleanValue();
    }

    /**
     * Returns the full node/member for this info item. May return null if
     * member is not longer available on nodemanager
     * 
     * @param controller
     * @return the node
     */
    public Member getNode(Controller controller) {
        return controller.getNodeManager().getNode(this);
    }

    /**
     * Returns the full node/member for this member info. May return null if
     * member is not longer available on nodemanager. If flagged with
     * addIfNessesary a new node will be created if not available @ nodemanager.
     * Calling with addIfNessesary = true always returns a valid node (Member).
     * @param controller
     * @param addIfNessesary
     * if a new node should be added if not available @ nodemanager
     * @return the node
     */
    public Member getNode(Controller controller, boolean addIfNessesary) {
        Member node = getNode(controller);
        if (node == null && addIfNessesary) {
            node = controller.getNodeManager().addNode(this);
        }
        return node;
    }

    /*
     * General
     */

    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MemberInfo) {
            MemberInfo other = (MemberInfo) obj;
            return Util.equals(id, other.id);
        }
        return false;
    }

    public String toString() {
        return "Member '" + nick + "' (con. at " + connectAddress + ")";
    }

    // Serialization optimization *********************************************

    private void readObject(ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();
        this.id = id.intern();
        this.nick = nick.intern();
    }
}