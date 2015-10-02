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

package de.dal33t.powerfolder.d2d;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.net.AbstractSocketConnectionHandler;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.ConnectionHandlerFactory;
import de.dal33t.powerfolder.protocol.AnyProto;

/**
 * Handler for relayed connections to other clients. NO encrypted transfer.
 * <p>
 * TRAC #597.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.72 $
 */
public class D2DSocketConnectionHandler extends
  AbstractSocketConnectionHandler implements ConnectionHandler
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

  public
  D2DSocketConnectionHandler(Controller controller,
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
   **/

  @Override
  protected Object
  deserialize(byte[] data,
    int len) throws ClassNotFoundException, ConnectionException
  {
    try
      {
        AnyProto.Any any = AnyProto.Any.parseFrom(data);

        /* Assemble name and package */
        String className = any.getClassName();
        String classPkg  = String.format(
          "de.dal33t.powerfolder.protocol.%sProto.%s",
          className, className);

        /* Try to create D2D message */
        Class<?>        klass = Class.forName(classPkg);
        Method          meth  = klass.getMethod("parseFrom");
        AbstractMessage amesg = (AbstractMessage)meth.invoke(data); ///< Call parseForm()

        /* Try to create message */
        classPkg = String.format("de.dal33t.powerfolder.message.%s", className);
        klass    = Class.forName(classPkg);
        meth     = klass.getMethod("initFromD2DMessage");

        Object mesg = klass.newInstance();

        meth.invoke(mesg, amesg); ///< Call initFromD2DMessage

        return mesg;
      }
    catch(NoSuchMethodException|SecurityException|IllegalArgumentException|
        InvocationTargetException|InstantiationException|
        IllegalAccessException|InvalidProtocolBufferException e)
      {
        throw new ConnectionException(
          "Unable to read message to peer, connection closed", e)
          .with(this);
      }
  }

  /** serialize
   * Serialize message data
   * @param  message  {@link Message} to serialize
   * @return Serialized byte data
   **/

  @Override
  protected byte[]
  serialize(Message mesg)
  {
    byte[] data = null;

    if(mesg instanceof D2DObject)
      {
        data = ((D2DObject)mesg).toD2D().toByteArray();
      }

    return data;
  }

  /** createOwnIdentity
   * Create identity
   * @return Own {@link Identity}
   **/

  @Override
  protected Identity
  createOwnIdentity()
  {
    return new Identity(getController(), getController().getMySelf()
      .getInfo(), getMyMagicId(), false, false, this);
  }
}