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
import de.dal33t.powerfolder.message.*;
import de.dal33t.powerfolder.net.AbstractSocketConnectionHandler;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.ConnectionHandlerFactory;
import de.dal33t.powerfolder.protocol.AnyMessageProto;
import de.dal33t.powerfolder.protocol.FolderFilesChangedProto;

/**
 * Handler for relayed connections to other clients. NO encrypted transfer.
 * <p>
 * TRAC #597.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.72 $
 */
public class D2DSocketConnectionHandler extends AbstractSocketConnectionHandler
    implements ConnectionHandler {

    /* Define full packages here; might be required in the future */
    private final String[] PACKAGES = new String[] {
        "de.dal33t.powerfolder.message.%s",
        "de.dal33t.powerfolder.clientserver.%s"
    };

    /**
     * Builds a new D2D connection manager for the socket.
     * <p>
     * Should be called from <code>ConnectionHandlerFactory</code> only.
     *
     * @see ConnectionHandlerFactory
     * @param controller
     *            The {@link controller}
     * @param socket
     *            The {@link socket}
     * @throws ConnectionException
     **/

    public D2DSocketConnectionHandler(Controller controller, Socket socket) {
        super(controller, socket);
    }

    /**
     * deserialize Deserialize data and convert to object
     * 
     * @param data
     *            Data to deserialize
     * @param len
     *            Length of data
     * @return Returns the serialized object
     * @throws {@link
     *             ConnectionException} when an error occurred
     **/

    @Override
    protected Object deserialize(byte[] data, int len)
        throws ClassNotFoundException, ConnectionException {

        String klassName = "unknown";

        if (isFiner()) {
            logFiner("Got message; parsing it..");
        }

        try {
            AnyMessageProto.AnyMessage anyMessage = AnyMessageProto.AnyMessage
                .parseFrom(data);

            /* Assemble name and package */
            klassName = anyMessage.getClazzName();
            String klassPkg = String.format(
                "de.dal33t.powerfolder.protocol.%sProto$%s", klassName,
                klassName);

            // Workaround for FolderFilesChanged (protected variables cannot be
            // set via reflection)
            if (klassName.equals("FolderFilesChanged")) {
                FolderFilesChangedProto.FolderFilesChanged folderFilesChangedProto = 
                    FolderFilesChangedProto.FolderFilesChanged.parseFrom(data);

                FolderFilesChangedExt folderFilesChangedExt = new FolderFilesChangedExt();
                folderFilesChangedExt.initFromD2D(folderFilesChangedProto);

                return folderFilesChangedExt;
            }

            /* Try to create D2D message */
            Class<?> klass = Class.forName(klassPkg);
            Method meth = klass.getMethod("parseFrom", byte[].class);
            AbstractMessage amesg = (AbstractMessage) meth.invoke(null, data);

            /* Try to find klass in package list and might
             * cause a NPE when no matching class can be found */
            for(String pkg : PACKAGES) {
                try {
                    klass = Class.forName(String.format(pkg, klassName));
                } catch (ClassNotFoundException e) {
                    /* We ignore that here */
                }
                
                if(null != klass) break; ///< Exit when done
            }
            
            meth = klass.getMethod("initFromD2D", AbstractMessage.class);

            Object mesg = klass.newInstance();

            meth.invoke(mesg, amesg); ///< Call initFromD2DMessage

            return mesg;
        } catch (NoSuchMethodException | SecurityException
            | IllegalArgumentException | InvocationTargetException
            | InstantiationException | IllegalAccessException
            | InvalidProtocolBufferException | NullPointerException e)
        {
            if (isFiner()) {
                logFiner("Cannot read message(" + klassName + "): " + e.toString());
            }

            throw new ConnectionException(
                "Unable to read message from peer, connection closed", e)
                    .with(this);
        }
    }

    /**
     * serialize Serialize message data
     * 
     * @param message
     *            {@link Message} to serialize
     * @return Serialized byte data
     * @throws ConnectionException
     **/

    @Override
    protected byte[] serialize(Message mesg) throws ConnectionException {
        // Block the AddFriendNotification message
        if (mesg instanceof AddFriendNotification) {
            mesg = new Ping();
        }

        byte[] data = null;

        if (mesg instanceof D2DObject) {
            AbstractMessage amesg = ((D2DObject) mesg).toD2D();

            if (isFiner()) {
                logFiner("Sent " + amesg.getClass().getCanonicalName());
            }

            data = amesg.toByteArray();
        } else {
            throw new ConnectionException(
                "Message " + mesg.getClass().getSimpleName()
                    + " does not implement D2Object").with(this);
        }

        return data;
    }

    /**
     * createOwnIdentity Create identity
     * 
     * @return Own {@link Identity}
     **/

    @Override
    protected Identity createOwnIdentity() {
        return new Identity(getController(),
            getController().getMySelf().getInfo(), getMyMagicId(), false, false,
            this);
    }
}