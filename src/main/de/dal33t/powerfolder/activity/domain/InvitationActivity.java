package de.dal33t.powerfolder.activity.domain;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.protocol.InvitationActivityProto;
import de.dal33t.powerfolder.security.AccessMode;
import de.dal33t.powerfolder.security.Account;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.util.UUID;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
@Entity
public class InvitationActivity implements D2DObject {
    @Id
    @Column(length = 32)
    private final String id;

    @ManyToOne
    @JoinColumn(name = "folder_id")
    private final FolderInfo folderInfo;

    @Enumerated(EnumType.STRING)
    private final AccessMode permissionType;

    @ManyToOne
    @JoinColumn(name = "recipient_account_id")
    private final Account recipientAccount;

    // hibernate
    private InvitationActivity() {
        id = null;
        folderInfo = null;
        permissionType = null;
        recipientAccount = null;
    }

    InvitationActivity(@NotNull FolderInfo folderInfo,
                       @NotNull AccessMode permissionType,
                       @NotNull Account recipientAccount) {
        this.id = UUID.randomUUID().toString().replaceAll("-", "");
        this.folderInfo = folderInfo;
        this.permissionType = permissionType;
        this.recipientAccount = recipientAccount;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull FolderInfo getFolderInfo() {
        return folderInfo;
    }

    public @NotNull AccessMode getPermissionType() {
        return permissionType;
    }

    public @Nullable Account getRecipientAccount() {
        return recipientAccount;
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
        InvitationActivityProto.InvitationActivity.Builder builder = InvitationActivityProto.InvitationActivity.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setId(this.getId());
        builder.setFolderId(this.getFolderInfo().getId());
        if (this.getPermissionType().toD2D() != null) builder.setPermissionType(this.getPermissionType().toD2D());
        if (this.getRecipientAccount() != null) {
            builder.setFolderId(this.getRecipientAccount().getOID());
        }
        return builder.build();
    }

}
