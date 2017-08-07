package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.protocol.FolderCreateRequestProto;
import de.dal33t.powerfolder.protocol.FolderInfoProto;

public class FolderCreateRequest extends D2DRequestMessage {

    protected FolderInfo folderInfo;

    public FolderCreateRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public FolderCreateRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof FolderCreateRequestProto.FolderCreateRequest) {
            FolderCreateRequestProto.FolderCreateRequest proto = (FolderCreateRequestProto.FolderCreateRequest) message;
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
        FolderCreateRequestProto.FolderCreateRequest.Builder builder = FolderCreateRequestProto.FolderCreateRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.folderInfo != null) builder.setFolderInfo((FolderInfoProto.FolderInfo) this.folderInfo.toD2D());
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.folderInfo != null;
    }

}
