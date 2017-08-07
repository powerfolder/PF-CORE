package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.protocol.FolderRemoveRequestProto;

public class FolderRemoveRequest extends FolderCreateRequest {

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof FolderRemoveRequestProto.FolderRemoveRequest) {
            FolderRemoveRequestProto.FolderRemoveRequest proto = (FolderRemoveRequestProto.FolderRemoveRequest) message;
            this.requestCode = proto.getRequestCode();
            this.folderInfo = new FolderInfo(proto.getFolderInfo());
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        FolderRemoveRequestProto.FolderRemoveRequest.Builder builder = FolderRemoveRequestProto.FolderRemoveRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.folderInfo != null) builder.setFolderInfo((FolderInfoProto.FolderInfo) this.folderInfo.toD2D());
        return builder.build();
    }

}
