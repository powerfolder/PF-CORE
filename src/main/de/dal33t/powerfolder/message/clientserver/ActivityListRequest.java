/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
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
