package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.protocol.FolderServerNodesReplyProto;
import de.dal33t.powerfolder.protocol.NodeInfoProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;

public class FolderServerNodesReply extends D2DReplyMessage {

    private MemberInfo[] nodeInfos;

    public FolderServerNodesReply() {
    }

    public FolderServerNodesReply(String replyCode, ReplyStatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public FolderServerNodesReply(String replyCode, ReplyStatusCode replyStatusCode, MemberInfo[] nodeInfos) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.nodeInfos = nodeInfos;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public FolderServerNodesReply(AbstractMessage message) {
        initFromD2D(message);
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof FolderServerNodesReplyProto.FolderServerNodesReply) {
            FolderServerNodesReplyProto.FolderServerNodesReply proto = (FolderServerNodesReplyProto.FolderServerNodesReply) message;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
            this.nodeInfos = new MemberInfo[proto.getNodeInfosCount()];
            int i = 0;
            for (NodeInfoProto.NodeInfo nodeInfo : proto.getNodeInfosList()) {
                this.nodeInfos[i++] = new MemberInfo(nodeInfo);
            }
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        FolderServerNodesReplyProto.FolderServerNodesReply.Builder builder = FolderServerNodesReplyProto.FolderServerNodesReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null)
            builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
        if (this.nodeInfos != null) {
            for (MemberInfo nodeInfo : this.nodeInfos) {
                builder.addNodeInfos((NodeInfoProto.NodeInfo) nodeInfo.toD2D());
            }
        }
        return builder.build();
    }

}
