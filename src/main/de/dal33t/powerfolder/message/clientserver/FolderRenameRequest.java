package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.protocol.FolderRenameRequestProto;

public class FolderRenameRequest extends Message implements D2DObject {

    private static final long serialVersionUID = 100L;

    private String requestCode;
    private FolderInfo folderInfo;
    private String name;

    public FolderRenameRequest() {}

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public FolderRenameRequest(AbstractMessage mesg) {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     **/
    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if (mesg instanceof FolderRenameRequestProto.FolderRenameRequest) {
            FolderRenameRequestProto.FolderRenameRequest proto = (FolderRenameRequestProto.FolderRenameRequest) mesg;
            this.requestCode = proto.getRequestCode();
            this.folderInfo = new FolderInfo(proto.getFolderInfo());
            this.name = proto.getName();
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
        FolderRenameRequestProto.FolderRenameRequest.Builder builder = FolderRenameRequestProto.FolderRenameRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.folderInfo != null) builder.setFolderInfo((FolderInfoProto.FolderInfo) this.folderInfo.toD2D());
        if (this.name != null) builder.setName(this.name);
        return builder.build();
    }

}
