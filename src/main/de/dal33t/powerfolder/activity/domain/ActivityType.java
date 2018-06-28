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
