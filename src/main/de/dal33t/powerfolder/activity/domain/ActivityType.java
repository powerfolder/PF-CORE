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
package de.dal33t.powerfolder.activity.domain;

import de.dal33t.powerfolder.protocol.ActivityTypeProto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * All supported activity types.
 *
 * @author <a href="mailto:wiegmann@powerfolder.com>Jan Wiegmann</a>
 */
public enum ActivityType {

    FILE_CREATED,
    FILE_MODIFIED,
    FILE_DELETED,

    INVITATION_ACCEPTED,
    INVITATION_DECLINED,
    INVITATION_RECEIVED,

    PERMISSION_GRANTED,
    PERMISSION_CHANGED,
    PERMISSION_REVOKED,
    PERMISSION_PUBLIC;

    public @NotNull ActivityTypeProto.ActivityType toD2D() {
        switch (this) {
            case FILE_CREATED:
                return ActivityTypeProto.ActivityType.FILE_CREATED;
            case FILE_MODIFIED:
                return ActivityTypeProto.ActivityType.FILE_MODIFIED;
            case FILE_DELETED:
                return ActivityTypeProto.ActivityType.FILE_DELETED;
            case INVITATION_ACCEPTED:
                return ActivityTypeProto.ActivityType.INVITATION_ACCEPTED;
            case INVITATION_DECLINED:
                return ActivityTypeProto.ActivityType.INVITATION_DECLINED;
            case INVITATION_RECEIVED:
                return ActivityTypeProto.ActivityType.INVITATION_RECEIVED;
            case PERMISSION_GRANTED:
                return ActivityTypeProto.ActivityType.PERMISSION_GRANTED;
            case PERMISSION_CHANGED:
                return ActivityTypeProto.ActivityType.PERMISSION_CHANGED;
            case PERMISSION_REVOKED:
                return ActivityTypeProto.ActivityType.PERMISSION_REVOKED;
            case PERMISSION_PUBLIC:
                return ActivityTypeProto.ActivityType.PERMISSION_PUBLIC;
        }
        return ActivityTypeProto.ActivityType.UNRECOGNIZED;
    }

    public static @Nullable ActivityType getEnum(ActivityTypeProto.ActivityType activityType) {
        switch (activityType) {
            case FILE_CREATED:
                return FILE_CREATED;
            case FILE_MODIFIED:
                return FILE_MODIFIED;
            case FILE_DELETED:
                return FILE_DELETED;
            case INVITATION_ACCEPTED:
                return INVITATION_ACCEPTED;
            case INVITATION_DECLINED:
                return INVITATION_DECLINED;
            case INVITATION_RECEIVED:
                return INVITATION_RECEIVED;
            case PERMISSION_GRANTED:
                return PERMISSION_GRANTED;
            case PERMISSION_CHANGED:
                return PERMISSION_CHANGED;
            case PERMISSION_REVOKED:
                return PERMISSION_REVOKED;
            case PERMISSION_PUBLIC:
                return PERMISSION_PUBLIC;
            default:
                return null;
        }
    }

}
