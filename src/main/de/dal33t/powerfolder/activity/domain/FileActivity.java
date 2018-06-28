package de.dal33t.powerfolder.activity.domain;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.protocol.FileActivityProto;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
@Entity
public class FileActivity implements D2DObject {
    @Id
    @Column(length = 32)
    private final String id;

    @ManyToOne
    @JoinColumn(name = "folder_id")
    private final FolderInfo folderInfo;

    @Index(name = "IDX_REL_PATH")
    private final String relativePath;

    private final Date modificationDate;

    // hibernate
    private FileActivity() {
        id = null;
        folderInfo = null;
        relativePath = null;
        modificationDate = null;
    }

    FileActivity(@NotNull FolderInfo folderInfo,
                 @NotNull String relativePath,
                 @NotNull Date modificationDate) {
        this.id = UUID.randomUUID().toString().replaceAll("-", "");
        this.folderInfo = folderInfo;
        this.relativePath = relativePath;
        this.modificationDate = modificationDate;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull FolderInfo getFolderInfo() {
        return folderInfo;
    }

    public @NotNull String getRelativePath() {
        return relativePath;
    }

    public @NotNull Date getModificationDate() {
        return modificationDate;
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
        FileActivityProto.FileActivity.Builder builder = FileActivityProto.FileActivity.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setId(this.getId());
        builder.setFileId(this.getFolderInfo().getId() + "/" + this.getRelativePath());
        builder.setModificationDate(this.getModificationDate().getTime());
        return builder.build();
    }

}
