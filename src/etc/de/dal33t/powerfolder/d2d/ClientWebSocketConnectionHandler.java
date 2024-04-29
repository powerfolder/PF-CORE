package de.dal33t.powerfolder.d2d;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.*;
import de.dal33t.powerfolder.net.AbstractSocketConnectionHandler;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.protocol.AnyMessageProto;
import de.dal33t.powerfolder.protocol.FolderFilesChangedProto;
import de.dal33t.powerfolder.transfer.LimitedInputStream;
import de.dal33t.powerfolder.transfer.LimitedOutputStream;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.Reject;
import org.squirrelframework.foundation.exception.TransitionException;

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

public class ClientWebSocketConnectionHandler extends AbstractSocketConnectionHandler
        implements ConnectionHandler {

    private final NodeStateMachine nodeStateMachine = NodeStateMachine.build();

    private ConnectionListener.SocketAcceptor socketAcceptor;

    public ClientWebSocketConnectionHandler(Controller controller, Socket socket) {
        super(controller, socket);
    }

    public NodeStateMachine getNodeStateMachine() {
        return nodeStateMachine;
    }

    private ConnectionListener.SocketAcceptor getSocketAcceptor() {
        return socketAcceptor;
    }

    public void setSocketAcceptor(ConnectionListener.SocketAcceptor socketAcceptor) {
        this.socketAcceptor = socketAcceptor;
    }

    @Override
    public void init() throws ConnectionException {
        if (socket == null) {
            throw new NullPointerException("Socket is null");
        }
        if (!socket.isClosed() && socket.isConnected()) {
            this.started = true;
            this.identityReply = null;
            this.messagesToSendQueue = new ConcurrentLinkedQueue<>();
            this.senderSpawnLock = new ReentrantLock();

            try {
                out = new LimitedOutputStream(getController().getTransferManager().getOutputLimiter(this), socket.getOutputStream());
                in = new LimitedInputStream(getController().getTransferManager().getInputLimiter(this), socket.getInputStream());

                getController().getIOProvider().startIO(new ClientWebSocketConnectionHandler.Receiver());

                sendMessagesAsynchron(createOwnIdentity());
            } catch (IOException e) {
                throw new ConnectionException("Unable to open connection: " + e.getMessage(), e).with(this);
            }
            getController().getIOProvider().startKeepAliveCheck(this);
            return;
        }

        throw new ConnectionException("Connection to peer is closed")
                .with(this);
    }


    @Override
    protected byte[] serialize(Message message) throws ConnectionException {
        // Block unsupported messages
        switch (message.getClass().getSimpleName()) {
            case "AddFriendNotification":
            case "KnownNodes":
            case "RelayedMessageExt":
            case "Invitation":
            case "Problem":
            case "RequestNodeList":
            case "TransferStatus":
            case "UDTMessage":
                message = new Ping();
                break;
        }

        if (!(message instanceof D2DObject)) {
            throw new ConnectionException("Message " + message.getClass().getSimpleName() + " does not implement D2DObject" + message).with(this);
        }

        AbstractMessage abstractMessage = ((D2DObject) message).toD2D();
        if (isFiner()) {
            logFiner("Sent " + abstractMessage.getClass().getCanonicalName());
        }

        return abstractMessage.toByteArray();
    }

    @Override
    protected Object deserialize(byte[] data, int len) throws ClassNotFoundException, ConnectionException {
        if (isFiner()) {
            logFiner("Got message; parsing it..");
        }

        String klassName = "unknown";
        try {
            AnyMessageProto.AnyMessage anyMessage = AnyMessageProto.AnyMessage.parseFrom(data);

            klassName = anyMessage.getClazzName();
            String klassPkg = String.format("de.dal33t.powerfolder.protocol.%sProto$%s", klassName, klassName);

            if (klassName.equals("FolderFilesChanged")) {
                return handleFolderFilesChanged(data);
            }

            Class<?> klass = Class.forName(klassPkg);
            Method method = klass.getMethod("parseFrom", byte[].class);
            AbstractMessage abstractMessage = (AbstractMessage) method.invoke(null, (Object) data);

            klassName = translateClassName(klassName);


            method = klass.getMethod("initFromWebClient", AbstractMessage.class);
            Object message = klass.newInstance();
            method.invoke(message, abstractMessage);

            return message;
        } catch (NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException | InstantiationException | IllegalAccessException | InvalidProtocolBufferException |
                 NullPointerException e) {
            if (isFiner()) {
                logFiner("Cannot read message(" + klassName + "): " + e);
            }

            throw new ConnectionException("Unable to read message from peer, connection closed", e).with(this);
        }
    }

    private Object handleFolderFilesChanged(byte[] data) throws InvalidProtocolBufferException {
        FolderFilesChangedProto.FolderFilesChanged folderFilesChangedProto = FolderFilesChangedProto.FolderFilesChanged.parseFrom(data);
        FolderFilesChangedExt folderFilesChangedExtWebSocket = new FolderFilesChangedExt();

        folderFilesChangedExtWebSocket.initFromD2D(folderFilesChangedProto);
        return folderFilesChangedExtWebSocket;
    }

    private String translateClassName(String className) {
        switch (className) {
            case "DownloadAbort":
                return "AbortDownload";
            case "DownloadRequest":
                return "RequestDownload";
            case "FileListRequest":
                return "BiFileListRequest";
            case "FilePartInfo":
                return "PartInfo";
            case "FilePartInfoList":
                return "FilePartsRecord";
            case "FilePartInfoListReply":
                return "ReplyFilePartsRecord";
            case "FilePartInfoListRequest":
                return "RequestFilePartsRecord";
            case "FilePartReply":
                return "FileChunk";
            case "FilePartRequest":
                return "RequestPart";
            case "NodeInfo":
                return "MemberInfo";
            case "NodeList":
                return "KnownNodes";
            case "NodeListRequest":
                return "RequestNodeList";
            case "UploadAbort":
                return "AbortUpload";
            case "UploadStart":
                return "StartUpload";
            case "UploadStop":
                return "StopUpload";
            default:
                return className;
        }
    }


    @Override
    protected Identity createOwnIdentity() {
        return new Identity(getController(),
                getController().getMySelf().getInfo(), getMyMagicId(), false, false,
                this);
    }

    @Override
    public boolean acceptIdentity(Member node) {
        if (node.isServer()) {

            sendMessagesAsynchron(IdentityReply.reject("Forbidden"));
            return false;
        }
        this.nodeStateMachine.setNode(node);
        Reject.ifNull(node, "node is null");
        member = node;
        if (isFiner()) {
            logFiner("Sending accept of identity to " + this);
        }
        sendMessagesAsynchron(IdentityReply.accept());
        return true;
    }

    class Receiver implements Runnable {
        @Override
        public void run() {
            try {
                while (started && isConnected()) {
                    processMessage();
                }
            } catch (Exception e) {
                handleException(e);
            } finally {
                shutdownWithMember();
            }
        }

        private void processMessage() throws IOException, ClassNotFoundException, ConnectionException, TransitionException {
            byte[] sizeArr = new byte[4];
            readDataHeader(sizeArr);
            int totalSize = getTotalSize(sizeArr);
            Object message = deserializeMessage(totalSize);
            handleBinaryMessage(message);
            handleKeepaliveMessage(totalSize);
        }

        private void readDataHeader(byte[] sizeArr) throws IOException {
            read(in, sizeArr, 0, sizeArr.length);
        }

        private int getTotalSize(byte[] sizeArr) throws IOException {
            int totalSize = Convert.convert2Int(sizeArr);
            if (!started || totalSize <= 0) {
                throw new IOException("Illegal packet size: " + totalSize);
            }
            return totalSize;
        }

        private Object deserializeMessage(int totalSize) throws IOException, ClassNotFoundException, ConnectionException {
            byte[] data = serializer.read(in, totalSize);
            return deserialize(data, totalSize);
        }

        private void handleBinaryMessage(Object message) throws TransitionException {
            if (message instanceof D2DObject) {
                handleSpecialMessage(message);
                // can be added events
                nodeStateMachine.fire(((D2DEvent) message).getNodeEvent(), message);
            }
        }

        private void handleSpecialMessage(Object message) {
            if (message instanceof Identity) {
                handleIdentityMessage((Identity) message);
            }
        }

        private void handleIdentityMessage(Identity message) {
            synchronized (identityWaiter) {
                identity = message;
                identityWaiter.notifyAll();
            }
            acceptConnection();
        }

        private void acceptConnection() {
            ConnectionListener.SocketAcceptor acceptor = getSocketAcceptor();
            if (acceptor == null) {
                logConnectionClose(null);
                shutdownWithMember();
            } else {
                acceptor.acceptConnection(ClientWebSocketConnectionHandler.this);
            }
        }

        private void handleKeepaliveMessage(int totalSize) {
            lastKeepaliveMessage = new Date();
            getController().getTransferManager().getTotalDownloadTrafficCounter().bytesTransferred(totalSize);
        }

        private void handleException(Exception e) {
            if (e instanceof SocketTimeoutException) {
                logFiner("Socket timeout on read, not disconnecting. " + e);
            } else if (e instanceof SocketException || e instanceof EOFException) {
                logConnectionClose(e);
            } else if (e instanceof InvalidClassException || e instanceof InvalidObjectException || e instanceof ClassNotFoundException) {
                handleUnknownMessage(e);
            } else if (e instanceof IOException || e instanceof ConnectionException) {
                logConnectionClose(e);
            } else if (e instanceof TransitionException) {
                logWarning(getMember() + ": Received unexpected message: " + e.getMessage());
            } else if (e instanceof RuntimeException) {
                logSevere("RuntimeException. " + e, e);
                shutdownWithMember();
                throw (RuntimeException) e;
            }
        }

        private void handleUnknownMessage(Exception e) {
            logFiner(e.getClass().getSimpleName(), e);
            String from = (getMember() != null) ? getMember().getNick() : this.toString();
            logWarning("Received unknown packet/class: " + e.getMessage() + " from " + from);
        }

        private boolean isConnected() {
            return getController().isStarted() && member != null && isConnected();
        }
    }

}
