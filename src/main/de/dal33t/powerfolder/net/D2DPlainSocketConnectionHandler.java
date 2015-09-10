/*
 * Copyright 2015 Christian Sprajc. All rights reserved.
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
 * @author Christoph Kappel <kappel@powerfolder.com>
 * @version $Id$
 */
package de.dal33t.powerfolder.net;

import java.io.IOException;
import java.net.Socket;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.util.ByteSerializer;

/**
 * Handler for relayed connections to other clients. NO encrypted transfer.
 * <p>
 * TRAC #597.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.72 $
 */
public class D2DPlainSocketConnectionHandler extends
  PlainSocketConnectionHandler
{
  /**
   * Builds a new D2D connection manager for the socket.
   * <p>
   * Should be called from <code>ConnectionHandlerFactory</code> only.
   *
   * @see ConnectionHandlerFactory
   * @param  controller  The {@link controller}
   * @param  socket      The {@link socket}
   * @throws ConnectionException
   **/

  protected
  D2DPlainSocketConnectionHandler(Controller controller,
    Socket socket)
  {
    super(controller, socket);
  }

  /** deserialize
   * Deserialize data and convert to object
   * @param  data  Data to deserialize
   * @param  len   Length of data
   * @return Returns the serialized object
   * @throws {@link ConnectionException} when an error occurred
   */

  @Override
  protected Object
  deserialize(byte[] data,
    int len) throws ClassNotFoundException, ConnectionException
  {
    try
      {
        return ByteSerializer.deserializeStatic(data, false); ///< FIXME: Probably makes no sense to compress binary?
      }
    catch(IOException e)
      {
        throw new ConnectionException(
          "Unable to send message to peer, connection closed", e)
          .with(this);
      }
  }

  /** serialize
   * Serialize message data
   * @param  message  {@link Message} to serialize
   * @return Serialized byte data
   * @throws {@link ConnectionException} when an error occurred
   */

  @Override
  protected byte[]
  serialize(Message message) throws ConnectionException
  {
    try
      {
        ByteSerializer serializer = getSerializer();

        if(null == serializer)
          {
            throw new IOException("Connection already closed");
          }

        return serializer.serialize(message, false, -1);
      }
    catch(IOException e)
      {
        throw new ConnectionException(
          "Unable to send message to peer, connection closed", e)
          .with(this);
      }
  }
}