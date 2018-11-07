package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.d2d.D2DReplyFromServer;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.light.GroupInfo;
import de.dal33t.powerfolder.protocol.GroupInfoProto;
import de.dal33t.powerfolder.protocol.GroupSearchReplyProto;

import java.util.Collection;

public class GroupSearchReply extends D2DReplyMessage implements D2DReplyFromServer {

    private Collection<GroupInfo> groupInfos;

    public GroupSearchReply() {
    }

    public GroupSearchReply(String replyCode, StatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public GroupSearchReply(String replyCode, StatusCode replyStatusCode, Collection<GroupInfo> groupInfos) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.groupInfos = groupInfos;
    }

    public Collection<GroupInfo> getGroupInfos() {
        return groupInfos;
    }

    public void setGroupInfos(Collection<GroupInfo> groupInfos) {
        this.groupInfos = groupInfos;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public GroupSearchReply(AbstractMessage message) {
        initFromD2D(message);
    }


    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        GroupSearchReplyProto.GroupSearchReply.Builder builder = GroupSearchReplyProto.GroupSearchReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        builder.setReplyStatusCode(this.replyStatusCode.getCode());
        if (this.groupInfos != null) {
            for (GroupInfo groupInfo : this.groupInfos) {
                builder.addGroupInfos((GroupInfoProto.GroupInfo) groupInfo.toD2D());
            }
        }
        return builder.build();
    }

}
