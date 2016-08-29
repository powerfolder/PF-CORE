/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: Folder.java 8681 2009-07-19 00:07:45Z tot $
 */
package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.AccountStateChangedProto;
import de.dal33t.powerfolder.protocol.MemberInfoProto;
import de.dal33t.powerfolder.util.Reject;


/**
 * Used to inform that a Account has changed on a node.
 * <P>
 * This event triggers for example the refresh of the account data of that node.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class AccountStateChanged extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private MemberInfo node;
    private int ttl;

    public AccountStateChanged(MemberInfo node, int ttl) {
        super();
        Reject.ifNull(node, "Node is null");
        Reject.ifFalse(ttl > 0 && ttl < 10, "Illegal value for ttl");
        this.node = node;
        this.ttl = ttl;
    }

    public MemberInfo getNode() {
        return node;
    }

    public int getTTL() {
        return ttl;
    }

    public void decreaseTTL() {
        ttl--;
    }

    public boolean isAlive() {
        return !isDead();
    }

    public boolean isDead() {
        return ttl <= 0;
    }

    @Override
    public String toString() {
        return "AccountStateChanged on " + node + " TTL=" + ttl;
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof AccountStateChangedProto.AccountStateChanged) {
            AccountStateChangedProto.AccountStateChanged proto = (AccountStateChangedProto.AccountStateChanged)mesg;
            this.node = new MemberInfo(proto.getMember());
          }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage toD2D() {
        AccountStateChangedProto.AccountStateChanged.Builder builder = AccountStateChangedProto.AccountStateChanged.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setMember((MemberInfoProto.MemberInfo)this.node.toD2D());
        return builder.build();
    }

}
