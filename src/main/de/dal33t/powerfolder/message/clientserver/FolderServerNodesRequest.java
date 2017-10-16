package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.protocol.FolderServerNodesRequestProto;

public class FolderServerNodesRequest extends D2DRequestMessage {

    private FolderInfo[] folderInfos;

    public FolderServerNodesRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public FolderServerNodesRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public FolderInfo[] getFolderInfos() {
        return folderInfos;
    }

    public void setFolderInfos(FolderInfo[] folderInfos) {
        this.folderInfos = folderInfos;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof FolderServerNodesRequestProto.FolderServerNodesRequest) {
            FolderServerNodesRequestProto.FolderServerNodesRequest proto = (FolderServerNodesRequestProto.FolderServerNodesRequest) message;
            this.requestCode = proto.getRequestCode();
            this.folderInfos = new FolderInfo[proto.getFolderInfosCount()];
            int i = 0;
            for (FolderInfoProto.FolderInfo folderInfo : proto.getFolderInfosList()) {
                this.folderInfos[i++] = new FolderInfo(folderInfo);
            }
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        FolderServerNodesRequestProto.FolderServerNodesRequest.Builder builder = FolderServerNodesRequestProto.FolderServerNodesRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.folderInfos != null) {
            for (FolderInfo folderInfo : this.folderInfos) {
                builder.addFolderInfos((FolderInfoProto.FolderInfo) folderInfo.toD2D());
            }
        }
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.folderInfos != null;
    }

}
