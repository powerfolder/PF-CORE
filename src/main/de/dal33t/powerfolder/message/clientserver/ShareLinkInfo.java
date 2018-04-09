package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.protocol.ShareLinkInfoProto;

import java.util.Date;

public class ShareLinkInfo extends D2DReplyMessage {

    private int downloads;
    private Date expirationDate;
    private String fileRelativePath;
    private String folderId;
    private String id;
    private int maxDownloads;
    private String password;
    private boolean uploadEnabled;
    private String url;

    public ShareLinkInfo() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public ShareLinkInfo(AbstractMessage message) {
        initFromD2D(message);
    }

    public int getDownloads() {
        return downloads;
    }

    public void setDownloads(int downloads) {
        this.downloads = downloads;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(int maxDownloads) {
        this.maxDownloads = maxDownloads;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUploadEnabled() {
        return uploadEnabled;
    }

    public void setUploadEnabled(boolean uploadEnabled) {
        this.uploadEnabled = uploadEnabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof ShareLinkInfoProto.ShareLinkInfo) {
            ShareLinkInfoProto.ShareLinkInfo proto = (ShareLinkInfoProto.ShareLinkInfo) message;
            this.downloads = proto.getDownloads();
            if (proto.getExpirationDate() >= 0) {
                this.expirationDate = new Date(proto.getExpirationDate());
            }
            this.fileRelativePath = proto.getFileRelativePath();
            this.folderId = proto.getFolderId();
            this.id = proto.getId();
            this.maxDownloads = proto.getMaxDownloads();
            this.password = proto.getPassword();
            this.uploadEnabled = proto.getUploadEnabled();
            this.url = proto.getUrl();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        ShareLinkInfoProto.ShareLinkInfo.Builder builder = ShareLinkInfoProto.ShareLinkInfo.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setDownloads(this.downloads);
        if (this.expirationDate != null) {
            builder.setExpirationDate(this.expirationDate.getTime());
        } else {
            builder.setExpirationDate(-1);
        }
        if (this.fileRelativePath != null) builder.setFileRelativePath(this.fileRelativePath);
        if (this.folderId != null) builder.setFolderId(this.folderId);
        if (this.id != null) builder.setId(this.id);
        builder.setMaxDownloads(this.maxDownloads);
        if (this.password != null) builder.setPassword(this.password);
        builder.setUploadEnabled(this.uploadEnabled);
        if (this.url != null) builder.setUrl(this.url);
        return builder.build();
    }

}
