package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.activity.domain.ActivityItem;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.protocol.ActivityItemProto;
import de.dal33t.powerfolder.protocol.ActivityListReplyProto;

import java.util.Collection;

public class ActivityListReply extends D2DReplyMessage {

    private Collection<ActivityItem> activityItems;

    public ActivityListReply() {
    }

    public ActivityListReply(String replyCode, StatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public ActivityListReply(String replyCode, StatusCode replyStatusCode, Collection<ActivityItem> activityItems) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.activityItems = activityItems;
    }

    public Collection<ActivityItem> getActivityItems() {
        return activityItems;
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
        ActivityListReplyProto.ActivityListReply.Builder builder = ActivityListReplyProto.ActivityListReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        builder.setReplyStatusCode(this.replyStatusCode.getCode());
        if (this.getActivityItems() != null) {
            for (ActivityItem activityItem : this.getActivityItems()) {
                builder.addActivityItems((ActivityItemProto.ActivityItem) activityItem.toD2D());
            }
        }
        return builder.build();
    }

}
