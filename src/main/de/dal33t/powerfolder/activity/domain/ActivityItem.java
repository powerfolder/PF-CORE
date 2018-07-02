package de.dal33t.powerfolder.activity.domain;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.ActivityItemProto;
import de.dal33t.powerfolder.security.Account;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Representation of an Activity (see {@link ActivityType}).
 *
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
@Entity
public class ActivityItem implements D2DObject {

    public static final String PROPERTY_CREATION_DATE = "creationDate";
    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_INITIATOR = "initiator";

    @Id
    @Column(length = 32)
    private final String id;

    @Column(nullable = false)
    private final Date creationDate;

    @Enumerated(EnumType.STRING)
    @Index(name = "IDX_ACTIVITY_TYPE")
    private final ActivityType type;

    @ManyToOne
    @JoinColumn(name = "initiator_account_id")
    private final Account initiator;

    @ManyToOne
    @JoinColumn(name = "file_activity_id")
    private final FileActivity fileActivity;

    @ManyToOne
    @JoinColumn(name = "permission_activity_id")
    private final PermissionActivity permissionActivity;

    @ManyToOne
    @JoinColumn(name = "invitation_activity_id")
    private final InvitationActivity invitationActivity;

    // hibernate
    private ActivityItem() {
        id = null;
        creationDate = null;
        type = null;
        initiator = null;
        fileActivity = null;
        permissionActivity = null;
        invitationActivity = null;
    }

    ActivityItem(@NotNull Date creationDate,
                 @NotNull ActivityType type,
                 @NotNull Account initiator,
                 @Nullable FileActivity fileActivity,
                 @Nullable PermissionActivity permissionActivity,
                 @Nullable InvitationActivity invitationActivity) {
        this.id = UUID.randomUUID().toString().replaceAll("-", "");
        this.creationDate = creationDate;
        this.type = type;
        this.initiator = initiator;
        this.fileActivity = fileActivity;
        this.permissionActivity = permissionActivity;
        this.invitationActivity = invitationActivity;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull Date getCreationDate() {
        return creationDate;
    }

    public @NotNull ActivityType getType() {
        return type;
    }

    public @Nullable Account getInitiator() {
        return initiator;
    }

    public @Nullable FileActivity getFileActivity() {
        return fileActivity;
    }

    public @Nullable PermissionActivity getPermissionActivity() {
        return permissionActivity;
    }

    public @Nullable InvitationActivity getInvitationActivity() {
        return invitationActivity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ActivityItem that = (ActivityItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
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
        ActivityItemProto.ActivityItem.Builder builder = ActivityItemProto.ActivityItem.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setId(this.getId());
        if (this.getFileActivity() != null) {
            builder.setActivity(com.google.protobuf.Any.pack(this.getFileActivity().toD2D()));
        } else if (this.getPermissionActivity() != null) {
            builder.setActivity(com.google.protobuf.Any.pack(this.getPermissionActivity().toD2D()));
        } else if (this.getInvitationActivity() != null) {
            builder.setActivity(com.google.protobuf.Any.pack(this.getInvitationActivity().toD2D()));
        }
        builder.setCreationDate(this.getCreationDate().getTime());
        builder.setActivityType(this.getType().toD2D());
        return builder.build();
    }

}
