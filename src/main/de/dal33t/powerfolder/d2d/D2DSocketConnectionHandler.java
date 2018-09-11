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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.*;
import de.dal33t.powerfolder.net.*;
import de.dal33t.powerfolder.protocol.AnyMessageProto;
import de.dal33t.powerfolder.protocol.FolderFilesChangedProto;
import de.dal33t.powerfolder.transfer.LimitedInputStream;
import de.dal33t.powerfolder.transfer.LimitedOutputStream;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Reject;

import java.io.EOFException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class D2DSocketConnectionHandler extends AbstractSocketConnectionHandler
    implements ConnectionHandler {

    private NodeStateMachine nodeStateMachine = NodeStateMachine.build(null);
    // Socket acceptor to accept connection after identity was received
    private ConnectionListener.SocketAcceptor socketAcceptor;

    /* Define full packages here; might be required in the future */
    private final String[] PACKAGES = new String[] {
        "de.dal33t.powerfolder.message.%s",
        "de.dal33t.powerfolder.message.clientserver.%s",
        "de.dal33t.powerfolder.util.delta.%s"
    };

    /**
     * Builds a new D2D connection manager for the socket.
     * <p>
     * Should be called from <code>ConnectionHandlerFactory</code> only.
     *
     * @see ConnectionHandlerFactory
     * @param controller
     *            The {@link Controller}
     * @param socket
     *            The {@link Socket}
     **/

    public D2DSocketConnectionHandler(Controller controller, Socket socket) {
        super(controller, socket);
    }

    public ConnectionListener.SocketAcceptor getSocketAcceptor() {
        return socketAcceptor;
    }

    public void setSocketAcceptor(ConnectionListener.SocketAcceptor socketAcceptor) {
        this.socketAcceptor = socketAcceptor;
    }

    /**
     * Initializes the connection handler.
     *
     * @throws ConnectionException ¯\_(ツ)_/¯
     */
    @Override
    public void init() throws ConnectionException {
        if (socket == null) {
            throw new NullPointerException("Socket is null");
        }
        if (socket.isClosed() || !socket.isConnected()) {
            throw new ConnectionException("Connection to peer is closed")
                    .with(this);
        }
        this.started = true;
        this.identityReply = null;
        this.messagesToSendQueue = new ConcurrentLinkedQueue<>();
        this.senderSpawnLock = new ReentrantLock();

        try {
            out = new LimitedOutputStream(getController().getTransferManager().getOutputLimiter(this), socket.getOutputStream());
            in = new LimitedInputStream(getController().getTransferManager().getInputLimiter(this), socket.getInputStream());
            // Start receiver
            getController().getIOProvider().startIO(new D2DSocketConnectionHandler.Receiver());
            // Send identity
            sendMessagesAsynchron(createOwnIdentity());
        } catch (IOException e) {
            throw new ConnectionException("Unable to open connection: " + e.getMessage(), e).with(this);
        }
        getController().getIOProvider().startKeepAliveCheck(this);
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

        if (isFiner()) {
            logFiner("Got message; parsing it..");
        }

        String klassName = "unknown";
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
            Method method = klass.getMethod("parseFrom", byte[].class);
            AbstractMessage abstractMessage = (AbstractMessage) method.invoke(null, (Object) data);

            // Translate new names defined in protocol files to old message names
            if (klassName.equals("DownloadAbort")) {
                klassName = "AbortDownload";
            } else if (klassName.equals("DownloadRequest")) {
                klassName = "RequestDownload";
            } else if (klassName.equals("FilePartInfo")) {
                klassName = "PartInfo";
            } else if (klassName.equals("FilePartInfoList")) {
                klassName = "FilePartsRecord";
            } else if (klassName.equals("FilePartInfoListReply")) {
                klassName = "ReplyFilePartsRecord";
            } else if (klassName.equals("FilePartInfoListRequest")) {
                klassName = "RequestFilePartsRecord";
            } else if (klassName.equals("FilePartReply")) {
                klassName = "FileChunk";
            } else if (klassName.equals("FilePartRequest")) {
                klassName = "RequestPart";
            } else if (klassName.equals("NodeInfo")) {
                klassName = "MemberInfo";
            } else if (klassName.equals("NodeList")) {
                klassName = "KnownNodes";
            } else if (klassName.equals("NodeListRequest")) {
                klassName = "RequestNodeList";
            } else if (klassName.equals("UploadAbort")) {
                klassName = "AbortUpload";
            } else if (klassName.equals("UploadStart")) {
                klassName = "StartUpload";
            } else if (klassName.equals("UploadStop")) {
                klassName = "StopUpload";
            }

            /* Try to find klass in package list and might
             * cause a NPE when no matching class can be found */
            for(String pkg : PACKAGES) {
                try {
                    klass = Class.forName(String.format(pkg, klassName));
                } catch (ClassNotFoundException e) {
                    klass = null; ///< Make this this fails
                }
                
                if(null != klass) break; ///< Exit when done
            }
            
            method = klass.getMethod("initFromD2D", AbstractMessage.class);

            Object message = klass.newInstance();

            method.invoke(message, abstractMessage); ///< Call initFromD2DMessage

            return message;
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
    protected byte[] serialize(Message message) throws ConnectionException {
        // Block unsupported messages
        if (message instanceof AddFriendNotification) {
            message = new Ping();
        } else if (message instanceof KnownNodes) {
            message = new Ping();
        } else if (message instanceof RelayedMessageExt) {
            message = new Ping();
        } else if (message instanceof Invitation) {
            message = new Ping();
        } else if (message instanceof Problem) {
            message = new Ping();
        } else if (message instanceof RequestNodeList) {
            message = new Ping();
        } else if (message instanceof TransferStatus) {
            message = new Ping();
        } else if (message instanceof UDTMessage) {
            message = new Ping();
        }

        byte[] data = null;

        if (message instanceof D2DObject) {
            AbstractMessage abstractMessage = ((D2DObject) message).toD2D();

            if (isFiner()) {
                logFiner("Sent " + abstractMessage.getClass().getCanonicalName());
            }

            data = abstractMessage.toByteArray();
        } else {
            throw new ConnectionException(
                "Message " + message.getClass().getSimpleName()
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

    @Override
    public boolean acceptIdentity(Member node) {
        this.nodeStateMachine.setNode(node);
        Reject.ifNull(node, "node is null");
        member = node;
        if (isFiner()) {
            logFiner("Sending accept of identity to " + this);
        }
        sendMessagesAsynchron(IdentityReply.accept());
        return true;
    }

    /**
     * Receiver, responsible to deserialize messages
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.72 $
     */
    class Receiver implements Runnable {

        @Override
        public void run() {
            byte[] sizeArr = new byte[4];
            while (started) {
                // check connection status
                if (!isConnected()) {
                    break;
                }

                try {
                    // Read data header, total size
                    read(in, sizeArr, 0, sizeArr.length);
                    int totalSize = Convert.convert2Int(sizeArr);
                    if (!started) {
                        // Do not process this message
                        break;
                    }
                    if (totalSize <= 0) {
                        throw new IOException("Illegal packet size: " + totalSize);
                    }
                    byte[] data = serializer.read(in, totalSize);
                    Object obj = deserialize(data, totalSize);
                    if (obj instanceof D2DObject) {
                        if (obj instanceof Identity) {
                            // I know this is really ugly but ¯\_(ツ)_/¯
                            synchronized (identityWaiter) {
                                identity = (Identity) obj;
                                identityWaiter.notifyAll();
                            }
                            getSocketAcceptor().acceptConnection(D2DSocketConnectionHandler.this);
                        }
                        nodeStateMachine.fire(NodeEvent.getEnum((D2DObject) obj), obj);
                        continue;
                    }
                    lastKeepaliveMessage = new Date();
                    getController().getTransferManager().getTotalDownloadTrafficCounter().bytesTransferred(totalSize);

                    if (!getController().isStarted() || member == null || !isConnected()) {
                        break;
                    }
                } catch (SocketTimeoutException e) {
                    logFiner("Socket timeout on read, not disconnecting. " + e);
                } catch (SocketException | EOFException e) {
                    logConnectionClose(e);
                    // connection closed
                    break;
                } catch (InvalidClassException e) {
                    logFiner("InvalidClassException", e);
                    String from = getMember() != null
                            ? getMember().getNick()
                            : this.toString();
                    logWarning("Received unknown packet/class: "
                            + e.getMessage() + " from " + from);
                    // do not break connection
                } catch (InvalidObjectException e) {
                    logFiner("InvalidObjectException", e);
                    String from = getMember() != null
                            ? getMember().getNick()
                            : toString();
                    logWarning("Received invalid object: " + e.getMessage()
                            + " from " + from);
                    // do not break connection
                } catch (IOException e) {
                    logFiner("IOException", e);
                    logConnectionClose(e);
                    break;
                } catch (ConnectionException e) {
                    logFiner("ConnectionException", e);
                    logConnectionClose(e);
                    break;
                } catch (ClassNotFoundException e) {
                    logFiner("ClassNotFoundException", e);
                    logWarning("Received unknown packet/class: "
                            + e.getMessage() + " from "
                            + D2DSocketConnectionHandler.this);
                    // do not break connection
                } catch (RuntimeException e) {
                    logSevere("RuntimeException. " + e, e);
                    shutdownWithMember();
                    throw e;
                }
            }

            // Shut down
            shutdownWithMember();
        }
    }

}