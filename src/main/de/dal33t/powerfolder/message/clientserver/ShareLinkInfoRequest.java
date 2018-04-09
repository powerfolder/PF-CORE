package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.protocol.ShareLinkInfoRequestProto;
import de.dal33t.powerfolder.util.StringUtils;

public class ShareLinkInfoRequest extends D2DRequestMessage {

    private String fileRelativePath;
    private String folderId;

    public ShareLinkInfoRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public ShareLinkInfoRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public String getFileRelativePath() {
        return fileRelativePath;
    }

    public void setFileRelativePath(String fileRelativePath) {
        this.fileRelativePath = fileRelativePath;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof ShareLinkInfoRequestProto.ShareLinkInfoRequest) {
            ShareLinkInfoRequestProto.ShareLinkInfoRequest proto = (ShareLinkInfoRequestProto.ShareLinkInfoRequest) message;
            this.requestCode = proto.getRequestCode();
            this.fileRelativePath = proto.getFileRelativePath();
            this.folderId = proto.getFolderId();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        ShareLinkInfoRequestProto.ShareLinkInfoRequest.Builder builder = ShareLinkInfoRequestProto.ShareLinkInfoRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.fileRelativePath != null) builder.setFileRelativePath(this.fileRelativePath);
        if (this.folderId != null) builder.setFolderId(this.folderId);
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.fileRelativePath != null && StringUtils.isNotBlank(this.folderId);
    }

}
