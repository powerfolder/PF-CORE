package de.dal33t.powerfolder.message;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.clientserver.ReplyStatusCode;
import de.dal33t.powerfolder.protocol.*;
import de.dal33t.powerfolder.security.*;

public class NodeListReply extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private int replyCode;
    private ReplyStatusCode replyStatusCode;
    private KnownNodes nodeList;

    /**
     * Serialization constructor
     */
    public NodeListReply() {
    }

    public NodeListReply(int replyCode, ReplyStatusCode replyStatusCode, KnownNodes nodeList) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.nodeList = nodeList;
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public NodeListReply(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public int getReplyCode() {
        return replyCode;
    }

    public void setReplyCode(int replyCode) {
        this.replyCode = replyCode;
    }

    public ReplyStatusCode getReplyStatusCode() {
        return replyStatusCode;
    }

    public void setReplyStatusCode(ReplyStatusCode replyStatusCode) {
        this.replyStatusCode = replyStatusCode;
    }

    public KnownNodes getNodeList() {
        return nodeList;
    }

    public void setNodeList(KnownNodes nodeList) {
        this.nodeList = nodeList;
    }

    /**
     * initFromD2DMessage
     * Init from D2D message
     *
     * @param mesg Message to use data from
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     **/
    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if (mesg instanceof NodeListReplyProto.NodeListReply) {
            NodeListReplyProto.NodeListReply proto = (NodeListReplyProto.NodeListReply) mesg;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
            this.nodeList = new KnownNodes(proto.getNodeList());
        }
    }

    /**
     * toD2D
     * Convert to D2D message
     *
     * @return Converted D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     **/
    @Override
    public AbstractMessage toD2D() {
        NodeListReplyProto.NodeListReply.Builder builder = NodeListReplyProto.NodeListReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode)this.replyStatusCode.toD2D());
        if (this.nodeList != null) builder.setNodeList((NodeListProto.NodeList)this.nodeList.toD2D());
        return builder.build();
    }
}
