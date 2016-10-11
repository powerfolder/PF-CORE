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

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.IdentityReplyProto;

/**
 * Indicated the accept of the identity, which was sent
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class IdentityReply extends Message
  implements D2DObject
{
    private static final long serialVersionUID = 100L;

    public boolean accepted;
    public String message;

	public IdentityReply() {
		// Serialisation constructor
	}

    /**
     * Builds a new identity reply
     * @param accepted
     * @param message
     */
    private IdentityReply(boolean accepted, String message) {
        this.accepted = accepted;
        this.message = message;
    }

    /**
     * Builds a identity reply rejecting the
     * identity. a cause should be declared
     *
     * @param why
     * @return
     */
    public static IdentityReply reject(String why) {
        return new IdentityReply(false, why);
    }

    /**
     * Builds a identity reply, accpeting identity
     * @return
     */
    public static IdentityReply accept() {
        return new IdentityReply(true, null);
    }

    @Override
    public String toString() {
        String reply = accepted ? "accepted" : "rejected";
        return "Identity " + reply + (message == null ? "" : ": " + message);
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
      if(mesg instanceof IdentityReplyProto.IdentityReply)
        {
          IdentityReplyProto.IdentityReply proto = (IdentityReplyProto.IdentityReply)mesg;

          this.accepted = proto.getAccepted();
          this.message  = proto.getMessage();
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2D()
    {
      IdentityReplyProto.IdentityReply.Builder builder = IdentityReplyProto.IdentityReply.newBuilder();

      builder.setClazzName(this.getClass().getSimpleName());
      builder.setAccepted(this.accepted);
      builder.setMessage(null == this.message ? "" : this.message);

      return builder.build();
    }
}
