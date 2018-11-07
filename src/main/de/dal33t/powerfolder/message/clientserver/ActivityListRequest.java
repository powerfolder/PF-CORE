package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.activity.domain.ActivityType;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.d2d.D2DRequestToServer;
import de.dal33t.powerfolder.d2d.NodeEvent;
import de.dal33t.powerfolder.protocol.ActivityListRequestProto;

import java.util.Date;

public class ActivityListRequest extends D2DRequestMessage implements D2DRequestToServer {

    private String accountId;
    private ActivityType activityType;
    private Date startDate;

    public ActivityListRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public ActivityListRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public String getAccountId() {
        return accountId;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public Date getStartDate() {
        return startDate;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof ActivityListRequestProto.ActivityListRequest) {
            ActivityListRequestProto.ActivityListRequest proto = (ActivityListRequestProto.ActivityListRequest) message;
            this.requestCode = proto.getRequestCode();
            this.accountId = proto.getAccountId();
            this.activityType = ActivityType.getEnum(proto.getActivityType());
            this.startDate = new Date(proto.getStartDate());
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        ActivityListRequestProto.ActivityListRequest.Builder builder = ActivityListRequestProto.ActivityListRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.accountId != null) builder.setAccountId(this.accountId);
        if (this.activityType != null) builder.setActivityType(this.getActivityType().toD2D());
        if (this.startDate != null) builder.setStartDate(this.getStartDate().getTime());
        return builder.build();
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.ACTIVITY_LIST_REQUEST;
    }

}
