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

import java.net.Socket;

import de.dal33t.powerfolder.Controller;

public class
D2DConnectionListener
  extends ConnectionListener
{
  public static final int DEFAULT_PORT = 1338;

  /** D2DConnectionListener
   * Listen for D2D connections
   * @param  controller       Handling controller
   * @param  port             Port to bind to
   * @param  bindToInterface  Interface to bind to
   * @throws ConnectionException
   **/

  public
  D2DConnectionListener(Controller controller,
    int port,
    String bindToInterface) throws ConnectionException
  {
    super(controller, port, bindToInterface);
  }

  /* Acceptor class */
  private class
  D2DSocketAcceptor
    extends SocketAcceptor
  {
    /** D2DSocketAcceptor
     * Accept connection
     * @param  socket  @{link Socket} to use
     **/

    private
    D2DSocketAcceptor(Socket socket)
    {
      super(socket);
    }

    /** accept
     * Accept connection
     * @throws @{link ConnectionException} when an error occurred
     **/

    @Override
    protected void
    accept()
      throws ConnectionException
    {
      if(isFiner())
        {
          logFiner("Accepting D2D connection from: " +
            socket.getInetAddress() + ":" + socket.getPort());
        }

      ConnectionHandler handler = getController().getIOProvider()
        .getConnectionHandlerFactory()
        .createAndInitD2DSocketConnectionHandler(socket);

      /* Accept node */
      acceptConnection(handler);
    }
  };
}