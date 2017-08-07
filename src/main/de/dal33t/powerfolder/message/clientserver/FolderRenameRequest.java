package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.protocol.FolderRenameRequestProto;

public class FolderRenameRequest extends FolderCreateRequest {

    private String name;

    public String getName() {
        return name;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof FolderRenameRequestProto.FolderRenameRequest) {
            FolderRenameRequestProto.FolderRenameRequest proto = (FolderRenameRequestProto.FolderRenameRequest) message;
            this.requestCode = proto.getRequestCode();
            this.folderInfo = new FolderInfo(proto.getFolderInfo());
            this.name = proto.getName();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        FolderRenameRequestProto.FolderRenameRequest.Builder builder = FolderRenameRequestProto.FolderRenameRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.folderInfo != null) builder.setFolderInfo((FolderInfoProto.FolderInfo) this.folderInfo.toD2D());
        if (this.name != null) builder.setName(this.name);
        return builder.build();
    }

}
