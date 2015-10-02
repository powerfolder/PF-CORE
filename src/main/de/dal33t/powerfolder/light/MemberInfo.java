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
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.MemberInfoProto;
import de.dal33t.powerfolder.util.ExternalizableUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.db.InetSocketAddressUserType;
import de.dal33t.powerfolder.util.intern.Internalizer;
import de.dal33t.powerfolder.util.net.NetworkUtil;

/**
 * Member information class. contains all important informations about a member
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
@TypeDef(name = "socketAddressType", typeClass = InetSocketAddressUserType.class)
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class MemberInfo implements Serializable, D2DObject {
    private static final long serialVersionUID = 100L;
    public static Internalizer<MemberInfo> INTERNALIZER;

    public static final String PROPERTYNAME_NICK = "nick";
    public static final String PROPERTYNAME_ID = "id";
    public static final String PROPERTYNAME_CONNECT_ADDRESS = "connectAddress";

    // some identification marks
    public String nick;
    // The world wide unique id / logical address
    @Id
    public String id;
    /**
     * The network Id of this node. #1373
     *
     * @see ConfigurationEntry#NETWORK_ID
     */
    public String networkId;

    // last know address
    @Type(type = "socketAddressType")
    private InetSocketAddress connectAddress;
    /**
     * The last time a successful (physical) connection was possible. Just the
     * initial handshake. Node might not have been completely connected
     * afterwards because handshake was not completed, e.g. because
     * uninteresting.
     */
    private Date lastConnectTime;

    // flag if peer was connected at remote side
    public boolean isConnected;
    public boolean isSupernode;

    // Transient caches
    private transient Boolean hasNullIP;

    /**
     * The cached hash info.
     */
    private transient int hash;

    private MemberInfo() {
        // NOP - only for hibernate
    }

    public MemberInfo(String nick, String id, String networkId) {
        this.nick = nick;
        this.id = id;
        if (networkId != null) {
            this.networkId = networkId;
        } else {
            this.networkId = ConfigurationEntry.NETWORK_ID.getDefaultValue();
        }
    }

    /** MemberInfo
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    public
    MemberInfo(AbstractMessage mesg)
    {
      initFromD2D(mesg);
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

    public String getId() {
        return id;
    }

    public String getNick() {
        return nick;
    }

    public Date getLastConnectTime() {
        return lastConnectTime;
    }

    public void setLastConnectNow() {
        lastConnectTime = new Date();
    }

    /**
     * WARNING: FOR TESTS ONLY.
     */
    public void setLastConnectTime(Date date) {
        lastConnectTime = date;
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
        Reject.ifNull(controller, "Controller is null");
        Member node = getNode(controller, false);
        return node != null && node.isFriend();
    }

    /**
     * @param controller
     * @return true if this node is on the same network = same network Id.
     */
    public boolean isOnSameNetwork(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        if (controller.getNodeManager().getNetworkId()
            .equals(Constants.NETWORK_ID_ANY))
        {
            return true;
        }
        if (networkId.equals(Constants.NETWORK_ID_ANY)) {
            return true;
        }
        return controller.getNodeManager().getNetworkId().equals(networkId);
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
        // #1334
        // if (connectAddress == null || connectAddress.getAddress() == null) {
        // return true;
        // }
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

        return false;
        // #1334
        // if (hasNullIP == null) {
        // if (NULL_IP != null) {
        // // Using advanced check
        // hasNullIP = Boolean.valueOf(NULL_IP.equals(connectAddress
        // .getAddress()));
        // } else {
        // // Fallback, this works
        // byte[] addr = connectAddress.getAddress().getAddress();
        // hasNullIP = Boolean.valueOf((addr[0] & 0xff) == 0
        // && (addr[1] & 0xff) == 0 && (addr[2] & 0xff) == 0
        // && (addr[3] & 0xff) == 0);
        // }
        // }
        // return hasNullIP.booleanValue();
    }

    public boolean hasNullIP() {
        if (hasNullIP == null) {
            hasNullIP = NetworkUtil.isNullIP(connectAddress.getAddress());
        }
        return hasNullIP.booleanValue();
    }

    /**
     * Returns the full node/member for this member info. Return null if member
     * is not longer known to the NodeManager. If flagged with addIfNessesary a
     * new node will be created if not available @ NodeManager. Calling with
     * addIfNessesary = true always returns a valid node (Member).
     *
     * @param controller
     * @param addIfNessesary
     *            if a new node should be added if not available @ nodemanager
     * @return the node OR null if node was not found.
     */
    public Member getNode(Controller controller, boolean addIfNessesary) {
        Reject.ifNull(controller, "Controller is null");
        Member node = controller.getNodeManager().getNode(this);
        if (node == null && addIfNessesary) {
            node = controller.getNodeManager().addNode(this);
        }
        return node;
    }

    /*
     * General
     */

    public MemberInfo intern() {
        if (INTERNALIZER == null) {
            // No actual intern
            return this;
        }
        return INTERNALIZER.intern(this);
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            // Cache the hashcode
            hash = hashCode0();
        }
        return hash;
    }

    public int hashCode0() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
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

    @Override
    public String toString() {
        return "Member '" + nick + "' (con. at " + connectAddress + ")";
    }

    // Serialization optimization *********************************************

    private void readObject(ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();
        // this.id = id.intern();
        // this.nick = nick.intern();
        if (this.networkId == null) {
            this.networkId = ConfigurationEntry.NETWORK_ID.getDefaultValue();
        } else {
            // this.networkId = networkId.intern();
        }
    }

    private static final long extVersionUID = 100L;

    public static MemberInfo readExt(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.readExternal(in);
        return memberInfo;
    }

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        long extUID = in.readLong();
        if (extUID != extVersionUID) {
            throw new InvalidClassException(this.getClass().getName(),
                "Unable to read. extVersionUID(steam): " + extUID
                    + ", expected: " + extVersionUID);
        }
        id = in.readUTF();
        nick = in.readUTF();
        networkId = ExternalizableUtil.readString(in);
        if (networkId == null) {
            networkId = ConfigurationEntry.NETWORK_ID.getDefaultValue();
        }
        connectAddress = ExternalizableUtil.readAddress(in);
        lastConnectTime = ExternalizableUtil.readDate(in);
        isConnected = in.readBoolean();
        isSupernode = in.readBoolean();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(extVersionUID);
        out.writeUTF(id);
        out.writeUTF(nick);
        ExternalizableUtil.writeString(out, networkId);
        ExternalizableUtil.writeAddress(out, connectAddress);
        ExternalizableUtil.writeDate(out, lastConnectTime);
        out.writeBoolean(isConnected);
        out.writeBoolean(isSupernode);
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2D(AbstractMessage mesg)
    {
      if(mesg instanceof MemberInfoProto.MemberInfo)
        {
          MemberInfoProto.MemberInfo minfo = (MemberInfoProto.MemberInfo)mesg;

          this.nick            = minfo.getNick();
          this.id              = minfo.getId();
          this.networkId       = minfo.getNetworkId();

          /* Disassemble host:port string */
          String[] split = minfo.getConnectAddress().split(":");

          if(2 <= split.length)
            {
              this.connectAddress  = new InetSocketAddress(split[0],
                Integer.valueOf(split[1]));
            }

          this.lastConnectTime = new Date(minfo.getLastConnectTime());
          this.isConnected     = minfo.getIsConnected();
          this.isSupernode     = minfo.getIsSuperNode();
          this.hasNullIP       = minfo.getHasNullIP();
        }
    }

    /** toD2DMessage
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2D()
    {
      MemberInfoProto.MemberInfo.Builder builder = MemberInfoProto.MemberInfo.newBuilder();

      builder.setClassName("MemberInfo");
      builder.setNick(this.nick);
      builder.setId(this.id);
      builder.setNetworkId(this.networkId);
      builder.setConnectAddress(this.connectAddress.toString()); ///< Assemble to host:port
      builder.setLastConnectTime(this.lastConnectTime.getTime());
      builder.setIsConnected(this.isConnected);
      builder.setIsSuperNode(this.isSupernode);
      builder.setHasNullIP(this.hasNullIP);

      return builder.build();
    }
}