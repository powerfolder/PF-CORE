package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.FolderCreateRequestProto;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.protocol.NodeInfoProto;

public class FolderCreateRequest extends Message implements D2DObject {

    private static final long serialVersionUID = 100L;

    private String requestCode;
    private FolderInfo folderInfo;

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public FolderCreateRequest(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    public void setFolderInfo(FolderInfo folderInfo) {
        this.folderInfo = folderInfo;
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     **/
    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if (mesg instanceof FolderCreateRequestProto.FolderCreateRequest) {
            FolderCreateRequestProto.FolderCreateRequest proto = (FolderCreateRequestProto.FolderCreateRequest) mesg;
            this.requestCode = proto.getRequestCode();
            this.folderInfo = new FolderInfo(proto.getFolderInfo());
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     **/
    @Override
    public AbstractMessage toD2D() {
        FolderCreateRequestProto.FolderCreateRequest.Builder builder = FolderCreateRequestProto.FolderCreateRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.folderInfo != null) builder.setFolderInfo((FolderInfoProto.FolderInfo) this.folderInfo.toD2D());
        return builder.build();
    }

}
