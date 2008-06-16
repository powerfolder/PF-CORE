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
package de.dal33t.powerfolder.net;

import java.io.IOException;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.util.ByteSerializer;

/**
 * A implementation of relayed connection that communicates non-encrypted.
 * <p>
 * TRAC #597
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class PlainRelayedConnectionHandler extends
    AbstractRelayedConnectionHandler
{

    protected PlainRelayedConnectionHandler(Controller controller,
        MemberInfo destination, long connectionId, Member relay)
    {
        super(controller, destination, connectionId, relay);
    }

    @Override
    protected Identity createOwnIdentity() {
        return new Identity(getController(), getController().getMySelf()
            .getInfo(), getMyMagicId(), false, true, this);
    }

    @Override
    protected byte[] serialize(Message message) throws ConnectionException {
        try {
            // Serialize message, don't compress on LAN
            // unless config says otherwise
            boolean compressed = !isOnLAN()
                || (isOnLAN() && getController().useZipOnLan());
            return getSerializer().serialize(message, compressed, -1);
        } catch (IOException e) {
            throw new ConnectionException(
                "Unable to send message to peer, connection closed", e)
                .with(this);
        }
    }

    @Override
    protected Object deserialize(byte[] data, int len)
        throws ConnectionException, ClassNotFoundException
    {
        boolean expectCompressed = !isOnLAN();
        try {
            return ByteSerializer.deserializeStatic(data, expectCompressed);
        } catch (IOException e) {
            throw new ConnectionException(
                "Unable to send message to peer, connection closed", e)
                .with(this);
        }
    }

    // Logger methods *********************************************************

    public String getLoggerName() {
        return "RelConHandler-" + getConnectionId() + " " + getRemote().nick;
    }
}
