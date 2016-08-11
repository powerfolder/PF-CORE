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
package de.dal33t.powerfolder.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.PingProto;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;

/**
 * A simple ping
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class Ping extends Message
    implements D2DObject
{
    // #462: implements Externalizable {
    private static final long serialVersionUID = 100L;
    private static final int MESSAGE_VERSION = 100;

    long sendTime;
    byte[] payload;
    public String id;

    /**
     * Constructs a null/empty ping. Also called by serialization process.
     */
    public Ping() {
        this(-1);
    }

    /**
     * @param payloadSize
     *            the size of the random payload. values <0 initalize a complete
     *            empty ping, even without id
     */
    public Ping(int payloadSize) {
        if (payloadSize >= 0) {
            // generates a unique id, to identify pong response
            id = IdGenerator.makeId();
            payload = new byte[payloadSize];
            // fill payload
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (Math.random() * 256);
            }
        } else {
            id = null;
            payload = null;
        }
    }

    public void sent() {
        sendTime = System.currentTimeMillis();
    }

    /*
     * General
     */

    @Override
    public String toString() {
        return "Ping"
            + ((payload != null) ? " " + Format.formatBytes(payload.length)
                + " bytes payload" : "");
    }

    // Serialization protoype code: #462 **************************************

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        // Version check
        int serializatedMessageVersion = in.readInt();
        if (serializatedMessageVersion != MESSAGE_VERSION) {
            throw new ClassNotFoundException("Incompatible class versions for "
                + getClass().getName() + ". remote version: "
                + serializatedMessageVersion + ", own version: "
                + MESSAGE_VERSION);
        }
        int utfSize = in.readInt();
        if (utfSize >= 0) {
            id = in.readUTF();
        } else {
            id = null;
        }
        int payloadSize = in.readInt();
        if (payloadSize == -1) {
            payload = null;
        } else {
            payload = new byte[payloadSize];
            in.read(payload);
        }
        payload = (byte[]) in.readObject();
        sendTime = in.readLong();
        in.close();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(MESSAGE_VERSION);

        if (id != null) {
            out.write(id.length());
            out.writeUTF(id);
        } else {
            out.write(-1);
        }

        if (payload != null) {
            out.writeInt(payload.length);
            out.write(payload);
        } else {
            out.writeInt(-1);
        }

        out.writeLong(sendTime);
        out.close();
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2D(AbstractMessage mesg)
    {
      if(mesg instanceof PingProto.Ping)
        {
          PingProto.Ping ping = (PingProto.Ping)mesg;

          this.sendTime = ping.getSendTime();
          this.payload  = ping.getPayload().getBytes();
          this.id       = ping.getId();
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
      PingProto.Ping.Builder builder = PingProto.Ping.newBuilder();

      builder.setClazzName("Ping");
      builder.setSendTime(sendTime);
      builder.setPayload(String.valueOf(payload));
      if (this.id != null) {
          builder.setId(id);
      }

      return builder.build();
    }
}